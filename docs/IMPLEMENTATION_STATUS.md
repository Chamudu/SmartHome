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
- Branded Material 3 light/dark schemes and status text plus semantic container colors.

## Current verification evidence

| Layer | Evidence |
| --- | --- |
| Android domain/ViewModel | 16 passing JUnit tests |
| Android packaging | `:app:assembleDebug` succeeds |
| Firestore authorization/schema | 20 passing emulator-backed Vitest tests |
| Simulator | TypeScript typecheck, Oxlint, and Vite production build succeed |
| Cloud rules | Tested rules deployed successfully to the development Firebase project |
| Physical integration | Outlet synchronization and earlier floor lifecycle accepted on a phone |

## Implemented but awaiting physical acceptance

- Branded theme on representative light/dark system modes.
- Drag-to-prefill room creation and long-press-to-prefill device creation.
- Creation and reactive display of every profile from Android.

The last install attempt could not run because no ADB device was connected; this is an environment state,
not an APK compilation failure.

## Not implemented yet

- Tap selection and touch resize/move for existing rooms and device markers.
- Per-channel multi-switch commands.
- Safety configuration editing, trusted Cloud Function cutoff, events, and alerts.
- Scheduled light execution.
- Camera snapshot rendering/upload and optional second-phone camera node.
- Generalized multi-device simulator and deterministic two-floor seed script.
- Activity reporting, offline demonstration, CI, and signed release packaging.

## Active branch

`feature/device-dashboard` contains the device-dashboard and direct-manipulation work. It remains
separate from `develop` until physical acceptance and simulator compatibility are complete.
