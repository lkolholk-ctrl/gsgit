package gs.git.vps.data.github.model

// Модели домена Projects (classic Projects + Projects V2).
// Вынесены из GitHubManager.kt по эталону декомпозиции (см. docs/decomposition-log.md).

data class GHProject(
    val id: Long,
    val nodeId: String,
    val name: String,
    val body: String,
    val state: String,
    val number: Int,
    val columnsUrl: String,
    val htmlUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val creator: String
)

data class GHProjectColumn(
    val id: Long,
    val nodeId: String,
    val name: String,
    val cardsUrl: String,
    val createdAt: String,
    val updatedAt: String
)

data class GHProjectCard(
    val id: Long,
    val nodeId: String,
    val note: String,
    val creator: String,
    val contentUrl: String,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val columnUrl: String
)

data class GHProjectV2(
    val id: String,
    val number: Int,
    val title: String,
    val shortDescription: String,
    val url: String,
    val closed: Boolean,
    val isPublic: Boolean,
    val updatedAt: String,
    val itemsCount: Int
)

data class GHProjectV2Detail(
    val id: String,
    val number: Int,
    val title: String,
    val shortDescription: String,
    val readme: String,
    val url: String,
    val closed: Boolean,
    val isPublic: Boolean,
    val updatedAt: String,
    val itemsCount: Int,
    val fields: List<GHProjectV2Field>,
    val items: List<GHProjectV2Item>,
    val views: List<GHProjectV2View> = emptyList(),
    val workflows: List<GHProjectV2Workflow> = emptyList()
)

data class GHProjectV2Field(
    val id: String,
    val name: String,
    val dataType: String,
    val options: List<GHProjectV2FieldOption> = emptyList()
)

data class GHProjectV2FieldOption(
    val id: String,
    val name: String,
    val color: String = "",
    val description: String = ""
)

data class GHProjectV2View(
    val id: String,
    val number: Int,
    val name: String,
    val layout: String,
    val filter: String,
    val updatedAt: String,
    val fields: List<String>
)

data class GHProjectV2Workflow(
    val id: String,
    val number: Int,
    val name: String,
    val enabled: Boolean,
    val updatedAt: String
)

data class GHProjectV2Item(
    val id: String,
    val type: String,
    val contentId: String,
    val contentType: String,
    val title: String,
    val body: String,
    val number: Int,
    val state: String,
    val url: String,
    val creator: String,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val fieldValues: List<GHProjectV2ItemFieldValue>
)

data class GHProjectV2ItemFieldValue(
    val fieldId: String,
    val fieldName: String,
    val dataType: String,
    val value: String,
    val optionId: String = "",
    val iterationId: String = ""
)
