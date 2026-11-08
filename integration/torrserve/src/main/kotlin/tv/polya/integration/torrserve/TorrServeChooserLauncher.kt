package tv.polya.integration.torrserve

import android.content.Context
import android.content.Intent

/**
 * Delegates playback to TorrServe MatriX.
 *
 * action=play is the native smart path: a single-file torrent starts immediately,
 * while a multi-file torrent opens TorrServe's own file/episode list.
 */
class TorrServeChooserLauncher {
    fun open(context: Context, entry: TorrServeEntry): Boolean = runCatching {
        context.startActivity(createIntent(entry))
    }.isSuccess

    internal fun createIntent(entry: TorrServeEntry): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setClassName(
                "ru.yourok.torrserve",
                "ru.yourok.torrserve.ui.activities.play.PlayActivity",
            )
            putExtra("hash", entry.hash)
            putExtra("title", entry.title)
            if (entry.poster.isNotBlank()) putExtra("poster", entry.poster)
            putExtra("action", "play")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
