# Stage 6a — Design System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give DevSuhbat the "A — Expressive" design system: new light and dark colours, bundled fonts, larger shapes and a cookie shape, spring motion tokens, reduced-motion support, haptics, and the reusable `ui/design` components that stages 6b–6e build the screens from.

**Architecture:** The theme is split into `Color.kt`, `Type.kt`, `Shape.kt` and `Theme.kt` under `ui/theme`. Components live in a new `ui/design` package and take plain parameters (text, enum state, colours), never screen view-model types. Geometry and mapping logic (cookie outline, wave points, confetti particles, haptic constants, shake rule) are pure functions, built test-first. The composables are checked by build, `@Preview` and the emulator. Existing screens keep their layout and only pick up the new theme.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 (stable 1.4 via BOM `2026.09.00`), `material-icons-extended` (already a dependency), JUnit 4, Robolectric.

**Spec:** `docs/superpowers/specs/2026-10-04-stage6-ui-redesign-design.md` (section 3; sections 10–12 for 6a)

## Global Constraints

- `minSdk 26`, `targetSdk 37`. No new Gradle dependencies; fonts are files, not a library. No Expressive alpha APIs (`material3` 1.5.0-alpha is out of scope).
- No `INTERNET` permission; fonts are bundled in `app/src/main/res/font/`, licences (SIL OFL 1.1) in `app/src/main/assets/licenses/`.
- Colour hex values, shape radii and spring values are exactly those in spec 3.1, 3.3, 3.4. One addition this plan makes: `ExtraColors.onSuccess` (light `#FFFFFF`, dark `#0B3D20`), needed for text on a `success`-coloured button.
- Components never reference `SessionUiState`, `Feedback` or other screen types; screens adapt to them in 6b–6e.
- New user-visible strings go to `res/values/strings.xml` (Uzbek, Latin, apostrophes escaped `\'`). 6a adds none except content descriptions where noted.
- Gradle: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"; export PATH="$JAVA_HOME/bin:$PATH"`, run from `/d/projects/devsuhbat` in the Bash tool, wrapped in `timeout`; trust results only after "BUILD SUCCESSFUL".
- Commits: no co-author line. Branch `stage6/6a-design-system` from `stage6/spec`. PR to `master` with an Uzbek body ending in the Claude Code line; the user merges.
- Emulator: always `adb -s emulator-5554`; never touch the physical phone; after boot run `am kill-all` before testing.

## Review Focus

1. **Dark mode, success button:** white text on `#7FD8A0` would be unreadable. The pair `onSuccess`/`success` must pass 4.5:1 in both themes. Test in Task 1 (`ThemeContrastTest`).
2. **Cookie shape in non-square bounds:** for example, a 36dp icon versus a wide card. The outline must stay inside the bounds, never be clipped. Test in Task 3 (`cookieStaysInsideNonSquareBounds`).
3. **"Remove animations" turned on in the system:** endless rotation, wave and pulse stop, and confetti draws nothing. Tests in Task 4 (`isReducedMotion`) and Task 7 (`confettiShowsOnlyWhenPlayingAndMotionAllowed`).
4. **Wrong option recomposed or scrolled back into view:** it must shake only once, when it turns wrong, not on every recomposition. Test in Task 8 (`shakesOnlyOnTransitionIntoWrong`).
5. **Font scale 200%:** option, tile and button text grows without clipping, so containers use `heightIn(min = …)`, never a fixed `height` around text. Checked on the emulator in Task 9, step 3.

---

### Task 1: Colour system

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/theme/Color.kt`
- Modify: `app/src/main/java/uz/devsuhbat/ui/theme/Theme.kt` (move `LightColors`, `DarkColors`, `ExtraColors`, `LightExtra`, `DarkExtra` out; keep `LocalExtraColors`, `isDark`, `DevSuhbatTheme`)
- Modify: `app/src/main/res/values/colors.xml` (`window_background` → `#FFF6F4FD`), `app/src/main/res/values-night/colors.xml` (`window_background` → `#FF131218`)
- Test: `app/src/test/java/uz/devsuhbat/ui/theme/ThemeContrastTest.kt`

