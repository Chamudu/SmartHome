# Product Requirements

## Purpose

This document defines testable behavior for the Smart Home MVP. Requirement identifiers remain stable
so implementation changes, tests, and release evidence can refer to them.

Priority meanings:

- **P0** — required for the first end-to-end vertical slice
- **P1** — required for the MVP
- **P2** — planned only after the MVP is stable

## Actors

- **Home user** — creates floors, monitors equipment, issues commands, and reviews alerts and usage.
- **Simulated device** — receives commands and reports state, connectivity, and errors.
- **Automation service** — enforces safety limits, executes schedules, and records outcomes.

## Functional requirements

### Authentication and authorization

#### AUTH-01 — Sign in (P1)

An existing user shall be able to sign in and restore an authenticated session.

Acceptance criteria:

- Given valid credentials, when the user signs in, then the home dashboard is displayed.
- Given invalid credentials, when sign-in is attempted, then a useful error is shown without revealing
  whether a specific account exists.
- Given an expired or revoked session, when protected data is requested, then the user returns to the
  authentication flow.

#### AUTH-02 — Home isolation (P1)

A user shall only read or modify homes for which they have an active membership.

Acceptance criteria:

- Given membership in Home A, when the user reads Home A, then authorized data is returned.
- Given no membership in Home B, when the same user attempts to read or write Home B by document ID,
  then Firebase Security Rules deny the request.
- Authorization is verified with automated Firebase Emulator Suite tests.

### Floor and room management

#### FLOOR-01 — Manage floors (P1)

A user shall be able to create, rename, select, and remove floors within an authorized home.

Acceptance criteria:

- A floor requires a non-blank name and grid dimensions within configured limits.
- A floor level is unique within a home and determines vertical ordering.
- Newly created floors appear without a manual refresh.
- Removing a floor requires confirmation and cannot leave orphaned devices.
- The UI clearly distinguishes loading, empty, and failure states.

#### FLOOR-02 — Create rectangular rooms (P1)

A user shall be able to build a simple floor layout from axis-aligned rectangular rooms on a grid.

Acceptance criteria:

- A room has a name, origin coordinate, width, and height expressed in grid cells.
- A room cannot extend beyond the floor boundary.
- A room cannot overlap another room on the same floor.
- Editing a room cannot leave an assigned device outside its new boundary.
- Removing a room requires confirmation and cannot leave orphaned device references.
- Valid layout edits persist and appear on other subscribed clients without refresh.

#### FLOOR-03 — Place devices (P1)

A user shall be able to place a device at a valid grid coordinate on a floor.

Acceptance criteria:

- A device marker cannot be placed outside the floor grid.
- When a marker is inside a room, the stored placement references that room.
- The marker is rendered at the same logical coordinate after screen resize or orientation change.
- Selecting a marker opens the device summary or controls.

### Common device behavior

#### DEVICE-01 — Display operational status (P0)

Every device shall expose a current status of `ON`, `OFF`, `ERROR`, or `DISCONNECTED`.

Acceptance criteria:

- Status is represented by text as well as color or iconography.
- A status update in Firestore appears in subscribed clients without manual refresh.
- `ERROR` and `DISCONNECTED` cannot be mistaken for a normal `OFF` state.
- The last confirmed update time is available to the user.

#### DEVICE-02 — Issue commands safely (P0)

An authorized user shall be able to request a supported device state change.

Acceptance criteria:

- The control shows progress while a command is pending.
- Repeated taps do not produce an invalid or ambiguous final state.
- A rejected or timed-out command restores a consistent UI and explains the failure.
- Unauthorized writes are rejected by Security Rules.

#### DEVICE-03 — Bidirectional synchronization (P0)

The Android client and simulator shall observe device changes through real-time listeners.

Acceptance criteria:

- A valid Android command appears in the simulator without refresh.
- A simulated physical-state update appears in Android without refresh.
- Backend-driven changes appear in both clients without refresh.
- Each state includes a server-generated update timestamp and origin metadata for diagnosis.

### Device profiles

#### OUTLET-01 — Single-channel outlet (P0)

A user shall be able to turn a connected outlet on or off.

Acceptance criteria:

- Only `ON` and `OFF` may be requested as normal power states.
- An outlet in `ERROR` or `DISCONNECTED` rejects normal control until its operational condition permits
  control again.
