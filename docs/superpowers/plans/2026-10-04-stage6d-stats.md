# Stage 6d — Statistics Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the third bottom tab, **Statistika**, with four cards:
- activity: current and longest streak, plus an 18-week heatmap;
- Leitner breakdown: new, learning and mastered questions;
- every topic's mastery, weakest first;
- the field's last 10 mock interviews as a bar chart.

**Architecture:**
- Grid and breakdown rules are pure `core/engine` functions.
- `ProgressRepository` gains a mock-history flow.
- A new `StatsViewModel` combines the settings, states, session days and mock history.
- `StatsScreen` draws the cards with the 6a components and `Canvas`.
- The tab joins `TOP_LEVEL_ROUTES`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 (stable 1.4), Navigation Compose 2.10, Room 2.8, JUnit 4, Robolectric, kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-10-04-stage6-ui-redesign-design.md` (section 8; sections 10–12 for 6d)

## Global Constraints

- `minSdk 26`; no new Gradle dependencies (charts are drawn with `Canvas`); no Room schema change (new `@Query` only).
- UI text is Uzbek (Latin) in `res/values/strings.xml`, with apostrophes escaped as `\'`.
- Colours come only from the theme and `LocalExtraColors`. Text containers use `heightIn(min = …)`. Decorative motion stops under `LocalReducedMotion`.
- **Data scope:**
  - streak and heatmap count sessions of **every** field;
  - Leitner, topics and mock history use the **current** field and level (spec 11).
- Gradle: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"; export PATH="$JAVA_HOME/bin:$PATH"`, run from `/d/projects/devsuhbat` in the Bash tool, wrapped in `timeout`; trust results only after "BUILD SUCCESSFUL".
- Commits: no co-author line. Branch `stage6/6d-stats` from `master`. PR to `master`, Uzbek body ending with the Claude Code line; the user merges.
- **Emulator (shared):**
  - Check `C:\Users\user\AppData\Local\Temp\claude\emulator.lock`. If it is absent, write `devsuhbat <ISO time>`; if it is present, wait.
  - When done, delete the lock and message `uds:\\.\pipe\LOCAL\cc-msg-992fefb1fe6cf1e462494b794237a87a`.
  - Use `adb -s emulator-5554` and `install -r` of `app-release.apk`.
  - Restore every setting you change.

## Review Focus

1. **Heatmap placement:**
   - the last column is the current Monday–Sunday week;
   - today is on its weekday row;
   - later days in the week are empty, not zero;
   - a session at 23:50 local time counts for that local day.

   Test in Task 1 (`todaySitsInTheLastColumnOnItsWeekday`). The local day itself comes from `sessionDays()` (6b).
2. **Old history:** sessions older than the 18 weeks are ignored, without a crash or a wrapped count. Test in Task 1 (`daysOutsideTheWindowAreIgnored`).
3. **Mock history:**
   - only the current field;
   - the newest 10;
   - drawn oldest to newest;
   - another field's mocks never appear.

   Test in Task 3 (`mockHistoryIsTheFieldsLastTenOldestFirst`).
4. **Field switch:** Leitner, topics and mocks follow the new field, while the streak and heatmap stay the same. Test in Task 4 (`switchingFieldKeepsActivityButChangesTheRest`).
5. **New user:**
   - streak 0;
   - empty heatmap;
   - all questions "Yangi";
   - topics at 0%;
   - the mock empty state with its start button;
   - no division by zero anywhere.

   Test in Task 4 (`newUserGetsEmptyButValidStats`).

---

