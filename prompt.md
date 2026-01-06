Goal

- Fully replace legacy :core with the split modules (:konditional-core, :konditional-runtime, :konditional-serialization, :konditional-observability) and delete :core from the
  build, source tree, docs, and published coordinates.

Guiding Principles (Circular Dependency Hard Rules)

- :konditional-core contains only abstractions + minimal consumer DSL/public surface.
- :konditional-core must not depend on serialization/runtime/observability implementations.
- :konditional-runtime, :konditional-serialization, :konditional-observability depend “inwards” on :konditional-core (dependency inversion).
- No -Xfriend-paths as a long-term solution; remove once refactor is complete.
- Use @RequiresOptIn + “internal API” packages for implementation contracts when Kotlin internal visibility would otherwise force circularity.

———

## 0) Preconditions / Baseline

1. Confirm IDE index is ready (ide_index_status).
2. Confirm make check currently passes (baseline snapshot).
3. Inventory current module boundaries and published artifact IDs:
    - settings.gradle.kts
    - core/build.gradle.kts
    - konditional-*/build.gradle.kts
4. Create a tracking doc section in MODULE_SPLIT_STATUS.md for this refactor plan (phases + exit criteria).

———

## 1) Define the Target Architecture (Contracts First)

1. Freeze the desired minimal public API for :konditional-core (use prompt.md as source of truth).
2. Add a strict “API boundary policy”:
    - Decide on one: Kotlin Binary Compatibility Validator OR explicit public-api-surface.md gate.
    - Document which packages are allowed to be public in :konditional-core (e.g., io.amichne.konditional.api, ...context, ...values, ...core.dsl).
3. Introduce an opt-in annotation in :konditional-core for internal contracts:
    - Example: @RequiresOptIn(level = RequiresOptIn.Level.ERROR) annotation like @KonditionalInternalApi.
    - Policy: anything required for other modules to implement core abstractions lives behind this opt-in.

Exit criteria

- A written “core public API allowlist” exists and is enforced (even if initially permissive).

———

## 2) Break Circular Dependencies Through Refactoring (Industry Best Practice Path)

### 2.1 Extract interfaces to :konditional-core

1. Namespace registry contract
    - Define NamespaceRegistry as interface only in :konditional-core.
    - Ensure it does not reference serialization DTOs or observability types.
    - Split into:
        - Consumer-safe surface (minimal): evaluate access + registry kill switch.
        - Opt-in “runtime contract” (behind @KonditionalInternalApi): mutation/lifecycle hooks if needed.
2. Metrics contract
    - In :konditional-core, define minimal metrics interface(s) (e.g., KonditionalLogger, MetricsSink).
    - Keep event shapes minimal and stable (primitives/strings/value classes), not heavy DTO graphs.
3. Configuration view types
    - Define view interfaces in :konditional-core:
        - ConfigurationView
        - FlagDefinitionView<T> (or non-generic + typed accessors via ValueType)
        - RuleView / TargetingView / RampUpView as needed
    - Ensure evaluation engine depends only on these views (not concrete data classes).

### 2.2 Move implementations to specialized modules

1. :konditional-runtime
    - Move InMemoryNamespaceRegistry and any registry lifecycle/admin operations here.
    - Implement NamespaceRegistry (core interface).
2. :konditional-observability
    - Move full metrics collectors, hooks, and shadow evaluation APIs here.
    - Implement/adapt MetricsSink + logging contracts from core.
3. :konditional-serialization
    - Move all snapshot/config JSON codecs and all concrete configuration data classes here.
    - Concrete Configuration implements ConfigurationView.
    - Ensure serialization can build configuration without referencing core-internal classes.

### 2.3 Use dependency inversion everywhere

1. Remove any imports in :konditional-core that reference:
    - io.amichne.konditional.serialization.*
    - runtime implementations (InMemoryNamespaceRegistry)
    - observability implementations (RegistryHooks, concrete MetricsCollector)
2. Ensure:
    - :konditional-runtime depends on :konditional-core (+ optionally :konditional-serialization)
    - :konditional-observability depends on :konditional-core and/or :konditional-runtime (depending on where hooks live)
    - :konditional-serialization depends on :konditional-core only

Exit criteria

- :konditional-core compiles with zero references to serialization/runtime/observability packages.
- :konditional-serialization compiles without -Xfriend-paths.

———

## 3) Refactor Evaluation Engine to Use Views (Core Depends Only on Abstractions)

