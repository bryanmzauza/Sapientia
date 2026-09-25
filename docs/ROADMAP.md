# Roadmap

This document describes where Sapientia is going. What has already shipped is recorded in
[CHANGELOG.md](CHANGELOG.md).

Sapientia is developed **era by era**. The general rules (eras, mining, materials, performance)
are in [jogabilidade.md](jogabilidade.md), and every era has its own document in
[eras/](eras/README.md) with each item, recipe, machine and task. Those documents are in
Portuguese.

**Status:** Done · In progress · Planned

## Standing requirements

These apply to every milestone:

- Content is declared in Java in `sapientia-content`; YAML only overrides existing content.
- Every player-facing screen works on Java and on Bedrock (through Geyser and Floodgate).
- Every player-facing string exists in both `en.yml` and `pt_BR.yml`.
- Every item and block ships with a bundled texture (see `scripts/textures/`).
- An item of era N is only made from ingredients of eras 0 to N.
- `./gradlew build` passes, including the translation, texture and era coherence checks.
- Breaking changes to `sapientia-api` only happen in a major version.
- Performance scales to 10 million active Sapientia blocks (machines, cables, pipes; 100,000 per
  player with 100 players): per-tick cost stays within a fixed budget, idle and passive blocks cost
  nothing, machines pause outside the activity radius, and per-chunk limits apply (see
  `jogabilidade.md`, section 9).

## How work is organized

Sapientia 2 is the era-based rewrite of the plugin. Its versions follow the eras:

| Version line | Contents |
|--------------|----------|
| **2.0.0** | Foundation 1 (performance) |
| **2.0.1** | Foundation 2 (progression and world) |
| **2.0.2** | Era 0 (arrival) |
| **2.0.x** | Later fixes and adjustments to the foundations and era 0 |
| **2.N.0** | Release of era N (era 1 is 2.1.0, era 2 is 2.2.0, … era 24 is 2.24.0) |
| **2.N.x** | Fixes and balance changes for era N |

1. **Foundations first.** Foundation 1 is 2.0.0, a major version because it removed an unused part
   of the public API; Foundation 2 is 2.0.1 (eras, research, minerals and plants, all additive in
   the API; the raw ore items become mineral fragments); era 0 follows as 2.0.2.
2. **Then one era at a time, in order.** Each era is a minor version: it adds content without
   breaking the API.
3. Tasks use `F<foundation>.<n>` and `E<era>.<n>` codes, which are also used in commit messages
   (`Refs: E3.2`).

**Current cycle:** 2.0.2 (era 0), ready for testing on a server.

---

## Foundation 1 — Performance · Done (2.0.0)

**Goal.** Replace the current full-scan tick loops with an engine that stays within a fixed budget
at 10 million active blocks.

- [x] F1.1 Single scheduler with a per-tick budget and time slicing, replacing the four tickers that
  scan every energy node (machines, petroleum, electronics, geology).
- [x] F1.2 Machine states (busy, idle, sleeping) with event-driven wake-up.
- [x] F1.3 Activity radius: machines run only within 4 chunks of a player and pause 30 seconds after
  the last player leaves (configurable).
- [x] F1.4 Per-chunk limits for all Sapientia blocks, processing machines and each machine type,
  with a clear message when a limit is reached.
- [x] F1.5 Networks solved as aggregates, recalculated only when their topology changes; logistics
  runs per network only when there is work. Solvers visit only the blocks that produce, store or
  consume; energy networks sleep until something changes, idle item and fluid networks back off.
- [x] F1.6 Compact per-chunk state storage (primitive arrays, numeric ids) instead of per-block
  objects: the block index (about 5 bytes per block), the machine scheduler (about 32 bytes per
  machine) and network topology (cables, pipes and junctions have no objects, about 50 bytes each).
- [x] F1.7 Network solving and persistence off the main thread: energy is solved on its own thread;
  all database writes and chunk reads run on one database thread. Item and fluid solvers stay on
  the main thread because they move items and fluids in the world.
