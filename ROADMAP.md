# Roadmap

Per-release plan for Sapientia. Each entry lists scope, exit criteria and the tasks (`T-NNN`) that compose it.

Status legend:
- ✅ done
- 🚧 in progress
- ⏳ planned

> Scope changes between versions follow SemVer. Breaking changes in `sapientia-api` happen only on major bumps.

---

## 0.1.0 — Foundation ✅

**Goal.** Buildable project, multi-module layout and the minimum runtime to load on Paper.

- ✅ T-000 Repository + Gradle multi-module layout (6 modules)
- ✅ T-001 Convention plugins under `buildSrc/`
- ✅ T-002 Version catalog `libs.versions.toml`
- ✅ T-003 Root task `buildPluginJar` producing the shaded plugin artifact
- ✅ T-004 `plugin.yml` + `softdepend: [floodgate]` (author `BRMZ.dev`)
- ✅ T-100 `sapientia-api`: `Machine`, `EnergyNode`, enums (`MachineCategory`, `EnergyNodeType`, `EnergyTier`), records (`MachineState`, `RecipeProgress`), `PlatformType`, `Version`, `SapientiaPlayerPlatformDetectEvent`
- ✅ T-104 Messages API + `en.yml` / `pt_BR.yml` catalogs (MiniMessage)
- ✅ T-110/T-111 Migration loader with SHA-256 + `DatabaseManager` (HikariCP + SQLite WAL)
- ✅ T-115 `SapientiaScheduler` (Paper/Folia via reflection)
- ✅ T-120/T-121 Platform detection via Floodgate + SQLite cache + event emission on join
- ✅ T-101/T-103 `ItemRegistry` PDC + `/sapientia give` / `reload` / `help` commands
- ✅ T-122/T-123 `UIService` + `JavaInventoryUIProvider` + `BedrockFormsUIProvider` stub
- ✅ T-112/T-114 `CustomBlockStore` + `TickBucketing` (20 rotating buckets)
- ✅ T-199 Package root under `dev.brmz.sapientia` and BRMZ.dev authorship wired into plugin metadata

**Exit gate:** `./gradlew build buildPluginJar` green, 6 tests passing, shaded jar produced at `sapientia-core/build/libs/sapientia-core-0.1.0-SNAPSHOT.jar`, plugin loads cleanly on Paper 26.1.2.

---

## 0.2.0 — Items & Blocks ✅

**Goal.** Full custom item + persistent custom block lifecycle, plus the strategic pivot to fixed Slimefun-style content (ADR-012).

- ✅ ADR-012 Conteúdo fixo em Java (estilo Slimefun), não declarativo em YAML
- ✅ T-100b `sapientia-api`: `SapientiaItem`, `SapientiaBlock` abstractions (first-class content API)
- ✅ T-102 `SapientiaItemInteractEvent` + generic listener
- ✅ T-105 CI gate `verifyTranslations` (en/pt_BR key parity)
- ✅ T-106 ArchUnit lint blocking user-facing literals (`@AllowLiteral` escape)
- ✅ T-113 `SapientiaBlockPlaceEvent` / `SapientiaBlockBreakEvent` / `SapientiaBlockInteractEvent`
- ✅ T-112 (finalization) block load/unload via `ChunkLoadEvent` / `ChunkUnloadEvent` with in-memory chunk index
- ✅ T-111 (advanced) Async write-behind for `custom_blocks` with 500 ms batching, last-write-wins dedup
- ✅ T-180 Built-in demo catalog in `sapientia-content` (wrench item, pedestal block, console block)

**Exit gate (met):** `./gradlew build verifyTranslations buildPluginJar` green; 8 tests passing including the ArchUnit literal gate; demo catalog registers on enable; `verifyTranslations` confirms 22 keys aligned between `en` and `pt_BR`.

**Deferred to later milestones:**
- JMH throughput benchmarks for `WriteBehindQueue` → folded into T-170 (1.0.0-beta).
- `UIService.open(NamespacedKey)` overload for blocks like `SapientiaConsole` → T-145 (0.3.0).

---

## 0.3.0 — Energy 🔌 ✅

**Goal.** First working energy system: nodes (generator/cable/capacitor/consumer), network graph with split/merge, per-tick distribution, persistence and events.

- ✅ T-140 Migration V003 `energy_nodes` (one row per node; networks recomputed at runtime from adjacency)
- ✅ T-141 In-memory `NetworkGraph` with connected-components, split-on-removal and merge-on-add
- ✅ T-142 Greedy proportional energy solver (simplified — full Ford-Fulkerson hardening tracked for 1.1.0 logistics work)
- ✅ T-143 Four reference blocks in `sapientia-content` (`SapientiaGenerator`, `SapientiaCable`, `SapientiaCapacitor`, `SapientiaConsumer`)
- ⏳ T-144 Kryo `state_blob` serialization for node state (nice-to-have; current schema persists buffers as columns)
- ⏳ T-145 Energy machine UI (energy bar + start/stop) — depends on `UIService.open(NamespacedKey)` API
- ✅ T-146 `SapientiaEnergyFlowEvent` + `SapientiaMachineTickEvent`

**Exit gate:** generators feed consumers through cables across server restarts; `NetworkGraph` correctness verified by unit tests; `./gradlew build` green; CHANGELOG `[0.3.0]` written. Performance gate P-003 (≤ 5 ms per tick on a 500-node graph) is tracked but not blocking for this milestone — formal benchmark lands with T-170.

---

## 0.4.0 — Crafting & Guide ✅

**Goal.** Recipe mechanic and in-game navigable guide.

- ✅ T-130 Custom workbench (3×3 grid, stateless; machine state/timers deferred to 1.2.0)
- ✅ T-131 Hardcoded recipe parser (shape-exact 3×3 matcher, vanilla and Sapientia ingredients)
- ✅ T-132 `SapientiaRecipeCompleteEvent` (cancellable)
- ✅ T-150 Book/GUI navigable by category (Java inventory, flat view grouped by `GuideCategory`)
- ✅ T-151 Simple unlock/progression (SQLite `unlocked_content` table + cached `UnlockService`; recipes auto-unlock on craft)

**Exit gate:** three working recipes (wrench, cable, generator) + guide lists every registered item and block; locked entries render as grey panes.

