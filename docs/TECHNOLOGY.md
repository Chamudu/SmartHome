# Technology Reference

## Android

| Technology | Purpose in Smart Home |
| --- | --- |
| Kotlin 2.2 | Null-safe domain models, exhaustive enums/sealed profiles, coroutines, and application logic |
| Jetpack Compose | Declarative screens, dialogs, grid rendering, pointer gestures, and reactive recomposition |
| Material 3 | Components, typography, light/dark color schemes, accessibility-sized controls |
| AndroidX Activity Compose | Hosts Compose from the single Android activity |
| Lifecycle Runtime Compose | Collects `StateFlow` only while the UI lifecycle is active |
| Lifecycle ViewModel | Retains UI state and owns coroutine/listener jobs across recomposition |
| Kotlin Coroutines and Flow | Structured asynchronous work and observable real-time streams |
| Firebase Authentication | Establishes owner/simulator identities |
| Cloud Firestore Android SDK | Real-time snapshots, atomic document writes, batches, queries, and local caching |
| Firebase Android BoM | Keeps Firebase library versions mutually compatible |
| Google Services plugin | Converts local Firebase Android configuration into generated resources |
| Coil 2.7 | Loads and caches HTTPS mock-camera snapshots in Compose with asynchronous state callbacks |
| JUnit 4 and coroutine-test | Tests geometry, status rules, and ViewModel asynchronous behavior |

## Simulator and Firebase tooling

| Technology | Purpose in Smart Home |
| --- | --- |
| React 19 | Declarative browser hardware-console UI |
| TypeScript | Typed device-twin and Firebase data structures |
| Vite | Development server and production simulator bundling |
| Firebase Web SDK | Auth, document listeners, and simulator reported-state writes |
| Vitest | Executes Firestore authorization tests |
| Firebase Rules Unit Testing | Creates authenticated/unauthenticated emulator contexts and assertions |
| Firebase Emulator Suite | Isolated rule evaluation without production data |
| Oxlint | Static checks for the simulator source |
| Firebase CLI | Project selection, emulators, rules compilation, and deployment |
| Node.js 22 | Runtime for tooling and deployed TypeScript Cloud Functions |
| Firebase Functions v2 | Firestore update trigger and scheduled trusted safety enforcement |
| Firebase Admin SDK | Privileged transactional device, event, and alert writes after backend validation |
| Cloud Scheduler integration | Once-per-minute due-device scan independent of client lifecycle |

## Engineering concepts

- **MVVM:** Compose renders state; ViewModel validates intent and coordinates repositories.
- **Repository pattern:** Firebase SDK details are hidden behind replaceable interfaces.
- **Observer/reactive programming:** snapshot listeners become Flow and drive recomposition.
- **Device twin:** desired state is a command; reported state is hardware-confirmed truth.
- **Polymorphism/sum types:** one common device plus sealed profile configuration.
- **NoSQL modeling:** home-scoped documents and bounded embedded switch channels.
- **Authentication vs authorization:** Auth proves identity; Rules decide permitted data operations.
- **Least privilege:** owner/operator and simulator write different protected fields.
- **Direct manipulation:** grid gestures translate spatial intent into logical coordinates.
- **Defense in depth:** ViewModel validation improves feedback; Security Rules protect the cloud boundary.
- **Structured concurrency:** ViewModel jobs are cancelled with lifecycle/sign-out and listeners remove
  registrations through `awaitClose`.
- **Atomicity and idempotency:** document writes initialize complete state; request IDs prepare commands
  for safe correlation and retry handling.
- **Optimistic transactions:** channel-array writers re-read and retry on conflict, preventing unrelated
  channel updates from being lost.
- **Trusted time and at-least-once delivery:** server timestamps establish deadlines; transactions and
  deterministic event IDs make repeated function execution safe.
- **Asynchronous image pipeline:** Coil performs network fetch, decoding, size-aware rendering, and
  memory/disk caching away from the main UI work while Compose displays loading/content/error states.
