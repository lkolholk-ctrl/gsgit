package gs.git.vps.data.github.model

/**
 * Модели домена Orgs слоя GitHub API: организации, членство, а также org-админ-фичи
 * (audit-log, SCIM-пользователи, SAML credential-authorizations). Вынесены из god-файла
 * GitHubManager.kt в рамках декомпозиции data-слоя (см. docs/decomposition-log.md).
 */

data class GHOrg(val login: String, val avatarUrl: String, val description: String)

data class GHOrgMembership(val org: String, val state: String, val role: String, val url: String)

/** Запись org audit-log. */
data class GHAuditLogEntry(
    val id: String,
    val action: String,
    val actor: String,
    val createdAt: String,
    val org: String,
    val repo: String,
    val user: String,
    val operationType: String,
    val transportProtocol: String
)

/** Страница SCIM-пользователей провизионинга организации. */
data class GHScimUsersPage(
    val totalResults: Int = 0,
    val startIndex: Int = 1,
    val itemsPerPage: Int = 0,
    val users: List<GHScimUser> = emptyList(),
    val error: String = ""
)

data class GHScimUser(
    val id: String,
    val userName: String,
    val displayName: String,
    val givenName: String,
    val familyName: String,
    val active: Boolean,
    val externalId: String,
    val emails: List<String>
)

/** Авторизация SAML-credential участника организации. */
data class GHSamlAuthorization(
    val login: String,
    val credentialId: Long,
    val credentialType: String,
    val tokenLastEight: String,
    val authorizedAt: String,
    val accessedAt: String,
    val expiresAt: String,
    val scopes: List<String>
)