### Task 1: Activity grid

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/engine/ActivityGrid.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/ActivityGridTest.kt`

**Interfaces:**
- Produces:
  - `object ActivityGrid { const val WEEKS = 18; fun of(sessionDays: List<Long>, today: Long, weeks: Int = WEEKS): List<List<Int?>>; fun level(count: Int): Int }`.
  - `of` returns `weeks` columns, oldest first. Each column holds 7 entries, Monday to Sunday. Each entry is the number of sessions that day, or `null` for a day after `today`.
  - `level` maps a count to 0, 1, 2 or 3, where 3 means 3 or more sessions.

- [ ] **Step 1: Write the failing tests.** Use `wed = LocalDate.of(2026, 10, 7).toEpochDay()` (a Wednesday).
  - `gridHasEighteenWeeksOfSevenDays`: the size is 18 and every column has 7 entries.
  - `todaySitsInTheLastColumnOnItsWeekday`: `of(listOf(wed, wed, wed - 2), wed)`:
    - in the last column, index 2 (Wednesday) is 2 and index 0 (Monday) is 1;
    - indices 3–6 are `null`;
    - every other column is all zeros.
  - `daysOutsideTheWindowAreIgnored`: a day 200 days before `wed` adds nothing. The sum of all counts stays 0.
  - `levelsCapAtThree`: `level(0..4)` gives `[0, 1, 2, 3, 3]`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :core:engine:test --tests "uz.devsuhbat.engine.ActivityGridTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement.**
  - The first Monday is `today - (dow - 1) - 7 * (weeks - 1)`, where `dow = LocalDate.ofEpochDay(today).dayOfWeek.value`.
  - Count the days with `groupingBy { it }.eachCount()`.
- [ ] **Step 4:** Run the same command and expect PASS.
- [ ] **Step 5: Commit** with `git commit -m "feat(engine): add activity grid"`.

### Task 2: Leitner breakdown

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/engine/LeitnerBreakdown.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/LeitnerBreakdownTest.kt`

**Interfaces:**
- Produces:
  - `data class Breakdown(val fresh: Int, val learning: Int, val mastered: Int) { val total: Int }`.
  - `object LeitnerBreakdown { fun of(questions: List<Question>, states: Map<String, QuestionState>): Breakdown }`:
    - no state counts as fresh;
    - `state.mastered` (box ≥ 3) counts as mastered;
    - otherwise learning.

- [ ] **Step 1: Write the failing tests:**
  - `splitsQuestionsIntoThreeGroups`: 5 questions, with states box 1, box 2, box 3 and box 5, and one without a state, give `Breakdown(1, 2, 2)` and total 5.
  - `statesOfOtherQuestionsAreIgnored`: a state for an id not in the list changes nothing.
  - `noQuestionsIsAllZero`: gives `Breakdown(0, 0, 0)`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :core:engine:test --tests "uz.devsuhbat.engine.LeitnerBreakdownTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4:** Run the same command and expect PASS.
- [ ] **Step 5: Commit** with `git commit -m "feat(engine): add Leitner breakdown"`.

### Task 3: Mock history from the log

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/data/AppDatabase.kt` (`ProgressDao.observeRecentSessions`)
- Modify: `app/src/main/java/uz/devsuhbat/data/ProgressRepository.kt` (`mockHistory`)
- Test: `app/src/test/java/uz/devsuhbat/data/ProgressRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `@Query("SELECT * FROM session_log WHERE mode = :mode AND fieldId = :fieldId ORDER BY startedAt DESC LIMIT :limit") fun observeRecentSessions(mode: String, fieldId: String, limit: Int): Flow<List<SessionLogEntity>>`
  - `fun mockHistory(fieldId: String, limit: Int = 10): Flow<List<MockSummary>>`: oldest first, built from `firstTryCorrect`/`total`.

- [ ] **Step 1: Write the failing test** `mockHistoryIsTheFieldsLastTenOldestFirst`:
  - record 12 android mocks with `recordMock` at increasing `startedAt` and `correct = 1..12` (total 25);
  - record one `ios` mock;
  - record one android PRACTICE session.

  `mockHistory("android").first().map { it.correct } == (3..12).toList()`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.data.ProgressRepositoryTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement.** Map with `.reversed()`.
- [ ] **Step 4:** Run the tests and expect PASS.
- [ ] **Step 5: Commit** with `git commit -m "feat(data): expose a field's recent mock history"`.

