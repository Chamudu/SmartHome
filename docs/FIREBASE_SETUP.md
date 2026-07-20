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

## 6. Configure the simulator identifiers

The default values in `.env.example` match the seeded paths:

```dotenv
VITE_FIREBASE_HOME_ID=demo-home
VITE_FIREBASE_OUTLET_ID=main-outlet
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

## Verification checklist

- `app/google-services.json` is ignored by Git.
- `simulator/.env.local` is ignored by Git.
- Email/Password authentication is enabled.
- Owner and simulator accounts are separate identities.
- Both membership document IDs exactly match their Authentication user IDs.
- The simulator account can read the outlet and write reported state.
- The simulator account cannot write desired state, membership, events, or alerts.
- The owner account cannot directly forge reported hardware state.
