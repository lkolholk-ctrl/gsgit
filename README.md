# GsGit

Android GitHub client built with Kotlin and Jetpack Compose.

## Local build

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Android NDK 26.3.11579264
- CMake 3.22.1

The debug variant uses the Android Gradle Plugin's default debug signing key:

```bash
./gradlew :app:assembleDebug
```

For release builds, keep `release.keystore` outside the repository and provide
these Gradle properties locally or through CI secrets:

```properties
GSGIT_RELEASE_STORE_PASSWORD=...
GSGIT_RELEASE_KEY_ALIAS=...
GSGIT_RELEASE_KEY_PASSWORD=...
```

`app/omvll/libOMVLL.so` is optional for local builds. A release build without
it is not obfuscated by O-MVLL; production CI should install it explicitly.

## CI

GitHub Actions builds the debug APK and runs `lintDebug` for each push and pull
request. The workflow fetches full history so `GITHUB_RUN_NUMBER` can provide
a monotonically increasing CI `versionCode`.
