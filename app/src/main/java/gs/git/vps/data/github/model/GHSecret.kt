package gs.git.vps.data.github.model

/**
 * Секреты и переменные GitHub Actions (repo + environment). Модели вынесены из god-файла
 * GitHubManager.kt в рамках декомпозиции data-слоя (домен Secrets, см. docs/decomposition-log.md).
 */
data class GHActionPublicKey(val keyId: String, val key: String)

data class GHActionSecret(val name: String, val createdAt: String, val updatedAt: String)

data class GHActionVariable(val name: String, val value: String, val createdAt: String, val updatedAt: String)

data class GHEnvironmentSecret(
    val name: String,
    val createdAt: String,
    val updatedAt: String
)
