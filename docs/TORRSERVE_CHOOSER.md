# TorrServe MatriX: стандартное меню выбора

PolyaTV вызывает явный Android Intent:
- package: `ru.yourok.torrserve`
- activity: `ru.yourok.torrserve.ui.activities.play.PlayActivity`
- extras: `hash`, `title`, необязательный `poster`
- extra `action`: **`choose`**
- flag: `FLAG_ACTIVITY_NEW_TASK`

Контракт дополнительно проверен по исходникам официального клиента TorrServe на точном теге **MatriX.134.Client** (3cb028c): `PlayActivity.processIntent()` немедленно вызывает `play(false)` только для `action=play`; в остальных случаях показывает `ChooserFragment`, а `action=choose` принудительно открывает окно выбора. `Helper.kt` этой версии читает extras `hash`, `title` и `poster`. Это подтверждает совместимость имени Activity и набора extras на уровне исходников именно версии 134. В CleanMyTV2 используется тот же Intent. PolyaTV передаёт только `choose`.

На Philips 50PUS8057/60 с TorrServe MatriX 134 нужно проверить наличие Activity, открытие chooser, выбор файла/серии, отсутствие автоматического старта и возврат с восстановлением фокуса. Это подтверждено исходным кодом именно MatriX.134.Client, но фактический запуск ещё не проверен на физическом телевизоре. Fallback на `/stream` или прямой playback не реализуется.


## Исходники MatriX.134.Client
- [PlayActivity.kt](https://github.com/YouROK/TorrServe/blob/3cb028c/app/src/main/java/ru/yourok/torrserve/ui/activities/play/PlayActivity.kt)
- [Helper.kt](https://github.com/YouROK/TorrServe/blob/3cb028c/app/src/main/java/ru/yourok/torrserve/ui/activities/play/Helper.kt)
- [AndroidManifest.xml](https://github.com/YouROK/TorrServe/blob/3cb028c/app/src/main/AndroidManifest.xml)

В manifest тега 3cb028c PlayActivity объявлена как exported Activity, пригодная для явного внешнего запуска.
