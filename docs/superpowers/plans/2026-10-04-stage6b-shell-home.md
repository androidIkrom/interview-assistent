# Stage 6b — Navigation Shell and Home Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give DevSuhbat a bottom navigation bar and the redesigned "A — Expressive" Home screen with streak, weak topics and a one-tap mixed practice. Fix the 6a deferred findings in `ReadinessRing` and `WavyProgress` before Home starts using them.

**Architecture:**
- The streak rule is a pure `core/engine` function.
- `ProgressRepository` exposes the local day of every finished session.
- `HomeViewModel` combines that with the existing flows.
- `Nav.kt` wraps the `NavHost` in a `Scaffold` whose `NavigationBar` shows only on top-level routes.
- `HomeScreen` is rebuilt from the 6a `ui/design` components.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 (stable 1.4), Navigation Compose 2.10, Room 2.8, JUnit 4, Robolectric, kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-10-04-stage6-ui-redesign-design.md` (sections 4, 5, 11, 12)

## Global Constraints

- `minSdk 26`; no new Gradle dependencies; no Room schema change (new `@Query` only).
- UI text is Uzbek (Latin) in `res/values/strings.xml`, with apostrophes escaped as `\'`. Code and technical terms stay English.
- Colours, type and shapes come only from the theme and `LocalExtraColors`; no hex literals in screens.
- Endless animations stop under `LocalReducedMotion`. Text containers use `heightIn(min = …)`, never a fixed height.
- Gradle: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"; export PATH="$JAVA_HOME/bin:$PATH"`, run from `/d/projects/devsuhbat` in the Bash tool, wrapped in `timeout`; trust results only after "BUILD SUCCESSFUL".
- Commits: no co-author line. Branch `stage6/6b-shell-home` from `master`. PR to `master`, Uzbek body ending with the Claude Code line; the user merges.
- Emulator: always `adb -s emulator-5554` with `-memory 2048`; never touch the physical phone. The emulator app is release-signed: install `app-release.apk` with `install -r` (never uninstall, which loses its data).

**Two refinements of the spec, decided here:**
1. **Session days as a list.** Spec 5.2's `activeDays(): Flow<Set<Long>>` becomes `sessionDays(): Flow<List<Long>>`, one entry per finished session. Home takes `.toSet()` for the streak, and the 6d heatmap reuses the same list to count sessions per day.
2. **Two tabs for now.** The bar ships with **Bosh** and **Mavzular** in 6b. **Statistika** is added together with its screen in 6d, so no placeholder screen ever ships.

## Review Focus

1. **Midnight and time zone:** a session started at 23:50 local time counts for that local day, not the UTC day. Test in Task 2 (`sessionDaysUseTheLocalCalendar`).
2. **Only yesterday active, nothing today yet:** the streak still shows yesterday's run, with today pending rather than broken. Test in Task 1 (`pendingTodayKeepsYesterdaysStreak`).
3. **All topics at 100%:** "Bo'sh mavzular" disappears instead of listing mastered topics. Topics with no question at the chosen level never appear. Test in Task 3 (`weakTopicsSkipMasteredAndEmptyTopics`).
4. **Bottom bar and system insets:** content inside a tab is not covered by the bar and is not padded twice. The bar disappears in a session. Checked on the emulator in Task 7, step 2.
5. **Font scale 200% on Home:** the two side-by-side tiles stack into one column, and the ring label stays inside the ring. Checked on the emulator in Task 7, step 3. The ring-label rule is tested in Task 4 (`ringLabelSizeIgnoresFontScale`).

---

### Task 1: Streak rule

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/engine/Streak.kt`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/StreakTest.kt`

**Interfaces:**
- Produces:
  - `enum class DayMark { DONE, TODAY_PENDING, MISSED, FUTURE }`
  - `data class StreakInfo(val current: Int, val longest: Int, val week: List<DayMark>)`, where `week` has 7 entries, Monday to Sunday of today's week
  - `object Streak { fun of(activeDays: Set<Long>, today: Long): StreakInfo; val NONE: StreakInfo }`, where `NONE` is `StreakInfo(0, 0, emptyList())`

