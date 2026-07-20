# Delivery Backlog

Target: August 3, 2026

This backlog tracks product work. Detailed acceptance criteria belong in feature specifications and
completed items should remain checked for release traceability.

## P0 — Foundation and vertical slice

- [ ] Confirm installed JDK, Android SDK, Node.js, npm, Firebase CLI, and emulator capabilities.
- [ ] Create requirements and acceptance-criteria document.
- [ ] Create initial Android and simulator wireframes.
- [ ] Define the domain model and Firestore document schema.
- [ ] Write the first architecture decision records.
- [ ] Scaffold the Kotlin/Compose Android application.
- [ ] Scaffold the React/TypeScript simulator.
- [ ] Configure Firebase projects and the local Emulator Suite.
- [ ] Implement authentication and initial Firestore Security Rules.
- [ ] Complete one outlet vertical slice with bidirectional real-time synchronization.
- [ ] Add automated tests for the outlet state transition and authorization rules.

## P1 — Core product

- [ ] Implement floor creation, renaming, selection, and removal.
- [ ] Implement grid configuration and rectangular room editing.
- [ ] Validate room boundaries and prevent overlaps.
- [ ] Implement device placement and status indicators.
- [ ] Implement variable-channel switch units.
- [ ] Implement safety-critical device configuration.
- [ ] Implement the trusted maximum-duration cutoff.
- [ ] Implement scheduled light operation with timezone-aware server timestamps.
- [ ] Implement persistent in-app alerts.
- [ ] Implement mock camera snapshots or stream links.
- [ ] Implement event history and basic usage reporting.
- [ ] Support error, disconnected, loading, empty, and offline UI states.

## P1 — Simulator and integration

- [ ] Display all seeded devices in the hardware simulator.
- [ ] Reflect cloud commands without refresh.
- [ ] Simulate state changes, errors, disconnects, and reconnects.
- [ ] Create deterministic seed data with two floors and every device profile.
- [ ] Add end-to-end tests for Android/backend/simulator data contracts where practical.

## P1 — Release readiness

- [ ] Review Firestore Security Rules with emulator tests.
- [ ] Test concurrent changes and repeated backend events.
- [ ] Test scheduling and safety behavior across timezone boundaries.
- [ ] Add CI checks for Android, backend, rules, and simulator.
- [ ] Write setup, deployment, and demo runbooks.
- [ ] Capture product screenshots and architecture diagrams.
- [ ] Build and verify a signed release APK.
- [ ] Tag the release and publish versioned release notes.

## P2 — Time permitting

- [ ] Add Firebase Cloud Messaging push notifications.
- [ ] Make the complete demonstration runnable offline with Firebase emulators.
- [ ] Add floor-plan background image upload.
- [ ] Add energy and estimated-cost reporting.

