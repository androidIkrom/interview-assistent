# Stage 5 — Play Store Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make DevSuhbat ready for its first Google Play release: fix the on-device findings, remove the "correct answer is the longest option" bias, ship the chosen icon, a daily reminder, progress export/import, a signed release bundle and the Play Console materials.

**Architecture:** Pure logic goes into `core:engine` (or a UI-free class in `:app`) and is built test-first; Android pieces (WorkManager, SAF, notifications, signing) are thin wrappers checked on the emulator. Each part (5a, 5g, 5b, 5c, 5d, 5e, 5f) is its own branch and PR, merged by the user before the next starts.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room 2.8, DataStore, kotlinx.serialization, WorkManager (new in 5c), Gradle Kotlin DSL, R8.

**Spec:** `docs/superpowers/specs/2026-10-03-stage5-play-store-design.md`

## Global Constraints

- Package `uz.devsuhbat`, app name `DevSuhbat`; `versionCode = 1`, `versionName = "1.0.0"` (set in 5b).
- No `INTERNET` permission, no account, no backend; everything stays on the device.
- UI text is Uzbek (Latin); technical terms and code stay English. New strings go to `res/values/strings.xml`; apostrophes are escaped (`\'`).
- Every question keeps `"reviewed": false`; content must pass `ContentValidator` (see the batch recipe in memory: single = exactly one correct, 2–5 options; multi = ≥2 correct and ≥1 wrong; every wrong option has a hint; a hint must not contain a correct option's text of ≥5 characters; `true_false` keeps "Ha"/"Yo'q").
- Gradle: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"; export PATH="$JAVA_HOME/bin:$PATH"`, run from `/d/projects/devsuhbat` in the Bash tool, wrapped in `timeout`; trust parsed XML results only after "BUILD SUCCESSFUL".
- Commits: no co-author line. PR bodies in Uzbek, end with the Claude Code line. Branch per part, PR to `master`, the user merges.
- Emulator: always `adb -s emulator-5554`; never touch the physical phone; after boot run `am kill-all` before testing (2 GB RAM).
- Keystore and passwords never enter git or the chat.

## Review Focus

1. A field whose own topics are short at the chosen level (e.g. Junior with few field questions): the mock must still fill 25 from common topics, never return fewer while questions exist — test in Task 1.
2. Mistakes scheduled for several different future days: the Home card names the earliest day and only that day's count — test in Task 2.
3. Reminder at a time already passed today, and across midnight: the next delay points to tomorrow, never zero or negative — test in Task 9.
4. Importing a backup whose question ids no longer exist in the content: the import succeeds and skips them; a backup with a newer `version` is refused without touching data — tests in Task 10.
5. A rewritten question where the shortened correct text now appears inside a wrong option's hint: the validator must fail the build — covered by `allContentPassesValidator` in every 5g batch (Task 4).

---

## Part 5a — on-device findings (branch `stage5/5a-fixes`)

### Task 1: Mock takes at least 60% from the field's own topics

**Files:**
- Modify: `core/engine/src/main/kotlin/uz/devsuhbat/engine/QuestionPicker.kt` (`mock`)
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/QuestionPickerTest.kt`

**Interfaces:**
- Produces: `QuestionPicker.mock(pool: List<Question>, maxLevel: Level, random: Random, count: Int = MOCK_SIZE, isCommon: (Question) -> Boolean = { it.topicId.startsWith("core.") }): List<Question>`; constant `MOCK_FIELD_SHARE = 0.6`.

- [ ] **Step 1: Write the failing tests** (ids `f.NNN` = field topic, `core.x.NNN` = common topic; helper `many(prefix, level, n)` already exists)

```kotlin
@Test fun mockTakesSixtyPercentFromFieldTopics() {
    val pool = many("f", Level.MIDDLE, 40) + many("core.x", Level.MIDDLE, 40)
    val picked = QuestionPicker.mock(pool, Level.MIDDLE, Random(3))
    assertEquals(25, picked.size)
    assertEquals(15, picked.count { !it.id.startsWith("core.") })
}
@Test fun mockFillsFromCommonWhenFieldIsShort() {
    val picked = QuestionPicker.mock(many("f", Level.MIDDLE, 6) + many("core.x", Level.MIDDLE, 40), Level.MIDDLE, Random(3))
    assertEquals(25, picked.size)
    assertEquals(6, picked.count { !it.id.startsWith("core.") })
}
@Test fun mockFillsFromFieldWhenCommonIsShort() {
    val picked = QuestionPicker.mock(many("f", Level.MIDDLE, 40) + many("core.x", Level.MIDDLE, 4), Level.MIDDLE, Random(3))
    assertEquals(21, picked.count { !it.id.startsWith("core.") })
}
@Test fun mockKeepsLevelMixInsideEachPart() {
    val pool = many("f.m", Level.MIDDLE, 30) + many("f.j", Level.JUNIOR, 30) +
        many("core.m", Level.MIDDLE, 30) + many("core.j", Level.JUNIOR, 30)
    val picked = QuestionPicker.mock(pool, Level.MIDDLE, Random(3))
    assertEquals(15, picked.count { it.level == Level.MIDDLE })
}
```

- [ ] **Step 2:** Run `timeout 400 ./gradlew :core:engine:test --tests "uz.devsuhbat.engine.QuestionPickerTest"` — expect the first and third new tests to FAIL (current mock ignores topic groups).
- [ ] **Step 3: Implement.** Split `eligible(pool, maxLevel)` into field and common parts with `isCommon`; field gets `max(round(count * 0.6), count - common.size)` capped by its size, common gets the rest capped by its size; inside each part reuse today's level split (60% exactly `maxLevel`, fill from lower) as a private `byLevel(questions, maxLevel, n, random)`; shuffle the union. All existing mock tests must still pass (their ids have no `core.` prefix).
- [ ] **Step 4:** Run the same test command — expect PASS, then `timeout 500 ./gradlew test` — BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `fix(mock): take at least 60% of mock questions from the field's own topics`

### Task 2: Home mistakes card says when mistakes come back

**Files:**
- Modify: `core/engine/src/main/kotlin/uz/devsuhbat/engine/QuestionPicker.kt` (new `nextReview`)
- Modify: `app/src/main/java/uz/devsuhbat/ui/home/HomeViewModel.kt` (`HomeUiState.nextReview`)
- Modify: `app/src/main/java/uz/devsuhbat/ui/home/HomeScreen.kt` (`MistakesCard`), `app/src/main/res/values/strings.xml`
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/QuestionPickerTest.kt`, `app/src/test/java/uz/devsuhbat/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Produces: `data class NextReview(val inDays: Int, val count: Int)` in `uz.devsuhbat.engine`; `QuestionPicker.nextReview(pool: List<Question>, maxLevel: Level, states: Map<String, QuestionState>, today: Long): NextReview?` — null when nothing is scheduled after today; `HomeUiState.nextReview: NextReview?`.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun nextReviewIsTheEarliestFutureDay() {
    val states = mapOf("j.001" to state("j.001", dueDay = 12), "j.002" to state("j.002", dueDay = 12), "j.003" to state("j.003", dueDay = 15))
    assertEquals(NextReview(inDays = 2, count = 2), QuestionPicker.nextReview(pool, Level.JUNIOR, states, today = 10))
}
@Test fun nextReviewIgnoresDueAndOutOfLevelQuestions() {
    val states = mapOf("j.001" to state("j.001", dueDay = 9), "x.001" to state("x.001", dueDay = 11))
    assertNull(QuestionPicker.nextReview(pool, Level.JUNIOR, states, today = 10))
}
```
plus in `HomeViewModelTest`: with one answered question due tomorrow and none due today, `state.value.nextReview == NextReview(1, 1)` and `dueCount == 0`.

- [ ] **Step 2:** Run both test classes — FAIL (`nextReview` undefined).
- [ ] **Step 3: Implement** `nextReview` (states of eligible questions with `dueDay > today`, min day, count on it) and fill `HomeUiState.nextReview` in the same `combine`. `MistakesCard(dueCount, nextReview, onMistakes)`: due > 0 → existing text; else `nextReview` with `inDays == 1` → `home_mistakes_tomorrow` = `"%d ta savol ertaga qaytadi"`; `inDays > 1` → `home_mistakes_later` = `"%1$d ta savol %2$d kundan keyin qaytadi"`; null → existing "no mistakes" text.
- [ ] **Step 4:** `timeout 500 ./gradlew test` — BUILD SUCCESSFUL, 0 failures.
- [ ] **Step 5: Commit** `feat(home): tell when the next mistakes come back`

### Task 3: Memory check, progress stop dot, emulator pass, PR

**Files:**
- Modify: `app/src/main/java/uz/devsuhbat/ui/home/HomeScreen.kt:141`, `app/src/main/java/uz/devsuhbat/ui/session/SessionScreen.kt:161` (only if Material 3 version has `drawStopIndicator`)

- [ ] **Step 1:** Install debug build; on Home run `adb -s emulator-5554 shell dumpsys meminfo uz.devsuhbat | grep -E "TOTAL PSS|TOTAL RSS|Java Heap|Native Heap"`; repeat after a 10-question session and after a mock. Record the numbers for the PR. Only if TOTAL PSS > 150 MB: find the largest heap user (`am dumpheap` is out of scope; check whether `ContentStore` caches topics of other fields after a field switch) and add a task to the PR; otherwise no code change.
- [ ] **Step 2:** If `LinearProgressIndicator(progress, …, drawStopIndicator = {})` compiles with the project's Material 3, pass `drawStopIndicator = {}` in both places; otherwise skip and note it.
- [ ] **Step 3:** Emulator check: Android Middle mock shows ≥15 field-topic questions on the result screen's topic list; after a practice session with mistakes the Home card says "… ertaga qaytadi".
- [ ] **Step 4:** `timeout 500 ./gradlew test :app:assembleDebug` — BUILD SUCCESSFUL; push `stage5/5a-fixes`, PR "Stage 5a: qurilma topilmalari" (body: findings T2–T5, memory numbers, test count).

---

## Part 5g — option-length bias (branches `stage5/5g-<group>`)

### Task 4: Length-bias test and the first batch

**Files:**
- Create: `tools/option_length_report.py` (prints per-topic and total shares; used before and after each batch)
- Modify: `app/src/test/java/uz/devsuhbat/content/ContentAssetsTest.kt`
- Modify: the batch's `app/src/main/assets/content/questions/*.json`

**Interfaces:**
- Produces in `ContentAssetsTest`: `lengthFixedTopics: Set<String>`; constants `MAX_LONGEST_SHARE_TOPIC = 0.5`, `MAX_LONGEST_SHARE_BANK = 0.35`; tests `fixedTopicsDoNotRevealAnswersByLength()` and `bankDoesNotRevealAnswersByLength()`.

Definitions (both the script and the test): a **single** question (kind ≠ `true_false`) "leaks" when its correct option's text is strictly longer than every wrong option's text; a **multi** question leaks when every correct option is strictly longer than every wrong option. Share = leaking questions / questions considered.

- [ ] **Step 1:** Add the two tests. `fixedTopicsDoNotRevealAnswersByLength` asserts, for each topic in `lengthFixedTopics`, share ≤ 0.5 (message names the topic and its share). `bankDoesNotRevealAnswersByLength` asserts the bank share ≤ 0.35 and is guarded by `assumeTrue(lengthFixedTopics.size == ALL_TOPIC_COUNT)` (88) so it only runs once every topic is fixed.
- [ ] **Step 2:** Add the first batch's topics to `lengthFixedTopics`; run `ContentAssetsTest` — FAIL with the topic's current share.
- [ ] **Step 3:** Rewrite the batch: for each leaking question shorten the correct option to its core claim (aim for the length of the median wrong option), move the removed detail into the front of `explanation`, and lengthen weak wrong options into plausible, specific statements; adjust a hint only when it no longer matches its option. Meaning and the correct choice do not change. Run `python tools/option_length_report.py <topics>` until every topic is ≤ 0.5 (target ≤ 0.35).
- [ ] **Step 4:** In a separate tool call: `timeout 500 ./gradlew test :app:assembleDebug` — BUILD SUCCESSFUL, 0 failures.
- [ ] **Step 5:** Commit `fix(content): balance option lengths in <group>`; push; PR "Stage 5g-N: <group> variant uzunliklari" with before/after shares.

### Task 5: Remaining batches

Same steps as Task 4, one PR per group, in this order (topics per group as in `catalog.json`): Android → Frontend → Python → Node.js → Java → Flutter → Go → PHP → .NET → iOS → QA → DevOps → Data/ML → `core.mobile`+`core.git`+`core.http` → `core.sql`+`core.dsa` → `core.oop`+`core.security`+`core.testing` → `core.sysdesign`+`core.aicode`. The last PR also makes `bankDoesNotRevealAnswersByLength` run and pass.

---

## Part 5b — icon, version, error report (branch `stage5/5b-icon`)

### Task 6: Launcher icon and Play icon

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/values/colors.xml` (`launcher_background` = `#13235B`), `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (add `<monochrome android:drawable="@drawable/ic_launcher_monochrome"/>`)
- Create: `app/src/main/res/drawable/ic_launcher_monochrome.xml`, `docs/play/icon-512.svg`, `docs/play/icon-512.png`
- Modify: `app/build.gradle.kts` (`versionName = "1.0.0"`)

- [ ] **Step 1:** Convert the `badge` variant of `Icon.dc.html` (108-unit SVG) to vector drawables: background colour layer, foreground paths without the background rect, monochrome = foreground silhouette in one colour. Keep every path inside the 66 dp safe circle.
- [ ] **Step 2:** Render `icon-512.png` (512×512, sRGB, opaque full square, no rounded corners) from `icon-512.svg` with any available SVG renderer (Python `cairosvg` or headless Chrome); check size and dimensions with Python PIL.
- [ ] **Step 3:** Install on emulator; screenshot launcher (normal and themed icons on) and compare with the artifact.
- [ ] **Step 4:** Commit `feat(brand): add the DevSuhbat launcher and Play icons, version 1.0.0`.

### Task 7: "Report a mistake" share action

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/ui/common/ReportIssue.kt`
- Modify: `SessionScreen.kt`, `MockScreen.kt` (top bar action), `strings.xml`
- Test: `app/src/test/java/uz/devsuhbat/ui/common/ReportIssueTest.kt`

**Interfaces:**
- Produces: `fun issueReportText(questionId: String, prompt: String, versionName: String): String` and `fun shareIssueReport(context: Context, text: String)` (`ACTION_SEND`, `text/plain`, chooser title `report_issue_chooser`).

- [ ] **Step 1:** Test `issueReportText("android.kotlin.003", "Bu kod nima chiqaradi?", "1.0.0")` contains the id, the prompt, `"DevSuhbat 1.0.0"`, and ends with the line `"Izoh: "`.
- [ ] **Step 2:** Run — FAIL. **Step 3:** Implement; add an `IconButton` (outlined flag icon, content description `"Xato haqida xabar berish"`) to both top bars, passing the current question.
- [ ] **Step 4:** `./gradlew test`; emulator: tap the action, the share sheet opens with the text. **Step 5:** Commit `feat(session): let users report a mistake in a question`; PR "Stage 5b: ikonka va xato xabari".

---

## Part 5c — daily reminder (branch `stage5/5c-reminder`)

### Task 8: Reminder settings and schedule math

**Files:**
- Create: `core/engine/src/main/kotlin/uz/devsuhbat/engine/ReminderSchedule.kt`
- Modify: `app/src/main/java/uz/devsuhbat/data/SettingsRepository.kt` (`reminder_enabled` default false, `reminder_minutes` default 1200; `UserSettings.reminderEnabled`, `UserSettings.reminderMinutes`; `setReminder(enabled: Boolean, minutes: Int)`)
- Test: `core/engine/src/test/kotlin/uz/devsuhbat/engine/ReminderScheduleTest.kt`, `app/src/test/java/uz/devsuhbat/data/SettingsRepositoryTest.kt`

**Interfaces:**
- Produces: `object ReminderSchedule { fun delayUntilNext(now: LocalDateTime, minutesOfDay: Int): Duration; fun practicedToday(lastFinishedAt: Instant?, now: Instant, zone: ZoneId): Boolean }`.

- [ ] **Step 1: Failing tests:** `delayUntilNext(2026-10-03T19:30, 1200) == 30 min`; `delayUntilNext(2026-10-03T20:00, 1200) == 24 h`; `delayUntilNext(2026-10-03T23:50, 1200) == 20 h 10 min`; `practicedToday(null, …) == false`; finished at 00:05 local today → true; finished yesterday 23:59 → false. Settings test: defaults are `false` / `1200`; one `setReminder(true, 480)` is read back (one DataStore write per test).
- [ ] **Step 2:** Run — FAIL. **Step 3:** Implement. **Step 4:** `./gradlew test` — PASS. **Step 5:** Commit `feat(reminder): add reminder settings and schedule math`.

### Task 9: Worker, notification, Settings UI

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (`androidx.work:work-runtime-ktx`, latest stable)
- Create: `app/src/main/java/uz/devsuhbat/reminder/ReminderWorker.kt`, `app/src/main/java/uz/devsuhbat/reminder/ReminderScheduler.kt`
- Modify: `AndroidManifest.xml` (`POST_NOTIFICATIONS`), `DevSuhbatApp.kt` (reschedule on start), `ProgressRepository.kt` (`suspend fun lastFinishedAt(): Instant?`), `SettingsScreen.kt`, `strings.xml`
- Test: `app/src/test/java/uz/devsuhbat/data/ProgressRepositoryTest.kt` (`lastFinishedAt`)

**Interfaces:**
- Consumes: `ReminderSchedule`, `UserSettings.reminderEnabled/reminderMinutes`.
- Produces: `ReminderScheduler.apply(context: Context, enabled: Boolean, minutesOfDay: Int)` — enqueues unique work `"daily_reminder"` with `ExistingWorkPolicy.REPLACE` and the computed delay, or cancels it.

- [ ] **Step 1:** Failing test: `lastFinishedAt()` is null on an empty DB and equals the newest session's `finishedAt` after two `logSession` calls.
- [ ] **Step 2:** Implement `lastFinishedAt`; worker: if `practicedToday` → no notification; else post on channel `daily_reminder` (title `"DevSuhbat"`, text `"Bugun 10 ta savol yechamizmi?"`, tap opens `MainActivity`), then `ReminderScheduler.apply` for tomorrow. Settings: switch "Kunlik eslatma" + time row (Material 3 `TimePicker` dialog); turning on asks `POST_NOTIFICATIONS` on API 33+; if refused, keep it off and show `"Bildirishnomalarga ruxsat berilmagan. Uni tizim sozlamalarida yoqing."`.
- [ ] **Step 3:** `./gradlew test` — PASS. Emulator: set the time 2 minutes ahead, wait, the notification appears; finish a session, set again — no notification; deny permission — switch stays off with the message.
- [ ] **Step 4:** Commit `feat(reminder): daily reminder with WorkManager`; PR "Stage 5c: kunlik eslatma".

---

## Part 5d — export/import (branch `stage5/5d-backup`)

### Task 10: Backup codec and repository snapshot

**Files:**
- Create: `app/src/main/java/uz/devsuhbat/data/Backup.kt` (`@Serializable data class Backup(format, version, exportedAt, settings, questionStates, sessions, mockTopicResults)` and `object BackupCodec`)
- Modify: `ProgressRepository.kt` (`suspend fun snapshot(): Backup`, `suspend fun restore(backup: Backup, knownQuestionIds: Set<String>)`), `AppDatabase.kt` (DAO: read all, replace all in one `@Transaction`), `SettingsRepository.kt` (`restore(settings)`)
- Test: `app/src/test/java/uz/devsuhbat/data/BackupCodecTest.kt`, `app/src/test/java/uz/devsuhbat/data/BackupRestoreTest.kt` (Robolectric)

**Interfaces:**
- Produces: `BackupCodec.encode(backup: Backup): String`; `BackupCodec.decode(json: String): Result<Backup>` failing with `BackupError.Malformed`, `BackupError.WrongFormat`, `BackupError.UnsupportedVersion`; constants `FORMAT = "devsuhbat-backup"`, `VERSION = 1`.

- [ ] **Step 1: Failing tests:** encode→decode returns an equal `Backup`; `decode("{")` → `Malformed`; `format = "other"` → `WrongFormat`; `version = 2` → `UnsupportedVersion`. Robolectric: after `restore`, the DB holds exactly the backup's rows; ids not in `knownQuestionIds` are skipped; a decode failure leaves existing rows untouched (restore is never called).
- [ ] **Step 2:** Run — FAIL. **Step 3:** Implement (`Json { ignoreUnknownKeys = true }`; restore = delete all + insert in one Room transaction). **Step 4:** `./gradlew test` — PASS. **Step 5:** Commit `feat(backup): add progress backup codec and restore`.

### Task 11: Export/import in Settings

**Files:**
- Modify: `SettingsScreen.kt`, `strings.xml`

- [ ] **Step 1:** Two rows: "Progressni eksport qilish" (`CreateDocument("application/json")`, file name `devsuhbat-backup-YYYY-MM-DD.json`) and "Progressni import qilish" (`OpenDocument(arrayOf("application/json", "text/plain"))` → confirm dialog "Joriy progress almashtiriladi. Davom etasizmi?" → restore); Snackbar for success and for each `BackupError` (`"Fayl buzilgan"`, `"Bu DevSuhbat zaxira fayli emas"`, `"Bu fayl ilovaning yangiroq versiyasida yaratilgan"`). File IO on `container.io`.
- [ ] **Step 2:** Emulator: export, reset progress, import — Home readiness returns to the exported value; import a text file with `{` — message, data unchanged.
- [ ] **Step 3:** `./gradlew test :app:assembleDebug`; commit `feat(settings): export and import progress`; PR "Stage 5d: progress eksport/import".

---

## Part 5e — release build (branch `stage5/5e-release`)

### Task 12: R8, signing, bundle

**Files:**
- Modify: `app/build.gradle.kts`, `app/proguard-rules.pro`, `.gitignore` (`keystore.properties`)
- Create (outside git): `D:\projects\devsuhbat-keys\upload.jks`, `D:\projects\devsuhbat\keystore.properties`

- [ ] **Step 1:** `release { isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }`; `signingConfigs.create("release")` read from `keystore.properties` when the file exists, else the release stays unsigned.
- [ ] **Step 2:** Generate the upload key with `keytool -genkeypair -v -keystore D:/projects/devsuhbat-keys/upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 9125` and a random password written only to `keystore.properties`; confirm `git status` does not list either file.
- [ ] **Step 3:** `timeout 900 ./gradlew bundleRelease assembleRelease` — BUILD SUCCESSFUL; `jarsigner -verify` on the AAB prints "jar verified".
- [ ] **Step 4:** Install the release APK on the emulator (uninstall debug first — different signature); smoke test onboarding, practice, mock, export/import, reminder. Add keep rules for anything R8 breaks.
- [ ] **Step 5:** Commit `build: enable R8 and release signing`; PR "Stage 5e: reliz build" (body: AAB size, how to back up the key, Play App Signing note).

---

## Part 5f — Play Console materials (branch `stage5/5f-play`)

### Task 13: Privacy policy, listing, data safety, graphics, screenshots

**Files:**
- Create: `docs/privacy/index.html` (uz + en), `docs/play/listing-uz.md`, `docs/play/data-safety.md`, `docs/play/feature-graphic.png`, `docs/play/screenshots/*.png`

- [ ] **Step 1:** Privacy policy page per spec 5f (no collection, on-device storage, export file location chosen by the user, no internet permission, optional notifications, contact email = the repo owner's — ask the user which address to publish).
- [ ] **Step 2:** Listing: title ≤ 30 chars, short description ≤ 80, full description ≤ 4000; a Python check prints the three lengths and fails on "eng yaxshi", "#1", "bepul", emoji or a job guarantee.
- [ ] **Step 3:** Feature graphic 1024×500: 2–3 variants in the design artifact, the user picks, export PNG.
- [ ] **Step 4:** Screenshots from the emulator (light theme, fresh data with some progress): Home, practice with a hint, practice correct, result, mock, topics — 1080×2400 PNG.
- [ ] **Step 5:** Data safety answers (no data collected or shared; no encryption-in-transit question applies; no deletion request flow needed).
- [ ] **Step 6:** Commit `docs(play): add privacy policy, listing and store assets`; PR "Stage 5f: Play materiallari"; then ask the user before enabling GitHub Pages (`gh api -X POST repos/androidIkrom/interview-assistent/pages -f "source[branch]=master" -f "source[path]=/docs"`) and give them the resulting URL.
