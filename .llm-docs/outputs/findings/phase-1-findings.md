Critical Evaluation: Konditional

Dimensions 1 & 2: API Surface Analysis and Complexity Audit

  ---
Dimension 1: API Surface Analysis

1.1 Public Type Inventory

| Category         | Type                           | Type Parameters | Justification                | Verdict                       |
  |------------------|--------------------------------|-----------------|------------------------------|-------------------------------|
| Core             | Namespace                      | -               | Isolation boundary           | ✓ Essential                   |
|                  | FeatureContainer<M>            | 1               | Feature definition container | ✓ Essential                   |
|                  | Feature<S,T,C,M>               | 4               | Core feature abstraction     | ⚠️ Over-parameterized         |
|                  | FlagDefinition<S,T,C,M>        | 4               | Runtime state                | ⚠️ Over-parameterized         |
| Feature Variants | BooleanFeature<C,M>            | 2               | Type-safe boolean            | ✓ Essential                   |
|                  | StringFeature<C,M>             | 2               | Type-safe string             | ✓ Essential                   |
|                  | IntFeature<C,M>                | 2               | Type-safe int                | ✓ Essential                   |
|                  | DoubleFeature<C,M>             | 2               | Type-safe double             | ✓ Essential                   |
|                  | EnumFeature<E,C,M>             | 3               | Type-safe enum               | ✓ Essential                   |
|                  | KotlinClassFeature<T,C,M>      | 3               | Custom types                 | ⚠️ Niche use case             |
| Context          | Context                        | -               | Evaluation context           | ✓ Essential                   |
|                  | ContextAware<C>                | 1               | Context factory              | ⚠️ Questionable value         |
|                  | FeatureAware<M>                | 1               | Container access             | ⚠️ Questionable value         |
| Type System      | EncodableValue<T>              | 1               | Sealed wrapper               | ⚠️ Accidental complexity      |
|                  | EncodableEvidence<T>           | 1               | Compile-time witness         | ⚠️ Clever, not obvious        |
|                  | KotlinEncodeable<S>            | 1               | Custom type interface        | ✓ Necessary for extensibility |
|                  | 6 *Encodeable variants         | -               | Type-specific wrappers       | ⚠️ Redundant abstraction      |
| Rules            | Rule<C>                        | 1               | Targeting rule               | ✓ Essential                   |
|                  | Evaluable<C>                   | 1               | Custom matcher               | ✓ Essential                   |
|                  | VersionRange (sealed)          | -               | Version targeting            | ✓ Essential                   |
| Serialization    | SnapshotSerializer             | -               | Global serializer            | ⚠️ Redundant path             |
|                  | NamespaceSnapshotSerializer<M> | 1               | Namespace serializer         | ⚠️ Overlaps with above        |
|                  | FeatureRegistry                | -               | Feature lookup               | ⚠️ Thread-safety issue        |
|                  | ParseResult<T>                 | 1               | Parse outcome                | ✓ Essential                   |
|                  | ParseError                     | -               | Error hierarchy              | ✓ Essential                   |
| DSL              | FlagScope<S,T,C,M>             | 4               | Flag config DSL              | ⚠️ Over-parameterized         |
|                  | RuleScope<C>                   | 1               | Rule config DSL              | ✓ Essential                   |
| Axis             | Axis<T>                        | 1               | Custom targeting axis        | ✓ Essential for extensibility |
|                  | AxisValue                      | -               | Axis value marker            | ✓ Essential                   |

Type Count Summary: ~43 public types (excluding Kontracts submodule)

  ---
1.2 Findings Table

| ID     | Finding                                                                                                           | Evidence                                                                                                                                                                                                                     | Severity | Category        |
  |--------|-------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-----------------|
