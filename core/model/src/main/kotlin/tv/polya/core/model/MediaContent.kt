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


/** Provider-neutral search match. Adapters keep their original launch reference separately. */
data class MediaSearchResult(
    val title: String,
    val providerName: String,
    val providerLabel: String,
    val year: Int,
    val sizeLabel: String,
    val seedCount: Int,
    val magnetUri: String,
    val providerContentId: String,
)
