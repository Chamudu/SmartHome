# Team Development Onboarding

This runbook takes a contributor from repository access to a verified local environment. Complete it in
order because Android, Firebase emulators, and the web simulator share parts of the toolchain.

## 1. Understand the three applications and three identities

| Component | Stack | Main directory |
| --- | --- | --- |
| Mobile controller | Kotlin, Jetpack Compose, Gradle | `app/` |
| Hardware simulator | React, TypeScript, Vite | `simulator/` |
| Trusted automation | TypeScript, Cloud Functions, Firebase Admin | `functions/` |

Do not confuse these identities:

1. A **GitHub account** authorizes clone, push, and pull-request operations.
2. A contributor's **Google account** authorizes Firebase Console and Firebase CLI operations.
3. A **Firebase Authentication user** signs into the running Android app or simulator and receives
   permissions from `homes/{homeId}/members/{uid}`.

Each contributor uses their own GitHub and Google accounts. The Android owner and simulator use
separate runtime accounts because their Firestore permissions are deliberately different.

## 2. Request access and local configuration

Ask the maintainer for:

- GitHub repository collaborator access;
- Firebase project access only if the assigned work needs Console or deploy operations;
- `app/google-services.json` through a private team channel;
- the Firebase Web app values needed for `simulator/.env.local`;
- a Firebase Authentication test identity with an active home membership.

Never share account passwords. Never commit `google-services.json`, `.env.local`, `local.properties`,
signing files, service-account keys, or generated output.

## 3. Install workstation tools

Required tools:

- Git
- Android Studio
- Android SDK Platform 37
- Android SDK Platform-Tools and Build-Tools
- JDK 17 or newer; Android Studio's bundled JBR is supported
- `fnm` for Node version selection
- Node.js 22.23.1 and npm
- Firebase CLI 15 or a compatible version

On Arch Linux, install Git and `fnm` from the configured package repositories:

```bash
sudo pacman -S --needed git fnm
```

Install Android Studio using the team's accepted Arch package source or Google's archive. In Android
Studio, open **Settings > Languages & Frameworks > Android SDK** and install Platform 37, Build-Tools,
Platform-Tools, and Command-line Tools.

Why the tools differ:

- the JDK runs Gradle and the Firebase emulators;
- the Android SDK supplies Android APIs, packaging tools, and `adb`;
- the project Gradle plugin supplies Kotlin, so no separate system Kotlin compiler is required;
- Node runs the simulator and TypeScript backend tools; npm installs their dependencies.

## 4. Configure Java and Android tools

For the common Arch Android Studio location, add this to `~/.zshrc`:

```zsh
export JAVA_HOME=/opt/android-studio/jbr
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

Use the actual SDK location shown by Android Studio if it differs. Reload and verify:

```bash
source ~/.zshrc
java -version
adb version
```

## 5. Configure Node 22

Add the `fnm` hook to `~/.zshrc`:

```zsh
eval "$(fnm env --use-on-cd --shell zsh)"
```

Then run:

```bash
source ~/.zshrc
fnm install 22.23.1
fnm use 22.23.1
fnm default 22.23.1
node --version
npm --version
```

Expected Node output is `v22.23.1`. `.node-version` lets `fnm --use-on-cd` select it when entering the
repository.

## 6. Clone the integration branch

```bash
git clone https://github.com/Chamudu/SmartHome.git
cd SmartHome
git switch dev
git pull --ff-only origin dev
git status -sb
```

Expected: a clean `dev...origin/dev` status. `dev` currently contains all integrated product work;
`develop` is an older ancestor and must not receive new independent commits.

Set commit attribution if needed:

```bash
git config --global user.name "Your Name"
git config --global user.email "your-github-email@example.com"
```

## 7. Create Android machine configuration

Create `local.properties` at the repository root using the SDK path shown by Android Studio:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Place the privately supplied Firebase Android file at:

```text
app/google-services.json
```

Confirm both are ignored:

```bash
git check-ignore -v local.properties app/google-services.json
```

The Firebase configuration identifies the app/project; it is not authorization. Firebase Auth and
Firestore Security Rules still decide which reads and writes are allowed.

## 8. Install locked JavaScript dependencies

Run from the repository root:

```bash
npm --prefix simulator ci
npm --prefix functions ci
npm --prefix firebase/tests ci
```

`npm ci` reproduces each committed `package-lock.json`. Use `npm install` only when intentionally
changing dependencies and reviewing both the manifest and lockfile changes.

## 9. Configure Firebase CLI

With Node 22 active:

```bash
npm install -g firebase-tools
firebase --version
firebase login
firebase use development
firebase projects:list
firebase firestore:databases:list
```

The tracked `.firebaserc` maps `development` to `smart-home-a99ed`. CLI login uses the contributor's
Google account; it does not sign into the Android app or simulator.

Do not deploy Rules, indexes, or Functions from an unreviewed branch. Production Functions deployment
is outside the current no-billing workflow.

## 10. Configure and run the simulator

```bash
cp simulator/.env.example simulator/.env.local
git check-ignore -v simulator/.env.local
```

Fill the ignored file from the Firebase Web app configuration:

```dotenv
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=...
VITE_FIREBASE_PROJECT_ID=smart-home-a99ed
VITE_FIREBASE_STORAGE_BUCKET=...
VITE_FIREBASE_MESSAGING_SENDER_ID=...
VITE_FIREBASE_APP_ID=...
VITE_FIREBASE_HOME_ID=demo-home
```

Start it:

```bash
npm --prefix simulator run dev
```

Open the Vite URL and sign in with the dedicated `SIMULATOR` runtime account. Stop with `Ctrl+C`.

## 11. Build and install Android

Use the Gradle Wrapper; do not install global Gradle or Kotlin versions:

```bash
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

