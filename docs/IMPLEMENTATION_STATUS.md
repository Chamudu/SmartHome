# Implementation Status

Status date: 2026-08-16

## Verified implementation

- Native Android client using Kotlin, Jetpack Compose, Material 3, MVVM, repositories, coroutines, and
  `Flow`.
- Firebase Email/Password authentication with restored sessions and home-scoped role authorization.
- Deny-by-default Firestore Security Rules with emulator tests for owner, simulator, outsider, and
  unauthenticated access.
- Bidirectional device-twin synchronization between Android and the React simulator, including request
  correlation, pending state, acknowledgement, errors, and disconnection.
- Multi-floor grid layouts with unique levels, rectangular rooms, validation, touch drag creation,
  long-press device placement, editing, and protected deletion.
- Outlet, two/three/five-channel multi-switch, safety outlet, scheduled light, and mock-camera profiles.
- Independent transactional multi-switch channel commands and simulator reports.
- Material 3 light/dark presentation, semantic device states, profile icons, dashboard filtering, and a
  dedicated Profile destination.
- HTTPS mock camera snapshots with loading, failure, cache, captured-time, and connectivity presentation.
- Node 22/TypeScript safety and light-schedule Functions with pure decision logic, idempotent cutoff
  transactions, events, and alerts.
- Append-only device state events written by the simulator and trusted automation events written by
  Cloud Functions. Human clients cannot create or rewrite event history.
- Android Usage views for Today, 7 days, and 30 days, including activation count, active duration,
  recent events, per-device estimates, multi-switch channel breakdown, whole-home estimated kWh, and
  estimated cost.
- Matching Kotlin and TypeScript usage/energy calculators with explicit handling for duplicate,
  unpaired, pre-period, and still-active intervals.
- Realtime operational and safety-alert observation. Simulator operational alerts use the canonical
  alert shape and cannot impersonate trusted safety-cutoff alerts.
- Android connectivity observation, cached-state messaging, listener retry with exponential backoff,
  and in-memory command retry after reconnection.
- Deterministic authenticated seed tooling and a browser hardware dashboard covering all five profiles.

## Current verification evidence

| Layer | Evidence on `dev` |
| --- | --- |
| Android domain/ViewModel | 61 passing JUnit tests |
| Android packaging | `:app:assembleDebug` succeeds |
| Android static analysis | `:app:lintDebug` succeeds |
| Firestore authorization/schema | 49 emulator-backed Vitest tests after integration hardening |
| Cloud Functions | TypeScript build and 24 Vitest tests pass |
| Simulator | TypeScript typecheck, Oxlint, and Vite production build succeed |
| Physical integration | Previous outlet/floor flows accepted; merged usage/recovery changes still need phone acceptance |

The simulator build currently reports a non-failing large-chunk warning: its main minified JavaScript
bundle is approximately 777 KB before gzip and 233 KB after gzip.

## Implemented but awaiting acceptance

- Usage, event history, energy estimates, alert reports, camera connectivity, and offline/recovery UI on
  a physical phone.
- Complete two-client regression testing after the `dev` branch integration.
- End-to-end device-state acceptance of safety cutoffs and light schedules in the local emulator. The
  one-shot helper and both empty-database scheduler invocations are verified.
- TalkBack semantics, 48 dp touch targets, large font scaling, rotation, and representative light/dark
  modes on hardware.

## Known limitations

- Production scheduled Functions are not deployed. Safety cutoffs and scheduled lights must be
  demonstrated with local emulators; no billing-dependent work is part of the current plan.
- Per-user alert read acknowledgement is represented in the schema and Rules but is not exposed in the
  Android UI or repository yet.
- Android observes at most 200 recent events per device. A busy device can therefore have an incomplete
  30-day report; the UI does not yet disclose truncation.
- Estimated energy uses assumed profile wattages and a fixed demonstration tariff rather than meter
  telemetry or a configured regional price.
- Pending commands are retained only in ViewModel memory. Firestore cache keeps visible data offline,
  but a queued command does not survive application-process death.
- Existing rooms are edited with forms rather than touch resize/move.
- Camera upload and the optional second-phone camera node remain unimplemented; current camera media is
  an HTTPS mock snapshot.
- CI, a signed release APK, complete offline demonstration, and release packaging remain pending.

## Branch state

`dev` is the current integration branch and contains all commits from `develop` plus the merged teammate
features. `develop` is an ancestor and should not receive independent changes. After this integration is
accepted, choose one canonical integration branch and update repository branch protection/defaults to
avoid future `dev`/`develop` divergence.
