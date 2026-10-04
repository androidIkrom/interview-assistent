# DevSuhbat — Data safety (Play Console javoblari)

Play Console → Ilova kontenti (App content) → Data safety. Javoblar ilovaning 1.0.0 versiyasiga mos.

## Ma'lumot yig'ish va ulashish

| Savol | Javob | Asos |
|---|---|---|
| Ilova foydalanuvchi ma'lumotlarini yig'adimi yoki ulashadimi? | **Yo'q** | Ilovada `INTERNET` ruxsati yo'q; analitika, reklama, crash reporting SDK'lari yo'q. |
| Ma'lumot uchinchi tomonlarga ulashiladimi? | **Yo'q** | Tarmoq orqali hech narsa yuborilmaydi. |

"Yo'q" javobidan keyin Play ma'lumot turlari, shifrlash (encryption in transit) va o'chirish so'rovi bo'limlarini so'ramaydi — ular faqat ma'lumot yig'adigan ilovalarga tegishli.

## Nima uchun bu to'g'ri

- Progress va sozlamalar faqat qurilmada (Room va DataStore) saqlanadi va qurilmadan chiqmaydi. Google ta'rifiga ko'ra, qurilmadan tashqariga yuborilmaydigan ma'lumot "yig'ilgan" hisoblanmaydi.
- Eksport fayli foydalanuvchi tanlagan joyga yoziladi (Storage Access Framework); ilova uni o'zi hech qayerga yubormaydi.
- "Xato haqida xabar berish" tizimning ulashish oynasini ochadi: yuborishni foydalanuvchi o'zi tanlaydi va ilova o'zi hech narsa yubormaydi.
- Merged manifest'dagi ruxsatlar: `POST_NOTIFICATIONS` (ixtiyoriy kunlik eslatma), WorkManager'ning `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `ACCESS_NETWORK_STATE`. `INTERNET` yo'q.

## Boshqa "App content" bo'limlari

- **Privacy policy:** https://androidikrom.github.io/interview-assistent/privacy/
- **Ads:** ilovada reklama yo'q.
- **App access:** barcha funksiyalar login'siz ochiq.
- **Target audience:** 18+.
- **Content rating:** so'rovnomada barcha savollarga "yo'q" (ta'lim ilovasi).
- **Government apps / Financial features / Health:** tegishli emas.
