package tv.polya.integration.torrserve

import android.content.Context
import android.content.Intent

/**
 * Opens TorrServe's native chooser. action=play is deliberately not used because it skips
 * the file/episode selection screen and starts playback immediately.
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
            putExtra("action", "choose")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
