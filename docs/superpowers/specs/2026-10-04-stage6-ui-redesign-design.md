# 6-bosqich — UI redizayn (A: Expressive): dizayn spetsifikatsiyasi

Sana: 2026-10-04
Holat: 1–3-bo'limlar suhbatda kelishildi (2026-10-04). Statistika (8-bo'lim) va qolgan ekranlar (9-bo'lim) taklif sifatida yozildi; yozma spec foydalanuvchi ko'rib chiqishini kutmoqda
Asosiy spec: `2026-10-02-devsuhbat-design.md`
Vizual namuna: `docs/design/devsuhbat-ui-yonalishlari.html`, "A — Expressive" qatori

## 1. Maqsad

Ilovaning butun UI'sini Material 3 Expressive uslubiga o'tkazish. Bunga yangi rang, shrift, shakl va motion tizimi, pastki navigatsiya, yangi Statistika ekrani va motivatsion elementlar kiradi: seriya, bo'sh mavzular va tayyorlik o'sishi. O'rganish mantig'i (Leitner, sessiya, mock) o'zgarmaydi.

**Muvaffaqiyat mezoni:** barcha ekranlar light va dark rejimda mockup'dagi A uslubida. Pastki navigatsiya va Statistika ishlaydi. Seriya, bo'sh mavzular va tayyorlik o'sishi to'g'ri hisoblanadi va unit testlar bilan yopilgan. `./gradlew test :app:assembleDebug` yashil. Har bir bosqich emulatorda tekshirilgan.

## 2. Foydalanuvchi qarorlari (2026-10-04)

| Savol | Qaror |
|---|---|
| Yo'nalish | A — Expressive (Material 3 Expressive asosida) |
| Texnik yo'l | Stable `material3` 1.4 + o'zimizning yupqa dizayn tizimi. `1.5.0-alpha` Expressive API ishlatilmaydi |
| Yangi xususiyatlar | Pastki navigatsiya, seriya (streak), "Bo'sh mavzular" kartasi, natijada tayyorlik o'sishi |
| Statistika ekrani | Faollik kalendari, mavzular kuchli/bo'sh, Leitner qutilari, mock tarixi |
| "Mashqni boshlash" | Darhol aralash sessiyani boshlaydi; mavzu tanlash Mavzular tab'iga o'tadi |
| Natija sarlavhasi | ≥80%: "Zo'r natija!", ≥50%: "Yaxshi harakat", aks holda: "Davom eting" |
| Konfetti | Faqat birinchi urinishdagi to'g'ri ulushi ≥80% bo'lganda |
| Ish tartibi | Bitta spec, 5 bosqich, har biri alohida PR (6a–6e) |

### Cheklovlar

- Offline, backend yo'q, internet ruxsati so'ralmaydi. Shuning uchun shriftlar APK ichida bo'ladi.
- Ma'lumotlar bazasi sxemasi o'zgarmaydi (Room migratsiyasi yo'q). Faqat yangi `@Query` so'rovlari qo'shiladi.
- `minSdk 26` saqlanadi. Grafik va animatsiya uchun yangi kutubxona qo'shilmaydi: hammasi Compose `Canvas` va `animate*` API'lari bilan quriladi.
- UI matnlari o'zbekcha (lotin), `strings.xml` orqali. Kod va texnik terminlar inglizcha.

## 3. Dizayn tizimi (6a)

### 3.1 Ranglar

`ui/theme/Theme.kt` ichidagi `lightColorScheme` va `darkColorScheme` almashtiriladi. Kartalar `surfaceContainerLowest` rangida, fon esa `background` rangida bo'ladi.

| Rol | Light | Dark |
|---|---|---|
| primary / onPrimary | `#4433D1` / `#FFFFFF` | `#C7BFFF` / `#23157A` |
| primaryContainer / onPrimaryContainer | `#E3DEFF` / `#1A0F6B` | `#3A2CB8` / `#E3DEFF` |
| background, surface | `#F6F4FD` | `#131218` |
| surfaceContainerLowest (karta) | `#FFFFFF` | `#1C1B22` |
| surfaceContainer | `#EEEBFA` | `#24232B` |
| onSurface / onSurfaceVariant | `#1C1B22` / `#57556A` | `#E6E1EC` / `#C8C4D4` |
| outline / outlineVariant | `#D9D5EA` / `#E2DEF0` | `#4A4858` / `#34323F` |
| error / errorContainer / onErrorContainer | `#B3261E` / `#FFDAD6` / `#410E0B` | `#FFB4AB` / `#8C1D18` / `#FFDAD6` |

Material'da roli yo'q ranglar `ExtraColors` ichida (`LocalExtraColors`):

| Rol | Light | Dark |
|---|---|---|
| success / successContainer / onSuccessContainer | `#1E7A45` / `#D3F4DD` / `#0B3D20` | `#7FD8A0` / `#154D2E` / `#C9F2D6` |
| streak / streakContainer / onStreakContainer | `#F07A2B` / `#FFE6D2` / `#4A1D00` | `#FFB37A` / `#5A2A08` / `#FFE6D2` |
| codeBackground / codeText | `#E7E3FA` / `#2B1FA0` | `#2A2640` / `#CFC7FF` |

Bosh sahifadagi hero karta light rejimda `primary` fonda `onPrimary` matn bilan, dark rejimda esa `primaryContainer` fonda `onPrimaryContainer` matn bilan chiziladi. Dynamic color (Material You) qo'shilmaydi.

### 3.2 Shriftlar

Variable font fayllari `app/src/main/res/font/` ichiga joylanadi; og'irligi `FontVariation` orqali beriladi. Litsenziya: SIL OFL 1.1, matni `app/src/main/assets/licenses/` ichida saqlanadi.

| Shrift | Qayerda |
|---|---|
| Bricolage Grotesque | `display*`, `headline*`, `title*` (700–800) |
| Figtree | `body*` (400), `label*` (600–700) |
| JetBrains Mono | `InlineCodeText` va `CodeBlock` ichidagi kod (400–500) |

APK hajmi taxminan 0.6–1 MB oshishi kutiladi; 6a PR'ida aniq farq yoziladi.

### 3.3 Shakllar

- `Shapes`: extraSmall 8, small 12, medium 22, large 28, extraLarge 32 (dp).
- `CookieShape(lobes = 9, depth = 0.07f)`: scallop shaklidagi `Shape`. U readiness halqasi orqasida, natija halqasi orqasida va "To'g'ri" belgisida ishlatiladi.
- Morph: tanlangan karta va badge burchaklari `animateDpAsState` bilan o'zgaradi. Masalan, `OptionCard` 22 → 32 dp, badge 13 dp → doira.

### 3.4 Motion

`ui/design/Motion.kt` ichida spring tokenlari (M3 Expressive qiymatlari):

| Token | dampingRatio | stiffness | Qayerda |
|---|---|---|---|
| `spatialFast` | 0.6 | 800 | Bosish, badge, pop |
| `spatialDefault` | 0.8 | 380 | Karta, sheet, ekranga kirish |
| `spatialSlow` | 0.8 | 200 | Halqa, katta progress |
| `effectsFast` | 1.0 | 3800 | Rang, opacity (tez) |
| `effectsDefault` | 1.0 | 1600 | Rang, opacity |

**Kamaytirilgan motion.** Tizimda `ANIMATOR_DURATION_SCALE == 0` bo'lsa, `LocalReducedMotion = true` bo'ladi. Bunda cheksiz animatsiyalar (cookie aylanishi, pulsatsiya, to'lqin) to'xtaydi va konfetti chiqmaydi; qolgan o'tishlar Compose'ning o'zi tomonidan qisqartiriladi.

### 3.5 Haptika

`ui/design/Haptics.kt`, `LocalView.performHapticFeedback` orqali:

| Hodisa | API 30+ | API 26–29 |
|---|---|---|
| Variant tanlash | `CLOCK_TICK` | `CLOCK_TICK` |
| To'g'ri javob | `CONFIRM` | `VIRTUAL_KEY` |
| Xato javob | `REJECT` | `LONG_PRESS` |

### 3.6 Komponentlar (`ui/design`)

| Komponent | Xatti-harakati |
|---|---|
| `ReadinessRing(progress, size)` | Halqa 0 dan `spatialSlow` bilan chiziladi; orqasida aylanuvchi `CookieShape`; markazda foiz |
| `WavyProgress(fraction)` | To'lqinli chiziq (`Canvas`), to'lqin fazasi cheksiz siljiydi; to'lgan qismi `spatialDefault` bilan o'zgaradi; qolgani tekis trek |
| `OptionCard(text, letter, state, multi, onClick)` | Holatlar: oddiy, tanlangan, xato (ustidan chizilgan, shake bir marta), to'g'ri (pop), xira. `single` badge doiraga, `multi` badge yumaloq kvadratga morph bo'ladi |
| `FeedbackSheet(feedback)` | Pastdan `spatialDefault` bilan chiqadi. Xato: sarlavha, tanlangan xato variant(lar) matni va maslahat(lar), `multi` uchun "N ta to'g'ri tanlandi, M ta yetishmaydi". To'g'ri: cookie belgisi va izoh |
| `ExpressiveButton` | Bosilganda 0.96 masshtab va burchak 28 → 16 dp morph; `primary`, `success`, `outlined` variantlari |
| `StatTile(value, label, colors)` | Katta raqam va izoh; kirishda `spatialDefault` bilan ko'tariladi |
| `Confetti(play)` | `Canvas`dagi 40 ta zarracha, taxminan 3 soniya; `LocalReducedMotion` da chizilmaydi |
| `SectionCard` | 28 dp radiusli karta, `surfaceContainerLowest` fon |

Har bir komponentning light va dark `@Preview`'i bo'ladi.

## 4. Navigatsiya qobig'i (6b)

- `DevSuhbatNavHost` `Scaffold` ichiga o'raladi. Pastki `NavigationBar` 3 ta tab bilan: **Bosh** (`home`), **Mavzular** (`topics`), **Statistika** (yangi `stats`).
- Bar faqat shu 3 ta marshrutda ko'rinadi. `session/*`, `mock`, `onboarding`, `profile` va `settings` ochilganda bar yashiriladi.
- Tab'ga o'tish: `navigate(route) { popUpTo(HOME) { saveState = true }; launchSingleTop = true; restoreState = true }`.
- `TopicsScreen`dan orqaga tugmasi (`onBack`) olib tashlanadi.
- Animatsiyalar: tab'lar orasida fade-through; ichki ekranlar uchun gorizontal slide (`spatialDefault`); predictive back `targetSdk 37` bo'yicha.
- `onAgain` mantig'i o'zgarmaydi: avval `topics`, bo'lmasa `home`.

## 5. Bosh sahifa (6b)

Mockup'dagi A tartibida, yuqoridan pastga:

1. **Header.** "Bugun ham bir qadam", "DevSuhbat", sozlamalar ikonkasi (Sozlamalar ekraniga).
2. **Hero karta.** Field nomi, daraja chipi, `ReadinessRing`, "X / Y savol o'zlashtirilgan", **"Mashqni boshlash"** tugmasi. Tugma `session(MIXED)` sessiyasini ochadi.
3. **Xatolar plitkasi.** Kutayotgan savollar soni va "Takrorlash" tugmasi (`session(MISTAKES)`). Takrorlanadigan savol bo'lmasa, tugma o'rniga hozirgi `nextReview` matni chiqadi.
4. **Seriya plitkasi.** Joriy seriya ("5 kun"), shu haftaning Dushanba–Yakshanba nuqtalari. Mashq qilingan kun to'ldirilgan; bugun hali mashq bo'lmasa, bugungi nuqta pulsatsiya qiladi; kelgusi kunlar xira. Seriya 0 bo'lsa: "Bugun boshlang".
5. **Mock interview kartasi.** Savollar soni, vaqt, oxirgi natija. `mockQuestionCount < MOCK_MIN` bo'lsa, hozirgidek o'chiq holatda.
6. **"Bo'sh mavzular" kartasi.** Field mavzularidan, tanlangan darajada kamida bitta savoli borlari orasidan, o'zlashtirish foizi eng past 3 tasi. Teng foizda savoli ko'prog'i oldin. Mavzu bosilsa `session(topicId)`; "Barchasi" Mavzular tab'iga o'tkazadi. Hamma mavzu 100% bo'lsa, karta ko'rsatilmaydi.

### 5.1 Seriya qoidasi

- Kamida bitta yakunlangan sessiya bo'lgan mahalliy kalendar kuni "faol kun" hisoblanadi. Mashq, xatolar va mock sessiyalari hammasi sanaladi, barcha fieldlar bo'yicha.
- Joriy seriya: bugun faol bo'lsa, bugundan; aks holda kecha faol bo'lsa, kechadan boshlab orqaga ketma-ket faol kunlar soni. Ikkalasi ham faol bo'lmasa, 0.
- Eng uzun seriya (Statistika uchun): tarixdagi eng uzun ketma-ket faol kunlar.

### 5.2 Ma'lumot

- `core/engine/Streak.kt` (sof Kotlin): `Streak.of(activeDays: Set<Long>, today: Long): StreakInfo(current, longest, week: List<DayMark>)`. `DayMark`: `DONE`, `TODAY_PENDING`, `MISSED`, `FUTURE`.
- `ProgressDao.observeSessions(): Flow<List<SessionLogEntity>>` (`ORDER BY startedAt`). `ProgressRepository.activeDays(): Flow<Set<Long>>` `startedAt`ni mahalliy kunga o'giradi (`Clock` orqali, test qilinadigan).
- `HomeUiState`ga `streak: StreakInfo` va `weakTopics: List<TopicProgress>` qo'shiladi. `TopicProgress(topicId, title, progress: Progress)` har bir mavzu uchun `Readiness.of` bilan hisoblanadi.

## 6. Savol ekrani (6c)

`SessionViewModel` mantig'i o'zgarmaydi: `single` va `multi`, xato variantlarni o'chirish, takror savollar, chiqishni tasdiqlash oynasi, `ReportIssueAction`.

- **Yuqori qator:** ✕ (chiqish), `WavyProgress(position / queueSize)`, "N / M", bayroqcha.
- **Chiplar:** savol turi (`kind`: Tushuncha, To'g'ri/Noto'g'ri, Kod natijasi, Kod review), daraja, takror bo'lsa "Takror" (`streakContainer`).
- **Savol matni:** `headlineSmall` (Bricolage, ~22sp); `InlineCodeText` kod qismlari `codeBackground`/`codeText` bilan.
- **`CodeBlock`:** `surfaceContainer` fon, 20dp radius, JetBrains Mono, gorizontal scroll. Syntax highlighting yo'q.
- **Variantlar:** `OptionCard`, 14dp oraliq.
- **Pastki qism:** `FeedbackSheet`, ostida `ExpressiveButton`: "Tekshirish" (primary, `canCheck` bo'lmasa o'chiq), "Keyingi savol" yoki "Yakunlash" (success).
- **Savollar orasida o'tish:** `AnimatedContent`, kalit `question.id + isRepeat`, gorizontal slide.
- **Haptika:** 3.5 bo'yicha.

## 7. Natija ekrani (6c)

- **Sarlavha** `ResultHeadline.of(firstTryCorrect, total)` sof funksiyasi bilan: ≥80% "Zo'r natija!", ≥50% "Yaxshi harakat", aks holda "Davom eting".
- **Halqa:** `CookieShape` fonli halqa, markazda "firstTryCorrect / total", raqam 0 dan sanab chiqadi.
- **Plitkalar:** 3 ta `StatTile`: savollar, birinchi urinishda to'g'ri, qayta ishlangan.
- **Tayyorlik kartasi:** "50% → 54%", o'sgan qism `success` rangida animatsiya bilan to'ladi. O'zgarish bo'lmasa: "Tayyorlik: 50%".
- **Konfetti:** faqat ulush ≥80% bo'lsa.
- **Tugmalar:** "Yana mashq" va "Bosh sahifa", mantig'i hozirgidek. Bo'sh sessiya holati (`result_empty`) saqlanadi.

### 7.1 Tayyorlik o'sishi

- `SessionViewModel`ga `readiness: suspend () -> Progress` parametri qo'shiladi. Nav darajasida u joriy field va daraja doirasidagi `Readiness.of` sifatida tuziladi.
- Sessiya yuklanganda bir marta o'qiladi (`readinessBefore`). Sessiya tugaganda, `onFinished` va oxirgi `onOutcome` yozuvlari tugagach yana o'qiladi (`readinessAfter`).
- Ikkalasi `SessionUiState`ga qo'shiladi. `readinessAfter` hali null bo'lsa, karta "Tayyorlik: …" ko'rinishida kutib turadi va qiymat kelganda o'sish animatsiyasi boshlanadi.

## 8. Statistika ekrani (6d)

> Taklif; spec ko'rib chiqilganda tasdiqlanadi.

Yangi `ui/stats/StatsScreen.kt` va `StatsViewModel`. Sarlavha "Statistika", ostida field va daraja. Yuqoridan pastga 4 ta `SectionCard`:

1. **Faollik.**
   - Joriy va eng uzun seriya (ikkita `StatTile`).
   - So'nggi 18 hafta × 7 kun heatmap'i. Ustunlar hafta, qatorlar Du–Ya. Rang darajasi kundagi sessiyalar soniga qarab: 0, 1, 2, 3+ (`surfaceContainer` → `primary` tuslari).
   - Bugungi katakcha chegara bilan belgilanadi. Kelgusi kunlar chizilmaydi.
2. **Leitner qutilari.** Joriy field va daraja doirasidagi savollar uchta guruhda, bitta segmentli bar va izoh bilan:
   - Yangi: holati yo'q.
   - O'rganilmoqda: `box` 1–2.
   - O'zlashtirilgan: `box` 3–5.
3. **Mavzular.** Field mavzularining hammasi, o'zlashtirish foizi o'sish tartibida (eng bo'shi tepada), har birida bar. Bosilsa `session(topicId)`.
4. **Mock tarixi.**
   - Joriy field'ning oxirgi 10 ta mock natijasi ustunli grafikda (`Canvas`), ustun tepasida foiz, eng yangisi o'ngda.
   - Mock bo'lmasa: "Hali mock interview topshirilmagan" va "Mock'ni boshlash" tugmasi. Tugma `MOCK_MIN` qoidasiga bo'ysunadi.

**Ma'lumot.**
- `core/engine/ActivityGrid.kt`: `ActivityGrid.of(sessionDays: List<Long>, today: Long, weeks = 18): List<List<Int?>>`, kun bo'yicha sessiyalar soni; kelgusi kunlar `null`.
- `core/engine/LeitnerBreakdown.kt`: `of(questions, states): Breakdown(fresh, learning, mastered)`.
- `ProgressDao.observeMockSessions(fieldId): Flow<List<SessionLogEntity>>` (`mode = MOCK`, oxirgi 10 ta).

## 9. Qolgan ekranlar (6e)

> Taklif; spec ko'rib chiqilganda tasdiqlanadi.

Mantiq o'zgarmaydi, faqat uslub:

- **Onboarding (va `profile`).**
  - Fieldlar guruh sarlavhalari bilan (Mobile, Frontend, Backend, Boshqa) 2 ustunli plitkalarda.
  - Darajalar vertikal kartalarda: nom, tajriba yillari, qisqa tavsif.
  - Tanlanganda karta `primaryContainer`ga o'tadi va burchaklari morph bo'ladi.
  - Pastda `ExpressiveButton` "Davom etish".
  - Brend logolari ishlatilmaydi.
- **Mavzular (tab).**
  - Tepada "Aralash" hero kartasi (`primaryContainer`).
  - Har bir mavzu kartasida nom, savollar soni va o'zlashtirish bar'i.
  - "Tez orada" holati saqlanadi.
- **Mock.**
  - Yuqori qatorda taymer chipi. Oxirgi 5 daqiqada `errorContainer`ga o'tadi va har daqiqada bir marta pulsatsiya qiladi.
  - Savol ko'rinishi 6-bo'limdagi bilan bir xil: `OptionCard`, lekin javob oxirigacha tekshirilmaydi.
  - "Oldingi" va "Keyingi" `ExpressiveButton` juftligi.
  - Yakunlash va chiqish oynalari hozirgidek.
- **Mock natija.**
  - Foiz halqasi (`ReadinessRing` uslubida).
  - Mavzular bo'yicha natija bar'lari, eng pastidan.
  - "Xatolar ustida ishlash" (success) va "Bosh sahifa" tugmalari.
- **Sozlamalar.** Guruhlangan `SectionCard`lar:
  - Profil: field va daraja.
  - Ko'rinish: tema `SingleChoiceSegmentedButtonRow`.
  - Eslatma.
  - Zaxira: eksport va import.
  - Xavfli zona: progressni tozalash, `error` rangida.
- **Dialoglar.** `AlertDialog` `extraLarge` shakl bilan, tugmalar `ExpressiveButton` uslubida.
- **Play skrinshotlari.** `docs/play/screenshots/` yangi UI bilan qayta olinadi.

## 10. Bosqichlar

Har bir bosqich alohida branch va PR, oldingisi merge bo'lgach boshlanadi.

| Bosqich | Mazmuni | Tugash sharti |
|---|---|---|
| 6a | Ranglar, shriftlar, shakllar, motion, haptika, `ui/design` komponentlari va preview'lar. Mavjud ekranlar yangi tema bilan chiziladi, tartibi hali eski | Build yashil; preview'lar light/dark; APK hajmi farqi PR'da |
| 6b | Navigatsiya qobig'i, Bosh sahifa, `Streak`, `activeDays`, bo'sh mavzular | `Streak` va `HomeViewModel` testlari; emulatorda tab'lar va Bosh sahifa |
| 6c | Savol va Natija ekranlari, tayyorlik o'sishi, `ResultHeadline` | `SessionViewModel` va `ResultHeadline` testlari; emulatorda `single`, `multi`, xato va to'g'ri oqimlari |
| 6d | Statistika ekrani, `ActivityGrid`, `LeitnerBreakdown`, mock tarixi | Engine va `StatsViewModel` testlari; emulatorda bo'sh va to'la holatlar |
| 6e | Onboarding, Mavzular, Mock, Mock natija, Sozlamalar, dialoglar, Play skrinshotlari | Emulatorda barcha ekranlar light/dark; skrinshotlar yangilangan |

## 11. Xatolar va chegaraviy holatlar

- **Yangi foydalanuvchi:** seriya 0, heatmap bo'sh, mock tarixi bo'sh, "Bo'sh mavzular" hamma mavzuni 0% ko'rsatadi. Har biri uchun ma'noli bo'sh holat matni bo'ladi.
- **Kun almashishi:** seriya va heatmap `today()` asosida qayta hisoblanadi. Ilova yarim tunda ochiq tursa, keyingi qayta tuzilishda yangilanadi.
- **Vaqt mintaqasi o'zgarishi:** kun `startedAt`dan joriy mintaqada hisoblanadi; mintaqa almashsa, chegaradagi sessiya qo'shni kunga o'tishi mumkin. Bu qabul qilinadi.
- **Field yoki daraja almashtirilsa:** tayyorlik, Leitner, mavzular va mock tarixi yangi field bo'yicha. Seriya va heatmap esa barcha fieldlar bo'yicha qoladi.
- **Shrift masshtabi 200%:** kartalar va plitkalar balandlik bo'yicha o'sadi, matn qirqilmaydi. Bosh sahifadagi ikki ustunli plitkalar `fontScale > 1.5` da bitta ustunga o'tadi.
- **Kamaytirilgan motion:** 3.4 bo'yicha.

## 12. Testlash

- **Unit (core/engine):** `Streak`, `ActivityGrid`, `LeitnerBreakdown`, `ResultHeadline`. Bo'sh, chegaraviy va uzilgan holatlar.
- **ViewModel (Robolectric/coroutines-test):** `HomeViewModel` (seriya, bo'sh mavzular), `SessionViewModel` (tayyorlik o'sishi), `StatsViewModel`.
- **Build:** har PR'da `./gradlew test :app:assembleDebug`.
- **Qurilma:** har bosqichda emulatorda light va dark, shrift masshtabi 200% va TalkBack bilan asosiy oqimlar. Tekshirilgan narsalar PR'da sanab o'tiladi.
- **Kontrast:** matn va fon juftlari 4.5:1 (24sp+ uchun 3:1) dan past bo'lmasligi 6a'da tekshiriladi.

## 13. Qamrov tashqarisida

Dynamic color, syntax highlighting, home screen widget, B va C yo'nalishlaridan elementlar (glass, neo-brutalizm), planshet va foldable uchun adaptive layout, Android 16 Live Updates, `material3` 1.5.0-alpha Expressive API, Lottie va boshqa animatsiya kutubxonalari, grafik kutubxonalari.
