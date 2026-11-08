# PolyaTV

Самостоятельный Android TV launcher/медиахаб. CleanMyTV2 не является основой приложения и используется только для аудита отдельных технических контрактов.

## Уже реализовано
- многомодульная основа Android TV на Kotlin + Jetpack Compose for TV;
- домашний экран с D-pad навигацией и списком установленных ТВ-приложений;
- TorrServe MatriX adapter: список, постеры, last-known-good cache и устойчивость к кратковременно пропадающим карточкам;
- история по MatriX `/viewed` с локальным кэшем отметок;
- поиск TorrServe и открытого каталога Lampa: torrent добавляется только после выбора пользователя, Lampa-результат открывает карточку в Lampa;
- системный Android TV Search provider для поиска по уже сохранённой медиатеке TorrServe;
- открытие стандартного TorrServe chooser через `action=choose`;
- GitHub Actions: unit tests и сборка debug APK.

NUM пока не подключён как каталог: его стабильный внешний контракт ещё не подтверждён. Системный Android TV Search provider по-прежнему выдаёт только кэш TorrServe; внутренний экран поиска обращается к TorrServe и экспортированному provider Lampa.

## Сборка Windows
Требуются JDK 17 и Android SDK Platform 37:
```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```
APK: `app\build\outputs\apk\debug\app-debug.apk`.

## Подключение TorrServe
PolyaTV → Настройки → укажите адрес сервера (например `http://192.168.1.20:8090`) → «Сохранить и проверить». Нажатие на карточку передаёт управление штатному chooser TorrServe. Подробности: [docs/TORRSERVE_CHOOSER.md](docs/TORRSERVE_CHOOSER.md).
