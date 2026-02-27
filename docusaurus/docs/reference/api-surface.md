---
title: API Surface
sidebar_position: 2
---

# API Surface

Exhaustive claim-linked public API catalog from `docs/claim-trace/claim-signature-links.json`, grouped by module and sorted deterministically.

<span id="claim-clm-pr01-01a"></span>
Konditional models feature declarations through namespace-owned typed feature definitions.

<span id="claim-clm-pr01-01b"></span>
Runtime configuration ingestion is exposed through a snapshot loader that returns `Result` and supports typed parse failures.

<span id="claim-clm-pr01-02a"></span>
Deterministic ramp-up behavior is backed by stable bucketing functions.

<span id="claim-clm-pr01-02b"></span>
Namespace-scoped runtime operations are implemented by an in-memory namespace registry.

<span id="claim-clm-pr01-03a"></span>
Feature declarations are modeled as typed entities under `Namespace` and `Feature` abstractions.

<span id="claim-clm-pr01-03b"></span>
Parse boundary failures are represented with explicit `ParseError` and `KonditionalBoundaryFailure` types.

<span id="claim-clm-pr01-04a"></span>
First-success routes correspond to concrete runtime APIs for ramp-up and snapshot loading.

<span id="claim-clm-pr01-05a"></span>
Adoption phases rely on load, disable, and rollback runtime operations.

## konditional-core

