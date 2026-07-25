# Firebase Environment Setup

## Overview

Smart Home uses separate Firebase application registrations for Android and the browser simulator. Both
applications connect to the same Firebase project and Firestore database while authenticating as
different users with different home roles.

Environment-specific files and credentials must not be committed.

## 1. Create the Firebase project

1. Create a project in the Firebase console.
2. Choose the Firestore location carefully; the database location cannot be changed after provisioning.
3. Analytics is optional for the current product scope.
4. Record the Firebase project ID for local configuration and CLI project selection.

## 2. Register the Android application

1. Add an Android application with package name:

   ```text
   com.smarthome.app
   ```

2. Download `google-services.json`.
3. Place it at:

   ```text
   app/google-services.json
   ```

This file is ignored by Git. Do not share it through the repository.

## 3. Register the simulator web application

1. Add a Web application in the same Firebase project.
2. Copy the web configuration values.
3. In `simulator/`, copy `.env.example` to `.env.local`.
4. Fill the `VITE_FIREBASE_*` values from the Firebase web application configuration.

The web API key identifies the Firebase application but is not an administrative secret. Authorization
still depends on Firebase Authentication and Security Rules. The local environment file remains
untracked to keep environments independent.

## 4. Enable authentication

Enable the Email/Password provider under Firebase Authentication. Create two initial accounts:

- An owner account used by the Android application
- A dedicated simulator account used only by the browser hardware simulator

Use strong local credentials and do not store passwords in source code, seed files, shell history, or
documentation. Record each generated Firebase Authentication user ID for membership documents.

## 5. Create Firestore

Create a Cloud Firestore database. Security configuration is defined in
`firebase/firestore.rules`; do not leave a deployed database in permissive test mode.

Use the Firebase console or trusted Admin tooling to create the initial documents below. Console/Admin
operations are appropriate for bootstrapping because the owner membership does not exist yet.

### Home document

Path:

```text
homes/demo-home
```

Fields:

```text
name: "Primary home"
timezone: "Asia/Colombo"
createdBy: <owner user ID>
createdAt: <timestamp>
updatedAt: <timestamp>
```

### Owner membership

Path:

```text
homes/demo-home/members/<owner user ID>
```

Fields:

```text
role: "OWNER"
active: true
createdAt: <timestamp>
```

### Simulator membership

Path:

```text
homes/demo-home/members/<simulator user ID>
```

Fields:

```text
role: "SIMULATOR"
active: true
createdAt: <timestamp>
```

### Demo outlet

Path:

```text
homes/demo-home/devices/main-outlet
```

Document shape:

```text
name: "Main outlet"
profile: "OUTLET"
floorId: "ground-floor"
roomId: "utility"
position:
  column: 1
  row: 1
desired:
  status: "OFF"
  requestId: null
  requestedBy: null
  requestedAt: null
reported:
  status: "OFF"
  requestId: null
  updatedAt: <timestamp>
  errorCode: null
commandState: "IDLE"
config: {}
createdAt: <timestamp>
updatedAt: <timestamp>
```

## 6. Configure the simulator home

The default value in `.env.example` matches the seeded home path:

```dotenv
VITE_FIREBASE_HOME_ID=demo-home
```

Start the simulator and sign in with the dedicated simulator account:

```bash
cd simulator
npm run dev
```

## 7. Configure the Firebase CLI

Use a Node.js version supported by Cloud Functions development before backend work. Then install and
authenticate the Firebase CLI:

```bash
npm install -g firebase-tools
firebase login
firebase use --add
```

Select the Firebase project and choose an alias such as `development`. The resulting `.firebaserc` may
be committed because a Firebase project ID is not an administrative credential.

Deploy Firestore rules only after running their emulator-backed tests:

```bash
firebase deploy --only firestore:rules,firestore:indexes
```

## 8. Verify and deploy backend automation

The `functions/` package targets the Node.js 22 runtime. Build and test it before deployment:

```bash
npm --prefix functions install
npm --prefix functions run check
```

Scheduled functions require a billing-enabled Firebase project. Enabling billing is an explicit project
owner decision. After that decision, deploy the tested index and functions:

```bash
firebase deploy --only firestore:indexes,functions
```

The safety scheduler scans once per minute, so a due cutoff may be applied up to approximately one
minute after its exact deadline. The transaction rechecks the current document and uses deterministic
event/alert IDs so retries or overlapping invocations do not create duplicates.

## 9. Build and run the Android client

The Android client uses the Firebase Android BoM so Authentication and Firestore SDK versions remain
compatible. The Google Services Gradle plugin converts `app/google-services.json` into generated Android
resources during the build.

Build and run unit tests before installing:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Connect a development device with USB debugging enabled, verify it, and install the debug application:

```bash
adb devices
./gradlew :app:installDebug
```

Sign in with an active owner account. Passwords remain user-supplied runtime values and must never be
stored in source code or project documentation.

## 9. Verify real-time outlet synchronization

Run the simulator and Android application simultaneously using their separate authenticated identities.
Verify this command lifecycle:

```text
Android writes desired state and PENDING
    → simulator receives the Firestore snapshot
    → simulator applies the command
    → simulator writes reported state and APPLIED
    → Android receives the Firestore snapshot and renders confirmation
```

Then report `ERROR` and `DISCONNECTED` from the simulator. The Android viewport must update without a
manual refresh and prevent normal power commands until the reported state returns to `ON` or `OFF`.

## Verification checklist

- `app/google-services.json` is ignored by Git.
- `simulator/.env.local` is ignored by Git.
- Email/Password authentication is enabled.
- Owner and simulator accounts are separate identities.
- Both membership document IDs exactly match their Authentication user IDs.
- The simulator account can read the outlet and write reported state.
- The simulator account cannot write desired state, membership, events, or alerts.
- The owner account cannot directly forge reported hardware state.
- Android commands appear in the simulator without a manual refresh.
- Simulator confirmations and error states appear in Android without a manual refresh.

## 10. Verify layout and device creation

The Android owner can create floor and room documents, then create any supported device profile from a
valid grid coordinate. Device creation initializes a complete safe `OFF`/`IDLE` twin and profile config
in one write. The deployed rules validate authorization, floor/room containment, channel count, safety
duration, schedule structure, and HTTPS camera media.

Current profile creation checks:

- `OUTLET`: empty config
- `MULTI_SWITCH`: two, three, or five initialized channel records
- `SAFETY_OUTLET`: maximum on-duration from 60 to 14,400 seconds
- `LIGHT`: disabled default schedule with local times and `Asia/Colombo`
- `CAMERA`: snapshot media with an HTTPS URI and server capture timestamp

The repeatable authenticated seed tool is `simulator/scripts/seed-demo.mjs`. It requires Firebase web
configuration and owner credentials through process environment variables listed in
`firebase/seed.example`; values are never committed. The tool skips existing fixed IDs and uses the same
Security Rules as Android rather than bypassing authorization with a service account.
