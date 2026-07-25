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
| `HomeAlert.kt` | Persistent alert identity, severity, message, related device, and trusted timestamp |
| `FloorPlan.kt` | Floor, half-open room rectangles, computed edges, and validation violations |
| `FloorLayoutValidator.kt` | Pure floor, room, overlap, and device-coordinate validation |
| `OutletRepository.kt` | Authentication, device/alert observation, creation, control, and placement boundary |
| `FloorRepository.kt` | Floor and room observation and mutation boundary |
| `FirebaseOutletRepository.kt` | Firebase Auth, device/alert listeners, mapping, creation, commands, and placement |
| `FirebaseFloorRepository.kt` | Floor/room listeners, mapping, writes, deletion reference checks, and batch cascade |
| `OutletViewModel.kt` | Screen state, listener lifecycle, validation, selection, and user-action orchestration |
| `OutletScreen.kt` | Authentication, Devices/Layout navigation, profile controls, and status explanations |
| `FloorDashboardSection.kt` | Floor selector/grid, gestures, previews, CRUD dialogs, and Add Device form |
| `Theme.kt` | Branded light/dark Material 3 color schemes |

## Simulator responsibilities

| Source | Responsibility |
| --- | --- |
| `firebase.ts` | Reads local Vite environment configuration and initializes Auth/Firestore |
| `types.ts` | TypeScript twin/status contract for the current outlet |
| `useDeviceSimulator.ts` | Auth lifecycle, device listener, transactional device/channel acknowledgement, and reports |
| `App.tsx` | Configuration, sign-in, filters, multi-device telemetry, and simulator controls |
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
| `firebase/firestore.indexes.json` | Composite collection-group index for due safety-device lookup |

The Admin SDK bypasses client rules, so the function transaction revalidates profile, reported state,
duration, and deadline. Deterministic IDs make repeated scheduled delivery converge on one event/alert.