For a physical phone:

1. Enable Developer options and USB debugging.
2. Connect a data-capable cable and approve the debugging fingerprint.
3. Run:

   ```bash
   adb devices -l
   ./gradlew --no-daemon :app:installDebug
   ```

Sign into Android with an `OWNER` runtime account, not the simulator account.

## 12. Run the full local checks

```bash
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
./gradlew --no-daemon --no-configuration-cache :app:lintDebug

npm --prefix simulator run typecheck
npm --prefix simulator run lint
npm --prefix simulator run build

npm --prefix functions run check

firebase emulators:exec --project demo-smart-home --only firestore \
  "npm --prefix firebase/tests test"
```

The `demo-` project ID is a safety boundary: emulator tests cannot accidentally fall through to the
live Firebase project. Permission-denied output is expected for tests that prove malicious writes fail;
the final test result determines success.

## 13. Verify the realtime device twin

Run Android and the simulator simultaneously:

1. Toggle an outlet in Android.
2. Confirm the simulator receives the desired state without refresh.
3. Confirm the simulator reports the applied state and correlated request ID.
4. Confirm Android leaves pending state and shows the reported state.
5. Report `ERROR` and `DISCONNECTED` from the simulator and confirm Android updates.
6. Restore `ON` or `OFF`.
7. Open Usage and verify the event, activation count, active duration, and estimate update.

This validates authentication, authorization, listeners, the device-twin contract, event history, and
reactive rendering together.

## 14. Create a feature branch

Never work directly on `main`, `dev`, or the older `develop` branch:

```bash
git switch dev
git pull --ff-only origin dev
git switch -c feature/short-description
```

Work in focused commits:

```bash
git status --short
git diff
git add path/to/file1 path/to/file2
git commit -m "feat(android): add alert acknowledgement"
git push -u origin feature/short-description
```

Stage explicit paths rather than `git add .`. Before review, integrate current `dev`. Rebase only a
private branch:

```bash
git fetch origin
git rebase origin/dev
```

If multiple people share the feature branch, preserve their commit IDs with a merge:

```bash
git fetch origin
git merge origin/dev
```

Rerun affected checks, push, and open a pull request into **`dev`**. The pull request should describe
behavior, tests, manual evidence, schema/Rules effects, and documentation changes.

## 15. Collaboration rules

- One branch and pull request should represent one coherent change.
- Coordinate before editing high-conflict files such as `OutletScreen.kt`, `OutletViewModel.kt`,
  `useDeviceSimulator.ts`, `functions/src/index.ts`, or `firestore.rules`.
- Treat every Firestore field change as a shared Android/simulator/functions/Rules contract change.
- Do not use `git add -f` to bypass ignored-file protection.
- Do not rewrite a shared branch with a force push.
- Do not describe a client timer as trusted server enforcement.
- Update requirements, status, tests, code map, and the demo runbook with the implementation.

## Completion checklist

- [ ] A fresh shell reports Node 22.23.1 and Java 17 or newer.
- [ ] `adb`, `firebase`, `npm`, and Git are available.
- [ ] The clone is clean on `dev`.
- [ ] Android and simulator configuration files exist and are ignored.
- [ ] All three `npm ci` operations succeed.
- [ ] Android tests, assembly, and lint pass.
- [ ] Simulator typecheck, lint, and build pass.
- [ ] Functions build/tests and Firestore Rules tests pass locally.
- [ ] Android runs on a phone or emulator.
- [ ] Android and simulator complete the realtime outlet loop.
- [ ] The contributor can push a feature branch and open a pull request into `dev`.

## Common failures

| Symptom | Likely reason | Correction |
| --- | --- | --- |
| `java: command not found` | Android Studio JBR is not on `PATH` | Correct `JAVA_HOME`, reload zsh, and run `which java` |
| Fresh shell uses another Node | Missing `fnm` hook | Add the hook and run `exec zsh` |
| `adb: command not found` | Platform-Tools absent from `PATH` | Install it and correct `ANDROID_HOME` |
| Gradle cannot find the SDK | Wrong `local.properties` | Copy the SDK location displayed by Android Studio |
| Google Services task fails | Missing/wrong Android Firebase config | Obtain the `com.smarthome.app` file privately |
| Missing/insufficient permissions | Wrong UID, role, membership, or forbidden write | Compare Auth UID with `homes/demo-home/members/{uid}` |
| Simulator has no Firebase config | `.env.local` incomplete | Fill every web-app value and restart Vite |
| Rules tests log denials but pass | Negative authorization cases | Read the final Vitest status |
| Push rejected | Missing access or stale branch | Confirm collaborator access, fetch, and integrate `origin/dev` |
