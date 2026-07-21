# Milestone 0 validation

## Required commands

```bash
./gradlew clean test assembleDebug
aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk
apkanalyzer manifest print app/build/outputs/apk/debug/app-debug.apk
apkanalyzer dex packages app/build/outputs/apk/debug/app-debug.apk
apksigner verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.lifeos.personal.debug/com.lifeos.personal.MainActivity
```

## Acceptance criteria

- Build and unit tests pass.
- APK package is `com.lifeos.personal.debug` for debug or `com.lifeos.personal` for release.
- `minSdkVersion` is 28 and `targetSdkVersion` is 35.
- APK signature verifies.
- App starts on Android 14 without a fatal exception.
- The DataStore test value survives an `adb install -r` update signed by the same key.
