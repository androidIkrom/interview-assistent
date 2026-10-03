# Savollar taksonomiyasi: field × daraja × mavzu

Sana: 2026-10-02

Bu hujjat DevSuhbat savollar bazasining rejasi: qaysi fieldda qaysi mavzular bor, har darajada nechta savol yoziladi va 2025–2026 yillardagi suhbatlarda aynan nima so'raladi. Savollar shu ro'yxat bo'yicha yangidan yoziladi; boshqa manbalardan ko'chirilmaydi.

## 1. Manbalar va trendlar

Mavzular quyidagilarga tayangan:

- **State of Dev in Uzbekistan 2025** (stateofdev.uz): JavaScript 47.9%, Python 35.4%, TypeScript 32.6%, SQL 24.3%, Java 9.7%, PHP 8.3%, Go 5.6%; React 43.8%, Next.js 25%, Vue 16%; FastAPI 20.1%, Django 17.4%, Express 14.6%, NestJS 14.6%, Spring 9.7%, Laravel 7.6%; PostgreSQL 61.8%, Redis 26.4%. Rollar: backend 27.7%, frontend 20.9%, full-stack 19.4%, mobile 4.4%.
- 2025–2026 yillardagi interview savollari to'plamlari va tahlillari (Android, frontend, Java, Python, Flutter, DevOps bo'yicha; havolalar hujjat oxirida).

2025–2026 trendlari, taksonomiyaga ta'siri:

1. **Jumboq-algoritmlar kamaydi, amaliy kod ko'paydi.** Senior suhbatlarning katta qismida LeetCode uslubidagi masala yo'q; kodlash bo'lsa — satrlar, kolleksiyalar, real mantiq. Shuning uchun `core.dsa` da murakkab masalalar emas, murakkablik bahosi va to'g'ri tuzilma tanlash ustun.
2. **"Kod yoza olasizmi" o'rniga "kodni baholay olasizmi".** AI vositalari sabab nomzoddan tayyor kodni tekshirish, xatoni topish, qarorni himoya qilish so'raladi. Shuning uchun `code_review` savol turi va `core.aicode` bloki bor.
3. **System design'da AI/LLM komponentlari.** 2026 da system design suhbatlarining yarmiga yaqinida ML/LLM qismi uchraydi (RAG, vector search, latency/xarajat). `core.sysdesign` va `ml.dl` da aks etgan.
4. **Zamonaviy stack kutiladi.** Android — Compose, coroutines, Flow; frontend — React + TypeScript, Next.js; Java — 21 (virtual threads), Spring Boot 3; Python — FastAPI, Pydantic, asyncio; Flutter — Riverpod/Bloc; DevOps — Kubernetes, Terraform, GitOps.
5. **Middle va undan yuqorida ta'rif emas, trade-off.** "X nima?" emas, "qachon X, qachon Y va nega?".

## 2. Darajalar

| Daraja | Tajriba | Savol uslubi | Namuna |
|---|---|---|---|
| Junior | 0–1 yil | Ta'rif, sintaksis, asosiy tushuncha | "`val` va `var` farqi nima?" |
| Middle | 1–3 yil | Ichki mexanizm, kod natijasi, tipik xatolar | "Bu coroutine kodi nima chiqaradi?" |
| Strong Middle | 3–5 yil | Trade-off, performance, debugging, modul dizayni | "Ro'yxat sekin scroll bo'lyapti. Birinchi nimani tekshirasiz?" |
| Senior | 5+ yil | Arxitektura, system design, masshtab, texnik qarorlar | "Offline-first sinxronizatsiyada konfliktni qanday hal qilasiz?" |

Jadvallardagi ustunlar: **J** — Junior, **M** — Middle, **SM** — Strong Middle, **S** — Senior. Raqamlar — yoziladigan savollar soni.

## 3. Umumiy bloklar (402 savol)

Bir marta yoziladi, bir nechta fieldga ulanadi.

