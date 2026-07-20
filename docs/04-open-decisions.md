# Product Decisions

## Confirmed

| Decision | Outcome |
| --- | --- |
| Mobile platform | Native Android with Kotlin and Jetpack Compose |
| Cloud platform | Firebase; no external backend constraint |
| Floor creation | Users create simple floors from rectangular rooms on a configurable grid |
| Hardware representation | Independent React and TypeScript web simulator |
| Connectivity | Cloud-first real-time sync with local caching |
| Offline demo environment | Firebase Emulator Suite support is a post-MVP enhancement |
| Initial delivery model | The system must be buildable and maintainable by one primary developer |

## Pending

The following decisions should be resolved before their related milestone begins:

1. Minimum supported Android API level; API 26 is the proposed default.
2. Authentication experience: registration plus sign-in, or provisioned accounts only.
3. Floor editor constraints: maximum grid size, room overlap behavior, and supported room shapes.
4. Camera experience: periodically updated snapshots or a mock URI stream.
5. Notification scope: in-app alert history only, or Android push notifications as well.
6. Public hosting and Firebase environment strategy for the simulator.
7. Repository license and product name availability.
