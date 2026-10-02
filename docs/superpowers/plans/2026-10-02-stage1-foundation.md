# Stage 1: Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A buildable Android app where the user picks a field and a level, opens a topic and works through a 10-question practice session with hint-only feedback on wrong answers and an in-session repeat of missed questions.

**Architecture:** Two Gradle modules. `:core:engine` is pure Kotlin/JVM and holds the content model, parser, validator, grading and the practice session queue, all unit-tested. `:app` is the Compose UI, reads content JSON from assets through `ContentStore`, and keeps the chosen field and level in DataStore. No Room in this stage; progress persistence is Stage 2.

**Tech Stack:** Kotlin 2.4.20, AGP 9.4.1, Gradle 9.8.0, Compose BOM 2026.09.00 + Material 3, Navigation Compose 2.10.2, DataStore Preferences 1.2.1, kotlinx.serialization 1.11.0, coroutines 1.11.0, JUnit 4.13.2, kotlinx-coroutines-test. JDK 21.

**Spec:** `docs/superpowers/specs/2026-10-02-devsuhbat-design.md` (sections 2, 3, 4.1–4.3, 5.1, 6, 7, 8). Topic list: `docs/content/taxonomy.md`.

## Global Constraints

- Package and applicationId: `uz.devsuhbat`. App name: `DevSuhbat`. `minSdk 26`, `targetSdk 37`, `compileSdk 37`.
- No `INTERNET` permission. No backend, account, ads or analytics.
- UI text and content are Uzbek (Latin); technical terms and code stay English. All UI text lives in `res/values/strings.xml`.
- A wrong answer never reveals which option is correct: no marking, no colour, no text.
- Question ids are stable: `<topic id>.<3 digits>`.
- Level ids: `junior`, `middle`, `strong_middle`, `senior`. A chosen level includes all lower levels.
- Practice session size: 10 questions.
- Questions are written fresh from `taxonomy.md`, never copied from other sites; every question ships with `"reviewed": false`.
- Manual DI through `AppContainer`; no Hilt.
- Commits carry no AI co-author line.

## Review Focus

1. A prompt, option or hint with an unpaired backtick must render as literal text, not crash or swallow the rest of the line. Test in Task 6 (`InlineTextTest`).
2. A topic file that is missing or has broken JSON must leave the app running with that topic shown as unavailable. Test in Task 6 (`ContentStoreTest`).
3. A topic with fewer than 10 eligible questions must give a session of the available size; a topic with none must not be startable. Tests in Task 5 (`QuestionPickerTest`) and Task 9 (topic row disabled).
4. Submitting with nothing selected, or with an already eliminated option, must be impossible from the UI and rejected by the engine. Test in Task 4 (`QuestionAttemptTest`).
5. A hint that quotes the correct option verbatim leaks the answer. Test in Task 3 (`ContentValidatorTest`) and enforced on real assets in Task 7.

---

