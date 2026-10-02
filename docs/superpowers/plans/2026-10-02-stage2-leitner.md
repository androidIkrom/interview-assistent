# Stage 2: Retry and Leitner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Answers are remembered: each question gets a Leitner box and a due day, missed questions come back in a "Xatolar" session on later days, and Home and Topics show a readiness percentage.

**Architecture:** The Leitner rules, the state-aware question picking and the readiness calculation are pure functions in `:core:engine`. `:app` stores one row per answered question in Room and feeds the states to the engine. Content stays in assets and is joined to the state by question id only.

**Tech Stack:** as Stage 1, plus Room 2.8.5 (KSP 2.3.12), Robolectric 4.17, androidx.test core 1.7.0.

**Spec:** `docs/superpowers/specs/2026-10-02-devsuhbat-design.md` (sections 4.4, 5.1, 5.2, 5.4, 6, 7.4, 8).

## Global Constraints

- Boxes are 1 to 5. Intervals: box 1 → 1 day, 2 → 3, 3 → 7, 4 → 14, 5 → 30.
- New question, first try correct: box 3. Seen question, first try correct: `min(box + 1, 5)`. Any wrong submission: box 1. `dueDay = today + interval(box)`.
- A question is mastered when `box >= 3`.
- Only the first presentation of a question in a session changes its state; the in-session repeat does not.
- The state is written after every solved question, not at the end of the session.
- "Xatolar" session: due questions (`dueDay <= today`) of the chosen field and level, at most 20.
- Readiness: `mastered / all questions of the chosen level and below`, overall and per topic. States of question ids that are not in the content are ignored.
- Days are local calendar days (`LocalDate.toEpochDay()` in the device time zone).
- Room schema version 1 with all three tables of spec 7.4, schema exported to `app/schemas`.
- Stage 1 constraints still hold (Uzbek UI strings in `strings.xml`, no correct-answer reveal, manual DI, no AI co-author line).

## Review Focus

1. A question missed today must not be due today: it is due tomorrow. Test in Task 1 (`LeitnerTest.wrongAnswerIsDueTomorrow`) and Task 2 (`mistakesExcludeQuestionsDueLater`).
2. States for question ids that no longer exist in the content must not count in readiness or the due count, and must not crash. Test in Task 2 (`ReadinessTest.ignoresStatesOfUnknownQuestions`).
3. After the user lowers the level, due questions above the new level must not appear or be counted. Test in Task 2 (`mistakesRespectTheLevel`).
4. The in-session repeat must not write a second outcome. Test in Task 4 (`outcomeIsReportedOncePerQuestion`).
5. Killing the app mid-session must keep the answers given so far: the outcome is reported when the question is solved, before `next()`. Test in Task 4 (`outcomeIsReportedAsSoonAsSolved`).

---

### Task 1: Leitner

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/engine/Leitner.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/LeitnerTest.kt`

**Interfaces:**
- Consumes: `QuestionOutcome(questionId, firstTryCorrect, wrongSubmissions)`.
- Produces:

```kotlin
data class QuestionState(val questionId: String, val box: Int, val dueDay: Long, val attempts: Int = 0, val wrongAttempts: Int = 0) {
    val mastered: Boolean            // box >= Leitner.MASTERED_BOX
    fun isDue(today: Long): Boolean  // dueDay <= today
}
object Leitner {
    const val FIRST_BOX = 1; const val MASTERED_BOX = 3; const val MAX_BOX = 5
    fun intervalDays(box: Int): Long                 // 1, 3, 7, 14, 30; throws IllegalArgumentException outside 1..5
    fun afterOutcome(previous: QuestionState?, outcome: QuestionOutcome, today: Long): QuestionState
}
```

`afterOutcome` adds 1 to `attempts` and `outcome.wrongSubmissions` to `wrongAttempts`.

- [ ] **Step 1: Write the failing tests** (`today = 100`):

```kotlin
@Test fun intervals()                              // 1,3,7,14,30
@Test(expected = IllegalArgumentException::class) fun intervalOfUnknownBoxThrows()   // box 0
@Test fun newQuestionFirstTryGoesToBoxThree()      // previous null, firstTry -> QuestionState(id, 3, 107, 1, 0)
@Test fun newQuestionMissedGoesToBoxOne()          // wrongSubmissions 2 -> QuestionState(id, 1, 101, 1, 2)
@Test fun wrongAnswerIsDueTomorrow()               // previous box 4 -> box 1, dueDay 101, !isDue(100), isDue(101)
@Test fun seenQuestionFirstTryMovesUpOneBox()      // box 1 -> 2 (due 103); box 3 -> 4 (due 114)
@Test fun boxNeverExceedsFive()                    // box 5 -> 5, due 130
@Test fun countersAccumulate()                     // previous attempts 3, wrong 2; outcome wrong 1 -> attempts 4, wrong 3
@Test fun masteredFromBoxThree()                   // box 2 false, box 3 true
```

- [ ] **Step 2:** Run `./gradlew :core:engine:test`. Expected: compilation failure.
- [ ] **Step 3:** Implement `Leitner.kt`.
- [ ] **Step 4:** Run `./gradlew :core:engine:test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(engine): Leitner boxes and question state`.

### Task 2: State-aware picking and readiness

**Files:**
- Modify: `core/engine/src/main/kotlin/uz/devsuhbat/engine/QuestionPicker.kt`
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/engine/Readiness.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/QuestionPickerTest.kt` (add), `ReadinessTest.kt`

