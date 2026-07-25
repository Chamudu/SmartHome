# Implementation Status

Status date: 2026-07-25

## Verified implementation

- Native Android foundation using Kotlin, Compose, and Material 3.
- Firebase Email/Password authentication with restored sessions.
- Deny-by-default, role-based Firestore Security Rules.
- Bidirectional device-twin synchronization for `main-outlet` between Android and the React simulator.
- Desired versus reported state, request correlation, pending state, and hardware error/disconnection.
- Real-time floor and selected-room listeners.
- Floor create, select, rename, resize, confirm-delete, and unique-level validation.
- Rectangular room create, edit, confirm-delete, boundary validation, overlap prevention, and shared edges.
- Device placement with floor and optional inferred room references.
- Safe prevention of floor/room deletion or resize that would orphan a currently observed device.
- Shared polymorphic device collection and secure creation for outlet, multi-switch, safety outlet, light,
  and camera profiles.
- Touch-first room drag preview and long-press Add Device coordinate prefill.
- Tap device marker details, move with placement revalidation, and confirmed deletion.
- Branded Material 3 light/dark schemes and status text plus semantic container colors.
- Multi-device simulator collection listener, floor/profile filters, automatic command acknowledgement,
  and independent status/error/disconnection reports.
- Authenticated deterministic seed tool for two floors, four rooms, and all five profiles.
- Node 22/TypeScript second-generation Cloud Functions that start safety timers and enforce due cutoffs.
- Idempotent cutoff transactions that update device state and create one event plus one persistent alert.
- Real-time safety-alert observation and critical alert cards in Android.
- Independent two-, three-, and five-channel switch controls with transactional Android requests and
  simulator acknowledgements/manual reports.
- Task-focused Devices/Layout tabs and direct power switches for outlet, safety-outlet, and light cards.
- Explicit pending/error/disconnected explanations and clearer wording for unconfigured light automation.

## Current verification evidence

| Layer | Evidence |
| --- | --- |
| Android domain/ViewModel | 23 passing JUnit tests |
| Android packaging | `:app:assembleDebug` succeeds |
| Firestore authorization/schema | 30 passing emulator-backed Vitest tests |
| Cloud Functions | TypeScript build and 5 Vitest state-machine tests pass |
| Simulator | TypeScript typecheck, Oxlint, and Vite production build succeed |
| Cloud rules | Tested rules deployed successfully to the development Firebase project |
| Physical integration | Outlet synchronization and earlier floor lifecycle accepted on a phone |

## Implemented but awaiting physical acceptance

- Branded theme on representative light/dark system modes.
- Automatic cutoff and persistent alert display after production Functions deployment.

## Not implemented yet

- Touch resize/move for existing rooms; device movement currently uses a coordinate form after tapping.
- Editing an existing safety duration and production deployment of the verified cutoff functions.
- Scheduled light execution.
- Camera snapshot rendering/upload and optional second-phone camera node.
- Executing the seed against a target environment still requires owner credentials exported locally.
- Activity reporting, offline demonstration, CI, and signed release packaging.

## Active branch

`feature/device-dashboard` contains the device-dashboard and direct-manipulation work. It remains
separate from `develop` until physical acceptance and simulator compatibility are complete.
