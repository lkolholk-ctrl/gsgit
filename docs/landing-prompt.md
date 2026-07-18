Build a product landing page for **GsGit** — a free, open-source GitHub client
for Android. Produce exactly two deliverables: a single-page `index.html` and a
separate `privacy.html`. Everything you produce — all copy, headings, alt
texts, meta tags, code comments — must be in **English only**.

## What GsGit is

GsGit is a full-featured native GitHub client for Android (Kotlin + Jetpack
Compose, ~75k lines of code), built by a solo developer and fully open source.
Its visual identity is a dark terminal aesthetic: near-black background,
monospaced type, subtle neon-green accents, thin borders, light glassmorphism.
It is aimed at developers who live in GitHub and want the full workflow —
code, issues, pull requests, CI, releases — in their pocket.

Three things make it stand out from other mobile GitHub clients:

1. **Sign in without tokens.** Authentication uses the GitHub App device flow:
   the user never creates, pastes or stores a personal access token. The app
   keeps only a short-lived GitHub App session in hardware-encrypted storage,
   refreshes it automatically, and access can be revoked at any moment from
   GitHub settings. When a new device signs in to the account, every other
   device gets a security alert push.

2. **Instant push notifications.** GsGit runs its own open-source notification
   backend (api.gsgit.org): GitHub webhooks hit the server, the server routes
   the event to the right user and delivers it through FCM — a GitHub event
   reaches the phone in about two seconds, with no battery-draining polling.
   Notifications are rich (expandable full text of comments, issue bodies,
   commit messages, CI results with ✅/❌) and split into ten native Android
   notification channels the user can toggle individually in system settings:
   Issues, Pull requests, Comments, CI & deployments, CI starts (opt-in,
   off by default), Commits & branches, Releases, Discussions,
   Stars/forks/members, Account security.

3. **CI that feels native.** Workflow runs open as full screens with jobs,
   steps and logs; while a run is in progress the screen live-refreshes
   automatically. Artifacts download in the background with a progress
   notification and a Cancel button, land silently in the system Downloads
   folder (no save-as dialogs), get unzipped automatically, and a tap opens
   the result. Workflows can be dispatched and re-run from the phone, and
   artifacts can even be published as release assets.

## Full feature list

- Repository browser: file tree, README rendering, code viewer and a real
  code editor with syntax highlighting, commit sheet, branch create/switch/
  delete, file blame and history, quick-open and repo-wide code search.
- Issues: lists with filters, full detail, comments, labels, milestones,
  creation and state management.
- Pull requests: reviews, review comments, checks, merge with method choice,
  branch compare, commit diffs.
- Actions: runs, jobs, steps, logs, artifacts (background downloads),
  workflow dispatch with input forms, re-runs, deployments.
- Releases: browse, download assets, publish releases straight from CI
  artifacts.
- Also: discussions, gists, webhooks management, collaborators, branch
  protection rules, notifications inbox, commit history and compare screens,
  repo insights, packages, projects.
- Deep links: any github.com URL — commits, runs, compare, issues, PRs,
  releases, files — opens directly on the right native screen.
- Security: optional biometric/PIN app lock, encrypted token storage, native
  anti-tamper checks; the app stores nothing in plaintext.
- The push backend is open source in the same repository and self-hostable
  (Docker Compose, Caddy, zero-dependency Node.js).
- Free, no ads, no analytics, no tracking.

Links to use:
- Download APK → https://github.com/lkolholk-ctrl/gsgit/releases/latest
- Source code → https://github.com/lkolholk-ctrl/gsgit
- Service status → https://status.gsgit.org
- Privacy → privacy.html

## Hard technical requirements

- Each page is **one self-contained HTML file**: all CSS and JS inline. No
  CDNs, no external requests of any kind, no build tools, no frameworks.
  Fonts: system fonts or anything you can embed inline — just no network
  fetches.
- Mobile-first and fully responsive from 360 px up to desktop; the page body
  must never scroll horizontally.
- Fast and lightweight: vanilla JS only where genuinely needed (anchor
  smooth-scroll; optional simple lightbox for the gallery).
- SEO and social meta: `<title>`, meta description, Open Graph tags.
- The screenshot gallery uses six images at relative paths
  `assets/screen-1.png` … `assets/screen-6.png`. **These images are pre-made
  device mockups with transparent backgrounds — the phone hardware frame is
  already part of each PNG. Do not draw any CSS device frames around them;
  display the images as-is.** Until the files exist the layout must degrade
  gracefully to neat labeled placeholders without breaking.

## Design

The design, layout, typography, color system, motion and overall art
direction are **entirely your call — you decide what looks best**. For
context only: the app itself has a dark terminal aesthetic (near-black,
monospace, subtle green accents, light glassmorphism) — you may echo it,
reinterpret it, or take the landing somewhere stronger if you believe it
sells the product better. The audience is developers.

One legal constraint: do not use the GitHub logo or the Octocat as the
product logo; the product is represented by the text mark "GsGit".

## index.html content (structure is a suggestion — presentation is yours)

1. **Hero**: product name, a short punchy tagline about GitHub in your pocket
   with events pushed to you in seconds (improve on this), two buttons —
   `Download APK` and `Source code` — plus a small line: Android 8.0+, free &
   open source.
2. **Three killer features** as cards: token-free sign-in, instant pushes
   (~2 s, own open backend, 10 channels), native CI experience (live runs,
   background artifact downloads).
3. **Full capabilities** as a compact grid using the feature list above.
4. **Gallery**: the six mockup screenshots with short captions.
5. **How instant pushes work**: a one-line scheme
   `GitHub webhook → api.gsgit.org → FCM → your phone (~2 s)` and two
   sentences noting the server stores only the GitHub login and the device
   push token, and that the backend is open source and self-hostable.
6. **Security & privacy** teaser: open source, tokens never leave the device,
   encrypted storage, revoke from GitHub anytime; link to privacy.html.
7. **Footer**: GitHub · Releases · Status · Privacy · © 2026 GsGit.

## privacy.html structure

Same visual style, short and honest, in English:

- What the notification server (api.gsgit.org) stores: the GitHub login and
  the device push token — solely to deliver notifications. Nothing else: no
  analytics, no ads, no selling of data.
- What stays on the device: GitHub App session tokens in encrypted storage.
- GitHub data is accessed via the official GitHub API on the user's behalf
  and is never persisted server-side.
- Data deletion: signing out or disabling pushes unregisters the device;
  revoking the GsGit App in GitHub settings cuts all access instantly.
- Contact: the project's GitHub Issues page.
- Last-updated date.
