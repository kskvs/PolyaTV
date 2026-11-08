# PolyaTV

Самостоятельный Android TV launcher/медиахаб. CleanMyTV2 не является основой приложения и используется только для аудита отдельных технических контрактов.

## Первая реализация
- многомодульная основа Android TV;
- Kotlin + Jetpack Compose for TV;
- домашний экран с D-pad навигацией, базовой системой фокуса и экраном подключений;
- TorrServe MatriX adapter: POST `/torrents` с `action=list`, last-known-good cache и защита карточек от временного исчезновения;
- запуск стандартного TorrServe chooser через `action=choose`;
- GitHub Actions: unit tests и сборка debug APK.

Lampa, NUM, общий поиск и полноценный каталог постеров пока не реализованы.

## Сборка Windows
Требуются JDK 17 и Android SDK Platform 37:
```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```
APK: `app\build\outputs\apk\debug\app-debug.apk`.

## Подключение TorrServe
PolyaTV → Настройки → укажите адрес сервера (например `http://192.168.1.20:8090`) → «Сохранить и проверить». Нажатие на карточку передаёт управление штатному chooser TorrServe. Подробности: [docs/TORRSERVE_CHOOSER.md](docs/TORRSERVE_CHOOSER.md).
