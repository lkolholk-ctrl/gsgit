package gs.git.vps.data.github.model

/**
 * Модели домена Auth слоя GitHub API: OAuth-app токен-инфо и OAuth device-flow. Вынесены из
 * god-файла GitHubManager.kt (см. docs/decomposition-log.md).
 */

data class GHOAuthTokenInfo(
    val id: Long,
    val url: String,
    val appName: String,
    val appUrl: String,
    val clientId: String,
    val tokenLastEight: String,
    val note: String,
    val noteUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val scopes: List<String>,
    val fingerprint: String,
    val token: String = ""
)

data class GHDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int
)

data class GHDeviceTokenResult(
    val token: String?,
    val error: String?
)
