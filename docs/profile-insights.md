# Profile Insights data contract

Version 1.0.51 removes seeded/random and hard-coded profile telemetry. Every displayed number now
has an explicit GitHub source or a labelled calculation.

| UI metric | Source | Window / limitation |
|---|---|---|
| Contribution calendar, total and streaks | GraphQL `contributionsCollection.contributionCalendar` | GitHub's returned `startedAt` → `endedAt` window |
| Commits / PRs / issues / reviews | GraphQL `total*Contributions` | Same contribution window; values may include private contribution counts GitHub exposes to the viewer |
| Language volume | GraphQL repository `languages.edges.size` | Up to 100 owned public non-fork repos and top 10 languages per repo; values are bytes |
| Pushed commits by local hour | REST public Events `PushEvent.created_at` weighted by `payload.size` | At most 300 public events and GitHub's public Events retention; chart uses device local time |
| Stars, repository/follower/gist achievements | REST user and fully paginated public repository data | Public data only |
| Account age | REST profile `created_at` compared with current device date | Calculated days, not uptime or online presence |
| Developer profile bars | Language-byte and contribution-type shares | Rule-based descriptive label, explicitly not a GitHub skill rating |

If GraphQL analytics are unavailable, the UI says so. It must not substitute seeded values, force
zero counts to one, infer online status, or display simulated boot/signature messages.
