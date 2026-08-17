# ADR-0003: Desired and Reported Device State

- Status: Accepted
- Date: 2026-07-20

## Context

A remote control request does not prove that physical or simulated hardware applied the requested
change. Network loss, hardware errors, concurrent commands, and rejected operations can make a single
state field misleading.

## Decision

Represent each controllable device with separate desired and reported state. Correlate requests and
reports using a unique request ID, and render confirmed status from reported state.

## Consequences

- The UI can distinguish pending commands from confirmed hardware state.
- Simulator and Android behavior follows a realistic IoT control pattern.
- Documents and UI state are more complex than a single Boolean toggle.
- Timeouts, rejection, retry, and request ordering must be handled explicitly.