**Interfaces:**
- Produces: `internal val LightColors: ColorScheme`, `internal val DarkColors: ColorScheme`, `internal val LightExtra: ExtraColors`, `internal val DarkExtra: ExtraColors`.
- Produces: `@Immutable data class ExtraColors(val success: Color, val onSuccess: Color, val successContainer: Color, val onSuccessContainer: Color, val streak: Color, val streakContainer: Color, val onStreakContainer: Color, val codeBackground: Color, val codeText: Color)`. Existing call sites (`success`, `successContainer`, `onSuccessContainer`) keep compiling.

- [ ] **Step 1: Write the failing test** `ThemeContrastTest`. Use a helper `contrast(a: Color, b: Color) = (max(La, Lb) + 0.05) / (min(La, Lb) + 0.05)` with `Color.luminance()`. Run one test per theme over these pairs, asserting `>= 4.5` with the pair name in the message:
  - `onPrimary`/`primary`
  - `onPrimaryContainer`/`primaryContainer`
  - `onSurface`/`background`
  - `onSurface`/`surfaceContainerLowest`
  - `onSurfaceVariant`/`surfaceContainerLowest`
  - `onErrorContainer`/`errorContainer`
  - `error`/`surfaceContainerLowest`
  - `onSuccess`/`success`
  - `onSuccessContainer`/`successContainer`
  - `onStreakContainer`/`streakContainer`
  - `codeText`/`codeBackground`

  Add one more test asserting the spec values literally: `LightColors.primary == Color(0xFF4433D1)`, `DarkColors.background == Color(0xFF131218)`, `LightExtra.streak == Color(0xFFF07A2B)`.

- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.theme.ThemeContrastTest"`. Expect a compile FAIL, because `onSuccess`, `streak` and `codeText` are missing and `LightColors` is private.
- [ ] **Step 3: Implement `Color.kt`.** Use every value from spec 3.1. Set both `background` and `surface` to the background value, and set `surfaceContainerLowest` and `surfaceContainer` explicitly. Remove the old indigo palette. Update the two `window_background` colours.
- [ ] **Step 4:** Run the same test command and expect PASS. Then run `timeout 600 ./gradlew test` and expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** with `git commit -m "feat(theme): add expressive colour system"`.

### Task 2: Fonts and typography

**Files:**
- Create: `app/src/main/res/font/bricolage_grotesque.ttf`, `figtree.ttf`, `jetbrains_mono.ttf`
- Create: `app/src/main/assets/licenses/bricolage-grotesque-OFL.txt`, `figtree-OFL.txt`, `jetbrains-mono-OFL.txt`
- Create: `app/src/main/java/uz/devsuhbat/ui/theme/Type.kt`
- Modify: `app/src/main/java/uz/devsuhbat/ui/common/InlineCodeText.kt` (code span: `JetBrainsMono`, `codeBackground`, `codeText`)
- Modify: `app/src/main/java/uz/devsuhbat/ui/common/CodeBlock.kt` (`FontFamily.Monospace` → `JetBrainsMono`)
- Test: `app/src/test/java/uz/devsuhbat/ui/theme/TypographyTest.kt`

**Interfaces:**
- Produces: `internal val BricolageGrotesque: FontFamily`, `internal val Figtree: FontFamily`, `val JetBrainsMono: FontFamily`, `internal val DevSuhbatTypography: Typography`.

- [ ] **Step 1: Download the variable fonts and licences** from the `google/fonts` repository (OFL):
  ```bash
  B=https://github.com/google/fonts/raw/main/ofl
  F=app/src/main/res/font; L=app/src/main/assets/licenses; mkdir -p $F $L
  curl -fsSL -o $F/bricolage_grotesque.ttf "$B/bricolagegrotesque/BricolageGrotesque%5Bopsz,wdth,wght%5D.ttf"
  curl -fsSL -o $F/figtree.ttf "$B/figtree/Figtree%5Bwght%5D.ttf"
  curl -fsSL -o $F/jetbrains_mono.ttf "$B/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf"
  curl -fsSL -o $L/bricolage-grotesque-OFL.txt "$B/bricolagegrotesque/OFL.txt"
  curl -fsSL -o $L/figtree-OFL.txt "$B/figtree/OFL.txt"
  curl -fsSL -o $L/jetbrains-mono-OFL.txt "$B/jetbrainsmono/OFL.txt"
  ls -l $F $L
  ```
  Expected: three `.ttf` files of non-zero size, each starting with the TrueType magic bytes (`xxd -l 4` shows `0001 0000`), and three OFL text files.

