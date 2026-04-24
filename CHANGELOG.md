# Changelog

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and SemVer.

## [0.1.0] — Foundation

First tagged release. Published by **BRMZ.dev** (<https://brmz.dev>). Establishes the multi-module skeleton, core runtime services and the public API surface consumed by addons.

### Added
- Gradle multi-module layout (6 modules) with convention plugins and version catalog.
- `sapientia-api`: `Machine`, `EnergyNode`, enums (`MachineCategory`, `EnergyNodeType`, `EnergyTier`, `PlatformType`), records (`MachineState`, `RecipeProgress`), `Version`, and `SapientiaPlayerPlatformDetectEvent`.
- `Messages` with MiniMessage + `en` and `pt_BR` catalogs.
- `DatabaseManager` (HikariCP + SQLite WAL) and `MigrationLoader` with SHA-256 checksum.
- `SapientiaScheduler` adapting Paper and Folia.
- `PlatformService` with Floodgate detection (reflection) + persistent cache; `SapientiaPlayerPlatformDetectEvent` fired on player join.
- `ItemRegistry` PDC + `/sapientia give` / `reload` / `help` commands.
- `UIService` + `JavaInventoryUIProvider` + `BedrockFormsUIProvider` stub.
- `TickBucketing` with 20 rotating buckets.
- `CustomBlockStore` with upsert CRUD over `custom_blocks`.
- Root task `buildPluginJar` producing the distributable shaded jar.

### Changed
- Build targets Java 25 and Minecraft / Paper 26.1.2 (alpha build).
- Package root renamed to `dev.brmz.sapientia` and shaded library prefix to `dev.brmz.sapientia.libs`.
- `plugin.yml` author is `BRMZ.dev`; project coordinate `dev.brmz.sapientia:sapientia-*`.
