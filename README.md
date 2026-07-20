# GsGit

GitHub in your pocket. A full GitHub client for Android — not a viewer.
You actually work: repositories, code, issues, pull requests and reviews,
straight from your phone.

Built with Kotlin and Jetpack Compose.

- Website: https://gsgit.org
- Live status: https://status.gsgit.org
- Price: Free
- Requires: Android 8.0+

## Features

- **Repositories** — browse, search, star, and manage with full repo control.
- **Code** — view and edit with syntax highlighting, right on device.
- **Issues** — read, open, comment, label, and close.
- **Pull requests** — diffs, reviews, comments, and merge.
- **Actions** — watch workflows, re-run jobs, and read logs.
- **Releases and Projects** — tags, releases, project boards, branches, and commits.

## Instant notifications

Push arrives about two seconds after the event on GitHub.

- **Speed** — GitHub to server to device in a couple of seconds.
- **Channels** — per-event control; pick what pings you.
- **Quiet hours** — silence by schedule.
- **Digest** — batch the noise into a summary.

## Built for privacy

- Sign in through the GitHub App — no classic personal access tokens.
- Zero tokens stored on the device.
- No excess permissions — no location, no personal-data access.

## Under the hood

- Single-module Kotlin app, Jetpack Compose UI.
- One networking core over raw `HttpURLConnection` — no Ktor, no Retrofit.
  Every GitHub API call goes through a single request layer with ETag caching,
  rate-limit handling, backoff, and retry.
- Native protection layer (hardware-backed keys, NDK).
- Zero-dependency push backend behind `api.gsgit.org`.

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
