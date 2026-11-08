package tv.polya.integration.lampa

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.BaseColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tv.polya.core.model.MediaSearchResult

/**
 * Reads Lampa's public Android TV searchable ContentProvider.
 *
 * The result opens a TMDB card inside Lampa; it does not contain a TorrServe hash or magnet.
 * The user continues through Lampa's normal catalogue/playback flow, which can hand the chosen
 * torrent to TorrServe. We deliberately do not invent a direct chooser payload for this source.
 */
class LampaSearchAdapter(context: Context) {
    private val appContext = context.applicationContext

    fun isAvailable(): Boolean = runCatching {
        appContext.packageManager.resolveContentProvider(AUTHORITY, 0) != null
    }.getOrDefault(false)

    suspend fun search(query: String): List<MediaSearchResult> = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.isBlank() || !isAvailable()) return@withContext emptyList()

        val cursor = appContext.contentResolver.query(
            SEARCH_URI,
            PROJECTION,
            " ? ",
            arrayOf(normalized),
            null,
        ) ?: return@withContext emptyList()

        cursor.use { resultCursor ->
            val idColumn = resultCursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID)
            val typeColumn = resultCursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA)
            val titleColumn = resultCursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
            val descriptionColumn = resultCursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_2)
            val posterColumn = resultCursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE)

            if (idColumn < 0 || typeColumn < 0 || titleColumn < 0) {
                return@withContext emptyList()
            }

            buildList {
                while (resultCursor.moveToNext()) {
                    val suggestion = LampaSearchSuggestion(
                        tmdbId = resultCursor.getString(idColumn).orEmpty(),
                        mediaType = resultCursor.getString(typeColumn).orEmpty(),
                        title = resultCursor.getString(titleColumn).orEmpty(),
                        description = if (descriptionColumn >= 0) resultCursor.getString(descriptionColumn).orEmpty() else "",
                        posterUrl = if (posterColumn >= 0) resultCursor.getString(posterColumn) else null,
                    )
                    suggestion.toMediaSearchResult()?.let(::add)
                }
            }
        }
    }

    fun open(context: Context, result: MediaSearchResult): Boolean {
        if (result.sourceId != SOURCE_ID) return false
        val id = result.providerContentId.toLongOrNull()?.takeIf { it > 0 } ?: return false
        val type = result.launchType.takeIf { it == "movie" || it == "tv" } ?: return false

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("content://$AUTHORITY/video/$id")).apply {
            setClassName(LAMPA_PACKAGE, LAMPA_ACTIVITY)
            action = GLOBAL_SEARCH_ACTION
            putExtra(SearchManager.EXTRA_DATA_KEY, type)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    private companion object {
        const val SOURCE_ID = "lampa"
        const val LAMPA_PACKAGE = "top.rootu.lampa"
        const val LAMPA_ACTIVITY = "top.rootu.lampa.MainActivity"
        const val AUTHORITY = "top.rootu.lampa.atvsearch"
        const val GLOBAL_SEARCH_ACTION = "GLOBALSEARCH"

        val SEARCH_URI: Uri = Uri.parse(
            "content://$AUTHORITY/search/${SearchManager.SUGGEST_URI_PATH_QUERY}",
        )

        val PROJECTION = arrayOf(
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
        )
    }
}
