# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.1] - 2026-11-05

### Added

- Initial release of Hikari.
- Dedicated `.hkbackup` backup creation format.
- Backward-compatible restore support for legacy `.tachibk` backups.
- Material 3 dynamic Monet theming alongside standard default theme.
- Native ComicInfo metadata schema tags for Hikari (`hk:PublishingStatusHikari`, `hk:Categories`, `hk:SourceHikari`).
- Support for stripping `Hikari: ` prefix in extension titles alongside legacy `Tachiyomi: `.

### Changed

- Rebranded and refactored codebase from Mihon into `app.hikari`.
- Relocated database schema persistence to `hikari.db`.
- Standardized extension source API under `app.hikari.source`.
- Streamlined theme selection to Default and Dynamic (Monet) color schemes.
- Migrated default theme and color scheme tokens to `HikariColorScheme` (`@color/hikari_*`).
- Replaced legacy `Widget.Tachiyomi.*` styles with `Widget.Hikari.*`.
- Updated Samsung S-Pen Air Action action identifiers from `tachiyomi_*` to `hikari_*`.
- Switched `DirectionalViewPager`, `unifile`, and `subsampling-scale-image-view` dependencies to `HikariCollection` forks.
- Transferred repository URLs and remote endpoints to `HikariCollection/hikari`.
- Replaced legacy Tachiyomi class references in ART Ahead-of-Time baseline and startup profiles with `app.hikari`.
- Configured dedicated AniList OAuth application client credentials.
- Increased More screen branding logo header size.

### Removed

- Upstream telemetry, Firebase analytics, and proprietary crash reporting.
- Built-in updater mechanisms and background update polling services.
- Fastlane automations and upstream CI workflow configurations.
- Unused color themes (Catppuccin, Tokyo Night, Green Apple, Lavender, Midnight Dusk, Monochrome, Nord, Strawberry Daiquiri, Tako, Teal Turquoise, Tidal Wave, Yin & Yang, and Yotsuba).
- Legacy Tachiyomi XML tags and fallback deserialization from ComicInfo.
