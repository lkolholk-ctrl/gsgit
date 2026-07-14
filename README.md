# GsGit

Android GitHub client built with Kotlin and Jetpack Compose.

## Release build

Requirements:

- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Android NDK 26.3.11579264
- CMake 3.22.1

Keep `release.keystore` outside the repository and provide these Gradle
properties locally or through CI secrets:

```properties
GSGIT_RELEASE_STORE_PASSWORD=...
GSGIT_RELEASE_KEY_ALIAS=...
GSGIT_RELEASE_KEY_PASSWORD=...
```

The user-facing version has a single source of truth in `gradle.properties`:

```properties
GSGIT_VERSION_NAME=<version>
```

Build both signed release formats with:

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

`app/omvll/libOMVLL.so` is optional for local builds. A release build without
it is not obfuscated by O-MVLL; production CI should install it explicitly.

## CI

GitHub Actions only builds the release variant. Every eligible push, pull
request, or manual run produces a signed APK and AAB, verifies both signatures,
generates SHA-256 checksums, and stores the files as a workflow artifact.

A successful push to `main` additionally creates a `v<version>` tag and a
GitHub Release containing the APK, AAB, checksums, and an automatically generated
changelog. An existing tag causes an early failure, so every published batch
must advance `GSGIT_VERSION_NAME`. Manual runs can opt into publication with the
`publish_release` input; it is disabled by default.

The workflow fetches full history so `GITHUB_RUN_NUMBER` can provide a
monotonically increasing CI `versionCode`. No debug task is run in CI.