- [ ] **Step 1: Write the failing tests.** Days are epoch days. Use `val wed = LocalDate.of(2026, 10, 7).toEpochDay()` (a Wednesday).
  - `emptyHistoryHasNoStreak`: `of(emptySet(), wed)` gives current 0 and longest 0.
  - `streakCountsBackFromToday`: `{wed, wed-1, wed-2}` gives current 3.
  - `pendingTodayKeepsYesterdaysStreak`: `{wed-1, wed-2}` gives current 2.
  - `gapBreaksTheStreak`: `{wed, wed-2}` gives current 1. `{wed-2, wed-3}` gives current 0.
  - `longestIsTheLongestRunInHistory`: `{wed-20, wed-19, wed-18, wed-10, wed}` gives longest 3 and current 1.
  - `weekRunsMondayToSunday`: `{wed-2}` (Monday) gives week `[DONE, MISSED, TODAY_PENDING, FUTURE, FUTURE, FUTURE, FUTURE]`. `{wed-2, wed}` gives `[DONE, MISSED, DONE, FUTURE, …]`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :core:engine:test --tests "uz.devsuhbat.engine.StreakTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement `Streak.of`.**
  - Start the current run at `today` if it is active, else at `today - 1` if that is active, else return 0. From there count back over consecutive active days.
  - `longest` is the longest consecutive run in the sorted days.
  - The week's Monday is `today - (LocalDate.ofEpochDay(today).dayOfWeek.value - 1)`.
- [ ] **Step 4:** Run the same command and expect PASS.
- [ ] **Step 5: Commit** with `git commit -m "feat(engine): add streak rule"`.

### Task 2: Session days from the log

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/data/AppDatabase.kt` (`ProgressDao.observeSessions()`)
- Modify: `app/src/main/java/uz/devsuhbat/data/ProgressRepository.kt` (`sessionDays()`)
- Test: `app/src/test/java/uz/devsuhbat/data/ProgressRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `@Query("SELECT * FROM session_log ORDER BY startedAt") fun observeSessions(): Flow<List<SessionLogEntity>>`
  - `fun sessionDays(): Flow<List<Long>>`: the local epoch day of each session's `startedAt` in `clock.zone`, in start order, one entry per session.

- [ ] **Step 1: Write the failing tests** in `ProgressRepositoryTest`. Use the file's `zone = Asia/Tashkent` (UTC+5).
  - `sessionDaysUseTheLocalCalendar`: a session logged with `startedAt = Instant.parse("2026-10-02T18:50:00Z")` (23:50 local) and one at `2026-10-02T19:10:00Z` (00:10 the next local day) give `listOf(today, today + 1)`.
  - `sessionDaysStartEmpty`: `sessionDays().first()` is empty.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.data.ProgressRepositoryTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement** the DAO query and `sessionDays()` with `Instant.ofEpochMilli(it.startedAt).atZone(clock.zone).toLocalDate().toEpochDay()`.
- [ ] **Step 4:** Run the tests and expect PASS.
- [ ] **Step 5: Commit** with `git commit -m "feat(data): expose the local day of each session"`.

### Task 3: HomeViewModel — streak and weak topics

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/ui/home/HomeViewModel.kt`
- Test: `app/src/test/java/uz/devsuhbat/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: `Streak.of`, `StreakInfo`, `ProgressRepository.sessionDays()`.
- Produces:
  - `data class TopicProgress(val topicId: String, val title: String, val progress: Progress)`.
  - `HomeUiState` gains `val streak: StreakInfo = Streak.NONE` and `val weakTopics: List<TopicProgress> = emptyList()`.
  - The `HomeViewModel` constructor gains a last parameter `sessionDays: Flow<List<Long>> = flowOf(emptyList())`.

Weak topics rule:
- Take the field's topics (`content.topicsOf(fieldId)`).
- For each, compute the eligible questions at the chosen level and their `Readiness.of`.
- Keep only topics with `total > 0` and `percent < 100`.
- Sort by `percent` ascending; on equal percent, larger `total` first.
- Take 3.

- [ ] **Step 1: Write the failing tests.** Extend the test catalog with a second field topic `a.u` (file `a_u.json`) holding 2 junior questions, and a third topic `a.e` with only a senior question.
  - `streakComesFromSessionDays`: `sessionDays = flowOf(listOf(today - 1, today - 1, today))` gives `state.streak.current == 2`.
  - `weakTopicsAreLowestFirst`: `a.t` at 1/4 (25%) and `a.u` at 0/2 (0%) give `weakTopics.map { it.topicId } == listOf("a.u", "a.t")`.
  - `weakTopicsSkipMasteredAndEmptyTopics`: with both `a.u` questions at box 3, `a.u` is absent. `a.e` never appears, since it has no junior question. When every eligible question is mastered, `weakTopics` is empty.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.home.HomeViewModelTest"`. Expect a compile FAIL. The existing tests must still compile with the new catalog; adjust their expected `readiness` if the junior question count changes.
