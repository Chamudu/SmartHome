# Delivery Backlog

Target: August 3, 2026

This backlog tracks product work. Detailed acceptance criteria belong in feature specifications and
completed items should remain checked for release traceability.

## P0 — Foundation and vertical slice

- [x] Audit the installed development toolchain and emulator capabilities.
- [x] Configure terminal access to the bundled JDK and Android SDK tools.
- [ ] Create an Android Virtual Device and verify it from the host session.
- [x] Install and verify the Android foundation on a physical device.
- [x] Install and authenticate the Firebase CLI.
- [x] Create requirements and acceptance-criteria document.
- [x] Create initial Android and simulator wireframes.
- [x] Define the domain model and Firestore document schema.
- [x] Write the first architecture decision records.
- [x] Scaffold the Kotlin/Compose Android application.
- [x] Scaffold the React/TypeScript simulator.
- [x] Configure Firebase projects and the local Emulator Suite.
- [x] Implement authentication and initial Firestore Security Rules.
- [x] Complete one outlet vertical slice with bidirectional real-time synchronization.
- [x] Add automated tests for the outlet state transition and authorization rules.

## P1 — Core product

- [x] Implement floor creation, renaming, selection, and removal.
- [x] Implement grid configuration and rectangular room editing.
- [x] Validate room boundaries and prevent overlaps.
- [x] Implement device placement and status indicators for the outlet vertical slice.
- [ ] Add a touch-first floor editor with drag selection and visible accessible alternatives.
- [x] Add in-app creation and initial configuration for every supported device profile.
- [x] Apply a cohesive Material 3 theme and semantic status styling.
- [x] Separate daily device control from floor editing with task-focused dashboard tabs.
- [ ] Complete physical verification of 48 dp targets, screen-reader semantics, font scaling, and themes.
- [x] Implement independent control and simulator reporting for variable-channel switch units.
- [x] Implement initial safety-critical device duration configuration.
- [x] Implement and locally verify the trusted maximum-duration cutoff.
- [x] Implement scheduled light configuration and a timezone-aware backend evaluator.
- [x] Implement persistent realtime safety-alert display.
- [x] Add realtime operational and safety alert history.
- [ ] Add per-user alert read acknowledgement to the Android UI.
- [x] Implement HTTPS mock camera snapshots with loading and failure states.
- [x] Implement event history and basic usage reporting.
- [x] Support error, disconnected, loading, empty, and offline UI states.

## P1 — Simulator and integration

- [x] Display all seeded devices in the hardware simulator.
- [x] Reflect cloud commands without refresh.
- [x] Simulate state changes, errors, disconnects, and reconnects.
- [x] Create a deterministic, idempotent seed tool with two floors and every device profile.
- [ ] Add end-to-end tests for Android/backend/simulator data contracts where practical.
- [ ] Accept safety and schedule Functions through the local emulator without enabling billing.

## P1 — Release readiness

- [x] Review Firestore Security Rules with emulator tests.
- [ ] Test concurrent changes and repeated backend events.
- [x] Test daytime, overnight, boundary, malformed-time, and invalid-timezone schedule decisions.
- [ ] Add CI checks for Android, backend, rules, and simulator.
- [ ] Write setup, deployment, and demo runbooks.
- [ ] Capture product screenshots and architecture diagrams.
- [ ] Build and verify a signed release APK.
- [ ] Tag the release and publish versioned release notes.

## P2 — Time permitting

- [ ] Add Firebase Cloud Messaging push notifications.
- [ ] Add an optional Android camera-node mode that captures snapshots on a second phone and uploads
  them to Firebase Storage with Firestore metadata.
- [ ] Make the complete demonstration runnable offline with Firebase emulators.
- [ ] Add floor-plan background image upload.
- [x] Add energy and estimated-cost reporting.
