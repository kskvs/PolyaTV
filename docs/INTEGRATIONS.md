# Интеграции и воспроизведение

## TorrServe
TorrServe — основной playback target. Его адаптер изолирован собственным контрактом.

**Критическое правило:** передавать запуск в штатный TorrServe MatriX через `action=play`. MatriX сам выбирает поведение: один playable-файл запускается сразу, несколько файлов открывают штатный список файлов/серий. Не показывать промежуточное меню PolyaTV и не вызывать принудительный `action=choose`.

До production-кода проверить:
- фактические HTTP API и ответы;
- различия версий MatriX;
- поведение запуска для single-file и multi-file торрентов;
- формат ID и launch reference от Lampa/NUM;
- доступность истории и прогресса;
- поведение при недоступном TorrServe.

Собрать fixtures и контрактные тесты. Не выводить API из имён методов или предположений.

## Lampa search

Lampa's exported Android TV search provider is queried through `content://top.rootu.lampa.atvsearch/search/search_suggest_query`. Its rows contain TMDB ID, media type, title, description and artwork URL. Selecting a result opens Lampa's exported `top.rootu.lampa.MainActivity` with action `GLOBALSEARCH`, the TMDB ID and `movie/tv` type. No private Lampa database is read. This is a catalogue hand-off; the final torrent selection remains in Lampa and then uses the normal TorrServe hand-off.

## MatriX search

MatriX.134 client uses GET `/search?query=...` and returns tracker results with title, magnet link, info hash, year, size, tracker and seed count. PolyaTV displays those matches without adding them to the server. Only after explicit user selection does it POST `/torrents` with action `add` and the chosen magnet, then starts the returned torrent through the native MatriX `action=play` path. This avoids silently adding every search result.

## Lampa и NUM
Каждый источник имеет независимый адаптер для discovery/search, карточек, метаданных/изображений и launch reference. Не предполагать одинаковые API или полноту данных. Сохранять исходный launch payload безопасно; не показывать и не писать чувствительные данные в логи.

### Подтверждённые внешние интерфейсы и ограничения

- Официальный Android-клиент LAMPA (application ID `top.rootu.lampa`) публикует экспортированный поисковый ContentProvider `top.rootu.lampa.atvsearch`. Адаптер PolyaTV запрашивает его через Android ContentResolver и получает TMDB-сущности. Выбор результата передаётся в Lampa как `GLOBALSEARCH` с TMDB ID и типом `movie/tv`. Это открывает карточку внутри Lampa, но **не выдаёт torrent hash и не открывает TorrServe chooser напрямую**: пользователь продолжает штатный сценарий Lampa, который может передать выбранный torrent в TorrServe.
- NUM (пакет, указанный в публичной теме приложения: `ru.yourok.num`) описывается как Android TV приложение с каналами/рекомендациями и системным голосовым поиском. Публичный стабильный контракт прямого чтения его результатов PolyaTV пока не подтверждён. Не читать его приватную БД и не считать внутренние компоненты API без проверки на устройстве.

Перед включением любого адаптера определить установленный вариант приложения, проверить exported provider/activity и проверить полный путь до запуска MatriX 134 на реальном ТВ.


## NUM Android TV recommendations

The NUM adapter uses Android's public TV Provider contract, not NUM's private files or database. It requests the dangerous runtime permission `android.permission.READ_TV_LISTINGS` only after the user chooses “Разрешить доступ к рекомендациям NUM” in PolyaTV settings. It queries `TvContract.PreviewPrograms` without a SQL selection, then filters rows by owning package `ru.yourok.num` and `COLUMN_SEARCHABLE=1`. Each row is accepted only when its `intent_uri` resolves back to the NUM package; selecting a result launches that published intent. Relative or private poster URIs are not copied into the external UI.

This adapter is **capability-based**: if NUM does not publish searchable preview-program rows, the adapter correctly returns no results. Its real presence and result count on Philips 50PUS8057/60 still needs device testing. Android restricts third-party reads to searchable rows and the permission; the signature/privileged all-EPG permission is not requested.

## Unified feed
Дедуплицировать произведения, сохраняя все найденные источники; выбирать метаданные и изображения по правилам качества; оставлять возможность открыть элемент через нужный источник; переживать частичный отказ адаптеров.

## Постеры и metadata providers
Для TMDB, Kinopoisk и альтернатив проверить официальные API, русский язык, качество изображений, attribution, права локального кэширования и лимиты. Провайдер подключается только после технической и лицензионной проверки.

## Android TV system search

PolyaTV declares its own searchable ContentProvider and `searchable.xml`. The provider returns only the cached TorrServe catalog (no network requests while the system search dialog is typing). Selecting a result hands playback to the native MatriX `action=play` path. Submitting a search query without selecting a suggestion opens PolyaTV's in-app search screen.

The Android TV framework can query third-party searchable providers, but the TV manufacturer's launcher controls whether it displays those results and how its physical Search key is routed. This integration does not replace the system search UI or guarantee that a Philips firmware build includes third-party suggestions.

## История playback
Исследовать отдельно: TorrServe API/данные сервера; Android MediaSession/PlaybackState и notification access; события запуска/возврата Lampa/NUM. Сквозной точный прогресс нельзя обещать без реального источника данных. Сопоставление контента делать по нескольким идентификаторам и confidence score.