## File Structure

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew(.bat), gradle/wrapper/*
core/engine/build.gradle.kts
core/engine/src/main/kotlin/uz/devsuhbat/content/
    Model.kt             Level, QuestionType, QuestionKind, Option, Question, TopicFile, Field, Topic, Catalog
    ContentParser.kt     JSON text -> model
    ContentValidator.kt  spec 3.3 rules -> List<String>
    ContentStore.kt      lazy cached access to catalog and topic files through a read lambda
    InlineText.kt        backtick segments
core/engine/src/main/kotlin/uz/devsuhbat/engine/
    Grader.kt            Verdict, Grader
    QuestionAttempt.kt   attempts on one question
    PracticeSession.kt   queue with in-session repeat, SessionResult, QuestionOutcome
    QuestionPicker.kt    eligible questions for a level, practice pick
core/engine/src/test/kotlin/uz/devsuhbat/...   one test class per unit
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/assets/content/catalog.json
app/src/main/assets/content/questions/android_*.json   (6 files)
app/src/main/java/uz/devsuhbat/
    DevSuhbatApp.kt, AppContainer.kt, MainActivity.kt
    data/SettingsRepository.kt
    ui/Nav.kt
    ui/theme/Theme.kt
    ui/common/InlineCodeText.kt, CodeBlock.kt
    ui/onboarding/OnboardingScreen.kt, OnboardingViewModel.kt
    ui/home/HomeScreen.kt, HomeViewModel.kt
    ui/topics/TopicsScreen.kt, TopicsViewModel.kt
    ui/session/SessionScreen.kt, SessionViewModel.kt, SessionResultScreen.kt
app/src/main/res/values/strings.xml, themes.xml, colors.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml, drawable/ic_launcher_foreground.xml
app/src/test/java/uz/devsuhbat/content/ContentAssetsTest.kt
app/src/test/java/uz/devsuhbat/ui/session/SessionViewModelTest.kt
```

---

### Task 1: Project scaffold

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `core/engine/build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/{strings,themes,colors}.xml`, launcher icon files, `app/src/main/java/uz/devsuhbat/MainActivity.kt`
- Copy from `D:\projects\hangul-friend`: `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `local.properties` (git-ignored)

**Interfaces:**
- Produces: modules `:app` and `:core:engine`; version catalog aliases identical to Hangul Friend's (`libs.androidx.core.ktx`, `libs.compose.bom`, `libs.kotlinx.serialization.json`, ...), minus `media3`, `markdown`, `work`, `room` (Room returns in Stage 2).

- [ ] **Step 1:** Write the Gradle files. `:core:engine` applies `kotlin-jvm` and `kotlin-serialization`, `jvmToolchain(21)`, depends on `kotlinx-serialization-json`, tests on `junit`. `:app` applies `android-application`, `kotlin-compose`, `kotlin-serialization`; Java 21; `buildFeatures.compose = true`; `testOptions.unitTests.isIncludeAndroidResources = true`; depends on `:core:engine`.
- [ ] **Step 2:** Write the manifest (one exported `MainActivity`, `android:allowBackup="true"`, no permissions), an adaptive launcher icon (vector foreground, solid background), and a `MainActivity` that shows the text "DevSuhbat" inside `MaterialTheme`.
- [ ] **Step 3:** Run `.\gradlew.bat :core:engine:test :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 4:** Commit: `chore: scaffold app and core:engine modules`.

### Task 2: Content model and parser

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/content/Model.kt`, `ContentParser.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/content/ContentParserTest.kt`

**Interfaces:**
- Produces:

```kotlin
@Serializable enum class Level { JUNIOR, MIDDLE, STRONG_MIDDLE, SENIOR }   // @SerialName junior, middle, strong_middle, senior; ordinal = seniority
@Serializable enum class QuestionType { SINGLE, MULTI }                    // @SerialName single, multi
@Serializable enum class QuestionKind { CONCEPT, TRUE_FALSE, CODE_OUTPUT, CODE_REVIEW }  // @SerialName concept, true_false, code_output, code_review
@Serializable data class Option(val id: String, val text: String, val correct: Boolean = false, val hint: String? = null)
@Serializable data class Question(
    val id: String, val level: Level, val type: QuestionType, val kind: QuestionKind = QuestionKind.CONCEPT,
    val prompt: String, val code: String? = null, val options: List<Option>,
    val explanation: String, val reviewed: Boolean = false,
)
@Serializable data class TopicFile(val topic: String, val questions: List<Question>)
@Serializable data class Field(val id: String, val title: String, val group: String, val topics: List<String>)
@Serializable data class Topic(val id: String, val title: String, val file: String? = null)   // file == null: planned, no content yet
@Serializable data class Catalog(val version: Int, val fields: List<Field>, val topics: List<Topic>)

object ContentParser {
    fun parseCatalog(json: String): Catalog        // throws SerializationException on bad input
    fun parseTopicFile(json: String): TopicFile
}
```

`Json { ignoreUnknownKeys = true }`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun parsesTopicFile() {
    val file = ContentParser.parseTopicFile("""
        {"topic":"android.kotlin","questions":[{"id":"android.kotlin.001","level":"strong_middle","type":"single",
        "kind":"code_output","prompt":"p","code":"println(1)","options":[{"id":"a","text":"1","correct":true},
        {"id":"b","text":"2","hint":"h"}],"explanation":"e"}]}""")
    val q = file.questions.single()
    assertEquals(Level.STRONG_MIDDLE, q.level); assertEquals(QuestionType.SINGLE, q.type)
    assertEquals(QuestionKind.CODE_OUTPUT, q.kind); assertEquals("println(1)", q.code)
    assertTrue(q.options[0].correct); assertNull(q.options[0].hint)
    assertFalse(q.options[1].correct); assertEquals("h", q.options[1].hint)
    assertFalse(q.reviewed)
}
@Test fun kindDefaultsToConceptAndCodeToNull()          // question JSON without "kind" and "code"
@Test fun parsesCatalogWithPlannedTopic()               // {"id":"ios.swift","title":"Swift tili"} -> file == null
@Test fun levelsAreOrderedBySeniority()                 // JUNIOR < MIDDLE < STRONG_MIDDLE < SENIOR
@Test(expected = SerializationException::class) fun unknownLevelFails()   // "level":"lead"
```

- [ ] **Step 2:** Run `.\gradlew.bat :core:engine:test`. Expected: compilation failure (unresolved `ContentParser`).
- [ ] **Step 3:** Implement `Model.kt` and `ContentParser.kt`.
- [ ] **Step 4:** Run `.\gradlew.bat :core:engine:test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(engine): content model and parser`.

### Task 3: ContentValidator

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/content/ContentValidator.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/content/ContentValidatorTest.kt`

**Interfaces:**
- Consumes: Task 2 model.
- Produces: `object ContentValidator { fun validate(catalog: Catalog, files: Map<String, TopicFile>): List<String> }` — `files` is keyed by `Topic.file`; an empty result means valid. Every message starts with the offending id or file name.

Rules (spec 3.3):
1. A topic with `file != null` must have an entry in `files`, and that entry's `topic` must equal the topic id. Topic ids are unique. A field may only reference existing topic ids. Field ids are unique.
2. Question ids are unique across all files and match `^<topic id>\.\d{3}$`.
3. `SINGLE`: exactly 1 correct option, 2 to 5 options. `MULTI`: at least 2 correct and at least 1 wrong option.
4. Every wrong option has a non-blank `hint`; a correct option has `hint == null`.
5. No hint contains the text of a correct option of the same question (case-insensitive, checked only when that text is longer than 4 characters).
6. `prompt` and `explanation` are non-blank; option ids are unique within the question; option texts are non-blank and unique within the question.
7. `CODE_OUTPUT` and `CODE_REVIEW` questions have non-blank `code`. `TRUE_FALSE` questions are `SINGLE` with exactly 2 options.

- [ ] **Step 1: Write the failing tests.** A `valid()` helper builds a one-field, one-topic, one-question catalog that passes; each test mutates one thing and asserts exactly one error mentioning the id:

```kotlin
@Test fun validContentHasNoErrors()
@Test fun missingTopicFileIsReported()
@Test fun plannedTopicWithoutFileIsAllowed()
@Test fun topicFileForOtherTopicIsReported()
@Test fun fieldReferencingUnknownTopicIsReported()
@Test fun duplicateQuestionIdIsReported()
@Test fun questionIdMustMatchTopicPrefixAndThreeDigits()      // "android.kotlin.1", "core.dsa.001" in android.kotlin
@Test fun singleNeedsExactlyOneCorrect()                      // zero correct; two correct
@Test fun singleNeedsTwoToFiveOptions()                       // one option; six options
@Test fun multiNeedsTwoCorrectAndOneWrong()
@Test fun wrongOptionNeedsHint()                              // null and blank
@Test fun correctOptionMustNotHaveHint()
@Test fun hintMustNotQuoteCorrectOption()                     // correct "Immutable reference", hint "Bu yerda immutable reference kerak" -> error
@Test fun shortCorrectTextIsNotCheckedForLeak()               // correct "Ha", hint "Ha deb o'ylash xato" -> no error
@Test fun blankPromptOrExplanationIsReported()
@Test fun duplicateOptionIdOrTextIsReported()
@Test fun codeKindsNeedCode()
@Test fun trueFalseNeedsSingleWithTwoOptions()
```

- [ ] **Step 2:** Run `.\gradlew.bat :core:engine:test`. Expected: compilation failure.
- [ ] **Step 3:** Implement `ContentValidator.validate`.
- [ ] **Step 4:** Run `.\gradlew.bat :core:engine:test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(engine): content validator`.

### Task 4: Grader and QuestionAttempt

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/engine/Grader.kt`, `QuestionAttempt.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/GraderTest.kt`, `QuestionAttemptTest.kt`

**Interfaces:**
- Consumes: Task 2 model.
- Produces:

```kotlin
sealed interface Verdict {
    data object Correct : Verdict
    /** wrongPicked: the selected wrong options (each carries its hint). correctPicked / correctMissing: counts only, never ids. */
    data class Wrong(val wrongPicked: List<Option>, val correctPicked: Int, val correctMissing: Int) : Verdict
}
object Grader { fun grade(question: Question, selected: Set<String>): Verdict }

