# Changelog

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and SemVer.

## [1.8.0] — Advanced logistics 📦 ✅

Catalogue release for industrial-grade item / fluid routing. Adds eight new
item-logistics blocks (buffer, splitter, filter chamber, overflow module,
comparator sensor, packager, unpackager, conveyor belt), two new fluid-logistics
blocks (valve, level sensor) and the public `SapientiaItemPackagedEvent`
event scaffolding. Mirrors the 1.4.0 → 1.4.1, 1.5.0 → 1.5.1, 1.6.0 → 1.6.1 and
1.7.0 → 1.7.1 splits: this release ships items, blocks, recipes and i18n; the
kinetic loop (Ford-Fulkerson swap, splitter ratio table, multi-pass filter
chains, packager NBT, conveyor belt visible item rendering, comparator-sensor
runtime hookup) lands in 1.8.1.

### Added

- `dev.brmz.sapientia.api.events.SapientiaItemPackagedEvent` — cancellable event
  fired when a `packager` block bundles items into a `packaged_bundle` stack
  (T-450). Ships in 1.8.0; the kinetic packaging tick that fires it lands in
  1.8.1.
- 8 item-logistics blocks under `sapientia-content/logistics/` (T-441 / T-442):
  - `item_buffer` — high-priority CONSUMER (priority +5) — barrel-based smoothing sink.
  - `item_splitter` — JUNCTION — observer-driven distributor; ratio table in 1.8.1.
  - `filter_chamber` — FILTER — multi-pass filter; 1.8.0 ships single-pass parity with the 1.1.0 `item_filter`.
  - `overflow_module` — low-priority CONSUMER (priority -10) — last-resort sink.
  - `comparator_sensor` — placement-only stub; logic-runtime hookup in 1.8.1.
  - `packager` / `unpackager` — placement-only stubs; NBT format locked by ADR-020 in 1.8.1.
  - `conveyor_belt` — JUNCTION — visible item-on-belt rendering in 1.8.1.
- 2 fluid-logistics blocks under `sapientia-content/fluids/` (T-443):
  - `fluid_valve` — JUNCTION — manual + logic-driven toggle in 1.8.1.
  - `fluid_level_sensor` — placement-only stub; logic-runtime hookup in 1.8.1.
- 10 shaped recipes in `LogisticsRecipes` (T-448) — one per new block, gated
  behind 1.6.0 HV components for the heavy items (packager / unpackager use
  CIRCUIT_T3) and 1.4.0 brass / iron ingots for the lighter routing pieces.
- `SapientiaItemPackagedEventTest` (sapientia-api) — verifies the event class
  exposes a HandlerList and implements `Cancellable`.

### Changed

- `ContentBootstrap` — registers the 10 new blocks after the 1.7.0 geo
  catalogue and calls `LogisticsRecipes.registerAll` before the
  `EnergyInspector` start.

### Deferred to 1.8.1

- T-444 Ford-Fulkerson hardening (max-flow swap for HV+ networks).
- T-445 Routing improvements — explicit priority lanes (P0..P3) per node.
- T-446 Tests — splitter ratio integrity, packager NBT round-trip, max-flow
  correctness vs reference.
- T-447 Benchmark P-019 — Ford-Fulkerson on 1000-node item network.
- T-449 ADR-020 — packager / unpackager NBT format.
- Splitter ratio table.
- Multi-pass filter rule chaining.
- Comparator sensor + fluid level sensor logic-runtime read.
- Conveyor belt visible item-on-belt rendering (display-entity API on Java;
  static texture on Bedrock).

### i18n

- +20 keys (10 blocks × name + desc) in en/pt_BR; parity now at **422**.

## [1.7.1] — Geo & atmosphere kinetic loop ⛏️ ✅

Activates the 1.7.0 catalogue. The four geo / atmosphere multiblocks plus the
two MV machines now actually consume energy and move fluids per tick. Mirrors
the 1.4.0 → 1.4.1, 1.5.0 → 1.5.1 and 1.6.0 → 1.6.1 pattern: 1.7.0 shipped the
catalogue, 1.7.1 ships the loop.

