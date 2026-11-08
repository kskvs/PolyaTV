package tv.polya.integration.num

import tv.polya.core.model.MediaSearchResult

data class NumPreviewProgram(
    val rowId: Long,
    val contentId: String,
    val title: String,
    val description: String,
    val posterUri: String?,
    val intentUri: String,
    val searchable: Boolean,
)

fun NumPreviewProgram.toMediaSearchResult(): MediaSearchResult? {
    if (!searchable) return null
    val cleanTitle = title.trim()
    val stableId = contentId.trim().ifBlank { rowId.takeIf { it > 0L }?.toString().orEmpty() }
    if (cleanTitle.isBlank() || stableId.isBlank() || intentUri.isBlank()) return null

    return MediaSearchResult(
        title = cleanTitle,
        providerName = "NUM",
        providerLabel = "Рекомендация · NUM",
        year = Regex("""\b(?:19|20)\d{2}\b""").find(description)?.value?.toIntOrNull() ?: 0,
        sizeLabel = "",
        seedCount = 0,
        magnetUri = "",
        providerContentId = stableId,
        sourceId = "num",
        posterUrl = posterUri?.trim()?.takeIf {
            it.startsWith("https://", true) || it.startsWith("http://", true)
        },
        description = description.trim().takeIf { it.isNotBlank() },
        launchType = "tv_provider",
        sourceLaunchUri = intentUri,
    )
}
