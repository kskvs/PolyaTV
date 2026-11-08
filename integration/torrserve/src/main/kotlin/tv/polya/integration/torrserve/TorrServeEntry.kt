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
    val next = LinkedHashMap(previous)
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


/** Keep previously watched torrents in the local history even if MatriX later prunes Viewed marks. */
fun mergeTorrServeHistory(
    previous: List<TorrServeHistoryEntry>,
    incoming: List<TorrServeHistoryEntry>,
    maxItems: Int = 100,
): List<TorrServeHistoryEntry> {
    val merged = LinkedHashMap<String, TorrServeHistoryEntry>()
    previous.forEach { old -> merged[old.hash.lowercase()] = old.copy(hash = old.hash.lowercase()) }
    incoming.forEach { fresh ->
        val hash = fresh.hash.lowercase()
        val old = merged[hash]
        if (old == null) {
            merged[hash] = fresh.copy(hash = hash)
        } else {
            val indices = (old.fileIndices + fresh.fileIndices).distinct().sorted()
            val times = LinkedHashMap(old.timecodes)
            times.putAll(fresh.timecodes)
            merged[hash] = TorrServeHistoryEntry(
                hash = hash,
                fileIndices = indices,
                timecodes = times,
                lastObservedAt = maxOf(old.lastObservedAt, fresh.lastObservedAt),
            )
        }
    }
    return merged.values
        .sortedByDescending { it.lastObservedAt }
        .take(maxItems.coerceIn(1, 500))
}
