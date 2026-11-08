package tv.polya.app

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import tv.polya.integration.torrserve.TorrServeChooserLauncher
import tv.polya.integration.torrserve.TorrServeRepository

/**
 * Receives selections from Android TV's system search provider.
 * A result click opens TorrServe's native file/episode chooser, not direct playback.
 */
class SystemSearchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            Intent.ACTION_VIEW -> openTorrServeSuggestion()
            Intent.ACTION_SEARCH -> openInAppSearch()
            else -> Unit
        }
        finish()
    }

    private fun openTorrServeSuggestion() {
        val hash = intent.data?.lastPathSegment
            ?: intent.getStringExtra(SearchManager.EXTRA_DATA_KEY)
        val entry = hash?.let { requested ->
            TorrServeRepository(applicationContext).cachedEntries()
                .firstOrNull { it.hash.equals(requested, ignoreCase = true) }
        }

        if (entry == null || !TorrServeChooserLauncher().open(this, entry)) {
            Toast.makeText(
                this,
                "Не удалось открыть раздачу в TorrServe. Обновите медиатеку PolyaTV.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun openInAppSearch() {
        val query = intent.getStringExtra(SearchManager.QUERY)?.trim().orEmpty()
        if (query.isBlank()) return
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_SYSTEM_SEARCH_QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }
}
