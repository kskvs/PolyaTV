package tv.polya.core.model

/** Source-neutral content. Provider IDs remain available after feed normalization/deduplication. */
data class MediaContent(
    val id: String,
    val title: String,
    val sourceId: String,
    val sourceContentId: String,
    val posterUrl: String? = null,
    val year: Int? = null,
    val contentType: ContentType = ContentType.UNKNOWN,
)

enum class ContentType { MOVIE, SERIES, EPISODE, UNKNOWN }

/**
 * Search suggestion from a content source.
 * TorrServe items carry a magnet and hash; catalogue items can carry a poster and a source launch type.
 */
data class MediaSearchResult(
    val title: String,
    val providerName: String,
    val providerLabel: String,
    val year: Int,
    val sizeLabel: String,
    val seedCount: Int,
    val magnetUri: String,
    val providerContentId: String,
    val sourceId: String = "torrserve",
    val posterUrl: String? = null,
    val description: String? = null,
    val launchType: String = "",
    val sourceLaunchUri: String? = null,
)

/** Deduplicate within a provider without collapsing distinct source launch paths. */
fun deduplicateMediaSearchResults(results: List<MediaSearchResult>): List<MediaSearchResult> =
    results.asSequence()
        .filter {
            it.title.isNotBlank() &&
                if (it.sourceId == "torrserve") {
                    it.magnetUri.isNotBlank()
                } else {
                    it.providerContentId.isNotBlank()
                }
        }
        .distinctBy {
            val stableId = it.providerContentId.trim().lowercase()
                .ifBlank { it.magnetUri.trim().lowercase() }
            "${it.sourceId.lowercase()}:$stableId"
        }
        .toList()
