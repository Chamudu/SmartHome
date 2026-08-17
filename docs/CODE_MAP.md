# Code Map

## Android request and data flow

```text
Compose event
  → OutletViewModel validation/state
  → repository interface
  → Firebase implementation
  → Firestore Security Rules
  → snapshot listener / Flow
  → StateFlow
  → lifecycle-aware Compose recomposition
```

## Android source responsibilities

| Source | Responsibility |
| --- | --- |
| `MainActivity.kt` | Android entry point and root theme/application composition |
| `DeviceStatus.kt` | Operational status and whether normal power commands are safe |
| `OutletDevice.kt` | Proven outlet twin, power state, command state, and pending/control derivations |
| `SmartDevice.kt` | Shared profile discriminator, sealed configuration, per-channel switch twin, and creation request |
| `HomeAlert.kt` | Persistent alert identity, severity, message, related device, and timestamp |
| `DeviceEvent.kt` | Append-only state transition shape and event-origin categories |
| `domain/usage/UsageCalculator.kt` | Pairs state transitions into activation and active-duration totals |
| `domain/usage/EnergyEstimator.kt` | Converts active duration and assumed wattage into kWh/cost estimates |
| `FloorPlan.kt` | Floor, conventional level-derived names, half-open room rectangles, edges, and violations |
| `FloorLayoutValidator.kt` | Pure floor, room, overlap, and device-coordinate validation |
| `OutletRepository.kt` | Authenticated identity, device/alert/event observation, control, scheduling, and placement boundary |
| `FloorRepository.kt` | Floor and room observation and mutation boundary |
| `FirebaseOutletRepository.kt` | Firebase Auth, device/alert/event listeners, mapping, creation, commands, and placement |
| `data/connectivity/` | Android network observation exposed as lifecycle-independent `StateFlow` |
| `data/recovery/` | Firestore error classification and bounded exponential retry helpers |
| `FirebaseFloorRepository.kt` | Floor/room listeners, mapping, writes, deletion reference checks, and batch cascade |
| `OutletViewModel.kt` | Screen state, listener lifecycle, validation, selection, and user-action orchestration |
| `OutletScreen.kt` | Authentication, Devices/Layout/Profile navigation, filters, controls, snapshot, and schedule UI |
| `FloorDashboardSection.kt` | Floor selector/grid, gestures, previews, CRUD dialogs, and Add Device form |
| `Theme.kt` | Branded Material 3 schemes and active-scheme-aware semantic status colors |
| `SmartHomeIcons.kt` | Small dependency-free vector icon set for navigation, profiles, status, and scheduling |

## Simulator responsibilities

| Source | Responsibility |
| --- | --- |
| `firebase.ts` | Reads local Vite environment configuration and initializes Auth/Firestore |
| `types.ts` | TypeScript device, event, floor-summary, and status contracts |
| `useDeviceSimulator.ts` | Auth lifecycle, realtime floor/device listeners, transactional device/channel acknowledgement, and reports |
| `App.tsx` | Configuration, sign-in, name-resolved floor/profile filters, multi-device telemetry, and simulator controls |
| `App.css` / `index.css` | Responsive diagnostic-console presentation and global styling |
| `scripts/seed-demo.mjs` | Authenticated, idempotent demo-floor/room/device creation |

## Firebase responsibility

`firebase/firestore.rules` denies unmatched access, resolves home membership, separates owner/operator
desired writes from simulator reported writes, validates floor/room geometry shape, validates placement,
and enforces complete profile-specific creation documents. Emulator tests seed trusted fixtures with
rules disabled, then make assertions through real authenticated rule contexts.

## Backend automation responsibilities

| Source | Responsibility |
| --- | --- |
| `functions/src/safetyDecision.ts` | Pure timer-transition, deadline, and due-state decisions |
| `functions/src/index.ts` | Firestore activation trigger, scheduled scan, transactional cutoff/event/alert writes |
| `functions/src/safetyDecision.test.ts` | Boundary, transition, duration, and retry/idempotency unit tests |
| `functions/src/usageCalculator.ts` | TypeScript mirror of event interval aggregation |
| `functions/src/energyEstimator.ts` | TypeScript mirror of energy and cost estimation |
| `functions/src/lightSchedule.ts` | Pure IANA-timezone daytime/overnight light-state decision |
| `functions/src/lightSchedule.test.ts` | Schedule boundaries, overnight, malformed-input, and timezone tests |
| `firebase/firestore.indexes.json` | Composite collection-group indexes for due safety and enabled light lookup |

The Admin SDK bypasses client rules, so the function transaction revalidates profile, reported state,
duration, and deadline. Deterministic IDs make repeated scheduled delivery converge on one event/alert.