### Added

- `dev.brmz.sapientia.core.geo.GeoTicker` — per-tick driver (T-431..T-435):
  - `quarry_controller` (3×3×4 hollow shell) drains 512 SU and pushes 25 mB
    of slurry (water proxy) into the tank above.
  - `drill_rig_controller` (5×5×8 hollow shell) drains 1024 SU and rolls a
    20% chance to deposit 10 mB `crude_oil` into the tank above
    (sub-bedrock virtual mining; deepest reservoirs are oil-rich).
  - `desalinator_controller` (5×3×3 hollow shell) consumes 100 mB water from
    the input tank above, drains 256 SU, and emits 90 mB fresh water into
    the tank below (the missing 10 mB models the rock-salt residue —
    item-form deferred to 2.0.0).
  - `gas_extractor` MV CONSUMER drains 256 SU and pulls 20 mB nitrogen from
    the chunk atmosphere into the tank above.
  - `atmospheric_collector` MV CONSUMER drains 256 SU and round-robins
    nitrogen → argon → carbon_dioxide (15 mB per cycle) into the tank above.
- Energy graph registration on the three controllers (`onPlace` / `onBreak`
  in `SapientiaQuarryController`, `SapientiaDrillRigController`,
  `SapientiaDesalinatorController`) — they now register as HV CONSUMER nodes
  so cables can power them.
- 8 pure-arithmetic invariants in `GeoTickerArithmeticTest` covering draw
  positivity, drill-rig probability range, desalinator efficiency window
  (80–100%), round-robin gas rotation, and HV buffer headroom.

### Changed

- `SapientiaPlugin` — owns the new `geoTicker` field, instantiates it after
  `electronicsTicker`, schedules it at 17L delay / 5L period, and exposes a
  public `geoTicker()` accessor.

### Deferred

- T-438 (P-018 quarry chunk-budget benchmark) → 1.8.0 alongside the
  performance pass.
- T-440 (Bedrock CustomForm AABB editor) → 2.0.0 with the rest of the
  Bedrock-specific UX.
- GPS coverage radius scan + handheld-map overlay (kinetic side of T-436)
  → 1.8.0 with advanced logistics.

### i18n

- No new keys; en/pt_BR parity stays at **402**.

## [1.7.0] — Geo & atmosphere ⛏️ ✅

Catalogue release for industrial-scale resource gathering. Adds three multiblock
controllers, two new MV-tier machines, the GPS infrastructure (transmitter +
marker block + handheld map), the GPS-style prospector tool, and three new
fluids (argon, carbon_dioxide, liquid_oxygen). Mirrors the 1.4.0 → 1.4.1 and
1.6.0 → 1.6.1 splits: this release ships items, blocks, fluids, recipes and
i18n; the kinetic-loop processing (quarry AABB tick, drill-rig probability
tables, GPS coverage scan, atmospheric collection, desalination cycle) lands
in 1.7.1.

### Added
- **Atmospheric gases & cryogenics** (sapientia-core, T-435) — three new
  `FluidType`s registered automatically by `SapientiaPlugin`:
  - `argon` — inert atmospheric gas (density 2 kg/m³); atmospheric collector
    output, used as a shielding gas reagent.
  - `carbon_dioxide` — atmospheric collector + combustion byproduct (density
    2 kg/m³); reagent in algae bioreactor chains (2.0.0).
  - `liquid_oxygen` — cryogenic liquid produced by chilling `oxygen_gas` in
    the liquefier (density 1141 kg/m³, well above water — guards the
    "low-density => gas" routing rule).
