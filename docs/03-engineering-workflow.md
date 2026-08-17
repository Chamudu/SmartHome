# Engineering Workflow

## Change process

New contributors should complete the [team development onboarding](TEAM_ONBOARDING.md) before taking
ownership of a feature.

- Keep `main` releasable and develop changes on short-lived feature branches.
- Connect each change to a documented product capability or defect.
- Prefer small commits with focused, imperative messages.
- Review data contracts whenever a change crosses Android, simulator, and backend boundaries.
- Update tests and documentation in the same change as behavior.

## Definition of done

A feature is complete when:

- Acceptance criteria are satisfied.
- Relevant unit and integration tests pass.
- Loading, empty, offline, disconnected, and error behavior has been considered.
- Authorization rules protect all new reads and writes.
- Logging contains enough context to diagnose failures without exposing sensitive data.
- User-facing and engineering documentation reflects the implemented behavior.

## Source-control boundaries

The repository includes application code, infrastructure configuration, non-secret example settings,
architecture decisions, test fixtures, and operational documentation. It excludes credentials,
signing material, local environment configuration, generated output, and personal workspace files.

## Ownership

Areas may have primary maintainers, but contracts are shared. A change to the device schema must be
validated against the Android client, hardware simulator, backend automation, security rules, and
event reporting before release.
