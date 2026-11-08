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

## Unified feed
Дедуплицировать произведения, сохраняя все найденные источники; выбирать метаданные и изображения по правилам качества; оставлять возможность открыть элемент через нужный источник; переживать частичный отказ адаптеров.

## Постеры и metadata providers
Для TMDB, Kinopoisk и альтернатив проверить официальные API, русский язык, качество изображений, attribution, права локального кэширования и лимиты. Провайдер подключается только после технической и лицензионной проверки.

## История playback
Исследовать отдельно: TorrServe API/данные сервера; Android MediaSession/PlaybackState и notification access; события запуска/возврата Lampa/NUM. Сквозной точный прогресс нельзя обещать без реального источника данных. Сопоставление контента делать по нескольким идентификаторам и confidence score.