- [ ] **Step 3: Implement.** Combine `sessionDays` into the existing `combine`, which now takes 4 flows. Compute everything inside the existing `withContext(io)`.
- [ ] **Step 4:** Run the tests and expect PASS. Then run `timeout 600 ./gradlew test` and expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** with `git commit -m "feat(home): add streak and weak topics to home state"`.

### Task 4: Ring and wave fixes from the 6a review

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/ui/design/ReadinessRing.kt`, `WavyProgress.kt`
- Test: `app/src/test/java/uz/devsuhbat/ui/design/RingAndWaveTest.kt`

**Interfaces:**
- Produces:
  - `fun ringLabelSizeSp(ringSizeDp: Float, fontScale: Float): Float`. The ring label is 22% of the ring size, divided by `fontScale`, so it keeps its drawn size and stays inside the ring.
  - `fun trackSegment(width: Float, activeEnd: Float, gap: Float, stroke: Float): Pair<Float, Float>?`. It returns the start and end x of the flat track: start `max(stroke / 2, activeEnd + gap)` (or `stroke / 2` when `activeEnd == 0f`) and end `width - stroke / 2`. It returns `null` when start ≥ end.
  - `ReadinessRing` gains `contentDescription: String? = null`, applied as semantics.

- [ ] **Step 1: Write the failing tests** in `RingAndWaveTest`:
  - `ringLabelSizeIgnoresFontScale`: `ringLabelSizeSp(120f, 1f) == 26.4f` and `ringLabelSizeSp(120f, 2f) == 13.2f`, each within `0.01f`.
  - `trackStaysInsideTheCanvas`: `trackSegment(200f, 0f, 10f, 5f) == 2.5f to 197.5f`.
  - `trackStartsAfterTheGap`: `trackSegment(200f, 50f, 10f, 5f) == 60f to 197.5f`.
  - `noTrackWhenTheWaveIsFull`: `trackSegment(200f, 195f, 10f, 5f) == null`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.design.RingAndWaveTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement.**
  - `ReadinessRing`:
    - draw with `Modifier.drawWithCache`; build the cookie `Path` once per size in the cache block;
    - read the rotation and sweep `State`s only inside `onDrawBehind`, so there is no recomposition per frame;
    - the centre `Text` uses `fontSize = ringLabelSizeSp(size.value, LocalDensity.current.fontScale).sp` and keeps `centerStyle` otherwise;
    - `contentDescription` goes on the root via `semantics { this.contentDescription = it }`, and the inner text is cleared from semantics when a description is given.
  - `WavyProgress`:
    - read the phase and fraction `State`s inside the draw lambda;
    - draw the track from `trackSegment`.
- [ ] **Step 4:** Run `timeout 600 ./gradlew test :app:assembleDebug` and expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** with `git commit -m "fix(design): draw ring and wave without per-frame recomposition"`.

### Task 5: Navigation shell

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/NavMotion.kt`
- Modify: `app/src/main/java/uz/devsuhbat/ui/Nav.kt`, `app/src/main/java/uz/devsuhbat/ui/topics/TopicsScreen.kt` (drop `onBack` and its back button)
- Modify: `app/src/main/res/values/strings.xml` (`nav_home` = "Bosh", `nav_topics` = "Mavzular")
- Test: `app/src/test/java/uz/devsuhbat/ui/NavMotionTest.kt`

**Interfaces:**
- Produces:
  - `val TOP_LEVEL_ROUTES: List<String> = listOf(Routes.HOME, Routes.TOPICS)`. 6d appends `Routes.STATS`.
  - `fun isTopLevel(route: String?): Boolean`.
  - `enum class NavMotion { FADE, SLIDE }`.
  - `fun navMotion(from: String?, to: String?): NavMotion`, which is FADE when both routes are top level and SLIDE otherwise.
  - Routes compare by `NavDestination.route` strings, so `session/{topicId}` is not top level.

