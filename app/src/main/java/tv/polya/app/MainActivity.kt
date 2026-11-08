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
import tv.polya.integration.torrserve.TorrServeHistoryEntry
import tv.polya.integration.torrserve.TorrServeRepository
import tv.polya.integration.torrserve.TorrServeSearchResult
import tv.polya.integration.torrserve.resolvePosterUrl

class MainActivity : ComponentActivity() {
    private lateinit var torrServe: TorrServeRepository
    private val serverUrl = mutableStateOf("")
    private val items = mutableStateOf<List<MediaContent>>(emptyList())
    private val historyItems = mutableStateOf<List<MediaContent>>(emptyList())
    private val searchQuery = mutableStateOf("")
    private val searchResults = mutableStateOf<List<TorrServeSearchResult>>(emptyList())
    private val searchStatus = mutableStateOf("Введите название фильма или сериала.")
    private val isSearching = mutableStateOf(false)
    private val status = mutableStateOf("Введите адрес TorrServe в настройках и проверьте соединение.")
    private val isRefreshing = mutableStateOf(false)
    private val chooserLauncher = TorrServeChooserLauncher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        torrServe = TorrServeRepository(applicationContext)
        serverUrl.value = torrServe.serverUrl()
        items.value = torrServe.cachedEntries().map { it.toMediaContent(serverUrl.value) }
        historyItems.value = mapHistory(torrServe.cachedHistory(), torrServe.cachedEntries(), serverUrl.value)

        setContent {
            PolyaHomeScreen(
                items = items.value,
                historyItems = historyItems.value,
                searchQuery = searchQuery.value,
                searchResults = searchResults.value,
                searchStatus = searchStatus.value,
                isSearching = isSearching.value,
                onSearchQueryChange = { searchQuery.value = it },
                onSearch = ::searchTorrServe,
                onSearchResultSelected = ::openSearchResult,
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

    private fun searchTorrServe() {
        if (isSearching.value) return
        val query = searchQuery.value.trim()
        if (query.isBlank()) {
            searchStatus.value = "Введите название для поиска."
            searchResults.value = emptyList()
            return
        }
        lifecycleScope.launch {
            isSearching.value = true
            searchStatus.value = "Ищем в каталоге TorrServe…"
            try {
                val found = torrServe.searchTorrents(query)
                searchResults.value = found
                searchStatus.value = if (found.isEmpty()) {
                    "Ничего не найдено в каталоге TorrServe."
                } else {
                    "Найдено: ${found.size}. Выберите раздачу, чтобы добавить её в TorrServe и открыть штатный выбор файла."
                }
            } catch (error: Exception) {
                searchResults.value = emptyList()
                searchStatus.value = "Ошибка поиска: ${error.message ?: error.javaClass.simpleName}"
            } finally {
                isSearching.value = false
            }
        }
    }

    private fun openSearchResult(result: TorrServeSearchResult) {
        lifecycleScope.launch {
            try {
                searchStatus.value = "Добавляем «${result.title}» в TorrServe…"
                val entry = torrServe.addSearchResult(result)
                items.value = torrServe.cachedEntries().map { it.toMediaContent(serverUrl.value) }
                if (!chooserLauncher.open(this@MainActivity, entry)) {
                    searchStatus.value = "Раздача добавлена, но не удалось открыть штатный chooser TorrServe."
                } else {
                    searchStatus.value = "Открыто стандартное меню выбора файла TorrServe."
                }
            } catch (error: Exception) {
                searchStatus.value = "Не удалось добавить раздачу: ${error.message ?: error.javaClass.simpleName}"
            }
        }
    }

    private fun refreshTorrServe() {
        if (isRefreshing.value) return
        lifecycleScope.launch {
            isRefreshing.value = true
            try {
                val result = torrServe.refresh()
                items.value = result.entries.map { it.toMediaContent(serverUrl.value) }
                historyItems.value = mapHistory(result.history, result.entries, serverUrl.value)
                status.value = result.message
            } finally {
                isRefreshing.value = false
            }
        }
    }
}

private fun mapHistory(
    history: List<TorrServeHistoryEntry>,
    entries: List<TorrServeEntry>,
    baseUrl: String,
): List<MediaContent> {
    val byHash = entries.associateBy { it.hash }
    return history.mapNotNull { viewed ->
        byHash[viewed.hash]?.toMediaContent(baseUrl)
    }
}

private fun TorrServeEntry.toMediaContent(baseUrl: String) = MediaContent(
    id = "torrserve:$hash",
    title = title,
    sourceId = "torrserve",
    sourceContentId = hash,
    posterUrl = resolvePosterUrl(baseUrl, poster),
    contentType = ContentType.UNKNOWN,
)
