# Stage 6c — Session and Result Screens Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the practice session and its result screen in the "A — Expressive" style. The session gets the wavy progress, morphing option cards, a sliding feedback panel and haptics. The result screen gets a cookie ring, stat tiles, readiness growth and confetti for strong results.

**Architecture:**
- Learning logic stays in `SessionViewModel`. It gains two readiness readings: one before the session and one after everything is saved.
- Mapping from session state to component state (option state, readiness line) lives in pure functions with tests.
- Screens are composed from the 6a `ui/design` components.
- The old `ui/common/OptionCard` is deleted. The mock screen moves to the design `OptionCard`, which only changes its look; mock gets its full restyle in 6e.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 (stable 1.4), Navigation Compose 2.10, JUnit 4, kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-10-04-stage6-ui-redesign-design.md` (sections 6, 7, 7.1, 11, 12)

## Global Constraints

- `minSdk 26`; no new Gradle dependencies; no Room schema change.
- UI text is Uzbek (Latin) in `res/values/strings.xml`, with apostrophes escaped as `\'`. Code and terms stay English.
- No hex literals in screens; colours come from the theme and `LocalExtraColors`. Text containers use `heightIn(min = …)`. Endless or decorative motion stops under `LocalReducedMotion`.
- `SessionViewModel` behaviour stays the same:
  - single and multi questions;
  - eliminated wrong options;
  - repeat questions at the end;
  - the exit confirmation;
  - `ReportIssueAction`;
  - outcomes written as soon as a question is solved.
- Gradle: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"; export PATH="$JAVA_HOME/bin:$PATH"`, run from `/d/projects/devsuhbat` in the Bash tool, wrapped in `timeout`; trust results only after "BUILD SUCCESSFUL".
- Commits: no co-author line. Branch `stage6/6c-session-result` from `master`. PR to `master`, Uzbek body ending with the Claude Code line; the user merges.
- **Emulator (shared with another session):**
  - Before using it, check `C:\Users\user\AppData\Local\Temp\claude\emulator.lock`. If it is absent, write `devsuhbat <ISO time>` to it. If it is present, wait.
  - When done, delete the lock and message the peer session (`uds:\\.\pipe\LOCAL\cc-msg-992fefb1fe6cf1e462494b794237a87a`).
  - Always use `adb -s emulator-5554`. Install `app-release.apk` with `install -r`.
  - Restore any setting you change (font scale, density, animator scale, app theme).

**One refinement of the spec, decided here.** Spec 6 places `FeedbackSheet` in the bottom area above the button. It goes instead at the end of the scrolling question content, directly above the bottom button. The existing auto-scroll brings it into view, so on screen it still sits right above the button. The reason: a bottom-anchored panel with a long explanation, or with several multi-choice hints, would cover the options on a small phone.

## Review Focus

1. **The last answer is in the "after" readiness.** Outcome writes run in their own coroutines. The after-reading must wait for all of them and for the session log. Test in Task 2 (`readinessAfterWaitsForSlowWrites`).
2. **Readiness can go down.** A wrong first try sends a question back to box 1, so it is no longer mastered. The result then shows the plain current percent, never a misleading "→". Test in Task 3 (`droppedReadinessShowsOnlyTheCurrentPercent`).
3. **Multi-choice states after solving:**
   - the selected options show as correct;
   - options eliminated earlier stay crossed out (not dimmed);
   - untouched wrong options are dimmed.

   Test in Task 3 (`multiSolvedStates`).
4. **Headline boundaries:** 4/5 (exactly 80%) is "Zo'r natija!" and confetti. 1/2 (exactly 50%) is "Yaxshi harakat". 0 questions never celebrate. Test in Task 1.
5. **Long content on a 360 dp phone:**
   - a code question;
   - a multi question with two hints;
   - a long explanation.

   All stay readable and scrollable, with the bottom button never covering text. Checked on the emulator in Task 7, step 2.

---

