package tv.polya.integration.num

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.tv.TvContract
import android.net.Uri
import android.provider.BaseColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tv.polya.core.model.MediaSearchResult

/**
 * Reads NUM's public Android TV preview-program rows.
 *
 * This deliberately does not access NUM's private database or internal activities by guessed
 * names. Only rows that Android exposes to other apps, owned by ru.yourok.num and marked
 * searchable, are returned. The permission is optional and must be granted by the user.
 */
data class NumCatalogSearchResult(val items: List<MediaSearchResult>, val status: String)

class NumTvCatalogAdapter(context: Context) {
    private val appContext = context.applicationContext

    fun hasReadPermission(): Boolean =
        appContext.checkSelfPermission(READ_TV_LISTINGS) == PackageManager.PERMISSION_GRANTED

    suspend fun search(query: String): List<MediaSearchResult> = searchWithStatus(query).items

    suspend fun searchWithStatus(query: String): NumCatalogSearchResult = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.isBlank()) return@withContext NumCatalogSearchResult(emptyList(), "NUM: введите запрос")
        if (!hasReadPermission()) return@withContext NumCatalogSearchResult(emptyList(), "NUM: нет разрешения на чтение рекомендаций Android TV")

        val cursor = runCatching {
            appContext.contentResolver.query(TvContract.PreviewPrograms.CONTENT_URI, PROJECTION, null, null, null)
        }.getOrNull() ?: return@withContext NumCatalogSearchResult(emptyList(), "NUM: системный каталог рекомендаций недоступен")

        cursor.use { result ->
            val packageColumn = result.getColumnIndex(TvContract.BaseTvColumns.COLUMN_PACKAGE_NAME)
            val searchableColumn = result.getColumnIndex(TvContract.PreviewPrograms.COLUMN_SEARCHABLE)
            val titleColumn = result.getColumnIndex(TvContract.PreviewPrograms.COLUMN_TITLE)
            val descriptionColumn = result.getColumnIndex(TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION)
            val posterColumn = result.getColumnIndex(TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI)
            val thumbnailColumn = result.getColumnIndex(TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI)
            val intentColumn = result.getColumnIndex(TvContract.PreviewPrograms.COLUMN_INTENT_URI)
            val contentIdColumn = result.getColumnIndex(TvContract.PreviewPrograms.COLUMN_CONTENT_ID)

            if (packageColumn < 0 || searchableColumn < 0 || titleColumn < 0 || intentColumn < 0) {
                return@use NumCatalogSearchResult(emptyList(), "NUM: системный каталог не вернул обязательные поля")
            }

            var numRows = 0
            var searchableRows = 0
            val unique = LinkedHashMap<String, MediaSearchResult>()
            while (result.moveToNext()) {
                if (result.getString(packageColumn) != NUM_PACKAGE) continue
                numRows++
                if (result.getInt(searchableColumn) != 1) continue
                searchableRows++
                val title = result.getString(titleColumn).orEmpty().trim()
                if (title.isBlank() || !title.contains(normalized, ignoreCase = true)) continue

                val intentUri = result.getString(intentColumn).orEmpty().trim()
                if (!isNumLaunchUri(intentUri)) continue
                val rowId = result.getLong(result.getColumnIndexOrThrow(BaseColumns._ID))
                val contentId = if (contentIdColumn >= 0) result.getString(contentIdColumn).orEmpty() else ""
                val description = if (descriptionColumn >= 0) result.getString(descriptionColumn).orEmpty() else ""
                val poster = (if (posterColumn >= 0) result.getString(posterColumn) else null)
                    ?.takeIf { it.isNotBlank() }
                    ?: (if (thumbnailColumn >= 0) result.getString(thumbnailColumn) else null)

                val item = NumPreviewProgram(rowId, contentId, title, description, poster, intentUri, true)
                    .toMediaSearchResult() ?: continue
                unique.putIfAbsent(item.providerContentId, item)
            }

            val items = unique.values.take(MAX_RESULTS)
            val message = when {
                numRows == 0 -> "NUM: карточек нет в системном каталоге. Откройте NUM для обновления рекомендаций."
                searchableRows == 0 -> "NUM: системный каталог есть, но карточки не опубликованы для поиска."
                items.isEmpty() -> "NUM: опубликовано карточек: $searchableRows, совпадений по запросу нет."
                else -> "NUM: найдено ${items.size} из ${searchableRows} опубликованных карточек."
            }
            NumCatalogSearchResult(items, message)
        }
    }

    fun open(context: Context, result: MediaSearchResult): Boolean {
        if (result.sourceId != SOURCE_ID) return false
        val rawUri = result.sourceLaunchUri?.trim().orEmpty()
        if (!isNumLaunchUri(rawUri)) return false

        return runCatching {
            val intent = Intent.parseUri(rawUri, Intent.URI_INTENT_SCHEME)
            val resolved = intent.resolveActivity(context.packageManager) ?: return false
            if (resolved.packageName != NUM_PACKAGE) return false

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }.isSuccess
    }

    private fun isNumLaunchUri(value: String): Boolean {
        if (value.isBlank()) return false
        return runCatching {
            val intent = Intent.parseUri(value, Intent.URI_INTENT_SCHEME)
            val resolved = intent.resolveActivity(appContext.packageManager)
            resolved?.packageName == NUM_PACKAGE
        }.getOrDefault(false)
    }

    private companion object {
        const val SOURCE_ID = "num"
        const val NUM_PACKAGE = "ru.yourok.num"
        const val READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS"
        const val MAX_RESULTS = 40

        val PROJECTION = arrayOf(
            BaseColumns._ID,
            TvContract.BaseTvColumns.COLUMN_PACKAGE_NAME,
            TvContract.PreviewPrograms.COLUMN_SEARCHABLE,
            TvContract.PreviewPrograms.COLUMN_TITLE,
            TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION,
            TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI,
            TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI,
            TvContract.PreviewPrograms.COLUMN_INTENT_URI,
            TvContract.PreviewPrograms.COLUMN_CONTENT_ID,
        )
    }
}