| id | Blok | J | M | SM | S | Jami |
|---|---|---|---|---|---|---|
| `core.dsa` | Algoritmlar va ma'lumot tuzilmalari | 20 | 18 | 12 | 6 | 56 |
| `core.oop` | OOP, SOLID, dizayn patternlari | 16 | 14 | 10 | 6 | 46 |
| `core.sql` | SQL va ma'lumotlar bazasi | 20 | 18 | 12 | 8 | 58 |
| `core.git` | Git va jamoaviy ish | 14 | 10 | 6 | 2 | 32 |
| `core.http` | HTTP, REST, tarmoq | 16 | 14 | 10 | 6 | 46 |
| `core.security` | Xavfsizlik asoslari | 8 | 10 | 8 | 6 | 32 |
| `core.testing` | Testlash asoslari | 10 | 10 | 8 | 4 | 32 |
| `core.sysdesign` | System design | 0 | 10 | 16 | 22 | 48 |
| `core.aicode` | AI yordamida kod va code review | 6 | 8 | 8 | 6 | 28 |
| `core.mobile` | Mobile umumiy | 6 | 8 | 6 | 4 | 24 |
| | **Jami** | **116** | **120** | **96** | **70** | **402** |

Eng ko'p so'raladigan mavzular:

**`core.dsa`**
- J: Big-O asoslari; massiv, linked list, stack, queue, hash map farqlari; chiziqli va binary search; oddiy saralashlar.
- M: hash map ichki tuzilishi va kolliziya; two pointers, sliding window; rekursiya; daraxt aylanishlari (BFS/DFS); saralash murakkabliklari.
- SM: heap va priority queue; graflar (eng qisqa yo'l, topologik saralash); dinamik dasturlash asoslari; amortizatsiya.
- S: vazifaga tuzilma tanlash trade-off'lari; LRU cache, trie, consistent hashing g'oyasi; xotira va vaqt murosasi.

**`core.oop`**
- J: inkapsulyatsiya, meros, polimorfizm, abstraksiya; interface va abstract class; composition va inheritance.
- M: SOLID har bir prinsipi misolda; Singleton, Factory, Builder, Observer, Strategy; immutability.
- SM: Dependency Injection va inversiya; Adapter, Decorator, Facade; anti-patternlar; coupling va cohesion.
- S: Clean/Hexagonal arxitektura chegaralari; DDD asoslari (aggregate, bounded context); patternni qachon qo'llamaslik.

**`core.sql`**
- J: SELECT, WHERE, ORDER BY, GROUP BY; JOIN turlari; primary/foreign key; NULL bilan ishlash.
- M: indekslar va ular qachon ishlamaydi; HAVING va WHERE; subquery va JOIN; normalizatsiya (1–3 NF); tranzaksiya va ACID.
- SM: izolyatsiya darajalari va anomaliyalar; EXPLAIN o'qish; window funksiyalar; N+1; deadlock; optimistic va pessimistic lock.
- S: replikatsiya va sharding; CAP; SQL va NoSQL tanlovi; nolga yaqin downtime bilan migratsiya; partitioning.

**`core.git`**
- J: commit, branch, merge, pull/push; `.gitignore`; clone va fork; konfliktni hal qilish.
- M: merge va rebase farqi; reset/revert/restore; stash; cherry-pick; pull request jarayoni.
- SM: branching strategiyalar (trunk-based, Git Flow); interaktiv rebase; bisect; tarixni tozalash xavflari.
- S: monorepo va polyrepo; reliz jarayoni va versiyalash.

**`core.http`**
- J: HTTP metodlari va status kodlari; so'rov/javob tuzilishi; REST asoslari; JSON; HTTP va HTTPS.
- M: idempotentlik; cookie, session, token; CORS; caching sarlavhalari; DNS va TCP asoslari; pagination.
- SM: HTTP/2 va HTTP/3; WebSocket, SSE, long polling; REST, GraphQL, gRPC tanlovi; rate limiting; API versiyalash.
- S: API gateway, load balancer turlari; retry, timeout, circuit breaker; idempotency key; backward compatibility.

**`core.security`**
- J: autentifikatsiya va avtorizatsiya farqi; parolni hash qilish; HTTPS nima uchun; SQL injection nima.
- M: XSS, CSRF; JWT tuzilishi va xavflari; OAuth 2.0 oqimlari; secret'larni saqlash.
- SM: OWASP Top 10; token refresh va revocation; input validatsiya qatlamlari; dependency zaifliklari.
- S: threat modeling; zero trust; shifrlash (at rest, in transit), kalitlarni boshqarish; audit va compliance.

**`core.testing`**
- J: unit, integration, e2e farqi; test piramidasi; assert; test nima uchun kerak.
- M: mock, stub, fake farqi; test izolyatsiyasi; code coverage ma'nosi; TDD sikli.
- SM: flaky test sabablari; test ma'lumotlari; contract testing; nimani test qilmaslik.
- S: test strategiyasi; CI'da test vaqti; production'da test (canary, feature flag).

**`core.sysdesign`**
- M: client-server, stateless servis; vertikal va gorizontal masshtab; cache qatlamlari; load balancer; message queue nima uchun.
- SM: cache invalidatsiya strategiyalari; DB replikatsiya; rate limiter; URL shortener, chat, feed kabi klassik dizaynlar; consistency modellari.
- S: sharding va partitioning; event-driven arxitektura, saga, outbox; ko'p regionli tizim; observability; LLM/RAG servisi dizayni (vector search, latency va xarajat, streaming); mikroservis chegaralari.

**`core.aicode`**
- J: AI yozgan kodni nega tekshirish kerak; prompt'ga maxfiy ma'lumot bermaslik; kod sharhida nimaga qaraladi.
- M: berilgan kodda xatoni topish (off-by-one, null, resurs yopilmagan); o'qiluvchanlik; nomlash; ortiqcha murakkablik.
- SM: AI kodidagi tipik muammolar (mavjud bo'lmagan API, xavfsizlik teshigi, chekka holatlar); review'da ustuvorlik; test bilan tekshirish.
- S: jamoada AI vositalari siyosati; arxitekturaviy qarorni himoya qilish; texnik qarzni baholash.

**`core.mobile`**
- J: ilova hayot sikli umumiy g'oyasi; ruxsatlar; native va cross-platform farqi.
- M: offline saqlash; push bildirishnomalar ishlash prinsipi; deep link; tarmoq xatolarini boshqarish.
- SM: offline-first va sinxronizatsiya; batareya va xotira; ilova hajmi; crash va ANR tahlili.
- S: reliz strategiyasi (staged rollout, feature flag); modulli arxitektura; xavfsiz saqlash; ko'p platformali kod ulashish.

## 4. Fieldlar

Har bir fieldda 6 ta mavzu, jami 70 savol (J 24, M 22, SM 14, S 10). Mavzular bo'yicha taqsimot bir xil:

| Mavzu o'rni | J | M | SM | S | Jami |
|---|---|---|---|---|---|
| 1 (til asoslari) | 6 | 5 | 2 | 1 | 14 |
| 2 | 5 | 4 | 2 | 1 | 12 |
| 3 | 4 | 4 | 3 | 2 | 13 |
| 4 | 4 | 4 | 3 | 2 | 13 |
| 5 | 3 | 3 | 2 | 2 | 10 |
| 6 (arxitektura) | 2 | 2 | 2 | 2 | 8 |
| **Jami** | **24** | **22** | **14** | **10** | **70** |

Fieldga ulanadigan umumiy bloklar va foydalanuvchiga ko'rinadigan jami savollar:

| Field | Umumiy bloklar | O'z | Umumiy | Jami |
|---|---|---|---|---|
| Android, iOS, Flutter | dsa, oop, git, http, security, testing, sysdesign, aicode, mobile | 70 | 344 | 414 |
| Frontend | dsa, git, http, security, testing, sysdesign, aicode | 70 | 274 | 344 |
| Python, Node.js, Java, Go, PHP, .NET | dsa, oop, sql, git, http, security, testing, sysdesign, aicode | 70 | 378 | 448 |
| QA | sql, git, http, security, testing, aicode | 70 | 228 | 298 |
| DevOps | git, http, security, sysdesign, aicode | 70 | 186 | 256 |
| Data/ML | dsa, sql, git, sysdesign, aicode | 70 | 222 | 292 |

Baza bo'yicha jami: 13 × 70 + 402 = **1312 savol**.

### 4.1 Android (Kotlin) — `android`

Mavzular: 1 `android.kotlin` Kotlin tili · 2 `android.components` Komponentlar va lifecycle · 3 `android.compose` Jetpack Compose · 4 `android.async` Coroutines va Flow · 5 `android.data` Ma'lumot qatlami · 6 `android.arch` Arxitektura, DI, performance

- **J:** `val`/`var`, null-safety (`?.`, `?:`, `!!`), data class, `when`, extension funksiya; Activity va Fragment lifecycle; Intent turlari; 4 ta asosiy komponent; Composable nima, `remember`, `State`; `suspend` funksiya nima; SharedPreferences va Room farqi.
- **M:** scope funksiyalar (`let`, `apply`, `run`, `also`, `with`); `sealed class`, `object`, `inline`; ViewModel nima uchun konfiguratsiya o'zgarishidan omon qoladi; recomposition nimadan boshlanadi; `LaunchedEffect`, `rememberSaveable`; `launch` va `async`; Dispatcher'lar; `Flow`, `StateFlow`, `SharedFlow` farqi; Room migratsiya; Retrofit/OkHttp interceptor.
- **SM:** structured concurrency, bekor qilish va `SupervisorJob`; xatolarni ushlash (`CoroutineExceptionHandler`); Compose stability, `derivedStateOf`, `key`, ortiqcha recomposition'ni topish; side-effect API tanlovi; process death va `SavedStateHandle`; WorkManager cheklovlari; Hilt scope'lari; memory leak sabablari; offline cache strategiyasi.
- **S:** modulli arxitektura va build vaqti; MVI/MVVM tanlovi va holat boshqaruvi; baseline profile, startup, R8; offline-first sinxronizatsiya va konflikt; KMP qachon o'zini oqlaydi; katta jamoada navigatsiya va feature chegaralari.

### 4.2 iOS (Swift) — `ios`

Mavzular: 1 `ios.swift` Swift tili · 2 `ios.ui` UIKit va view lifecycle · 3 `ios.swiftui` SwiftUI · 4 `ios.concurrency` GCD, async/await, actors · 5 `ios.data` Tarmoq va saqlash · 6 `ios.arch` Xotira, arxitektura, performance

- **J:** `let`/`var`, Optional va unwrap usullari; `struct` va `class` farqi; protocol; closure; UIViewController lifecycle; Auto Layout asoslari; `@State` nima; UserDefaults.
- **M:** value va reference semantikasi, copy-on-write; ARC, `weak` va `unowned`, retain cycle; `@State`, `@Binding`, `@ObservedObject`, `@StateObject`; GCD queue'lar, main thread qoidasi; `async/await`, `Task`; Codable; URLSession; delegate va closure tanlovi.
- **SM:** actor va `Sendable`, data race; `@MainActor`; SwiftUI view identity va qayta chizish; Combine va async sequence; Core Data / SwiftData konkurentligi; generics va `some`/`any`; Instruments bilan leak topish.
- **S:** modullashtirish (SPM), build vaqti; MVVM, TCA, VIPER tanlovi; ilova ishga tushish vaqti; UIKit va SwiftUI aralash loyihada chegaralar; background ishlar cheklovlari; xavfsiz saqlash (Keychain).

### 4.3 Flutter — `flutter`

Mavzular: 1 `flutter.dart` Dart tili · 2 `flutter.widgets` Widgetlar va lifecycle · 3 `flutter.state` State management · 4 `flutter.async` Future, Stream, isolate · 5 `flutter.data` Tarmoq, saqlash, platform channels · 6 `flutter.arch` Rendering, arxitektura, performance

- **J:** `final`, `const`, `static`; null-safety; StatelessWidget va StatefulWidget; `setState`; `BuildContext`; hot reload va hot restart; `Future` va `async/await`; forma validatsiyasi.
- **M:** StatefulWidget lifecycle (`initState`, `didChangeDependencies`, `dispose`); `Key` turlari; ephemeral va app state; Provider, Riverpod, Bloc farqi; `Stream`, `StreamBuilder`; mixin va extension; navigatsiya (Navigator 2.0, go_router); `const` konstruktor foydasi.
- **SM:** Widget, Element, RenderObject daraxtlari; ortiqcha rebuild'ni topish; isolate va `compute`; Bloc va Riverpod trade-off'lari katta ilovada; platform channel; `InheritedWidget` ichki ishlashi; jank sabablari.
- **S:** rendering pipeline (build, layout, paint), Impeller; clean architecture va modul chegaralari; native kod bilan integratsiya strategiyasi; ilova hajmi va startup; test strategiyasi (widget, golden, integration).

### 4.4 Frontend (JS/TS, React) — `frontend`

Mavzular: 1 `fe.js` JavaScript · 2 `fe.htmlcss` HTML va CSS · 3 `fe.react` React · 4 `fe.ts` TypeScript · 5 `fe.browser` Brauzer, tarmoq, Next.js · 6 `fe.arch` Performance, arxitektura, accessibility

- **J:** `var`/`let`/`const`, hoisting; `==` va `===`; closure; `this`; massiv metodlari; semantik HTML; box model, flexbox, grid; JSX, props va state, `useState`, `key` nima uchun; TS asosiy tiplar, `interface` va `type`.
- **M:** event loop, microtask va macrotask; Promise va `async/await`; prototip; `useEffect` dependency va cleanup; re-render nimadan boshlanadi; `useMemo`, `useCallback`, `memo`; custom hook; TS generics, union, narrowing; CSS specificity; CORS; localStorage va cookie.
- **SM:** reconciliation va Fiber; state boshqaruvi tanlovi (Context, Redux Toolkit, Zustand, TanStack Query); SSR, SSG, ISR, Server Components; hydration xatolari; Core Web Vitals; code splitting; TS utility va conditional tiplar; accessibility (ARIA, klaviatura); XSS'dan himoya.
- **S:** design system va komponent API; micro-frontend qachon kerak; katta ilovada state arxitekturasi; bundle va render performance byudjeti; monorepo; "noto'g'ri holatni tip bilan imkonsiz qilish"; test strategiyasi.

### 4.5 Python (Django/FastAPI) — `python`

Mavzular: 1 `py.lang` Python tili · 2 `py.idioms` Ma'lumot tuzilmalari va idiomalar · 3 `py.web` Django va FastAPI · 4 `py.async` Konkurentlik: GIL, asyncio · 5 `py.db` ORM va DB · 6 `py.arch` Arxitektura, caching, queue

- **J:** mutable va immutable tiplar; list, tuple, set, dict; `is` va `==`; mutable default argument; list comprehension; `*args`, `**kwargs`; virtual environment; Django MVT; FastAPI'da path va query parametr.
- **M:** dekorator, generator, context manager; GIL nima va nimaga ta'sir qiladi; threading, multiprocessing, asyncio tanlovi; `async def` va `def` FastAPI'da; Pydantic validatsiya; `Depends`; Django ORM `select_related` va `prefetch_related`, N+1; middleware; migratsiyalar.
- **SM:** event loop'ni bloklash; `__slots__`, descriptor, MRO; xotira boshqaruvi va reference counting; Celery va background task; ORM so'rovini optimallashtirish; caching (Redis) strategiyasi; type hints va mypy; test (pytest fixture).
- **S:** servis chegaralari, monolit va mikroservis; ML modelni FastAPI ortida past latency bilan xizmat qilish; idempotent task'lar; DB connection pool; observability; katta Django loyihasini bo'lish.

### 4.6 Node.js (Express/NestJS) — `node`

Mavzular: 1 `node.js` JavaScript/TypeScript asoslari · 2 `node.runtime` Event loop va runtime · 3 `node.web` Express va NestJS · 4 `node.async` Promise, stream, worker · 5 `node.db` DB, ORM, caching · 6 `node.arch` Arxitektura va masshtab

- **J:** closure, `this`, Promise; CommonJS va ES modullar; `npm`, `package.json`; Node.js nima uchun single-threaded; Express route va middleware; `process.env`.
- **M:** event loop fazalari, `process.nextTick` va `setImmediate`; xatolarni boshqarish (async middleware); stream va buffer; NestJS modul, provider, DI; guard, pipe, interceptor; ORM (Prisma, TypeORM) va N+1; JWT autentifikatsiya.
- **SM:** event loop'ni bloklash va uni aniqlash; worker threads va cluster; memory leak topish; backpressure; graceful shutdown; queue (BullMQ); connection pool; caching.
- **S:** mikroservislar va aloqa (HTTP, gRPC, broker); monolitni bo'lish; gorizontal masshtab va stateless dizayn; observability; API versiyalash; TypeScript bilan katta kod bazasi.

### 4.7 Java (Spring) — `java`

Mavzular: 1 `java.lang` Java tili va OOP · 2 `java.collections` Collections va Stream API · 3 `java.spring` Spring Core va Boot · 4 `java.concurrency` Ko'p oqimlilik, virtual threads · 5 `java.data` JPA/Hibernate, tranzaksiyalar · 6 `java.arch` JVM, GC, mikroservislar

- **J:** primitiv va reference tiplar; `equals` va `hashCode`; `String` immutability; `final`, `static`; exception turlari; `List`, `Set`, `Map`; interface va abstract class; Spring bean nima, `@Component`, `@Autowired`.
- **M:** `HashMap` ichki tuzilishi; Stream API, `Optional`; generics va wildcard; record, sealed class; `synchronized`, `volatile`, `ExecutorService`; bean scope'lari; constructor va field injection; `@Transactional` ishlashi; JPA N+1, lazy va eager.
- **SM:** Spring Boot auto-configuration va `@Conditional`; proxy va `@Transactional` tuzoqlari (self-invocation); tranzaksiya propagation va izolyatsiya; `CompletableFuture`; virtual threads (Java 21) va pinning; Spring Security filter zanjiri; Hibernate cache; thread dump tahlili.
- **S:** GC tanlovi va sozlash; WebFlux va MVC tanlovi; mikroservis patternlari (saga, outbox, circuit breaker); Kafka bilan ishonchli yetkazish; native image; kuzatuvchanlik va performance diagnostikasi.

### 4.8 Go — `go`

Mavzular: 1 `go.lang` Go tili · 2 `go.types` Slice, map, interface · 3 `go.concurrency` Goroutine va channel · 4 `go.web` net/http, context, middleware · 5 `go.data` DB va tranzaksiya · 6 `go.arch` Profiling va arxitektura

- **J:** tiplar va zero value; pointer; `struct` va metod; xatolarni qaytarish (`error`); `defer`; slice va array farqi; `map`; paket va modul; goroutine nima.
- **M:** slice ichki tuzilishi (len, cap, `append`); interface va `nil` interface tuzog'i; buferlangan va buferlanmagan channel; `select`; `sync.WaitGroup`, `Mutex`; `context` bekor qilish va timeout; `errors.Is`/`As`, wrapping; `http.Handler` va middleware.
- **SM:** data race va race detector; goroutine leak; worker pool, fan-in/fan-out; scheduler (G-M-P) asoslari; escape analysis; generics; `database/sql` pool va tranzaksiya; `pprof`.
- **S:** GC va xotira sozlash; graceful shutdown; servis tuzilishi va dependency chegaralari; yuqori yuklamada konkurentlik patternlari; gRPC; kuzatuvchanlik.

### 4.9 PHP (Laravel) — `php`

Mavzular: 1 `php.lang` PHP tili · 2 `php.oop` OOP va Composer · 3 `php.laravel` Laravel asoslari · 4 `php.eloquent` Eloquent va DB · 5 `php.async` Queue, cache, events · 6 `php.arch` Arxitektura va performance

- **J:** tiplar va type juggling, `==` va `===`; massivlar; `include`/`require`; superglobals; class, interface, trait; namespace va Composer autoload; Laravel route, controller, Blade; migratsiya.
- **M:** PHP 8 imkoniyatlari (typed properties, match, enum, readonly, attributes); so'rov hayot sikli; middleware; service container va provider; Eloquent relation, eager loading, N+1; validation va Form Request; autentifikatsiya (Sanctum).
- **SM:** queue va job, retry; cache strategiyasi; event va listener; DB tranzaksiya va lock; Eloquent va Query Builder performance; OPcache; test (PHPUnit, Pest).
- **S:** katta Laravel loyihasida modul chegaralari, DDD; Octane; gorizontal masshtab (session, queue, cache); monolitni bo'lish; API versiyalash.

### 4.10 .NET (C#) — `dotnet`

Mavzular: 1 `net.csharp` C# tili · 2 `net.types` Tiplar, LINQ, collections · 3 `net.aspnet` ASP.NET Core · 4 `net.async` async/await va TPL · 5 `net.data` EF Core · 6 `net.arch` GC, performance, arxitektura

- **J:** value va reference tiplar; `class`, `struct`, `record`; `string` immutability; `interface` va abstract class; exception; `List`, `Dictionary`; LINQ asoslari; controller va minimal API; DI nima.
- **M:** boxing/unboxing; `IEnumerable` va `IQueryable`; deferred execution; `async/await` ishlashi, `Task` va `ValueTask`; DI lifetime (transient, scoped, singleton); middleware pipeline; EF Core tracking, lazy va eager loading, migratsiya.
- **SM:** deadlock va `ConfigureAwait`; `IDisposable` va `using`; GC avlodlari; `Span<T>`; EF Core so'rov optimallashtirish; captive dependency; background service; autentifikatsiya va avtorizatsiya policy.
- **S:** CQRS va MediatR qachon; mikroservislar, messaging; performance profillash; modulli monolit; yuqori yuklamada thread pool va konkurentlik.

### 4.11 QA — `qa`

Mavzular: 1 `qa.theory` Testlash nazariyasi · 2 `qa.docs` Test hujjatlari va bug report · 3 `qa.design` Test dizayn texnikalari · 4 `qa.api` API va DB testlash · 5 `qa.auto` Avtomatlashtirish · 6 `qa.process` Strategiya, CI, jarayon

- **J:** testlash turlari va darajalari; verification va validation; smoke, sanity, regression; test case tuzilishi; bug report tarkibi; severity va priority; bug hayot sikli; SDLC va STLC.
- **M:** ekvivalent bo'linish, chegaraviy qiymatlar, decision table, state transition; exploratory testing; API testlash (Postman, status kodlar, sxema); SQL bilan ma'lumot tekshirish; test piramidasi; Agile/Scrum'da QA roli.
- **SM:** avtomatlashtirish (Playwright, Selenium), locator strategiyasi, Page Object; kutishlar va flaky testlar; CI'da testlar; performance testlash asoslari (k6, JMeter); mobil testlash xususiyatlari; test ma'lumotlarini boshqarish.
- **S:** test strategiyasi va risk asosida testlash; avtomatlashtirish ROI; sifat metrikalari; shift-left; production'da test; jamoa jarayonini qurish.

### 4.12 DevOps — `devops`

Mavzular: 1 `ops.linux` Linux va shell · 2 `ops.docker` Docker va konteynerlar · 3 `ops.cicd` CI/CD · 4 `ops.k8s` Kubernetes · 5 `ops.iac` Terraform, IaC, cloud · 6 `ops.observability` Monitoring, SRE, xavfsizlik

- **J:** fayl ruxsatlari, jarayonlar, `grep`/`awk`/`sed`; SSH; image va container farqi; Dockerfile asoslari; CI va CD nima; Pod, Deployment, Service nima; IaC nima uchun.
- **M:** Docker layer va cache, multi-stage build; tarmoq va volume; pipeline bosqichlari, artifact; liveness va readiness probe; ConfigMap, Secret; Ingress; rolling update va rollback; Terraform state, plan/apply; systemd, loglar.
- **SM:** monorepo uchun pipeline (to'liq qayta build'siz); deployment strategiyalari (blue-green, canary); resource request/limit, HPA; PodDisruptionBudget va node drain; Terraform remote state, lock, drift; yarim yo'lda to'xtagan apply; Prometheus, alerting; secret boshqaruvi.
- **S:** ko'p regionli infratuzilma; GitOps (Argo CD); observability stack dizayni; SLO, error budget, incident jarayoni; xarajatni optimallashtirish; platform engineering; supply chain xavfsizligi.

### 4.13 Data/ML — `ml`

Mavzular: 1 `ml.python` Python, pandas, numpy · 2 `ml.stats` Statistika va ehtimollik · 3 `ml.classic` Klassik ML · 4 `ml.eval` Metrikalar va validatsiya · 5 `ml.dl` Deep learning va LLM · 6 `ml.ops` MLOps va tizim dizayni

- **J:** pandas DataFrame amallari, `groupby`, `merge`; numpy broadcasting; o'rtacha, mediana, dispersiya; supervised va unsupervised; train/test bo'linishi; overfitting nima; accuracy.
- **M:** bias-variance; regularizatsiya (L1, L2); precision, recall, F1, ROC-AUC; cross-validation; data leakage; feature engineering; chiziqli va logistik regressiya, daraxtlar, gradient boosting; gipoteza testi, p-value.
- **SM:** nomutanosib sinflar; hyperparametr qidirish; neyron tarmoq asoslari (backprop, optimizatorlar); transformer va attention g'oyasi; embedding; RAG nima va qachon; A/B test dizayni; model drift.
- **S:** model serving (latency, batching, caching); LLM tizim dizayni (retrieval, vector search, baholash, xarajat); feature store; monitoring va qayta o'qitish; fine-tuning va RAG tanlovi.

## 5. Yozish tartibi (4-bosqich partiyalari)

O'zbekiston bozoridagi ulush bo'yicha:

1. Android (1-bosqichda namuna, keyin to'ldiriladi) + `core.mobile`
2. `core.dsa`, `core.git`, `core.http`, `core.sql` (eng ko'p fieldga ulanadi)
3. Frontend
4. Python
5. Node.js
6. `core.oop`, `core.security`, `core.testing`, `core.sysdesign`, `core.aicode`
7. Java
8. Flutter
9. Go, PHP
10. .NET, iOS
11. QA, DevOps, Data/ML

Har bir savol `reviewed: false` bilan yoziladi va validator testidan o'tadi.

Tugallangan mavzular `ContentAssetsTest.completedTopics` ro'yxatida turadi: test har bir tugallangan mavzuda darajalar bo'yicha savollar soni shu hujjatdagi rejaga teng ekanini tekshiradi.

| Partiya | Holat | Savollar |
|---|---|---|
| 1. Android (6 mavzu) + `core.mobile` | yozildi (2026-10-02), inson tekshiruvi kutilmoqda | 94 / 1312 |
| 2. `core.git` + `core.http` | yozildi (2026-10-03), inson tekshiruvi kutilmoqda | 172 / 1312 |
| 3. `core.sql` + `core.dsa` | yozildi (2026-10-03), inson tekshiruvi kutilmoqda | 286 / 1312 |
| 4. Frontend (6 mavzu) | yozildi (2026-10-03), inson tekshiruvi kutilmoqda | 356 / 1312 |
| 5. Python (6 mavzu) | yozildi (2026-10-03), inson tekshiruvi kutilmoqda | 426 / 1312 |
| 6. Node.js (6 mavzu) | yozildi (2026-10-03), inson tekshiruvi kutilmoqda | 496 / 1312 |
| 7. `core.oop` + `core.security` + `core.testing` | yozildi (2026-10-03), inson tekshiruvi kutilmoqda | 606 / 1312 |

## 6. Manbalar

- [State of Dev in Uzbekistan 2025](https://stateofdev.uz/)
- [IT Park: In-Demand Professions of 2025](https://it-park.uz/en/itpark/news/in-demand-professions-of-2025-how-to-build-a-career-in-ai-data-cloud-and-game-development)
- [Top 50 Android developer interview questions in 2026](https://www.codinginterview.com/guide/android-developer-interview-questions/)
- [Top 40 Android Developer Interview Questions (2026)](https://getdeveloper.in/top-40-android-developer-interview-questions-2026/)
- [Frontend Developer Interview Questions 2026 — KORE1](https://www.kore1.com/frontend-developer-interview-questions/)
- [Java Developer Interview Questions 2026 — KORE1](https://www.kore1.com/java-developer-interview-questions/)
- [Python Developer Interview Questions 2026 — KORE1](https://www.kore1.com/python-developer-interview-questions/)
- [Flutter Developer Interview Questions & Answers (2026 Guide)](https://www.acemyinterviews.io/interview/flutter-developer)
- [DevOps Engineer Interview Questions 2026 — KORE1](https://www.kore1.com/devops-engineer-interview-questions/)
- [What Actually Changed in Tech Interviews in 2026](https://www.techinterview.org/post/3233475417/what-changed-tech-interviews-2026/)
- [Senior Software Engineer Interviews in 2026](https://interviewdb.com/blog/senior-software-interviews-beyond-leetcode-2026/)