- [x] F1.8 `/sapientia perf` showing the cost of each subsystem.
- [x] F1.9 Scale benchmarks with 10 million simulated blocks: `MachineSchedulerBenchmark` (2.1 ms
  per tick with 10 million registered machines, 50,000 run per tick), `NetworkScaleBenchmark`
  (10 million network blocks: 2.6 ms per energy cycle, off the main thread, with 1% of networks
  changing; 2 µs to unload and reload a chunk), `ChunkLoadBenchmark` (0.44 ms of main-thread time
  to load and unload a chunk with 1,000 blocks) and `MemoryFootprint`.
- [ ] F1.10 Verify the bundled textures on a Bedrock client through Geyser (carried over from
  1.11.0).

**Exit criteria.** The benchmark meets the targets in `jogabilidade.md` section 9.3; existing
content behaves the same inside the activity radius.

**Status against section 9.3.** Met: fixed main-thread budget, zero cost for passive, idle and
out-of-radius blocks, chunk load under 1 ms, 5 bytes per block in the block index and 32 bytes per
machine. Open: a cable or pipe still costs about 50 bytes in its network on top of the block index,
against the 4-byte target for passive blocks; the worst-tick target needs measuring on a loaded
server.

---

## Foundation 2 — Progression and world · Done (2.0.1)

**Goal.** Build the systems every era relies on.

- [x] F2.1 `Era` API, `ProgressionService` (migration V011), `/sapientia era` commands and
  configuration. Every built-in item and block belongs to an era.
- [x] F2.2 Era locks on crafting, machines, placement, planting and the guide; `sapientia.era.bypass`.
  Sapientia items no longer work as their vanilla base material in vanilla crafting, smelting or
  smithing.
- [x] F2.3 Per-player research: discoveries and prerequisites.
- [x] F2.4 Guide first page with the server era and the player's next goal.
- [x] F2.5 Natural-terrain tracking: per-section placed-block maps in chunk data.
- [x] F2.6 Mineral fragments: configurable drop tables by host block, depth, biome and dimension
  (46 minerals, pickaxe tiers by era, Fortune, no drops from Silk Touch or explosions).
- [x] F2.7 Veins derived from the world seed.
- [x] F2.8 Mineral composition, separation methods and tailings (the rules and data; the machines
  that separate come with their eras).
- [x] F2.9 Sapientia plants on vanilla crop blocks; wild seeds by biome (the system; the plants
  themselves come with era 1).
- [x] F2.10 Migration of the current raw ore items to the new mineral system.
- [x] F2.11 Automated era coherence check over all recipes. It runs over every registered recipe at
  server start (recipes are built from item stacks, which need a running server); the current
  recipes pass with no violations.

**Exit criteria.** An admin can set and advance the server era; locked content is hidden and
unusable; breaking natural rock drops fragments and placed blocks never do.

**Status.** Era commands, locks and the coherence check were verified on a Paper 26.3 server;
drops, research and the guide are covered by unit tests and still need a play test with a client.

---

## Eras · In progress