- **Multiblock controller stubs** (sapientia-content, T-431 / T-432 / T-434)
  — placement + shape-validation, kinetic ticks land in 1.7.1:
  - `quarry_controller` — 3×3×4 hollow shell of stainless casing or
    iron blocks (vanilla proxy). Future home of the AABB-driven mining tick.
  - `drill_rig_controller` — 5×5×8 hollow shell. Future home of sub-bedrock
    virtual mining via probability tables.
  - `desalinator_controller` — 5×3×3 hollow shell. Future home of sea-water
    → fresh-water + rock-salt processing.
- **MV machines** (sapientia-content, T-433) — placement-only stubs registering
  as MV CONSUMER nodes on the energy graph:
  - `gas_extractor` — pulls underground gas pockets into the fluid network
    (kinetic in 1.7.1).
  - `atmospheric_collector` — samples world atmosphere into nitrogen / argon
    / CO₂ tanks (biome-weighted in 1.7.1).
- **GPS infrastructure** (sapientia-content, T-436):
  - `gps_transmitter` (block) — broadcasts a coverage signal that lights up
    handheld maps (coverage scan in 1.7.1).
  - `gps_marker` (block) — passive way-point block; renders on the map.
  - `gps_handheld_map` (item) — shows nearby markers when in coverage.
- **Prospector** (sapientia-content, T-433) — GPS-style scan tool. Right-click
  surveys surrounding chunks for sub-bedrock ore reservoirs (kinetic in 1.7.1).
- **`GeoRecipes`** (sapientia-content, T-439) — 9 shaped workbench recipes
  covering every new block + item, gating heavier multiblock controllers
  behind 1.6.0 HV electronics (`circuit_t3`, `ram_t3`, `motor_t3`).
- **`GeoAndAtmosphereFluidsTest`** (sapientia-core, T-437 catalogue piece) —
  4 pure-arithmetic invariants on the new fluids: gas density gate, LOX
  liquid density, id namespacing, non-zero color.

### Changed
- **`SapientiaPlugin`** — registers the three new `FluidType`s alongside the
  existing 14 built-ins.
- **`ContentBootstrap`** — wires the 7 new blocks and 2 new items, plus the
  `GeoRecipes.registerAll` call, in the established 1.x sequence.

### Deferred
- **T-437 kinetic tests** — quarry AABB serialization, drill-rig probability
  tables, GPS coverage radius. Land in 1.7.1 with the kinetic loop.
- **T-438 Benchmark P-018** — quarry tick budget on 32×32 footprint.
  Deferred (mirrors the 1.4.1 / 1.5.1 / 1.6.1 pattern).
- **T-440 Bedrock parity** — quarry AABB selector via `CustomForm` numeric
  inputs. Lands in 1.7.1 alongside the wrench AABB selector.

### i18n
- 23 new keys per locale: 3 fluids (argon, carbon_dioxide, liquid_oxygen),
  2 items (prospector, gps_handheld_map — name + lore + desc each), 7 blocks
  (3 controllers + 2 machines + 2 GPS — name + desc each). Parity holds at
  **402** keys for both `en_us` and `pt_br`.

### Tests
- 4 new `GeoAndAtmosphereFluidsTest` cases, all green. `cleanTest test`
  reports BUILD SUCCESSFUL across every module; `verifyTranslations` confirms
  402-key parity.

## [1.6.1] — Electronics kinetic loop ⚡ ✅

Activates the 1.6.0 HV catalogue. The new `ElectronicsTicker` drives every HV
block contract (electrolyzer, boiler, condenser, geothermal, gas turbine, RTG)
on a 5-tick cadence with 15-tick start delay, mirroring the `PetroleumTicker`
pattern from 1.5.1. Mass and energy are conserved by construction and locked
by 8 pure-arithmetic invariants in `ElectronicsTickerStoichiometryTest`.