data class QuestionAttempt(
    val question: Question,
    val disabled: Set<String> = emptySet(),   // eliminated wrong option ids
    val wrongSubmissions: Int = 0,
    val solved: Boolean = false,
) {
    val firstTryCorrect: Boolean              // solved && wrongSubmissions == 0
    fun canSubmit(selected: Set<String>): Boolean
    fun submit(selected: Set<String>): Pair<QuestionAttempt, Verdict>   // throws IllegalArgumentException when !canSubmit(selected)
}
```

`canSubmit` is true when the attempt is not solved, `selected` is non-empty, contains only option ids of the question, none of them disabled, and for `SINGLE` exactly one id. A wrong submission adds the wrong picked ids to `disabled` and increments `wrongSubmissions`. A `MULTI` submission that picks only correct options but misses some is `Wrong(emptyList(), n, m)` and still counts as a wrong submission.

- [ ] **Step 1: Write the failing tests**

```kotlin
// GraderTest — single: options a(correct) b c d; multi: a(correct) b(correct) c d
@Test fun singleCorrect()            // grade(single, setOf("a")) == Verdict.Correct
@Test fun singleWrongCarriesHint()   // grade(single, setOf("c")) == Wrong(listOf(optionC), 0, 1)
@Test fun multiAllCorrect()          // setOf("a","b") -> Correct
@Test fun multiPartial()             // setOf("a") -> Wrong(emptyList(), 1, 1)
@Test fun multiWithWrong()           // setOf("a","c") -> Wrong(listOf(optionC), 1, 1)
@Test fun multiOnlyWrong()           // setOf("c","d") -> Wrong(listOf(optionC, optionD), 0, 2)

