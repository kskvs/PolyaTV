package tv.polya.app

import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import tv.polya.integration.num.NumTvCatalogAdapter
import tv.polya.integration.torrserve.TorrServeChooserLauncher
import tv.polya.integration.torrserve.TorrServeEntry
import tv.polya.integration.torrserve.TorrServeHistoryEntry
import tv.polya.integration.torrserve.TorrServeRepository
import tv.polya.integration.torrserve.resolvePosterUrl

class MainActivity : ComponentActivity() {
    private lateinit var torrServe: TorrServeRepository
    private lateinit var lampaSearch: LampaSearchAdapter
    private lateinit var numCatalog: NumTvCatalogAdapter
    private val serverUrl = mutableStateOf("")
    private val items = mutableStateOf<List<MediaContent>>(emptyList())
    private val historyItems = mutableStateOf<List<MediaContent>>(emptyList())
    private val searchQuery = mutableStateOf("")
    private val systemSearchQuery = mutableStateOf("")
    private val searchResults = mutableStateOf<List<MediaSearchResult>>(emptyList())
    private val searchStatus = mutableStateOf("Введите название фильма или сериала.")
    private val isSearching = mutableStateOf(false)
    private val status = mutableStateOf("Проверяем TorrServe на этом телевизоре.")
    private val isRefreshing = mutableStateOf(false)
    private val installedApps = mutableStateOf<List<TvAppShortcut>>(emptyList())
    private val allInstalledApps = mutableStateOf<List<TvAppShortcut>>(emptyList())
    private lateinit var launcherPreferences: LauncherPreferences
    private val numPermissionGranted = mutableStateOf(false)
    private val requestNumPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        numPermissionGranted.value = granted
        if (granted && searchQuery.value.isNotBlank()) searchAllSources()
    }
    private val chooserLauncher = TorrServeChooserLauncher()
    private var refreshNumOnReturn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        torrServe = TorrServeRepository(applicationContext)
        launcherPreferences = LauncherPreferences(applicationContext)
        lampaSearch = LampaSearchAdapter(applicationContext)
        numCatalog = NumTvCatalogAdapter(applicationContext)
        numPermissionGranted.value = numCatalog.hasReadPermission()
        serverUrl.value = torrServe.serverUrl()
        systemSearchQuery.value = intent.getStringExtra(EXTRA_SYSTEM_SEARCH_QUERY).orEmpty()
        searchQuery.value = systemSearchQuery.value
        refreshInstalledApps()
        items.value = torrServe.cachedEntries().map { it.toMediaContent(serverUrl.value) }
        historyItems.value = mapHistory(torrServe.cachedHistory(), torrServe.cachedEntries(), serverUrl.value)

        setContent {
            PolyaHomeScreen(
                items = items.value,
                installedApps = installedApps.value,
                allInstalledApps = allInstalledApps.value,
                onAppSelected = ::launchTvApp,
                onToggleFavorite = ::toggleFavorite,
                onToggleHidden = ::toggleHidden,
                historyItems = historyItems.value,
                searchQuery = searchQuery.value,
                initialSearchQuery = systemSearchQuery.value,
                searchResults = searchResults.value,
                numPermissionGranted = numPermissionGranted.value,
                onRequestNumPermission = ::requestNumSearchPermission,
                onRefreshNum = ::openNumForRefresh,
                searchStatus = searchStatus.value,
                isSearching = isSearching.value,
                onSearchQueryChange = { searchQuery.value = it },
                onSearch = ::searchAllSources,
                onSearchResultSelected = ::openSearchResult,
                serverUrl = serverUrl.value,
                connectionMessage = status.value,
                isRefreshing = isRefreshing.value,
                onServerUrlChange = { value ->
                    serverUrl.value = value
                    torrServe.saveServerUrl(value)
                },
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

    private fun refreshInstalledApps() {
        val favorites = launcherPreferences.favorites()
        val hidden = launcherPreferences.hidden()
        val all = discoverTvApps().map { app ->
            app.copy(favorite = app.packageName in favorites, hidden = app.packageName in hidden)
        }
        allInstalledApps.value = all
        installedApps.value = all.filterNot { it.hidden }.sortedWith(
            compareByDescending<TvAppShortcut> { it.favorite }.thenBy { it.label.lowercase() },
        )
    }

    private fun toggleFavorite(app: TvAppShortcut) {
        launcherPreferences.setFavorite(app.packageName, !app.favorite)
        refreshInstalledApps()
    }

    private fun toggleHidden(app: TvAppShortcut) {
        launcherPreferences.setHidden(app.packageName, !app.hidden)
        refreshInstalledApps()
    }

    private fun discoverTvApps(): List<TvAppShortcut> {
        // QUERY_ALL_PACKAGES lets the launcher see installed apps even when they do not
        // publish a LEANBACK_LAUNCHER activity. Keep only packages Android can actually launch.
        @Suppress("DEPRECATION")
        val applications = packageManager.getInstalledApplications(PackageManager.MATCH_ALL)
        return applications.asSequence()
            .filter { it.packageName != packageName }
            .mapNotNull { application ->
                val launchIntent = packageManager.getLeanbackLaunchIntentForPackage(application.packageName)
                    ?: packageManager.getLaunchIntentForPackage(application.packageName)
                    ?: return@mapNotNull null
                val label = runCatching { packageManager.getApplicationLabel(application).toString().trim() }
                    .getOrDefault("")
                if (label.isBlank()) return@mapNotNull null
                TvAppShortcut(packageName = application.packageName, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
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
        if (::torrServe.isInitialized) {
            refreshInstalledApps()
            numPermissionGranted.value = numCatalog.hasReadPermission()
            if (refreshNumOnReturn) {
                refreshNumOnReturn = false
                if (searchQuery.value.isNotBlank()) searchAllSources()
            }
        }
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
                val numTask = async { runCatching { numCatalog.searchWithStatus(query) } }

                val torrentResult = torrServeTask.await()
                val lampaResult = lampaTask.await()
                val numResult = numTask.await()
                val torrentMatches = torrentResult.getOrDefault(emptyList())
                val lampaMatches = lampaResult.getOrDefault(emptyList())
                val numMatches = numResult.getOrNull()?.items.orEmpty()
                searchResults.value = tv.polya.core.model.deduplicateMediaSearchResults(
                    torrentMatches + lampaMatches + numMatches,
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
                val numStatus = when {
                    !numPermissionGranted.value -> "NUM: нужно разрешение на рекомендации Android TV"
                    numResult.isFailure -> "NUM: ошибка чтения системного каталога"
                    else -> numResult.getOrNull()?.status ?: "NUM: каталог недоступен"
                }
                val total = searchResults.value.size
                searchStatus.value = if (total == 0) {
                    "Ничего не найдено. $torrentStatus · $lampaStatus · $numStatus"
                } else {
                    "Найдено: $total · $torrentStatus · $lampaStatus · $numStatus"
                }
            } finally {
                isSearching.value = false
            }
        }
    }

    private fun openSearchResult(result: MediaSearchResult) {
        if (result.sourceId == "num") {
            if (numCatalog.open(this, result)) {
                searchStatus.value = "Открыта рекомендация NUM."
            } else {
                searchStatus.value = "Не удалось открыть карточку NUM. Проверьте приложение и разрешение системных рекомендаций."
            }
            return
        }
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

    private fun openNumForRefresh() {
        val launchIntent = packageManager.getLeanbackLaunchIntentForPackage(NUM_PACKAGE)
            ?: packageManager.getLaunchIntentForPackage(NUM_PACKAGE)
        if (launchIntent == null) {
            searchStatus.value = "Приложение NUM не установлено."
            return
        }
        refreshNumOnReturn = true
        runCatching { startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure {
                refreshNumOnReturn = false
                searchStatus.value = "Не удалось открыть NUM: ${it.message ?: it.javaClass.simpleName}"
            }
    }

    private fun requestNumSearchPermission() {
        if (numCatalog.hasReadPermission()) {
            numPermissionGranted.value = true
            searchAllSources()
        } else {
            requestNumPermission.launch("android.permission.READ_TV_LISTINGS")
        }
    }

    companion object {
        const val EXTRA_SYSTEM_SEARCH_QUERY = "tv.polya.app.extra.SYSTEM_SEARCH_QUERY"
        private const val NUM_PACKAGE = "ru.yourok.num"
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