- [ ] **Step 1: Write the failing tests** in `NavMotionTest`:
  - `tabsFadeBetweenEachOther`: `navMotion(HOME, TOPICS) == FADE`.
  - `screensAboveTabsSlide`: `navMotion(HOME, Routes.SESSION) == SLIDE`, `navMotion(Routes.SESSION, HOME) == SLIDE`, `navMotion(HOME, SETTINGS) == SLIDE`.
  - `onlyTabsAreTopLevel`: `isTopLevel(HOME)`, `!isTopLevel(Routes.SESSION)`, `!isTopLevel(null)`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.NavMotionTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement.**
  - **Scaffold.** `DevSuhbatNavHost` builds a `Scaffold(contentWindowInsets = WindowInsets(0))`. Its `bottomBar` shows a `NavigationBar` only when `isTopLevel(currentRoute)`, wrapped in `AnimatedVisibility` (slide up/down, `DsMotion.spatialDefault()`).
  - **Tabs.**

    | Tab | Icon | Label |
    |---|---|---|
    | Bosh | `Icons.Rounded.Home` | `nav_home` |
    | Mavzular | `Icons.AutoMirrored.Rounded.List` | `nav_topics` |

    Selecting a tab calls `navigate(route) { popUpTo(Routes.HOME) { saveState = true }; launchSingleTop = true; restoreState = true }`.
  - **Insets.** The `NavHost` gets `Modifier.padding(bottom = pad.calculateBottomPadding()).consumeWindowInsets(PaddingValues(bottom = pad.calculateBottomPadding()))`, so inner `Scaffold`s do not pad the bottom twice.
  - **Transitions.** `NavHost` `enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition` use `navMotion(initialState.destination.route, targetState.destination.route)`:
    - FADE: `fadeIn(DsMotion.effectsDefault())` / `fadeOut(DsMotion.effectsDefault())`;
    - SLIDE: `slideInHorizontally(DsMotion.spatialDefault()) { it / 4 } + fadeIn`, mirrored for pop.
  - **Topics.** Remove `onBack` from `TopicsScreen`. `Routes.TOPICS`'s `composable` passes only `onOpen`.
- [ ] **Step 4:** Run `timeout 600 ./gradlew test :app:assembleDebug` and expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** with `git commit -m "feat(nav): add bottom navigation with home and topics tabs"`.

