# Development Guide

## Prerequisites

- Android Studio 2026.1.2 or a compatible release
- JDK 17 or newer; the Android Studio bundled JDK is supported
- Android SDK Platform 37 and Build Tools 36.0.0 or newer
- A physical Android device with API 26 or newer, or a compatible emulator
- Node.js 22 for Firebase CLI and simulator development
- Firebase CLI 15 or compatible

The project uses the Gradle Wrapper. A system-wide Gradle installation is not required.

## Local SDK configuration

Create an untracked `local.properties` file at the repository root:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Do not commit this file because the SDK location varies by machine.

## Build and test

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Run both checks together with:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## Run on a physical Android device

1. Enable Developer options by tapping the device build number seven times.
2. Enable USB debugging under Developer options.
3. Connect the device with a data-capable USB cable.
4. Accept the debugging fingerprint prompt on the device.
5. Confirm the device is visible:

   ```bash
   adb devices -l
   ```

6. Install or update the debug build:

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

Alternatively, select the connected device in Android Studio and run the `app` configuration.

If the device shows as `unauthorized`, unlock it and accept the debugging prompt. If it is absent,
check the cable, USB mode, device permissions, and `adb kill-server && adb start-server` from the host
terminal.

## Wireless debugging

On Android 11 or newer, Android Studio can pair with a phone over the same network through Device
Manager. USB is preferred for the initial setup because it removes network and pairing variables.

## Current modules

| Module | Purpose |
| --- | --- |
| `app` | Native Android application, Compose UI, domain logic, Firebase repositories, and Android tests |
| `simulator` | React/TypeScript hardware console using the Firebase Web SDK |
| `firebase/tests` | Vitest suite for Firestore Security Rules through the local emulator |

## Simulator checks

```bash
npm --prefix simulator run typecheck
npm --prefix simulator run lint
npm --prefix simulator run build
```

## Seed the demonstration home

`simulator/scripts/seed-demo.mjs` signs in as the owner and creates missing deterministic rooms/devices.
It reuses an existing level 0/1 floor when large enough and skips existing fixed IDs. Copy the variable
names from `firebase/seed.example`, export values for the current shell, then run:

```bash
npm --prefix simulator run seed:demo
```

Do not save or commit the owner password. Shell history can also retain inline values, so use a silent
prompt where available rather than typing the password into the command itself.

## Firestore rule checks

```bash
firebase emulators:exec --project demo-smart-home --only firestore \
  "npm --prefix firebase/tests test"
```

Use a `demo-` project ID for rule tests so the Firebase CLI cannot accidentally access production
services when an emulator is missing.
