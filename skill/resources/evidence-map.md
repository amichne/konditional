# Konditional Skill Evidence Map

This map links claims in `/Users/amichne/code/konditional/skill/SKILL.md` to repository signatures, source, and tests.

## Claim Links

| Claim ID | Claim | Signature / Source | Test Evidence |
|---|---|---|---|
| `CLM-NS-001` | Namespace-first API defines flags directly on `object : Namespace("...")` via delegates. | `signatures/konditional-core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt.sig` and `konditional-core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt` | `konditional-core/src/test/kotlin/io/amichne/konditional/core/NamespaceFeatureDefinitionTest.kt` |
| `CLM-CTX-001` | Context uses mix-ins (`LocaleContext`, `PlatformContext`, `VersionContext`, `StableIdContext`) and supports typed subtype extension. | `signatures/konditional-core/src/main/kotlin/io/amichne/konditional/context/Context.kt.sig` and `konditional-core/src/main/kotlin/io/amichne/konditional/context/Context.kt` | `konditional-core/src/test/kotlin/io/amichne/konditional/context/ContextPolymorphismTest.kt` |
| `CLM-DSL-001` | Rule DSL supports `rule`, `enable`/`disable`, `anyOf`, `axis`, `extension`, `versions`, `rampUp`, `allowlist`, and `ruleSet` composition. | `signatures/konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/FlagScope.kt.sig`, `signatures/konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/DslSugar.kt.sig`, `signatures/konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/rules/RuleScope.kt.sig` | `konditional-core/src/test/kotlin/io/amichne/konditional/core/AnyOfTargetingTest.kt`, `konditional-core/src/test/kotlin/io/amichne/konditional/core/RuleSetTest.kt`, `konditional-core/src/test/kotlin/io/amichne/konditional/core/AllowlistRolloutTest.kt` |
| `CLM-RT-001` | Runtime mutation/lifecycle APIs (`load`, `rollback`, history views) are in runtime module extensions. | `signatures/konditional-runtime/src/main/kotlin/io/amichne/konditional/runtime/NamespaceOperations.kt.sig` and `konditional-runtime/src/main/kotlin/io/amichne/konditional/runtime/NamespaceOperations.kt` | `konditional-runtime/src/test/kotlin/io/amichne/konditional/runtime/NamespaceLinearizabilityTest.kt`, `konditional-runtime/src/test/kotlin/io/amichne/konditional/ops/KillSwitchTest.kt` |
| `CLM-BND-001` | Snapshot decode is schema-aware and boundary failures are typed `ParseError` surfaced through `Result` (`parseErrorOrNull`). | `signatures/konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/snapshot/ConfigurationSnapshotCodec.kt.sig`, `signatures/konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt.sig`, `signatures/konditional-core/src/main/kotlin/io/amichne/konditional/core/result/KonditionalBoundaryFailure.kt.sig` | `konditional-runtime/src/test/kotlin/io/amichne/konditional/serialization/NamespaceConfigurationSnapshotCodecTest.kt`, `konditional-core/src/test/kotlin/io/amichne/konditional/core/BoundaryFailureResultTest.kt` |
| `CLM-SHD-001` | Shadow evaluation returns baseline value and reports mismatch as observability side-channel. | `signatures/konditional-observability/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt.sig` and `konditional-observability/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt` | `konditional-observability/src/test/kotlin/io/amichne/konditional/ops/ShadowEvaluationTest.kt` |

## Canonical recipe source

- `konditional-observability/src/docsSamples/kotlin/io/amichne/konditional/docsamples/RecipesSamples.kt`
