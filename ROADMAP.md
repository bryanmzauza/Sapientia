# Roadmap

This document describes where Sapientia is going. What has already shipped is recorded in
[CHANGELOG.md](CHANGELOG.md).

Each milestone lists its scope and the criteria it must meet before release. Task identifiers
(`T-NNN`) are used in commit messages (`Refs: T-NNN`) and code comments. Every milestone also has to
meet the standing requirements below.

**Status:** Done · In progress · Planned

## Standing requirements

These apply to every milestone:

- Content is declared in Java in `sapientia-content`; YAML only overrides existing content.
- Every player-facing screen works on Java and on Bedrock (through Geyser and Floodgate).
- Every player-facing string exists in both `en.yml` and `pt_BR.yml`.
- Every item and block ships with a bundled texture (see `scripts/textures/`).
- `./gradlew build` passes, including the translation and texture coverage checks.
- Breaking changes to `sapientia-api` only happen in a major version.

---

## 1.12.0 — World generation and progression · Planned

**Goal.** A survival world can progress from vanilla materials to high-voltage machines without
admin commands. Today the ten raw metals are only obtainable through `/sapientia give`.

- T-480 Ore generation for the ten raw metals, with per-metal height ranges, vein sizes and a
  configuration switch per world. Update the raw metal guide descriptions, which currently point
  players to `/sapientia give`.
- T-481 Brine and rock salt: the desalinator produces real outputs instead of a water proxy.
- T-482 Biome-weighted gas mix for the atmospheric collector.
- T-483 Quarry mines real blocks inside an area selected with the wrench, instead of emitting
  slurry.
- T-484 Persist machine recipe progress across restarts.
- T-485 Benchmarks for machine processing and the quarry tick budget.
- T-486 Verify the bundled textures on a Bedrock client through Geyser (icons and names), which
  was not covered by the 1.11.0 release checks.

**Exit criteria.** On a new world with default settings, every item needed to build an HV laser
cutter can be obtained by playing; quarry and machine ticks stay within the tick budget.

---

## 1.13.0 — Logistics and tools completion · Planned

**Goal.** Finish the behaviour of blocks and tools that are placeable today but only partly work.

- T-490 GPS: transmitter coverage, markers shown on the handheld map.
- T-491 Prospector: scan nearby chunks and report ore and oil reservoirs.
- T-492 Use the max-flow solver for routing when `network.solver: maxflow` is set.
- T-493 Splitter ratio table, multi-pass filter chains and priority lanes (P0 to P3).
- T-494 Fluid valve toggled by hand and by logic programs; conveyor belt shows moving items.

**Exit criteria.** Every block and tool in the catalogue does what its guide entry describes.

---

## 1.14.0 — Presentation and server operations · Planned

**Goal.** Make Sapientia content look distinct in the world and easier to deploy.

- T-495 Custom appearance for placed machines (display entities on Java, Geyser equivalents on
  Bedrock), without extra cost for chunks that contain no Sapientia blocks.
- T-496 Optional built-in resource pack delivery, so players receive the pack without a separate
  web host.
- T-497 Bedrock form for editing the quarry area with numeric inputs.
- T-498 Compatibility matrix of tested Paper, Geyser and Floodgate versions.

**Exit criteria.** A server can install the plugin and deliver textures to Java and Bedrock players
with no manual file copying.

---

## 2.0.0 — Nuclear · Planned

**Goal.** Fission power, radiation and a recycling loop. First major version after the 1.x line.

- T-500 Uranium ore; fluids: sulfuric acid, molten salt, heavy water.
- T-501 Fission reactor multiblock (5×5×5 tungsten carbide casing) consuming uranium fuel cells
  and producing spent cells, heat and tritium.
- T-502 Coolant loop (water, molten salt or heavy water); meltdown when coolant runs out.
- T-503 Radiation system, with the model chosen in T-507.
- T-504 Hazmat and thermal armour sets.
- T-505 Reprocessing chain: recycler and chemical reactor recipes producing plutonium and
  radioactive scrap.
