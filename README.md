# PolyaTV

Самостоятельный Android TV launcher/медиахаб. CleanMyTV2 не является основой приложения и используется только для аудита отдельных технических контрактов.

## Уже реализовано
- многомодульная основа Android TV на Kotlin + Jetpack Compose for TV;
- домашний экран с D-pad навигацией и списком установленных ТВ-приложений;
- TorrServe MatriX adapter: список, постеры, last-known-good cache и устойчивость к кратковременно пропадающим карточкам;
- история по MatriX `/viewed` с локальным кэшем отметок;
- внутренний поиск по каталогу TorrServe: результат добавляется только после выбора пользователя;
- системный Android TV Search provider для поиска по уже сохранённой медиатеке TorrServe;
- открытие стандартного TorrServe chooser через `action=choose`;
- GitHub Actions: unit tests и сборка debug APK.

Lampa/NUM пока не подключены как полноценные каталоги PolyaTV. Проверяются их внешние интерфейсы и ограничения. Глобальный поиск выдаёт только данные из уже сохранённого каталога PolyaTV и не делает сетевых запросов из ContentProvider.

## Сборка Windows
Требуются JDK 17 и Android SDK Platform 37:
```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```
APK: `app\build\outputs\apk\debug\app-debug.apk`.

## Подключение TorrServe
PolyaTV → Настройки → укажите адрес сервера (например `http://192.168.1.20:8090`) → «Сохранить и проверить». Нажатие на карточку передаёт управление штатному chooser TorrServe. Подробности: [docs/TORRSERVE_CHOOSER.md](docs/TORRSERVE_CHOOSER.md).
