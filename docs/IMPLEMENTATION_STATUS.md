# Implementation Status

Status date: 2026-07-28

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
- Visual device dashboard with a blue home header, profile icons, status summary tiles, rounded cards,
  and display names instead of internal floor/room identifiers.
- Convention-based floor names derived from unique levels during creation, with editable names afterward.
- Light schedule editor with 24-hour time, IANA timezone, overnight-window support, and role-restricted
  Firestore updates.
- Timezone-aware scheduled light evaluator implemented for local Functions verification.
- Filterable device dashboard, accessible status-aware floor markers, and responsive launcher artwork.
- Dedicated Profile tab with the authenticated email and sign-out moved out of the home header.
- HTTPS mock camera snapshot rendering with honest mock labeling, loading feedback, failure fallback,
  content description, memory caching, and disk caching through Coil.
- Energy estimate reporting in the Usage tab: estimated kilowatt-hours and cost derived from active
  duration times assumed per-profile wattage, with mirrored TypeScript calculators for Functions and the
  simulator.

## Current verification evidence

| Layer | Evidence |
| --- | --- |
| Android domain/ViewModel | 50 passing JUnit tests |
| Android packaging | `:app:assembleDebug` succeeds |
| Android static analysis | `:app:lintDebug` succeeds with 0 errors |
| Firestore authorization/schema | 33 passing emulator-backed Vitest tests |
| Cloud Functions | TypeScript build and 24 Vitest decision tests pass |
| Simulator | TypeScript typecheck, Oxlint, and Vite production build succeed |
| Cloud rules | Tested rules deployed successfully to the development Firebase project |
| Physical integration | Outlet synchronization and earlier floor lifecycle accepted on a phone |

## Implemented but awaiting physical acceptance

- Branded theme on representative light/dark system modes.
- Automatic cutoff and persistent alert display after production Functions deployment.
- Scheduled light execution after production Functions deployment.

## Not implemented yet

- Touch resize/move for existing rooms; device movement currently uses a coordinate form after tapping.
- Editing an existing safety duration and production deployment of the verified cutoff functions.
- Camera upload and optional second-phone camera node; URI snapshot rendering is complete.
- Executing the seed against a target environment still requires owner credentials exported locally.
- Activity reporting and energy estimation are implemented; offline demonstration, CI, and signed
  release packaging remain.

## Active branch

`feature/ui-ux-overhaul` contains the visual refinement, profile navigation, launcher, accessibility,
and mock-camera work. It remains separate until physical acceptance is complete.
