# Contributing to Sapientia

Thanks for considering a contribution. This document covers the essentials.

## Setup

1. JDK 25 (Eclipse Temurin recommended).
2. Clone the repository and run `./gradlew build`.
3. To launch a local Paper 26.3 server with the plugin:

   ```bash
   ./gradlew :sapientia-core:runServer
   ```

4. For texture work: Python 3 with Pillow and PyYAML (`pip install pillow pyyaml`).

Run a single test class with, for example:

```bash
./gradlew :sapientia-core:test --tests "dev.brmz.sapientia.core.energy.NetworkGraphTest"
```

## Code style

- Java 25, UTF-8, `-parameters` enabled; the build compiles with `-Xlint:all` and should stay free of
  warnings.
- Keep `sapientia-api` implementation-free: interfaces, enums and small records only.
- User-facing text goes through `Messages`. Calling `sendMessage(String)` directly in
  `sapientia-core` fails the build unless the class is annotated with `@AllowLiteral`.

## Translations

- Every key in `sapientia-core/src/main/resources/lang/en.yml` needs a counterpart in `pt_BR.yml`.
  `./gradlew verifyTranslations` (part of `check`) fails when the key sets differ.
- Every literal key used in code must exist in both files; a test checks this.
- An item's guide description is picked up automatically from `<key>.desc` next to its
  `<key>.name`.

## Content and textures

- New content is a Java class in `sapientia-content`, registered in `ContentBootstrap`. Register
  items and blocks before the recipes that use them.
- Every item and block needs a texture. Add a spec for the new id in `scripts/textures/specs.py`,
  then run `python scripts/textures/generate_textures.py --preview build/textures-preview.png` and
  check the preview. The script fails if a named item has no spec, and the test suite fails if a
  named item has no bundled model.
- Commit the regenerated files under `sapientia-core/src/main/resources/pack/`.

## Database migrations

Add a new `sapientia-core/src/main/resources/db/migrations/VNNN__description.sql` file for every
schema change. Never edit a migration that has already shipped: the plugin stores a checksum of
each applied migration and refuses to start if one changes.

## Manual smoke test

Run before a release. `scripts/smoke-bedrock.sh` (or `.ps1`) prepares a local Paper server with
Floodgate and Geyser; drop `paper.jar`, `plugins/floodgate.jar` and `plugins/Geyser-Spigot.jar`
into `build/smoke-bedrock/` first.

1. The server reaches `Done` with no errors from Sapientia, Floodgate or Geyser.
2. `/sapientia pack build all` succeeds. Apply the Java pack on a Java client and confirm the
   Geyser copy was installed (or copy it manually), then restart Geyser.
3. A Java player and a Bedrock player join. `/sapientia help` renders without raw MiniMessage tags.
4. `/sapientia give <player> guide` on both: the guide opens (chest on Java, forms on Bedrock) and
   navigates index, category, entry and back.
5. Give a few items of each family (metals, components, upgrades, machines): each shows its own
   texture on Java and its icon and name on Bedrock.
6. Place a generator, cable and electric furnace with a chest of copper dust above and an empty
   chest below: ingots appear below, and the machine UI opens on both clients.
7. Restart the server: placed blocks, tanks and energy buffers persist.

## Versioning and releases

- The version lives in the root `build.gradle.kts`. Between releases it carries a `-SNAPSHOT`
  suffix (for example `1.11.0-SNAPSHOT`).
- Record changes under `## [Unreleased]` in `CHANGELOG.md` as you go, written for server operators
  and addon developers.
- To release: remove `-SNAPSHOT`, rename the `[Unreleased]` section to the version and date, update
  the milestone in `ROADMAP.md`, and tag the commit `vX.Y.Z`.

## Commit conventions

```
<type>(<scope>): <summary>

<optional body, imperative mood>

Refs: T-NNN
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci`, `chore`.

## Pull requests

- One pull request per task (`T-NNN`) when possible.
- Include tests and run `./gradlew build` locally.
- Describe how the change meets the exit criteria of its milestone in `ROADMAP.md`.

## Reporting bugs

Open an issue with:

- Paper and Sapientia versions.
- Whether Floodgate and Geyser are installed, and their versions.
- The full server log (use a gist if it is large).
- Steps to reproduce.