### Task 1: Result headline rule

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/engine/ResultHeadline.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/ResultHeadlineTest.kt`

**Interfaces:**
- Produces: `enum class ResultHeadline { GREAT, GOOD, KEEP_GOING; companion object { fun of(firstTryCorrect: Int, total: Int): ResultHeadline } }` and `val ResultHeadline.celebrates: Boolean`, which is `this == GREAT`.

- [ ] **Step 1: Write the failing tests:**
  - `atLeastEightyPercentIsGreat`: `of(5, 6)` and `of(4, 5)` give GREAT.
  - `atLeastHalfIsGood`: `of(1, 2)` and `of(7, 10)` give GOOD.
  - `belowHalfKeepsGoing`: `of(2, 5)` gives KEEP_GOING.
  - `emptySessionNeverCelebrates`: `of(0, 0)` gives KEEP_GOING and `!celebrates`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :core:engine:test --tests "uz.devsuhbat.engine.ResultHeadlineTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement** with integer math: `firstTryCorrect * 100 >= 80 * total` (when `total > 0`), then `>= 50 * total`.
- [ ] **Step 4:** Run the same command and expect PASS.
- [ ] **Step 5: Commit** with `git commit -m "feat(engine): add result headline rule"`.

### Task 2: Readiness before and after a session

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/ui/session/SessionViewModel.kt`
- Test: `app/src/test/java/uz/devsuhbat/ui/session/SessionViewModelTest.kt`

**Interfaces:**
- Produces:
  - The `SessionViewModel` constructor gains a last parameter `readiness: suspend () -> Progress? = { null }`. It returns null when no field is set.
  - `SessionUiState` gains `val readinessBefore: Progress? = null` and `val readinessAfter: Progress? = null`.

Rules:
- **Before.** `readinessBefore` is read once in `init`, before `loadQuestions`.
- **Write tracking.** Every `persist` job is kept in a list.
- **After.** When the session finishes and is non-empty, one coroutine does the following: under `NonCancellable`, `joinAll` every outcome job, then run `onFinished(result)`; then read `readiness()`; then publish `readinessAfter`. Every published state carries `readinessBefore`.

- [ ] **Step 1: Write the failing tests:**
  - `readsReadinessBeforeTheFirstQuestion`: `readiness = { Progress(5, 10) }` gives `state.readinessBefore == Progress(5, 10)` once loaded.
  - `readinessAfterWaitsForSlowWrites`, inside `runTest(mainDispatcher)`:
    - `recorded = mutableListOf<QuestionOutcome>()`;
    - `onOutcome = { delay(50); recorded += it }`;
    - `onFinished = { delay(50) }`;
    - `readiness = { Progress(recorded.size, 10) }`.

    Answer all three singles correctly, then `advanceUntilIdle()`. Assert `readinessBefore == Progress(0, 10)` and `readinessAfter == Progress(3, 10)`.
  - `noReadinessWhenThereIsNoField`: the default `readiness` leaves both values null.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.session.SessionViewModelTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement.** Keep `persist` for outcome writes, collecting the jobs in `private val writes = mutableListOf<Job>()`.
- [ ] **Step 4:** Run the tests and expect all 25 to PASS.
- [ ] **Step 5: Commit** with `git commit -m "feat(session): read readiness before and after a session"`.

### Task 3: Mapping session state to component state

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/session/SessionMapping.kt`
- Test: `app/src/test/java/uz/devsuhbat/ui/session/SessionMappingTest.kt`

**Interfaces:**
- Consumes: `OptionState` (`ui/design`), `Progress`.
- Produces:
  - `fun optionState(optionId: String, selected: Set<String>, disabled: Set<String>, solved: Boolean): OptionState`:
    - in `disabled` gives WRONG;
    - else `solved` and selected gives CORRECT;
    - `solved` and not selected gives DIMMED;
    - selected gives SELECTED;
    - otherwise IDLE.
  - `sealed interface ReadinessLine { data class Pending(val percent: Int?) : ReadinessLine; data class Grew(val from: Int, val to: Int) : ReadinessLine; data class Current(val percent: Int) : ReadinessLine }`.
  - `fun readinessLine(before: Progress?, after: Progress?): ReadinessLine?`:
    - both null gives null;
    - `after == null` gives `Pending(before?.percent)`;
    - `after.percent > before.percent` gives `Grew`;
    - otherwise `Current(after.percent)`. A `before == null` with `after != null` also gives `Current`.

- [ ] **Step 1: Write the failing tests:**
  - `singleFlowStates`:
    - IDLE when nothing is selected;
    - SELECTED when selected and not solved;
    - WRONG when eliminated;
    - after solving, the selected option is CORRECT and the rest are DIMMED.
  - `multiSolvedStates`: `selected = {a, b}`, `disabled = {c}`, `solved = true` gives a and b CORRECT, c WRONG and d DIMMED.
  - `grownReadinessShowsBothValues`: `readinessLine(Progress(5, 10), Progress(6, 10)) == Grew(50, 60)`.
  - `droppedReadinessShowsOnlyTheCurrentPercent`: `readinessLine(Progress(6, 10), Progress(5, 10)) == Current(50)`, and an unchanged value gives `Current(60)`.
  - `pendingWhileSaving`: `readinessLine(Progress(5, 10), null) == Pending(50)`, and `readinessLine(null, null) == null`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.session.SessionMappingTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4:** Run the tests and expect PASS.
- [ ] **Step 5: Commit** with `git commit -m "feat(session): map session state to component state"`.

### Task 4: Question body, code block and one OptionCard

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/ui/common/QuestionViews.kt`:
  - `QuestionBody`: prompt in `headlineSmall`;
  - delete the old `OptionCard`.
