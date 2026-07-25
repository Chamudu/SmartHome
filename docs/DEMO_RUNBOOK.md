# Product Demonstration Runbook

## Purpose

This runbook produces a repeatable end-to-end demonstration and prevents setup steps from obscuring
product behavior.

## Preparation

1. Use the intended Git commit and verify a clean working tree.
2. Run Android tests/build, Firestore emulator tests, and simulator checks from `TESTING.md`.
3. Confirm deployed rules match the tested source.
4. Connect the Android phone and install with `./gradlew :app:installDebug`.
5. Start the simulator with `npm --prefix simulator run dev` and sign in as the simulator identity.
6. Sign in on Android as the owner identity.
7. Run the authenticated seed tool when deterministic demo data is missing.
8. Confirm both clients have network access and the simulator lists all five profiles.

Never expose passwords, environment files, Authentication user IDs, or Firebase configuration files in
screenshots or recordings.

## Current stable walkthrough

1. Explain separate owner and simulator identities and least-privilege roles.
2. Turn the main outlet on in Android; show desired `PENDING` and simulator acknowledgement.
3. Report `ERROR` and `DISCONNECTED` in the simulator; show reactive Android status/control prevention.
4. Create/select floors and demonstrate unique level ordering.
5. Create adjacent rooms; show overlap and out-of-bounds rejection.
6. Rename/edit rooms and show protected deletion while a device is assigned.
7. Drag on empty grid space to prefill a room rectangle.
8. Long-press a cell to prefill Add Device; create a profile and show it appear reactively in both clients.
9. Attempt an invalid four-channel switch or unsafe camera URI to show validation.
10. Filter devices and report error/disconnection independently from the simulator.
11. Briefly show emulator-backed rule tests as evidence that direct APIs cannot bypass authorization.

## Planned final walkthrough additions

- Independent multi-switch channel control.
- Short safety-outlet duration followed by a backend cutoff, event, and alert.
- Timezone-aware light schedule execution.
- Camera snapshot, capture time, placeholder/error behavior, and optional camera phone upload.
- Activity history and usage totals.

## Recovery

- If ADB is empty, reconnect/unlock the phone, confirm USB debugging, and run `adb devices`.
- If permission is denied, verify the signed-in UID has the expected active membership role.
- If the simulator is stale, verify `.env.local`, sign out/in, and inspect the browser console.
- If demo data is inconsistent, use the trusted seed/reset workflow once implemented; do not weaken rules.