| Claim ID | Kind | Signature | Source page | Concept/guide link |
| --- | --- | --- | --- | --- |
| `CLM-PR01-01A` | `type` | `io.amichne.konditional.core.Namespace` | [/overview/start-here.md](/overview/start-here.md) | [/overview/start-here.md](/overview/start-here.md) |
| `CLM-PR01-01A` | `type` | `io.amichne.konditional.core.features.Feature` | [/overview/start-here.md](/overview/start-here.md) | [/overview/start-here.md](/overview/start-here.md) |
| `CLM-PR01-01B` | `type` | `io.amichne.konditional.core.result.KonditionalBoundaryFailure` | [/overview/start-here.md](/overview/start-here.md) | [/overview/start-here.md](/overview/start-here.md) |
| `CLM-PR01-01B` | `type` | `io.amichne.konditional.core.result.ParseError` | [/overview/start-here.md](/overview/start-here.md) | [/overview/start-here.md](/overview/start-here.md) |
| `CLM-PR01-02A` | `method` | `io.amichne.konditional.core.evaluation.Bucketing#fun isInRampUp( rampUp: RampUp, bucket: Int, ): Boolean` | [/overview/product-value-fit.md](/overview/product-value-fit.md) | [/overview/product-value-fit.md](/overview/product-value-fit.md) |
| `CLM-PR01-02A` | `method` | `io.amichne.konditional.core.evaluation.Bucketing#fun stableBucket( salt: String, flagKey: String, stableId: HexId, ): Int` | [/overview/product-value-fit.md](/overview/product-value-fit.md) | [/overview/product-value-fit.md](/overview/product-value-fit.md) |
| `CLM-PR01-02A` | `type` | `io.amichne.konditional.core.evaluation.Bucketing` | [/overview/product-value-fit.md](/overview/product-value-fit.md) | [/overview/product-value-fit.md](/overview/product-value-fit.md) |
| `CLM-PR01-03A` | `type` | `io.amichne.konditional.core.FlagDefinition` | [/overview/why-typed-flags.md](/overview/why-typed-flags.md) | [/overview/why-typed-flags.md](/overview/why-typed-flags.md) |
| `CLM-PR01-03A` | `type` | `io.amichne.konditional.core.Namespace` | [/overview/why-typed-flags.md](/overview/why-typed-flags.md) | [/overview/why-typed-flags.md](/overview/why-typed-flags.md) |
| `CLM-PR01-03A` | `type` | `io.amichne.konditional.core.features.Feature` | [/overview/why-typed-flags.md](/overview/why-typed-flags.md) | [/overview/why-typed-flags.md](/overview/why-typed-flags.md) |
| `CLM-PR01-03B` | `type` | `io.amichne.konditional.core.result.KonditionalBoundaryFailure` | [/overview/why-typed-flags.md](/overview/why-typed-flags.md) | [/overview/why-typed-flags.md](/overview/why-typed-flags.md) |
| `CLM-PR01-03B` | `type` | `io.amichne.konditional.core.result.ParseError` | [/overview/why-typed-flags.md](/overview/why-typed-flags.md) | [/overview/why-typed-flags.md](/overview/why-typed-flags.md) |
| `CLM-PR01-04A` | `method` | `io.amichne.konditional.api.RampUpBucketing#fun bucket( stableId: StableId, featureKey: String, salt: String, ): Int` | [/overview/first-success-map.md](/overview/first-success-map.md) | [/overview/first-success-map.md](/overview/first-success-map.md) |
| `CLM-PR01-04A` | `type` | `io.amichne.konditional.api.RampUpBucketing` | [/overview/first-success-map.md](/overview/first-success-map.md) | [/overview/first-success-map.md](/overview/first-success-map.md) |
| `CLM-PR01-06A` | `type` | `io.amichne.konditional.core.Namespace` | [/quickstart/index.md](/quickstart/index.md) | [/quickstart/index.md](/quickstart/index.md) |
| `CLM-PR01-07A` | `type` | `io.amichne.konditional.core.Namespace` | [/quickstart/install.md](/quickstart/install.md) | [/quickstart/install.md](/quickstart/install.md) |
| `CLM-PR01-08A` | `type` | `io.amichne.konditional.core.FlagDefinition` | [/quickstart/define-first-flag.md](/quickstart/define-first-flag.md) | [/quickstart/define-first-flag.md](/quickstart/define-first-flag.md) |
| `CLM-PR01-08A` | `type` | `io.amichne.konditional.core.Namespace` | [/quickstart/define-first-flag.md](/quickstart/define-first-flag.md) | [/quickstart/define-first-flag.md](/quickstart/define-first-flag.md) |
| `CLM-PR01-08A` | `type` | `io.amichne.konditional.core.features.Feature` | [/quickstart/define-first-flag.md](/quickstart/define-first-flag.md) | [/quickstart/define-first-flag.md](/quickstart/define-first-flag.md) |
| `CLM-PR01-08B` | `type` | `io.amichne.konditional.core.Namespace` | [/quickstart/define-first-flag.md](/quickstart/define-first-flag.md) | [/quickstart/define-first-flag.md](/quickstart/define-first-flag.md) |
| `CLM-PR01-09A` | `type` | `io.amichne.konditional.core.FlagDefinition` | [/quickstart/evaluate-in-app-code.md](/quickstart/evaluate-in-app-code.md) | [/quickstart/evaluate-in-app-code.md](/quickstart/evaluate-in-app-code.md) |
| `CLM-PR01-09A` | `type` | `io.amichne.konditional.core.evaluation.Bucketing` | [/quickstart/evaluate-in-app-code.md](/quickstart/evaluate-in-app-code.md) | [/quickstart/evaluate-in-app-code.md](/quickstart/evaluate-in-app-code.md) |
| `CLM-PR01-10A` | `method` | `io.amichne.konditional.api.RampUpBucketing#fun bucket( stableId: StableId, featureKey: String, salt: String, ): Int` | [/quickstart/add-deterministic-ramp-up.md](/quickstart/add-deterministic-ramp-up.md) | [/quickstart/add-deterministic-ramp-up.md](/quickstart/add-deterministic-ramp-up.md) |
| `CLM-PR01-10A` | `method` | `io.amichne.konditional.core.evaluation.Bucketing#fun isInRampUp( rampUp: RampUp, bucket: Int, ): Boolean` | [/quickstart/add-deterministic-ramp-up.md](/quickstart/add-deterministic-ramp-up.md) | [/quickstart/add-deterministic-ramp-up.md](/quickstart/add-deterministic-ramp-up.md) |
| `CLM-PR01-10A` | `method` | `io.amichne.konditional.core.evaluation.Bucketing#fun stableBucket( salt: String, flagKey: String, stableId: HexId, ): Int` | [/quickstart/add-deterministic-ramp-up.md](/quickstart/add-deterministic-ramp-up.md) | [/quickstart/add-deterministic-ramp-up.md](/quickstart/add-deterministic-ramp-up.md) |
| `CLM-PR01-10A` | `type` | `io.amichne.konditional.core.id.StableId` | [/quickstart/add-deterministic-ramp-up.md](/quickstart/add-deterministic-ramp-up.md) | [/quickstart/add-deterministic-ramp-up.md](/quickstart/add-deterministic-ramp-up.md) |
| `CLM-PR01-11B` | `type` | `io.amichne.konditional.core.result.KonditionalBoundaryFailure` | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) |
| `CLM-PR01-11B` | `type` | `io.amichne.konditional.core.result.ParseError` | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) |
| `CLM-PR01-12A` | `type` | `io.amichne.konditional.core.evaluation.Bucketing` | [/quickstart/verify-end-to-end.md](/quickstart/verify-end-to-end.md) | [/quickstart/verify-end-to-end.md](/quickstart/verify-end-to-end.md) |
| `CLM-PR01-12A` | `type` | `io.amichne.konditional.core.result.ParseError` | [/quickstart/verify-end-to-end.md](/quickstart/verify-end-to-end.md) | [/quickstart/verify-end-to-end.md](/quickstart/verify-end-to-end.md) |

## konditional-runtime

