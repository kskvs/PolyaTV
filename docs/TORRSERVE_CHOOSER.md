# TorrServe MatriX: штатный запуск фильма и сериала

PolyaTV передаёт управление штатному PlayActivity TorrServe MatriX:
- package: `ru.yourok.torrserve`
- activity: `ru.yourok.torrserve.ui.activities.play.PlayActivity`
- extras: `hash`, `title`, необязательный `poster`
- extra `action`: **`play`**
- flag: `FLAG_ACTIVITY_NEW_TASK`

## Ожидаемое поведение в MatriX 134

Контракт проверен по исходникам официального клиента TorrServe на точном теге **MatriX.134.Client** (3cb028c):
- `PlayActivity.processIntent()` вызывает `play(false)` для `action=play`, минуя предварительный `ChooserFragment`.
- `Play.play(false)` получает список playable files.
- Если доступен один файл, TorrServe начинает его воспроизведение.
- Если файлов несколько, TorrServe открывает штатный `TorrentFilesFragment` со списком файлов/серий.
- `action=choose` вызывает дополнительный `ChooserFragment` перед этим сценарием; PolyaTV его больше не использует.

Таким образом, PolyaTV не создаёт собственное меню и не подменяет интерфейс TorrServe: решение о запуске одного файла или показе списка файлов остаётся за MatriX.

На Philips 50PUS8057/60 с MatriX 134 ещё требуется фактическая проверка полного сценария, включая возврат в PolyaTV и восстановление фокуса. Контракт подтверждён исходниками, но реальное поведение на телевизоре пока не проверено.

## Исходники MatriX.134.Client
- [PlayActivity.kt](https://github.com/YouROK/TorrServe/blob/3cb028c/app/src/main/java/ru/yourok/torrserve/ui/activities/play/PlayActivity.kt)
- [Play.kt](https://github.com/YouROK/TorrServe/blob/3cb028c/app/src/main/java/ru/yourok/torrserve/ui/activities/play/Play.kt)
- [Helper.kt](https://github.com/YouROK/TorrServe/blob/3cb028c/app/src/main/java/ru/yourok/torrserve/ui/activities/play/Helper.kt)
- [TorrentFilesFragment.kt](https://github.com/YouROK/TorrServe/blob/3cb028c/app/src/main/java/ru/yourok/torrserve/ui/fragments/play/TorrentFilesFragment.kt)
