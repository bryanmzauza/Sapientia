# Changelog

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and SemVer.

## [1.0.0-beta] — Java MVP polish ⚡

Sixth milestone. Release-grade quality: continuous benchmarking and a
regression gate so performance targets from `docs/performance-contract.md`
stay honest between releases.

### Added
- `sapientia-benchmarks` module with a JMH harness. Run via
  `./gradlew :sapientia-benchmarks:jmh` (T-170).
  - `NetworkGraphBenchmark.buildGraph500Nodes` — 500-node energy graph
    rebuild, covering P-003 at the graph layer (current: ~78 µs/op, budget
    5 ms).
  - `TickBucketBenchmark.dispatchOneBucket` — 20 000 tickables across 20
    buckets, covering P-007 (current: ~1.3 µs/op, budget 250 µs/bucket).
- `compareToBaseline` Gradle task (T-171): reads the latest JMH JSON report
  and fails the build when any benchmark regresses more than 10 % versus
  `docs/benchmarks/baseline.json`. Missing-from-baseline benchmarks are
  informational.
- `saveBenchmarkBaseline` Gradle task: promotes the latest JMH result to the
  committed baseline.
- `TickBucketing#runOneTickForBenchmark()` — benchmark-only entry point
  around `runOneTick()` so JMH can exercise the dispatcher without wiring a
  Paper scheduler.
- README "Running benchmarks" section (T-172).

### Notes
- Solver-level bench (`EnergySolver.tick`) deferred until the Bukkit
  event-bus dependency is mockable; P-003 coverage retained at the graph
  layer.
- P-006 `WriteBehindQueue` bench deferred (requires in-memory DataSource
  harness).
- JMH's annotation processor emits JDK-25 bytecode, so the `:jmh` `JavaExec`
  task pins its launcher to the Java 25 toolchain.

## [0.5.0] — YAML overrides & resource pack 🎛️ ✅

Fifth milestone. Operators can retune the Java-defined catalog at runtime via
three focused YAML files and ship a Java resource pack without touching the
plugin code.

### Added
- `sapientia-api`: `ItemOverride`, `BlockOverride`, `RecipeOverride` records and
  the `ContentOverrides` service (with `ReloadReport`). Exposed via new
  `SapientiaAPI#overrides()` (T-160 / 0.5.0).
- `sapientia-core`: `ContentOverrideService` reads `plugins/Sapientia/overrides/
  items.yml|blocks.yml|recipes.yml`, validates materials/keys/result amounts and
  publishes an atomic snapshot; invalid rows are logged and skipped (T-161).
  `ItemRegistry#createStack` and `SapientiaRecipeRegistry#effectiveResult`
  consult the snapshot on every lookup so reloads are immediate.
- `/sapientia reload content` hot-reload (permission
  `sapientia.command.reload`) with per-file counts and a summary of any
  validation issues (T-162).
- `sapientia-core`: `ResourcePackBuilder` seeds `pack.mcmeta` +
  `assets/sapientia/` in `plugins/Sapientia/pack/` and zips to
  `sapientia-resources.zip`; `/sapientia pack build java` surfaces it to
  operators (permission `sapientia.command.pack`) (T-164).
- Config: `resource-pack.pack-format` (default `32`) — adjustable per MC
  version.
- i18n: `command.help.desc.reload-content`, `command.help.desc.pack-build`,
  `command.reload.content.success`, `command.reload.content.issues`,
  `command.pack.usage|success|failure` in `en` and `pt_BR`.
- Tests: `ContentOverrideServiceTest` (6 parser cases covering success,
  unknown material, invalid key, block override, recipe result override, and
  rejection of invalid `result_amount`).

## [0.4.0] — Crafting & Guide 📖

Fourth milestone. Adds the shaped crafting mechanic (3×3 shape-exact matcher,
dedicated workbench UI) and the in-game guide with per-player unlocks.

### Added
- `sapientia-api`: sealed `RecipeIngredient` (`Vanilla` / `Sapientia` / `Empty`),
  `SapientiaRecipe` interface, `RecipeRegistry` with `openWorkbench(Player)`,
  `SapientiaRecipeCompleteEvent`, `GuideCategory` enum, `GuideEntry` record,
  `GuideService`, `UnlockService`. `SapientiaItem#guideCategory()` and
  `SapientiaBlock#guideCategory()` defaults. `SapientiaAPI#recipes()`,
  `#guide()` and `#unlocks()` entry points (T-131 / T-132 / T-150 / T-151).
- `sapientia-core`: `SapientiaRecipeRegistry` with pure `matchCells(...)` entry
  point for tests (T-131). `WorkbenchHolder` + `WorkbenchListener` implementing
  the 54-slot crafting window — grid at slots {10,11,12,19,20,21,28,29,30},
  output at 24, filler glass panes elsewhere, auto-return of grid on close
  (T-130). `GuideServiceImpl` auto-populates entries from the item and block
  registries and renders a category-sorted Java inventory with grey-pane
  placeholders for locked entries (T-150). `UnlockServiceImpl` backed by
  migration `V004__unlocked_content.sql` with an in-memory per-player cache
  (T-151). Workbench auto-unlocks each recipe on craft.