### Task 6: Home screen redesign

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/ui/home/HomeScreen.kt` (rewrite the composables; the view-model wiring adds `sessionDays = container.progress.sessionDays()`)
- Modify: `app/src/main/java/uz/devsuhbat/ui/Nav.kt` (Home callbacks)
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `HomeUiState` (Task 3); `ReadinessRing`, `RingDefaults.onPrimaryColors()`, `ExpressiveButton`, `SectionCard` (6a and Task 4).
- Produces: `HomeScreen(container, onPractice: () -> Unit, onMistakes: () -> Unit, onMock: () -> Unit, onTopic: (String) -> Unit, onAllTopics: () -> Unit, onSettings: () -> Unit)`.
  - `onPractice` navigates to `Routes.session(Routes.MIXED)`.
  - `onTopic` navigates to `Routes.session(topicId)`.
  - `onAllTopics` uses the same tab navigation as Task 5.

Layout (spec 5), inside a `LazyColumn`-free scrolling `Column` with 16dp padding and 12dp gaps:
1. **Header.** `home_greeting` in `bodyMedium` / `onSurfaceVariant`, then `app_name` in `headlineMedium`. A settings `IconButton` (`Icons.Rounded.Tune`, content description `settings_title`).
2. **Hero.**
   - A `Surface` with shape `extraLarge`. Light: `primary` with `onPrimary`. Dark: `primaryContainer` with `onPrimaryContainer`. Choose by `colorScheme.primary.luminance() < 0.5f`.
   - Contents: `home_your_target` in `labelMedium`, the field title in `headlineSmall`, and a level chip (`labelMedium` on a 18%-alpha `onPrimary` pill).
   - A row with `ReadinessRing(size = 120.dp, colors = RingDefaults.onPrimaryColors(), centerText = "${percent}%", contentDescription = stringResource(R.string.home_readiness, percent))`, next to "mastered / total" in `titleLarge` with `home_mastered_label` under it.
   - An `ExpressiveButton(home_start_practice)`. On the primary hero its colours are inverted: `onPrimary` fill, `primary` text. Add a `colors` override to `ExpressiveButton` only if needed, as `containerColor`/`contentColor` parameters with defaults from `tone`.
3. **Tiles row.** Mistakes and streak, `weight(1f)` each. When `LocalDensity.current.fontScale > 1.5f`, they stack into one column.
   - **Mistakes:** `errorContainer` with `onErrorContainer`. Big `dueCount` in `displaySmall`, `home_mistakes_tile`, and a `home_mistakes_action` button enabled when `dueCount > 0`. When `dueCount == 0`, the existing `nextReview` text replaces the button (`home_mistakes_none`, `_tomorrow`, `_later`).
   - **Streak:** `streakContainer` with `onStreakContainer`. Big `current` with `home_streak_days`, plus `home_streak_label`. When current is 0, show `home_streak_start` instead. Seven 16dp dots:

     | Mark | Dot |
     |---|---|
     | DONE | `streak` fill |
     | TODAY_PENDING | 2dp `streak` ring, pulsing scale 1→1.25 over 1.6s unless reduced motion |
     | MISSED | `onStreakContainer` at 12% |
     | FUTURE | `onStreakContainer` at 6% |

     Semantics `contentDescription = home_streak_a11y(current)`.
4. **Mock.** A `SectionCard` row: `home_mock` in `titleMedium`; `home_mock_meta(count)` plus, if `lastMock` exists, `home_mock_last` in `bodySmall`; and a round 48dp `primary` play `IconButton`, disabled when `mockQuestionCount < MOCK_MIN`. When disabled, it shows `home_mock_unavailable` under the title.
5. **Weak topics.** Only when `weakTopics` is not empty. A `SectionCard` with `home_weak_title` and a `home_weak_all` `TextButton` (`onAllTopics`). Each topic is a clickable row (`onTopic`) with its title and percent, and an 8dp bar of `primary` on `surfaceContainer` whose width animates from 0 with `spatialDefault`.

Entrance: sections 2–5 rise 24dp and fade in with `spatialDefault`, staggered 0/80/140/200/260 ms, once per screen entry. Under reduced motion they appear at once.

Strings:
- **Add:**

  | Key | Text |
  |---|---|
  | `home_greeting` | Bugun ham bir qadam |
  | `home_start_practice` | Mashqni boshlash |
  | `home_mastered_label` | savol o\'zlashtirilgan |
  | `home_mistakes_tile` | xato takrorlashni kutmoqda |
  | `home_streak_days` | %1$d kun |
  | `home_streak_label` | ketma-ket mashq |
  | `home_streak_start` | Bugun boshlang |
  | `home_streak_a11y` | %1$d kunlik seriya |
  | `home_mock_meta` | %1$d savol · 30 daqiqa |
  | `home_weak_title` | Bo\'sh mavzular |
  | `home_weak_all` | Barchasi |
  | `home_percent` | %1$d%% |

- **Remove** strings that are no longer referenced: `home_practice`, `home_practice_desc`, `home_mastered`, `home_mock_desc`. Check each with Grep first.

- [ ] **Step 1: Implement** the screen, the strings and the Nav callbacks.
- [ ] **Step 2:** Run `timeout 600 ./gradlew test :app:assembleDebug :app:assembleRelease` and expect BUILD SUCCESSFUL. `HomeViewModelTest` stays green.
- [ ] **Step 3: Commit** with `git commit -m "feat(home): rebuild home in the expressive style"`.

### Task 7: Verify on device and open PR

- [ ] **Step 1:** Install the release APK with `install -r` on `emulator-5554`. Check Home in light, then dark (Settings › Qorong'i):
  - hero, ring and tiles;
  - "Mashqni boshlash" opens a mixed session;
  - a weak-topic row opens that topic;
  - "Barchasi" switches to the Mavzular tab.

  Take one screenshot per theme. Restore the app theme to Yorug' afterwards.
- [ ] **Step 2:** Check tabs and insets:
  - switching Bosh ↔ Mavzular fades and keeps each tab's scroll position;
  - the last Home card is fully visible above the bar;
  - opening a session hides the bar;
  - the system back gesture from a session shows the predictive-back preview and returns to the tab.
- [ ] **Step 3:** Run `settings put system font_scale 2.0`. The tiles must stack, the ring label must stay inside the ring and no text may be clipped. Restore with `1.0`. Then run `settings put global animator_duration_scale 0`: the cookie and the pulse stand still and the app works. Restore with `1`.
- [ ] **Step 4:** Push `stage6/6b-shell-home` and open a PR to `master`. The Uzbek body lists what changed, the test counts, what was checked on the emulator, and that Statistika arrives in 6d. End with the Claude Code line, then wait for the user to merge.
