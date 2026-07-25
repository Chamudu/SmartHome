# Architecture

## Recommended technology stack

| Area | Technology | Why it fits |
| --- | --- | --- |
| Mobile | Kotlin, Jetpack Compose, Material 3 | Modern native UI, lifecycle integration, and strong Android tooling |
| Mobile architecture | MVVM, repositories, Kotlin coroutines and Flow | Separates UI/state/data and maps naturally to real-time streams |
| Dependency injection | Constructor defaults/manual injection currently; Hilt is optional later | Keeps the current graph small while repositories remain replaceable in tests |
| Local/offline data | Firestore Android cache | Avoids premature duplication; Room remains an option for durable reporting data |
| Cloud database | Firebase Cloud Firestore | Real-time listeners, offline support, flexible document model |
| Identity | Firebase Authentication | Provides user identity needed by database security rules |
| Backend automation | Firebase Cloud Functions using TypeScript | Trusted event/timer execution for cutoffs and schedules |
| Notifications | Firebase Cloud Messaging | Push alerts for safety cutoffs |
| Images | Firebase Storage or bundled demo assets | Supports floor plans and mock camera snapshots |
| Simulator | React, TypeScript, Vite | Quick web UI, typed shared concepts, Firebase Web SDK support |
| Testing | JUnit, Compose UI tests, Firebase Emulator Suite, Vitest | Covers domain logic, UI, backend, rules, and simulator |
| CI | GitHub Actions | Repeatable checks and visible engineering practice |

## Why native Android

Kotlin and Jetpack Compose provide direct access to the Android lifecycle, notification system,
background work APIs, and modern reactive UI primitives. They also avoid adding a cross-platform
runtime when Android is the only required mobile target. The simulator remains a web application
because it represents a separate hardware surface rather than another mobile screen.

## System shape

```text
Android app  <---- realtime listeners/writes ---->  Cloud Firestore
                                                       ^       |
Web simulator <---- realtime listeners/writes --------+       |
                                                               v
                                              Cloud Functions / scheduler
                                                       |
                                                       v
                                                alerts + FCM push
```

Firestore is the synchronization hub. A UI write is optimistic but not authoritative for safety.
Backend code validates time-sensitive rules, writes final state, and records an immutable event.
Both clients subscribe to the affected documents and therefore redraw without a refresh button.

## Repository layout

```text
SmartHome/
├── app/                    Android application module
├── functions/              Trusted TypeScript Cloud Functions
├── simulator/              React/TypeScript application
├── firebase/               Firestore rules, indexes, and emulator tests
├── docs/                   Product and engineering documentation
├── build.gradle.kts        Root Android build configuration
└── firebase.json           Firebase CLI and emulator configuration
```

Cloud Functions and a repeatable authenticated seed script are implemented. CI workflows remain planned.

## Core engineering concerns

- Client-server architecture and source of truth
- Observer/reactive programming, Kotlin Flow, and Compose state
- MVVM and repository pattern
- NoSQL document modeling and denormalization
- Authentication versus authorization
- Security rules and least privilege
- Event-driven and scheduled serverless functions
- Idempotency, race conditions, timestamps, and eventual consistency
- Offline-first behavior and conflict handling
- Unit, integration, UI, and end-to-end testing
- Git branching, pull requests, CI, and traceable contributions

Each significant feature should include a traceable requirement, an explicit design decision where
appropriate, implementation code, automated tests, and an entry in the demo runbook.