1. Identify evaluation entrypoints and data flow (use ide_call_hierarchy on Feature.evaluate / evaluateInternal).
2. Replace direct dependencies on concrete config/flag types with view interfaces:
    - Evaluation reads ConfigurationView from NamespaceRegistry.
    - Evaluation resolves FlagDefinitionView for a FeatureId.
    - Evaluation applies rules via view-provided targeting + ramp-up + values.
3. Convert “internal-only” evaluation helper types:
    - If runtime/serialization needs to create them, make them either:
        - public but opt-in (@KonditionalInternalApi), OR
        - private to core and created only through factory functions that accept view inputs.
4. Ensure Namespace (core) is just identity + feature DSL container:
    - No direct loading/rollback methods in core.
    - Provide runtime-only extensions in :konditional-runtime (e.g., Namespace.load(...)) that operate via registry implementations.

Exit criteria

- Core evaluation is fully driven by interfaces and stable IDs, not concrete serialization DTOs.

———

## 4) Eliminate Internal Visibility Cross-Module Leaks (Remove Friend Paths)

1. Remove current friend-path configuration from:
    - konditional-serialization/build.gradle.kts
    - konditional-observability/build.gradle.kts
2. Any remaining “needs internal access” must be fixed via:
    - proper public opt-in contracts in :konditional-core, or
    - shifting the logic into the module that owns the internals.

Exit criteria

- All modules compile without -Xfriend-paths.

———

## 5) Gradle + Publishing Consolidation (Make New Modules First-Class)

1. Align Gradle configuration across new modules (toolchain, detekt, publishing):
    - Either create a shared convention plugin, or duplicate minimally but consistently.
2. Ensure each module has correct artifactId and POM metadata.
3. Fix module dependencies for downstream projects in this repo:
    - :opentelemetry should depend on :konditional-observability and/or :konditional-runtime instead of :core.
    - :openapi, :kontracts, etc. updated as needed.
4. Update Makefile/CI scripts if they reference :core explicitly.

Exit criteria

- ./gradlew publishToMavenLocal works for the new modules (if publishing is part of CI).
- make check is green with :core still present but no longer required by dependents.

———

## 6) Migrate Tests from :core to Split Modules

1. Categorize tests by ownership:
    - Core-only API/evaluation tests → :konditional-core
    - Snapshot/codec tests → :konditional-serialization
    - Registry/lifecycle tests → :konditional-runtime
    - Hooks/metrics/shadow tests → :konditional-observability
2. Move tests using semantic refactors (using intellij MCP tooling):
    - ide_find_references + ide_refactor_rename where needed.
3. Recreate any needed testFixtures in the appropriate module(s) (or replace with internal test utilities).
4. Ensure the new modules run tests in CI (restore their test source sets).

Exit criteria

- All tests that used to run in :core now run in the correct module(s).
- ./gradlew test runs with no reliance on :core test sources.

———

## 7) Documentation + API Surface Update (Parallel Track)

1. Update docs to new dependency coordinates:
    - “Getting started” depends on konditional-core.
    - “Loading snapshots” depends on konditional-serialization + konditional-runtime.
    - “Metrics/Telemetry/Shadow” depends on konditional-observability.
2. Add a migration guide from :core to split artifacts (explicit import/package mapping).
3. Update llm-docs/context/public-api-surface.md expectations accordingly.

Exit criteria

- Docs build cleanly (if docs build exists).
- Migration guide covers common upgrade paths.

———

## 8) Remove Legacy :core Module (Final Phase)

1. Ensure no project depends on :core:
    - Use ide_find_references on project(":core") usages (Gradle files) and key packages.
2. Remove from Gradle:
    - Delete include("core") from settings.gradle.kts.
    - Delete core/ directory.
3. Confirm no published artifact references core module coordinates.
4. Run full verification:
    - make check
    - bash llm-docs/scripts/extract-llm-context.sh

Exit criteria

- Repository builds and tests with only :konditional-core + sibling modules.
- No references to legacy :core remain in code, docs, or build logic.

———

## 9) Final Validation Checklist (Must Pass Before “Done”)

- ./gradlew :konditional-core:compileKotlin :konditional-runtime:compileKotlin :konditional-serialization:compileKotlin :konditional-observability:compileKotlin
- make check
- No -Xfriend-paths in any module build files.
- llm-docs/scripts/extract-llm-context.sh executed and committed outputs updated.
- Public API of :konditional-core matches the allowlist; internal contracts are opt-in gated.

———

## Tooling / Execution Notes for the Implementing LLM

- Prefer JetBrains Intellij MCP tools over rg for symbol work:
    - ide_find_definition, ide_find_references, ide_find_implementations, ide_refactor_rename, ide_diagnostics.
- Make small, compilable steps; after each phase run targeted Gradle compile, then make check at phase boundaries.
