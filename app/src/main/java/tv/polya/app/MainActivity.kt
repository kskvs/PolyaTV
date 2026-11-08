package tv.polya.app

import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import tv.polya.core.model.ContentType
import tv.polya.core.model.MediaContent
import tv.polya.core.model.MediaSearchResult
import tv.polya.feature.home.PolyaHomeScreen
import tv.polya.feature.home.TvAppShortcut
import tv.polya.integration.lampa.LampaSearchAdapter
import tv.polya.integration.torrserve.TorrServeChooserLauncher
import tv.polya.integration.torrserve.TorrServeEntry
import tv.polya.integration.torrserve.TorrServeHistoryEntry
import tv.polya.integration.torrserve.TorrServeRepository
import tv.polya.integration.torrserve.resolvePosterUrl

class MainActivity : ComponentActivity() {
    private lateinit var torrServe: TorrServeRepository
    private lateinit var lampaSearch: LampaSearchAdapter
    private val serverUrl = mutableStateOf("")
    private val items = mutableStateOf<List<MediaContent>>(emptyList())
    private val historyItems = mutableStateOf<List<MediaContent>>(emptyList())
    private val searchQuery = mutableStateOf("")
    private val systemSearchQuery = mutableStateOf("")
    private val searchResults = mutableStateOf<List<MediaSearchResult>>(emptyList())
    private val searchStatus = mutableStateOf("Введите название фильма или сериала.")
    private val isSearching = mutableStateOf(false)
    private val status = mutableStateOf("Введите адрес TorrServe в настройках и проверьте соединение.")
    private val isRefreshing = mutableStateOf(false)
    private val installedApps = mutableStateOf<List<TvAppShortcut>>(emptyList())
    private val chooserLauncher = TorrServeChooserLauncher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        torrServe = TorrServeRepository(applicationContext)
        lampaSearch = LampaSearchAdapter(applicationContext)
        serverUrl.value = torrServe.serverUrl()
        systemSearchQuery.value = intent.getStringExtra(EXTRA_SYSTEM_SEARCH_QUERY).orEmpty()
        searchQuery.value = systemSearchQuery.value
        installedApps.value = discoverTvApps()
        items.value = torrServe.cachedEntries().map { it.toMediaContent(serverUrl.value) }
        historyItems.value = mapHistory(torrServe.cachedHistory(), torrServe.cachedEntries(), serverUrl.value)

        setContent {
            PolyaHomeScreen(
                items = items.value,
                installedApps = installedApps.value,
                onAppSelected = ::launchTvApp,
                historyItems = historyItems.value,
                searchQuery = searchQuery.value,
                initialSearchQuery = systemSearchQuery.value,
                searchResults = searchResults.value,
                searchStatus = searchStatus.value,
                isSearching = isSearching.value,
                onSearchQueryChange = { searchQuery.value = it },
                onSearch = ::searchAllSources,
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
        if (systemSearchQuery.value.isNotBlank()) searchAllSources()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val query = intent.getStringExtra(EXTRA_SYSTEM_SEARCH_QUERY)?.trim().orEmpty()
        if (query.isNotBlank()) {
            systemSearchQuery.value = query
            searchQuery.value = query
            searchResults.value = emptyList()
            searchAllSources()
        }
    }

    private fun discoverTvApps(): List<TvAppShortcut> {
        val found = LinkedHashMap<String, TvAppShortcut>()
        val categories = listOf(
            Intent.CATEGORY_LEANBACK_LAUNCHER,
            Intent.CATEGORY_LAUNCHER,
        )
        categories.forEach categoryLoop@ { category ->
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            @Suppress("DEPRECATION")
            val matches = packageManager.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY)
            matches.forEach matchLoop@ { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@matchLoop
                if (packageName == this.packageName || found.containsKey(packageName)) return@matchLoop
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                if (label.isNotBlank()) {
                    found[packageName] = TvAppShortcut(packageName = packageName, label = label)
                }
            }
        }
        return found.values.sortedBy { it.label.lowercase() }
    }

    private fun launchTvApp(shortcut: TvAppShortcut) {
        val launchIntent = packageManager.getLeanbackLaunchIntentForPackage(shortcut.packageName)
            ?: packageManager.getLaunchIntentForPackage(shortcut.packageName)
        if (launchIntent == null) {
            Toast.makeText(this, "Не удалось запустить ${shortcut.label}", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching { startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure {
                Toast.makeText(this, "Ошибка запуска ${shortcut.label}: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onResume() {
        super.onResume()
        if (::torrServe.isInitialized) installedApps.value = discoverTvApps()
    }

    private fun searchAllSources() {
        if (isSearching.value) return
        val query = searchQuery.value.trim()
        if (query.isBlank()) {
            searchStatus.value = "Введите название для поиска."
            searchResults.value = emptyList()
            return
        }

        lifecycleScope.launch {
            isSearching.value = true
            searchStatus.value = "Ищем в TorrServe и Lampa…"
            try {
                val torrServeTask = async { runCatching { torrServe.searchTorrents(query) } }
                val lampaTask = async { runCatching { lampaSearch.search(query) } }

                val torrentResult = torrServeTask.await()
                val lampaResult = lampaTask.await()
                val torrentMatches = torrentResult.getOrDefault(emptyList())
                val lampaMatches = lampaResult.getOrDefault(emptyList())
                searchResults.value = tv.polya.core.model.deduplicateMediaSearchResults(
                    torrentMatches + lampaMatches,
                )

                val torrentStatus = when {
                    torrentResult.isSuccess -> "TorrServe: ${torrentMatches.size}"
                    else -> "TorrServe: ошибка"
                }
                val lampaStatus = when {
                    !lampaSearch.isAvailable() -> "Lampa: не установлена"
                    lampaResult.isSuccess -> "Lampa: ${lampaMatches.size}"
                    else -> "Lampa: ошибка"
                }
                val total = searchResults.value.size
                searchStatus.value = if (total == 0) {
                    "Ничего не найдено. $torrentStatus · $lampaStatus"
                } else {
                    "Найдено: $total · $torrentStatus · $lampaStatus"
                }
            } finally {
                isSearching.value = false
            }
        }
    }

    private fun openSearchResult(result: MediaSearchResult) {
        if (result.sourceId == "lampa") {
            if (lampaSearch.open(this, result)) {
                searchStatus.value = "Открыта карточка в Lampa. Продолжите выбор торрента в приложении."
            } else {
                searchStatus.value = "Не удалось открыть результат в Lampa. Проверьте установку приложения."
            }
            return
        }

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

    companion object {
        const val EXTRA_SYSTEM_SEARCH_QUERY = "tv.polya.app.extra.SYSTEM_SEARCH_QUERY"
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