- Modify: `app/src/main/java/uz/devsuhbat/ui/common/CodeBlock.kt`: `surfaceContainer` fill, `RoundedCornerShape(20.dp)`, 16dp padding, `bodyMedium` size kept at 13sp/19sp.
- Modify: `app/src/main/java/uz/devsuhbat/ui/common/Labels.kt`: add `QuestionKind.titleRes`.
- Modify: `app/src/main/java/uz/devsuhbat/ui/mock/MockScreen.kt`: use the design `OptionCard` with `state = if (selected) SELECTED else IDLE`, `letter = 'A' + index`.
- Modify: `app/src/main/res/values/strings.xml`: add the kind strings.

  | Key | Text |
  |---|---|
  | `kind_concept` | Tushuncha |
  | `kind_true_false` | To\'g\'ri / noto\'g\'ri |
  | `kind_code_output` | Kod natijasi |
  | `kind_code_review` | Kod review |

**Interfaces:**
- Produces: `@get:StringRes val QuestionKind.titleRes: Int`.

- [ ] **Step 1: Implement** the changes above.
- [ ] **Step 2:** Run `timeout 700 ./gradlew test :app:assembleDebug` and expect BUILD SUCCESSFUL. `MockViewModelTest` stays green. `rg "ui.common.OptionCard"` must give no hits.
- [ ] **Step 3: Commit** with `git commit -m "refactor(question): share the design option card and restyle code"`.

### Task 5: Session screen

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/ui/session/SessionScreen.kt`

**Interfaces:**
- Consumes:
  - `SessionUiState.readinessBefore/After` (Task 2);
  - `optionState` (Task 3);
  - `QuestionKind.titleRes` and `Level.titleRes` (Task 4);
  - `WavyProgress`, `OptionCard`, `FeedbackSheet`, `ExpressiveButton`, `rememberHaptics`, `HapticEvent` (6a).
- Produces: `SessionScreen(container, topicId, onExit, onAgain, onHome)`, with an unchanged signature.

**View-model wiring.** The factory passes `readiness`:
- read `settings.first()` and `states.first()`;
- take the field's topic pool, as `loadSessionQuestions` does;
- `QuestionPicker.eligible(pool, level)`;
- `Readiness.of(...)` on `container.io`;
- return null without a field or level.

**Layout:**
- **Top row** (`statusBarsPadding`, 8dp horizontal padding):
  - close `IconButton` (`Icons.Rounded.Close`, opens the exit dialog);
  - `WavyProgress(position / queueSize, Modifier.weight(1f))`;
  - "N / M" in `titleSmall`;
  - `ReportIssueAction(question)`.
- **Content:** an `AnimatedContent` keyed by `Triple(question.id, state.isRepeat, state.position)`. It slides horizontally (`DsMotion.spatialDefault()`, 1/4 width) and fades. Inside, a scrolling column (20dp horizontal, 14dp gaps) holds:
  1. A chip row (`FlowRow`, 8dp gaps). The kind chip uses `primaryContainer`/`onPrimaryContainer`. The level chip uses `surfaceContainerLowest` with an `outlineVariant` border. When `isRepeat`, a "Takror" chip uses `streakContainer`/`onStreakContainer`. Each chip is `labelMedium` with a 10dp radius and 10×5dp padding.
  2. `QuestionBody(question)`.
  3. One `OptionCard` per option. Use `letter = 'A' + index`, `state = optionState(...)`, `multi = type == MULTI`, `onClick = { haptics(SELECT); onToggle(id) }`.
  4. The `FeedbackSheet` for the current feedback:
     - **Wrong:** `session_wrong_title`. For multi, `session_multi_feedback`. Each hint shows its option text with line-through in `labelLarge`, then the hint in `bodyMedium`, both via `InlineCodeText`.
     - **Correct:** `session_correct_title`, then the explanation.
- **Bottom** (`navigationBarsPadding`, 16dp):
  - `ExpressiveButton("Tekshirish", enabled = canCheck)`;
  - or, when solved, `ExpressiveButton(next or finish, tone = SUCCESS)`.
- **Haptics:** `LaunchedEffect(state.feedback)` fires `CORRECT` or `WRONG` once for each new feedback.
- **Kept from today:**
  - scroll to top on a new question;
  - scroll to the bottom on new feedback;
  - the exit dialog;
  - `BackHandler`;
  - the loading spinner.

- [ ] **Step 1: Implement.**
- [ ] **Step 2:** Run `timeout 700 ./gradlew test :app:assembleDebug` and expect BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** with `git commit -m "feat(session): rebuild the practice screen in the expressive style"`.

### Task 6: Result screen

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/ui/session/SessionResultScreen.kt` (new signature below), `SessionScreen.kt` (call site)
- Modify: `app/src/main/res/values/strings.xml`. Add:

  | Key | Text |
  |---|---|
  | `result_great` | Zo\'r natija! |
  | `result_good` | Yaxshi harakat |
  | `result_keep_going` | Davom eting |
  | `result_readiness` | Tayyorlik |
  | `result_readiness_value` | Tayyorlik: %1$d%% |
  | `result_readiness_grew` | %1$d%% → %2$d%% |
  | `result_score` | %1$d / %2$d |

  Remove `result_title` if it is no longer used.

