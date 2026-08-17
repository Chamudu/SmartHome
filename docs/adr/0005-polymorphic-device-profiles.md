# ADR 0005: Polymorphic Device Profiles

- Status: Accepted
- Date: 2026-07-25

## Context

Every device has common identity, placement, twin state, and connectivity data, but switch units, safety
outlets, lights, and cameras require different validated configuration. A flat document with every field
nullable would permit nonsensical combinations and spread profile checks throughout each client.

## Decision

Use one home-scoped `devices` collection with a required profile discriminator and a profile-specific
`config` map. Android maps the discriminator into an exhaustive sealed configuration hierarchy. The
simulator and backend use the same documented Firestore shapes.

Device creation initializes common twin state to confirmed `OFF`/`IDLE`, associates one logical grid
coordinate, and creates the complete configuration required for its profile. Firestore Security Rules
validate allowed document keys, placement, initial state, profile discriminator, and configuration.

## Consequences

- One real-time collection listener can render and filter every device.
- Adding a profile requires deliberate mapper, UI, rules, simulator, and test changes.
- Profile code can avoid unrelated nullable fields and use exhaustive branching.
- Firestore remains schemaless at the database level, so Security Rules and emulator tests act as the
  externally enforced schema for client writes.
- Profile-specific commands still require narrowly validated update paths and may not reuse the common
  creation validation directly.

## Alternatives considered

- Separate collection per profile: simpler individual schemas but fragmented listeners, queries, and
  placement operations.
- One flat document with nullable profile fields: simple serialization but weak invalid-state control.
- A backend-only creation API: strongest centralized invariant enforcement, but deferred until trusted
  backend automation is introduced; Security Rules provide the current boundary.