### Added
- **`ElectronicsTicker`** (sapientia-core, T-425 / T-426 / T-429) — per-tick
  kinetic loop dispatching by `SapientiaBlock` id via `ChunkBlockIndex`:
  - **Electrolyzer** — 2 H₂O → 2 H₂ + O₂ (100 mB water above → 200 mB hydrogen
    below + 100 mB oxygen_gas to z=-1 neighbour, 1024 SU/cycle)
  - **Boiler** — water above → compressed_air below (1:2 expansion, 50 mB →
    100 mB, 256 SU)
  - **Condenser** — compressed_air above → water below (2:1 contraction, mass
    conservation inverse of boiler, 128 SU)
  - **Geothermal generator** — scans 6 immediate lava neighbours,
    `200 SU × count` pushed into the energy graph per tick
  - **Gas turbine** — burns hydrogen (100 SU/mB) or ethylene (60 SU/mB) from a
    tank below, up to 10 mB per cycle (H₂ is the better fuel)
  - **RTG** — constant 50 SU/cycle trickle, no fuel input (decay curve modeled
    as a fixed rate for now)
- **HV machine recipes** (sapientia-content, `MachineRecipeData.registerHvMachines`)
  — `rolling_mill` accepts every `Metal` ingot → 2× wire (256 SU, 20 ticks);
  `laser_cutter` accepts silicon_ingot → 4× silicon_wafer (256 SU, 20 ticks).
- **`ElectronicsTickerStoichiometryTest`** (sapientia-core, T-429) — 8
  pure-arithmetic invariants on the published rates: 2 H₂/H₂O ratio,
  O₂/H₂O equality, boiler/condenser mass conservation, boiler > condenser
  energy, all generator rates positive, H₂ > ethylene SU/mB, gases respect
  `FluidSpecs` tier capacity (gas-pipe pressure cap proxy until 1.7.0
  dedicated pressure pass), and BuiltinFluidTypes id sanity.

### Changed
- **`SapientiaPlugin`** — registers the new `ElectronicsTicker` alongside
  `PetroleumTicker`, scheduled at 15L delay / 5L period; exposed via
  `electronicsTicker()` accessor.

### Deferred
- **T-430 Benchmark P-017** — 500-node mixed-tier gas-network throughput
  benchmark deferred (mirrors the 1.4.1 / 1.5.1 pattern of skipping benchmarks
  in kinetic releases).
- **Dedicated gas pressure pass** — gases continue to share the fluid graph
  (ADR-019) and are capped by `FluidSpecs.capacityMb`. Distinct
  pressure/flow-rate semantics land in 1.7.0.

### i18n
- No new keys (no new items, blocks or fluids). Parity holds at **379** keys
  for both `en_us` and `pt_br`.

### Tests
- 8 new `ElectronicsTickerStoichiometryTest` cases, all green. `cleanTest test`
  reports BUILD SUCCESSFUL across every module; `verifyTranslations` confirms
  379-key parity.

## [1.6.0] — Electronics & HV ⚡ ✅

Catalogue release for the high-voltage tier. Adds the new ore tier, electronics
component chain, HV alloys, HV energy network and the gas pipeline. Mirrors the
1.4.0 → 1.4.1 split: this release ships items, blocks, fluids, recipes and i18n;
the kinetic-loop processing (gas pressure pass, electrolysis stoichiometry,
geothermal world-heat, RTG decay) lands in 1.6.1.

### Added
- **`Metal` extensions** (sapientia-content, T-421 / T-424) — 4 new raw metals
  (`aluminum`, `silicon`, `titanium`, `lithium`) and 3 new alloys
  (`stainless_steel`, `damascus_steel`, `nichrome`). Catalogue grew from
  78 → 138 metallurgy items.
- **`Component` + `ComponentItem` + `ComponentCatalog`** (sapientia-content,
  T-422) — 17-entry electronics chain spanning silicon wafer, motor T1..T3,
  circuit T1..T3, processor T1..T3, coil T1..T3, RAM T2/T3 and storage HDD/SSD.
- **`SapientiaCableT3` / `SapientiaCapacitorT3` / `SapientiaTransformerMvHv`**
  (sapientia-content, T-425) — HV cable, capacitor and MV↔HV transformer.