// QuestionAttemptTest
@Test fun firstTryCorrect()                      // submit a -> solved, firstTryCorrect, wrongSubmissions 0
@Test fun wrongThenCorrectIsNotFirstTry()        // submit c: disabled == {c}, wrongSubmissions 1, !solved; submit a: solved, !firstTryCorrect
@Test fun cannotSubmitEmptySelection()
@Test fun cannotSubmitDisabledOption()
@Test fun cannotSubmitUnknownOption()
@Test fun cannotSubmitTwoOptionsForSingle()
@Test fun cannotSubmitAfterSolved()
@Test(expected = IllegalArgumentException::class) fun submitThrowsWhenNotAllowed()
@Test fun multiPartialDisablesNothingButCountsAsWrong()   // submit {a}: disabled empty, wrongSubmissions 1
@Test fun multiWrongOptionsAreDisabled()                  // submit {a,c}: disabled == {c}
```

- [ ] **Step 2:** Run `.\gradlew.bat :core:engine:test`. Expected: compilation failure.
- [ ] **Step 3:** Implement `Grader` and `QuestionAttempt`.
- [ ] **Step 4:** Run `.\gradlew.bat :core:engine:test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(engine): grading and per-question attempts`.

### Task 5: PracticeSession and QuestionPicker

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/engine/PracticeSession.kt`, `QuestionPicker.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/PracticeSessionTest.kt`, `QuestionPickerTest.kt`

**Interfaces:**
- Consumes: `QuestionAttempt`, `Verdict` (Task 4).
- Produces:

```kotlin
data class QuestionOutcome(val questionId: String, val firstTryCorrect: Boolean, val wrongSubmissions: Int)
data class SessionResult(val total: Int, val firstTryCorrect: Int, val reworked: Int)   // reworked = total - firstTryCorrect

class PracticeSession(questions: List<Question>, private val random: Random) {
    val total: Int                      // number of distinct questions
    val queueSize: Int                  // total + repeats appended so far
    val position: Int                   // 0-based index of the current item in the queue
    val attempt: QuestionAttempt?       // current item, options already shuffled; null when finished
    val isRepeat: Boolean               // current item is an in-session repeat
    val finished: Boolean
    val outcomes: List<QuestionOutcome> // one per distinct question, appended when its first presentation is solved
    fun submit(selected: Set<String>): Verdict     // delegates to the current attempt
    fun next()                                     // requires the current attempt to be solved
    fun result(): SessionResult                    // requires finished
}

object QuestionPicker {
    const val PRACTICE_SIZE = 10
    fun eligible(pool: List<Question>, maxLevel: Level): List<Question>           // level <= maxLevel, order kept
    fun practice(pool: List<Question>, maxLevel: Level, random: Random, count: Int = PRACTICE_SIZE): List<Question>
}
```

