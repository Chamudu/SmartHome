# Optional Mobile Camera Node

## Goal

Allow a second Android phone to act as a cloud-connected snapshot camera while preserving the existing
mock-camera contract and keeping live video streaming outside the MVP.

## Proposed flow

```text
Camera phone (CameraX)
  → capture JPEG
  → Firebase Storage home/device path
  → Firestore camera config metadata and server timestamp
  → Android dashboard listener
  → authenticated snapshot display
```

## Recommended implementation

- Add an explicit camera-node mode or small companion Android module.
- Request camera permission only when entering that mode.
- Capture manually first; optional periodic capture can use lifecycle-aware foreground operation.
- Compress and resize snapshots before upload.
- Store media under a home/device-scoped Storage path protected by membership rules.
- Store a Storage object path rather than accepting arbitrary local file or script schemes.
- Update `capturedAt` with trusted server time and show loading/error/placeholder states in the viewer.

## Why snapshots before streaming

Firebase Storage is designed for objects, not continuous low-latency video. Snapshots work across
networks and fit the current Firestore listener architecture. WebRTC streaming would require signaling,
peer negotiation, lifecycle/audio-video handling, and possibly TURN infrastructure. A local HTTP/MJPEG
server would depend on the same Wi-Fi network, changing IP addresses, and cleartext network policy.

## Security and privacy

- Require authenticated home membership for reads and a dedicated camera-node role/write path.
- Never make the whole Storage bucket public.
- Do not log download tokens or capture private spaces without consent.
- Validate file type, object size, device association, and allowed metadata.
- Provide an obvious capture indicator and a way to disable the camera node.

This capability is time-permitting and must not delay the trusted safety cutoff or required profile
controls.