- **`SapientiaGeothermalGen` / `SapientiaGasTurbine` / `SapientiaRtg`**
  (sapientia-content, T-425) — three HV generators registered as
  `EnergyNodeType.GENERATOR` / `EnergyTier.HIGH`.
- **`SapientiaElectrolyzer` / `SapientiaRollingMill` / `SapientiaLaserCutter` /
  `SapientiaChemicalReactor`** (sapientia-content, T-423) — four HV machines
  extending `MachineEnergyBlock`.
- **6 new gas `FluidType`s** (sapientia-core, T-426 / ADR-019) — `hydrogen`,
  `oxygen_gas`, `nitrogen`, `chlorine`, `ethylene`, `compressed_air`. Density
  in [1, 12] kg/m³; classified as gases by the < 100 threshold.
- **`FluidsContentBlockExt` + 6 gas blocks** (sapientia-content, T-426) —
  `pressurized_pipe`, `gas_compressor`, `boiler`, `condenser`, `liquefier`,
  `phase_separator`.
- **`ElectronicsRecipes`** (sapientia-content, T-428) — ~25 shaped recipes
  covering every component, HV energy block, HV machine and gas block.
- **i18n** — +198 keys across `en.yml` and `pt_BR.yml` (60 new metals,
  17 components, 16 blocks, 6 fluids per locale). `verifyTranslations` reports
  379 keys per locale in parity.
- **Tests** — `ComponentCatalogTest` (17 unique components, all materials
  non-null), `BuiltinFluidTypesTest#gasesAreLowDensity` (ADR-019 invariant),
  `MetalCatalogTest` updated for 138 items / 10 raw / 6 alloy.
- **ADR-019** (`docs/decision-log.md`) — vapour classification: gases are
  registered as `FluidType` with `density < 100 kg/m³` and share the existing
  fluid graph. A dedicated gas-pressure pass arrives in 1.6.1.

### Deferred
- T-421 ore world-gen — catalogue ships, generation deferred (mirrors T-401).
- T-429 kinetic-loop tests (electrolysis stoichiometry, gas-pipe pressure cap,
  cable-tier burn) → 1.6.1.
- T-430 benchmark P-017 (500-node gas-network throughput) → 1.6.1.

### Build
- `gradlew build verifyTranslations` BUILD SUCCESSFUL with 379 i18n keys per
  locale and all module test suites green.

---

## [1.5.1] — Petroleum kinetic loop ⛽ ✅

Closes the deferred items from 1.5.0 so the crude → diesel → combustion vertical
slice actually runs end-to-end.

### Added
- **`MachineProcessor`** (sapientia-core, T-404 / 1.4.1) — per-tick recipe
  driver. Scans every CONSUMER energy node, looks up the SapientiaBlock kind via
  `ChunkBlockIndex`, scans the chest above for a matching `MachineRecipe` from
  `MachineRecipeRegistry`, advances the in-flight recipe, and on completion
  drains the energy buffer and deposits the output stack into the chest below.
  In-flight progress is in-memory only (server restart rolls back).
- **`MachineRecipe` / `MachineRecipeRegistry`** (sapientia-api, T-404) — public
  recipe model and synchronized registry exposed through
  `SapientiaAPI#machineRecipes()`.
- **`MachineRecipeData`** (sapientia-content, T-404 / T-405 / T-414) — bulk
  registration of ~40 metallurgy + chemistry recipes covering every machine
  block placed by 1.4.0 and 1.5.0.
- **`ReservoirService` + V008 migration** (sapientia-core, T-412) — per-chunk
  crude-oil reservoirs persisted in `crude_oil_reservoirs`. Initialisation is
  deterministic (FNV-1a-mixed seed of `world × chunkX × chunkZ`) yielding
  reserves in [10 000, 100 000] mB; slow regeneration of 1 mB/min capped at the
  initial reserve (ADR-018).
