# Архитектура PolyaTV

## Основа

PolyaTV наследует Android TV launcher shell из [Arc Launcher](https://github.com/meddouribadis/arclauncher/). Git history Arc и прежняя история PolyaTV соединены merge-коммитом; авторские заголовки, LICENSE GPL-3.0, Flutter и Android-модули Arc сохранены. Основа — Flutter/Dart, а не Kotlin/Compose.

Обновления upstream должны приходить отдельными проверяемыми merge/cherry-pick операциями. Не переписывать или заменять историю Arc. Новые интеграции изолировать от launcher shell и связывать с ним через стабильные модели и интерфейсы.

## Текущие границы

```text
Flutter launcher shell (Arc)
  ├── приложения, категории и настройки
  ├── навигация D-pad и восстановление фокуса
  ├── обои и screensaver
  └── Watch Next (основная первая поверхность витрины; Android TV provider + подтверждённые source adapters)

Будущие PolyaTV модули
  ├── TorrServe discovery/API и проверенный launch bridge
  ├── Lampa каталог/search-provider bridge
  ├── NUM bridge только после проверки публичного контракта
  └── единая витрина и поиск
```

Arc Watch Next используется как основная первая поверхность витрины и сохраняет существующие D-pad focus/hero/карточки. Его текущая выборка из Android TV `TvProvider` — лишь один источник данных. TorrServe, Lampa и NUM подключаются отдельными адаптерами к той же presentation-модели только после проверки контрактов в [INTEGRATIONS.md](INTEGRATIONS.md); не подменять неизвестный контракт имитацией карточек.

## Принципы

1. Сохранять исходную launcher-навигацию Arc и её D-pad focus behavior.
2. Источники не разделяют идентификаторы и launch payload без нормализации и provenance.
3. Сетевые операции не выполнять в build-методах Flutter widgets; использовать отдельные service/provider слои.
4. Витрина не удаляет last-known-good данные после временного пустого/ошибочного ответа.
5. Не запускать playback напрямую из PolyaTV: целевой flow — штатный выбор файла/серии MatriX, пока его безопасный внешний контракт не подтверждён.
6. Не декларировать незапрошенные runtime permissions; запрашивать их непосредственно перед понятным пользователю действием.

## Планируемые data contracts

Нормализованный элемент витрины должен хранить canonical ID, title/original title, тип (movie/series/episode), год, poster/backdrop URLs с provenance и несколько source references. Source reference содержит source ID, идентификатор источника и подтверждённый launch payload; его нельзя подменять торрент hash по названию или TMDB ID.

## Проверки

Для изменений shell запускать `flutter analyze`, `flutter test` и `flutter build apk --debug`. На каждом этапе проверить D-pad focus, back navigation и восстановление фокуса после возврата из стороннего приложения. Физическая проверка выполняется на Philips 50PUS8057/60; её результат отмечать отдельно от эмулятора.
