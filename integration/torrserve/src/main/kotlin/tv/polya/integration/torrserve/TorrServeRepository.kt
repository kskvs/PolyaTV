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

/** MatriX HTTP adapter with a last-known-good local snapshot. */
class TorrServeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences: SharedPreferences =
        appContext.getSharedPreferences("polya_torrserve", Context.MODE_PRIVATE)
    private val cacheFile = File(appContext.filesDir, "torrserve-snapshot.json")

    fun serverUrl(): String = preferences.getString(KEY_SERVER_URL, "").orEmpty()

    fun saveServerUrl(value: String) {
        preferences.edit().putString(KEY_SERVER_URL, value.trim().trimEnd('/')).apply()
    }

    fun cachedEntries(): List<TorrServeEntry> = runCatching {
        if (!cacheFile.exists()) emptyList() else parseEntries(cacheFile.readText())
    }.getOrDefault(emptyList())

    suspend fun refresh(): TorrServeRefreshResult = withContext(Dispatchers.IO) {
        val base = serverUrl().trim().trimEnd('/')
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            return@withContext TorrServeRefreshResult(
                cachedEntries(), true,
                "Укажите адрес TorrServe, например http://192.168.1.10:8090",
            )
        }
        try {
            val incoming = parseEntries(postList(base))
            val merged = mergeTorrServeEntries(cachedEntries(), incoming)
            saveSnapshot(merged)
            TorrServeRefreshResult(
                merged, false, "Связь с TorrServe установлена · элементов: ${incoming.size}",
            )
        } catch (error: Exception) {
            TorrServeRefreshResult(
                cachedEntries(), true,
                "TorrServe недоступен: ${error.message ?: error.javaClass.simpleName}. Показан последний сохранённый список.",
            )
        }
    }

    private fun postList(base: String): String {
        val connection = (URL("$base/torrents").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 4_500
            readTimeout = 6_500
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.outputStream.use { it.write("""{"action":"list"}""".toByteArray(Charsets.UTF_8)) }
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
                ),
            )
        }
        return unique.values.toList()
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
        val temporary = File(appContext.filesDir, "torrserve-snapshot.tmp")
        temporary.writeText(json)
        if (!temporary.renameTo(cacheFile)) {
            cacheFile.writeText(json)
            temporary.delete()
        }
    }

    private companion object { const val KEY_SERVER_URL = "server_url" }
}
