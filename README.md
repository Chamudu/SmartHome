# Smart Home

A real-time home monitoring and control platform for Android. Smart Home brings floor planning,
device control, safety automation, camera monitoring, and usage insights into a single system.

## Platform components

- `mobile/` — native Android client built with Kotlin and Jetpack Compose
- `simulator/` — browser-based hardware simulator built with React and TypeScript
- `backend/` — trusted automation and notification services
- `firebase/` — database rules, indexes, and local emulator configuration
- `docs/` — product and engineering documentation

## Product capabilities

- Create simple multi-floor home layouts using rooms and a configurable grid
- Place and control outlets, switch gangs, lights, safety-critical appliances, and cameras
- Observe real-time device state across the Android client and hardware simulator
- Automatically switch off safety-critical appliances after a configured duration
- Schedule lighting and receive operational alerts
- Review device activity and usage history

The product is currently in the architecture and specification stage. See the
[documentation index](docs/README.md) and [delivery roadmap](docs/02-roadmap.md).
