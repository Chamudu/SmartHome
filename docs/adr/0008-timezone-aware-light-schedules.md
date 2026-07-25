# ADR 0008: Evaluate Light Schedules in a Trusted Timezone-Aware Worker

- Status: Accepted
- Date: 2026-07-25

## Context

Light schedules must continue to work when the Android application is closed and must not depend on a
phone's clock, network availability, or current timezone. A configured period may also cross midnight.

## Decision

Store each light's enabled flag, local start/end `HH:mm` values, IANA timezone, and trusted last
evaluation timestamp in its profile configuration. A scheduled backend worker evaluates enabled lights
once per minute. It writes a normal desired-state command only when the target differs from current
intent, allowing the hardware simulator to acknowledge the request through the existing device-twin
flow.

Use a half-open interval `[start, end)`. A start greater than end means the interval crosses midnight.
Reject equal endpoints and malformed values in both the Android boundary and Firestore Security Rules.

## Consequences

- Scheduling is independent of mobile lifecycle and local clock manipulation.
- IANA zones preserve regional behavior, including daylight-saving rules where applicable.
- At most one minute of evaluator latency is expected.
- The worker requires scheduled backend infrastructure for production execution. Until that deployment
  is authorized, its decision logic and integration remain locally verifiable without affecting cloud
  billing.
