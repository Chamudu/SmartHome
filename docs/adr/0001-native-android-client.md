# ADR-0001: Native Android Client

- Status: Accepted
- Date: 2026-07-20

## Context

The product requires one Android client with reactive device control, notifications, lifecycle-aware
state handling, and access to native platform tooling.

## Decision

Build the mobile client with Kotlin, Jetpack Compose, Material 3, coroutines, Flow, and a ViewModel-based
presentation architecture.

## Consequences

- Android platform APIs and lifecycle behavior are available without a cross-platform bridge.
- Compose enables state-driven UI and reusable previews for disconnected and error states.
- The product does not gain an iOS client from the same UI codebase.
- Business and data logic must remain separated from composables to support testing and maintenance.
