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
