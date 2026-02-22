# Claims registry

This page is generated from `docs/claim-trace/claims-registry.json` and is the human-readable index for claim IDs used across documentation.

| Claim ID | Claim text | Evidence status | Risk category | Source pages |
|---|---|---|---|---|
| [CLM-PR01-01A](#clm-pr01-01a) | Konditional models feature declarations through namespace-owned typed feature definitions. | linked | operational | `docusaurus/docs/overview/start-here.md#claim-clm-pr01-01a` |
| [CLM-PR01-01B](#clm-pr01-01b) | Runtime configuration ingestion is exposed through a snapshot loader that returns Result and supports typed parse failures. | linked | operational | `docusaurus/docs/overview/start-here.md#claim-clm-pr01-01b` |
| [CLM-PR01-02A](#clm-pr01-02a) | Deterministic ramp-up behavior is backed by stable bucketing functions. | linked | operational | `docusaurus/docs/overview/product-value-fit.md#claim-clm-pr01-02a` |
| [CLM-PR01-02B](#clm-pr01-02b) | Namespace-scoped runtime operations are implemented by an in-memory namespace registry. | linked | operational | `docusaurus/docs/overview/product-value-fit.md#claim-clm-pr01-02b` |
| [CLM-PR01-03A](#clm-pr01-03a) | Feature declarations are modeled as typed entities under Namespace and Feature abstractions. | linked | operational | `docusaurus/docs/overview/why-typed-flags.md#claim-clm-pr01-03a` |
| [CLM-PR01-03B](#clm-pr01-03b) | Parse boundary failures are represented with explicit ParseError and KonditionalBoundaryFailure types. | linked | operational | `docusaurus/docs/overview/why-typed-flags.md#claim-clm-pr01-03b` |
| [CLM-PR01-04A](#clm-pr01-04a) | The first-success routes correspond to concrete runtime APIs for ramp-up and snapshot loading. | linked | operational | `docusaurus/docs/overview/first-success-map.md#claim-clm-pr01-04a` |
| [CLM-PR01-05A](#clm-pr01-05a) | Adoption phases rely on load, disable, and rollback runtime operations. | linked | operational | `docusaurus/docs/overview/adoption-roadmap.md#claim-clm-pr01-05a` |
| [CLM-PR01-06A](#clm-pr01-06a) | The quickstart sequence is grounded in namespace declaration and snapshot loading APIs. | linked | operational | `docusaurus/docs/quickstart/index.md#claim-clm-pr01-06a` |
| [CLM-PR01-07A](#clm-pr01-07a) | Installation targets the core namespace model and runtime in-memory registry implementation. | linked | operational | `docusaurus/docs/quickstart/install.md#claim-clm-pr01-07a` |
| [CLM-PR01-08A](#clm-pr01-08a) | Feature declarations are represented by Namespace, Feature, and FlagDefinition types. | linked | operational | `docusaurus/docs/quickstart/define-first-flag.md#claim-clm-pr01-08a` |
| [CLM-PR01-08B](#clm-pr01-08b) | Namespaces provide a compiled schema used by boundary codecs and loaders. | linked | operational | `docusaurus/docs/quickstart/define-first-flag.md#claim-clm-pr01-08b` |
| [CLM-PR01-09A](#clm-pr01-09a) | Feature evaluation follows core flag-definition evaluation semantics and deterministic bucketing behavior. | linked | operational | `docusaurus/docs/quickstart/evaluate-in-app-code.md#claim-clm-pr01-09a` |
| [CLM-PR01-10A](#clm-pr01-10a) | Ramp-up assignment is deterministic and exposed through stable bucket APIs. | linked | operational | `docusaurus/docs/quickstart/add-deterministic-ramp-up.md#claim-clm-pr01-10a` |
| [CLM-PR01-11A](#clm-pr01-11a) | Snapshot loading API exposes Result-based ingestion with options and codec-backed decode. | linked | operational | `docusaurus/docs/quickstart/load-first-snapshot-safely.md#claim-clm-pr01-11a` |
| [CLM-PR01-11B](#clm-pr01-11b) | Boundary failures are modeled as ParseError values wrapped by KonditionalBoundaryFailure. | linked | operational | `docusaurus/docs/quickstart/load-first-snapshot-safely.md#claim-clm-pr01-11b` |
| [CLM-PR01-12A](#clm-pr01-12a) | End-to-end verification relies on deterministic bucketing, boundary parse types, and namespace runtime operations. | linked | operational | `docusaurus/docs/quickstart/verify-end-to-end.md#claim-clm-pr01-12a` |

## CLM-PR01-01A

Konditional models feature declarations through namespace-owned typed feature definitions.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `namespace-isolation`, `type-safety`
- **Source pages:** `docusaurus/docs/overview/start-here.md#claim-clm-pr01-01a`
- **Signature links:** `type:io.amichne.konditional.core.Namespace`, `type:io.amichne.konditional.core.features.Feature`
- **Test links:** -
- **Related claims:** -

## CLM-PR01-01B

Runtime configuration ingestion is exposed through a snapshot loader that returns Result and supports typed parse failures.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `parse-boundary`, `snapshot-loading`, `type-safety`
- **Source pages:** `docusaurus/docs/overview/start-here.md#claim-clm-pr01-01b`
- **Signature links:** `type:io.amichne.konditional.core.result.KonditionalBoundaryFailure`, `type:io.amichne.konditional.core.result.ParseError`, `type:io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader`
- **Test links:** `unit:konditional-core:konditional-core/src/test/kotlin/io/amichne/konditional/core/BoundaryFailureResultTest.kt:BoundaryFailureResultTest`
- **Related claims:** -

## CLM-PR01-02A

Deterministic ramp-up behavior is backed by stable bucketing functions.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `determinism`, `ramp-up`
- **Source pages:** `docusaurus/docs/overview/product-value-fit.md#claim-clm-pr01-02a`
- **Signature links:** `method:io.amichne.konditional.core.evaluation.Bucketing#fun isInRampUp( rampUp: RampUp, bucket: Int, ): Boolean`, `method:io.amichne.konditional.core.evaluation.Bucketing#fun stableBucket( salt: String, flagKey: String, stableId: HexId, ): Int`, `type:io.amichne.konditional.core.evaluation.Bucketing`
- **Test links:** `unit:konditional-core:konditional-core/src/test/kotlin/io/amichne/konditional/core/MissingStableIdBucketingTest.kt:MissingStableIdBucketingTest`
- **Related claims:** -

## CLM-PR01-02B

Namespace-scoped runtime operations are implemented by an in-memory namespace registry.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `namespace-isolation`
- **Source pages:** `docusaurus/docs/overview/product-value-fit.md#claim-clm-pr01-02b`
- **Signature links:** `method:io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun load(config: ConfigurationView)`, `method:io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun rollback(steps: Int): Boolean`, `type:io.amichne.konditional.core.registry.InMemoryNamespaceRegistry`
- **Test links:** `unit:konditional-runtime:konditional-runtime/src/test/kotlin/io/amichne/konditional/runtime/NamespaceLinearizabilityTest.kt:NamespaceLinearizabilityTest`
- **Related claims:** -

## CLM-PR01-03A

Feature declarations are modeled as typed entities under Namespace and Feature abstractions.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `namespace-isolation`, `type-safety`
- **Source pages:** `docusaurus/docs/overview/why-typed-flags.md#claim-clm-pr01-03a`
- **Signature links:** `type:io.amichne.konditional.core.FlagDefinition`, `type:io.amichne.konditional.core.Namespace`, `type:io.amichne.konditional.core.features.Feature`
- **Test links:** `unit:konditional-core:konditional-core/src/test/kotlin/io/amichne/konditional/core/NamespaceFeatureDefinitionTest.kt:NamespaceFeatureDefinitionTest`
- **Related claims:** -

## CLM-PR01-03B

Parse boundary failures are represented with explicit ParseError and KonditionalBoundaryFailure types.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `parse-boundary`, `type-safety`
- **Source pages:** `docusaurus/docs/overview/why-typed-flags.md#claim-clm-pr01-03b`
- **Signature links:** `type:io.amichne.konditional.core.result.KonditionalBoundaryFailure`, `type:io.amichne.konditional.core.result.ParseError`
- **Test links:** `unit:konditional-core:konditional-core/src/test/kotlin/io/amichne/konditional/core/BoundaryFailureResultTest.kt:BoundaryFailureResultTest`
- **Related claims:** -

## CLM-PR01-04A

The first-success routes correspond to concrete runtime APIs for ramp-up and snapshot loading.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `ramp-up`, `snapshot-loading`
- **Source pages:** `docusaurus/docs/overview/first-success-map.md#claim-clm-pr01-04a`
- **Signature links:** `method:io.amichne.konditional.api.RampUpBucketing#fun bucket( stableId: StableId, featureKey: String, salt: String, ): Int`, `type:io.amichne.konditional.api.RampUpBucketing`, `type:io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader`
- **Test links:** -
- **Related claims:** -

## CLM-PR01-05A

Adoption phases rely on load, disable, and rollback runtime operations.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `rollback`
- **Source pages:** `docusaurus/docs/overview/adoption-roadmap.md#claim-clm-pr01-05a`
- **Signature links:** `method:io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun disableAll()`, `method:io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun rollback(steps: Int): Boolean`, `type:io.amichne.konditional.core.registry.InMemoryNamespaceRegistry`
- **Test links:** `unit:konditional-runtime:konditional-runtime/src/test/kotlin/io/amichne/konditional/runtime/NamespaceLinearizabilityTest.kt:NamespaceLinearizabilityTest`
- **Related claims:** -

## CLM-PR01-06A

The quickstart sequence is grounded in namespace declaration and snapshot loading APIs.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `namespace-isolation`, `snapshot-loading`
- **Source pages:** `docusaurus/docs/quickstart/index.md#claim-clm-pr01-06a`
- **Signature links:** `type:io.amichne.konditional.core.Namespace`, `type:io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader`
- **Test links:** -
- **Related claims:** -

## CLM-PR01-07A

Installation targets the core namespace model and runtime in-memory registry implementation.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `namespace-isolation`
- **Source pages:** `docusaurus/docs/quickstart/install.md#claim-clm-pr01-07a`
- **Signature links:** `type:io.amichne.konditional.core.Namespace`, `type:io.amichne.konditional.core.registry.InMemoryNamespaceRegistry`
- **Test links:** -
- **Related claims:** -

## CLM-PR01-08A

Feature declarations are represented by Namespace, Feature, and FlagDefinition types.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `namespace-isolation`, `type-safety`
- **Source pages:** `docusaurus/docs/quickstart/define-first-flag.md#claim-clm-pr01-08a`
- **Signature links:** `type:io.amichne.konditional.core.FlagDefinition`, `type:io.amichne.konditional.core.Namespace`, `type:io.amichne.konditional.core.features.Feature`
- **Test links:** -
- **Related claims:** -

## CLM-PR01-08B

Namespaces provide a compiled schema used by boundary codecs and loaders.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `namespace-isolation`, `parse-boundary`
- **Source pages:** `docusaurus/docs/quickstart/define-first-flag.md#claim-clm-pr01-08b`
- **Signature links:** `type:io.amichne.konditional.core.Namespace`, `type:io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec`
- **Test links:** `unit:konditional-runtime:konditional-runtime/src/test/kotlin/io/amichne/konditional/serialization/NamespaceConfigurationSnapshotCodecTest.kt:NamespaceConfigurationSnapshotCodecTest`
- **Related claims:** -

## CLM-PR01-09A

Feature evaluation follows core flag-definition evaluation semantics and deterministic bucketing behavior.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `determinism`, `ramp-up`
- **Source pages:** `docusaurus/docs/quickstart/evaluate-in-app-code.md#claim-clm-pr01-09a`
- **Signature links:** `type:io.amichne.konditional.core.FlagDefinition`, `type:io.amichne.konditional.core.evaluation.Bucketing`
- **Test links:** -
- **Related claims:** -

## CLM-PR01-10A

Ramp-up assignment is deterministic and exposed through stable bucket APIs.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `determinism`, `ramp-up`
- **Source pages:** `docusaurus/docs/quickstart/add-deterministic-ramp-up.md#claim-clm-pr01-10a`
- **Signature links:** `method:io.amichne.konditional.api.RampUpBucketing#fun bucket( stableId: StableId, featureKey: String, salt: String, ): Int`, `method:io.amichne.konditional.core.evaluation.Bucketing#fun isInRampUp( rampUp: RampUp, bucket: Int, ): Boolean`, `method:io.amichne.konditional.core.evaluation.Bucketing#fun stableBucket( salt: String, flagKey: String, stableId: HexId, ): Int`, `type:io.amichne.konditional.core.id.StableId`
- **Test links:** `unit:konditional-core:konditional-core/src/test/kotlin/io/amichne/konditional/core/MissingStableIdBucketingTest.kt:MissingStableIdBucketingTest`
- **Related claims:** -

## CLM-PR01-11A

Snapshot loading API exposes Result-based ingestion with options and codec-backed decode.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `snapshot-loading`
- **Source pages:** `docusaurus/docs/quickstart/load-first-snapshot-safely.md#claim-clm-pr01-11a`
- **Signature links:** `method:io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader#override fun load( json: String, options: SnapshotLoadOptions, ): Result<MaterializedConfiguration>`, `type:io.amichne.konditional.serialization.options.SnapshotLoadOptions`, `type:io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec`, `type:io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader`
- **Test links:** -
- **Related claims:** -

## CLM-PR01-11B

Boundary failures are modeled as ParseError values wrapped by KonditionalBoundaryFailure.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `parse-boundary`
- **Source pages:** `docusaurus/docs/quickstart/load-first-snapshot-safely.md#claim-clm-pr01-11b`
- **Signature links:** `type:io.amichne.konditional.core.result.KonditionalBoundaryFailure`, `type:io.amichne.konditional.core.result.ParseError`
- **Test links:** `unit:konditional-core:konditional-core/src/test/kotlin/io/amichne/konditional/core/BoundaryFailureResultTest.kt:BoundaryFailureResultTest`
- **Related claims:** -

## CLM-PR01-12A

End-to-end verification relies on deterministic bucketing, boundary parse types, and namespace runtime operations.

- **Evidence status:** `linked`
- **Risk category:** `operational`
- **Topics:** `determinism`, `namespace-isolation`, `parse-boundary`, `ramp-up`, `type-safety`
- **Source pages:** `docusaurus/docs/quickstart/verify-end-to-end.md#claim-clm-pr01-12a`
- **Signature links:** `method:io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun load(config: ConfigurationView)`, `type:io.amichne.konditional.core.evaluation.Bucketing`, `type:io.amichne.konditional.core.registry.InMemoryNamespaceRegistry`, `type:io.amichne.konditional.core.result.ParseError`
- **Test links:** `unit:konditional-core:konditional-core/src/test/kotlin/io/amichne/konditional/core/BoundaryFailureResultTest.kt:BoundaryFailureResultTest`, `unit:konditional-core:konditional-core/src/test/kotlin/io/amichne/konditional/core/MissingStableIdBucketingTest.kt:MissingStableIdBucketingTest`, `unit:konditional-runtime:konditional-runtime/src/test/kotlin/io/amichne/konditional/runtime/NamespaceLinearizabilityTest.kt:NamespaceLinearizabilityTest`
- **Related claims:** -
