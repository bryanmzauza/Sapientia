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

## 1.1.0 — Item logistics ⏳

- ⏳ T-300 Item transport (cables + logistics graph, target P-004)
- ⏳ Round-robin / priority / first-match filters
- ⏳ Events and public API for addons

---

## 1.2.0 — Fluids ⏳

- ⏳ T-301 Fluid transport (tanks + continuous volume)
- ⏳ Fluid types declarable via YAML

---

## 1.3.0 — Programmable logic ⏳

- ⏳ T-302 Non-Turing rules compiled into a DAG
- ⏳ In-game visual editor + YAML export

---

## 1.4.0+ — Strategic backlog ⏳

- ⏳ T-303 Advanced tiers (nuclear, multi-block auto-crafting)
- ⏳ T-304 Public progression/research API
- ⏳ T-305 **Sapientia Studio** — external authoring tool that generates Java addon scaffolding (separate project; not a YAML runtime — see ADR-012)

---

## Published releases

_None yet._ First planned tag: `v1.0.0`.

---

## References

- Detailed plan in `docs/implementation-plan.md` (internal).
- Architectural decisions in `docs/decision-log.md` (ADR-001..ADR-011).
- Performance contract in `docs/performance-contract.md` (P-001..P-014).