| Era | Version | Name | Highlights | Document |
|-----|---------|------|------------|----------|
| 0 | 2.0.2 | Arrival | Guide, Sapientia Workbench (done) | [era-00](eras/era-00-chegada.md) |
| 1 | 2.1.x | Stone Age | Agriculture (flax, herbs, quern, bread), charcoal pit, fire clay, clay furnace | [era-01](eras/era-01-pedra.md) |
| 2 | 2.2.x | Copper Age | Native copper, gold and silver; rice; salt pan; hand loom | [era-02](eras/era-02-cobre.md) |
| 3 | 2.3.x | Bronze Age | Tin, bronze, gold pan, scythe, cheese, pedal loom | [era-03](eras/era-03-bronze.md) |
| 4 | 2.4.x | Iron Age | Bloomery, bellows, lead, cupellation, mercury, lime | [era-04](eras/era-04-ferro.md) |
| 5 | 2.5.x | Classical Antiquity | Water wheel, mechanical machines, aqueducts, wine, brass, Roman concrete | [era-05](eras/era-05-antiguidade.md) |
| 6 | 2.6.x | Middle Ages | Windmill, blast furnace, Damascus steel, alembic and acids, beekeeping | [era-06](eras/era-06-idade-media.md) |
| 7 | 2.7.x | Renaissance | Precision mechanism, optical glass, printing press, New World crops, resin and latex | [era-07](eras/era-07-renascenca.md) |
| 8 | 2.8.x | Industrial Revolution | Steam network, coke, Bessemer steel, steam drill, harvester, power loom, sulfuric acid, rubber | [era-08](eras/era-08-revolucao-industrial.md) |
| 9 | 2.9.x | Electricity | LV network, electric machines, electrolysis, aluminium, water treatment, refrigeration | [era-09](eras/era-09-eletricidade.md) |
| 10 | 2.10.x | Age of Steel | MV network, steel alloys, flotation, quarry, fertilizers, soybeans | [era-10](eras/era-10-aco.md) |
| 11 | 2.11.x | Oil and Chemistry | Refinery, plastics, synthetic rubber, biofuels, greenhouse, penicillin, drill rig | [era-11](eras/era-11-petroleo-quimica.md) |
| 12 | 2.12.x | Electronics | HV network, silicon, clean room, lithography, processors, hydroponics | [era-12](eras/era-12-eletronica.md) |
| 13 | 2.13.x | Atomic Age | Uranium enrichment, fission reactor, EV network, radiation | [era-13](eras/era-13-atomica.md) |
| 14 | 2.14.x | Space Age | Titanium, rockets, satellites, GPS, RTG, tunnel boring machine | [era-14](eras/era-14-espacial.md) |
| 15 | 2.15.x | Information Age | Computers running logic programs, rare earths, precision agriculture, IV network | [era-15](eras/era-15-informacao.md) |
| 16 | 2.16.x | Robotics | Androids acting on the real world, lithium-ion batteries, farm drones | [era-16](eras/era-16-robotica.md) |
| 17 | 2.17.x | Renewable Energy | Solar, wind, fuel cells, battery banks, recycler, vertical farm | [era-17](eras/era-17-renovavel.md) |
| 18 | 2.18.x | Fusion | Superconductors, fusion reactor, LuV network | [era-18](eras/era-18-fusao.md) |
| 19 | 2.19.x | Nanotechnology | Graphene, molecular separator, molecular assembler, replicator, gene editing | [era-19](eras/era-19-nanotecnologia.md) |
| 20 | 2.20.x | Orbital Age | Orbital station, asteroid and lunar missions, solar satellites, ZPM network | [era-20](eras/era-20-orbital.md) |
| 21 | 2.21.x | Quantum Age | Quantum computer, item teleporter, quantum storage and gear | [era-21](eras/era-21-quantica.md) |
| 22 | 2.22.x | Antimatter | Particle accelerator, antimatter cells and reactor, UV network | [era-22](eras/era-22-antimateria.md) |
| 23 | 2.23.x | Interstellar Age | Exotic matter, warp core, interstellar probes, alien crops | [era-23](eras/era-23-interestelar.md) |
| 24 | 2.24.x | Singularity | Transmutation, programmable matter, food synthesis, Dyson swarm | [era-24](eras/era-24-singularidade.md) |

Each era's exit criteria: every task in its document is done, its content is playable from the
previous era's gate item without admin commands, and the standing requirements hold.

---

## Backlog

Ideas without a scheduled milestone:

- Custom appearance for placed machines, limited to blocks near players.
- Optional built-in resource pack delivery, so players receive the pack without a separate web host.
- Public research API so addons can add their own eras and items.
- Energy compatibility adapters for other plugins.
- External authoring tool that generates Java addon scaffolding.
- Chunk-level noise and pollution from heavy industry.

---

## Completed milestones

Details for each version are in [CHANGELOG.md](CHANGELOG.md).

| Version | Theme | Highlights |
|---------|-------|------------|
| 2.0.1 | Foundation 2 | Eras and research, era locks, 46 minerals from natural terrain, veins, separation rules, plant system |
| 2.0.0 | Foundation 1 | Budgeted engine, activity radius, per-chunk limits, compact storage, background persistence |
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
