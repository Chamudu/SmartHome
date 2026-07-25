# Domain Model and Firestore Schema

## Design goals

The data model supports real-time control, clear authorization boundaries, backend safety enforcement,
and usage reporting while remaining small enough to evolve quickly.

The central rule is that a requested state and a confirmed device state are different facts. A client
may request `ON`, but only the simulated or physical device reports whether that command succeeded.

## Domain relationships

```text
User ──< HomeMembership >── Home
                              │
                              ├──< Floor ──< Room
                              ├──< Device ──< DeviceEvent
                              └──< Alert

Device
  ├── desired state
  ├── reported state
  ├── placement
  └── profile-specific configuration
```

## Firestore paths

```text
users/{userId}

homes/{homeId}
homes/{homeId}/members/{userId}
homes/{homeId}/floors/{floorId}
homes/{homeId}/floors/{floorId}/rooms/{roomId}
homes/{homeId}/devices/{deviceId}
homes/{homeId}/devices/{deviceId}/events/{eventId}
homes/{homeId}/alerts/{alertId}
```

Keeping protected data under a home path gives Security Rules one consistent authorization boundary.
References use document IDs instead of duplicating full Firestore paths in client-facing fields.

## Enumerations

### Device profile

```text
OUTLET | MULTI_SWITCH | SAFETY_OUTLET | LIGHT | CAMERA
```

### Reported status

```text
ON | OFF | ERROR | DISCONNECTED
```

### Command state

```text
IDLE | PENDING | APPLIED | REJECTED | TIMED_OUT
```

### Event origin

```text
ANDROID | SIMULATOR | AUTOMATION | SYSTEM
```

### Membership role

```text
OWNER | OPERATOR | VIEWER | SIMULATOR
```

The MVP seeds one owner and one simulator identity. Roles preserve a path to least-privilege writes even
when the browser simulator is used instead of physical hardware.

## Documents

### `users/{userId}`

| Field | Type | Notes |
| --- | --- | --- |
| `displayName` | string | User-facing name |
| `email` | string | Normalized account email; not used alone for authorization |
| `createdAt` | timestamp | Server timestamp |
| `updatedAt` | timestamp | Server timestamp |

Authentication credentials remain in Firebase Authentication and are never stored in this document.

### `homes/{homeId}`

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | Non-blank display name |
| `timezone` | string | IANA name such as `Asia/Colombo` |
| `createdBy` | string | User ID |
| `createdAt` | timestamp | Server timestamp |
| `updatedAt` | timestamp | Server timestamp |

### `homes/{homeId}/members/{userId}`

| Field | Type | Notes |
| --- | --- | --- |
| `role` | enum string | `OWNER`, `OPERATOR`, `VIEWER`, or `SIMULATOR` |
| `active` | boolean | Revocation without deleting audit context |
| `createdAt` | timestamp | Server timestamp |

The document ID equals the Firebase Authentication user ID, allowing direct membership lookup in
Security Rules.

### `homes/{homeId}/floors/{floorId}`

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | Unique naming is optional; non-blank is required |
| `level` | number | Unique home elevation/order such as `0`, `1`, or `-1` |
| `gridColumns` | number | Proposed range: 4–40 |
| `gridRows` | number | Proposed range: 4–40 |
| `createdAt` | timestamp | Server timestamp |
| `updatedAt` | timestamp | Server timestamp |

### `homes/{homeId}/floors/{floorId}/rooms/{roomId}`

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | Non-blank room name |
| `column` | number | Zero-based left coordinate |
| `row` | number | Zero-based top coordinate |
| `width` | number | Positive grid-cell width |
| `height` | number | Positive grid-cell height |
| `createdAt` | timestamp | Server timestamp |
| `updatedAt` | timestamp | Server timestamp |

Boundary and overlap checks run in domain logic for immediate feedback. A trusted backend transaction
must revalidate layout mutations when strict concurrent-edit protection is required.

Room assignment is optional for a device, but when `roomId` is present, Security Rules require the room
to exist under the selected floor and require the device coordinate to lie inside that room.

For the simple-house model, each floor uses a unique level: `-1` represents a basement, `0` the ground
floor, and positive values represent floors above ground. Separate wings at one elevation should be
represented within one floor plan rather than as duplicate levels.

### `homes/{homeId}/devices/{deviceId}`

Common fields:

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | Non-blank device name |
| `profile` | enum string | Immutable after creation in the MVP |
| `floorId` | string | Existing floor ID |
| `roomId` | string or null | Existing room ID when assigned |
| `position.column` | number | Valid grid column |
| `position.row` | number | Valid grid row |
| `desired.status` | `ON` or `OFF` | Latest requested power state |
| `desired.requestId` | string | Unique ID used for correlation/idempotency |
| `desired.requestedBy` | string | Auth user ID or `AUTOMATION` |
| `desired.requestedAt` | timestamp | Server timestamp |
| `reported.status` | status enum | Latest confirmed operational status |
| `reported.requestId` | string or null | Applied request correlation |
| `reported.updatedAt` | timestamp | Server timestamp |
| `reported.errorCode` | string or null | Stable diagnostic code, not sensitive details |
| `commandState` | command enum | Derived/managed command progress |
| `config` | map | Profile-specific validated configuration |
| `createdAt` | timestamp | Server timestamp |
| `updatedAt` | timestamp | Server timestamp |