---

## 0.5.0 — YAML overrides & resource pack ✅

**Goal.** Server operators can rebalance the built-in Java catalog (energy costs, recipe timings, display names) via YAML files and ship a resource pack. No new content is declared in YAML — content is always Java (ADR-012).

- ✅ T-160 YAML override schema + validator with actionable messages (`items.yml` / `blocks.yml` / `recipes.yml`, material/display-name/lore/result-amount)
- ✅ T-161 Override loader applied over registered `SapientiaItem` / `SapientiaBlock` / `SapientiaRecipe` through the new `ContentOverrides` API
- ✅ T-162 Hot-reload via `/sapientia reload content` (atomic snapshot swap, no restart)
- ✅ T-164 Java resource pack pipeline — seeded `pack.mcmeta` + `/sapientia pack build java` producing `sapientia-resources.zip`; the full `ItemModel` generation loop (custom-model-data) remains open until the item API exposes CMD

**Exit gate:** example override files are seeded under `plugins/Sapientia/overrides/` on first run and retune the built-in catalog without restart; invalid overrides never crash the plugin, only warn and fall back. Resource pack command produces a playable zip.

---

## 1.0.0-beta — Java MVP polish ✅

**Goal.** Release-grade quality: benchmarks, CI regression, docs.

- ✅ T-170 `sapientia-benchmarks` + initial JMH (P-003 via `NetworkGraphBenchmark`, P-007 via `TickBucketBenchmark`; P-006 WriteBehindQueue deferred — see note below)
- ✅ T-171 `compareToBaseline` Gradle task + `saveBenchmarkBaseline` promote flow (regression > 10 % blocks merge)
- ✅ T-172 CHANGELOG `[1.0.0-beta]` entry + README benchmark instructions

**Exit gate:** `./gradlew :sapientia-benchmarks:jmh` green; `docs/benchmarks/baseline.json` seeded; `compareToBaseline` passes; plugin published as `1.0.0-beta`.

**Notes.** Solver-level benchmark (`EnergySolver.tick`) deferred until the Bukkit event-bus dependency is mockable; P-003 coverage retained at the graph layer. P-006 `WriteBehindQueue` bench deferred (requires in-memory DataSource harness).

---

## 1.0.0 — Bedrock parity 📱 ✅

**Goal.** Bedrock players (Geyser/Floodgate) get an equivalent experience to Java.

- ✅ T-201 `SimpleForm` / `ModalForm` / `CustomForm` wrappers (`sapientia-bedrock.forms`, typed Cumulus API)
- ✅ T-202 Machine UI on Bedrock (`MachineBedrockRenderer` + `CustomForm`)
- ✅ T-203 Filter UI stub (`FilterDescriptor`, gated by `experimental.filter` flag)
- ✅ T-204 Guide UI Bedrock renderers (index + detail SimpleForm)
- ✅ T-205 `TextAdapter.toPlainBedrock(Component)`
- ✅ T-205b Bedrock `.lang` catalogs derived from `en.yml` / `pt_BR.yml` (`LangFileWriter`)
- ✅ T-206 Java-only fallback → auto-generated `SimpleForm` (`BedrockFallbackForm`)
- ✅ T-207 `.mcpack` pipeline + manifest (`ResourcePackBuilder.buildBedrockPack`, `BedrockPackConstants`)
- ✅ T-208 Geyser item mappings (`GeyserMappingsBuilder`)
- ✅ T-209 `/sapientia pack build bedrock|all`
- ✅ T-210 Bedrock smoke script (`scripts/smoke-bedrock.{sh,ps1}`) + checklist (`docs/bedrock-smoke-checklist.md`)
- ✅ T-211 Benchmarks P-009..P-012 + P-014 (`PlatformDetectBenchmark`, `CustomFormOpenBenchmark`, `BedrockMixOverheadBenchmark`, `GeyserMappingBenchmark`)

**Exit gate:** Bedrock smoke checklist passes; Geyser/Floodgate compatibility matrix published.

---

## 1.1.0 — Item logistics ✅

- ✅ T-300a Public logistics API (`ItemNode`, `ItemNetwork`, `ItemService`, `ItemFilterRule`, `ItemRoutingPolicy`, events)
- ✅ T-300b `ItemNetworkGraph` (6-neighbour BFS, split-on-removal, merge-on-add)
- ✅ T-300c V005 migration + `ItemNodeStore` (chunk-indexed, rule-cascade)
- ✅ T-300d `ItemSolver` per-tick greedy round-robin / priority / first-match
- ✅ T-300e `ItemFilterRuleMatcher` (glob `*`, `namespace:*`, exact)
- ✅ T-300f Demo blocks: `SapientiaItemCable` / `Producer` / `Consumer` / `Filter` (+ recipes)
- ✅ T-300g `FilterDescriptor` (Java + Bedrock) wired to `ItemNode` context (replaces the 1.0.0 experimental stub; `experimental.filter` flag removed)
- ✅ T-300h `/sapientia logistics info|policy|filter` with full tab-complete + i18n
- ✅ T-300i `SapientiaPlugin` wiring, chunk hydrate/unload, per-tick scheduler
- ✅ Round-robin / priority / first-match policies (per-network)
- ✅ Events: `SapientiaItemFlowEvent`, `SapientiaItemFilterEvent` (cancellable), `SapientiaItemRouteEvent`
- ⏳ Ford-Fulkerson hardening (deferred to 1.4.0+)

**Exit gate:** `./gradlew build verifyTranslations` green with `ItemNetworkGraphTest` and updated `MigrationLoaderTest` passing.

---

## 1.2.0 — Fluids 💧 ✅

- ✅ T-301a Public fluids API (`FluidNode`, `FluidNetwork`, `FluidService`,
  `FluidType`, `FluidStack`, `FluidNodeType`, `FluidSpecs`, events)
- ✅ T-301b V006 migration — `fluid_nodes` table (chunk-indexed)
- ✅ T-301c `FluidNetworkGraph` — 6-neighbour BFS, split-on-removal,
  merge-on-add (port of `ItemNetworkGraph`)
- ✅ T-301d `FluidSolver` — per-network pump → tank → drain pipeline,
  per-tier throughput cap, no-mixing tank semantics
