# Product Scope

## Objective

Smart Home enables users to model a multi-floor home, monitor connected equipment, control devices in
real time, and rely on cloud-enforced automation for safety-critical behavior.

## MVP scope

### Identity and home access

- Sign in with a supported Firebase Authentication method.
- Access only homes and devices authorized for the signed-in user.
- Seed a reliable demonstration account and home without embedding credentials in source control.

### Floor management

- Create, rename, view, and remove floors.
- Configure a simple grid for each floor.
- Create rectangular rooms by specifying their grid position and dimensions.
- Edit room names and placement while preventing invalid or overlapping layouts.
- Place devices at valid positions within a floor layout.

Complex architectural drawing, freehand walls, CAD import, and automatic floor-plan recognition are
outside the MVP.

### Device monitoring and control

- Display device name, profile, floor/room, last update time, and current status.
- Support `ON`, `OFF`, `ERROR`, and `DISCONNECTED` operational states.
- Control a single-channel electrical outlet.
- Control each channel of variable two-, three-, and five-channel switch units independently.
- Configure and control safety-critical outlets with a maximum active duration.
- Configure scheduled on/off periods for lights.
- Display a mock camera snapshot or safe mock stream.
- Propagate state changes between Android, Firestore, and the simulator without manual refresh.

### Safety automation and alerts

- Enforce maximum active duration from trusted backend code.
- Switch an overdue safety device off and record the reason.
- Store alerts for display inside the Android application.
- Use server timestamps and an explicit home timezone for time-based behavior.

### Reporting

- Record important device state transitions as events.
- Show recent activity for important devices.
- Report activation count and accumulated active duration for a selected period.

### Hardware simulator

- Display the configured floors and devices in a browser dashboard.
- Subscribe directly to real-time device changes.
- Reflect control commands visually.
- Simulate physical state changes, errors, disconnections, and reconnections.

## Post-MVP scope

- Android system push notifications through Firebase Cloud Messaging
- Fully offline demonstration using the Firebase Emulator Suite
- Multiple homes, invitations, and role management
- Custom image upload behind the grid floor editor
- Rich analytics and energy/cost estimation
- Live camera streaming
- Advanced recurring schedules and conflict resolution
- iOS or cross-platform mobile clients

## Non-functional requirements

- Fast listener-driven updates under normal network conditions
- Least-privilege authorization enforced by Firebase Security Rules
- Consistent device contracts across every system component
- Clear loading, empty, offline, error, and disconnected experiences
- Idempotent backend automation that tolerates repeated events
- Automated tests for domain rules, authorization, synchronization, and time calculations
- No secrets or personal data committed to source control

## MVP acceptance boundary

The MVP is complete when a seeded home with at least two floors can demonstrate every supported device
profile, bidirectional real-time control, a backend-triggered safety cutoff, a scheduled light change,
a camera mock, and a usage summary in a repeatable end-to-end scenario.

