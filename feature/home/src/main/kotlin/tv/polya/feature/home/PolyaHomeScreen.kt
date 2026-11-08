package tv.polya.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import tv.polya.core.model.MediaContent

private enum class HomePage { HOME, SETTINGS }

@Composable
fun PolyaHomeScreen(
    items: List<MediaContent>,
    serverUrl: String,
    connectionMessage: String,
    isRefreshing: Boolean,
    onServerUrlChange: (String) -> Unit,
    onSaveAndRefresh: () -> Unit,
    onRefresh: () -> Unit,
    onItemSelected: (MediaContent) -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(HomePage.HOME) }
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF10151D), Color(0xFF080B10))),
            ).padding(horizontal = 54.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("POLYA", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Text("TV", color = Color(0xFF70D6C2), fontSize = 25.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 7.dp))
                Spacer(Modifier.weight(1f))
                NavItem("Главная", page == HomePage.HOME) { page = HomePage.HOME }
                Spacer(Modifier.width(10.dp))
                NavItem("Настройки", page == HomePage.SETTINGS) { page = HomePage.SETTINGS }
            }
            when (page) {
                HomePage.HOME -> HomeContent(
                    items, connectionMessage, isRefreshing, onRefresh,
                    { page = HomePage.SETTINGS }, onItemSelected,
                )
                HomePage.SETTINGS -> SettingsContent(
                    serverUrl, connectionMessage, isRefreshing, onServerUrlChange,
                    onSaveAndRefresh, onRefresh,
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    items: List<MediaContent>,
    connectionMessage: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onItemSelected: (MediaContent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 36.dp),
    ) {
        item(key = "hero") {
            Surface(
                onClick = if (items.isNotEmpty()) ({ onItemSelected(items.first()) }) else onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(244.dp),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(22.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f),
                colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF1B2A37), focusedContainerColor = Color(0xFF263C4B)),
            ) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(listOf(Color(0xFF183D43), Color(0xFF1B2939), Color(0xFF111720))),
                    ).padding(horizontal = 38.dp, vertical = 28.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (items.isNotEmpty()) "ПРОДОЛЖИТЬ СМОТРЕТЬ" else "ВАШ ДОМАШНИЙ КИНОТЕАТР",
                            color = Color(0xFF70D6C2), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                        )
                        Text(
                            items.firstOrNull()?.title ?: "Всё кино — в одном месте",
                            color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (items.isNotEmpty()) "OK — открыть стандартное меню TorrServe"
                            else "Подключите TorrServe, чтобы загрузить вашу медиатеку",
                            color = Color(0xFFD5DEE7), fontSize = 16.sp,
                        )
                    }
                }
            }
        }
        item(key = "sources") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ИСТОЧНИКИ", color = Color(0xFFB9C4D0), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SourcePill("TorrServe", true)
                    SourcePill("Lampa", false)
                    SourcePill("NUM", false)
                }
                Text(
                    connectionMessage.ifBlank { "TorrServe — первый подключаемый источник. Lampa и NUM будут добавлены отдельными модулями." },
                    color = Color(0xFF8F9BA8), fontSize = 13.sp, maxLines = 2,
                )
            }
        }
        if (items.isNotEmpty()) {
            item(key = "shelf-title") {
                Text("Медиатека TorrServe", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            }
            item(key = "shelf") {
                val firstCardFocus = remember { FocusRequester() }
                LazyRow(
                    modifier = Modifier.fillMaxWidth().focusRestorer(firstCardFocus).focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                ) {
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        MediaCard(item, if (index == 0) firstCardFocus else null) { onItemSelected(item) }
                    }
                }
            }
        } else {
            item(key = "empty") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Медиатека пока пуста", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "После подключения PolyaTV загрузит список TorrServe. При временном сбое останется последний сохранённый список.",
                        color = Color(0xFF98A4B2), fontSize = 14.sp,
                    )
                    ActionSurface("Настроить TorrServe", onOpenSettings)
                }
            }
        }
        item(key = "refresh") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ActionSurface(if (isRefreshing) "Обновление…" else "Обновить список", onRefresh, enabled = !isRefreshing)
                Text("Выбор карточки открывает штатный chooser TorrServe, без прямого старта файла.", color = Color(0xFF7F8B98), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsContent(
    serverUrl: String,
    connectionMessage: String,
    isRefreshing: Boolean,
    onServerUrlChange: (String) -> Unit,
    onSaveAndRefresh: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Подключения", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("TorrServe MatriX", color = Color(0xFF70D6C2), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Введите адрес сервера в локальной сети вместе с портом. Например: http://192.168.1.20:8090",
            color = Color(0xFF9BA8B6), fontSize = 14.sp,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(0.72f).height(62.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF1B2530)),
        ) {
            Box(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                    decorationBox = { inner ->
                        if (serverUrl.isBlank()) Text("http://адрес-телевизора:8090", color = Color(0xFF778494), fontSize = 17.sp)
                        inner()
                    },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionSurface(if (isRefreshing) "Проверка…" else "Сохранить и проверить", onSaveAndRefresh, enabled = !isRefreshing)
            ActionSurface("Проверить ещё раз", onRefresh, enabled = !isRefreshing)
        }
        Text(connectionMessage, color = Color(0xFFB8C4D0), fontSize = 14.sp, maxLines = 3)
        Text("Lampa и NUM будут добавлены отдельными независимыми адаптерами.", color = Color(0xFF7F8B98), fontSize = 13.sp)
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color(0xFF263C45) else Color.Transparent,
            focusedContainerColor = Color(0xFF314B56),
        ),
    ) {
        Text(
            label, color = if (selected) Color(0xFF8BE3D2) else Color(0xFFD5DDE6),
            fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun SourcePill(label: String, active: Boolean) {
    Surface(shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(30.dp)), colors = ClickableSurfaceDefaults.colors(containerColor = if (active) Color(0xFF1D383D) else Color(0xFF1B222B))) {
        Text(
            if (active) "$label · подключение" else "$label · скоро",
            color = if (active) Color(0xFF80D9C7) else Color(0xFF8995A2),
            fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun MediaCard(item: MediaContent, focusRequester: FocusRequester?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(148.dp).then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.055f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                BorderStroke(2.dp, Color(0xFF75D9C8)), RoundedCornerShape(14.dp),
            ),
        ),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF1B2732), focusedContainerColor = Color(0xFF273946)),
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(198.dp).background(
                    Brush.linearGradient(listOf(Color(0xFF35515A), Color(0xFF202B38))),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.title.take(1).uppercase(), color = Color(0xFFDAE8E8), fontSize = 42.sp, fontWeight = FontWeight.Bold)
            }
            Text(item.title, color = Color.White, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp))
        }
    }
}

@Composable
private fun ActionSurface(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.035f),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF254A4D), focusedContainerColor = Color(0xFF326568)),
    ) {
        Text(label, color = if (enabled) Color.White else Color(0xFF78838D), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp))
    }
}