- **`PetroleumTicker`** (sapientia-core, T-412 / T-413 / T-414 / T-415) — drives
  pumpjack (drains reservoir → fills tank above), oil-refinery controller
  (validates 5×5×7 shell, drains 100 mB crude, emits 40/30/20/10 mB
  diesel/gasoline/lubricant/water to N/E/S/W tanks), combustion_gen (5 mB diesel
  or gasoline → 200 / 250 SU per cycle), biogas_gen (10 mB nutrient_broth →
  80 SU per cycle).
- **ADR-017** (Voltage incompatibility between tiers) and **ADR-018**
  (Reservoir replenishment = chunk-decay slow-regen finite) — full prose in
  `docs/decision-log.md`.
- **Tests** — `ReservoirServiceTest` (5 cases: deterministic init range,
  stability, distinctness, drain reduces amount + persists, drain caps at
  available); `MigrationLoaderTest` extended to expect V008.

### Changed
- `SapientiaPlugin` schedules `machineProcessor.tick()` every 10 ticks and
  `petroleumTicker.tick()` every 5 ticks alongside the existing energy/
  logistics/fluid solvers.
- `MachineProcessor.tick()` reaps stale in-flight entries whose energy node has
  disappeared (block break) so map size stays bounded.

### Notes
- T-401 ore world-gen rolled forward again to 1.6.0 to ship alongside the
  electronics-tier ores in a single `WorldGenerator` integration.
- Recipe progress is intentionally non-persistent for now; 1.6.0 will add an
  optional snapshot table once the kinetic loop has bedded in.

---

## [1.2.0] — Fluids 💧 ✅

Continuous-volume fluid logistics. Mirrors the 1.1.0 graph contract over a
new `FluidNetworkGraph` and adds a vanilla-aware solver that pumps from /
drains to water and lava blocks plus water/lava cauldrons.

### Added
- **Fluids API** (T-301a) — `FluidNode`, `FluidNetwork`, `FluidService`,
  `FluidNodeType` (`PIPE`/`PUMP`/`DRAIN`/`TANK`/`JUNCTION`), `FluidType`
  (record: id, displayKey, color, density, hot), `FluidStack`,
  `FluidSpecs.capacityMb` (4 000 / 16 000 / 64 000 / 256 000 mB) and
  `FluidSpecs.throughputPerTick` (50 / 200 / 800 / 3 200 mB/tick). Public
  events: `SapientiaFluidFlowEvent`, `SapientiaFluidTransferEvent`. Exposed
  through `SapientiaAPI#fluids()`.
- **Fluid graph + persistence** (T-301b/c) — `FluidNetworkGraph` (port of
  `ItemNetworkGraph`), `SimpleFluidNode`, V006 migration with the
  `fluid_nodes` table (chunk-indexed; tank contents = fluid type id +
  amount). No mixing — tanks reject a second fluid type until drained.
- **Fluid solver** (T-301d) — `FluidSolver`: per-network pump → tank →
  drain pipeline running every 5 ticks, capped by per-tier throughput and
  tank capacity; fires `SapientiaFluidTransferEvent` per move and
  `SapientiaFluidFlowEvent` per active network.
- **Vanilla bridge** (T-301e) — `AdjacentFluids` reads/writes vanilla:
  consumes / fills water and lava cauldrons by `Levelled` level, removes
  source water/lava blocks on extract, and places water/lava sources or
  fills cauldrons on deposit.
- **Built-in fluid registry** (T-301f) — `BuiltinFluidTypes` registers
  `sapientia:water`, `sapientia:lava`, `sapientia:milk` at boot via
  `FluidService#registerType` (Java-declared, per ADR-016 — supersedes the
  pre-1.2.0 ROADMAP note about YAML-declared fluid types).
- **Content blocks** (T-301g) — `SapientiaFluidPipe` (iron bars),
  `SapientiaFluidPump` (blast furnace), `SapientiaFluidTank` (glass shell),
  `SapientiaFluidDrain` (smoker) sharing `FluidsContentBlock` base.
  Auto-registered bundled recipes under `GuideCategory.LOGISTICS`.