Behaviour: options are shuffled with `random` at every presentation, except for `TRUE_FALSE` questions, which keep file order. When the first presentation of a question is solved with `wrongSubmissions > 0`, `next()` appends one repeat of it to the end of the queue. A repeat never appends another repeat and adds no outcome. `practice` returns `min(count, eligible.size)` distinct questions in random order.

- [ ] **Step 1: Write the failing tests**

```kotlin
// PracticeSessionTest — three single questions q1..q3, correct option "a"; Random(1)
@Test fun allFirstTryFinishesWithoutRepeats()    // queueSize stays 3; result == SessionResult(3, 3, 0)
@Test fun missedQuestionIsRepeatedAtTheEnd()     // q1: wrong then correct; after q3 the 4th item is q1 with isRepeat; queueSize 4
@Test fun repeatIsNotRepeatedAgain()             // miss the repeat too: finished after it, queueSize 4
@Test fun outcomesRecordFirstPresentationOnly()  // outcomes == [(q1,false,1),(q2,true,0),(q3,true,0)] even after the repeat
@Test fun resultCountsFirstTryCorrect()          // SessionResult(3, 2, 1)
@Test fun optionsAreShuffledButComplete()        // attempt.question.options.map{id}.toSet() == setOf("a","b","c","d")
@Test fun trueFalseKeepsOptionOrder()
@Test(expected = IllegalStateException::class) fun nextBeforeSolvedThrows()
@Test fun emptySessionIsFinishedImmediately()    // PracticeSession(emptyList(), r).finished; result == SessionResult(0,0,0)

// QuestionPickerTest — pool: 4 junior, 3 middle, 2 strong_middle, 1 senior
@Test fun eligibleIncludesLowerLevels()          // MIDDLE -> 7, JUNIOR -> 4, SENIOR -> 10
@Test fun practiceReturnsDistinctQuestionsUpToCount()   // SENIOR, count 5 -> 5 distinct ids
@Test fun practiceReturnsAllWhenPoolIsSmall()    // JUNIOR, count 10 -> 4
@Test fun practiceOfEmptyPoolIsEmpty()
```

- [ ] **Step 2:** Run `.\gradlew.bat :core:engine:test`. Expected: compilation failure.
- [ ] **Step 3:** Implement `PracticeSession` and `QuestionPicker`.
- [ ] **Step 4:** Run `.\gradlew.bat :core:engine:test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(engine): practice session queue and question picker`.

### Task 6: ContentStore and InlineText

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/content/ContentStore.kt`, `InlineText.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/content/ContentStoreTest.kt`, `InlineTextTest.kt`

**Interfaces:**
- Consumes: `ContentParser` (Task 2).
- Produces:

```kotlin
/** read(path) returns the file text, or null when the file does not exist. Paths: "content/catalog.json", "content/questions/<file>". Thread-safe; results are cached. */
class ContentStore(private val read: (path: String) -> String?) {
    fun catalog(): Catalog?                           // null when missing or broken
    fun field(fieldId: String): Field?
    fun topicsOf(fieldId: String): List<Topic>        // in the field's order; unknown ids skipped
    fun questions(topicId: String): List<Question>    // empty when the topic is planned, missing or broken
}

data class InlineSegment(val text: String, val code: Boolean)
object InlineText { fun parse(text: String): List<InlineSegment> }
```

`InlineText.parse`: text between a pair of backticks becomes a `code = true` segment without the backticks. An unpaired backtick and everything after it stays literal. Empty segments are dropped.

- [ ] **Step 1: Write the failing tests**

```kotlin
// ContentStoreTest — backed by a mutableMapOf<String, String>() and a read counter
@Test fun readsCatalogAndQuestions()
@Test fun missingCatalogGivesNull()
@Test fun brokenCatalogGivesNull()                    // "{not json"
@Test fun plannedTopicHasNoQuestions()                // file == null -> emptyList, read not called for it
@Test fun missingTopicFileGivesEmptyList()
@Test fun brokenTopicFileGivesEmptyListAndOthersStillLoad()
@Test fun resultsAreCached()                          // two calls, one read per path
@Test fun topicsOfKeepsFieldOrderAndSkipsUnknown()

