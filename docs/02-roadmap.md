# Delivery Roadmap

Documentation begins with the product, architecture, and data contracts, then evolves with verified
system behavior.

## Phase 0 — Scope and foundations

- Resolve the pending decisions listed in `04-open-decisions.md`.
- Initialize Git and agree on branch/PR conventions.
- Write user stories and measurable acceptance criteria.
- Draw low-fidelity mobile and simulator wireframes.
- Define the domain model and first Firestore schema.
- Create Architecture Decision Records for major choices.

Exit condition: every MVP capability maps to a planned screen, data structure, backend behavior,
and acceptance test.

## Phase 1 — Walking skeleton

- Scaffold Android, web simulator, Firebase, and local emulators.
- Connect both clients to an emulator project.
- Implement one demo outlet and show real-time state changes in both directions.
- Add the first security rules and automated integration test.

Exit condition: toggling one outlet in either client updates the other client without refresh.

## Phase 2 — Floors and device profiles

- Implement navigation and floor selection.
- Implement direct grid manipulation for room selection/creation and long-press device placement.
- Retain explicit editor actions as accessible gesture alternatives.
- Apply the Material 3 theme, navigation hierarchy, and semantic device/status presentation.
- Add common status UI and all heterogeneous device profiles.
- Add authorized device creation and profile-specific configuration.
- Add loading, error, disconnected, and offline behavior.

Exit condition: two floors and every required device type can be demonstrated reliably.

## Phase 3 — Automation and alerts

- Implement safety duration configuration and backend cutoff.
- Implement scheduled lighting with server timestamps and a declared timezone policy.
- Record state transition events.
- Deliver in-app alerts and then FCM push notifications.

Exit condition: an intentionally short demo timer proves a server-driven cutoff and alert.

## Phase 4 — Reporting and camera mock

- Show camera snapshots or safe mock stream links.
- Aggregate event history into useful usage statistics.
- Add filters by device/date where time permits.

Exit condition: reporting answers at least one concrete question, such as total on-time per safety
device in the selected period.

## Phase 5 — Hardening and release

- Test authorization, concurrency, offline recovery, and error paths.
- Run a complete demo rehearsal using seeded data.
- Finish operational documentation with screenshots and diagrams.
- Produce a signed release APK and publish it through an appropriate GitHub release.
- Produce a repeatable demo runbook and release notes.

## Recommended first implementation exercise

Build only a single outlet through the entire stack. This vertical slice teaches Compose state,
Firestore listeners, security rules, simulator subscriptions, and testing before complexity is added.
