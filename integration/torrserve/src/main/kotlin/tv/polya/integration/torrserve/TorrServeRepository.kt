package tv.polya.integration.torrserve

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** MatriX HTTP adapter with last-known-good list and viewed-history snapshots. */
class TorrServeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences: SharedPreferences =
        appContext.getSharedPreferences("polya_torrserve", Context.MODE_PRIVATE)
    private val cacheFile = File(appContext.filesDir, "torrserve-snapshot.json")
    private val viewedStateFile = File(appContext.filesDir, "torrserve-viewed-state.json")
    private val historyFile = File(appContext.filesDir, "torrserve-history.json")

    fun serverUrl(): String = preferences.getString(KEY_SERVER_URL, "").orEmpty()

    fun saveServerUrl(value: String) {
        preferences.edit().putString(KEY_SERVER_URL, value.trim().trimEnd('/')).apply()
    }

    fun cachedEntries(): List<TorrServeEntry> = runCatching {
        if (!cacheFile.exists()) emptyList() else parseEntries(cacheFile.readText())
    }.getOrDefault(emptyList())

    fun cachedHistory(): List<TorrServeHistoryEntry> = runCatching {
        if (!historyFile.exists()) emptyList() else parseHistory(historyFile.readText())
    }.getOrDefault(emptyList())

    suspend fun refresh(): TorrServeRefreshResult = withContext(Dispatchers.IO) {
        val base = serverUrl().trim().trimEnd('/')
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            return@withContext TorrServeRefreshResult(
                cachedEntries(), true,
                "Укажите адрес TorrServe, например http://192.168.1.10:8090",
                cachedHistory(),
            )
        }

        try {
            val incoming = parseEntries(postJson(base, "/torrents", """{"action":"list"}"""))
            val merged = mergeTorrServeEntries(cachedEntries(), incoming)

            // History is optional: old servers or disabled endpoints must not blank the catalogue.
            val freshMarks = runCatching {
                parseViewedMarks(postJson(base, "/viewed", """{"action":"list","hash":""}"""))
            }.getOrNull()
            val history = if (freshMarks != null) {
                val state = mergeTorrServeViewedState(
                    previous = readViewedState(),
                    marks = freshMarks,
                    now = System.currentTimeMillis(),
                )
                saveViewedState(state)
                buildTorrServeHistory(freshMarks, state).also(::saveHistory)
            } else {
                cachedHistory()
            }

            val viewedAt = history.associate { it.hash to it.lastObservedAt }
            val ordered = merged
                .map { entry ->
                    entry.copy(lastViewedAt = maxOf(entry.lastViewedAt, viewedAt[entry.hash] ?: 0L))
                }
                .sortedByDescending { maxOf(it.lastViewedAt, it.timestamp) }
            saveSnapshot(ordered)

            val historyStatus = if (freshMarks == null) {
                " · история не ответила, оставлен локальный кэш"
            } else {
                " · просмотренных торрентов: ${history.size}"
            }
            TorrServeRefreshResult(
                entries = ordered,
                fromCache = false,
                message = "Связь с TorrServe установлена · элементов: ${incoming.size}$historyStatus",
                history = history,
            )
        } catch (error: Exception) {
            TorrServeRefreshResult(
                entries = cachedEntries(),
                fromCache = true,
                message = "TorrServe недоступен: ${error.message ?: error.javaClass.simpleName}. Показан последний сохранённый список.",
                history = cachedHistory(),
            )
        }
    }

    /** Search TorrServe's own index. Results are not added until the user selects one. */
    suspend fun searchTorrents(query: String): List<TorrServeSearchResult> = withContext(Dispatchers.IO) {
        val base = serverUrl().trim().trimEnd('/')
        val normalized = query.trim()
        if (normalized.isBlank()) return@withContext emptyList()
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            throw IllegalStateException("Сначала укажите адрес TorrServe в настройках")
        }

        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name())
        val connection = (URL("$base/search?query=$encoded").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
        }
        val response = try {
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
        parseSearchResults(response)
    }

    /**
     * A result from /search is only a tracker magnet. Add it to the local TorrServe server first,
     * then return its actual server hash so the caller can open the native file chooser.
     */
    suspend fun addSearchResult(result: TorrServeSearchResult): TorrServeEntry = withContext(Dispatchers.IO) {
        val base = serverUrl().trim().trimEnd('/')
        val existing = cachedEntries().firstOrNull {
            (result.hash.isNotBlank() && it.hash.equals(result.hash, ignoreCase = true))
        }
        if (existing != null) return@withContext existing

        val magnet = result.magnet.trim()
        if (magnet.isBlank()) {
            throw IllegalStateException("В результате поиска нет magnet-ссылки")
        }

        val request = JSONObject()
            .put("action", "add")
            .put("link", magnet)
            .put("title", result.title)
            .put("poster", "")
            .put("category", "")
            .put("data", "")
            .put("save_to_db", true)

        val response = JSONObject(postJson(base, "/torrents", request.toString()))
        val hash = response.optString("hash").trim().ifBlank { result.hash.trim().lowercase() }
        if (hash.isBlank()) {
            throw IllegalStateException("TorrServe добавил результат без hash в ответе")
        }

        val added = TorrServeEntry(
            hash = hash.lowercase(),
            title = response.optString("title").trim().ifBlank { result.title },
            poster = response.optString("poster").trim(),
            timestamp = response.optLong("timestamp", System.currentTimeMillis()),
        )
        val updated = (listOf(added) + cachedEntries()).distinctBy { it.hash.lowercase() }
        saveSnapshot(updated)
        added
    }

    private fun parseSearchResults(json: String): List<TorrServeSearchResult> {
        val array = JSONArray(json)
        val unique = LinkedHashMap<String, TorrServeSearchResult>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val title = item.optString("Title").trim().ifBlank { item.optString("Name").trim() }
            val magnet = item.optString("Magnet").trim()
            val hash = item.optString("Hash").trim().lowercase()
            if (title.isBlank() || magnet.isBlank()) continue
            val key = hash.ifBlank { magnet }
            unique.putIfAbsent(
                key,
                TorrServeSearchResult(
                    title = title,
                    torrentName = item.optString("Name").trim(),
                    tracker = item.optString("Tracker").trim(),
                    year = item.optInt("Year", 0),
                    size = item.optString("Size").trim(),
                    seeders = item.optInt("Seed", 0),
                    magnet = magnet,
                    hash = hash,
                ),
            )
        }
        return unique.values.toList()
    }

    private fun postJson(base: String, path: String, body: String): String {
        val connection = (URL("$base$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 4_500
            readTimeout = 6_500
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseEntries(json: String): List<TorrServeEntry> {
        val array = JSONArray(json)
        val unique = LinkedHashMap<String, TorrServeEntry>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val hash = item.optString("hash").trim().lowercase()
            val title = item.optString("title").trim().ifBlank { item.optString("name").trim() }
            if (hash.isBlank() || title.isBlank()) continue
            unique.putIfAbsent(
                hash,
                TorrServeEntry(
                    hash = hash,
                    title = title,
                    poster = item.optString("poster").trim(),
                    timestamp = item.optLong("timestamp", 0L),
                    lastViewedAt = item.optLong("lastViewedAt", item.optLong("last_viewed_at", 0L)),
                    missingPolls = item.optInt("missingPolls", item.optInt("missing_polls", 0)).coerceAtLeast(0),
                ),
            )
        }
        return unique.values.toList()
    }

    private fun parseViewedMarks(json: String): List<TorrServeViewedMark> {
        if (json.isBlank()) return emptyList()
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val hash = item.optString("hash").trim().lowercase()
                val fileIndex = item.optInt("file_index", -1)
                if (hash.isBlank() || fileIndex < 1) continue
                add(TorrServeViewedMark(hash, fileIndex, item.optDouble("timecode", 0.0)))
            }
        }
    }

    private fun readViewedState(): Map<String, TorrServeViewedState> = runCatching {
        if (!viewedStateFile.exists()) return@runCatching emptyMap()
        val json = JSONObject(viewedStateFile.readText())
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = json.optJSONObject(key) ?: continue
                val observedAt = item.optLong("lastObservedAt", 0L)
                if (observedAt > 0L) {
                    put(key, TorrServeViewedState(item.optDouble("timecode", 0.0), observedAt))
                }
            }
        }
    }.getOrDefault(emptyMap())

    private fun saveViewedState(state: Map<String, TorrServeViewedState>) {
        val json = JSONObject()
        state.forEach { (key, value) ->
            json.put(
                key,
                JSONObject()
                    .put("timecode", value.timecode)
                    .put("lastObservedAt", value.lastObservedAt),
            )
        }
        writeAtomically(viewedStateFile, json.toString())
    }

    private fun parseHistory(json: String): List<TorrServeHistoryEntry> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val hash = item.optString("hash").trim().lowercase()
                if (hash.isBlank()) continue
                val indicesArray = item.optJSONArray("fileIndices") ?: JSONArray()
                val indices = (0 until indicesArray.length())
                    .map { indicesArray.optInt(it, -1) }
                    .filter { it >= 1 }
                val timesObject = item.optJSONObject("timecodes") ?: JSONObject()
                val times = buildMap {
                    val keys = timesObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        key.toIntOrNull()?.let { put(it, timesObject.optDouble(key, 0.0)) }
                    }
                }
                add(
                    TorrServeHistoryEntry(
                        hash = hash,
                        fileIndices = indices,
                        timecodes = times,
                        lastObservedAt = item.optLong("lastObservedAt", 0L),
                    ),
                )
            }
        }.sortedByDescending { it.lastObservedAt }
    }

    private fun saveHistory(entries: List<TorrServeHistoryEntry>) {
        val json = JSONArray(entries.map { entry ->
            val timecodes = JSONObject()
            entry.timecodes.forEach { (index, timecode) -> timecodes.put(index.toString(), timecode) }
            JSONObject()
                .put("hash", entry.hash)
                .put("fileIndices", JSONArray(entry.fileIndices))
                .put("timecodes", timecodes)
                .put("lastObservedAt", entry.lastObservedAt)
        }).toString()
        writeAtomically(historyFile, json)
    }

    private fun saveSnapshot(entries: List<TorrServeEntry>) {
        val json = JSONArray(entries.map { entry ->
            JSONObject()
                .put("hash", entry.hash)
                .put("title", entry.title)
                .put("poster", entry.poster)
                .put("timestamp", entry.timestamp)
                .put("lastViewedAt", entry.lastViewedAt)
                .put("missingPolls", entry.missingPolls)
        }).toString()
        writeAtomically(cacheFile, json)
    }

    private fun writeAtomically(target: File, content: String) {
        val temporary = File(appContext.filesDir, target.nameWithoutExtension + ".tmp")
        temporary.writeText(content)
        if (!temporary.renameTo(target)) {
            target.writeText(content)
            temporary.delete()
        }
    }

    private companion object { const val KEY_SERVER_URL = "server_url" }
}