The displayed device status comes from `reported.status`, never directly from `desired.status`.

Profile-specific `config` shapes:

```text
OUTLET
  config: {}

MULTI_SWITCH
  config.channels: [
    { id, name, desiredStatus, reportedStatus, requestId, updatedAt }
  ]

SAFETY_OUTLET
  config.maxOnDurationSeconds
  config.activatedAt
  config.cutoffDueAt

LIGHT
  config.schedule.enabled
  config.schedule.startLocalTime     // HH:mm
  config.schedule.endLocalTime       // HH:mm
  config.schedule.timezone           // IANA name
  config.schedule.lastEvaluatedAt

CAMERA
  config.mediaType                   // SNAPSHOT or MOCK_STREAM
  config.mediaUri
  config.capturedAt
```

Light schedules use half-open intervals: the start is inclusive and the end is exclusive. If start is
earlier than end, the active range is within one local day. If start is later, the range crosses
midnight. Equal start/end values are rejected. The IANA timezone makes the rule independent of the
phone's clock and timezone; the trusted evaluator records its last evaluation with a server timestamp.

Android represents these alternatives as an exhaustive sealed configuration hierarchy selected by the
required `profile` discriminator. New client-created devices begin with desired and reported status
`OFF`, command state `IDLE`, null request correlation, and server-generated timestamps. Creation is one
document write, so listeners never observe a partially initialized profile.

The device marker coordinate is unique by application validation for the current editor. Firestore
Rules validate floor/room containment and the complete profile shape; strict coordinate uniqueness
across simultaneous creators would require a deterministic coordinate index or trusted transaction.

Multi-switch channel state is embedded because channel count is small and the unit is normally read as
one entity. `desiredStatus` is owner/operator intent, `reportedStatus` is simulator-confirmed truth, and
`requestId` identifies the latest desired channel command. Channel array updates use transactions to
prevent one concurrent channel update from overwriting another. Rules use per-map `diff` checks: an
owner may change only desired state plus request ID, while a simulator may change only reported state.

### `homes/{homeId}/devices/{deviceId}/events/{eventId}`

| Field | Type | Notes |
| --- | --- | --- |
| `type` | string | Example: `STATE_REPORTED`, `SAFETY_CUTOFF` |
| `fromStatus` | status or null | Previous confirmed status |
| `toStatus` | status or null | New confirmed status |
| `origin` | origin enum | Source category |
| `actorId` | string or null | Auth user ID where applicable |
| `requestId` | string or null | Correlates command and report |
| `reason` | string or null | Stable machine-readable reason |
| `occurredAt` | timestamp | Trusted event time |
| `metadata` | map | Small event-specific values |

Events are append-only from trusted paths. Clients read them for activity and reporting but do not edit
historical events.

### `homes/{homeId}/alerts/{alertId}`

| Field | Type | Notes |
| --- | --- | --- |
| `deviceId` | string or null | Related device |
| `eventId` | string or null | Related source event |
| `severity` | string | `INFO`, `WARNING`, or `CRITICAL` |
| `type` | string | Stable alert category |
| `message` | string | User-facing summary |
| `createdAt` | timestamp | Server timestamp |
| `readBy` | map | User ID to read timestamp |

An alert is shared home data, so read state is stored per user rather than as one global Boolean.

## State synchronization

```text
1. Android creates requestId and updates desired state.
2. Simulator listener receives the desired state.
3. Simulator applies or rejects the command.
4. Simulator updates reported state with the same requestId.
5. Trusted event processing appends a device event.
6. Android listener renders the reported state and command outcome.
```

Backend automation follows the same desired/reported contract. For the simulator-only MVP, the cutoff
function atomically writes both desired and reported `OFF` so safety behavior remains demonstrable
without a permanently connected simulator. ADR 0006 records this deliberate simulation boundary.

## Security ownership

| Actor | Allowed responsibility |
| --- | --- |
| Owner/operator | Manage layouts and configurations; request device states |
| Viewer | Read home state only |
| Simulator | Read desired state; write narrowly validated reported state |
| Cloud Functions | Enforce cutoffs/schedules, append events, create alerts |

Clients may not write server-controlled timestamps, historical events, cutoff outcomes, or another
actor's membership role.

## Initial indexes

Indexes are added only for implemented queries. The cutoff query uses a collection-group composite
index over `profile`, `reported.status`, and `config.cutoffDueAt`, all ascending. Additional expected
indexes include:

- Devices by `floorId`, then `name`
- Alerts by `createdAt` descending
- Device events by `occurredAt` descending
- Collection-group device events by `type` and `occurredAt` for reporting if selected

## Deletion policy

Deletion is explicit and confirmed. A floor with assigned devices cannot be deleted; its rooms and floor
document are otherwise removed together in one client write batch. A room with an assigned device cannot
be deleted until that device is moved. The client performs these reference checks for the current MVP.
A trusted backend transaction or soft-deletion workflow should own cascades once multiple simultaneous
layout editors or operational event history are supported.
