# Changelog

All notable changes to Sapientia are documented in this file.

The format is based on [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/), and the
project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Breaking changes to the
public `sapientia-api` module only happen in major versions.

Versions 0.1.0 through 1.10.0 were development milestones; no binaries were published for them.

## [Unreleased]

## [1.11.0] - 2026-09-25

Moves the plugin to Minecraft 26.3 and gives every built-in item and block its own texture on both
Java and Bedrock.

### Added

- Bundled textures for all 254 built-in items and blocks, shipped inside the plugin jar. Machines
  render as 3D blocks in the inventory and use trim colours to show their voltage tier; metals,
  components and android upgrades have dedicated sprites, and tiered items show tier pips.
- Items now carry the `minecraft:item_model` component so the Java client picks up the bundled
  models. Controlled by the new `resource-pack.item-models` option (default `true`).
- `/sapientia pack build java` merges the bundled assets with any file placed in
  `plugins/Sapientia/pack/` (operator files win) and reports the pack's SHA-1 for the
  `resource-pack-sha1` entry in `server.properties`. Rebuilding an unchanged pack produces the
  same hash.
- `/sapientia pack build bedrock` now includes item textures and inventory icons, and writes Geyser
  custom item mappings to `plugins/Sapientia/geyser/sapientia_items.json`. When Geyser runs on the
  same server, the pack and the mappings are copied into its `packs/` and `custom_mappings/`
  folders automatically.

### Changed

- **Breaking:** requires Paper 26.3 or newer (`api-version: 26.3`). Java 25 is still required.
- The default resource pack format is now 97 (Minecraft 26.3), and `pack.mcmeta` uses the
  `min_format`/`max_format` fields. Configured values below 65 are replaced with the default and
  logged, so existing `config.yml` files keep working.
- `pack.mcmeta` is always generated; a copy in `plugins/Sapientia/pack/` is ignored.
- The Bedrock pack manifest version now follows the plugin version, so clients download updated
  packs instead of reusing a cached copy.
- Geyser mappings use format version 2, matched by item model, and are no longer placed inside the
  `.mcpack`.
- The SQLite driver is now provided by Paper instead of being embedded, which reduces the plugin
  jar from about 15.7 MB to 2.2 MB. Paper was already the driver in use at runtime.
- Custom model data is written through the data component API; behaviour is unchanged.
- Development: the Gradle wrapper is now 9.8.0 and `run-paper` 3.1.0; `:sapientia-core:runServer`
  starts Paper 26.3. The CI artifact now contains only the plugin jar.

### Removed

- The `plugins/Sapientia/pack/bedrock/` staging folder is no longer used and can be deleted.

### Fixed

- `/sapientia reload` printed a missing-translation placeholder instead of its confirmation.
- The Bedrock pack build message showed literal `<pack>` and `<mappings>` placeholders instead of
  the file paths.
- `/sapientia help` did not list `/sapientia fluids`.
- Guide icons showed the vanilla base item (for example sugar for dusts) instead of the Sapientia
  texture. Category buttons on the first page now use representative Sapientia items.
- The `sapientia.command.logistics` and `sapientia.command.fluids` permissions were checked but not
  declared in `plugin.yml`. They are now declared with the same default (op).

## [1.10.0] - 2026-04-26

Reorganises the in-game guide and documents how to obtain items that have no obvious source.

### Added

- Guide navigation in three levels: categories (Materials, Tools, Machines, Energy, Logistics,
  Info), a paginated list per category (28 entries per page) and the entry detail, with Back
  returning to the originating page. Bedrock players get equivalent forms.
- Descriptions for items whose origin is not obvious: the ten raw metals, the silicon wafer and the
  six alloy ingots. Any item with a `<key>.desc` entry next to its `<key>.name` now shows it in the
  guide.

### Fixed

- The plugin failed to enable because the laser cutter recipe referenced the silicon wafer before
  it was registered.

## [1.9.1] - 2026-04-26

Makes the eight androids from 1.9.0 perform work.

### Added

