# Testing Strategy and Evidence

## Test pyramid

1. Pure Kotlin unit tests cover fast geometry and state decisions.
2. ViewModel tests use fake repositories and coroutine virtual time.
3. Firestore emulator tests exercise the real rule language and authenticated roles.
4. Typecheck, lint, and production build protect the simulator.
5. Physical-device tests cover gestures, lifecycle, Firebase integration, and HCI behavior.

## Commands

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug

firebase emulators:exec --project demo-smart-home --only firestore \
  "npm --prefix firebase/tests test"

npm --prefix simulator run typecheck
npm --prefix simulator run lint
npm --prefix simulator run build

npm --prefix functions run check
```

## Current automated coverage

- Device status permits commands only for normal connected states.
- Sign-in begins observation and clears password state.
- Valid commands reach the repository; error-state commands do not.
- Outlet placement infers the containing room.
- Safety duration converts minutes at the UI boundary into persisted seconds.
- Device movement infers its destination room and the synchronized primary outlet is protected from
  deletion during migration.
- Floor names/dimensions/unique levels and room boundaries/overlap/shared edges.
- Active membership and outsider/unauthenticated denial.
- Owner desired writes versus simulator reported writes.
- Floor/room creation validation and role restrictions.
- Device placement within its floor and optional room.
- Valid multi-switch creation, invalid channel counts, unsafe camera URI denial, and simulator creation
  denial.
- Complete valid outlet, safety outlet, light, and camera seed shapes.
- Safety timer start/clear transitions, duration bounds, exact due-time boundary, and retry behavior.
- Active members can read trusted alerts while owner and simulator clients cannot forge them.
- A ViewModel repository stream exposes backend-created alerts to reactive Android UI state.
- A multi-switch ViewModel command addresses the requested device/channel only.
- A generic light/outlet command routes to the selected device rather than the legacy primary outlet.
- Owner channel requests cannot forge reported state; simulator reports cannot change desired state.
- A channel write preserves every unrelated channel, and transactional clients re-read before updating
  the embedded array.

## Manual acceptance focus

Automated tests do not prove gesture arbitration, visual contrast, haptic feedback, font scaling, or
real network timing. These require a physical phone plus the simulator and Firebase development project.
Record the tested build commit, device/Android version, steps, expected result, and observed result.
