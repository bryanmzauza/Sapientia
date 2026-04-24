# Contributing to Sapientia

Thanks for considering contributing! This document covers the essentials.

## Setup

1. JDK 25 (Temurin/Eclipse Adoptium recommended).
2. Clone the repository and run `./gradlew build`.
3. To launch a local Paper server with the plugin:

   ```powershell
   ./gradlew :sapientia-core:runServer
   ```

## Code style

- Java 25, UTF-8, `-parameters` enabled.
- Keep `sapientia-api` implementation-free — only interfaces, enums and small records.
- Any user-facing string goes to `sapientia-core/src/main/resources/lang/en.yml` **and** `pt_BR.yml`. Code references only the key.

## Commit conventions

Recommended format:

```
<type>(<scope>): <summary>

<optional body, imperative mood>

Refs: T-NNN
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci`, `chore`.

## Pull requests

- One PR per task (`T-NNN`) when possible.
- Include tests (`sapientia-api`, `sapientia-core`) and run `./gradlew build` locally.
- Describe the acceptance gate of the task the PR belongs to.

## Translations

- Any new key in `en.yml` must have a counterpart in `pt_BR.yml`.
- The CI `verifyTranslations` job fails if parity breaks.

## Reporting bugs

Open an issue with:
- Paper and Sapientia versions.
- Floodgate/Geyser present? Which versions?
- Full log (use a gist if large).
- Steps to reproduce.