- Per-type android behaviour. Farmer, lumberjack, miner, fisherman, butcher and slayer consume fuel
  from the container above and output simulated loot to the container below; the builder places
  blocks from its input container within its scan radius; the trader exchanges nine items for one
  emerald.
- Upgrade effects: AI chips set the scan radius (4, 6, 9, 13 blocks), motor chips the cooldown
  (20, 14, 9, 5 ticks), armour plates the health and damage reduction, and fuel modules the fuel
  buffer (1,000 to 64,000 mB). Solid fuels: coal, charcoal, blaze powder and blaze rods.
- Program selector: right-clicking an android opens a menu listing the stored logic programs to
  assign or clear.
- Logic nodes `comparator_read` (redstone power, 0 to 15) and `fluid_level_read` (tank fill, 0 to
  100 %).
- Android tick benchmark for 100 and 200 androids.

### Changed

- `SapientiaAndroidTickEvent` now fires from the live android loop. Cancelling it skips the action
  but still starts the cooldown.
- Stored android timers are reset when the plugin loads, because their meaning changed from a
  timestamp to a tick counter.

## [1.9.0] - 2026-04-26

Adds androids: programmable machines that automate farming, gathering, building and trading.

### Added

- Eight android blocks (farmer, lumberjack, miner, fisherman, butcher, builder, slayer, trader)
  with persistent state.
- Sixteen android upgrades: AI chip, motor chip, armour plate and fuel module, each in four tiers,
  with crafting recipes.
- Placement limits of 4 androids per chunk and a configurable server-wide cap
  (`androids.cap.server`, default 200).
- API: `AndroidType`, `AndroidUpgrade`, `AndroidNode`, `AndroidService` (via
  `SapientiaAPI#androids()`) and the cancellable `SapientiaAndroidTickEvent`.

## [1.8.1] - 2026-04-25

Activates the packager and unpackager and adds an optional max-flow item router.

### Added

- Packager and unpackager processing: the packager bundles one stack from the container above into
  the container below, and the unpackager reverses it. Each bundle fires the cancellable
  `SapientiaItemPackagedEvent`.
- A max-flow (Edmonds-Karp) solver and the `network.solver: legacy|maxflow` option. The option is
  read and validated, but item routing still uses the default solver.
- Item routing benchmark on 100- and 1,000-node networks.

## [1.8.0] - 2026-04-25

Adds advanced item and fluid logistics blocks.

### Added

- Item logistics: buffer, splitter, filter chamber, overflow module, comparator sensor, packager,
  unpackager and conveyor belt.
- Fluid logistics: valve and level sensor.
- Crafting recipes for all ten blocks.
- API: `SapientiaItemPackagedEvent`.

## [1.7.1] - 2026-04-25

Makes the geology and atmosphere machines from 1.7.0 operate.

### Added

- The quarry controller consumes energy and outputs slurry into the tank above.
- The drill rig has a 20 % chance per cycle to extract crude oil from below bedrock.
- The desalinator turns 100 mB of water into 90 mB of fresh water.
- The gas extractor collects nitrogen and the atmospheric collector rotates between nitrogen, argon
  and carbon dioxide.
- The three multiblock controllers join energy networks as high-voltage consumers.

## [1.7.0] - 2026-04-25

Adds large-scale resource gathering and GPS infrastructure.

### Added

- Multiblock controllers for the quarry (3×3×4), drill rig (5×5×8) and desalinator (5×3×3).
- Gas extractor and atmospheric collector machines.
- GPS transmitter and marker blocks, the handheld GPS map and the prospector. Coverage, map display
  and prospecting are not implemented yet.
- Fluids: argon, carbon dioxide and liquid oxygen.
- Crafting recipes for the new blocks and items.

## [1.6.1] - 2026-04-25

Makes the high-voltage machines from 1.6.0 operate.

### Added

- Electrolyzer: 100 mB of water becomes 200 mB of hydrogen and 100 mB of oxygen.
- Boiler and condenser convert between water and compressed air.
- Geothermal generator output scales with adjacent lava; the gas turbine burns hydrogen or
  ethylene; the RTG produces a constant trickle without fuel.
- Rolling mill recipes (ingot to wire) and laser cutter recipes (silicon ingot to wafers).