- **Command** (T-301h) — `/sapientia fluids info` reports the targeted
  node, its network and the live tank contents (`<fluid> <amount>/<capacity>
  mB`). Permission `sapientia.command.fluids`.
- **i18n** — added `block.fluid_pipe`, `block.fluid_pump`, `block.fluid_tank`,
  `block.fluid_drain`, `fluid.water/lava/milk.name`, `command.fluids.*` and
  `command.help.desc.fluids` to `en.yml` and `pt_BR.yml`. `verifyTranslations`
  green at 118 keys.

### Decisions
- **ADR-015** — One fluid type per tank (no mixing). Trades realism for
  determinism; matches the no-mixing rule already used by item filters.
- **ADR-016** — Fluid types are Java-declared via `FluidService#registerType`.
  Supersedes the pre-1.2.0 ROADMAP entry "Fluid types declarable via YAML"
  in line with ADR-012 (Java-first content).

## [1.1.0] — Item Logistics 📦 ✅

First post-parity feature milestone. Introduces a fully wired item logistics
network that mirrors the 0.3.0 energy graph: 6-neighbour BFS, split-on-removal,
merge-on-add, per-tick solver, SQLite persistence and i18n-driven Java/Bedrock
filter UIs.

### Added
- **Logistics API** (T-300a) — `ItemNode`, `ItemNetwork`, `ItemService`,
  `ItemFilterRule`, `ItemFilterMode`, `ItemRoutingPolicy`, `ItemNodeType`,
  `ItemSpecs.throughputPerTick(EnergyTier)` (64/256/1024/4096 items/tick).
  Public events: `SapientiaItemFlowEvent`, `SapientiaItemFilterEvent`
  (cancellable), `SapientiaItemRouteEvent`. Exposed through
  `SapientiaAPI#logistics()`.
- **Network graph + persistence** (T-300b/c) — `ItemNetworkGraph` (direct
  port of `NetworkGraph`), `SimpleItemNode`, V005 migration with
  `item_nodes` + `item_filter_rules` tables (chunk-indexed), `ItemNodeStore`
  with `DELETE … RETURNING` rule cleanup.
- **Routing solver** (T-300d) — `ItemSolver`: per-tick greedy round-robin /
  priority / first-match policy; pulls/pushes via `AdjacentContainers`
  (vanilla `BlockState` `Container` only — chests, barrels, hoppers,
  dispensers, droppers, shulker boxes); fires `Filter`/`Route`/`Flow`
  events; round-robin cursor map keyed by network UUID. Ford-Fulkerson
  deferred to 1.4.0+.
- **Filter rule matcher** (T-300e) — `ItemFilterRuleMatcher`: glob support
  for `*`, `namespace:*`, exact `namespace:id`; BLACKLIST short-circuit;
  WHITELIST gate.
- **Content blocks** (T-300f) — `SapientiaItemCable` (iron bars),
  `SapientiaItemProducer` (dropper), `SapientiaItemConsumer` (hopper),
  `SapientiaItemFilter` (iron trapdoor) sharing `LogisticsContentBlock`
  base. Auto-registered companion items + bundled recipes under
  `GuideCategory.LOGISTICS`.
- **Filter UI** (T-300g) — `FilterDescriptor` evolved from the 1.0.0
  experimental stub: now `UIDescriptor<ItemNode>`, registered
  unconditionally, renders the current rule list and routing policy on Java
  (chest) and Bedrock (`SapientiaCustomForm` summary). Editing happens via
  `/sapientia logistics filter add|remove|clear|list`. The
  `experimental.filter` config flag is gone.
- **`/sapientia logistics`** (T-300h) — `info` (raytraced node summary),
  `policy <round_robin|priority|first_match>`, and
  `filter <add|remove|clear|list>` subcommands with full tab-completion
  and i18n keys in `en` + `pt_BR`.
- **Wiring** (T-300i) — `SapientiaPlugin` now hosts an `ItemServiceImpl` +
  `ItemSolver`, hydrates loaded chunks on startup, hooks chunk
  load/unload, and ticks the solver every tick (energy stays at every 10).