**Interfaces:**
- Consumes: `QuestionState` (Task 1).
- Produces:

```kotlin
object QuestionPicker {
    const val PRACTICE_SIZE = 10; const val MISTAKES_SIZE = 20
    fun eligible(pool: List<Question>, maxLevel: Level): List<Question>
    fun practice(pool: List<Question>, maxLevel: Level, random: Random, count: Int = PRACTICE_SIZE,
                 states: Map<String, QuestionState> = emptyMap(), today: Long = 0): List<Question>
    fun due(pool: List<Question>, maxLevel: Level, states: Map<String, QuestionState>, today: Long): List<Question>
    fun mistakes(pool: List<Question>, maxLevel: Level, states: Map<String, QuestionState>, today: Long,
                 random: Random, count: Int = MISTAKES_SIZE): List<Question>
}
data class Progress(val mastered: Int, val total: Int) { val percent: Int }   // mastered * 100 / total, 0 when total == 0
object Readiness { fun of(questions: List<Question>, states: Map<String, QuestionState>): Progress }
```

`practice` order: due questions first, then unseen, then seen-but-not-due by ascending box; random inside each group. `due` keeps pool order. `mistakes` returns due questions, most overdue first (ascending `dueDay`), random among equal days.

- [ ] **Step 1: Write the failing tests**

```kotlin
// QuestionPickerTest additions — pool of 6 junior questions q1..q6, today = 100
@Test fun practicePrefersDueThenUnseenThenLowestBox()  // q1 due (box 1, day 99); q2 box 4 day 200; q3 box 2 day 150; q4..q6 unseen; count 5 -> first is q1, next three are q4,q5,q6 in any order, last is q3
@Test fun practiceWithoutStatesBehavesAsBefore()       // existing tests stay green
@Test fun dueReturnsOnlyDueQuestionsInPoolOrder()
@Test fun mistakesAreMostOverdueFirst()                // q1 day 90, q2 day 100, q3 day 95 -> [q1, q3, q2]
@Test fun mistakesExcludeQuestionsDueLater()           // day 101 not included
@Test fun mistakesRespectTheLevel()                    // a senior question that is due is skipped for Level.JUNIOR
@Test fun mistakesAreCappedAtCount()                   // 25 due -> 20

// ReadinessTest
@Test fun countsMasteredQuestions()                    // 4 questions; boxes 3, 5, 2, unseen -> Progress(2, 4), percent 50
@Test fun emptyPoolIsZeroPercent()                     // Progress(0, 0).percent == 0
@Test fun ignoresStatesOfUnknownQuestions()            // state for "gone.001" box 5 -> not counted
@Test fun percentRoundsDown()                          // Progress(1, 3).percent == 33
```

- [ ] **Step 2:** Run `./gradlew :core:engine:test`. Expected: compilation failure.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run `./gradlew :core:engine:test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(engine): state-aware question picking and readiness`.

