# Архитектура PolyaTV

## Принципы
1. Отдельный проект; CleanMyTV2 не является базой приложения.
2. Разделение UI, доменной модели, хранилища и адаптеров источников.
3. Android TV UI на Kotlin + Jetpack Compose for TV.
4. Сеть и интеграционная логика не размещаются в composable.
5. Контракты адаптеров тестируются отдельно.
6. Не плодить Gradle-модули формально: важны чёткие API и границы ответственности.

## Предлагаемая структура
```
PolyaTV/
├── app/                       # Android application, DI, entry point
├── core/
│   ├── model/                 # Доменные модели
│   ├── database/              # Локальное хранилище и миграции
│   ├── network/               # HTTP, serialization, retry policies
│   ├── designsystem/          # TV-компоненты и темы
│   └── navigation/            # Навигация и focus management
├── feature/
│   ├── home/
│   ├── search/
│   ├── library/
│   ├── history/
│   ├── apps/
│   └── settings/
├── integration/
│   ├── torrserve/
│   ├── lampa/
│   └── num/
├── docs/
└── windows-companion/         # Возможно, отдельный репозиторий
```

## Потоки данных
Источники -> адаптеры -> нормализованные ContentItem -> repository/use cases -> ViewModel -> Compose UI.

Запуск: UI action -> LaunchContent use case -> TorrServe adapter/intent bridge -> штатный TorrServe chooser.

История: playback/session/source events -> History repository -> локальная БД -> Home/History.

## Модель контента
Минимальные поля: canonicalId, title, originalTitle, contentType (MOVIE/SERIES/EPISODE/UNKNOWN), year, rating, poster/backdrop candidates, sourceItems (sourceId, sourceContentId, launch reference), metadata provenance, updatedAt. Не использовать название или URL как единственный ключ дедупликации.

## Хранение
Настройки и порядок секций, кэш нормализованных карточек, mapping идентификаторов, история и доступный прогресс, sync state и timestamp последнего успешного обновления. Кэш и пользовательские настройки имеют разные правила очистки.

## Надёжность
Изолированные таймауты/retry, backoff, last-known-good snapshot. Не очищать содержимое из-за пустого/ошибочного ответа. Атомарно заменять snapshot после успешной загрузки. Логировать структурированно, маскировать токены и приватные URL.

## Тестирование
Unit-тесты нормализации, дедупликации, кэша, сортировки и восстановления фокуса. Интеграционные тесты TorrServe chooser/API выполнять с реальным сервисом и на целевом устройстве.
