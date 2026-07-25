# Documentation Index

This directory contains the product and engineering documentation for Smart Home. Documentation is
updated alongside implementation and records the current behavior of the system.

## Current documents

1. [Product specification](00-project-brief.md) — goals, capabilities, and MVP boundaries.
2. [Architecture](01-architecture.md) — implemented system structure, technology choices, and rationale.
3. [Delivery roadmap](02-roadmap.md) — implementation order and milestone exit criteria.
4. [Engineering workflow](03-engineering-workflow.md) — quality, ownership, and change process.
5. [Product decisions](04-open-decisions.md) — confirmed constraints and pending decisions.
6. [Product scope](SCOPE.md) — authoritative MVP and post-MVP boundaries.
7. [Product requirements](REQUIREMENTS.md) — stable requirements and acceptance criteria.
8. [UX flows and wireframes](UX_FLOWS.md) — navigation, screen structure, and state variants.
9. [Domain model and Firestore schema](DATA_MODEL.md) — entities, paths, fields, and synchronization.
10. [Development guide](DEVELOPMENT.md) — local build, tests, and device deployment.
11. [Firebase environment setup](FIREBASE_SETUP.md) — application registration, identity, and seed data.
12. [Implementation status](IMPLEMENTATION_STATUS.md) — verified capabilities, limitations, and next work.
13. [Technology reference](TECHNOLOGY.md) — libraries, engineering concepts, and where each is used.
14. [Code map](CODE_MAP.md) — responsibility and data-flow guide by module and source file.
15. [Testing strategy](TESTING.md) — test layers, commands, and current evidence.
16. [Demonstration runbook](DEMO_RUNBOOK.md) — repeatable end-to-end product walkthrough.
17. [Camera node design](CAMERA_NODE.md) — optional second-phone snapshot architecture.

Architecture decisions are stored under [`adr/`](adr/).

Active delivery work is tracked in the repository-level [backlog](../TODO.md).

Documentation is versioned with code. Planned behavior must be labelled separately from verified
implementation, and verification evidence must be updated when behavior changes.
