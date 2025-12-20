# Konditional Pre-Publication Evaluation
**Date**: 2025-12-19  
**Codebase Version**: 715ff3449450fdce6b2505fca30b24c208558951

## Executive Summary
Konditional’s core API is coherent and strongly typed, with deterministic evaluation, explicit JSON boundaries, and a rich DSL for flag definition and targeting; these claims are supported by the main runtime and doc set. The largest blockers for a public, enterprise-facing release are the fixed `AppLocale`/`Platform` enums baked into `Context` (limits consumer domain modeling) and several public-API documentation mismatches that will cause misuse and operational friction. No correctness failures in evaluation or serialization were found in code paths exercised by tests, but a few public API edges remain under-documented or overly exposed.

Evidence: `src/main/kotlin/io/amichne/konditional/core/Namespace.kt:69`, `src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt:27`, `src/main/kotlin/io/amichne/konditional/core/evaluation/Bucketing.kt:9`, `docusaurus/docs/evaluation.md:5`, `docusaurus/docs/remote-config.md:3`, `src/test/kotlin/io/amichne/konditional/adversarial/ConcurrencyAttacksTest.kt:27`.

## API Surface Review

### Public API Inventory
| API | Kind | Purpose | Evidence |
|---|---|---|---|
| `Namespace` | class | Namespace-scoped registry + DSL entry point | `src/main/kotlin/io/amichne/konditional/core/Namespace.kt:69` |
| `Namespace.TestNamespaceFacade` | nested class | Test-only namespace factory with unique identifier seed | `src/main/kotlin/io/amichne/konditional/core/Namespace.kt:106` |
| `NamespaceRegistry` | interface | Registry contract (load/config/rollback/hooks) | `src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:72` |
| `FlagDefinition` | data class | Compiled flag definition + evaluation logic | `src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt:27` |
| `Feature` | sealed interface | Typed feature key bound to namespace | `src/main/kotlin/io/amichne/konditional/core/features/Feature.kt:36` |
| `BooleanFeature` | sealed interface | Boolean feature specialization | `src/main/kotlin/io/amichne/konditional/core/features/BooleanFeature.kt:6` |
| `StringFeature` | sealed interface | String feature specialization | `src/main/kotlin/io/amichne/konditional/core/features/StringFeature.kt:6` |
| `IntFeature` | sealed interface | Int feature specialization | `src/main/kotlin/io/amichne/konditional/core/features/IntFeature.kt:6` |
| `DoubleFeature` | sealed interface | Double feature specialization | `src/main/kotlin/io/amichne/konditional/core/features/DoubleFeature.kt:6` |
| `EnumFeature` | sealed interface | Enum feature specialization | `src/main/kotlin/io/amichne/konditional/core/features/EnumFeature.kt:14` |
| `KotlinClassFeature` | sealed interface | Custom structured feature specialization | `src/main/kotlin/io/amichne/konditional/core/features/KotlinClassFeature.kt:38` |
| `Identifiable` | interface | Provides `FeatureId` for features | `src/main/kotlin/io/amichne/konditional/core/features/Identifiable.kt:6` |
| `KonditionalDsl` | annotation | DSL marker for flag/rule scopes | `src/main/kotlin/io/amichne/konditional/core/dsl/KonditionalDsl.kt:5` |
| `FlagScope` | interface | Flag definition DSL | `src/main/kotlin/io/amichne/konditional/core/dsl/FlagScope.kt:30` |
| `RuleScope` | interface | Rule targeting DSL | `src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:35` |
| `VersionRangeScope` | interface | Version range DSL | `src/main/kotlin/io/amichne/konditional/core/dsl/VersionRangeScope.kt:21` |
| `AxisValuesScope` | interface | Axis values DSL | `src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt:39` |
| `AxisValuesScope.axis` | inline fun | Type-based axis setter (implicit axis registration) | `src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt:79` |
| `unaryPlus` | context operator fun | DSL unary-plus for axis values | `src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt:83` |
| `Context` | interface | Evaluation context (locale/platform/version/stableId/axes) | `src/main/kotlin/io/amichne/konditional/context/Context.kt:33` |
| `Context.Core` | data class | Default Context implementation | `src/main/kotlin/io/amichne/konditional/context/Context.kt:50` |
| `Context.invoke` | operator fun | Context factory | `src/main/kotlin/io/amichne/konditional/context/Context.kt:71` |
| `AppLocale` | enum | Locale dimension enum | `src/main/kotlin/io/amichne/konditional/context/AppLocale.kt:8` |
| `Platform` | enum | Platform dimension enum | `src/main/kotlin/io/amichne/konditional/context/Platform.kt:8` |
| `Version` | data class | Semantic version type | `src/main/kotlin/io/amichne/konditional/context/Version.kt:9` |
| `RampUp` | value class | Rollout percentage type | `src/main/kotlin/io/amichne/konditional/context/RampUp.kt:10` |
| `Axis` | abstract class | Axis descriptor | `src/main/kotlin/io/amichne/konditional/context/axis/Axis.kt:44` |
| `AxisValue` | interface | Axis value contract | `src/main/kotlin/io/amichne/konditional/context/axis/AxisValue.kt:31` |
| `AxisValues` | class | Immutable axis value container | `src/main/kotlin/io/amichne/konditional/context/axis/AxisValues.kt:34` |
| `StableId` | sealed interface | Stable identifier for bucketing | `src/main/kotlin/io/amichne/konditional/core/id/StableId.kt:13` |
| `HexId` | value class | Hex identifier type | `src/main/kotlin/io/amichne/konditional/core/id/HexId.kt:11` |
| `FeatureId` | value class | Stable feature identifier | `src/main/kotlin/io/amichne/konditional/values/FeatureId.kt:6` |
| `Rule` | data class | Rule model (targeting + rollout) | `src/main/kotlin/io/amichne/konditional/rules/Rule.kt:68` |
| `Evaluable` | fun interface | Custom rule predicate | `src/main/kotlin/io/amichne/konditional/rules/evaluable/Evaluable.kt:22` |
| `Specifier` | interface | Rule specificity contract | `src/main/kotlin/io/amichne/konditional/rules/evaluable/Specifier.kt:3` |
| `Placeholder` | object | Default Evaluable implementation | `src/main/kotlin/io/amichne/konditional/rules/evaluable/Placeholder.kt:5` |
| `VersionRange` | sealed class | Version range model | `src/main/kotlin/io/amichne/konditional/rules/versions/VersionRange.kt:6` |
| `Unbounded` | class | VersionRange with no bounds | `src/main/kotlin/io/amichne/konditional/rules/versions/Unbounded.kt:5` |
| `LeftBound` | data class | VersionRange with min bound | `src/main/kotlin/io/amichne/konditional/rules/versions/LeftBound.kt:5` |
| `RightBound` | data class | VersionRange with max bound | `src/main/kotlin/io/amichne/konditional/rules/versions/RightBound.kt:5` |
| `FullyBound` | data class | VersionRange with min/max bounds | `src/main/kotlin/io/amichne/konditional/rules/versions/FullyBound.kt:5` |
| `Configuration` | data class | Runtime configuration snapshot | `src/main/kotlin/io/amichne/konditional/core/instance/Configuration.kt:7` |
| `ConfigurationMetadata` | data class | Snapshot metadata | `src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationMetadata.kt:18` |
| `ConfigurationPatch` | data class | Incremental config updates | `src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationPatch.kt:21` |
| `ConfigurationDiff` | data class | Diff between configurations | `src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationDiff.kt:15` |
| `ConfigValue` | sealed interface | Typed value wrapper for diffs | `src/main/kotlin/io/amichne/konditional/core/instance/ConfigValue.kt:7` |
| `ValueType` | enum | Value-type marker (serialization) | `src/main/kotlin/io/amichne/konditional/core/ValueType.kt:6` |
| `KotlinEncodeable` | interface | Custom structured value contract | `src/main/kotlin/io/amichne/konditional/core/types/KotlinEncodeable.kt:61` |
| `KotlinEncodeable.toJsonValue` | fun | Encode custom type to JsonObject | `src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:31` |
| `JsonObject.parseAs` | inline fun | Decode custom type from JsonObject | `src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:92` |
| `JsonValue.toPrimitiveValue` | fun | Convert JsonValue to primitives | `src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:162` |
| `ParseResult` | sealed interface | Parse success/failure result | `src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:15` |
| `ParseResult.getOrThrow` | fun | Throw on ParseResult failure | `src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:33` |
| `ParseError` | sealed interface | Structured parse errors | `src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt:11` |
| `ParseException` | class | Exception wrapper for ParseError | `src/main/kotlin/io/amichne/konditional/core/result/ParseException.kt:7` |
| `ParseResult.fold` | inline fun | Fold ParseResult | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:19` |
| `ParseResult.map` | inline fun | Map ParseResult | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:33` |
| `ParseResult.flatMap` | inline fun | FlatMap ParseResult | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:46` |
| `ParseResult.getOrNull` | fun | ParseResult to nullable | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:55` |
| `ParseResult.getOrDefault` | fun | ParseResult with default | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:63` |
| `ParseResult.getOrElse` | inline fun | ParseResult fallback with error | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:71` |
| `ParseResult.isSuccess` | fun | Success predicate | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:79` |
| `ParseResult.isFailure` | fun | Failure predicate | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:84` |
| `ParseResult.onSuccess` | inline fun | Side-effect on success | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:96` |
| `ParseResult.onFailure` | inline fun | Side-effect on failure | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:113` |
| `ParseResult.recover` | inline fun | Recover with fallback value | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:130` |
| `ParseResult.toResult` | fun | Convert to Kotlin Result | `src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:139` |
| `KonditionalLogger` | interface | Logging hook | `src/main/kotlin/io/amichne/konditional/core/ops/KonditionalLogger.kt:3` |
| `MetricsCollector` | interface | Metrics hook | `src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt:3` |
| `RegistryHooks` | data class | Observability hooks container | `src/main/kotlin/io/amichne/konditional/core/ops/RegistryHooks.kt:11` |
| `Metrics` | object | Metrics event models | `src/main/kotlin/io/amichne/konditional/core/ops/Metrics.kt:3` |
| `SnapshotSerializer` | object | Serialize/deserialize Configuration and patches | `src/main/kotlin/io/amichne/konditional/serialization/SnapshotSerializer.kt:45` |
| `NamespaceSnapshotSerializer` | class | Namespace-scoped JSON serializer | `src/main/kotlin/io/amichne/konditional/serialization/NamespaceSnapshotSerializer.kt:64` |
| `SnapshotLoadOptions` | data class | Snapshot load strategy | `src/main/kotlin/io/amichne/konditional/serialization/SnapshotLoadOptions.kt:12` |
| `UnknownFeatureKeyStrategy` | sealed interface | Unknown key handling | `src/main/kotlin/io/amichne/konditional/serialization/SnapshotLoadOptions.kt:24` |
| `SnapshotWarning` | data class | Snapshot warnings | `src/main/kotlin/io/amichne/konditional/serialization/SnapshotLoadOptions.kt:31` |
| `Serializer` | interface | JSON serializer contract | `src/main/kotlin/io/amichne/konditional/serialization/Serializer.kt:54` |
| `Feature.evaluate` | extension fun | Evaluate feature value | `src/main/kotlin/io/amichne/konditional/api/FeatureUtilities.kt:22` |
| `Feature.evaluateWithReason` | extension fun | Evaluate with explainable decision | `src/main/kotlin/io/amichne/konditional/api/FeatureUtilities.kt:27` |
| `EvaluationResult` | data class | Explainable evaluation outcome | `src/main/kotlin/io/amichne/konditional/api/EvaluationResult.kt:17` |
| `BucketInfo` | data class | Ramp-up bucket details | `src/main/kotlin/io/amichne/konditional/api/RolloutBucketing.kt:7` |
| `RampUpBucketing` | object | Deterministic bucketing utilities | `src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt:13` |
| `RolloutBucketing` | object | Deprecated alias | `src/main/kotlin/io/amichne/konditional/api/RolloutBucketing.kt:23` |
| `ShadowOptions` | data class | Shadow evaluation options | `src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:10` |
| `ShadowMismatch` | data class | Shadow mismatch report | `src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:23` |
| `Feature.evaluateWithShadow` | extension fun | Evaluate baseline + shadow | `src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:41` |
| `Feature.evaluateShadow` | extension fun | Shadow-only evaluation | `src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:88` |
| `Context.axis<T>` | inline fun | Axis lookup by type | `src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt:21` |
| `C.axis(axis)` | inline fun | Axis lookup by descriptor | `src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt:30` |
| `axisValues` | inline fun | AxisValues builder | `src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt:48` |

### Consistency Analysis
- Naming is mostly consistent with Kotlin conventions (`Namespace`, `Feature`, `Configuration`, `SnapshotSerializer`), and core APIs are cohesive across packages. Evidence: `src/main/kotlin/io/amichne/konditional/core/Namespace.kt:69`, `src/main/kotlin/io/amichne/konditional/serialization/SnapshotSerializer.kt:45`.
- Several docs refer to non-existent APIs (e.g., `NamespaceRegistry.create()`), creating discoverability drift. Evidence: `src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:35`, `src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:192`.
- Deprecated alias `RolloutBucketing` co-exists with `RampUpBucketing`, which is a known and explicit naming transition but adds surface area to learn. Evidence: `src/main/kotlin/io/amichne/konditional/api/RolloutBucketing.kt:19`.

### Type Safety Assessment
- Compile-time type safety is strong for flag definitions and call sites via `Feature<T, C, M>` and DSL builders (`boolean`, `string`, `enum`, `custom`), preventing key/type mismatch at compile time. Evidence: `src/main/kotlin/io/amichne/konditional/core/features/Feature.kt:36`, `src/main/kotlin/io/amichne/konditional/core/Namespace.kt:120`.
- Runtime JSON is treated as a trust boundary and is validated to `ParseResult` before load; feature lookups are guarded by `FeatureRegistry` and deserialization options. Evidence: `src/main/kotlin/io/amichne/konditional/serialization/SnapshotSerializer.kt:79`, `src/main/kotlin/io/amichne/konditional/serialization/ConversionUtils.kt:112`, `docusaurus/docs/remote-config.md:26`.
- Custom structured values use reflection + schema validation, which is type-safe only at the boundary and can fail at runtime (schema mismatch, missing constructor params). Evidence: `src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:92`, `src/main/kotlin/io/amichne/konditional/serialization/ConversionUtils.kt:167`.

## Developer Experience

### Onboarding Friction
- First-time setup requires understanding `Namespace`, delegated properties, `Context`, and `StableId`, but the minimal example is ~22 LoC including imports and evaluation. Evidence: `docusaurus/docs/getting-started.md:50`.
- JSON configuration requires namespace initialization before deserialization; this invariant is documented but not enforced via compile-time checks. Evidence: `docusaurus/docs/remote-config.md:26`, `src/main/kotlin/io/amichne/konditional/serialization/FeatureRegistry.kt:19`.

### Common Patterns
- Adding a new boolean feature typically costs 4–8 LoC (property + rule block) and uses the same DSL constructs as other value types. Evidence: `docusaurus/docs/getting-started.md:55`, `src/test/kotlin/io/amichne/konditional/serialization/ConsumerConfigurationLifecycleTest.kt:61`.
- Production configuration updates are 6–10 LoC using `SnapshotSerializer.applyPatchJson` plus `Namespace.load`. Evidence: `docusaurus/docs/remote-config.md:74`, `src/main/kotlin/io/amichne/konditional/serialization/SnapshotSerializer.kt:166`.
- Debugging uses `evaluateWithReason` + `EvaluationResult` and `RampUpBucketing.explain` for deterministic bucket inspection. Evidence: `docusaurus/docs/evaluation.md:33`, `src/main/kotlin/io/amichne/konditional/api/EvaluationResult.kt:17`, `src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt:13`.
- Testing utilities exist via test fixtures (`withOverride`, `withOverrides`) and the `TestNamespaceFacade`, enabling scoped overrides and isolation. Evidence: `src/testFixtures/kotlin/io/amichne/konditional/fixtures/core/TestNamespaceOverrides.kt:136`, `src/main/kotlin/io/amichne/konditional/core/Namespace.kt:106`.

### Error Handling & Debugging
- Deserialization errors are structured (`ParseError.InvalidJson`, `ParseError.FeatureNotFound`) and returned as `ParseResult.Failure`, forcing explicit handling. Evidence: `src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt:11`, `src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:15`.
- Evaluation trace is available via `EvaluationResult` and logged when `EXPLAIN` mode is used. Evidence: `src/main/kotlin/io/amichne/konditional/api/FeatureUtilities.kt:104`, `src/main/kotlin/io/amichne/konditional/api/EvaluationResult.kt:26`.
- Observability hooks are intentionally dependency-free but require user integration for real logs/metrics. Evidence: `src/main/kotlin/io/amichne/konditional/core/ops/RegistryHooks.kt:11`, `src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt:3`.

## Enterprise Readiness

### Thread-Safety
- Lock-free reads and atomic updates are implemented via `AtomicReference` + `AtomicBoolean`, with synchronized writes for history updates. Evidence: `src/main/kotlin/io/amichne/konditional/core/registry/InMemoryNamespaceRegistry.kt:75`, `src/main/kotlin/io/amichne/konditional/core/registry/InMemoryNamespaceRegistry.kt:96`.
- Deterministic bucketing uses per-thread `MessageDigest` to avoid thread-safety issues. Evidence: `src/main/kotlin/io/amichne/konditional/core/evaluation/Bucketing.kt:12`.
- Concurrency tests explicitly attack evaluation, registration, and digest usage. Evidence: `src/test/kotlin/io/amichne/konditional/adversarial/ConcurrencyAttacksTest.kt:33`.

### Performance Profile
- Evaluation iterates `valuesByPrecedence` (sorted once) and computes at most one SHA-256 hash per match; this matches the documented O(n) evaluation model. Evidence: `src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt:38`, `docusaurus/docs/evaluation.md:106`.
- Normal evaluation still measures time and constructs `EvaluationResult` internally, which is a fixed overhead per call. Evidence: `src/main/kotlin/io/amichne/konditional/api/FeatureUtilities.kt:41`.

### Serialization Stability
- Snapshot format and `FeatureId` encoding are explicitly documented; a legacy prefix is normalized on load. Evidence: `docusaurus/docs/persistence-format.md:42`, `src/main/kotlin/io/amichne/konditional/values/FeatureId.kt:30`.
- Forward-compatibility for unknown keys is supported via `SnapshotLoadOptions.skipUnknownKeys`. Evidence: `src/main/kotlin/io/amichne/konditional/serialization/SnapshotLoadOptions.kt:19`, `src/test/kotlin/io/amichne/konditional/serialization/OperationalSerializationTest.kt:30`.

### Testing Support
- Test fixtures provide scoped overrides and isolation patterns (`withOverride`, `withOverrides`) and are validated with concurrency tests. Evidence: `src/testFixtures/kotlin/io/amichne/konditional/fixtures/core/TestNamespaceOverrides.kt:136`, `src/test/kotlin/io/amichne/konditional/core/TestNamespaceOverridesTest.kt:40`.

## Gap Analysis

### Critical Gaps
- `Context` hardcodes `AppLocale` and `Platform` enums, which are fixed and not extensible; this blocks consumers who need custom locale/platform models and is incompatible with public SDK expectations. Evidence: `src/main/kotlin/io/amichne/konditional/context/Context.kt:34`, `src/main/kotlin/io/amichne/konditional/context/AppLocale.kt:8`, `src/main/kotlin/io/amichne/konditional/context/Platform.kt:8`.
- Project docs claim a platform enum including `DESKTOP`, but the public enum omits it; this creates inconsistency in client expectations. Evidence: `CONTRIBUTING.md:33`, `src/main/kotlin/io/amichne/konditional/context/Platform.kt:8`.

### Quality Gaps
- `Feature.evaluate` KDoc states it can return null when a feature is not registered, but the implementation throws (via `NamespaceRegistry.flag`). This is a public API contract mismatch. Evidence: `src/main/kotlin/io/amichne/konditional/api/FeatureUtilities.kt:20`, `src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:152`.
- `NamespaceRegistry` KDoc references a `create()` factory that does not exist, encouraging incorrect usage. Evidence: `src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:35`, `src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:192`.
- `withOverrides` is documented as compile-time type safe but accepts `Any` at call sites; this is misleading for users of the test fixtures. Evidence: `src/testFixtures/kotlin/io/amichne/konditional/fixtures/core/TestNamespaceOverrides.kt:194`, `src/testFixtures/kotlin/io/amichne/konditional/fixtures/core/TestNamespaceOverrides.kt:213`.
- `StableId.of` lowercases using the default locale, which can change deterministic bucketing across devices with locale-specific casing rules. Evidence: `src/main/kotlin/io/amichne/konditional/core/id/StableId.kt:34`.
- Public `Placeholder` and `Specifier` types are exposed despite being internal rule plumbing; they increase API surface without documented consumer value. Evidence: `src/main/kotlin/io/amichne/konditional/rules/evaluable/Placeholder.kt:5`, `src/main/kotlin/io/amichne/konditional/rules/evaluable/Specifier.kt:3`.
- Kotlin reflection is used for custom value encoding/decoding, but there is no dependency or documentation called out for consumers (risk of missing `kotlin-reflect` at runtime). Evidence: `src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:18`, `build.gradle.kts:31`.

### Recommended Additions
- Replace `AppLocale` and `Platform` in `Context` with extensible types (e.g., sealed interfaces or value classes) and supply defaults via adapters; this removes consumer lock-in while preserving DSL ergonomics. Evidence: `src/main/kotlin/io/amichne/konditional/context/Context.kt:34`.
- Add an explicit `StableId.fromHex` (or `StableId.ofHex`) and document locale-invariant normalization to support migrations and deterministic cross-platform bucketing. Evidence: `src/main/kotlin/io/amichne/konditional/core/id/StableId.kt:34`, `docusaurus/docs/persistence-format.md:130`.
- Add explicit documentation (and dependency) for Kotlin reflection requirements when using `KotlinEncodeable` to prevent runtime surprises. Evidence: `src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:18`, `build.gradle.kts:31`.

## Specific Recommendations
1. Generalize `Context` so `locale` and `platform` are consumer-defined types (value classes or sealed interfaces) and provide library defaults via adapters; ship a migration guide from `AppLocale`/`Platform`. Evidence: `src/main/kotlin/io/amichne/konditional/context/Context.kt:34`.
2. Fix public API documentation mismatches and add missing KDoc to public DSL markers and helpers to prevent misuse (e.g., `evaluate` return contract, `NamespaceRegistry` factory doc, `withOverrides` type safety note). Evidence: `src/main/kotlin/io/amichne/konditional/api/FeatureUtilities.kt:20`, `src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:35`, `src/testFixtures/kotlin/io/amichne/konditional/fixtures/core/TestNamespaceOverrides.kt:194`, `src/main/kotlin/io/amichne/konditional/core/dsl/KonditionalDsl.kt:5`.
3. Add `StableId` normalization APIs and switch to locale-invariant casing to keep bucketing deterministic across device locales. Evidence: `src/main/kotlin/io/amichne/konditional/core/id/StableId.kt:34`.
4. Document or include `kotlin-reflect` as a dependency for `KotlinEncodeable` features, and add Android obfuscation guidance for axis IDs. Evidence: `src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:18`, `src/main/kotlin/io/amichne/konditional/core/registry/AxisRegistry.kt:91`.

## Evidence Appendix
- `Context` requires `AppLocale` and `Platform`: `src/main/kotlin/io/amichne/konditional/context/Context.kt:34`.
- `AppLocale` is a closed enum: `src/main/kotlin/io/amichne/konditional/context/AppLocale.kt:8`.
- `Platform` enum is limited to IOS/ANDROID/WEB: `src/main/kotlin/io/amichne/konditional/context/Platform.kt:8`.
- Deterministic bucketing uses ThreadLocal SHA-256: `src/main/kotlin/io/amichne/konditional/core/evaluation/Bucketing.kt:12`.
- JSON boundary via ParseResult: `src/main/kotlin/io/amichne/konditional/serialization/SnapshotSerializer.kt:79`.
- Reflection-based custom type parsing: `src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:92`.
- Observability hooks: `src/main/kotlin/io/amichne/konditional/core/ops/RegistryHooks.kt:11`.

