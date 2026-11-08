package tv.polya.integration.torrserve

data class TorrServeEntry(
    val hash: String,
    val title: String,
    val poster: String = "",
    val timestamp: Long = 0L,
    val lastViewedAt: Long = 0L,
    val missingPolls: Int = 0,
)

data class TorrServeRefreshResult(
    val entries: List<TorrServeEntry>,
    val fromCache: Boolean,
    val message: String,
    val history: List<TorrServeHistoryEntry> = emptyList(),
)

/** Retain missing cards for two successful empty polls; failed HTTP requests never age the cache. */
fun mergeTorrServeEntries(
    previous: List<TorrServeEntry>,
    incoming: List<TorrServeEntry>,
    maxItems: Int = 40,
    missingPollLimit: Int = 3,
): List<TorrServeEntry> {
    val incomingHashes = incoming.mapTo(HashSet()) { it.hash.lowercase() }
    val retained = previous
        .filter { it.hash.lowercase() !in incomingHashes && it.missingPolls + 1 < missingPollLimit }
        .map { it.copy(missingPolls = it.missingPolls + 1) }
    return (incoming + retained)
        .distinctBy { it.hash.lowercase() }
        .sortedByDescending { maxOf(it.lastViewedAt, it.timestamp) }
        .take(maxItems.coerceIn(1, 100))
}

/** Resolve a MatriX poster reference against the configured server URL. */
fun resolvePosterUrl(serverUrl: String, posterReference: String?): String? {
    val reference = posterReference?.trim().orEmpty()
    if (reference.isBlank()) return null
    if (reference.startsWith("https://", ignoreCase = true) ||
        reference.startsWith("http://", ignoreCase = true)
    ) return reference

    val base = serverUrl.trim().trimEnd('/')
    if (base.isBlank()) return null
    return "$base/${reference.trimStart('/')}"
}


/** A watched file marker returned by POST /viewed. TorrServer file indexes start at 1. */
data class TorrServeViewedMark(
    val hash: String,
    val fileIndex: Int,
    val timecode: Double,
)

data class TorrServeViewedState(
    val timecode: Double,
    /** Time when PolyaTV first noticed this mark or observed its timecode change. */
    val lastObservedAt: Long,
)

data class TorrServeHistoryEntry(
    val hash: String,
    val fileIndices: List<Int>,
    val timecodes: Map<Int, Double>,
    val lastObservedAt: Long,
)

/**
 * The Viewed API does not provide a wall-clock watch timestamp. We store the first observation
 * locally, and refresh it only when a tracked timecode changes materially. This is an ordering
 * hint, not a claim of exact playback time.
 */
fun mergeTorrServeViewedState(
    previous: Map<String, TorrServeViewedState>,
    marks: List<TorrServeViewedMark>,
    now: Long,
): Map<String, TorrServeViewedState> {
    val next = LinkedHashMap<String, TorrServeViewedState>()
    marks.forEach { mark ->
        val hash = mark.hash.trim().lowercase()
        if (hash.isBlank() || mark.fileIndex < 1) return@forEach
        val key = "$hash:${mark.fileIndex}"
        val old = previous[key]
        val lastObservedAt = when {
            old == null -> now
            kotlin.math.abs(mark.timecode - old.timecode) > 0.5 -> now
            old.lastObservedAt <= 0L -> now
            else -> old.lastObservedAt
        }
        next[key] = TorrServeViewedState(
            timecode = mark.timecode.coerceAtLeast(0.0),
            lastObservedAt = lastObservedAt,
        )
    }
    return next
}

fun buildTorrServeHistory(
    marks: List<TorrServeViewedMark>,
    states: Map<String, TorrServeViewedState>,
): List<TorrServeHistoryEntry> {
    return marks
        .asSequence()
        .map { it.copy(hash = it.hash.trim().lowercase()) }
        .filter { it.hash.isNotBlank() && it.fileIndex >= 1 }
        .groupBy { it.hash }
        .map { (hash, torrentMarks) ->
            val unique = torrentMarks.distinctBy { it.fileIndex }
            val timecodes = unique.associate { it.fileIndex to it.timecode }
            val observedAt = unique.maxOfOrNull { mark ->
                states["$hash:${mark.fileIndex}"]?.lastObservedAt ?: 0L
            } ?: 0L
            TorrServeHistoryEntry(
                hash = hash,
                fileIndices = unique.map { it.fileIndex }.sorted(),
                timecodes = timecodes,
                lastObservedAt = observedAt,
            )
        }
        .sortedByDescending { it.lastObservedAt }
}


/** One external index result returned by the MatriX /search endpoint. */

/** Deduplicate tracker search matches while preserving the provider result order. */
fun deduplicateTorrServeSearchResults(
    results: List<tv.polya.core.model.MediaSearchResult>,
): List<TorrServeSearchResult> = results
    .asSequence()
    .filter { it.title.isNotBlank() && it.magnetUri.isNotBlank() }
    .distinctBy { it.providerContentId.trim().lowercase().ifBlank { it.magnetUri.trim() } }
    .toList()