- [ ] **Step 2: Write the failing test** `TypographyTest`:
  - `headlineSmall.fontFamily == BricolageGrotesque`, `fontSize == 22.sp`, `fontWeight == FontWeight.W700`;
  - `bodyLarge.fontFamily == Figtree`, `fontSize == 16.sp`;
  - `labelLarge.fontWeight == FontWeight.W700`;
  - `displayLarge.fontWeight == FontWeight.W800`.
- [ ] **Step 3:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.theme.TypographyTest"`. Expect a compile FAIL.
- [ ] **Step 4: Implement `Type.kt`.** Each family is a list of `Font(R.font.x, weight = FontWeight.Wnnn, variationSettings = FontVariation.Settings(FontVariation.weight(nnn)))` entries:
  - Bricolage: 500, 700, 800;
  - Figtree: 400, 600, 700;
  - JetBrains Mono: 400, 500.

  Typography scale, as size/line height in sp and weight:

  | Style | Family | Size / line | Weight |
  |---|---|---|---|
  | displayLarge / Medium / Small | Bricolage | 56/64, 44/52, 36/44 | 800, letterSpacing −0.5sp |
  | headlineLarge / Medium | Bricolage | 32/40, 28/36 | 800 |
  | headlineSmall | Bricolage | 22/30 | 700 |
  | titleLarge / Medium / Small | Bricolage | 22/28, 18/24, 15/20 | 700 |
  | bodyLarge / Medium / Small | Figtree | 16/24, 14/20, 12/16 | 400 |
  | labelLarge / Medium / Small | Figtree | 15/20, 13/18, 12/16 | 700, 600, 600 |

  In `InlineCodeText`, the code `SpanStyle` uses `fontFamily = JetBrainsMono`, `background = LocalExtraColors.current.codeBackground` and `color = codeText`.

- [ ] **Step 5:** Run the test and expect PASS. Then run `timeout 600 ./gradlew test :app:assembleDebug` and expect BUILD SUCCESSFUL.
- [ ] **Step 6: Commit** with `git add app/src/main/res/font app/src/main/assets/licenses app/src/main/java app/src/test && git commit -m "feat(theme): bundle Bricolage Grotesque, Figtree and JetBrains Mono"`.

### Task 3: Shapes and the cookie shape

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/theme/Shape.kt`
- Create: `app/src/main/java/uz/devsuhbat/ui/design/CookieShape.kt`
- Test: `app/src/test/java/uz/devsuhbat/ui/design/CookieShapeTest.kt`

**Interfaces:**
- Produces: `internal val DevSuhbatShapes: Shapes` with extraSmall 8, small 12, medium 22, large 28, extraLarge 32 dp (`RoundedCornerShape`).
- Produces: `fun cookiePoints(width: Float, height: Float, lobes: Int, depth: Float, steps: Int = 144): List<Offset>` and `class CookieShape(private val lobes: Int = 9, private val depth: Float = 0.07f) : Shape`.

- [ ] **Step 1: Write the failing tests** in `CookieShapeTest`:
  - `pointCountEqualsSteps`: `cookiePoints(100f, 100f, 9, 0.07f).size == 144`.
  - `outerRadiusTouchesTheBoundsAndInnerIsDepthDeeper`: around the centre (50, 50), the max distance is `50f ± 0.5f` and the min distance is `50f / 1.07f * 0.93f ± 0.5f`.
  - `hasOneBumpPerLobe`: the number of local maxima of the distance in the closed loop is `9`.
  - `cookieStaysInsideNonSquareBounds`: every point of `cookiePoints(200f, 100f, 9, 0.07f)` has `x` in `0..200` and `y` in `0..100`, and the outline is centred at (100, 50).
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.design.CookieShapeTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement.**
  - `cookiePoints` takes `half = min(w, h) / 2`, `R = half / (1 + depth)`, `r(t) = R * (1 + depth * cos(lobes * t))`, and points `centre + r(t)·(cos t, sin t)` for `t = 2π·i/steps`, `i in 0 until steps`.
  - `CookieShape.createOutline` returns `Outline.Generic` built from a `Path` through those points.
  - `Shape.kt` defines `DevSuhbatShapes`.
- [ ] **Step 4:** Run the tests and expect PASS.
- [ ] **Step 5: Commit** with `git commit -m "feat(design): add expressive shapes and cookie shape"`.

### Task 4: Motion tokens, reduced motion, haptics, theme wiring

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/design/Motion.kt`
- Create: `app/src/main/java/uz/devsuhbat/ui/design/Haptics.kt`
- Modify: `app/src/main/java/uz/devsuhbat/ui/theme/Theme.kt` (`DevSuhbatTheme` passes `typography = DevSuhbatTypography` and `shapes = DevSuhbatShapes`, and provides `LocalReducedMotion`)
- Test: `app/src/test/java/uz/devsuhbat/ui/design/MotionTest.kt`, `app/src/test/java/uz/devsuhbat/ui/design/HapticsTest.kt`