// InlineTextTest
@Test fun plainText()            // parse("abc") == [InlineSegment("abc", false)]
@Test fun codeInTheMiddle()      // parse("a `val` b") == [("a ", false), ("val", true), (" b", false)]
@Test fun codeAtStartAndEnd()    // parse("`a` va `b`") == [("a", true), (" va ", false), ("b", true)]
@Test fun unpairedBacktickIsLiteral()   // parse("a `b") == [("a `b", false)]
@Test fun pairThenUnpaired()     // parse("`a` b `c") == [("a", true), (" b `c", false)]
@Test fun emptyString()          // parse("") == emptyList()
@Test fun emptyCodeIsDropped()   // parse("a``b") == [("a", false), ("b", false)] or one merged plain segment "ab"; assert joined text == "ab" and no code segment
```

- [ ] **Step 2:** Run `.\gradlew.bat :core:engine:test`. Expected: compilation failure.
- [ ] **Step 3:** Implement `ContentStore` and `InlineText`.
- [ ] **Step 4:** Run `.\gradlew.bat :core:engine:test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(engine): content store and inline code parsing`.

### Task 7: Catalog and Android sample content

**Files:**
- Create: `app/src/main/assets/content/catalog.json`, `app/src/main/assets/content/questions/android_kotlin.json`, `android_components.json`, `android_compose.json`, `android_async.json`, `android_data.json`, `android_arch.json`
- Test: `app/src/test/java/uz/devsuhbat/content/ContentAssetsTest.kt`

**Interfaces:**
- Consumes: `ContentParser`, `ContentValidator`, `ContentStore`.
- Produces: a catalog with all 13 fields and all 88 topics from `taxonomy.md` (78 field topics + 10 `core.*` blocks) with the exact ids and Uzbek titles given there. Only the 6 `android.*` topics have a `file`; the rest are planned.

Content: 6 questions per Android topic (36 total), levels per file `junior ×2, middle ×2, strong_middle ×1, senior ×1`, taken from taxonomy section 4.1. Across the 36: at least 4 `multi`, at least 6 `code_output`, at least 3 `code_review`, at least 2 `true_false`. Ids `android.<topic>.001` to `.006`.

- [ ] **Step 1: Write the failing test.** A plain JVM test that reads from `src/main/assets` relative to the module directory (Gradle runs unit tests with the module as working directory):

```kotlin
private val assets = File("src/main/assets")
private val store = ContentStore { path -> File(assets, path).takeIf { it.isFile }?.readText() }

@Test fun catalogParses()                         // store.catalog() != null
@Test fun allContentPassesValidator()             // parse every file named in the catalog; ContentValidator.validate(...) == emptyList(), message joins errors
@Test fun catalogHasThirteenFieldsAndEightyEightTopics()
@Test fun everyQuestionFileIsReferencedByTheCatalog()   // no orphan files in content/questions
@Test fun androidFieldHasQuestionsOnEveryLevel()        // QuestionPicker.eligible(all android questions, JUNIOR).size >= 10
```

- [ ] **Step 2:** Run `.\gradlew.bat :app:testDebugUnitTest --tests "uz.devsuhbat.content.ContentAssetsTest"`. Expected: FAIL (catalog missing).
- [ ] **Step 3:** Write `catalog.json`.
- [ ] **Step 4:** Write the six question files.
- [ ] **Step 5:** Run the test again. Expected: PASS.
- [ ] **Step 6:** Commit: `feat(content): catalog and Android sample questions`.

### Task 8: Settings, container, theme, navigation, onboarding

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/DevSuhbatApp.kt`, `AppContainer.kt`, `data/SettingsRepository.kt`, `ui/Nav.kt`, `ui/theme/Theme.kt`, `ui/onboarding/OnboardingScreen.kt`, `ui/onboarding/OnboardingViewModel.kt`
- Modify: `MainActivity.kt`, `AndroidManifest.xml` (`android:name=".DevSuhbatApp"`), `res/values/strings.xml`

**Interfaces:**
- Consumes: `ContentStore`, `Level`, `Field`.
- Produces:

```kotlin
enum class ThemeMode { SYSTEM, LIGHT, DARK }
data class UserSettings(val fieldId: String?, val level: Level?, val onboardingDone: Boolean, val theme: ThemeMode)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<UserSettings>
    suspend fun completeOnboarding(fieldId: String, level: Level)
    suspend fun setTheme(theme: ThemeMode)
}

class AppContainer(context: Context) {
    val content: ContentStore        // read lambda opens context.assets, returns null on IOException
    val settings: SettingsRepository // DataStore name "settings"
    val io: CoroutineDispatcher      // Dispatchers.IO
}

object Routes { const val ONBOARDING = "onboarding"; const val HOME = "home"; const val TOPICS = "topics"
    const val SESSION = "session/{topicId}"; fun session(topicId: String) = "session/$topicId"
    const val MIXED = "mixed"; const val SETTINGS = "settings" }
```