## [1.6.0] - 2026-04-25

Adds the electronics chain and the high-voltage (HV) tier.

### Added

- Four raw metals (aluminium, silicon, titanium, lithium) and three alloys (stainless steel,
  Damascus steel, nichrome), bringing the metallurgy catalogue to 138 items.
- Seventeen electronic components: silicon wafer, motors, circuits, processors and coils in three
  tiers, RAM in two tiers, and HDD and SSD storage.
- HV cable, capacitor and MV-to-HV transformer.
- Generators: geothermal, gas turbine and RTG.
- HV machines: electrolyzer, rolling mill, laser cutter and chemical reactor.
- Gases (hydrogen, oxygen, nitrogen, chlorine, ethylene, compressed air) and gas handling blocks:
  pressurized pipe, gas compressor, boiler, condenser, liquefier and phase separator.
- Crafting recipes for the new content.

## [1.5.1] - 2026-04-25

Completes the petroleum chain from crude oil to electricity.

### Added

- Finite crude oil reservoirs per chunk (10,000 to 100,000 mB) that regenerate slowly. Pumpjacks
  drain them into the tank above.
- The oil refinery splits crude oil into diesel, gasoline, lubricant and water.
- The combustion generator burns diesel or gasoline and the biogas generator burns nutrient broth.
- Item recipes for the cracker, fermenter, still and bioreactor.

## [1.5.0] - 2026-04-25

Adds petroleum and basic chemistry blocks.

### Added

- Fluids: crude oil, diesel, gasoline, lubricant and nutrient broth.
- Pumpjack, oil refinery controller (5×5×7 multiblock) and stainless steel casing.
- Chemistry machines: cracker, fermenter, still and bioreactor.
- Combustion generator (MV) and biogas generator (LV).
- Crafting recipes for the new blocks.

## [1.4.1] - 2026-04-25

Machines now process recipes.

### Added

- Machine processing: a machine takes input from the container above, spends energy when a recipe
  completes and outputs to the container below. Progress is kept in memory and resets on restart.
- Around 40 machine recipes covering crushing, smelting, pressing, wire drawing, rod cutting and
  block compression.
- Induction furnace alloy recipes for steel, invar and kanthal.

## [1.4.0] - 2026-04-25

Adds metallurgy and the medium-voltage (MV) tier.

### Added

- Six metals (copper, tin, zinc, lead, silver, nickel) in nine forms and three alloys (bronze,
  brass, electrum) in eight forms: 78 items.
- Voltage tiers LV, MV, HV and EV. API: `MachineTier` and `TierCompatibility`, which define how
  mismatched tiers behave (a higher tier burns a lower one; a lower tier is clamped).
- Machines: macerator, ore washer, electric furnace, bench saw (LV) and mixer, compressor, plate
  press, extractor (MV).
- MV cable, MV capacitor, LV-to-MV transformer and LV/MV machine casings.
- Induction furnace controller (3×3×3 multiblock) and shape validation helpers in the API.
- More than 50 crafting recipes.

## [1.3.0] - 2026-04-25

Adds programmable logic.

### Added

- Logic programs as directed acyclic graphs, compiled in a deterministic order and evaluated every
  5 ticks. Programs with cycles or unknown nodes are rejected; programs that throw are disabled.
- Built-in nodes: constants, arithmetic, comparison, boolean logic, branching, memory, tick counter
  and logging.
- `/sapientia logic list|info|load|unload|enable|disable|export|tick` with the
  `sapientia.command.logic` permission. Programs are stored in the database and can be exported to
  YAML.
- API: `LogicService` (via `SapientiaAPI#logic()`) and the cancellable `SapientiaLogicTickEvent`.

## [1.2.0] - 2026-04-25

Adds fluid logistics.

### Added

- Fluid pipe, pump, tank and drain. Pumps take water and lava from source blocks and cauldrons;
  drains place them back.
- Tank capacity and pipe throughput scale with tier. A tank holds one fluid type at a time.
- Built-in fluids: water, lava and milk.
- `/sapientia fluids info` with the `sapientia.command.fluids` permission.
- API: `FluidService` (via `SapientiaAPI#fluids()`), `FluidType`, `FluidStack` and the
  `SapientiaFluidFlowEvent` and `SapientiaFluidTransferEvent` events.

