# Kotlin Software Engineer - Production-Grade Feature Flag Library

## Scope

This repo is a Kotlin/JVM, multi-module Gradle build for a type-safe feature flag library.
Focus on Kotlin modules and core workflows (build, lint, test).

## Module Map (high level)

- `konditional-core`: core DSL + evaluation engine
- `konditional-runtime`: runtime registry + loading
- `konditional-serialization`: JSON snapshot/config codecs
- `konditional-observability`: public API for shadow evaluations
- `kontracts`: type-safe JSON Schema DSL
- `config-metadata`: shared metadata model
- `openapi`: OpenAPI artifacts
- `opentelemetry`: telemetry adapters
- `detekt-rules`: custom static analysis rules
- `buildSrc`: Gradle conventions and custom tasks

## Mandatory Constraints

- **ALWAYS** run `make check` before completing Kotlin tasks.
- **ALWAYS** run `llm-docs/scripts/extract-llm-context.sh` before finalizing MCP changes.
- **ALWAYS** use `rename_refactoring` for identifier renames.
- **NEVER** use multiple `return` statements (prefer expression bodies).

## Build / Lint / Test Commands

### Common Gradle Targets

- `./gradlew clean`
- `./gradlew build`
- `./gradlew check`
- `./gradlew test`
- `./gradlew detekt`
- `./gradlew compileKotlin`
- `./gradlew compileTestKotlin`

### Makefile Shortcuts

- `make clean`
- `make build`
- `make test`
- `make detekt`
- `make check` (detekt + tests)

### Run a Single Test (JUnit 5)

- `./gradlew test --tests 'package.ClassName'`
- `./gradlew test --tests 'package.ClassName.methodName'`

### Run a Single Test in a Module

- `./gradlew :konditional-core:test --tests 'package.ClassName'`
- `./gradlew :konditional-core:test --tests 'package.ClassName.methodName'`

### Other Module Examples

- `./gradlew :kontracts:test --tests 'io.amichne.kontracts.SchemaDslTest'`
- `./gradlew :konditional-runtime:test --tests 'io.amichne.konditional.runtime.SomeTest'`

## Code Style Guidelines

### Formatting & Imports

- Kotlin official style (`ij_kotlin_code_style_defaults = KOTLIN_OFFICIAL`).
- No wildcard imports (see `.editorconfig`).
- Keep imports grouped and alphabetized.
- Always end files with a newline.
- Prefer small, focused files over massive utilities.

### Naming

- Classes/objects: `UpperCamelCase`.
- Functions/vars/params: `lowerCamelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Feature flags: use explicit, domain names (avoid abbreviations).
- Prefer `Namespace`-scoped flag objects (`object AppFlags : Namespace("app")`).

### Types & API Design

- Prefer expression bodies over block bodies.
- Avoid `var` unless mutation is unavoidable.
- Prefer immutable collections in public APIs.
- Use sealed interfaces/classes for closed sets.
- Use `data class` for value types with structural equality.
- Use `value class` where a strong type prevents misuse.
- Prefer reified generics + inline functions for type-safe DSLs.
- Public APIs should be explicit about nullability and defaults.
- Add KDoc when generics or reflection are involved.

### Error Handling

- Prefer explicit result types (`ParseResult`, `ValidationResult`) over exceptions.
- Use `error()` only for programmer mistakes or impossible states.
- Include context in error messages (namespace/feature identifiers).
- Never swallow errors in parsing or evaluation paths.

### Control Flow

- Prefer `when` expressions and exhaustive handling.
- Avoid multiple `return` statements (expression body preferred).
- Avoid Java patterns (no builders via mutability, no “manager” classes).

### Concurrency

- Use structured concurrency.
- Require explicit `CoroutineContext` in public APIs.
- Propagate exceptions unless explicitly documented otherwise.

## Testing Guidelines

- JUnit 5 is standard (`useJUnitPlatform()` in Gradle).
- Use descriptive test names and cover edge cases.
- Prefer deterministic tests (no randomness, stable IDs for ramp-ups).
- Keep unit tests close to modules they validate.

## References

- `llm-docs/context/public-api-surface.md`
- `llm-docs/context/core-types.kt`

## IntelliJ MCP Workflow Hints

**Discovery:** `get_project_modules` → `get_project_dependencies` → `list_directory_tree`
**Locate:** `find_files_by_name_keyword` | `find_files_by_glob`
**Analyze:** `get_symbol_info` | `search_in_files_by_regex` | `get_file_problems`
**Edit:** `rename_refactoring` | `replace_text_in_file` | `reformat_file`
**Execute:** `get_run_configurations` → `execute_run_configuration` | `execute_terminal_command`
**Navigate:** `get_all_open_file_paths` | `open_file_in_editor`

## Cursor / Copilot Rules

- No `.cursor/rules`, `.cursorrules`, or `.github/copilot-instructions.md` found in this repo.

## Communication Protocol

- Dense, direct responses (no preambles).
- Present options with trade-offs when uncertain.
- State unknowns explicitly; never assume business logic.
- Enterprise integration is a first-class priority.
