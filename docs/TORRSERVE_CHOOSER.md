# TorrServe MatriX: стандартное меню выбора

PolyaTV вызывает явный Android Intent:
- package: `ru.yourok.torrserve`
- activity: `ru.yourok.torrserve.ui.activities.play.PlayActivity`
- extras: `hash`, `title`, необязательный `poster`
- extra `action`: **`choose`**
- flag: `FLAG_ACTIVITY_NEW_TASK`

В CleanMyTV2 `home/app/src/main/java/io/github/wingedpanther/cleanmytv/home/TorrServe.kt`, функция `openTorrServe`, зафиксировано: `action=choose` вызывает штатный chooser, а `action=play` начинает воспроизведение напрямую. PolyaTV использует только `choose`.

На Philips 50PUS8057/60 с TorrServe MatriX 134 нужно проверить наличие Activity, открытие chooser, выбор файла/серии, отсутствие автоматического старта и возврат с восстановлением фокуса. Это подтверждённый кодом контракт, но не утверждение о протестированной совместимости каждой сборки. Fallback на `/stream` или прямой playback не реализуется.