| API-01 | Four type parameters on Feature<S,T,C,M> is excessive cognitive load                                              | Feature.kt:39 - Users must track S (wrapper), T (value), C (context), M (namespace). S and T are always paired (BooleanEncodeable/Boolean), making S redundant at call sites.                                                | Major    | Minimality      |
| API-02 | EncodableValue<T> wrapper provides marginal benefit vs. complexity                                                | EncodableValue.kt - Sealed interface wrapping primitives. Adds indirection without clear user benefit. Internal bookkeeping leaked to API surface.                                                                           | Major    | Minimality      |
| API-03 | Dual serialization paths: SnapshotSerializer + NamespaceSnapshotSerializer + FeatureContainer.toJson()/fromJson() | SnapshotSerializer.kt, NamespaceSnapshotSerializer.kt, FeatureContainer.kt:76-78 - Three ways to serialize. NamespaceSnapshotSerializer wraps SnapshotSerializer. Container methods delegate to NamespaceSnapshotSerializer. | Minor    | Redundancy      |
| API-04 | FeatureRegistry is NOT thread-safe but is a global singleton                                                      | FeatureRegistry.kt:37-38 - private val registry = mutableMapOf<...>(). No synchronization. Comment at line 33-34 warns about this but requires init-time registration.                                                       | Major    | Footgun         |
| API-05 | ContextAware<C> and FeatureAware<M> are mixin interfaces with unclear necessity                                   | ContextAware.kt:11, FeatureAware.kt:13 - These exist to support feature { } DSL syntax but add API surface for marginal syntactic sugar.                                                                                     | Minor    | Minimality      |
| API-06 | Two evaluation paths: feature { } DSL vs Feature.evaluate(context)                                                | FeatureUtilities.kt:12-14 vs FeatureUtilities.kt:34-37 - Both accomplish the same thing. The DSL requires ContextAware+FeatureAware mixins; direct evaluate() is simpler.                                                    | Minor    | Redundancy      |
| API-07 | Deprecated DATA_CLASS encoding still present                                                                      | EncodableValue.kt:42-47 - @Deprecated annotation but not removed. Should be cleaned up.                                                                                                                                      | Minor    | Consistency     |
| API-08 | rule(value) { criteria } syntax puts return value before criteria                                                 | FlagScope.kt:66-69 - Counter-intuitive ordering. Users think "if criteria, then value" but write "value, if criteria".                                                                                                       | Minor    | Discoverability |
| API-09 | Axis<T> auto-registers on init - implicit global state mutation                                                   | Axis.kt:68-71 - init { AxisRegistry.register(this) }. Implicit side effects during object initialization can cause ordering issues.                                                                                          | Minor    | Footgun         |
| API-10 | Namespace equality is by id string, not by reference                                                              | Namespace.kt:149-155 - Two different Namespace objects with same ID compare as equal. Could lead to accidental namespace collision.                                                                                          | Minor    | Footgun         |
| API-11 | ParseResult constructors are @PublishedApi internal but still accessible                                          | ParseResult.kt:20,26 - @PublishedApi internal constructor is meant for inline functions but allows construction from Java code.                                                                                              | Note     | Consistency     |
| API-12 | StableId.of(input) hex-encodes arbitrary strings, not parsing hex input                                           | StableId.kt:32-37 - Name suggests parsing but actually encodes. StableId.of("abc") produces hex encoding of "abc", not validation of hex string "abc".                                                                       | Minor    | Discoverability |

  ---
Dimension 2: Complexity Audit

2.1 Evaluation Path Trace

Entry → Exit (7 layers):
feature { AppFeatures.DARK_MODE }
→ FeatureUtilities.feature() extension
→ Feature.evaluate(context, registry)
→ NamespaceRegistry.flag(feature)
→ InMemoryNamespaceRegistry.flag() [override check]
→ FlagDefinition.evaluate(context)
→ Rule.matches(context)
→ BaseEvaluable.matches() + Evaluable.matches()
→ isInEligibleSegment() [SHA-256 bucket]
→ Return value or default

Abstraction depth: 7 layers from call site to SHA-256 bucketing decision.

2.2 Findings Table

| ID    | Finding                                                                             | Evidence                                                                                                                                                                                                                  | Severity | Category              |
  |-------|-------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-----------------------|