### Tests
- `ItemNetworkGraphTest` — mirrors `NetworkGraphTest` for adjacent merge,
  diagonal isolation, cable-split, network merge, default + mutable
  routing policy. `MigrationLoaderTest` updated to assert V005.

### Docs
- `decision-log.md` — ADR-013 (item logistics reuses energy
  `NetworkGraph` shape) and ADR-014 (per-network routing policy).
- `ROADMAP.md` marked 1.1.0 ✅.

## [1.0.0] — Bedrock parity 📱 ✅

Bedrock parity milestone. Floodgate-detected Bedrock players now get a UI
surface and resource-pack pipeline equivalent to Java.

### Added
- **Floodgate form wrappers** (T-201) — `SapientiaSimpleForm`,
  `SapientiaModalForm`, `SapientiaCustomForm` in
  `dev.brmz.sapientia.bedrock.forms`. Typed Cumulus 1.1.2 API; reflection
  removed from `BedrockFormsUIProvider#isReady`.
- **Machine UI on Bedrock** (T-202) — `MachineBedrockRenderer` builds a
  `CustomForm` with energy/percent labels and a Running toggle that round-
  trips state through `MachineRunningRegistry`.
- **Experimental filter UI stub** (T-203) — `FilterDescriptor`, opt-in via
  `experimental.filter` config flag. Lays the surface for 1.x logistics.
- **Guide UI on Bedrock** (T-204) — `GuideIndexBedrockRenderer` +
  `GuideDetailBedrockRenderer` mirror the Java guide flow as `SimpleForm`s.
- **`TextAdapter.toPlainBedrock(Component)`** (T-205) and
  **`LangFileWriter`** (T-205b) — render Adventure components into Bedrock-
  friendly legacy strings and emit one `.lang` file per loaded locale.
- **Auto Java→Bedrock fallback** (T-206) — `BedrockFallbackForm`
  synthesises a `SimpleForm` from any `JavaInventoryRenderer` when no
  dedicated `BedrockFormRenderer` is provided.
- **`.mcpack` pipeline** (T-207) — `ResourcePackBuilder.buildBedrockPack()`
  writes a stable Bedrock manifest using the fixed UUIDs in
  `BedrockPackConstants`.
- **Geyser item mappings** (T-208) — `GeyserMappingsBuilder` writes
  `mappings/sapientia_items.json`. Items with non-zero
  `customModelData()` get a deterministic Bedrock entry under their base
  material.
- **`/sapientia pack build {java|bedrock|all}`** (T-209) plus tab-complete
  + help entries.
- **Bedrock smoke harness** (T-210) — `scripts/smoke-bedrock.{sh,ps1}` +
  `docs/bedrock-smoke-checklist.md`.
- **Performance benchmarks** (T-211, P-009..P-012, P-014):
  `PlatformDetectBenchmark`, `CustomFormOpenBenchmark`,
  `BedrockMixOverheadBenchmark`, `GeyserMappingBenchmark`.
- `SapientiaItem#customModelData()` default and `ItemOverride.customModelData`
  YAML field, plus `ItemRegistry` propagation to spawned stacks (T-145).
- `UIService#open(Player, NamespacedKey, Object)` overload for descriptor-
  by-key open paths.
- `SapientiaAPI#openMachineUI(Player, EnergyNode)` and
  `SapientiaAPI#openUI(Player, NamespacedKey, Object)`.
- New i18n keys under `ui.machine.*`, `ui.filter.*`, plus the new
  `command.help.desc.pack-build-{bedrock,all}` and
  `command.pack.bedrock.*` strings — `verifyTranslations` parity preserved.

### Changed
- `BedrockFormsUIProvider#isReady` now uses
  `Bukkit.getPluginManager().getPlugin("floodgate")` instead of reflection.
- `ResourcePackBuilder` accepts optional `Messages` + `ItemRegistry`
  injections (no behaviour change for existing Java pack consumers).

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
