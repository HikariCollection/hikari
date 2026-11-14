# AGENTS.md

Repository guidance for coding agents working in **Hikari** (Android manga reader, Kotlin + Jetpack Compose, Mihon derivative).

## Build, Format, and Verification

Use `./gradlew` on POSIX/CI or `.\gradlew.bat` on Windows. Keep Gradle's Isolated Projects and configuration cache constraints in mind (`gradle.properties`).

- **Format check / apply**: `./gradlew spotlessCheck` (`./gradlew spotlessApply` to auto-fix)
- **All unit tests**: `./gradlew testDebugUnitTest`
- **Targeted unit test**: `./gradlew :app:testDebugUnitTest --tests "app.hikari.path.TestClass"` (always scope `--tests` to the owning module; root `testDebugUnitTest --tests` fails on modules with no matching tests)
- **Build debug APK**: `./gradlew assembleDebug`
- **Lint**: `./gradlew lintDebug` (or module-scoped, e.g., `./gradlew :app:lintDebug :i18n:lintDebug`)

Standard pre-completion verification matching CI (`.github/workflows/ci.yml`):
`spotlessCheck` -> `testDebugUnitTest` -> `assembleDebug`, plus `lintDebug` on modified modules (or root).

## Module Architecture

Defined in `settings.gradle.kts`, configured via `gradle/build-logic` (`hikari.plugins.*`), `gradle/libs.versions.toml` (`libs`), and `gradle/hikari.versions.toml` (`hikarix`):

- `:app`: Application entry points, Voyager Compose screens/ViewModels, WorkManager workers, backup/restore, notifications, trackers, extension loader, and `AppGraph`.
- `:domain`: Use cases/interactors, domain models, and repository interfaces.
- `:data`: SQLDelight `Database` (`data/src/main/sqldelight/app/hikari/data/*.sq`, async, SQLite 3.38 dialect) and repository implementations.
- `:presentation-core` / `:presentation-widget`: Shared Compose UI components, Material 3 themes, and Glance widgets.
- `:core:common`, `:core:archive`, `:core:metro`, `:core-metadata`: Low-level utilities, archive handling, Metro helpers, and ComicInfo metadata parsers.
- `:source-api`: Public catalog contract published as `app.hikari:source-api` (see `.github/workflows/publish_source_api.yml`). Keep external extension compatibility intact.
- `:source-local`: Local CBZ/EPUB/directory manga source.
- `:i18n`: Shared string and plural resources (`i18n/src/main/res/values/{strings,plurals}.xml`). `GenerateMRTask` generates `app.hikari.i18n.MR`.
- `:icons:*` & `:baseline-profile`: Icon resource modules and ART baseline/startup profiles.

## Code Conventions & Guardrails

- **Dependency injection**: Metro (`dev.zacsweers.metro`) is primary (`AppGraph`, `AppBindings`, `@Inject`, `@AssistedInject`). `MetroInjektRegistrar` is a read-only Injekt bridge for legacy callers—do not register new bindings in Injekt.
- **Icon resource imports** (enforced by `PluginSpotless.kt`):
  - `import app.hikari.icons.materialsymbols.R as MaterialSymbolsR`
  - `import app.hikari.icons.custommaterialsymbols.R as CustomMaterialSymbolsR`
- **Formatting** (`.editorconfig` + Spotless): 4-space indent for `.kt`, `.kts`, `.xml`, `.sq`; 120-char max line length; trailing commas allowed; `@Composable` functions may use PascalCase.
- **Testing**: JUnit 5 + MockK + Kotest in `app/src/test`, `domain/src/test`, and `core/common/src/test`. Prefer pure helper extraction or constructor/internal hooks for Worker/Android-coupled code.

## Hikari Invariants

- **Zero telemetry**: Do not add analytics, Firebase, Crashlytics, or background tracking telemetry.
- **Rebrand & compatibility**: Package/namespace is `app.hikari`, database is `hikari.db`, ComicInfo custom tags use `hk:`, and backups use `.hkbackup` while preserving `.tachibk` restore compatibility.
- **Themes**: Only Default (`HikariColorScheme`) and Dynamic (Monet) themes are supported; do not reintroduce upstream legacy themes.
- **Extensions**: No built-in online sources; extensions load dynamically with `source-api` compatibility.
