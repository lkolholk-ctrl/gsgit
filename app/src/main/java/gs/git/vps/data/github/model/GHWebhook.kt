package gs.git.vps.data.github.model

/**
 * Вебхук репозитория/организации GitHub. Модели вынесены из god-файла GitHubManager.kt
 * в рамках декомпозиции data-слоя (домен Webhooks, см. docs/decomposition-log.md).
 */
data class GHWebhook(
    val id: Long,
    val name: String,
    val url: String,
    val events: List<String>,
    val active: Boolean,
    val contentType: String = "json",
    val insecureSsl: String = "0",
    val createdAt: String = "",
    val updatedAt: String = "",
    val lastResponseCode: Int = 0,
    val lastResponseStatus: String = "",
    val lastResponseMessage: String = ""
)

data class GHWebhookConfig(
    val url: String,
    val contentType: String,
    val insecureSsl: String,
    val secret: String = ""
)

data class GHWebhookDelivery(
    val id: Long,
    val guid: String,
    val event: String,
    val action: String,
    val deliveredAt: String,
    val duration: Double,
    val status: String,
    val statusCode: Int,
    val redelivery: Boolean,
    val requestHeaders: List<Pair<String, String>> = emptyList(),
    val requestPayload: String = "",
    val responseHeaders: List<Pair<String, String>> = emptyList(),
    val responsePayload: String = ""
)
