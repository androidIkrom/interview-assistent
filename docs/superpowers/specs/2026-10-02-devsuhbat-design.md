# DevSuhbat — dizayn spetsifikatsiyasi

Sana: 2026-10-02
Holat: dizayn suhbatda tasdiqlangan (2026-10-02); yozma spec foydalanuvchi ko'rib chiqishini kutmoqda

## 1. Maqsad

**DevSuhbat** — dasturlash sohasida ish suhbatiga (interview) tayyorlanayotganlar uchun Android ilova. Foydalanuvchi o'z yo'nalishini (field) va topshirmoqchi bo'lgan darajasini tanlaydi, ilova shunga mos test savollarini beradi.

Asosiy farqi: noto'g'ri javobda ilova to'g'ri javobni **ko'rsatmaydi**. U tanlangan variant nega xato ekanini tushuntiradi va foydalanuvchi savolni qaytadan yechadi. Xato qilingan savol keyinroq yana qaytadi.

**Muvaffaqiyat mezoni:** foydalanuvchi tanlagan field va daraja bo'yicha qaysi mavzularda tayyor, qaysilarida bo'sh ekanini ilovadan ko'radi va bo'sh mavzularni xatolar ustida ishlash orqali yopadi.

### Foydalanuvchi qarorlari (2026-10-02)

| Savol | Qaror |
|---|---|
| Auditoriya | Play Store, o'zbek bozori. UI va tushuntirishlar o'zbekcha (lotin), texnik terminlar va kod inglizcha |
| Kontent manbai | Ilova ichidagi (bundled) baza, offline |
| Qamrov | 10+ field (13 ta), keng |
| Retry | Hint + darhol qayta urinish + keyinroq qaytarish |
| Biznes model | Bepul, akkauntsiz, offline. Backend yo'q |

### Cheklovlar

- Android native: Kotlin + Jetpack Compose.
- Backend, akkaunt, reklama, to'lov yo'q. Internet ruxsati so'ralmaydi.
- Savollar boshqa saytlardan **ko'chirilmaydi**. Ular `docs/content/taxonomy.md` dagi mavzular bo'yicha yangidan yoziladi.

### Qamrov tashqarisida (MVP)

iOS, bulutli sinxronizatsiya, leaderboard, AI yordamchi, ochiq (yozma) javobli savollar, kod yozish muhiti, rus/ingliz tili, monetizatsiya, serverdan kontent yangilash.

## 2. Fieldlar va darajalar

### 2.1 Fieldlar (13 ta)

