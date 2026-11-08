package tv.polya.app

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import tv.polya.integration.torrserve.TorrServeRepository
import tv.polya.integration.torrserve.resolvePosterUrl

/**
 * Android TV global-search bridge.
 *
 * Queries only the local last-known-good TorrServe snapshot. ContentProvider.query() is invoked
 * synchronously while the system search dialog is typing, so it must not perform network I/O.
 */
class TorrServeSearchProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        if (URI_MATCHER.match(uri) != SEARCH_SUGGEST) {
            return MatrixCursor(DEFAULT_PROJECTION, 0)
        }

        val query = selectionArgs?.firstOrNull()?.trim().orEmpty()
            .ifBlank { uri.pathSegments.lastOrNull()?.takeUnless { it == SEARCH_PATH }.orEmpty() }
        val columns = projection?.takeIf { it.isNotEmpty() } ?: DEFAULT_PROJECTION
        if (query.isBlank()) return MatrixCursor(columns, 0)

        val appContext = context ?: return MatrixCursor(columns, 0)
        val repository = TorrServeRepository(appContext)
        val baseUrl = repository.serverUrl()
        val matches = repository.cachedEntries()
            .asSequence()
            .filter { it.title.contains(query, ignoreCase = true) }
            .sortedBy { it.title.lowercase() }
            .take(MAX_RESULTS)
            .toList()

        val cursor = MatrixCursor(columns, matches.size)
        matches.forEachIndexed { index, entry ->
            val row = mapOf<String, Any?>(
                BaseColumns._ID to entry.hash.hashCode().toLong().let { if (it == 0L) index.toLong() + 1 else it },
                SearchManager.SUGGEST_COLUMN_TEXT_1 to entry.title,
                SearchManager.SUGGEST_COLUMN_TEXT_2 to "TorrServe",
                SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE to resolvePosterUrl(baseUrl, entry.poster),
                SearchManager.SUGGEST_COLUMN_CONTENT_TYPE to "video/mp4",
                SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR to 0,
                SearchManager.SUGGEST_COLUMN_DURATION to 0L,
                SearchManager.SUGGEST_COLUMN_INTENT_ACTION to Intent.ACTION_VIEW,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID to entry.hash,
                SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA to "torrserve",
            )
            cursor.addRow(columns.map { row[it] })
        }
        return cursor
    }

    override fun getType(uri: Uri): String = SearchManager.SUGGEST_MIME_TYPE

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Insert is not supported")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Delete is not supported")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("Update is not supported")

    private companion object {
        const val SEARCH_SUGGEST = 1
        const val SEARCH_PATH = "search_suggest_query"
        const val MAX_RESULTS = 20

        val DEFAULT_PROJECTION = arrayOf(
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE,
            SearchManager.SUGGEST_COLUMN_CONTENT_TYPE,
            SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
            SearchManager.SUGGEST_COLUMN_DURATION,
            SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
        )

        val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, SEARCH_PATH, SEARCH_SUGGEST)
            addURI(AUTHORITY, "$SEARCH_PATH/*", SEARCH_SUGGEST)
        }

        const val AUTHORITY = "tv.polya.app.search"
    }
}
