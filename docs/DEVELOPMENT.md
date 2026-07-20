# Development Guide

## Prerequisites

- Android Studio 2026.1.2 or a compatible release
- JDK 17 or newer; the Android Studio bundled JDK is supported
- Android SDK Platform 37 and Build Tools 36.0.0 or newer
- A physical Android device with API 26 or newer, or a compatible emulator

The project uses the Gradle Wrapper. A system-wide Gradle installation is not required.

## Local SDK configuration

Create an untracked `local.properties` file at the repository root:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Do not commit this file because the SDK location varies by machine.

## Build and test

```bash
./gradlew test
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Run both checks together with:

```bash
./gradlew test assembleDebug
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
| `app` | Native Android application, Compose UI, domain logic, and Android tests |

Backend and simulator modules will be added as independent components after the Android foundation is
stable.
