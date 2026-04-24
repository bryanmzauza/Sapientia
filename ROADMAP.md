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

## 0.2.0 — Items & Blocks ⏳

**Goal.** Full custom item + persistent custom block lifecycle.

- ⏳ T-102 `SapientiaItemInteractEvent` + generic listener
- ⏳ T-105 CI gate `verifyTranslations` (en/pt_BR key parity)
- ⏳ T-106 ArchUnit lint blocking user-facing literals
- ⏳ T-113 `SapientiaBlockPlaceEvent` / `SapientiaBlockBreakEvent`
- ⏳ T-112 (finalization) block load/unload via `ChunkLoadEvent` / `ChunkUnloadEvent`
- ⏳ Async write-behind for `custom_blocks` with 500 ms batching

**Exit gate:** Sapientia block survives a restart; `verifyTranslations` green on CI.

---

## 0.3.0 — Energy 🔌 ⏳

**Goal.** First working energy system (generator + cable + consumer + capacitor).

- ⏳ T-140 Migration V003 `energy_networks` + `energy_nodes`
- ⏳ T-141 In-memory `NetworkGraph` with split/merge
- ⏳ T-142 Simplified Ford-Fulkerson solver (target P-003)
- ⏳ T-143 Four reference blocks (generator, cable, capacitor, consumer)
- ⏳ T-144 Kryo graph serialization
- ⏳ T-145 Energy machine UI (energy bar + progress + start/stop)
- ⏳ T-146 `SapientiaEnergyFlowEvent` + `SapientiaMachineTickEvent`

**Exit gate:** 500-node graph resolved in ≤ 2 ms per tick on the reference server.

---

## 0.4.0 — Crafting & Guide ⏳

**Goal.** Recipe mechanic and in-game navigable guide.

- ⏳ T-130 Custom workbench (3×3 + state_blob)
- ⏳ T-131 Hardcoded recipe parser (proof of concept)
- ⏳ T-132 `SapientiaRecipeCompleteEvent`
- ⏳ T-150 Book/GUI navigable by category (Java)
- ⏳ T-151 Simple unlock/progression

**Exit gate:** three working recipes + guide lists every registered item.

---

## 0.5.0 — YAML content ⏳

**Goal.** Creators can declare machines/items/blocks without recompiling.

- ⏳ T-160 YAML schema + validator with actionable messages (JSON-Schema)
- ⏳ T-161 Machine/item/block loader from YAML
- ⏳ T-162 Hot-reload `/sapientia reload content`
- ⏳ T-163 Scaffold `/sapientia create machine <name>`
- ⏳ T-164 Java resource pack pipeline (`ItemModel`, mcmeta, zip)

**Exit gate:** example under `examples/content/` fully in YAML replaces the hardcoded layer.

---

## 1.0.0-beta — Java MVP polish ⏳

**Goal.** Release-grade quality: benchmarks, CI regression, docs.

- ⏳ T-170 `sapientia-benchmarks` + initial JMH (P-003, P-006, P-007)
- ⏳ T-171 CI `compareToBaseline` (regression > 10 % blocks merge)
- ⏳ T-172 Release documentation + initial changelog

**Exit gate:** all P-001..P-010 and P-013 targets green; plugin published as `1.0.0-beta`.

---

## 1.0.0 — Bedrock parity 📱 ⏳

**Goal.** Bedrock players (Geyser/Floodgate) get an equivalent experience to Java.

- ⏳ T-201 `SimpleForm` / `ModalForm` / `CustomForm` wrappers
- ⏳ T-202 Machine UI on Bedrock (mirror `CustomForm` + temporary chest)
- ⏳ T-203 Filter UI
- ⏳ T-204 Guide UI
- ⏳ T-205 `TextAdapter.toPlainBedrock(Component)`
- ⏳ T-205b Bedrock `.lang` catalogs derived from `en.yml` / `pt_BR.yml`
- ⏳ T-206 Java-only fallback → auto `CustomForm`
- ⏳ T-207 `.mcpack` pipeline + manifest + geometry
- ⏳ T-208 Geyser mappings (`items.json`, `blocks.json`)
- ⏳ T-209 `/sapientia pack build bedrock`
- ⏳ T-210 Integration test in container (Paper + Floodgate + Geyser)
- ⏳ T-211 Benchmarks P-009..P-012 + P-014

**Exit gate:** Bedrock smoke test passes; Geyser/Floodgate compatibility matrix published.

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
- ⏳ T-305 **Sapientia Studio** — external authoring tool (separate project)

---

## Published releases

_None yet._ First planned tag: `v0.1.0`.

---

## References

- Detailed plan in `docs/implementation-plan.md` (internal).
- Architectural decisions in `docs/decision-log.md` (ADR-001..ADR-011).
- Performance contract in `docs/performance-contract.md` (P-001..P-014).
