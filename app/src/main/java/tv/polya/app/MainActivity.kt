package tv.polya.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import tv.polya.core.model.ContentType
import tv.polya.core.model.MediaContent
import tv.polya.feature.home.PolyaHomeScreen
import tv.polya.integration.torrserve.TorrServeChooserLauncher
import tv.polya.integration.torrserve.TorrServeEntry
import tv.polya.integration.torrserve.TorrServeRepository
import tv.polya.integration.torrserve.resolvePosterUrl

class MainActivity : ComponentActivity() {
    private lateinit var torrServe: TorrServeRepository
    private val serverUrl = mutableStateOf("")
    private val items = mutableStateOf<List<MediaContent>>(emptyList())
    private val status = mutableStateOf("Введите адрес TorrServe в настройках и проверьте соединение.")
    private val isRefreshing = mutableStateOf(false)
    private val chooserLauncher = TorrServeChooserLauncher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        torrServe = TorrServeRepository(applicationContext)
        serverUrl.value = torrServe.serverUrl()
        items.value = torrServe.cachedEntries().map(TorrServeEntry::toMediaContent)

        setContent {
            PolyaHomeScreen(
                items = items.value,
                serverUrl = serverUrl.value,
                connectionMessage = status.value,
                isRefreshing = isRefreshing.value,
                onServerUrlChange = { serverUrl.value = it },
                onSaveAndRefresh = {
                    torrServe.saveServerUrl(serverUrl.value)
                    refreshTorrServe()
                },
                onRefresh = ::refreshTorrServe,
                onItemSelected = { item ->
                    val entry = torrServe.cachedEntries().firstOrNull { it.hash == item.sourceContentId }
                    if (entry == null || !chooserLauncher.open(this, entry)) {
                        Toast.makeText(
                            this,
                            "Не удалось открыть штатное меню TorrServe. Проверьте установку MatriX.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
            )
        }
        if (serverUrl.value.isNotBlank()) refreshTorrServe()
    }

    private fun refreshTorrServe() {
        if (isRefreshing.value) return
        lifecycleScope.launch {
            isRefreshing.value = true
            try {
                val result = torrServe.refresh()
                items.value = result.entries.map(TorrServeEntry::toMediaContent)
                status.value = result.message
            } finally {
                isRefreshing.value = false
            }
        }
    }
}

private fun TorrServeEntry.toMediaContent() = MediaContent(
    id = "torrserve:$hash",
    title = title,
    sourceId = "torrserve",
    sourceContentId = hash,
    posterUrl = resolvePosterUrl(serverUrl, poster),
    contentType = ContentType.UNKNOWN,
)