- `sapientia-content`: `SapientiaWorkbench` block (CRAFTING_TABLE) routing
  `onInteract` to `api.recipes().openWorkbench(player)`. `SapientiaGuide` item
  (WRITTEN_BOOK) opening the guide via `api.guide().open(player)`. Three
  reference recipes (`recipe_wrench`, `recipe_cable`, `recipe_generator`) with
  mixed vanilla + Sapientia outputs.
- i18n: `item.guide.name` / `item.guide.lore`, `block.workbench.name`,
  `guide.title`, `guide.locked` in `en` and `pt_BR`.
- Tests: `SapientiaRecipeRegistryTest` (7 cases: exact shape, missing
  ingredient, translated pattern, extra item in empty cell, Sapientia-tagged
  stack against vanilla cell, first-match precedence, duplicate-id rejection).
  `MigrationLoaderTest` updated for V004.

## [0.3.0] — Energy 🔌

Third milestone. Introduces the energy graph: nodes (generator / cable / capacitor /
consumer), connected-component networks with split/merge, a per-tick proportional
solver, persistence of node state and the events addons need to react to flow.

### Added
- `sapientia-api`: `EnergyService`, `EnergyNetwork`, `EnergySpecs` (capacity / generation
  / consumption table). `EnergyNode.tier()` default accessor. New events
  `SapientiaEnergyFlowEvent` and `SapientiaMachineTickEvent`. `SapientiaAPI#energy()`
  entry point (T-100/T-146).
- `sapientia-core`: `NetworkGraph` with 6-neighborhood adjacency, BFS split-on-removal
  and merge-on-add (T-141). `SimpleEnergyNode` with atomic buffers and dirty flag.
  `EnergyNodeStore` (SQLite-backed CRUD over `energy_nodes`). `EnergyServiceImpl`
  with chunk hydrate / unload hooks. `EnergySolver` greedy proportional pass that
  runs every 10 server ticks and emits `SapientiaEnergyFlowEvent` per network
  (T-142).
- Migration `V003__energy_nodes.sql` (T-140).
- `sapientia-content`: `EnergyContentBlock` base class plus the four reference blocks
  `SapientiaGenerator`, `SapientiaCable`, `SapientiaCapacitor`, `SapientiaConsumer`
  (T-143). i18n keys `block.generator/cable/capacitor/consumer.name` in `en` and
  `pt_BR`.
- Tests: `NetworkGraphTest` (6 cases covering merge / split / diagonal-isolation /
  offer / draw). MigrationLoader tests updated for V003.

### Deferred
- T-144 Kryo serialisation of node `state_blob` — current schema persists buffers
  as columns; revisit when machines need richer snapshot data.
- T-145 In-game energy bar UI — depends on `UIService.open(NamespacedKey)` being
  exposed through `SapientiaAPI`. Tracked under 0.4.0.

## [0.2.0] — Items & Blocks

Second milestone. Introduces the Slimefun-style fixed-Java-class content model
(ADR-012), the block lifecycle pipeline and the asynchronous persistence layer.

### Added
- `sapientia-api`: `SapientiaItem` and `SapientiaBlock` interfaces with default
  behavior hooks; events `SapientiaItemInteractEvent`, `SapientiaBlockPlaceEvent`,
  `SapientiaBlockBreakEvent`, `SapientiaBlockInteractEvent`.
- `SapientiaAPI` registry surface: `registerItem`, `registerBlock`, `findItem`,
  `findBlock`, `createStack` (T-100b).
- `sapientia-core`: `SapientiaBlockRegistry`, `ChunkBlockIndex` (chunk-scoped
  hydration), `BlockLifecycleListener` (place/break/interact pipeline with event
  emission and vanilla-drop suppression), `WriteBehindQueue` (500 ms async flush
  with last-write-wins dedup, single worker thread) — T-105, T-111, T-112.
- Root Gradle task `verifyTranslations` diffing `en.yml` vs `pt_BR.yml`, wired
  into `check` (T-105 CI gate).
- ArchUnit gate `NoUserFacingLiteralsTest` with escape annotation `@AllowLiteral`
  (T-106).
- `sapientia-content`: first demo content — `SapientiaWrench`, `SapientiaPedestal`,
  `SapientiaConsole` — plus `ContentBootstrap` invoked from `SapientiaPlugin`
  (T-180).
- i18n keys `item.wrench.*`, `block.pedestal.*`, `block.console.*` in `en.yml`
  and `pt_BR.yml`.

### Changed
- ADR-012: content is fixed Java classes Slimefun-style; YAML moves to
  override-only for operators (0.5.0). Architecture §3/§5/§10, implementation
  plan block 7 and module breakdown §2.3 updated; ROADMAP 0.2.0 expanded and
  0.5.0 rewritten as "YAML overrides & resource pack".
- `CustomBlockStore` now routes writes through `WriteBehindQueue` when attached;
  `loadChunk` returns keyed records for hydration.
- `ItemRegistry` accepts `SapientiaItem` registrations (bridged internally so
  `/sapientia give` continues to work for both APIs).

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