**Interfaces:**
- Produces: `object DsMotion { fun <T> spatialFast(): SpringSpec<T>; fun <T> spatialDefault(): SpringSpec<T>; fun <T> spatialSlow(): SpringSpec<T>; fun <T> effectsFast(): SpringSpec<T>; fun <T> effectsDefault(): SpringSpec<T> }` with the (dampingRatio, stiffness) pairs from spec 3.4: (0.6, 800), (0.8, 380), (0.8, 200), (1.0, 3800), (1.0, 1600).
- Produces: `val LocalReducedMotion: ProvidableCompositionLocal<Boolean>` (default `false`), `fun isReducedMotion(animatorDurationScale: Float): Boolean`, `@Composable fun rememberReducedMotion(): Boolean` (reads `Settings.Global.ANIMATOR_DURATION_SCALE`, default `1f`).
- Produces: `enum class HapticEvent { SELECT, CORRECT, WRONG }`, `fun hapticConstant(event: HapticEvent, sdkInt: Int): Int`, `@Composable fun rememberHaptics(): (HapticEvent) -> Unit` (`LocalView.current.performHapticFeedback(hapticConstant(event, Build.VERSION.SDK_INT))`).

- [ ] **Step 1: Write the failing tests.**
  - `MotionTest`: `isReducedMotion(0f) == true`, `isReducedMotion(1f) == false`, `isReducedMotion(0.5f) == false`. Also check `DsMotion.spatialFast<Float>().dampingRatio == 0.6f` and `stiffness == 800f`, and `spatialSlow<Float>().stiffness == 200f`.
  - `HapticsTest`: at SDK 30, `SELECT → CLOCK_TICK`, `CORRECT → CONFIRM`, `WRONG → REJECT`. At SDK 29, `CORRECT → VIRTUAL_KEY` and `WRONG → LONG_PRESS`. Use the `HapticFeedbackConstants` fields.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.design.*"`. Expect a compile FAIL for the new tests.
- [ ] **Step 3: Implement** `Motion.kt`, `Haptics.kt` and the theme wiring. `DevSuhbatTheme(dark, content)` keeps its signature.
- [ ] **Step 4:** Run `timeout 600 ./gradlew test :app:assembleDebug` and expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** with `git commit -m "feat(design): add motion tokens, reduced motion and haptics"`.

