# ADR-0002: Firebase Cloud Platform

- Status: Accepted
- Date: 2026-07-20

## Context

Android and the hardware simulator need a shared real-time source of truth, authenticated access,
trusted automation, media storage, and alert delivery without maintaining dedicated servers.

## Decision

Use Firebase Authentication, Cloud Firestore, Cloud Functions, Cloud Storage, and Cloud Messaging where
required. Use the Firebase Emulator Suite for security and backend integration tests.

## Consequences

- Firestore snapshot listeners provide real-time client updates and local caching.
- Cloud Functions provide trusted safety and scheduling behavior.
- Security Rules become part of the application authorization model and require automated tests.
- The document schema must be designed around queries and Firestore transaction limits.
- Managed-service coupling is accepted to reduce infrastructure and operations work.
