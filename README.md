# PolyaTV

PolyaTV развивается как Android TV лаунчер с единой кинотеатральной витриной. На текущем этапе в качестве основы используется Arc Launcher; интеграции TorrServe, Lampa и NUM ещё проходят техническую проверку и не должны считаться готовыми только потому, что лаунчерная основа уже работает.

## Основа и лицензия

Приложение основано на [Arc Launcher](https://github.com/meddouribadis/arclauncher/), форке [LTvLauncher](https://github.com/leanbitlab-org/LtvLauncher/), который восходит к [FLauncher](https://gitlab.com/flauncher/flauncher). История Arc сохранена в Git и объединена с историей PolyaTV отдельным merge-коммитом. Файл [LICENSE](LICENSE) содержит GNU GPL v3; изменения и распространяемые сборки должны соблюдать условия лицензии. Авторские заголовки исходников сохранены.

Новые интеграции PolyaTV следует держать в отдельных модулях, чтобы поддерживать обновления Arc. Документы о продукте, архитектуре и проверке источников находятся в [docs/](docs/).

## Текущее состояние

Arc предоставляет базовый Android TV домашний экран, сетку приложений, категории, навигацию пультом, настройки, обои и Watch Next. Конкретные версии приложения и поведение на Philips 50PUS8057/60 ещё требуют проверки после сборки PolyaTV.

Интеграции медиакаталогов не завершены. В частности, прямой запуск нативного выбора файла TorrServe MatriX без риска автозапуска одного файла не подтверждён. Проверенные факты и ограничения записаны в [docs/INTEGRATIONS.md](docs/INTEGRATIONS.md).

## Сборка и проверки

Текущий источник фиксирует Flutter 3.41.9 и Dart 3.11 в `.fvmrc` и `pubspec.yaml`. Установите соответствующую версию Flutter и Android SDK, затем выполните из корня репозитория:

```powershell
flutter pub get
flutter analyze
flutter test
flutter build apk --debug
```

Сборка debug APK: `build/app/outputs/flutter-apk/app-debug.apk`.

## Проектные документы

- [PRODUCT](docs/PRODUCT.md)
- [ARCHITECTURE](docs/ARCHITECTURE.md)
- [INTEGRATIONS](docs/INTEGRATIONS.md)
- [UX](docs/UX.md)
- [ROADMAP](docs/ROADMAP.md)
- [DECISIONS](docs/DECISIONS.md)