- ✅ T-301e `AdjacentFluids` — vanilla bridge for water/lava blocks and
  water/lava cauldrons (read & write)
- ✅ T-301f `BuiltinFluidTypes` — Java-declared `sapientia:water`,
  `sapientia:lava`, `sapientia:milk` (supersedes the previous "fluid types
  declarable via YAML" line; see ADR-016)
- ✅ T-301g Content blocks — `SapientiaFluidPipe`, `SapientiaFluidPump`,
  `SapientiaFluidTank`, `SapientiaFluidDrain` + bundled recipes
- ✅ T-301h `/sapientia fluids info` command + en/pt_BR i18n keys
- ✅ T-301i Tests — `FluidNetworkGraphTest` (graph + capacity + no-mixing),
  V006 migration coverage
- ✅ ADR-015 (no fluid mixing per tank), ADR-016 (Java-declared fluid registry)

---

## 1.3.0 — Programmable logic ✅

- ✅ T-302a `LogicProgram` / `LogicNode` / `LogicEdge` / `LogicValue` records in `sapientia-api`
- ✅ T-302b `LogicService` + `SapientiaLogicTickEvent` exposed via `SapientiaAPI#logic()`
- ✅ T-302c `LogicCompiler` — Kahn's-algorithm topological sort with deterministic lex tie-break, cycle / unknown-node / port-collision rejection
- ✅ T-302d `LogicRunner` — single-pass DAG evaluator bounded by `O(|V|+|E|)`
- ✅ T-302e Built-in node kinds — `constant`, `add`/`sub`/`mul`, `compare`, `and`/`or`/`not`, `branch`, `memory_read`/`memory_write`, `tick_counter`, `log`, `noop`
- ✅ T-302f `LogicYaml` round-trip serialiser (snakeyaml) covering all `LogicValueType`s
- ✅ T-302g `LogicProgramStore` + V007 migration (`logic_programs` table, idempotent upsert)
- ✅ T-302h `LogicServiceImpl` registers + persists + ticks every 5 ticks via `SapientiaScheduler`, fires cancellable `SapientiaLogicTickEvent`, auto-disables programs that throw
- ✅ T-302i `/sapientia logic <list|info|load|unload|enable|disable|export|tick>` command + `sapientia.command.logic` permission + tab completion
- ✅ T-302j i18n — full `command.logic.*` tree in `en.yml` + `pt_BR.yml` (verifyTranslations green)
- ✅ T-302k Tests — `LogicCompilerTest`, `LogicRunnerTest`, `LogicYamlTest`, `MigrationLoaderTest` updated for V007
- ⏳ T-302l Visual editor canvas — read-only inspection lands here via `/sapientia logic info`; full drag-and-drop editor is deferred to 1.9.0 where androids actually consume DAGs

**Exit gate:** ✅ DAG runtime in `sapientia-core` consumes rule definitions, executes deterministically inside the tick bucketing budget (every 5 ticks), and gates the android workflows planned for 1.9.0. Verified by `./gradlew verifyTranslations test` (51/51 passing).

---

## Content milestones (1.4.0 — 2.1.0)

The next eight releases roll out the full content catalogue defined in [docs/content-catalog.md](docs/content-catalog.md). Each milestone delivers a vertical slice (items + machines + recipes + UI + i18n + Bedrock parity) and unlocks the next tier. Numerical specs (SU/t, throughput, recipe times) ship as `docs/content-spec-T-4xx.md` per milestone, before any implementation PR.

Cross-cutting rules for every milestone below:
- All new content is declared in Java under `sapientia-content` (ADR-012).
- Every machine has a Java inventory UI **and** a Bedrock `CustomForm` renderer (parity gate).
- Every user-facing string lands in both `en.yml` and `pt_BR.yml` (`verifyTranslations` blocks merge).
- New blocks register through existing `EnergyService` / `ItemService` / `FluidService` — no new core subsystem unless a milestone explicitly calls one out.

---

## 1.4.0 — Metallurgy & MV tier ✅

**Goal.** Ship the bronze-to-steel pipeline and unlock MV. Establishes the tier model end-to-end (LV ⇆ MV transformer, MV cables, MV machines).

- ✅ T-400 Tier framework — `MachineTier` enum (LV/MV/HV/EV) in `sapientia-api/machine`, `TierCompatibility` policy helper (ALLOW/CLAMP/BURN), shared `machine_casing` + `machine_casing_mv` casing blocks
- ⏳ T-401 New ores (world-gen) — **deferred to 1.4.1** (catalog ships items + recipes; world-gen requires a `WorldGenerator` integration that is out of scope for the i18n/UX-focused 1.4.0 release; raw metals are obtainable today via `/sapientia give` and recipes)
- ✅ T-402 Item families — `_raw` / `_dust` / `_ingot` / `_block` / `_plate` / `_wire` / `_rod` / `_gear` / `_screw` for the six metals (54 items via `MetalCatalog`)
- ✅ T-403 Alloys (LV) — bronze (3 copper + 1 tin), brass (3 copper + 1 zinc), electrum (2 silver + 2 nickel) — registered as 3 alloys × 8 forms = 24 items
- ✅ T-404 LV/MV machines — `macerator` (LV), `ore_washer` (LV), `electric_furnace` (LV), `mixer` (MV), `compressor` (MV), `bench_saw` (LV), `plate_press` (MV), `extractor` (MV) all registered as `MachineEnergyBlock` consumer nodes with the standard machine UI; recipe-processing tick logic deferred to 1.4.1 (machines accept/burn energy today and react to running registry toggles)
- ✅ T-405 Multiblock — `MultiblockShapeValidator` helper in `sapientia-api/multiblock` (`validateSolidCube` / `validateHollowCube`); `induction_furnace_controller` registered, validates a 3×3×3 shell of `SMOOTH_BASALT` / `POLISHED_BLACKSTONE` on interact (steel/invar/kanthal recipe wiring deferred to 1.4.1)
- ✅ T-406 Energy expansion — `cable_t2`, `capacitor_t2`, `transformer_lv_mv` (single MV-tier capacitor node — paired-tier transformer model lands in 1.5.0 when MV consumers consume non-trivially)
- ✅ T-407 ADR — voltage incompatibility policy: source > target → BURN; source < target → CLAMP; equal → ALLOW. Codified in `TierCompatibility.check`. Full prose ADR doc deferred (decision is captured in code + Javadoc; see `docs/content-spec-T-4xx.md`)
- ✅ T-408 Content spec doc + recipes + guide entries — `docs/content-spec-T-4xx.md`; 50+ new recipes in `MetallurgyRecipes` (ore→dust, dust→ingot, ingot→block, alloys, plates, wires, casings, transformer, MV cable/capacitor, 8 machine recipes, induction furnace controller)
- ✅ T-409 Tests — `MachineTierTest`, `TierCompatibilityTest`, `MultiblockShapeValidatorTest`, `MetalCatalogTest`
- ⏳ T-410 Benchmark P-015 — **deferred to 1.4.1** (gated on T-401/T-404 recipe-tick implementation; the throughput surface is currently zero for machines)

**Exit gate.** Achieved for the 1.4.0 scope: i18n green (241 keys aligned across en + pt_BR), full 78-item metallurgy catalog registered, MV-tier energy blocks place/break/persist via `EnergyService`, all 8 machine blocks register and open the standard machine UI, transformer block bridges to MV-tier networks, multiblock validator + induction furnace controller validate shape on interact, `./gradlew build verifyTranslations` BUILD SUCCESSFUL.

**Rolled forward to 1.4.1:**
- T-401 ore world-gen + Y-layer placement
- T-404 recipe-tick processing inside machines (currently machines drain energy but do not yet pop output stacks)
- T-405 induction furnace recipe processing for steel/invar/kanthal
- T-410 P-015 benchmark
- Full ADR-017 prose doc (decision is final; only the formal write-up is deferred)
- Full Bedrock smoke checklist run for the 11 new MV blocks

---

## 1.4.1 — Kinetic loop polish ✅

**Goal.** Finalise the deferred items from 1.4.0 so machines stop being energy-burning stubs.

- ✅ T-404 Recipe-tick — new `MachineProcessor` (sapientia-core) ticks every 10 game ticks: scans every CONSUMER energy node, looks up the SapientiaBlock kind via `ChunkBlockIndex`, finds a matching `MachineRecipe` from the chest above, drains energy, deposits the output into the chest below. Energy drains only on completion; partial progress is in-memory only (server restart rolls back).
- ✅ T-404 Recipe data — `MachineRecipeData` (sapientia-content) registers ~40 recipes: per-raw-metal `raw → 2× dust` (macerator), `dust → ingot` (electric_furnace), `ingot → plate` (plate_press), `ingot → 4× wire` (extractor), `ingot → rod` (bench_saw), `9× ingot → block` (compressor), plus mixer alloy stubs and chemistry bridges.
- ✅ T-405 Induction-furnace recipes — alloy steel/invar/kanthal recipes register through `MachineRecipeData` against the `induction_furnace_controller` block; the existing 3×3×3 shape validator gates execution.
- ⏳ T-401 Ore world-gen — **rolled forward again to 1.6.0** alongside aluminum/silicon/titanium/lithium so a single `WorldGenerator` integration can ship all raw metals + electronics ores together.
- ⏳ T-410 Benchmark P-015 — `MachineProcessorBenchmark` placeholder; full JMH harness lands in the 1.6.0 perf-pass.
- ✅ ADR-017 Voltage incompatibility — short prose write-up appended to `docs/decision-log.md` (decision was finalised in 1.4.0; this milestone formalises it).
- ✅ Bedrock smoke — checklist updated in `docs/bedrock-smoke-checklist.md` for the 11 MV-tier blocks (smoke run is manual).

**Exit gate.** `gradlew build verifyTranslations` BUILD SUCCESSFUL with the new tests; macerator + electric_furnace produce dust + ingots from chest-above → chest-below in a manual end-to-end test (Java client).

---

## 1.5.0 — Petroleum & basic chemistry ✅

**Goal.** Bring liquid fuels online; introduce the first multi-output multiblock and the first "process plant".

- ✅ T-411 New fluids (catalogue §12.1) — `crude_oil`, `diesel`, `gasoline`, `lubricant`, `nutrient_broth` registered through `BuiltinFluidTypes`
- ⚠️ T-412 Geo-extraction — `pumpjack` block registered as MV consumer + crafting recipe; chunk-noise reservoir gen + depletion deferred to 1.5.1
- ⚠️ T-413 Multiblock — `oil_refinery_controller` registered with 5×5×7 hollow-shell shape validation (new `MultiblockShapeValidator.validateHollowBox`); refinery yield tick deferred to 1.5.1
- ⚠️ T-414 Machines — `cracker`, `fermenter`, `still`, `bioreactor` registered with crafting recipes; recipe-tick processing deferred to 1.5.1 (mirrors 1.4.0 deferral pattern)
- ⚠️ T-415 Combustion — `combustion_gen` (MV) and `biogas_gen` (LV) registered as energy GENERATOR nodes; fluid-fuel consumption logic deferred to 1.5.1
- ⚠️ T-416 ADR — replenishment-policy decision captured inline in `docs/content-spec-T-41x.md` (chunk-decay = slow-regen finite); full prose ADR-018 deferred to 1.5.1
- ✅ T-417 Content spec + recipes + guide entries — `docs/content-spec-T-41x.md` + 9 new recipes
- ✅ T-418 Tests — `BuiltinFluidTypesTest` (3 cases), `MultiblockHollowBoxTest` (3 cases); refinery-yield + combustion-energy-balance unit tests deferred to 1.5.1 with the recipe-tick implementation
- ⏳ T-419 Benchmark P-016 — deferred to 1.5.1 (no recipe-tick yet to benchmark)
- ⏳ T-420 Bedrock parity smoke — deferred to 1.5.1 (full smoke run after recipe-tick lands)

**Exit gate (vertical-slice).** Petroleum/chemistry blocks register, place, open the standard machine UI, and feed energy networks. The full crude→diesel→combustion vertical slice ships in 1.5.1 when recipe-tick processing arrives. Aligned i18n parity (en/pt_BR), `verifyTranslations` green.

---

## 1.5.1 — Petroleum kinetic loop ✅

**Goal.** Bring the 1.5.0 deferrals home so the petroleum vertical slice (crude → diesel → combustion → energy) actually runs.

- ✅ T-412 Geo-extraction — new `ReservoirService` + `V008__crude_oil_reservoirs.sql` migration. Every queried chunk receives a deterministic [10 000 .. 100 000] mB reserve (FNV-1a-mixed seed); pumpjacks drain 50 mB/cycle and the reservoir slowly regenerates at 1 mB/min, capped at the original reserve. Persisted synchronously through the existing `DataSource`.
- ✅ T-413 Refinery yield — `PetroleumTicker.tickRefinery()` validates the 5×5×7 stainless-steel shell every cycle, drains 100 mB crude from the input tank above, emits 40 mB diesel (north), 30 mB gasoline (east), 20 mB lubricant (south) and 10 mB residual water (west — placeholder for tar). Capacity preflight ensures all four outputs accept their share before any crude is consumed; partial draws are impossible.
- ✅ T-414 Chemistry recipes — `cracker`, `fermenter`, `still`, `bioreactor` participate in the `MachineProcessor` recipe-tick (item recipes registered through `MachineRecipeData`). Fluid recipes for these machines are tracked through `PetroleumTicker` instead.
- ✅ T-415 Combustion — `combustion_gen` consumes 5 mB/cycle of diesel (40 SU/mB) or gasoline (50 SU/mB) from the tank below; `biogas_gen` consumes 10 mB/cycle of nutrient_broth at 8 SU/mB. Each cycle deposits energy into the generator's own buffer up to `bufferMax`.
- ✅ T-416 ADR — full prose ADR-018 (reservoir replenishment policy = chunk-decay slow-regen finite) appended to `docs/decision-log.md`.
- ✅ T-418/T-419 Tests + benchmark hooks — `ReservoirServiceTest` (5 cases) covers determinism + drain + cap-at-zero; `MigrationLoaderTest` extended for V008. P-016 benchmark is set up as a placeholder; full JMH integration lands with the 1.6.0 performance pass.
- ✅ T-420 Bedrock parity — checklist `docs/bedrock-smoke-checklist.md` extended with petroleum + chemistry block coverage.

**Exit gate.** `gradlew build verifyTranslations` BUILD SUCCESSFUL with the new tests; pumpjack → refinery → combustion_gen → energy network produces measurable SU/min in a manual smoke test on the Java client.

---

## 1.6.0 — Electronics & HV ✅

**Goal.** Unlock processors, electrolysis and the HV tier — the foundation for everything endgame.

- ⏳ T-421 New ores — `aluminum` (bauxite), `silicon`, `titanium`, `lithium` (catalogue §5.1) — *catalogue ✅, world-gen deferred (mirrors T-401)*
- ✅ T-422 Component progression — `motor_t1..t3`, `circuit_t1..t3`, `processor_t1..t3`, `coil_t1..t3`, `ram_t2/t3`, `storage_hdd/ssd` (17 components, `ComponentCatalog`)
- ✅ T-423 Machines — `electrolyzer`, `rolling_mill`, `laser_cutter`, `chemical_reactor` (4 HV machines, all `MachineEnergyBlock` consumers)
- ✅ T-424 Alloys (HV) — `stainless_steel`, `damascus_steel`, `nichrome` (3 alloys × 8 forms = 24 items)
- ✅ T-425 Energy — `cable_t3`, `capacitor_t3`, `transformer_mv_hv`, `geothermal_gen`, `gas_turbine`, `rtg` (HV cable/cap/transformer + 3 generators wired into `EnergyService`)
- ✅ T-426 New gases — `hydrogen`, `oxygen_gas`, `nitrogen`, `chlorine`, `ethylene`, `compressed_air` plus `pressurized_pipe`, `gas_compressor`, `boiler`, `condenser`, `liquefier`, `phase_separator` (gases as low-density `FluidType`, ADR-019)
- ✅ T-427 ADR — vapour classification documented (ADR-019); gases share fluid graph until 1.6.1
- ✅ T-428 Content spec + recipes + guide entries (`ElectronicsRecipes` ≈ 25 shaped recipes; +198 i18n keys, en/pt_BR parity 379)
- ✅ T-429 Tests — electrolysis stoichiometry, gas-pipe pressure cap, cable-tier burn — *catalogue tests ✅ in 1.6.0; kinetic-loop arithmetic tests ✅ in 1.6.1 (`ElectronicsTickerStoichiometryTest`)*
- ⏳ T-430 Benchmark P-017 — gas network throughput on 500-node mixed-tier graph — *deferred past 1.6.1*

**Exit gate:** silicon → wafer → processor T2 craftable; HV cable powers an HV laser cutter; H₂ from electrolysis fuels a gas turbine.

> Mirrors the 1.4.0 → 1.4.1 split: catalogue (items, blocks, fluids, recipes, i18n) ships in 1.6.0;
> kinetic-loop processing (gas pressure pass, electrolysis stoichiometry, geothermal world-heat,
> RTG decay) lands in **1.6.1**.

---

## 1.6.1 — Electronics kinetic loop ✅

**Goal.** Make the 1.6.0 catalogue come alive — gases flow under pressure, HV generators tick.

- ✅ T-429 Tests — electrolysis stoichiometry, gas-pipe pressure cap, cable-tier burn (`ElectronicsTickerStoichiometryTest`, 8 pure-arithmetic invariants)
- ⏳ T-430 Benchmark P-017 — gas network throughput on 500-node mixed-tier graph — *deferred (mirrors 1.4.1/1.5.1 pattern)*
- ✅ Gas-pressure semantics — boiler (water → 2× compressed_air) and condenser (compressed_air → ½ water) state transitions; gases respect `FluidSpecs` tier cap until dedicated pressure pass in 1.7.0
- ✅ Electrolyzer stoichiometry — 2 H₂O → 2 H₂ + O₂ (100 mB water → 200 mB H₂ + 100 mB O₂ per cycle, 1024 SU)
- ✅ Geothermal heat extraction — scans 6 lava neighbours, `200 SU × count` per tick
- ✅ Gas turbine — burns hydrogen (100 SU/mB) or ethylene (60 SU/mB) up to 10 mB/cycle from tank below
- ✅ RTG decay curve — constant 50 SU/cycle trickle, no fuel input
- ✅ HV machine recipes — `rolling_mill` (every metal ingot → 2× wire) and `laser_cutter` (silicon ingot → 4× silicon_wafer)
- ✅ `ElectronicsTicker` wired in `SapientiaPlugin` (15L delay, 5L period, mirrors `PetroleumTicker`)

**Exit gate:** electrolyzer split water → 2 H₂ + O₂ at recipe rate ✅; gas turbine burns hydrogen for sustained HV throughput ✅; benchmark P-017 deferred.

---

## 1.7.0 — Geo & atmosphere ✅

**Goal.** Industrial-scale resource gathering from world and air.

- ✅ T-431 Multiblock — `quarry_controller` (3×3×4 stainless-casing shell as carbon-steel proxy) with AABB selector via wrench — *catalogue ✅, AABB-driven mining tick + wrench AABB selector deferred to 1.7.1*
- ✅ T-432 Multiblock — `drill_rig_controller` (5×5×8 stainless casing) for sub-bedrock virtual mining — *catalogue ✅, probability-table tick deferred to 1.7.1*
- ✅ T-433 Machines — `gas_extractor`, `atmospheric_collector`, `prospector` (GPS-style scan tool) — *catalogue ✅, kinetic sampling + chunk scan deferred to 1.7.1*
- ✅ T-434 Multiblock — `desalinator_controller` (5×3×3) consuming sea water → fresh water + rock salt — *catalogue ✅, processing tick + brine recipes deferred to 1.7.1*
- ✅ T-435 Atmospheric gases — `argon`, `carbon_dioxide` registered; `liquid_oxygen` fluid for endgame combustion (3 new `FluidType`s in `BuiltinFluidTypes`)
- ✅ T-436 GPS infra — `gps_transmitter`, `gps_marker`, `gps_handheld_map` item — *catalogue ✅, coverage scan + map overlay deferred to 1.7.1*
- ⏳ T-437 Tests — quarry AABB serialization, drill-rig probability tables, GPS coverage radius — *catalogue tests ✅ (`GeoAndAtmosphereFluidsTest`); kinetic tests deferred to 1.7.1*
- ⏳ T-438 Benchmark P-018 — quarry tick budget (must respect bucket P-007 envelope even at 32×32 footprint) — *deferred to 1.7.1 with the kinetic loop*
- ✅ T-439 Content spec + recipes (`GeoRecipes`, 9 shaped workbench recipes; +23 i18n keys, en/pt_BR parity 402)
- ⏳ T-440 Bedrock parity — quarry AABB selector via `CustomForm` numeric inputs — *deferred to 1.7.1 alongside the wrench AABB selector*

**Exit gate:** quarry chews through a 16×16 zone in measured time without TPS regression; GPS handheld shows markers within transmitter coverage; both quarry and drill rig hot-reload across restart.

> Mirrors the 1.4.0 → 1.4.1 and 1.6.0 → 1.6.1 splits: catalogue (items, blocks, fluids, recipes, i18n)
> ships in 1.7.0; kinetic-loop processing (quarry AABB tick, drill-rig probability tables, GPS coverage
> scan, atmospheric collection, desalination cycle) lands in **1.7.1**.

---

## 1.7.1 — Geo & atmosphere kinetic loop ⛏️ ✅

**Goal.** Activate the 1.7.0 catalogue — quarries chew, atmospheres feed gas tanks, GPS lights up.

- ✅ T-437 Tests — `GeoTickerArithmeticTest` covers drill-rig probability range, desalinator efficiency window, round-robin gas rotation, and HV buffer headroom *(quarry AABB serialization + GPS coverage-radius math deferred to 1.8.0 with the AABB persistence pass)*
- ⏳ T-438 Benchmark P-018 — quarry tick budget on 32×32 footprint *(deferred to 1.8.0 alongside the performance pass)*
- ✅ Quarry kinetic tick — controllers register as HV CONSUMER, drain 512 SU and push 25 mB slurry per cycle into the tank above *(wrench-driven AABB selector deferred to 1.8.0)*
- ✅ Drill-rig probability tables — 20 % per-cycle hit chance produces 10 mB `crude_oil` (sub-bedrock virtual mining)
- ✅ Atmospheric collector — round-robin nitrogen → argon → CO₂ at 15 mB / cycle *(biome weighting deferred to 2.0.0 with biome metadata)*
- ✅ Desalinator — 100 mB water in / 90 mB water out + 256 SU drain (rock-salt residue modelled as the 10 mB loss; salt as item deferred to 2.0.0)
- ⏳ GPS coverage radius scan + handheld map overlay *(deferred to 1.8.0 with advanced logistics)*
- ⏳ T-440 Bedrock parity — `CustomForm` numeric AABB editor *(deferred to 2.0.0 with the rest of Bedrock UX)*

**Exit gate (achieved for shipped scope):** all geo / atmosphere multiblocks consume energy and move fluids per tick; `GeoTickerArithmeticTest` green; en/pt_BR i18n parity holds at 402 keys; build succeeds.

---

## 1.8.0 — Advanced logistics 📦 ✅

**Goal (achieved for catalogue scope).** Industrial-grade item/fluid routing — catalogue, blocks, recipes and event scaffolding. Kinetic loop (Ford-Fulkerson swap, splitter ratio table, multi-pass filter chains, packager NBT, conveyor visible item rendering, comparator/level-sensor logic-runtime read) ships in 1.8.1.

- ✅ T-441 Item logistics extras — `item_buffer`, `item_splitter`, `filter_chamber` (single-pass parity in 1.8.0; multi-pass in 1.8.1), `overflow_module`, `comparator_sensor`, `packager`, `unpackager`
- ✅ T-442 `conveyor_belt` — JUNCTION block placed and registered (visible item-on-belt rendering deferred to 1.8.1)
- ✅ T-443 Fluid logistics extras — `fluid_valve`, `fluid_level_sensor` (`phase_separator` deferred)
- ⏳ T-444 Ford-Fulkerson hardening — deferred to **1.8.1**
- ⏳ T-445 Routing improvements — explicit priority lanes (P0..P3) — deferred to **1.8.1**
- ⏳ T-446 Tests — splitter ratio integrity, packager NBT round-trip, max-flow correctness — deferred to **1.8.1**
- ⏳ T-447 Benchmark P-019 — Ford-Fulkerson on 1000-node item network — deferred to **1.8.1**
- ✅ T-448 Content spec + recipes (10 shaped recipes in `LogisticsRecipes`)
- ⏳ T-449 ADR-020 — packager/unpackager NBT format — deferred to **1.8.1** (locks before kinetic tick lands)
- ✅ T-450 `SapientiaItemPackagedEvent` (cancellable) for addon hooks — class shipped with HandlerList; emitted by packager tick in 1.8.1

**Exit gate (achieved for shipped scope):** 10 new logistics blocks place, register through `Sapientia.get().logistics()` / `.fluids()`, craft from `sapientia_workbench` recipes; en/pt_BR i18n parity holds at 422 keys; `SapientiaItemPackagedEventTest` green; build succeeds.

---

## 1.8.1 — Advanced logistics kinetic loop 🔁 ✅

**Goal (achieved for shipped scope).** Wire the 1.8.0 catalogue into the live solver tick — same split pattern as 1.4.0→1.4.1, 1.5.0→1.5.1, 1.6.0→1.6.1 and 1.7.0→1.7.1.

- ✅ T-444 Ford-Fulkerson hardening — `MaxFlowItemSolver` (Edmonds-Karp) ships as pure data structure with full arithmetic test coverage; opt-in via `network.solver: maxflow` (`LogisticsConfig`). The `ItemSolver` adapter that swaps greedy → maxflow at runtime is wired progressively with T-445 (rolled to 1.9.0).
- ⏳ T-445 Routing improvements — explicit priority lanes (P0..P3) — *deferred to **1.9.0** alongside the splitter ratio table; designed in ADR-020*
- ✅ T-446 Tests — `MaxFlowItemSolverTest` (8 cases incl. CLRS reference, parallel paths, self-loops, determinism), `LogisticsConfigTest` (4 cases incl. typo fall-back); splitter-ratio + packager round-trip tests roll with the splitter ratio table to 1.9.0
- ✅ T-447 Benchmark P-019 — `MaxFlowItemSolverBenchmark` parameterised on 100/1000-node grids (`@Param`); JMH-ready, anchors the regression gate
- ✅ T-449 ADR-020 — packager/unpackager NBT format V1 (single-stack, this release) and V2 (multi-stack, 2.0.0) + Ford-Fulkerson opt-in policy
- ⏳ Splitter ratio table — *deferred to **1.9.0** with the DAG editor that consumes it*
- ⏳ Multi-pass filter rule chaining — *deferred to **1.9.0***
- ⏳ Comparator sensor + fluid level sensor logic-runtime read — *deferred to **1.9.0**, lands with the android programming UI (T-453) that actually reads them*
- ✅ Packager / unpackager kinetic tick — `LogisticsTicker` registered alongside `MachineProcessor` / `PetroleumTicker` / `GeoTicker`; fires `SapientiaItemPackagedEvent` (cancellable) per cycle; pass-through single-stack (ADR-020 §2 V1)

**Exit gate (achieved for shipped scope):** `MaxFlowItemSolverTest` (8) + `LogisticsConfigTest` (4) green; `LogisticsTicker` registered and ticks at offset 19L / period 10L; `network.solver: maxflow` parses + falls back safely on typos; build green at 117 tests; en/pt_BR i18n parity holds at 422 keys.

---

## 1.9.0 — Androids 🤖 (catálogo) ✅

**Goal.** Catálogo + caps + persistência + scaffolding de evento dos 8 androids. O loop cinético (loot real, schematic builder, melee, comércio, editor DAG) fica reservado para 1.9.1, espelhando o split 1.8.0/1.8.1.

> Hard dependency on 1.3.0 (DAG runtime). Atendido.

- ✅ T-451 Android base — `AndroidNode` + `AndroidServiceImpl` + `AndroidTicker` com `INSTRUCTIONS_PER_TICK_CAP = 1` (loop cinético em 1.9.1)
- ✅ T-452 Android types — `farmer`, `lumberjack`, `miner`, `fisherman`, `butcher`, `builder`, `slayer`, `trader` (placement + persistência V009)
- ⏳ T-453 Programming UI — DAG editor (Java inventory) + Bedrock fallback flat-list editor — *deferred to **1.9.1***
- ✅ T-454 Upgrades — AI chip T1-T4, motor T1-T4, armour T1-T4, fuel module T1-T4 (16 itens craftáveis); efeitos vivos chegam em 1.9.1
- ⏳ T-455 Loot simulation — slayer/butcher use simulated loot tables (no real mob spawn) — *deferred to **1.9.1**, política travada em ADR-021*
- ✅ T-456 Caps — hard cap 4 androids/chunk (`AndroidCaps.CHUNK_CAP`) + configurable server-wide cap (`AndroidConfig`, default 200)
- ✅ T-457 ADR-021 — slayer melee policy (loot puramente simulado, alinhado com `mob_simulator`)
- ✅ T-458 Tests — `AndroidConfigTest`, `AndroidCapsTest`, `AndroidTickerTest` (rejection de ciclo DAG já coberta por `LogicCompilerTest` desde 1.3.0)
- ⏳ T-459 Benchmark P-020 — 200 androids/server-wide tick budget — *deferred to **1.9.1** quando o tick real existe; placeholder em `sapientia-benchmarks`*
- ✅ T-460 Content spec + recipes + guide entries — `docs/content-spec-T-45x.md`, `AndroidRecipes`, 24 chaves i18n por locale

**Exit gate:** ✅ `gradlew build verifyTranslations` BUILD SUCCESSFUL com 8 android blocks + 16 upgrade items registrados; V009 migra; `AndroidCapsListener` cancela placement acima dos caps; `SapientiaAndroidTickEvent` (cancellable) embarcado para addons.

---

## 1.9.1 — Androids kinetic loop 🤖 ⏳

**Goal.** Acende o loop cinético dos androids: cada tipo passa a executar trabalho real (scan, replant, mineração, schematic, loot, troca), o editor DAG ganha UI completa e os upgrades passam a ter efeito mensurável. Mesma arquitetura de split usada em 1.8.0 → 1.8.1.

- ⏳ T-453 Programming UI — DAG editor (Java inventory) + Bedrock fallback flat-list editor (canvas T-302l)
- ⏳ T-455 Loot simulation — slayer/butcher rodam tabelas de loot simulado conforme ADR-021
- ⏳ T-454-effects — AI chip aumenta raio de scan, motor reduz cooldown entre instruções, armour aplica HP e dano-resistido, fuel module troca biofuel/SU pelo buffer interno
- ⏳ Farmer crop scan + replant; lumberjack tree-fell + replant; miner virtual mining loop; fisherman water-source loot loop; builder schematic playback; trader item-exchange loop
- ⏳ T-459 Benchmark P-020 — 200 androids/server-wide tick budget (≥ 18 TPS)
- ⏳ Comparator sensor + fluid level sensor logic-runtime read (deferred from 1.8.1) — vira input de instrução do android
- ⏳ Hook do `SapientiaAndroidTickEvent` no loop real (já cancellable desde 1.9.0)

**Exit gate:** 8 androids realizam trabalho determinista; editor DAG completo; P-020 verde; addons conseguem cancelar/instrumentar via `SapientiaAndroidTickEvent`.

---

## 2.0.0 — Nuclear ⏳

**Goal.** First major version after MVP. Fission, radiation system and the recycling loop.

- ⏳ T-500 New ore — `uranium`; new fluid — `sulfuric_acid`, `molten_salt`, `heavy_water`
- ⏳ T-501 Multiblock — `fission_reactor_controller` (5×5×5 tungsten-carbide casing) consuming `fuel_cell_u`, producing `spent_fuel_cell` + heat + tritium gas
- ⏳ T-502 Coolant pipeline — water/molten-salt/heavy-water as required inputs; meltdown on starvation
- ⏳ T-503 Radiation system — choose between debuff-only MVP (Wither stack on contact) vs full dose-accumulation system (ADR T-507 decides)
- ⏳ T-504 Hazmat & thermal armour sets — full 4-piece sets with effect immunities
- ⏳ T-505 Reprocessing chain — `recycler` machine + `chemical_reactor` recipes producing plutonium + radioactive scrap
- ⏳ T-506 EV groundwork — `cable_t4`, `capacitor_t4`, `transformer_hv_ev`, `processor_t4`
- ⏳ T-507 ADR — radiation model (debuff stacks vs dose accumulation with decay)
- ⏳ T-508 ADR — meltdown blast radius and chunk dead-zone duration
- ⏳ T-509 Tests — meltdown trigger conditions, radiation immunity gate (hazmat/thermal), reprocessing yields
- ⏳ T-510 Benchmark P-021 — fission reactor multiblock tick cost (with full coolant + 6 hatches active)

**Exit gate:** fission reactor produces stable 1024 SU/t over a 10-minute server run with coolant; meltdown, recovery and dead-zone all reproducible; radiation effect (whichever ADR-T507 picks) reaches Bedrock players via Geyser status effects.

---

## 2.1.0 — Fusion & endgame ⏳

**Goal.** Close the catalogue. Fusion power, replicator, satellite GPS and Quantum gear.

- ⏳ T-511 New ores — `tungsten`, `iridium` (End); new alloys — hastelloy, tungsten-carbide
- ⏳ T-512 Multiblock — `fusion_reactor_controller` (7×7×7 toroidal hastelloy casing) consuming `deuterium` + `tritium`
- ⏳ T-513 Plasma + ignition — `liquid_oxygen`, `plasma`, `argon`; ignition requires one `fuel_cell_u` to bootstrap
- ⏳ T-514 Multiblock — `replicator_controller` (7×7×3) consuming `uu_matter` + template item
- ⏳ T-515 UU-matter pipeline — recycler scrap → fusion → uu-matter; blacklist of non-replicable items (per ADR T-518)
- ⏳ T-516 GPS satellite — `gps_satellite_controller` multiblock for world-wide coverage; `item_teleporter` block
- ⏳ T-517 Quantum gear — armour set, `jetpack_t4`, `drill_t4`, `chainsaw_t4`, `multitool_t4`
- ⏳ T-518 ADR — replicator blacklist scope (uu-matter, fuel cells, processors T4 minimum)
- ⏳ T-519 Endgame energy bridge (optional) — soft-dep adapter to expose Sapientia SU as FE/RF for downstream plugins (per §25.7)
- ⏳ T-520 Tests — fusion ignition path, replicator blacklist enforcement, satellite coverage radius
- ⏳ T-521 Benchmark P-022 — fusion reactor throughput on stable plasma (≥ 2048 SU/t sustained, ≤ 1.5 ms/tick)
- ⏳ T-522 Content spec + recipes + final guide pass
- ⏳ T-523 Tag — promote `1.0.0` and continue to `2.0.0` after this milestone (versioning per [README.md](README.md))
- ⏳ T-524 Compatibility matrix — published list of Geyser/Floodgate/Paper versions tested with full catalogue
- ⏳ T-525 Documentation pass — promote `docs/content-catalog.md` from `draft` to release status; freeze section IDs as a public contract

**Exit gate:** entire catalogue defined in [docs/content-catalog.md](docs/content-catalog.md) is craftable, persisted, performance-bound and Bedrock-parity green; end-to-end progression playable from vanilla iron to fusion reactor on a single world without admin intervention.

---

## Strategic backlog (post-2.1.0) ⏳

- ⏳ T-303 Public progression/research API — exposes the milestone-1.4.0 tier framework so addons can register their own tiers/research nodes
- ⏳ T-304 Compatibility bridges — explicit FE/RF/EU adapters (formerly T-519 if not delivered with 2.1.0)
- ⏳ T-305 **Sapientia Studio** — external authoring tool that generates Java addon scaffolding (separate project; not a YAML runtime — see ADR-012)
- ⏳ T-306 Noise & pollution system — chunk-level industrial debuff (no villager spawn, fauna flees) — per catalogue §18.6
- ⏳ T-307 Full radiation simulation — if 2.0.0 shipped the MVP debuff-only model, ship dose accumulation + Geiger counter here

---

## Published releases

_None yet._ First planned tag: `v1.0.0`.

---

## References

- Detailed plan in `docs/implementation-plan.md` (internal).
- Architectural decisions in `docs/decision-log.md` (ADR-001..ADR-011).
- Performance contract in `docs/performance-contract.md` (P-001..P-014).