### Task 4: StatsViewModel

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/stats/StatsViewModel.kt`
- Test: `app/src/test/java/uz/devsuhbat/ui/stats/StatsViewModelTest.kt`

**Interfaces:**
- Consumes: `Streak.of`, `ActivityGrid.of`, `LeitnerBreakdown.of`, `Readiness.of`, `QuestionPicker.eligible`, `QuestionPicker.MOCK_MIN`, `MOCK_SIZE`, and `TopicProgress` (`ui/home/HomeViewModel.kt`).
- Produces:
  - `data class StatsUiState(val loading: Boolean = true, val fieldTitle: String? = null, val level: Level? = null, val streak: StreakInfo = Streak.NONE, val grid: List<List<Int?>> = emptyList(), val breakdown: Breakdown = Breakdown(0, 0, 0), val topics: List<TopicProgress> = emptyList(), val mocks: List<MockSummary> = emptyList(), val mockAvailable: Boolean = false)`.
  - `class StatsViewModel(content: ContentStore, settings: Flow<UserSettings>, states: Flow<Map<String, QuestionState>>, sessionDays: Flow<List<Long>>, today: () -> Long, io: CoroutineDispatcher, mockHistory: (fieldId: String) -> Flow<List<MockSummary>> = { flowOf(emptyList()) }) : ViewModel()` with `val state: StateFlow<StatsUiState>`.

Rules:
- `topics`: the field's topics with at least one eligible question, sorted by percent ascending, with ties going to the larger total. 100% topics are included, at the bottom.
- `breakdown`: over all eligible questions of the field.
- `mockAvailable`: the eligible count is at least `MOCK_MIN`.
- `mocks` follow the current field. The field switch uses `flatMapLatest`, as in `HomeViewModel`.

- [ ] **Step 1: Write the failing tests.** Copy the catalog fixture style from `HomeViewModelTest`: field `android` has topics `a.t` and `a.u`, field `multi` has topic `m.t`.
  - `combinesStreakGridBreakdownAndTopics`: `sessionDays = listOf(today - 1, today)` gives `streak.current == 2`. The grid has 18 columns. The breakdown matches the states. Topics are ordered weakest first.
  - `switchingFieldKeepsActivityButChangesTheRest`: change `settings` to field `multi`. The streak and grid are equal to before. `topics`, `breakdown` and `mocks` now come from `multi`; the `mockHistory` lambda returns per-field lists.
  - `newUserGetsEmptyButValidStats`: no states, no sessions, no mocks gives:
    - `streak.current == 0`;
    - grid sum 0;
    - `breakdown.fresh == breakdown.total`;
    - every topic at 0%;
    - `mocks` empty;
    - `mockAvailable` true when there are at least 5 eligible questions.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.stats.StatsViewModelTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement.** Do the content work inside `withContext(io)`, and use `stateIn(viewModelScope, WhileSubscribed(5_000), StatsUiState())`.
- [ ] **Step 4:** Run the tests and expect PASS. Then run `timeout 600 ./gradlew test` and expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** with `git commit -m "feat(stats): add statistics state"`.

### Task 5: Statistics screen

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/stats/StatsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `StatsViewModel` (Task 4); `SectionCard`, `StatTile`, `DsMotion`, `LocalReducedMotion`, `LocalExtraColors`.
- Produces: `StatsScreen(container: AppContainer, onTopic: (String) -> Unit, onMock: () -> Unit)`.

Layout: a `Scaffold` with no top bar. Inside, a scrolling `Column` (status bar padding, 16dp sides, 12dp gaps) holds a header and four cards.

**Header.** `stats_title` in `headlineMedium`, with the field title and level (`titleRes`) under it in `bodyMedium` / `onSurfaceVariant`.

**1. Activity** (`stats_activity`):
- Two `StatTile`s, `weight(1f)` each:
  - `home_streak_days(current)` with label `stats_streak_current`, in `streakContainer`;
  - `home_streak_days(longest)` with label `stats_streak_longest`, default colours.
- The heatmap is a `Canvas`. Its cell size comes from the width: `cell = (width - 17 × gap) / 18` with `gap = 3dp`, rounded corners of 3dp. Cell colour by `ActivityGrid.level`:

  | Level | Colour |
  |---|---|
  | 0 | `surfaceContainer` |
  | 1 | `primary` at 35% |
  | 2 | `primary` at 65% |
  | 3 | `primary` |

  `null` cells are not drawn. Today's cell gets a 1.5dp `onSurface` outline.
- Under it, a row with `stats_weeks` on the left and the `stats_less` / `stats_more` legend squares on the right.
- The whole heatmap has the semantics description `stats_heatmap_a11y(activeDays)`.

**2. Questions** (`stats_leitner`):
- One 14dp segmented bar with `weight`ed parts:

  | Group | Colour |
  |---|---|
  | fresh | `surfaceContainer` |
  | learning | `streak` |
  | mastered | `success` |

  A part with 0 questions is not drawn.
- Under it, three legend rows, each a dot, the label (`stats_fresh`, `stats_learning`, `stats_mastered`) and the count. When `total == 0`, show only `topics_no_content`.

**3. Topics** (`stats_topics`): every topic as a clickable row (`onTopic`) with its title, percent and an 8dp bar, the same row as Home's weak topics. Extract `WeakTopicRow` from `HomeScreen.kt` into `ui/home/TopicProgressRow.kt` as `internal fun TopicProgressRow(topic: TopicProgress, onClick: () -> Unit)` and use it in both places.

**4. Mock history** (`stats_mock`):
- **When there are mocks:** a 160dp `Canvas` bar chart, one bar per mock, oldest on the left.
  - Each bar's height is its percent of the available height.
  - Bars are `primary` and the newest is `success`.
  - Each bar is at most 28dp wide, with an 8dp gap and a 6dp top radius.
  - The percent is drawn above each bar in `labelSmall` with `rememberTextMeasurer`.
  - The bars grow from 0 with `spatialDefault` once.
  - Semantics `stats_mock_a11y(count, lastPercent)`.
- **When there are none:** `stats_mock_empty` and an `ExpressiveButton(stats_mock_start, enabled = mockAvailable, onClick = onMock)`.

Strings to add:

| Key | Text |
|---|---|
| `nav_stats` | Statistika |
| `stats_title` | Statistika |
| `stats_activity` | Faollik |
| `stats_streak_current` | joriy seriya |
| `stats_streak_longest` | eng uzun seriya |
| `stats_weeks` | So\'nggi 18 hafta |
| `stats_less` | kam |
| `stats_more` | ko\'p |
| `stats_heatmap_a11y` | So\'nggi 18 haftada %1$d kun mashq qilingan |
| `stats_leitner` | Savollar holati |
| `stats_fresh` | Yangi |
| `stats_learning` | O\'rganilmoqda |
| `stats_mastered` | O\'zlashtirilgan |
| `stats_topics` | Mavzular |
| `stats_mock` | Mock tarixi |
| `stats_mock_empty` | Hali mock interview topshirilmagan |
| `stats_mock_start` | Mock\'ni boshlash |
| `stats_mock_a11y` | Oxirgi %1$d ta mock, so\'nggisi %2$d%% |

- [ ] **Step 1: Implement** the screen, the shared row and the strings.
- [ ] **Step 2:** Run `timeout 700 ./gradlew test :app:assembleDebug` and expect BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** with `git commit -m "feat(stats): add the statistics screen"`.

### Task 6: Statistika tab

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/ui/Nav.kt`:
  - add `Routes.STATS = "stats"`;
  - add the third `Tab` with `Icons.Rounded.BarChart` and `nav_stats`;
  - add `composable(Routes.STATS) { BarRoom(Routes.STATS, barHeight) { StatsScreen(...) } }`, where `onTopic` navigates to `Routes.session(id)` and `onMock` to `Routes.MOCK`.