| CX-01 | EncodableValue<T> + EncodableEvidence<T> pattern is clever but non-obvious          | EncodableValue.kt, EncodableEvidence.kt - "Parse, don't validate" philosophy implemented via sealed interface + type witness. Requires understanding category theory concepts to grok. Not explained in user-facing docs. | Major    | Accidental Complexity |
| CX-02 | Generic type gymnastics: S/T pairing is always deterministic but both are required  | Feature<S,T,C,M> - S is always derivable from T (Boolean→BooleanEncodeable). The S parameter exists for internal type erasure reasons, not user benefit.                                                                  | Major    | Accidental Complexity |
| CX-03 | ConditionalValue<S,T,C,M> is internal but has 4 type parameters                     | ConditionalValue.kt:16 - Internal type with full generic complexity. Indicates type system complexity has leaked through the design.                                                                                      | Minor    | Accidental Complexity |
| CX-04 | Serialization model duplicates domain model                                         | internal/serialization/models/ - SerializableSnapshot, SerializableFlag, SerializableRule mirror Configuration, FlagDefinition, Rule. Necessary for Moshi but doubles the type count.                                     | Minor    | Essential Complexity  |          
| CX-05 | Registry delegation pattern adds indirection                                        | Namespace.kt:85 - Namespace implements NamespaceRegistry by registry. Users can call Namespace.load() or Namespace.registry.load(). Design intent is convenience but creates two paths.                                   | Minor    | Accidental Complexity |
| CX-06 | Axis<T> uses reflection to extract type argument                                    | Axis.kt:52-66 - this::class.supertypes.first { ... }.arguments.firstOrNull()?.type. Runtime reflection for compile-time information. Fragile if class hierarchy changes.                                                  | Minor    | Accidental Complexity |
| CX-07 | FeatureContainer.ContainerFeaturePropertyDelegate inner class has 4 type parameters | FeatureContainer.kt:328-333 - <F, S, T, C> plus implicit M from outer class. Users don't see this but it indicates internal complexity.                                                                                   | Note     | Essential Complexity  |
| CX-08 | SHA-256 bucketing recreates MessageDigest per call                                  | FlagDefinition.kt:116 - MessageDigest.getInstance("SHA-256") per evaluation. Comment explains thread-safety rationale but potential performance concern at scale.                                                         | Note     | Essential Complexity  |
| CX-09 | DSL implementation vs. interface split                                              | internal/builders/FlagBuilder.kt vs FlagScope.kt - Good separation, but requires maintaining parallel hierarchies.                                                                                                        | Note     | Essential Complexity  |
| CX-10 | Specificity calculation is opaque                                                   | BaseEvaluable.kt:60-64, Rule.kt:113-114 - Sum of constraint counts + extension specificity. Users must understand this for rule ordering. Not prominently documented.                                                     | Minor    | Discoverability       |

  ---
Summary Statistics

| Metric                       | Value               | Assessment                      |
  |------------------------------|---------------------|---------------------------------|
| Public types                 | ~43                 | High for a feature flag library |
| Type parameters on core type | 4                   | Excessive                       |
| Evaluation abstraction depth | 7 layers            | Acceptable but non-trivial      |
| Deprecated code present      | 1                   | Minor tech debt                 |
| Thread-safety issues         | 1 (FeatureRegistry) | Needs addressing                |
| Redundant API paths          | 3                   | Could consolidate               |

  ---
Severity Legend

| Severity | Definition                                         |
  |----------|----------------------------------------------------|
| Blocker  | Must fix before production use                     |
| Major    | Significant concern affecting adoption/maintenance |
| Minor    | Improvement opportunity, not blocking              |
| Note     | Observation, no action required                    |

  ---

⏺ Analysis complete. The findings tables above cover:

API Surface (Dimension 1): 12 findings identified

- 3 Major (type parameter overload, wrapper abstraction, thread-safety)
- 7 Minor (redundant paths, naming issues, implicit state)
- 2 Notes

Complexity Audit (Dimension 2): 10 findings identified

- 2 Major (EncodableValue/Evidence pattern, S/T type gymnastics)
- 5 Minor (reflection usage, delegation indirection, specificity opacity)
- 3 Notes (essential complexity that's acceptable)

Key concerns requiring attention:

1. FeatureRegistry thread-safety (API-04) - production risk
2. Four type parameters on Feature<S,T,C,M> where S is derivable from T (API-01, CX-02) - cognitive burden
3. EncodableValue wrapper leaks internal bookkeeping to API surface (API-02, CX-01) - barrier to adoption
