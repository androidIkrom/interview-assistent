# 5-bosqich — Play Store'ga tayyorlash: dizayn spetsifikatsiyasi

Sana: 2026-10-03
Holat: dizayn suhbatda kelishildi (2026-10-03); yozma spec foydalanuvchi ko'rib chiqishini kutmoqda
Asosiy spec: `2026-10-02-devsuhbat-design.md` (10-bo'lim, 5-bosqich)

## 1. Maqsad

Ilovani Google Play'ga birinchi reliz uchun tayyorlash: qurilmada topilgan xatolarni tuzatish, savollar bankidagi "to'g'ri javob eng uzun" muammosini yo'qotish, ikonka, kunlik eslatma, progress eksport/import, imzolangan reliz build va Play Console materiallari.

**Muvaffaqiyat mezoni:** Play Console'ga yuklashga tayyor imzolangan AAB; barcha ekranlar emulatorda tekshirilgan; listing, maxfiylik siyosati va Data safety javoblari tayyor; savollarda javobni uzunlik bo'yicha topib bo'lmaydi.

## 2. Foydalanuvchi qarorlari (2026-10-03)

| Savol | Qaror |
|---|---|
| Nom va paket | `DevSuhbat`, `uz.devsuhbat` (o'zgarmaydi) |
| Maxfiylik siyosati | `docs/` ichida, GitHub Pages orqali (repo ochiq) |
| Kunlik eslatma | Default o'chiq, Settings'da yoqiladi, vaqt default 20:00 |
| Upload kalit | Claude yaratadi, repo'dan tashqarida (`D:\projects\devsuhbat-keys\`) |
| Ikonka | "Ofis propuski" (A variant, 3-tur): ipdagi xodim kartasi, `</>` avatar, yashil ✓; to'q ko'k fon, oltin ip. Manba: https://claude.ai/artifact/9QoyBSxWNyWtWuTSTSdtwe |
| Listing materiallari | Matnlar (uz), feature graphic, skrinshotlar, Data safety javoblari |
| Uzunlik muammosi | Relizdan oldin tuzatiladi (5g) |
| Ish tartibi | Bitta spec, har qism alohida branch va PR |

Ikonka tanlovining asosi (tadqiqot): "kelajakdagi men" (possible selves) — dasturchi o'zini ishga qabul qilingan holda ko'radi; to'q ko'k — ishonch va ofis, oltin — umid, yashil — natija. Ikonkada matn, "#1", "Free" kabi yozuvlar va begona brend belgilari yo'q (Play metadata qoidasi).

## 3. Qurilmadagi tekshiruv natijalari (2026-10-03, Pixel_8_2 emulator)

Tekshirilgan ekranlar: onboarding, daraja, Home, mavzular, mashq sessiyasi (xato va to'g'ri javob), sessiya natijasi, Settings, tema, mock, mock natijasi, xatolar rejimi, yo'nalishni almashtirish. Crash yo'q.

| # | Topilma | Daraja | Qayerda tuzatiladi |
|---|---|---|---|
| T1 | Single savollarning 93,6% ida (1149/1228) to'g'ri javob eng uzun variant; multi savollarning 17/45 tasida har bir to'g'ri variant har bir xatodan uzun | Jiddiy | 5g |
| T2 | Mock'da field mavzulari kam (Android Middle: 25 dan 7 tasi algoritmlar, Kotlin 1, komponentlar 1) | O'rta | 5a |
| T3 | Mashqdan keyin Home'dagi Xatolar kartasi "Bugun takrorlanadigan savol yo'q" deydi, xatolar esa ertaga qaytadi | Kichik | 5a |
| T4 | Ilova ~214 MB RSS ishlatadi; 2 GB'li emulatorda tizim uni bir marta LOW_MEMORY bilan yopdi | Tekshiriladi | 5a |
| T5 | 0% progress chizig'i oxirida Material 3 "stop" nuqtasi | Kosmetik | 5a (ixtiyoriy) |

## 4. Qismlar va tartib

Har qism — alohida branch va PR. Tartib: 5a → 5g → 5b → 5c → 5d → 5e → 5f. 5f (skrinshotlar) oxirida, chunki u yakuniy ekranlar va kontentdan olinadi.

### 5a — Qurilma topilmalarini tuzatish

- **Mock tarkibi (T2).** `QuestionPicker.mock` field'ning o'z mavzularidan kamida 60% (25 dan 15) oladi, qolgani umumiy bloklardan. Darajalar nisbati (60% tanlangan daraja, qolgani pastroq) har guruh ichida saqlanadi. Field savollari yetmasa — bo'sh joy umumiy bloklardan to'ldiriladi (va aksincha). Sof funksiya, TDD bilan.
- **Xatolar kartasi (T3).** Bugun takrorlanadiganlar bo'lmasa, lekin keyinroq qaytadigan xatolar bo'lsa: "N ta savol ertaga qaytadi" (yoki eng yaqin kun). `ProgressRepository` eng yaqin `dueDay` va sonini qaytaradi.
- **Xotira (T4).** `dumpsys meminfo` bilan o'lchanadi. Agar barcha savol fayllari ishga tushishda xotiraga yuklanayotgan bo'lsa — faqat tanlangan field mavzulari yuklanadi (lazy). O'lchov natijasi PR tavsifida.
- **T5** — `LinearProgressIndicator`ning `drawStopIndicator`ini o'chirish (bitta qator).

### 5g — "To'g'ri javob eng uzun" muammosini tuzatish

- Har savolda to'g'ri variant qisqa va aniq bo'ladi; uni tasdiqlovchi tafsilot `explanation`ga ko'chadi. Xato variantlar mazmunli va uzunligi bo'yicha to'g'ri javobga yaqin qilib boyitiladi (hint'lar o'zgarmaydi yoki moslashtiriladi). Savol ma'nosi va to'g'ri javob o'zgarmaydi.
- **Yangi test** `ContentAssetsTest`da:
  - single (true_false'dan tashqari): to'g'ri variant qat'iy eng uzun bo'lgan savollar ulushi **butun bank bo'yicha ≤ 35%** va **har mavzu bo'yicha ≤ 50%**;
  - multi: "har bir to'g'ri variant har bir xato variantdan uzun" bo'lgan savollar ulushi ≤ 35%.
  - Hamma qiymatlar bitta joyda konstanta sifatida; test avval RED (hozirgi 93,6%).
- Partiyalar: kontent bosqichidagi tartibda, bir PR = bir field yoki 2–3 ta `core.*` mavzu. Har PR'dan keyin test o'sha mavzular uchun yashil bo'ladi; bank bo'yicha umumiy chegara oxirgi partiyada yashil bo'ladi (shu paytgacha umumiy tekshiruv `@Ignore` emas, balki "tuzatilgan mavzular" ro'yxati bo'yicha ishlaydi — kontent bosqichidagi `completedTopics` usuli).
- Savollar `"reviewed": false` bo'lib qoladi.

### 5b — Ikonka, versiya, xato xabari

- A variant `res/drawable/ic_launcher_foreground.xml` (108 dp vektor, 66 dp xavfsiz zona ichida), `ic_launcher_background` rangi, Android 13+ uchun `<monochrome>` qatlami. Eski vaqtinchalik ikonka almashtiriladi.
- Play uchun 512×512 PNG (32-bit, sRGB, to'liq kvadrat, burchaksiz) — o'sha SVG'dan render qilinadi, `docs/play/icon-512.png`.
- `versionCode = 1`, `versionName = "1.0.0"`.
- **"Xato haqida xabar berish"** (asosiy spec, 11-bo'lim): sessiya va mock ekranidagi menyuda. Bosilganda `ACTION_SEND` (text/plain) ulashish oynasi: savol ID, savol matni, ilova versiyasi va izoh uchun bo'sh qator. Manzil yo'q — foydalanuvchi o'zi tanlaydi (Telegram, email). Internet ruxsati qo'shilmaydi.

### 5c — Kunlik eslatma

- Settings'da "Kunlik eslatma" kaliti va vaqt tanlash (TimePicker). DataStore: `reminder_enabled` (default `false`), `reminder_minutes` (default 1200 = 20:00).
- Yoqilganda Android 13+ da `POST_NOTIFICATIONS` so'raladi. Rad etilsa — kalit o'chiq qoladi va tizim sozlamalariga yo'naltiruvchi izoh ko'rsatiladi.
- Rejalashtirish: WorkManager `OneTimeWorkRequest` (unique, REPLACE) keyingi eslatma vaqtigacha kechikish bilan; worker ishlagach o'zini ertangi kunga qayta rejalashtiradi. Ilova ishga tushganda va sozlama o'zgarganda qayta rejalashtiriladi.
- Worker: agar bugun (mahalliy sana) kamida bitta sessiya yakunlangan bo'lsa — bildirishnoma chiqmaydi. Aks holda: "Bugun 10 ta savol yechamizmi?" (bosilsa ilova ochiladi). Bitta notification channel.
- Sof funksiyalar (TDD): `nextReminderDelay(now: LocalDateTime, minutes: Int): Duration`, `practicedToday(lastFinishedAt, now, zone)`.
- Yangi bog'liqlik: `androidx.work:work-runtime-ktx`.

### 5d — Progress eksport/import

- Format (JSON, kotlinx.serialization):
  ```json
  { "format": "devsuhbat-backup", "version": 1, "exportedAt": "2026-10-03T20:00:00Z",
    "settings": { "field": "android", "level": "middle", "theme": "system",
                  "reminderEnabled": false, "reminderMinutes": 1200 },
    "questionStates": [ … ], "sessions": [ … ], "mockTopicResults": [ … ] }
  ```
- `BackupCodec` (`:app`, UI'siz): `encode(snapshot)`, `decode(json): Result<Snapshot>`. Rad etiladi: noto'g'ri JSON, boshqa `format`, qo'llanmaydigan `version`, majburiy maydon yo'q. Noma'lum savol ID'lari (kontentdan o'chirilgan) jim tashlab yuboriladi.
- Settings'da "Progressni eksport qilish" — `CreateDocument("application/json")`, nom `devsuhbat-backup-YYYY-MM-DD.json`; "Progressni import qilish" — `OpenDocument`, tasdiq dialogi ("Joriy progress almashtiriladi"), so'ng bitta Room tranzaksiyasida almashtirish va sozlamalarni yozish. Natija Snackbar bilan.
- Testlar: codec round-trip, rad etish holatlari; Robolectric — import eski holatni to'liq almashtiradi, xato faylda hech narsa o'zgarmaydi.

### 5e — Reliz build

- `release`: `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-android-optimize.txt` + `proguard-rules.pro` (kotlinx.serialization model klasslari, Room).
- Imzolash: `keystore.properties` (git-ignore) — `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Fayl bo'lmasa release imzosiz yig'iladi (boshqa mashinada build buzilmaydi).
- Upload kalit: `keytool -genkeypair -alias upload -keyalg RSA -keysize 4096 -validity 9125`, `D:\projects\devsuhbat-keys\upload.jks`; parollar faqat `keystore.properties`da. Foydalanuvchiga zaxira nusxa olish eslatiladi; kalit va parollar commit qilinmaydi va chatga chiqarilmaydi.
- Natija: `./gradlew bundleRelease` → AAB. R8 tekshiruvi: `assembleRelease` APK emulatorda o'rnatiladi va asosiy oqimlar (onboarding, sessiya, mock, eksport/import, eslatma) qo'lda tekshiriladi.
- Play App Signing yoqiladi (Play Console'da, foydalanuvchi tomonidan). Yuklashni foydalanuvchi qiladi.

### 5f — Play Console materiallari

- **Maxfiylik siyosati:** `docs/privacy/index.html`, o'zbekcha va inglizcha. Mazmuni: ma'lumot yig'ilmaydi va yuborilmaydi; progress faqat qurilmada; eksport fayli foydalanuvchi tanlagan joyga yoziladi; internet ruxsati yo'q; bildirishnomalar ixtiyoriy; aloqa uchun email. GitHub Pages'ni yoqish — foydalanuvchining alohida roziligi bilan.
- **Listing (uz):** `docs/play/listing-uz.md` — nom (30 belgigacha), qisqa tavsif (80), to'liq tavsif (4000). Play metadata qoidalariga mos: "eng yaxshi", "#1", "bepul", emoji, katta harflar yo'q; ishga joylashish kafolati haqida da'vo yo'q.
- **Feature graphic:** 1024×500, ikonka uslubida; artifact'da 2–3 variant, tanlangani PNG — `docs/play/feature-graphic.png`.
- **Skrinshotlar:** emulatordan 4–6 ta (Home, sessiya + hint, natija, mock, mavzular), `docs/play/screenshots/`.
- **Data safety:** `docs/play/data-safety.md` — Play formasi savollariga tayyor javoblar (ma'lumot yig'ilmaydi va ulashilmaydi).

## 5. Xatolar va chegaraviy holatlar

- Eslatma: qurilma qayta yoqilsa WorkManager vazifani o'zi tiklaydi; vaqt zonasi o'zgarsa keyingi ishga tushishda qayta hisoblanadi. Ruxsat keyin bekor qilinsa, worker hech narsa ko'rsatmaydi (xatosiz).
- Import: katta yoki buzilgan fayl — xabar va o'zgarishsiz qaytish; tranzaksiya yarim yo'lda to'xtamaydi.
- Reliz: R8 tufayli serialization yoki Room buzilsa — release APK tekshiruvida ushlanadi; keep qoidasi qo'shiladi.

## 6. Testlash

- Har PR: `./gradlew test` yashil (test soni oshadi), `:app:assembleDebug`, emulatorda o'zgargan ekranlar qo'lda tekshiriladi.
- Yangi unit testlar: mock tarkibi, Xatolar kartasi matni uchun repository, uzunlik testi (5g), eslatma vaqti va "bugun mashq qilinganmi", `BackupCodec`, import (Robolectric).
- 5e: release APK emulatorda smoke test.

## 7. Qamrov tashqarisida

Play Console'ga yuklash va listingni to'ldirish (foydalanuvchi qiladi), ingliz tilidagi listing, reklama/to'lov, bulutli sinxronizatsiya, store listing A/B tajribalari (relizdan keyin — ikonka bo'yicha tavsiya qilinadi).