### Task 5: Basic components: SectionCard, ExpressiveButton, StatTile

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/design/SectionCard.kt`, `ExpressiveButton.kt`, `StatTile.kt`
- Create: `app/src/main/java/uz/devsuhbat/ui/design/DesignPreviews.kt` (light and dark `@Preview` of every `ui/design` component, wrapped in `DevSuhbatTheme`; later tasks append to it)

**Interfaces:**
- Consumes: `DsMotion`, `LocalReducedMotion`, `LocalExtraColors`.
- Produces:
  - `@Composable fun SectionCard(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surfaceContainerLowest, contentColor: Color = MaterialTheme.colorScheme.onSurface, content: @Composable ColumnScope.() -> Unit)`: shape `large`, padding 18dp, vertical gap 10dp.
  - `enum class ButtonTone { PRIMARY, SUCCESS, OUTLINED }`.
  - `@Composable fun ExpressiveButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tone: ButtonTone = ButtonTone.PRIMARY, enabled: Boolean = true, trailingIcon: ImageVector? = null)`: full width, `heightIn(min = 56.dp)`, corner 28dp → 16dp while pressed and scale 1 → 0.96, both `DsMotion.spatialFast()`. Colours:
    - PRIMARY: `primary`/`onPrimary`;
    - SUCCESS: `success`/`onSuccess`;
    - OUTLINED: transparent with a 1.5dp `outline` border and `primary` text;
    - disabled: `onSurface` 12% fill and `onSurface` 38% text.
  - `@Composable fun StatTile(value: String, label: String, modifier: Modifier = Modifier, container: Color = MaterialTheme.colorScheme.surfaceContainerLowest, content: Color = MaterialTheme.colorScheme.onSurface, enterDelayMillis: Int = 0)`: corner 20dp, value in `headlineMedium`, label in `labelMedium`. On first composition it rises from 24dp and fades in with `spatialDefault` after `enterDelayMillis`, and appears at once when `LocalReducedMotion` is on.

- [ ] **Step 1: Implement** the three components and their previews.
- [ ] **Step 2:** Run `timeout 600 ./gradlew :app:assembleDebug` and expect BUILD SUCCESSFUL. Open `DesignPreviews.kt` in the IDE preview, or skip the visual check until Task 9.
- [ ] **Step 3: Commit** with `git commit -m "feat(design): add section card, expressive button and stat tile"`.

### Task 6: ReadinessRing and WavyProgress

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/design/ReadinessRing.kt`, `WavyProgress.kt`
- Modify: `DesignPreviews.kt`
- Test: `app/src/test/java/uz/devsuhbat/ui/design/WavePointsTest.kt`

**Interfaces:**
- Consumes: `CookieShape`/`cookiePoints`, `DsMotion`, `LocalReducedMotion`.
- Produces:
  - `fun wavePoints(width: Float, centerY: Float, amplitude: Float, wavelength: Float, phase: Float, step: Float = 2f): List<Offset>`: points from `x = 0` to `x = width` inclusive, `y = centerY + amplitude * sin(2π (x + phase) / wavelength)`.
  - `@Composable fun WavyProgress(fraction: Float, modifier: Modifier = Modifier)`: height 20dp, stroke 5dp with round caps, amplitude 3.2dp, wavelength 16dp. The active wave is `primary` up to `fraction`, and a flat `outlineVariant` track follows after a 10dp gap. `fraction` is animated with `spatialDefault`. The phase loops 0 → wavelength over 900ms with `LinearEasing`; the loop is stopped under reduced motion.
  - `@Immutable data class RingColors(val ring: Color, val track: Color, val cookie: Color, val text: Color)`.
  - `object RingDefaults { @Composable fun colors(): RingColors; @Composable fun onPrimaryColors(): RingColors }`:
    - `colors()`: `primary`, `surfaceContainer`, `primaryContainer`, `onSurface`;
    - `onPrimaryColors()`: `onPrimary`, `onPrimary` 22%, `onPrimary` 12%, `onPrimary`.
  - `@Composable fun ReadinessRing(fraction: Float, centerText: String, modifier: Modifier = Modifier, size: Dp = 120.dp, colors: RingColors = RingDefaults.colors(), centerStyle: TextStyle = MaterialTheme.typography.headlineSmall)`:
    - the cookie is drawn at the full `size` and rotates 360° every 24s, except under reduced motion;
    - the ring diameter is 66% of `size`, stroke 7% of `size`, starting at the top;
    - the sweep animates from 0 to `fraction` with `spatialSlow` on first composition.

- [ ] **Step 1: Write the failing test** `WavePointsTest`:
  - `startsAtZeroAndEndsAtWidth`: first `x == 0f`, last `x == 200f` for `width = 200f, step = 2f`.
  - `staysWithinAmplitude`: every `y` is in `10f ± 3.2f`.
  - `repeatsEveryWavelength`: `y` at `x` equals `y` at `x + 16f` within `0.01f`.
  - `phaseShiftsTheWave`: `y(x = 0, phase = 4f) == y(x = 4f, phase = 0f)`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.design.WavePointsTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement** both components and add their previews: ring at 0.5 in both colour sets, wave at 0.17 and 0.83.
