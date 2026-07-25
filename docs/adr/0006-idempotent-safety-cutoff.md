# ADR 0006: Idempotent Scheduled Safety Cutoff

- Status: Accepted
- Date: 2026-07-25

## Context

A safety outlet must be switched off even when Android is closed and the browser simulator is absent.
Firestore triggers and scheduled functions may retry, overlap, or observe stale candidates. Client clocks
and background execution are not trusted enough to enforce a property-protection rule.

## Decision

Use a second-generation Firestore update function to record trusted `activatedAt` and `cutoffDueAt`
timestamps when a safety outlet first reports `ON`. Run a scheduled function every minute to query due
safety outlets. For each candidate, re-read and revalidate the device inside a Firestore transaction.

The transaction writes desired and reported `OFF`, clears the timer, and creates a cutoff event and home
alert. Their IDs derive from device ID and cutoff deadline, making repeated delivery converge on the same
documents. Direct client writes to events and alert creation remain denied.

The backend reports `OFF` directly because the current hardware is a simulator that may be disconnected.
A physical hardware version should write desired `OFF` first and separately confirm reported state, with
an escalation path if acknowledgement times out.

## Consequences

- Enforcement continues independently of client lifecycle and client clocks.
- Transactions prevent a stale scheduled scan from overriding a device that is no longer due.
- Deterministic IDs make retries idempotent and prevent duplicate alerts.
- The one-minute schedule creates up to approximately one minute of enforcement latency.
- Deployment requires a billing-enabled Firebase project and a composite collection-group index.

## Alternatives considered

- Android timer or background worker: unavailable when the device is offline and not a trusted boundary.
- One delayed task per activation: more precise, but adds task lifecycle and cancellation infrastructure.
- Trigger-only duration check: no event occurs at the future deadline, so it cannot enforce by itself.
