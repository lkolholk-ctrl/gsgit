# CLAUDE.md — GsGit Project Rules

## Speed & Efficiency
- **NEVER enter plan mode** — just find the bug and fix it immediately
- **NEVER explore with subagents** — use grep/read directly on the specific file
- **Minimize tool calls** — read only the file you're editing, don't read 10 files "for context"
- **Fix, don't plan** — if you know what's wrong, edit the code. Don't write plans or ask questions first
- **One commit, one push, one build** — no intermediate commits, no retry loops

## Code Rules
- .md file rendering MUST use `ReadmeHtmlDocument` (WebView) — same as README tab. NEVER use `MarkdownCanvas` or native Compose block rendering for .md files
- When fixing UI issues, look at how the WORKING version does it and copy the same approach
- All functions in `GitHubRepoModule.kt` marked `internal` are accessible from other screens in the same module
- `GHRepo` requires all fields when constructing — use defaults for unused ones

## Build & Deploy
- `ssh myvps` connects to VPS
- Repo on VPS: `/root/gsgit/gsgit`
- Build: `./gradlew assembleRelease`
- APK output: `app/build/outputs/apk/release/app-release.apk`
- Download to phone: `scp myvps:/root/gsgit/gsgit/app/build/outputs/apk/release/app-release.apk /storage/emulated/0/Download/gsgit-latest.apk`
- Screenshots from user: `/storage/emulated/0/Download/Screenshot/`

## Common Mistakes to Avoid
- Don't use `MarkdownCanvas` from `GitHubMarkdownModule.kt` for .md file viewing — it's the old raw renderer
- Don't use `Column(verticalScroll)` for long documents — use `LazyColumn` or `WebView`
- Don't commit `app-release.apk` — it's in `.gitignore`
- `git push` may fail if APK was accidentally staged — unstage it first
