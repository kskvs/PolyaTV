# Интеграции и воспроизведение

## TorrServe
TorrServe — основной playback target. Его адаптер изолирован собственным контрактом.

**Критическое правило:** после выбора контента открыть штатный chooser TorrServe MatriX (выбор файла/эпизода). Не использовать прямой play-flow, если он пропускает chooser. Не создавать его замену.

До production-кода проверить:
- фактические HTTP API и ответы;
- различия версий MatriX;
- способ открыть штатный chooser (intent/deep link/API/совместимый протокол);
- формат ID и launch reference от Lampa/NUM;
- доступность истории и прогресса;
- поведение при недоступном TorrServe.

Собрать fixtures и контрактные тесты. Не выводить API из имён методов или предположений.

## MatriX search

MatriX.134 client uses GET `/search?query=...` and returns tracker results with title, magnet link, info hash, year, size, tracker and seed count. PolyaTV displays those matches without adding them to the server. Only after explicit user selection does it POST `/torrents` with action `add` and the chosen magnet, then opens the standard chooser using the returned torrent hash. This avoids silently adding every search result.

## Lampa и NUM
Каждый источник имеет независимый адаптер для discovery/search, карточек, метаданных/изображений и launch reference. Не предполагать одинаковые API или полноту данных. Сохранять исходный launch payload безопасно; не показывать и не писать чувствительные данные в логи.

### Подтверждённые внешние интерфейсы и ограничения

- Официальный Android-клиент LAMPA публикует экспортированный поисковый ContentProvider с authority вида `<applicationId>.atvsearch`. Он возвращает TMDB-сущности (фильмы/сериалы), а выбор системной подсказки передаётся в Lampa через `GLOBALSEARCH`. Это открывает карточку внутри Lampa, но само по себе **не даёт torrent hash и не открывает TorrServe chooser**. Поэтому этот provider подходит как дополнительный каталог/метаданные, но не должен выдаваться за готовый TorrServe launch adapter.
- NUM (пакет, указанный в публичной теме приложения: `ru.yourok.num`) описывается как Android TV приложение с каналами/рекомендациями и системным голосовым поиском. Публичный стабильный контракт прямого чтения его результатов PolyaTV пока не подтверждён. Не читать его приватную БД и не считать внутренние компоненты API без проверки на устройстве.

Перед включением любого адаптера определить установленный вариант приложения, проверить exported provider/activity и проверить полный путь до chooser MatriX 134 на реальном ТВ.


## Unified feed
Дедуплицировать произведения, сохраняя все найденные источники; выбирать метаданные и изображения по правилам качества; оставлять возможность открыть элемент через нужный источник; переживать частичный отказ адаптеров.

## Постеры и metadata providers
Для TMDB, Kinopoisk и альтернатив проверить официальные API, русский язык, качество изображений, attribution, права локального кэширования и лимиты. Провайдер подключается только после технической и лицензионной проверки.

## Android TV system search

PolyaTV declares its own searchable ContentProvider and `searchable.xml`. The provider returns only the cached TorrServe catalog (no network requests while the system search dialog is typing). Selecting a result opens the standard TorrServe chooser. Submitting a search query without selecting a suggestion opens PolyaTV's in-app search screen.

The Android TV framework can query third-party searchable providers, but the TV manufacturer's launcher controls whether it displays those results and how its physical Search key is routed. This integration does not replace the system search UI or guarantee that a Philips firmware build includes third-party suggestions.

## История playback
Исследовать отдельно: TorrServe API/данные сервера; Android MediaSession/PlaybackState и notification access; события запуска/возврата Lampa/NUM. Сквозной точный прогресс нельзя обещать без реального источника данных. Сопоставление контента делать по нескольким идентификаторам и confidence score.
