# Building the Crisis AI APK

This guide shows how to build the Crisis AI (flavor `crisisAI`) APK on Windows.

## 1. Prerequisites
- JDK 17 installed and `JAVA_HOME` set
- Android SDK + Platform tools (ADB) installed
- (Optional) Keystore + passwords for release builds

> Current toolchain (updated): Android Gradle Plugin 8.6.1 + Gradle 8.9

## 2. Clean (optional)
```bat
cd /d D:\Android-Dev\CrisisAI
gradlew.bat clean
```

## 3. Build Debug APK (Crisis AI flavor)
```bat
cd /d D:\Android-Dev\CrisisAI
gradlew.bat assembleCrisisAIDebug
```
**Output:** `app\build\outputs\apk\crisisAI\debug\app-crisisAI-debug.apk`

Install on a connected device:
```bat
adb install -r app\build\outputs\apk\crisisAI\debug\app-crisisAI-debug.apk
```

## 4. Build Release APK (signed & minified)
Set environment variables for the signing config **in the same terminal session**:
```bat
set RELEASE_KEYSTORE_PASSWORD=YourStorePassword
set RELEASE_KEYSTORE_ALIAS=yourKeyAlias
set RELEASE_KEY_PASSWORD=YourKeyPassword
```
Ensure `keystore.jks` exists at project root: `D:\Android-Dev\CrisisAI\keystore.jks`.

Then build:
```bat
cd /d D:\Android-Dev\CrisisAI
gradlew.bat assembleCrisisAIRelease
```
**Output:** `app\build\outputs\apk\crisisAI\release\app-crisisAI-release.apk`

Verify signature:
```bat
"%JAVA_HOME%\bin\jarsigner.exe" -verify -verbose -certs app\build\outputs\apk\crisisAI\release\app-crisisAI-release.apk
```

## 5. Build AAB (Play Store)
```bat
cd /d D:\Android-Dev\CrisisAI
gradlew.bat bundleCrisisAIRelease
```
**Output:** `app\build\outputs\bundle\crisisAIRelease\app-crisisAI-release.aab`

## 6. Variant Matrix
| Flavor    | Build Type | Task Name                | Artifact Path (APK) |
|-----------|------------|--------------------------|---------------------|
| crisisAI  | debug      | assembleCrisisAIDebug    | app/build/outputs/apk/crisisAI/debug |
| crisisAI  | release    | assembleCrisisAIRelease  | app/build/outputs/apk/crisisAI/release |
| smolChat  | debug      | assembleSmolChatDebug    | app/build/outputs/apk/smolChat/debug |
| smolChat  | release    | assembleSmolChatRelease  | app/build/outputs/apk/smolChat/release |

## 7. Common Issues
- Immediate build fail with just a number (e.g. `25`) and no explanation: This was caused by an invalid Android Gradle Plugin version (`8.13.0`) in `gradle/libs.versions.toml`. Fixed by downgrading to AGP `8.6.1` and Gradle `8.9` (see `gradle/wrapper/gradle-wrapper.properties`). Run `gradlew --version` to confirm.
- Missing NDK: let Android Studio download NDK version `27.2.12479018` or adjust `ndkVersion` in `app/build.gradle.kts`.
- Java version mismatch: run `gradlew.bat -version` to confirm JDK 17.
- Stuck model loading ("Loading model..."): Fixed by adding cancellation handling in `SmolLMManager.load` and resetting `modelLoadState` in `ChatScreenViewModel` if a load is cancelled by a fast chat switch.

## 8. Optional: Disable Minify for Easier Debugging
In `app/build.gradle.kts` inside `buildTypes.release`:
```kotlin
isMinifyEnabled = false
```

## 9. Quick Windows Helper Script
You can use `build_crisisai_debug.bat` (added in repo root) to build + locate APK.

## 10. CI Workflow
A GitHub Action workflow (`.github/workflows/build-crisis-ai.yml`) is included to auto-build debug & release artifacts. Add repository secrets for signing:
- `KEYSTORE_BASE64` (base64 of keystore.jks)
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEYSTORE_ALIAS`
- `RELEASE_KEY_PASSWORD`

Then trigger the workflow manually or on pushes.

---
If you need a script to also install automatically or to add ABI splits, open an issue or ask.