- [ ] **Step 4:** Run `timeout 600 ./gradlew test :app:assembleDebug` and expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** with `git commit -m "feat(design): add readiness ring and wavy progress"`.

### Task 7: Confetti

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/design/Confetti.kt`
- Modify: `DesignPreviews.kt`
- Test: `app/src/test/java/uz/devsuhbat/ui/design/ConfettiTest.kt`

**Interfaces:**
- Produces:
  - `data class ConfettiParticle(val xFraction: Float, val widthDp: Float, val heightDp: Float, val colorIndex: Int, val spinDegrees: Float, val durationMillis: Int, val delayMillis: Int)`.
  - `fun confettiParticles(count: Int = 40, seed: Int = 7): List<ConfettiParticle>`, generated with `kotlin.random.Random(seed)`.
  - `fun confettiVisible(play: Boolean, reducedMotion: Boolean): Boolean`.
  - `@Composable fun Confetti(play: Boolean, modifier: Modifier = Modifier)`: draws on a `Canvas` filling `modifier`. Pieces fall from the top edge (−24dp) to below the bottom edge while rotating by `spinDegrees`, and fade to 20% in the last 30% of their time. Colours by `colorIndex`: `primary`, `streak`, `success`, `primaryContainer`, `Color(0xFFFFC83D)`. It plays once when `play` turns true.

- [ ] **Step 1: Write the failing test** `ConfettiTest`:
  - `createsTheRequestedCount`: size `40`.
  - `valuesStayInRange`: `xFraction` in `0..1`, `widthDp` in `6..12`, `heightDp` in `10..18`, `colorIndex` in `0..4`, `spinDegrees` in `-360..360`, `durationMillis` in `2200..3600`, `delayMillis` in `500..1200`.
  - `sameSeedSameParticles`: two calls with seed 7 are equal; seed 8 differs.
  - `confettiShowsOnlyWhenPlayingAndMotionAllowed`: only `(play = true, reduced = false)` gives `true`.
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.design.ConfettiTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement** the component and its preview.
- [ ] **Step 4:** Run the tests and expect PASS.
- [ ] **Step 5: Commit** with `git commit -m "feat(design): add confetti"`.

### Task 8: OptionCard and FeedbackSheet

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/design/OptionCard.kt`, `FeedbackSheet.kt`
- Modify: `DesignPreviews.kt`
- Test: `app/src/test/java/uz/devsuhbat/ui/design/OptionStateTest.kt`

Note: `ui/common/QuestionViews.kt` already has an `OptionCard`. This one lives in `uz.devsuhbat.ui.design`, and the old one is deleted in 6c. Neither file imports the other.

**Interfaces:**
- Consumes: `InlineCodeText` (`ui/common`), `CookieShape`, `DsMotion`, `LocalReducedMotion`, `LocalExtraColors`.
- Produces:
  - `enum class OptionState { IDLE, SELECTED, WRONG, CORRECT, DIMMED; val interactive: Boolean }`, where `interactive` is true only for IDLE and SELECTED.
  - `fun shouldShake(previous: OptionState?, current: OptionState): Boolean`, true only when `current == WRONG && previous != WRONG && previous != null`.
  - `@Composable fun OptionCard(text: String, letter: Char, state: OptionState, multi: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)`, described below.
  - `enum class FeedbackTone { WRONG, CORRECT }`.
  - `@Composable fun FeedbackSheet(tone: FeedbackTone, title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`, described below.

**OptionCard:**
- Size and shape: `heightIn(min = 64.dp)`, padding 12/16dp. The card corner is 22dp; it animates to 32dp when SELECTED and 34dp when CORRECT, with `spatialDefault`.
- Badge: 38dp, corner 13dp. When SELECTED or CORRECT it morphs to 19dp (a circle) for `single`, or to 10dp for `multi`. The badge shows `letter`; WRONG shows `Icons.Rounded.Close`; CORRECT shows `Icons.Rounded.Check`, which scales in with `spatialFast`.
- Colours by state:

  | State | Container | Border | Badge fill / content |
  |---|---|---|---|
  | IDLE | `surfaceContainerLowest` | `outlineVariant` | `surfaceContainer` / `primary` |
  | SELECTED | `primaryContainer` | `primary` | `primary` / `onPrimary` |
  | WRONG | `errorContainer` at 35% over `surfaceContainerLowest` | `error` at 35% | `errorContainer` / `error` |
  | CORRECT | `successContainer` | `success` | `success` / `onSuccess` |
  | DIMMED | as IDLE | as IDLE | as IDLE, with alpha 0.45 on the whole card |

