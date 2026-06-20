package gs.git.vps.data.github.model

/** Тег репозитория. Домен Repos. */
data class GHTag(
    val name: String,
    val zipballUrl: String,
    val tarballUrl: String,
    val commitSha: String,
    val commitUrl: String
)

/** Deploy-ключ репозитория. Домен Repos. */
data class GHDeployKey(
    val id: Long,
    val title: String,
    val key: String,
    val verified: Boolean,
    val readOnly: Boolean,
    val createdAt: String,
    val addedBy: String,
    val lastUsed: String,
    val enabled: Boolean
)

/** Полные настройки репозитория (general + features + merge). Домен Repos. */
data class GHRepoSettings(
    val name: String,
    val description: String,
    val homepage: String,
    val isPrivate: Boolean,
    val hasIssues: Boolean,
    val hasProjects: Boolean,
    val hasWiki: Boolean,
    val hasDiscussions: Boolean,
    val allowForking: Boolean,
    val isTemplate: Boolean,
    val archived: Boolean,
    val disabled: Boolean,
    val defaultBranch: String,
    val topics: List<String>,
    val allowMergeCommit: Boolean,
    val allowSquashMerge: Boolean,
    val allowRebaseMerge: Boolean,
    val deleteBranchOnMerge: Boolean
)

/** Приглашение коллаборатора в конкретный репозиторий. Домен Repos. */
data class GHRepoInvitation(
    val id: Long,
    val invitee: String,
    val inviter: String,
    val permissions: String,
    val createdAt: String,
    val expired: Boolean,
    val htmlUrl: String
)

/** Приглашение текущего пользователя в чужой репозиторий. Домен Repos. */
data class GHUserRepositoryInvitation(
    val id: Long,
    val repository: GHRepo?,
    val repoFullName: String,
    val inviter: String,
    val inviterAvatarUrl: String,
    val permissions: String,
    val createdAt: String,
    val expired: Boolean
)

/** Запись лимита взаимодействий репозитория. Домен Repos. */
data class GHInteractionLimitEntry(val limit: String, val expiry: String?)