| Guruh | Field | id |
|---|---|---|
| Mobile | Android (Kotlin) | `android` |
| Mobile | iOS (Swift) | `ios` |
| Mobile | Flutter | `flutter` |
| Frontend | Frontend (JS/TS, React) | `frontend` |
| Backend | Python (Django/FastAPI) | `python` |
| Backend | Node.js (Express/NestJS) | `node` |
| Backend | Java (Spring) | `java` |
| Backend | Go | `go` |
| Backend | PHP (Laravel) | `php` |
| Backend | .NET (C#) | `dotnet` |
| Boshqa | QA | `qa` |
| Boshqa | DevOps | `devops` |
| Boshqa | Data/ML | `ml` |

Har bir field 6 ta o'z mavzusiga (jami 70 savol) va bir nechta **umumiy blokka** ulanadi. Umumiy bloklar (algoritmlar, SQL, Git, HTTP va h.k.) bir marta yoziladi va bir nechta fieldda ishlatiladi. To'liq ro'yxat, savollar soni va har darajada eng ko'p so'raladigan mavzular: `docs/content/taxonomy.md`.

Jami maqsad: 13 × 70 + 402 umumiy = **1312 savol**.

### 2.2 Darajalar (4 ta)

| Daraja | id | Tajriba | Savol nimani tekshiradi |
|---|---|---|---|
| Junior | `junior` | 0–1 yil | Ta'rif, sintaksis, asosiy tushuncha |
| Middle | `middle` | 1–3 yil | Ichki mexanizm, "bu kod nima chiqaradi", tipik xatolar |
| Strong Middle | `strong_middle` | 3–5 yil | Trade-off, performance, debugging senariy, modul dizayni |
| Senior | `senior` | 5+ yil | Arxitektura, system design, masshtab, texnik qarorlar |

Foydalanuvchi tanlagan daraja — yuqori chegara. Unga shu daraja va undan pastki darajalar savollari beriladi, chunki real suhbatda ham shunday.

## 3. Savol modeli

### 3.1 Turlar

Texnik jihatdan ikki tur bor: bitta to'g'ri javobli (`single`) va bir nechta to'g'ri javobli (`multi`). Ko'rinish uchun `kind` belgisi qo'shiladi:

| kind | Ma'nosi | type |
|---|---|---|
| `concept` | Tushuncha savoli | single yoki multi |
| `true_false` | To'g'ri / noto'g'ri | single (2 variant) |
| `code_output` | "Bu kod nima chiqaradi?" | single |
| `code_review` | "Bu kodda muammo nima?" | single yoki multi |

### 3.2 JSON sxemasi

Katalog — `app/src/main/assets/content/catalog.json`:

```json
{
  "version": 1,
  "fields": [
    { "id": "android", "title": "Android (Kotlin)", "group": "mobile",
      "topics": ["android.kotlin", "android.components", "core.dsa"] }
  ],
  "topics": [
    { "id": "android.kotlin", "title": "Kotlin tili", "file": "android_kotlin.json" }
  ]
}
```

Savollar — `app/src/main/assets/content/questions/<file>`, har bir mavzu alohida fayl:

```json
{
  "topic": "android.kotlin",
  "questions": [
    {
      "id": "android.kotlin.001",
      "level": "junior",
      "type": "single",
      "kind": "concept",
      "prompt": "`val` va `var` o'rtasidagi farq nima?",
      "code": null,
      "options": [
        { "id": "a", "text": "...", "correct": true },
        { "id": "b", "text": "...", "hint": "Nega bu variant xato ekanligi." }
      ],
      "explanation": "Nega to'g'ri javob to'g'ri ekanligi.",
      "reviewed": false
    }
  ]
}
```

Qoidalar:

- `id` butun baza bo'yicha yagona va **o'zgarmas** (progress shu id ga bog'lanadi). Format: `<topic>.<3 xonali raqam>`.
- `prompt`, `text`, `hint`, `explanation` ichida `` `backtick` `` bilan inline kod yozish mumkin. `code` — alohida ko'p qatorli kod bloki.
- Har bir xato variantda `hint` majburiy. To'g'ri variantda `hint` bo'lmaydi.
- `hint` to'g'ri javobni oshkor qilmasligi kerak.
- `reviewed: false` — savol inson tomonidan hali tekshirilmagan.

### 3.3 Validator

Qoidalar `core:engine` modulidagi `ContentValidator` da (sof Kotlin) yoziladi va unit test barcha asset fayllarni shu validator orqali o'tkazadi. Alohida Python skript yozilmaydi: qoidalar bitta joyda turadi va `gradlew test` kontentni ham tekshiradi.

Tekshiruvlar:

1. `file` ko'rsatilgan har bir mavzuning fayli mavjud; har bir field faqat mavjud mavzularga ishora qiladi. `file` ko'rsatilmagan mavzu — rejalashtirilgan (kontenti hali yozilmagan), ilovada "Tez orada" deb ko'rinadi.
2. Savol `id` lari yagona va o'z mavzusi prefiksi bilan boshlanadi.
3. `level`, `type`, `kind` qiymatlari ruxsat etilgan ro'yxatdan.
4. `single`: aynan 1 ta to'g'ri variant, 2–5 ta variant. `multi`: kamida 2 ta to'g'ri va kamida 1 ta xato variant.
5. Har bir xato variantda bo'sh bo'lmagan `hint` bor; to'g'ri variantda `hint` yo'q.
6. `hint` ichida to'g'ri variant matni so'zma-so'z uchramaydi (to'g'ri variant matni 4 belgidan uzun bo'lsa).
7. `explanation` bo'sh emas; variant `id` lari savol ichida yagona.
8. `code_output` va `code_review` savollarida `code` bor.

## 4. Retry mexanikasi

### 4.1 Bitta savol ichida (`single`)

1. Foydalanuvchi variant tanlab "Tekshirish"ni bosadi.
2. Noto'g'ri bo'lsa: shu variantning `hint` matni chiqadi, variant o'chirilgan (bosilmaydigan) holatga o'tadi. To'g'ri javob belgilanmaydi.
3. Foydalanuvchi qolgan variantlardan qayta tanlaydi. To'g'ri topguncha davom etadi.
4. To'g'ri topgach: `explanation` chiqadi va "Keyingi" tugmasi ochiladi.

### 4.2 `multi` savolda

1. Foydalanuvchi bir nechta variantni belgilab "Tekshirish"ni bosadi.
2. To'plam to'liq to'g'ri bo'lmasa: "Tanlaganlaringizdan N tasi to'g'ri, yana M ta to'g'ri variant tanlanmagan" degan xabar chiqadi. Qaysi variant to'g'ri ekani aytilmaydi. Tanlangan xato variantlarning `hint` lari ko'rsatiladi va ular o'chiriladi.
3. Foydalanuvchi qayta urinadi. To'liq to'g'ri to'plamda `explanation` chiqadi.

### 4.3 Sessiya ichida

Kamida bitta xato urinish bo'lgan savol sessiya oxirida yana bir marta beriladi, variantlari boshqa tartibda. Bu takror ham shu qoidalar bilan ishlaydi, lekin savol navbatga ikkinchi marta qo'shilmaydi.

### 4.4 Kunlar bo'yicha (Leitner)

Har bir savol uchun holat saqlanadi: `box` (0–5) va `dueDay` (kun).

| Hodisa | Natija |
|---|---|
| Yangi savol, birinchi urinishda to'g'ri | `box = 3`, 7 kundan keyin qaytadi |
| Ko'rilgan savol, birinchi urinishda to'g'ri | `box = min(box + 1, 5)` |
| Kamida bitta xato urinish | `box = 1`, ertaga qaytadi |
| Sessiya ichidagi takrorda to'g'ri | holat o'zgarmaydi |

Intervallar: box 1 → 1 kun, 2 → 3 kun, 3 → 7 kun, 4 → 14 kun, 5 → 30 kun.

Savol **o'zlashtirilgan** hisoblanadi, agar `box >= 3`. Demak faqat birinchi urinishda to'g'ri topilgan savol o'zlashtirilgan sanaladi; variantlarni birma-bir bosib topish yordam bermaydi.

## 5. Rejimlar

### 5.1 Mashq

Foydalanuvchi mavzuni (yoki "Aralash"ni) tanlaydi. Sessiya 10 savoldan iborat. Tanlash tartibi: avval muddati kelgan (due) savollar, keyin ko'rilmaganlar, keyin eng past `box` dagilar. Daraja bo'yicha: tanlangan daraja va undan pastlari. Har savolda darhol feedback (4-bo'lim).

### 5.2 Xatolar

Muddati kelgan (`dueDay <= bugun`) savollar, tanlangan field doirasida, ko'pi bilan 20 ta. Home ekranida soni ko'rinadi.

### 5.3 Mock interview

- 25 savol, 30 daqiqa, fieldning barcha mavzularidan aralash. Taxminan 60% tanlangan daraja, 40% pastki darajalar (Junior uchun hammasi Junior).
- Har savolga bitta urinish, sessiya davomida feedback yo'q. Ortga qaytib javobni o'zgartirish mumkin.
- Yakunda: umumiy natija va mavzular kesimida to'g'ri/jami. To'g'ri javoblar **ko'rsatilmaydi**.
- Xato javob berilgan savollar `box = 1`, `dueDay = bugun` bo'ladi, ya'ni darhol "Xatolar"ga tushadi. Natija ekranidagi "Xatolar ustida ishlash" tugmasi shu savollar bilan hint'li sessiyani boshlaydi.
- To'g'ri javoblar birinchi urinishda to'g'ri deb hisoblanadi (4.4-jadval).

### 5.4 Tayyorlik ko'rsatkichi

Field va daraja uchun: `o'zlashtirilgan savollar / doiradagi barcha savollar`, umumiy va mavzular kesimida. Doira — tanlangan daraja va pastki darajalar savollari. Home ekranida umumiy foiz, mavzular ro'yxatida har mavzu foizi ko'rinadi.

## 6. Ekranlar

| Ekran | Vazifasi |
|---|---|
| Onboarding | 2 qadam: field tanlash (guruhlar bo'yicha), daraja tanlash |
| Home | Tayyorlik foizi, "Xatolar" (soni bilan), "Mashq", "Mock interview", oxirgi mock natijasi |
| Mavzular | Field mavzulari ro'yxati, har birida foiz va savollar soni; "Aralash" |
| Sessiya | Savol, kod bloki, variantlar, hint/explanation paneli, progress chizig'i |
| Sessiya natijasi | Birinchi urinishda to'g'ri soni, qaytadan ishlanganlar, "Yana" |
| Mock | Taymer, savol navigatsiyasi, "Yakunlash" |
| Mock natijasi | Umumiy ball, mavzular kesimi, "Xatolar ustida ishlash" |
| Sozlamalar | Field va darajani o'zgartirish, mavzu (yorug'/qorong'i/tizim), progressni tozalash |

Navigatsiya: pastki panel yo'q; Home — markaz, qolgan ekranlar undan ochiladi. Sessiya va mock ekranidan chiqishda tasdiqlash so'raladi.

Kod bloklari: monospace shrift, gorizontal scroll. Sintaksis bo'yash MVP'da yo'q.

## 7. Arxitektura

### 7.1 Modullar

```
:app            Android: UI (Compose), Room, DataStore, asset o'qish, DI
:core:engine    Sof Kotlin/JVM: kontent modeli va parser, validator, baholash,
                sessiya navbati, Leitner, savol tanlash, tayyorlik hisobi
```

`core:engine` Android'ga bog'liq emas, shuning uchun barcha mantiq oddiy JVM unit testlari bilan tekshiriladi.

### 7.2 Kontent va holat ajratilgan

- **Kontent**: assets ichidagi JSON. `ContentRepository` katalogni bir marta, mavzu fayllarini kerak bo'lganda o'qiydi va xotirada saqlaydi. Room'ga yozilmaydi.
- **Foydalanuvchi holati**: Room. Kontentga faqat savol `id` si orqali bog'lanadi. Kontent yangilanganda DB migratsiyasi kerak bo'lmaydi; bazada yo'q savol `id` lari e'tiborga olinmaydi.

Rad etilgan variantlar: tayyor Room DB (`createFromAsset`) — kontentni tahrirlash va diff qilish qiyin; JSON'ni Room'ga import — versiyalash murakkab, foydasi yo'q (1312 savol xotiraga bemalol sig'adi).

### 7.3 `core:engine` birliklari

| Birlik | Vazifasi |
|---|---|
| `Catalog`, `Topic`, `Question`, `Option`, `Level` | Kontent modeli (kotlinx.serialization) |
| `ContentParser` | JSON matn → model |
| `ContentValidator` | 3.3-bo'lim qoidalari → xatolar ro'yxati |
| `Grader` | Tanlov + savol → `Verdict` (to'g'ri / xato + hint'lar + multi hisobi) |
| `QuestionAttempt` | Bitta savol ustidagi urinishlar holati (o'chirilgan variantlar, birinchi urinish to'g'rimi) |
| `PracticeSession` | Savollar navbati, sessiya ichidagi takror, yakuniy statistika |
| `MockSession` | Javoblarni yig'ish, yakuniy baholash, mavzular kesimi |
| `Leitner` | `QuestionState` + hodisa + bugungi kun → yangi `QuestionState` |
| `QuestionPicker` | Mashq / xatolar / mock uchun savol tanlash |
| `Readiness` | O'zlashtirish foizi (umumiy va mavzular bo'yicha) |

Tasodifiylik (`Random`) va bugungi kun (`epochDay`) parametr sifatida beriladi, testlar deterministik bo'ladi.

### 7.4 `:app` qatlamlari

- `content/` — `AssetContentRepository` (assets → `core:engine` modeli).
- `data/` — Room: `question_state`, `session_log`, `mock_topic_result`; `ProgressRepository`. DataStore: `field`, `level`, `onboardingDone`, `theme`.
- `ui/` — ekranlar va ViewModel'lar; `ui/theme` — Material 3, yorug' va qorong'i.
- `AppContainer` — qo'lda DI (Hilt ishlatilmaydi).

Room jadvallari:

```
question_state(questionId PK, box, dueDay, attempts, wrongAttempts, lastAnsweredAt)
session_log(id PK auto, mode, fieldId, level, startedAt, finishedAt, total, firstTryCorrect)
mock_topic_result(sessionId, topicId, total, correct; PK(sessionId, topicId))
```

### 7.5 Texnologiyalar

Kotlin 2.4, AGP 9.4, Compose BOM 2026.09 + Material 3, Navigation Compose, Room 2.8 (KSP), DataStore Preferences, kotlinx.serialization, coroutines/Flow. `minSdk 26`, `targetSdk 37`. Test: JUnit 4, Robolectric (Room va repository uchun), kotlinx-coroutines-test. Versiyalar Hangul Friend loyihasidagi bilan bir xil.

Paket: `uz.devsuhbat`.

## 8. Xatolar bilan ishlash

- Asset fayl o'qilmasa yoki JSON buzuq bo'lsa: shu mavzu o'tkazib yuboriladi, ekranda "Kontent yuklanmadi" holati ko'rsatiladi; ilova yiqilmaydi. Reliz oldidan validator testi buni ushlab qoladi.
- Mavzuda tanlangan darajaga mos savol bo'lmasa: mavzu ro'yxatda "Tez orada" belgisi bilan ko'rinadi va bosilmaydi.
- Kontenti hali yozilmagan yo'nalishni ham tanlash mumkin (foydalanuvchi talabi, 2026-10-02): tanlash ekranida u "Savollar tez orada qo'shiladi" izohi bilan ko'rinadi, mavzular ekranida esa savollar hali yozilmagani aytiladi va barcha mavzular "Tez orada" bo'ladi.
- Mock uchun savollar 25 tadan kam bo'lsa: mavjud savollar soni bilan o'tkaziladi (kamida 5 ta), aks holda tugma o'chirilgan.
- Jarayon o'ldirilsa (process death): mashq sessiyasi qayta tiklanmaydi, lekin allaqachon javob berilgan savollarning holati saqlangan bo'ladi (har savoldan keyin yoziladi). Mock sessiyasi yo'qoladi.
- Ekran aylantirilganda holat ViewModel'da saqlanadi.

## 9. Testlash

- `core:engine`: har bir birlik uchun unit testlar (TDD). Asosiy senariylar: single/multi baholash, hint'lar, sessiya ichidagi takror, Leitner o'tishlari, daraja bo'yicha tanlash, tayyorlik foizi.
- Kontent: barcha asset fayllar `ContentValidator` dan o'tadi (xatolar ro'yxati bo'sh bo'lishi shart).
- `:app`: Room DAO va `ProgressRepository` uchun Robolectric testlar; ViewModel'lar uchun coroutines-test.
- UI qo'lda tekshiriladi (qurilma yoki emulyator).

## 10. Bosqichlar

| Bosqich | Mazmuni |
|---|---|
| 1. Poydevor | Loyiha, `core:engine` (model, parser, validator, Grader, PracticeSession), onboarding, Home, mavzular, mashq sessiyasi; Android fieldi uchun namuna kontent |
| 2. Retry va Leitner | Room holati, Leitner, "Xatolar" rejimi, tayyorlik foizi |
| 3. Mock interview | Mock sessiya, taymer, natija ekrani, sessiyalar tarixi |
| 4. Kontent | Qolgan 12 field va umumiy bloklar, partiyalab; har partiya validator testidan o'tadi |
| 5. Play Store | Ikonka, maxfiylik siyosati, kunlik eslatma (WorkManager), progress export/import, reliz build (R8, imzolash) |

Har bosqich alohida implementation plan oladi (`docs/superpowers/plans/`).

## 11. Xavflar

- **Kontent hajmi.** 1312 savol, har birida 3–4 variant, har xato variantga hint va tushuntirish — ishning eng katta qismi. Shuning uchun kod 1–3 bosqichlarda bitta field bilan tugatiladi, kontent 4-bosqichda oqim bo'lib qo'shiladi. Savollar `reviewed: false` bilan yoziladi va inson tekshiruvidan o'tishi kerak.
- **Savol sifati.** Xato yoki eskirgan savol ishonchni yo'qotadi. Yumshatish: validator, `reviewed` bayrog'i, 5-bosqichda "Xato haqida xabar berish" (matnni ulashish orqali).
- **Hint to'g'ri javobni oshkor qilishi.** Avtomatik tekshiruv faqat so'zma-so'z mos kelishni ushlaydi; mazmunan oshkor qilishni inson tekshiradi.
