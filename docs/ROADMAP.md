# Roadmap

## Phase 0 — Technical validation
- [ ] Зафиксировать версию TorrServe MatriX и проверить запуск штатного chooser.
- [x] Проверить API/контракты TorrServe и публичный search-provider Lampa.
- [x] Добавить capability-based NUM adapter через TV Provider.
- [ ] Проверить, публикует ли NUM searchable rows на Philips 50PUS8057/60.
- [ ] Проверить доступность истории и прогресса, включая Android MediaSession.
- [ ] Проверить API, лицензии и attribution источников постеров.
- [x] Реализовать Android TV Search Provider по публичному Search API.
- [ ] Проверить отображение сторонних результатов и кнопку Search на Philips firmware.
- [ ] Зафиксировать совместимые версии Android SDK, Gradle, Kotlin и Compose for TV.

## Phase 1 — Skeleton
- [x] Минимальный многомодульный Android TV проект.
- [x] CI: unit tests и debug APK.
- [ ] Дизайн-система и тестовый D-pad экран.
- [ ] Базовая навигация и устойчивое восстановление фокуса.
- [ ] Локальная БД и настройки.

## Phase 2 — Content platform
- [ ] Нормализованная модель и дедупликация.
- [ ] Независимые адаптеры источников.
- [ ] Кэш карточек/постеров и last-known-good snapshots.
- [ ] Единая лента TorrServe/Lampa/NUM.
- [x] Внутренний поиск TorrServe и Android TV system-search provider для локального кэша.

## Phase 3 — Playback
- [x] TorrServe integration со штатным chooser по подтверждённому `action=choose` контракту; реальная проверка на Philips ещё нужна.
- [ ] Source launch references для Lampa/NUM.
- [ ] Проверка запуска на Philips TV.
- [x] Первичная история просмотренных файлов через MatriX `/viewed` с локальным журналом наблюдений.
- [ ] Проверка точного resume progress и синхронизация с Lampa/NUM.

## Phase 4 — Product UX
- [ ] Hero banner и настраиваемые ряды.
- [ ] Настройки карточек и метаданных.
- [ ] Редактор секций и расширенные настройки.
- [ ] Голосовой ввод при системной поддержке.
- [ ] Прототип переназначения кнопок пульта.

## Phase 5 — Windows Companion and release
- [ ] Протокол Companion ↔ PolyaTV.
- [ ] Русская локализация Companion.
- [ ] Настройка, диагностика и ADB-функции.
- [ ] Versioning, APK signing и release workflow.
- [ ] Документация установки/обновления.