**Interfaces:**
- Consumes: `ResultHeadline` (Task 1), `readinessLine` (Task 3), `ReadinessRing`, `StatTile`, `Confetti`, `ExpressiveButton`, `SectionCard` (6a).
- Produces: `SessionResultScreen(result: SessionResult, readiness: ReadinessLine?, onAgain: (() -> Unit)?, onHome: () -> Unit)`.

**Layout**, inside a `Box` with a full-size `Confetti(play = ResultHeadline.of(...).celebrates)` drawn on top:
- **Title.** When `result.total > 0`: the headline string in `headlineMedium`, centred, 32dp from the top.
- **Score ring.** `ReadinessRing` at 200dp:
  - default colours, `centerStyle = displaySmall`;
  - `fraction = firstTryCorrect / total`;
  - the centre text counts 0 → `firstTryCorrect` over 900ms (an `Animatable` started once), rendered with `result_score`. Under reduced motion it shows the final value at once.
- **Stat tiles.** A row of three `StatTile`s, staggered `enterDelayMillis` 550/650/750:

  | Tile | Colours |
  |---|---|
  | total, `result_total` | default |
  | first try, `result_first_try` | `successContainer`/`onSuccessContainer` |
  | reworked, `result_reworked` | `streakContainer`/`onStreakContainer` |

  Above `fontScale > 1.5` they stack.
- **Readiness card.** A `SectionCard`, only when `readiness != null`:
  - `Pending` shows `result_readiness_value(percent)`, or nothing when percent is null;
  - `Current` shows `result_readiness_value`;
  - `Grew` shows `result_readiness` with `result_readiness_grew` on the right in `success`. Under it, a 10dp bar: `primary` up to `from`, then `success` from `from` to `to`, animated from 0 width with `spatialDefault`.
- **Empty session.** When `result.total == 0`, only `result_empty` is shown, centred, with no ring, tiles or confetti.
- **Bottom.**
  - `ExpressiveButton(result_again)` with an OUTLINED `result_home`;
  - for a mistakes session (`onAgain == null`), only a primary `result_home`.

- [ ] **Step 1: Implement.** The `SessionScreen` call site passes `readinessLine(state.readinessBefore, state.readinessAfter)`.
- [ ] **Step 2:** Run `timeout 900 ./gradlew test :app:assembleDebug :app:assembleRelease` and expect BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** with `git commit -m "feat(session): rebuild the result screen with readiness growth"`.

### Task 7: Verify on device and open PR

- [ ] **Step 1:** Take the emulator lock and install the release APK. Then, in light theme:
  - a single question: wrong pick → shake and hint; right pick → pop and explanation;
  - a multi question (find one in "Algoritmlar…" or the mixed session), with a partial pick that shows the multi feedback line;
  - finish a session and check the result screen: headline, counting ring, tiles, readiness line;
  - confetti only at ≥80%;
  - "Yana mashq" and "Bosh sahifa".

  Repeat one wrong/correct pair and the result screen in dark theme.
- [ ] **Step 2:** Set density 480 (360 dp) and font scale 1.3. Open a code question and a multi question with two hints, and check readability and scrolling. Restore density and font scale.
- [ ] **Step 3:** With `animator_duration_scale 0`, finish a session: there is no confetti, the result values show at once and the app works. Restore to 1. Restore the app theme to Yorug'. Delete the lock and message the peer session.
- [ ] **Step 4:** Push `stage6/6c-session-result` and open a PR to `master`. The Uzbek body lists what changed, the test counts and what was checked. End with the Claude Code line, then wait for the user to merge.