| Claim ID | Kind | Signature | Source page | Concept/guide link |
| --- | --- | --- | --- | --- |
| `CLM-PR01-01B` | `type` | `io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader` | [/overview/start-here.md](/overview/start-here.md) | [/overview/start-here.md](/overview/start-here.md) |
| `CLM-PR01-02B` | `method` | `io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun load(config: ConfigurationView)` | [/overview/product-value-fit.md](/overview/product-value-fit.md) | [/overview/product-value-fit.md](/overview/product-value-fit.md) |
| `CLM-PR01-02B` | `method` | `io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun rollback(steps: Int): Boolean` | [/overview/product-value-fit.md](/overview/product-value-fit.md) | [/overview/product-value-fit.md](/overview/product-value-fit.md) |
| `CLM-PR01-02B` | `type` | `io.amichne.konditional.core.registry.InMemoryNamespaceRegistry` | [/overview/product-value-fit.md](/overview/product-value-fit.md) | [/overview/product-value-fit.md](/overview/product-value-fit.md) |
| `CLM-PR01-04A` | `type` | `io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader` | [/overview/first-success-map.md](/overview/first-success-map.md) | [/overview/first-success-map.md](/overview/first-success-map.md) |
| `CLM-PR01-05A` | `method` | `io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun disableAll()` | [/overview/adoption-roadmap.md](/overview/adoption-roadmap.md) | [/overview/adoption-roadmap.md](/overview/adoption-roadmap.md) |
| `CLM-PR01-05A` | `method` | `io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun rollback(steps: Int): Boolean` | [/overview/adoption-roadmap.md](/overview/adoption-roadmap.md) | [/overview/adoption-roadmap.md](/overview/adoption-roadmap.md) |
| `CLM-PR01-05A` | `type` | `io.amichne.konditional.core.registry.InMemoryNamespaceRegistry` | [/overview/adoption-roadmap.md](/overview/adoption-roadmap.md) | [/overview/adoption-roadmap.md](/overview/adoption-roadmap.md) |
| `CLM-PR01-06A` | `type` | `io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader` | [/quickstart/index.md](/quickstart/index.md) | [/quickstart/index.md](/quickstart/index.md) |
| `CLM-PR01-07A` | `type` | `io.amichne.konditional.core.registry.InMemoryNamespaceRegistry` | [/quickstart/install.md](/quickstart/install.md) | [/quickstart/install.md](/quickstart/install.md) |
| `CLM-PR01-11A` | `method` | `io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader#fun load( json: String, options: SnapshotLoadOptions = SnapshotLoadOptions.strict(), ): Result<Configuration>` | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) |
| `CLM-PR01-11A` | `type` | `io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader` | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) |
| `CLM-PR01-12A` | `method` | `io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun load(config: ConfigurationView)` | [/quickstart/verify-end-to-end.md](/quickstart/verify-end-to-end.md) | [/quickstart/verify-end-to-end.md](/quickstart/verify-end-to-end.md) |
| `CLM-PR01-12A` | `type` | `io.amichne.konditional.core.registry.InMemoryNamespaceRegistry` | [/quickstart/verify-end-to-end.md](/quickstart/verify-end-to-end.md) | [/quickstart/verify-end-to-end.md](/quickstart/verify-end-to-end.md) |

## konditional-serialization

| Claim ID | Kind | Signature | Source page | Concept/guide link |
| --- | --- | --- | --- | --- |
| `CLM-PR01-08B` | `type` | `io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec` | [/quickstart/define-first-flag.md](/quickstart/define-first-flag.md) | [/quickstart/define-first-flag.md](/quickstart/define-first-flag.md) |
| `CLM-PR01-11A` | `type` | `io.amichne.konditional.serialization.options.SnapshotLoadOptions` | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) |
| `CLM-PR01-11A` | `type` | `io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec` | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) | [/quickstart/load-first-snapshot-safely.md](/quickstart/load-first-snapshot-safely.md) |

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-01A | Konditional models feature declarations through namespace-owned typed feature definitions. |
| CLM-PR01-01B | Runtime configuration ingestion is exposed through a snapshot loader that returns Result and supports typed parse failures. |
| CLM-PR01-02A | Deterministic ramp-up behavior is backed by stable bucketing functions. |
| CLM-PR01-02B | Namespace-scoped runtime operations are implemented by an in-memory namespace registry. |
| CLM-PR01-03A | Feature declarations are modeled as typed entities under Namespace and Feature abstractions. |
| CLM-PR01-03B | Parse boundary failures are represented with explicit ParseError and KonditionalBoundaryFailure types. |
| CLM-PR01-04A | The first-success routes correspond to concrete runtime APIs for ramp-up and snapshot loading. |
| CLM-PR01-05A | Adoption phases rely on load, disable, and rollback runtime operations. |

## Next Steps

- [Snapshot Format](/reference/snapshot-format) - Inspect configuration JSON shape and field semantics.
- [Module Dependency Map](/reference/module-dependency-map) - Choose modules by capability boundary.
