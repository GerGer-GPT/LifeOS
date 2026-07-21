# LifeOS

Milestone 0 of the maintainable Android application. This project deliberately uses the
standard Android Gradle Plugin build and does not reuse DEX files or APK packaging scripts.

## Identity

- Application ID: `com.lifeos.personal`
- Debug application ID: `com.lifeos.personal.debug`
- Minimum Android: API 28
- Target/compile SDK: API 35

## Build

Open the directory in Android Studio or install Android SDK 35 and run:

```bash
./gradlew clean test assembleDebug
```

The committed Gradle Wrapper uses Gradle 8.9.

## Release signing

Copy `keystore.properties.example` to `keystore.properties` and reference the permanent
PKCS12 release key. The private key and passwords must never be committed. An unsigned
release build is expected when that private file is absent.

## Update persistence check

1. Install version code 1 and increment the displayed saved value.
2. Increase `versionCode` and `versionName` without changing the application ID or key.
3. Build and run `adb install -r` with the new APK.
4. Confirm that the saved DataStore value remains unchanged.