- Successful changes generate device events.

#### SWITCH-01 — Multi-switch unit (P1)

A user shall be able to control each channel of a variable-channel switch unit independently.

Acceptance criteria:

- The supported MVP configurations include two, three, and five channels.
- Each channel has a stable identifier, display name, and state.
- Updating one channel does not change other channels.
- Unit-level connectivity applies consistently to its channels.

#### SAFETY-01 — Maximum active duration (P1)

An authorized user shall be able to configure a maximum active duration for a safety-critical device.

Acceptance criteria:

- The duration must be within documented safe configuration bounds.
- The active interval is calculated from trusted server timestamps.
- Client clock changes cannot bypass the cutoff.

#### SAFETY-02 — Server-enforced cutoff (P1)

The automation service shall turn off a safety-critical device that exceeds its active-duration limit.

Acceptance criteria:

- The final state becomes `OFF` even when no client is open.
- The operation is idempotent when the worker or event is retried.
- A safety event records the device, configured limit, activation time, cutoff time, and reason.
- An in-app alert is created and appears without manual refresh.

#### LIGHT-01 — Scheduled light operation (P1)

A user shall be able to configure an automatic on/off period for a light.

Acceptance criteria:

- A schedule stores an explicit home timezone, start time, end time, and enabled state.
- The backend applies the intended state when a boundary is reached.
- Disabling a schedule prevents future automatic changes from that schedule.
- Execution records success or a diagnosable failure without silently losing the schedule.

#### CAMERA-01 — Mock camera monitoring (P1)

A user shall be able to view a camera's latest mock snapshot or approved mock stream URI.

Acceptance criteria:

- The UI shows captured/updated time and camera connectivity.
- Missing or failed media produces a safe placeholder and error state.
- Untrusted URI schemes are not opened.

### Alerts and reporting

#### ALERT-01 — In-app alert history (P1)

A user shall be able to review safety and operational alerts for an authorized home.

Acceptance criteria:

- Alerts include severity, message, device reference, creation time, and read state.
- New alerts appear without refresh.
- A user can mark an alert as read without changing the underlying device event.

#### REPORT-01 — Device usage summary (P1)

A user shall be able to review important-device activity for a selected period.

Acceptance criteria:

- The report shows activation count and accumulated active duration.
- Duration is derived from ordered state-transition events using server timestamps.
- Missing pairs or incomplete active intervals are handled explicitly rather than producing negative or
  misleading totals.
- The selected period and timezone are visible.

### Hardware simulator

#### SIM-01 — Reflect cloud state (P0)

The simulator shall display devices and react to subscribed Firestore state changes.

Acceptance criteria:

- A command issued elsewhere updates the corresponding simulator control without refresh.
- The simulator displays device identity, profile, status, and connectivity.

#### SIM-02 — Simulate hardware reports (P1)

The simulator shall be able to report power changes, errors, disconnections, and reconnections.

Acceptance criteria:

- Simulated reports use the same data contract as normal device reports.
- The Android application reflects each report without refresh.
- The simulator cannot write data outside the selected authorized home.

## Cross-cutting requirements

### NFR-01 — Security

- Deny access by default in Firestore and Storage rules.
- Validate ownership or membership and allowed field transitions on every client-accessible write.
- Keep credentials, signing material, and environment-specific configuration out of source control.
- Treat mobile and browser clients as untrusted.

### NFR-02 — Reliability

- Backend operations must tolerate retries without duplicating safety effects or corrupting history.
- Time-dependent decisions must use trusted timestamps and explicit timezones.
- The UI must recover from listener errors and temporary connectivity loss.

### NFR-03 — Usability and accessibility

- Controls must provide immediate, visible feedback.
- Status must not depend on color alone.
- Interactive targets and text must remain usable at supported Android font and display scales.
- Destructive actions require confirmation where accidental activation is plausible.

### NFR-04 — Testability

- Domain calculations are isolated from UI and Firebase SDK code where practical.
- Security Rules and Cloud Functions have emulator-backed tests.
- Seed data provides a deterministic end-to-end scenario with two floors and every MVP device profile.

## Traceability convention

Tests and future implementation notes should include the relevant identifier, for example
`SAFETY-02`, in the test name, issue, or change description. This makes it possible to prove which
behavior is implemented without coupling requirements to a specific UI design.