DataStore keys: `field` (string), `level` (string, the serial name), `onboarding_done` (boolean), `theme` (string, enum name). An unknown stored level or theme falls back to `null` / `SYSTEM`.

Onboarding: step 1 lists fields grouped by `group` in catalog order with group headers (`mobile` → "Mobile", `frontend` → "Frontend", `backend` → "Backend", `other` → "Boshqa"). A field whose topics have no questions at all is shown disabled with the label "Tez orada". Step 2 lists the four levels with the experience range and description from spec 2.2. "Boshlash" calls `completeOnboarding` and navigates to Home, clearing the back stack. The same screen is reused from Settings (Task 9) to change field and level.

`MainActivity` collects `settings`, shows nothing until the first value arrives, then starts at `ONBOARDING` or `HOME`. Theme: Material 3 light and dark colour schemes from a fixed brand palette (primary indigo `#3F51B5` light / `#B4BEFF` dark), no dynamic colour; `ThemeMode` picks the scheme.

- [ ] **Step 1:** Implement `SettingsRepository`, `AppContainer`, `DevSuhbatApp`, theme.
- [ ] **Step 2:** Implement `OnboardingViewModel` (state: fields with `available` flag, selected field, selected level, step) and `OnboardingScreen`.
- [ ] **Step 3:** Implement `Nav.kt` with placeholders for routes built in later tasks, and wire `MainActivity`.
- [ ] **Step 4:** Run `.\gradlew.bat :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 5:** Commit: `feat(app): settings, navigation and onboarding`.

### Task 9: Home, topics and settings screens

**Files:**
- Create: `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt`, `ui/topics/TopicsScreen.kt`, `ui/topics/TopicsViewModel.kt`, `ui/settings/SettingsScreen.kt`
- Modify: `ui/Nav.kt`, `res/values/strings.xml`

**Interfaces:**
- Consumes: `AppContainer`, `QuestionPicker.eligible`, `Routes`.
- Produces: `data class TopicRow(val topic: Topic, val questionCount: Int)` where `questionCount` is the number of questions eligible for the user's level; `TopicsViewModel.state: StateFlow<TopicsUiState(loading: Boolean, rows: List<TopicRow>, mixedCount: Int)>`.

Home: field title, level name, a primary "Mashq" button (opens Topics) and a settings icon. Topics: an "Aralash" row first (all eligible questions of the field; disabled when `mixedCount == 0`), then one row per topic with its count; a row with `questionCount == 0` is disabled and labelled "Tez orada". Tapping a row navigates to `Routes.session(topic.id)` or `Routes.session(Routes.MIXED)`. Settings: "Yo'nalish va daraja" (opens onboarding in edit mode) and theme choice (Tizim / Yorug' / Qorong'i). Content is loaded on `container.io`.

- [ ] **Step 1:** Implement the view models and screens, replace the placeholders in `Nav.kt`.
- [ ] **Step 2:** Run `.\gradlew.bat :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 3:** Commit: `feat(app): home, topics and settings screens`.

### Task 10: Practice session screen

**Files:**
- Create: `ui/session/SessionViewModel.kt`, `ui/session/SessionScreen.kt`, `ui/session/SessionResultScreen.kt`, `ui/common/InlineCodeText.kt`, `ui/common/CodeBlock.kt`
- Modify: `ui/Nav.kt`, `res/values/strings.xml`
- Test: `app/src/test/java/uz/devsuhbat/ui/session/SessionViewModelTest.kt`

**Interfaces:**
- Consumes: `PracticeSession`, `QuestionPicker.practice`, `Verdict`, `InlineText`, `AppContainer`.
- Produces:

```kotlin
sealed interface Feedback {
    /** hints: (option text, hint) for each wrong option just picked. correctPicked / correctMissing are null for SINGLE questions. */
    data class Wrong(val hints: List<Pair<String, String>>, val correctPicked: Int?, val correctMissing: Int?) : Feedback
    data class Correct(val explanation: String) : Feedback
}
data class SessionUiState(
    val loading: Boolean = true,
    val question: Question? = null,          // options in presentation order
    val isRepeat: Boolean = false,
    val position: Int = 0, val queueSize: Int = 0,
    val selected: Set<String> = emptySet(),
    val disabled: Set<String> = emptySet(),
    val feedback: Feedback? = null,
    val solved: Boolean = false,
    val canCheck: Boolean = false,
    val result: SessionResult? = null,       // non-null when the session is finished
)
class SessionViewModel(private val loadQuestions: suspend () -> List<Question>, private val random: Random) : ViewModel() {
    val state: StateFlow<SessionUiState>
    fun toggle(optionId: String)   // SINGLE: replaces the selection; MULTI: toggles; ignored for disabled options and after solved
    fun check()                    // no-op unless canCheck
    fun next()                     // no-op unless solved
}
```

The factory builds `loadQuestions` from the container: read settings, load the topic's questions (or all field topics for `MIXED`) on `io`, then `QuestionPicker.practice(pool, level, random)`.

Screen: top bar with a close button (asks "Sessiyadan chiqasizmi?" before leaving, also on system back) and a linear progress indicator `position / queueSize`; a "Takror" chip when `isRepeat`; prompt via `InlineCodeText`; `CodeBlock` when `code != null` (monospace, horizontal scroll, surface-variant background); options as selectable cards (radio look for `SINGLE`, checkbox look for `MULTI`, with a "Bir nechta javob to'g'ri" caption); eliminated options are greyed out, struck through and not clickable. A wrong verdict shows an error-coloured panel with each hint and, for `MULTI`, the line "Tanlaganlaringizdan %1$d tasi to'g'ri, yana %2$d ta to'g'ri variant tanlanmagan." After a wrong verdict the selection is cleared of eliminated options. A correct verdict shows a success-coloured panel with the explanation and turns the bottom button from "Tekshirish" into "Keyingi". Nothing on screen marks a correct option before the question is solved. When `result != null` the result screen shows total, first-try-correct and reworked counts with "Yana mashq" (back to Topics) and "Bosh sahifa".

- [ ] **Step 1: Write the failing tests** (`Dispatchers.setMain(UnconfinedTestDispatcher())`, three single questions with correct option "a", one multi question with correct "a","b"):

```kotlin
@Test fun loadsFirstQuestion()                    // !loading, question != null, queueSize 3, !canCheck
@Test fun selectingEnablesCheck()
@Test fun wrongAnswerShowsHintAndDisablesOption() // toggle("c"); check(): feedback is Wrong with the hint of c, disabled == {c}, selected empty, !solved
@Test fun wrongFeedbackNeverNamesTheCorrectOption() // Wrong.hints texts do not contain the correct option's text
@Test fun correctAnswerShowsExplanation()         // feedback == Correct(explanation), solved
@Test fun nextAdvancesAndClearsFeedback()
@Test fun multiPartialReportsCounts()             // toggle a; check(): Wrong(emptyList(), 1, 1), selected stays {a}
@Test fun finishingProducesResult()               // all correct: result == SessionResult(3, 3, 0), question == null
@Test fun missedQuestionComesBackAsRepeat()       // isRepeat true on the 4th item
@Test fun emptyPoolFinishesImmediately()          // result == SessionResult(0, 0, 0)
@Test fun toggleIgnoredForDisabledOption()
```

- [ ] **Step 2:** Run `.\gradlew.bat :app:testDebugUnitTest --tests "uz.devsuhbat.ui.session.SessionViewModelTest"`. Expected: compilation failure.
- [ ] **Step 3:** Implement `SessionViewModel`.
- [ ] **Step 4:** Run the test again. Expected: PASS.
- [ ] **Step 5:** Implement `InlineCodeText`, `CodeBlock`, `SessionScreen`, `SessionResultScreen`; wire the route.
- [ ] **Step 6:** Run `.\gradlew.bat test :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`, all tests pass.
- [ ] **Step 7:** Commit: `feat(app): practice session with hint-only feedback`.

### Task 11: Verification

- [ ] **Step 1:** Run `.\gradlew.bat clean test :app:assembleDebug`. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 2:** If an emulator is already running (`adb devices` lists `emulator-*`), install the debug APK and walk through onboarding → topic → session → result. Do not touch a physical phone. If no emulator is running, record that the UI was not exercised on a device.
- [ ] **Step 3:** Push the branch and open a pull request against `master`.
