package tv.polya.integration.lampa

import tv.polya.core.model.MediaSearchResult

data class LampaSearchSuggestion(
    val tmdbId: String,
    val mediaType: String,
    val title: String,
    val description: String,
    val posterUrl: String?,
)

fun LampaSearchSuggestion.toMediaSearchResult(): MediaSearchResult? {
    val id = tmdbId.trim().toLongOrNull()?.takeIf { it > 0 }?.toString() ?: return null
    val type = mediaType.trim().lowercase()
    if (type != "movie" && type != "tv") return null
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return null

    return MediaSearchResult(
        title = cleanTitle,
        providerName = "Lampa",
        providerLabel = if (type == "movie") "Фильм · Lampa" else "Сериал · Lampa",
        year = Regex("""\b(?:19|20)\d{2}\b""").find(description)?.value?.toIntOrNull() ?: 0,
        sizeLabel = "",
        seedCount = 0,
        magnetUri = "",
        providerContentId = id,
        sourceId = "lampa",
        posterUrl = posterUrl?.trim()?.takeIf { it.isNotBlank() },
        description = description.trim().takeIf { it.isNotBlank() },
        launchType = type,
    )
}
