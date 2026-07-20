# ADR-0004: Home-Scoped Firestore Data

- Status: Accepted
- Date: 2026-07-20

## Context

Most product data belongs to a home and must be isolated from users without active membership. Security
Rules should be understandable and consistent across floors, devices, events, and alerts.

## Decision

Nest protected product collections beneath `homes/{homeId}` and store membership documents at
`homes/{homeId}/members/{userId}`. Use the authenticated user ID for direct membership checks.

## Consequences

- Rules share one clear authorization boundary.
- Removing access can be represented by one membership change.
- Cross-home queries are not the default and require collection-group queries or backend aggregation.
- Event and reporting queries must account for subcollection paths and indexes.
