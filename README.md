# Sapientia

Modular tech/automation platform for **Minecraft Paper 26.1.2+** servers, with functional parity between **Java** and **Bedrock** (via Geyser + Floodgate).

Created and maintained by **[BRMZ.dev](https://brmz.dev)**.

> Status: `0.1.0-SNAPSHOT` — early development. No public release yet.

---

## Highlights

- 🧩 **Multi-module** — public API separated from the core; addons compile only against `sapientia-api`.
- 🌐 **Native Java + Bedrock** — per-platform `UIProvider`, automatic detection via Floodgate.
- 💾 **Embedded SQLite** — numbered migrations with checksum, HikariCP pool, WAL.
- ⏱️ **Unified scheduler** — adapts transparently to Paper and Folia.
- 🎯 **Tick bucketing** — 20 rotating buckets prevent tick spikes as the world grows.
- 🌍 **Built-in i18n** — `en` and `pt_BR` MiniMessage catalogs, no hardcoded strings.

## Requirements

| Component | Minimum version |
|-----------|-----------------|
| Java      | 25              |
| Paper     | 26.1.2          |
| Floodgate | 2.2.2 (optional, enables Bedrock) |
| Geyser    | 2.4.x (optional, enables Bedrock) |

## Build

```powershell
./gradlew build
```

The shadow JAR lands in `sapientia-core/build/libs/sapientia-core-<version>.jar`. Drop it into the server's `plugins/` folder.

## Running benchmarks

```powershell
./gradlew :sapientia-benchmarks:jmh                    # runs the JMH harness
./gradlew :sapientia-benchmarks:compareToBaseline       # fails if any bench regresses >10 % vs docs/benchmarks/baseline.json
./gradlew :sapientia-benchmarks:saveBenchmarkBaseline   # promote the latest result to the committed baseline
```

Current benches cover P-003 (energy graph rebuild, 500 nodes), P-007 (tick bucket dispatch, 20 000 tickables across 20 buckets), and the Bedrock parity suite — P-009/P-010 (`PlatformDetectBenchmark`), P-011 (`CustomFormOpenBenchmark`), P-012 (`BedrockMixOverheadBenchmark`), P-014 (`GeyserMappingBenchmark`). See [docs/performance-contract.md](docs/performance-contract.md).

## Resource packs

```powershell
# Java + Bedrock packs at once
./gradlew :sapientia-core:buildPluginJar
# In-game (op required):
#   /sapientia pack build java
#   /sapientia pack build bedrock
#   /sapientia pack build all
```

The Bedrock pipeline emits `plugins/Sapientia/sapientia-bedrock.mcpack` containing a stable manifest, generated `.lang` files for every loaded locale, and a Geyser `mappings/sapientia_items.json`. Smoke-test a release with [scripts/smoke-bedrock.ps1](scripts/smoke-bedrock.ps1) (or `.sh`) and the [Bedrock smoke checklist](docs/bedrock-smoke-checklist.md).

## Layout

```
sapientia-api/         public contract (interfaces, enums, events)
sapientia-core/        main implementation (JavaPlugin + services)
sapientia-content/     bundled content (declarative machines/items)
sapientia-bedrock/     Bedrock UI provider (Floodgate soft-dep)
sapientia-testkit/     test utilities for addons
sapientia-benchmarks/  JMH benchmarks
```

## Roadmap

Full plan in [ROADMAP.md](ROADMAP.md).

## License

Distributed under the **MIT** license — see [LICENSE](LICENSE). © BRMZ.dev.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR.
