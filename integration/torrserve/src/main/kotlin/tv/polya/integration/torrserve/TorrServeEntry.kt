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