## [1.1.0] - 2026-04-25

Adds item logistics.

### Added

- Item cable, producer, consumer and filter blocks that move items between vanilla containers.
- Per-network routing policies: round robin, priority and first match.
- Whitelist and blacklist filters with wildcards (`*`, `namespace:*`), editable with
  `/sapientia logistics filter add|remove|clear|list` and viewable in a filter UI on Java and
  Bedrock.
- `/sapientia logistics info|policy|filter`.
- API: `ItemService` (via `SapientiaAPI#logistics()`) and the `SapientiaItemFlowEvent`,
  `SapientiaItemFilterEvent` (cancellable) and `SapientiaItemRouteEvent` events.

### Removed

- The `experimental.filter` configuration option; the filter UI is always available.

## [1.0.0] - 2026-04-24

Bedrock players get the same interfaces and resource pack pipeline as Java players.

### Added

- Bedrock forms for the machine UI and the guide, with an automatic fallback form for any menu that
  has no dedicated Bedrock layout.
- `/sapientia pack build java|bedrock|all`, producing a Java resource pack and a Bedrock `.mcpack`
  with translated `.lang` files.
- Bedrock smoke-test scripts and platform detection benchmarks.
- API: `SapientiaAPI#openMachineUI` and `SapientiaAPI#openUI`.

## [1.0.0-beta] - 2026-04-24

Adds performance benchmarks and a regression gate.

### Added

- JMH benchmarks for energy graph rebuilds and tick bucket dispatch
  (`./gradlew :sapientia-benchmarks:jmh`).
- `compareToBaseline`, which fails when any benchmark regresses more than 10 % against the stored
  baseline, and `saveBenchmarkBaseline` to update that baseline.

## [0.5.0] - 2026-04-24

Server operators can rebalance the built-in content without code changes.

### Added

- YAML overrides for items, blocks and recipes in `plugins/Sapientia/overrides/`. Invalid entries
  are logged and skipped.
- `/sapientia reload content` applies overrides without a restart.
- `/sapientia pack build java` and the `resource-pack.pack-format` option.
- API: `ContentOverrides` (via `SapientiaAPI#overrides()`).

## [0.4.0] - 2026-04-24

Adds crafting and the in-game guide.

### Added

- The Sapientia workbench with shaped 3×3 recipes that accept vanilla and Sapientia ingredients.
- The guide item, listing every item and block by category; locked entries show as placeholders
  until unlocked, and recipes unlock when first crafted.
- API: `RecipeRegistry`, `GuideService`, `UnlockService` and the cancellable
  `SapientiaRecipeCompleteEvent`.

## [0.3.0] - 2026-04-24

Adds the energy system.

### Added

- Generator, cable, capacitor and consumer blocks forming energy networks that split and merge as
  blocks are placed and broken. Energy is distributed every 10 ticks and persisted.
- API: `EnergyService` (via `SapientiaAPI#energy()`) and the `SapientiaEnergyFlowEvent` and
  `SapientiaMachineTickEvent` events.

## [0.2.0] - 2026-04-24

Adds custom items and persistent custom blocks.

### Added

- API: `SapientiaItem` and `SapientiaBlock` for content defined in Java, plus item interaction and
  block place, break and interact events.
- Persistent custom blocks loaded and unloaded with their chunks, with asynchronous batched writes.
- Wrench item and pedestal and console blocks.
- Build checks for translation parity between English and Brazilian Portuguese and for untranslated
  text sent to players.

## [0.1.0] - 2026-04-24

Initial project foundation.

### Added

- Multi-module Gradle build targeting Java 25 and Paper.
- Public API module with machine, energy and platform types.
- Translations in English and Brazilian Portuguese using MiniMessage.
- Embedded SQLite storage with checksummed migrations.
- Scheduler support for Paper and Folia.
- Bedrock player detection through Floodgate.
- `/sapientia give`, `/sapientia reload` and `/sapientia help`.
