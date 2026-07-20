# Product Specification

## Product vision

Smart Home provides a responsive and understandable way to model a home, monitor connected devices,
control them remotely, and apply trusted automation that reduces safety risks.

The platform has three cooperating parts:

1. An Android application in which a user views floors and controls devices.
2. A cloud backend/database that acts as the shared source of truth and enforces safety rules.
3. A web hardware simulator that behaves like physical IoT equipment.

When any participant changes a device—the app, simulator, or backend—the others should observe
the new state automatically. This is the key idea behind bidirectional real-time synchronization.

## Product scope

### Floors and device placement

- Support multiple floor plans.
- Show an abstract grid over a sample floor-plan image.
- Place devices at grid coordinates so their locations can be rendered consistently.
- Allow switching between floors and managing floor/device data.

### Device model

Every device has common identity, location, connectivity, and operational status. Profiles then
add specialized behavior:

- Outlet: one binary on/off channel.
- Multi-switch unit: one physical unit containing a variable number of independent channels.
- Safety outlet/appliance: an on/off channel plus maximum allowed active duration.
- Scheduled light: on/off behavior plus one or more configured time windows.
- Camera: availability/status plus a mock snapshot or stream URI.

The UI must distinguish `ON`, `OFF`, `ERROR`, and `DISCONNECTED`. Connectivity and power are
related but conceptually different: an outlet may have a last known power state while the device
itself is disconnected. The exact data representation will be decided in the schema design.

### Automation and reporting

- A backend worker observes safety-critical devices.
- If a device exceeds its configured maximum active duration, the backend writes `OFF` and creates
  an alert/notification.
- Scheduled lights are switched according to their saved schedules.
- Important state transitions are recorded as events so the app can report usage duration,
  activations, cutoffs, and trends.

### Simulator

The browser dashboard represents physical hardware. It listens for cloud commands, changes its
visual controls, and can send simulated status changes or failures back to the cloud. This proves
that synchronization works independently of the Android UI.

## Core quality requirements

- Fast listener-driven updates without manual refresh.
- Authentication and per-home authorization.
- Server-side validation and safety enforcement; the mobile client must not be trusted alone.
- Clear loading, offline, error, and disconnected states.
- Traceable event history for reporting and debugging.
- Testable business logic and a repeatable demonstration setup.

## Minimum viable product

The first defensible release should include:

- One demo home with two floors and sample plan images.
- At least one instance of every required device profile.
- Reactive control from both Android and simulator.
- One safety cutoff producing an in-app alert.
- One automatic light schedule.
- Camera mock snapshot display.
- A small usage report based on event history.

Advanced features such as custom floor-plan uploads, live video, energy estimation, invitations,
and elaborate analytics should wait until the minimum release is stable.
