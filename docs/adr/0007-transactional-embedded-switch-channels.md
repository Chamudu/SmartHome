# ADR 0007: Transactional Embedded Switch Channels

- Status: Accepted
- Date: 2026-07-25

## Context

A physical multi-switch is one device with two, three, or five small channel records. Firestore cannot
address an arbitrary array element by channel ID. Replacing an array from a stale client snapshot could
undo a different channel update.

## Decision

Keep the bounded channels embedded in the device document and update them through Firestore
transactions. Android re-reads the document, changes one channel's desired state and request ID, and
writes the complete array. The simulator re-reads, verifies the captured request ID is still current,
and changes only that channel's reported state.

Security Rules compare each before/after channel map. Owners/operators may change only
`desiredStatus` and `requestId`; simulators may change only `reportedStatus`. Stable identity/name and
the other actor's fields cannot be modified through these paths.

## Consequences

- One listener reads a complete physical unit and its small bounded channel set.
- Transaction retries preserve concurrent writes to different channels.
- A request ID prevents the simulator from acknowledging a superseded command.
- Rules must explicitly inspect the maximum five entries because Rules do not provide general loops.
- A much larger or dynamically growing channel set should use a channel subcollection instead.

## Alternatives considered

- Channel subcollection: natural independent writes, but more listeners/reads and fragmented unit state.
- Non-transactional array replacement: simpler but vulnerable to lost updates.
- One map keyed by channel ID: improves field addressing, but would require migration of existing devices
  and still needs careful dynamic-key validation.