- Text: WRONG text is drawn with line-through.
- Motion:
  - shake: when `shouldShake(previous, state)` is true, horizontal keyframes 0, −9, 9, −9, 9, −4, 0 dp over 550ms, skipped under reduced motion; `previous` is held in `remember`;
  - CORRECT pops the scale 1 → 1.045 → 1 with `spatialFast`.
- Clicks and accessibility: clickable only when `state.interactive`. Semantics: `Role.Checkbox` for `multi`, otherwise `Role.RadioButton`, with `selected = state == SELECTED || state == CORRECT`.

**FeedbackSheet:**
- Shape and colours: corner 28dp, padding 18dp, gap 8dp. WRONG uses `errorContainer`/`onErrorContainer`; CORRECT uses `successContainer`/`onSuccessContainer`.
- Header row: WRONG shows a 34dp white rounded square (corner 12dp) with `Icons.Rounded.Lightbulb` in `error`. CORRECT shows a 36dp `CookieShape` in `success` with a check mark in `onSuccess`. The title is `titleMedium`.
- Entrance: on first composition it slides in from its full height and fades in with `spatialDefault`; under reduced motion it appears at once.

- [ ] **Step 1: Write the failing test** `OptionStateTest`:
  - `onlyIdleAndSelectedAreInteractive`;
  - `shakesOnlyOnTransitionIntoWrong`, with these cases:

    | previous | current | shakes |
    |---|---|---|
    | SELECTED | WRONG | true |
    | IDLE | WRONG | true |
    | WRONG | WRONG | false |
    | null | WRONG | false (first composition, e.g. scrolled back into view) |
    | WRONG | IDLE | false |
- [ ] **Step 2:** Run `timeout 500 ./gradlew :app:testDebugUnitTest --tests "uz.devsuhbat.ui.design.OptionStateTest"`. Expect a compile FAIL.
- [ ] **Step 3: Implement** both components and add previews: all five option states for `single` and `multi`, and both sheet tones with sample Uzbek text taken from question `android.kotlin.009`.
- [ ] **Step 4:** Run `timeout 600 ./gradlew test :app:assembleDebug` and expect BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** with `git commit -m "feat(design): add option card and feedback sheet"`.

### Task 9: Verify on device, measure size, open PR

**Files:** none changed unless a check fails.

- [ ] **Step 1:** Run `timeout 900 ./gradlew test :app:assembleDebug :app:assembleRelease`. Expect BUILD SUCCESSFUL and record the number of tests. Compare the size of `app/build/outputs/apk/release/app-release.apk` with the baseline of **2 257 909 bytes** (stage 5 release) and record the difference.
- [ ] **Step 2:** Install the debug APK on `emulator-5554`. Walk through Onboarding (or Settings › profile), Home, Topics, a practice session (one wrong and one correct answer) and Settings, in light and then dark theme (Settings › theme). Expected:
  - the new background, primary colour and fonts everywhere;
  - inline code in JetBrains Mono with the new code colours;
  - no unreadable text.

  Take one screenshot per theme for the PR.
- [ ] **Step 3:** On the emulator, run `adb -s emulator-5554 shell settings put system font_scale 2.0` and repeat the session screen. Expected: no clipped text in existing screens. Then restore with `font_scale 1.0`. Run `adb -s emulator-5554 shell settings put global animator_duration_scale 0` and confirm the app still works; restore with `1`.
- [ ] **Step 4:** Push `stage6/6a-design-system` and open a PR to `master`. The Uzbek body lists:
  - what changed;
  - the test count;
  - the APK size difference;
  - what was checked on the emulator;
  - that screen layouts change from 6b on.

  End with the Claude Code line, and wait for the user to merge.