### Task 3: Room storage and ProgressRepository

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts` (KSP, Room, Robolectric, androidx.test), `AppContainer.kt`
- Create: `app/src/main/java/uz/devsuhbat/data/AppDatabase.kt` (entities, DAO, database), `data/ProgressRepository.kt`, `app/src/test/resources/robolectric.properties` (`application=android.app.Application`)
- Test: `app/src/test/java/uz/devsuhbat/data/ProgressRepositoryTest.kt` (Robolectric, `@Config(sdk = [35])`, in-memory Room)

**Interfaces:**
- Consumes: `Leitner`, `QuestionState`, `QuestionOutcome`, `SessionResult`, `Level`.
- Produces:

```kotlin
@Entity(tableName = "question_state") data class QuestionStateEntity(@PrimaryKey val questionId: String, val box: Int, val dueDay: Long, val attempts: Int, val wrongAttempts: Int, val lastAnsweredAt: Long)
@Entity(tableName = "session_log") data class SessionLogEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val mode: String, val fieldId: String, val level: String, val startedAt: Long, val finishedAt: Long, val total: Int, val firstTryCorrect: Int)
@Entity(tableName = "mock_topic_result", primaryKeys = ["sessionId", "topicId"]) data class MockTopicResultEntity(val sessionId: Long, val topicId: String, val total: Int, val correct: Int)
@Database(version = 1, exportSchema = true) abstract class AppDatabase : RoomDatabase() { abstract fun progressDao(): ProgressDao }

enum class SessionMode { PRACTICE, MISTAKES, MOCK }

class ProgressRepository(private val db: AppDatabase, private val clock: Clock = Clock.systemDefaultZone()) {
    fun today(): Long                                             // LocalDate.now(clock).toEpochDay()
    val states: Flow<Map<String, QuestionState>>                  // keyed by question id
    suspend fun record(outcome: QuestionOutcome)                  // read previous, Leitner.afterOutcome, upsert, in one transaction
    suspend fun logSession(mode: SessionMode, fieldId: String, level: Level, startedAt: Instant, result: SessionResult): Long
    suspend fun reset()                                           // deletes every row of all three tables
}
```

`AppContainer` gains `val progress: ProgressRepository` (database file `devsuhbat.db`).

- [ ] **Step 1: Write the failing tests** (fixed clock at 2026-10-02T10:00 in `Asia/Tashkent`; `today = LocalDate.of(2026, 10, 2).toEpochDay()`):

```kotlin
@Test fun todayIsTheLocalCalendarDay()            // clock 2026-10-02T23:30+05:00 (18:30Z) -> day of 2026-10-02
@Test fun statesStartEmpty()
@Test fun recordStoresFirstTryInBoxThree()        // states.first()["q.001"] == QuestionState("q.001", 3, today + 7, 1, 0)
@Test fun recordAppliesLeitnerToTheStoredState()  // miss, then first try -> box 2, attempts 2, wrongAttempts 1
@Test fun logSessionStoresARow()                  // returned id > 0; dao.sessions() has mode "PRACTICE", level "strong_middle", total 10, firstTryCorrect 7
@Test fun resetClearsEverything()
```

- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.data.ProgressRepositoryTest"`. Expected: compilation failure.
- [ ] **Step 3:** Add the dependencies, implement database and repository, wire `AppContainer`.
- [ ] **Step 4:** Run the test. Expected: PASS. `app/schemas/uz.devsuhbat.data.AppDatabase/1.json` exists.
- [ ] **Step 5:** Commit: `feat(app): Room progress storage`.

### Task 4: Sessions write progress; "Xatolar" mode

**Files:**
- Modify: `ui/session/SessionViewModel.kt`, `ui/session/SessionScreen.kt`, `ui/session/SessionResultScreen.kt`, `ui/Nav.kt`
- Test: `app/src/test/java/uz/devsuhbat/ui/session/SessionViewModelTest.kt` (add)

**Interfaces:**
- Consumes: `ProgressRepository`, `QuestionPicker.practice` / `mistakes`.
- Produces:

```kotlin
class SessionViewModel(
    private val loadQuestions: suspend () -> List<Question>,
    private val random: Random,
    private val onOutcome: suspend (QuestionOutcome) -> Unit = {},
    private val onFinished: suspend (SessionResult) -> Unit = {},
) : ViewModel()

object Routes { const val MISTAKES = "mistakes" }   // used as the topicId of the session route
```

`onOutcome` is called once per distinct question, as soon as its first presentation is solved. `onFinished` is called once when the session ends with `total > 0`. The screen passes `progress::record` and a `logSession` call with mode `MISTAKES` for `Routes.MISTAKES`, otherwise `PRACTICE`. For `Routes.MISTAKES` the questions come from `QuestionPicker.mistakes` over all topics of the field; practice sessions pass the stored states and `progress.today()` to `QuestionPicker.practice`. The result screen hides "Yana mashq" in mistakes mode, and "back" from the result returns to the screen the session was opened from.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun outcomeIsReportedAsSoonAsSolved()     // toggle a; check() -> outcomes == [QuestionOutcome("t.001", true, 0)] before next()
@Test fun outcomeIsReportedOncePerQuestion()    // miss q1, finish incl. the repeat -> 3 outcomes, q1 is (false, 1)
@Test fun wrongSubmissionAloneReportsNothing()  // toggle c; check() -> outcomes empty
@Test fun finishedIsReportedOnce()              // finished == [SessionResult(3, 3, 0)]
@Test fun emptySessionIsNotReported()           // finished empty
```

- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.session.SessionViewModelTest"`. Expected: compilation failure.
- [ ] **Step 3:** Implement the view model change, then the screen and navigation changes.
- [ ] **Step 4:** Run `./gradlew test :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 5:** Commit: `feat(app): sessions record progress; mistakes session`.

### Task 5: Readiness on Home and Topics; reset in Settings

**Files:**
- Modify: `ui/home/HomeViewModel.kt`, `ui/home/HomeScreen.kt`, `ui/topics/TopicsViewModel.kt`, `ui/topics/TopicsScreen.kt`, `ui/settings/SettingsScreen.kt`, `res/values/strings.xml`
- Test: `app/src/test/java/uz/devsuhbat/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Produces:

```kotlin
data class HomeUiState(val loading: Boolean = true, val fieldTitle: String? = null, val level: Level? = null,
                       val readiness: Progress = Progress(0, 0), val dueCount: Int = 0)
class HomeViewModel(content: ContentStore, settings: Flow<UserSettings>, states: Flow<Map<String, QuestionState>>, today: () -> Long, io: CoroutineDispatcher)
data class TopicRow(val topic: Topic, val questionCount: Int, val progress: Progress)
```

Both view models combine the settings flow with the states flow, so the numbers update when the user returns from a session. Home shows the readiness percentage with a progress bar and "N / M savol o'zlashtirilgan", and a "Xatolar" card with the due count; its button is disabled and the card reads "Bugun takrorlanadigan savol yo'q" when the count is 0. Each topic row shows its percentage. Settings gets "Progressni tozalash" behind a confirmation dialog.

- [ ] **Step 1: Write the failing tests** (fake flows, a `ContentStore` over an in-memory map with 4 junior questions):

```kotlin
@Test fun showsReadinessAndDueCount()        // states: q1 box 3 day 200, q2 box 1 day 100, today 100 -> readiness Progress(1, 4), dueCount 1
@Test fun updatesWhenStatesChange()          // emit new states -> state changes
@Test fun fieldWithoutContentIsAllZero()     // Progress(0, 0), dueCount 0, fieldTitle set
```

- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.home.HomeViewModelTest"`. Expected: compilation failure.
- [ ] **Step 3:** Implement view models and screens.
- [ ] **Step 4:** Run `./gradlew clean test :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 5:** Commit: `feat(app): readiness on home and topics, progress reset`.

### Task 6: Verification

- [ ] **Step 1:** `./gradlew clean test :app:assembleDebug` → `BUILD SUCCESSFUL`.
- [ ] **Step 2:** Device check only if the emulator's foreground app is not another project's; otherwise record that the UI was not exercised.
- [ ] **Step 3:** Push the branch and open a pull request against `master`.