- Modify: `app/src/main/java/uz/devsuhbat/ui/NavMotion.kt`: `TOP_LEVEL_ROUTES = listOf(HOME, TOPICS, STATS)`.
- Test: `app/src/test/java/uz/devsuhbat/ui/NavMotionTest.kt`

- [ ] **Step 1: Write the failing test** `statsIsATab`:
  - `isTopLevel(Routes.STATS)`;
  - `navMotion(Routes.STATS, Routes.HOME) == FADE`;
  - `contentBottomPadding(Routes.STATS, 80.dp) == 80.dp`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.NavMotionTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4:** Run `timeout 900 ./gradlew test :app:assembleRelease` and expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** with `git commit -m "feat(nav): add the statistics tab"`.

### Task 7: Verify on device and open PR

- [ ] **Step 1:** Take the lock and install. In light and dark theme, check:
  - the Statistika tab: current/longest streak, heatmap (today outlined, future days empty), Leitner bar and legend, topics weakest first;
  - a topic row opens that topic;
  - mock history bars, if the emulator has mocks. If not, the empty state, and its button opens the mock.

  Switch tabs Bosh ↔ Mavzular ↔ Statistika: the bar highlights the right tab and each tab keeps its scroll position.
- [ ] **Step 2:** At 360 dp (`wm density 480`) with font 1.3, the heatmap fits the width and no text is clipped. With animator 0, the bars appear at once. Restore every setting and the app theme, delete the lock and message the peer.
- [ ] **Step 3:** Push `stage6/6d-stats` and open a PR to `master`. The Uzbek body lists what changed, the test counts and what was checked. End with the Claude Code line, then wait for the user to merge.