- T-506 EV tier: cable, capacitor, HV-to-EV transformer, tier 4 processor.
- T-507 Decision: radiation as status effects or as accumulated dose with decay.
- T-508 Decision: meltdown blast radius and how long the affected area stays hazardous.
- T-509 Tests for meltdown conditions, radiation protection and reprocessing yields.
- T-510 Benchmark for the fission reactor tick with full coolant and all hatches active.

**Exit criteria.** A reactor produces a stable 1,024 SU/t for 10 minutes with coolant; meltdown and
recovery are reproducible; radiation effects reach Bedrock players.

---

## 2.1.0 — Fusion and endgame · Planned

**Goal.** Complete the catalogue with fusion power, replication, satellite GPS and top-tier gear.

- T-511 Tungsten and iridium ores; hastelloy and tungsten carbide alloys.
- T-512 Fusion reactor multiblock (7×7×7) consuming deuterium and tritium.
- T-513 Plasma handling and ignition, bootstrapped by a uranium fuel cell.
- T-514 Replicator multiblock consuming UU-matter and a template item.
- T-515 UU-matter production from recycler scrap, with a list of items that cannot be replicated.
- T-516 Satellite GPS for world-wide coverage and the item teleporter.
- T-517 Quantum armour, jetpack, drill, chainsaw and multitool.
- T-518 Decision: which items the replicator refuses.
- T-519 Optional energy bridge exposing Sapientia energy to other plugins.
- T-520 Tests for fusion ignition, replicator restrictions and satellite coverage.
- T-521 Benchmark: stable fusion at 2,048 SU/t or more within 1.5 ms per tick.

**Exit criteria.** The full progression from vanilla iron to a fusion reactor is playable on a
single world without admin intervention, within the performance budget and with Bedrock parity.

---

## Backlog

Ideas without a scheduled version:

- Public research and progression API so addons can add their own tiers.
- Energy compatibility adapters for other plugins, if not delivered in 2.1.0.
- External authoring tool that generates Java addon scaffolding.
- Chunk-level noise and pollution from heavy industry.
- Full radiation simulation with a Geiger counter, if 2.0.0 ships the simpler model.

---

## Completed milestones

Details for each version are in [CHANGELOG.md](CHANGELOG.md).

| Version | Theme | Highlights |
|---------|-------|------------|
| 1.11.0 | Platform | Minecraft 26.3; bundled textures for all items and blocks; Java and Bedrock pack pipeline |
| 1.10.0 | Guide | Category-based guide navigation; descriptions for hard-to-find items |
| 1.9.1 | Androids | Android behaviour, upgrade effects, program selector |
| 1.9.0 | Androids | Eight android types, sixteen upgrades, placement caps |
| 1.8.1 | Logistics | Packager and unpackager processing; max-flow solver (not yet used for routing) |
| 1.8.0 | Logistics | Buffer, splitter, filter chamber, overflow, sensors, conveyor, valve |
| 1.7.1 | Geology | Quarry, drill rig, desalinator and gas collection processing |
| 1.7.0 | Geology | Multiblock controllers, GPS blocks, prospector, atmospheric gases |
| 1.6.1 | Electronics | Electrolysis, steam and gas generators, HV machine recipes |
| 1.6.0 | Electronics | HV tier, electronic components, gases, four new metals |
| 1.5.1 | Petroleum | Oil reservoirs, refinery and combustion power |
| 1.5.0 | Petroleum | Petroleum fluids and chemistry machines |
| 1.4.1 | Metallurgy | Machine recipe processing |
| 1.4.0 | Metallurgy | Metals and alloys, MV tier, first machines, multiblocks |
| 1.3.0 | Logic | Programmable logic graphs |
| 1.2.0 | Fluids | Pipes, pumps, tanks and drains |
| 1.1.0 | Logistics | Item cables, filters and routing policies |
| 1.0.0 | Bedrock | Bedrock forms and resource pack pipeline |
| 1.0.0-beta | Quality | Benchmarks and regression gate |
| 0.5.0 | Operations | YAML overrides and Java resource pack |
| 0.4.0 | Crafting | Workbench recipes and the in-game guide |
| 0.3.0 | Energy | Energy networks |
| 0.2.0 | Content | Custom items and persistent blocks |
| 0.1.0 | Foundation | Build, storage, translations, platform detection |
