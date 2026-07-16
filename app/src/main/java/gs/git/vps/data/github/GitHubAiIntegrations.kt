package gs.git.vps.data.github

internal data class GitHubAiIntegration(
    val id: String,
    val title: String,
    val appSlug: String,
    val description: String,
    val finishHint: String,
    val guideUrl: String,
) {
    val appUrl: String get() = "https://github.com/apps/$appSlug"
    val installUrl: String get() = "$appUrl/installations/new"
}

internal val officialGitHubAiIntegrations = listOf(
    GitHubAiIntegration(
        id = "codex",
        title = "Codex",
        appSlug = "chatgpt-codex-connector",
        description = "Official Codex connector for GitHub repositories.",
        finishHint = "After GitHub, finish the connection in ChatGPT / Codex settings.",
        guideUrl = "https://help.openai.com/en/articles/11145903-connecting-github-to-chatgpt",
    ),
    GitHubAiIntegration(
        id = "claude-code",
        title = "Claude Code",
        appSlug = "claude",
        description = "Official Anthropic app for pull requests, issues and Claude Code automation.",
        finishHint = "For Claude Code Actions, also run /install-github-app and complete its setup.",
        guideUrl = "https://docs.anthropic.com/en/docs/claude-code/github-actions",
    ),
)
