## I. GLOBAL SHAPE

* Primary concerns:

    * Context & targeting dimensions (`context` package).
    * Feature definition/evaluation (`core.features`, `core`, `rules`).
    * Configuration storage & atomics (`core.instance`, `core.registry`).
    * Encoding & supported value types (`core.types`).
    * Serialization & deserialization (`serialization`, `internal.serialization`).
    * Internal builders/DSL plumbing (`internal.builders`, `core.dsl`).

* Key generic axis:

    * `Feature<S, T, C, M>`

        * `S : EncodableValue<T>`
        * `T : Any`
        * `C : Context`
        * `M : Namespace`

* Evaluation flow (high-level):

    1. Consumer defines features via `FeatureContainer<M>`.
    2. Container delegates register flags into a `Namespace`’s `NamespaceRegistry`.
    3. Runtime builds a `Context` value (app + dimensions).
    4. Evaluation: `feature.evaluate(context)` → `NamespaceRegistry.flag(feature)` → `FlagDefinition.evaluate(context)` → `Rule.matches(context)` + rollout bucketing.

## II. PACKAGE: `io.amichne.konditional.context`

### 1. `AppLocale` (enum)

* FQN: `io.amichne.konditional.context.AppLocale`
* Purpose: canonicalized locale abstraction (language + country) without exposing raw ISO codes.
* Fields:

    * `private val language: String`
    * `private val country: String`
* Companion:

    * `fun from(language: String, country: String): AppLocale`

        * Looks up via `entries.single { it.language == language && it.country == country }`.
* Invariants:

    * One (language, country) pair per enum constant.
    * `from` throws if no or multiple matches.

### 2. `Platform` (enum)

* FQN: `io.amichne.konditional.context.Platform`
* Values: `IOS`, `ANDROID`, `WEB`.

### 3. `Version` (semantic version)

* FQN: `io.amichne.konditional.context.Version`
* Structure:

    * `data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version>`
* Invariants:

    * All components `>= 0` or throws.
* Ordering: `compareValuesBy(this, other, Version::major, Version::minor, Version::patch)`.
* Companion:

    * `of(major, minor, patch): Version`.
    * `parse(raw: String): ParseResult<Version>`

        * Uses safe `Result` to `ParseResult`, no throwing.
    * `parseUnsafe(raw: String): Version`

        * Deprecated; strict checking on segments.
    * `val default = Version(0, 0, 0)`.
* Role:

    * Used for version range targeting in rules.

### 4. `Rampup` (rollout percentage)

* FQN: `io.amichne.konditional.context.Rampup`
* `@JvmInline value class Rampup private constructor(val value: Double) : Comparable<Number>`
* Invariants:

    * `value` must be in `[0.0, 100.0]`, else require fails.
* Companion:

    * `of(Double|Int|String|Rampup): Rampup`.
    * `val MAX = Rampup(100.0)`, `val default = MAX`.
* `compareTo(other: Number)` → `value.compareTo(other.toDouble())`.
* Deprecated alias: `typealias Rollout = Rampup`.

### 5. `DimensionKey` & `Dimension<T>`

* `DimensionKey`

    * FQN: `io.amichne.konditional.context.DimensionKey`
    * Interface: `val id: String`
    * Intended for consumer enums: e.g. `enum class Environment(override val id: String) : DimensionKey { … }`.
* `Dimension<T>`

    * FQN: `io.amichne.konditional.context.Dimension`
    * Constraints: `T : DimensionKey`, `T : Enum<out T>`
    * Fields:

        * `val id: String` (axis id, e.g. `"env"`, `"region"`).
        * `val valueClass: KClass<out T>` (enum type).
    * Companion:

        * Inline factory: `operator fun <reified T> invoke(id: String): Dimension<T>` where `T : DimensionKey, T : Enum<T>`.
    * Semantics:

        * Describes a *dimension axis* (e.g. "env", "region") with a concrete enum type.

### 6. `Dimensions` (strongly typed dimension set)

* FQN: `io.amichne.konditional.context.Dimensions`
* Structure:

    * `class Dimensions internal constructor(private val values: Map<String, DimensionKey>)`
* Access:

    * `operator fun get(axisId: String): DimensionKey?`
    * `operator fun <T> get(axis: Dimension<T>): T? where T : DimensionKey, T : Enum<T> = values[axis.id] as? T`
* Companion:

    * `val EMPTY = Dimensions(emptyMap())`.
* Semantics:

    * Immutable map of axisId → concrete `DimensionKey`, with strongly-typed access via `Dimension<T>` descriptors.
    * Typically built via DSL `dimensions { … }` (see `DimensionBuilder`).

### 7. `Context`

* FQN: `io.amichne.konditional.context.Context`
* Interface:

    * Core fields:

        * `val locale: AppLocale`
        * `val platform: Platform`
        * `val appVersion: Version`
        * `val stableId: StableId`
    * Optional dimensions:

        * `val dimensions: Dimensions get() = Dimensions.EMPTY` (default).
    * Dynamic dimension access:

        * `fun getDimension(axisId: String): DimensionKey? = dimensions[axisId]`
* Nested:

    * `data class Core(...) : Context`
* Companion:

    * `operator fun invoke(locale, platform, appVersion, stableId): Core`
    * Deprecated extension: `fun <S, T, C, M> C.evaluate(key: Feature<S, T, C, M>): T` (bridged to namespace registry).
* Semantics:

    * Canonical evaluation input; all rules and rollouts depend on `Context`.
    * `dimensions` enables consumer-defined axes (env, region, tenant) through `Dimensions` and `DimensionKey`.

### 8. `ContextAware`

* FQN: `io.amichne.konditional.context.ContextAware`
* `fun interface ContextAware<out C : Context>`

    * `fun factory(): C`
    * `val context: C get() = factory()`
* Nested `Factory` fun interface for producing contexts.
* Semantics:

    * Utility for tying a *feature evaluation caller* to a context factory, used in DSL convenience functions.

### 9. Dimension extensions & axis registration

* File: `context/Extensions.kt`

* Key items:

    1. `inline fun <reified T, reified C : Context> C.dimension(axis: Dimension<T>): T?`

        * Reads via `getDimension(axis.id) as? T`.
    2. Type-based getter:

        * `inline fun <reified T> Context.dimension(): T? where T : DimensionKey, T : Enum<T>`

            * Uses `DimensionRegistry.axisFor(T::class)` to resolve `Dimension<T>`, then `dimensions[axis]`.
    3. `inline fun <reified T> dimensionAxis(id: String): Dimension<T>`

        * Returns `object : RegisteredDimension<T>(id, T::class) {}`
        * Registers axis in `DimensionRegistry` via `RegisteredDimension` init.
    4. Feature evaluation helpers (`feature(...)`) coupling `ContextAware` + `FeatureAware`.

* Axis-centric helpers for ergonomics:

    * `fun <T> Dimension<T>.valueIn(ctx: Context): T?`
    * `fun <T> Dimension<T>.setIn(builder: DimensionScope, value: T)`
    * Operator overloads:

        * `operator fun <T> Dimension<T>.invoke(ctx: Context): T?`
        * `operator fun <T> Dimension<T>.invoke(builder: DimensionScope, value: T)`

### 10. `RegisteredDimension<T>`

* FQN: `io.amichne.konditional.context.RegisteredDimension`
* Abstract class implementing `Dimension<T>`, performing registration:

    * `init { DimensionRegistry.register(this) }` (with `@Suppress("LeakingThis")`).
* Semantics:

    * Base class for axes that should auto-register with the type registry.

## III. PACKAGE: `io.amichne.konditional.core`

### 1. `Namespace`

* FQN: `io.amichne.konditional.core.Namespace`
* Sealed class:

    * Constructor: `(val id: String, internal val registry: NamespaceRegistry = NamespaceRegistry())`
    * Delegates to `NamespaceRegistry` (`: NamespaceRegistry by registry`).
* Subtypes:

    * `data object Global : Namespace("global")`
    * `data object Authentication : Namespace("auth")`
    * `data object Payments : Namespace("payments")`
    * `data object Messaging : Namespace("messaging")`
    * `data object Search : Namespace("search")`
    * `data object Recommendations : Namespace("recommendations")`
    * `sealed class Domain(id: String) : Namespace(id)` (for per-team/domain namespaces).
* Test:

    * `abstract class TestNamespaceFacade(id: String) : Namespace(id)`
* Equals/hashCode by `id`.
* Role:

    * Logical domain boundary for features.
    * Each `Namespace` owns its own `NamespaceRegistry` instance → configuration & runtime isolation.

### 2. `FlagDefinition<S, T, C, M>`

* FQN: `io.amichne.konditional.core.FlagDefinition`
* Data class:

    * `val defaultValue: T`
    * `val feature: Feature<S, T, C, M>`
    * `internal val values: List<ConditionalValue<S, T, C, M>>`
    * `val isActive: Boolean`
    * `val salt: String`
* Internal computed property:

    * `private val conditionalValues` sorted by `rule.specificity()` descending.
* Companion:

    * `operator fun invoke(feature, bounds, defaultValue, salt="v1", isActive=true): FlagDefinition<...>`
* Core method:

    * `fun evaluate(context: C): T`

        * If `!isActive` → `defaultValue`.
        * Else:

            * Finds first `ConditionalValue` where:

                * `it.rule.matches(context)`
                * `isInEligibleSegment(...) == true` (note: name is "inEligible" but semantics are "within rollout bucket").
            * Returns `.value` if found, otherwise `defaultValue`.
* Bucketing:

    * `isInEligibleSegment(flagKey, id: HexId, salt, rollout: Rampup): Boolean`:

        * If `rollout <= 0.0` → `false`.
        * If `rollout >= 100.0` → `true`.
        * Else:

            * `stableBucket(flagKey, id, salt) < (rollout.value * 100).roundToInt()`.
    * `stableBucket(flagKey, id, salt)`:

        * Uses SHA-256 on `"salt:flagKey:${id.id}"`.
        * Takes first 4 bytes, builds 32-bit number, modulo `10_000`.
* Semantics:

    * Each rule’s `Rollout` (0-100) is mapped to 0–10_000 discrete buckets.
    * Combined with `Context.stableId` to get deterministic rollout assignment.

### 3. `ValueType`

* Enum enumerating supported primitive/structured value flavors:

    * `BOOLEAN`, `STRING`, `INT`, `LONG`, `DOUBLE`, `JSON`, `ENUM`, `JSON_OBJECT`, `JSON_ARRAY`, `DATA_CLASS`.

## IV. PACKAGE: `io.amichne.konditional.core.dsl`

### 1. `KonditionalDsl` annotation

* `@DslMarker` to isolate DSL scopes.

### 2. `DimensionScope`

* FQN: `io.amichne.konditional.core.dsl.DimensionScope`
* Methods:

    * `fun <T> set(axis: Dimension<T>, value: T) where T : DimensionKey, T : Enum<T>`
    * `fun <T> setIfNotNull(axis: Dimension<T>, value: T?)`
* Implemented by `DimensionBuilder`.

### 3. `FlagScope<S, T, C, M>`

* FQN: `io.amichne.konditional.core.dsl.FlagScope`
* Generic constraints: `S : EncodableValue<T>, T : Any, C : Context, M : Namespace`
* Members:

    * `val default: T`
    * `fun active(block: () -> Boolean)`
    * `fun salt(value: String)`
    * `fun rule(build: RuleScope<C>.() -> Unit): Rule<C>`
    * `infix fun Rule<C>.returns(value: T)`
* Used by builders to construct `FlagDefinition` per feature.

### 4. `RuleScope<C : Context>`

* Methods:

    * `fun locales(vararg appLocales: AppLocale)`
    * `fun platforms(vararg ps: Platform)`
    * `fun versions(build: VersionRangeScope.() -> Unit)`
    * `fun <T> dimension(axis: Dimension<T>, vararg values: T) where T : DimensionKey, T : Enum<T>`
    * `fun extension(block: C.() -> Boolean)`
    * `fun note(text: String)`
    * `fun rollout(function: () -> Number)`
* Semantics:

    * Accumulates base constraints (locale, platform, versions, custom dimensions) + extension predicate + rollout percentage + note.

### 5. `VersionRangeScope`

* Methods:

    * `fun min(major: Int, minor: Int = 0, patch: Int = 0)`
    * `fun max(major: Int, minor: Int = 0, patch: Int = 0)`
* Implemented by `VersionRangeBuilder` → builds `VersionRange` variants.

## V. PACKAGE: `io.amichne.konditional.core.features`

### 1. `Feature<S, T, C, M>`

* Sealed interface (core abstraction):

    * `val key: String`
    * `val namespace: M`
* Implementations:

    * `BooleanFeature<C, M>`
    * `StringFeature<C, M>`
    * `IntFeature<C, M>`
    * `DoubleFeature<C, M>`
    * `EnumFeature<E, C, M>`
    * `DataClassFeature<T, C, M>` (for `KotlinEncodeable<ObjectSchema>` types).
* Each has internal `Impl` data class and a `companion object` factory.

### 2. `FeatureContainer<M : Namespace>`

* Abstract base for feature sets.
* Constructor: `(internal val namespace: M)`
* Implements `FeatureAware<M>`; property `container` = `this`.
* Internals:

    * `_features: MutableList<Feature<*, *, *, M>>`
    * `fun allFeatures(): List<Feature<*, *, *, M>>`
* DSL factory methods (delegated properties):

    * `protected fun <C : Context> boolean(default: Boolean, flagScope: FlagScope<BooleanEncodeable, Boolean, C, M>.() -> Unit = {})`
    * `protected fun <C : Context> string(default: String, ...)`
    * `protected fun <C : Context> integer(default: Int, ...)`
    * `protected fun <C : Context> double(default: Double, ...)`
    * `protected fun <E : Enum<E>, C : Context> enum(default: E, ...)`
    * `protected inline fun <reified T : KotlinEncodeable<ObjectSchema>, C : Context> custom(default: T, ...)`
    * Deprecated `int` and `dataClass` convenience methods.
* Inner `ContainerFeaturePropertyDelegate<F, S, T, C>`:

    * Captures property name as feature key.
    * On first access:

        * Creates feature via `factory(namespace, name)`.
        * Adds to `_features`.
        * Builds `FlagDefinition` via `FlagBuilder(default, feature).apply(configScope).build()`.
        * Updates namespace via `NamespaceRegistry.updateDefinition`.
* Semantics:

    * Central, type-safe definition point for all flags in a module.
    * Namespace scoping is explicit and compile-time enforced.

### 3. Feature extensions (`FeatureExtensions.kt`)

* `fun <S, T, C, M> Feature<S, T, C, M>.evaluate(context: C, registry: NamespaceRegistry = namespace): T`

    * `registry.flag(this).evaluate(context)`.
* Internal `update(...)` overloads on `Feature` to reconfigure definitions.
* Top-level `fun dimensions(block: DimensionScope.() -> Unit): Dimensions`

    * Creates `DimensionBuilder().apply(block).build()`.

### 4. `FeatureAware<M>`

* Interface with `val container: FeatureContainer<M>`.
* Companion `invoke` to produce an anonymous implementation given a container.

## VI. PACKAGE: `io.amichne.konditional.core.registry`

### 1. `NamespaceRegistry`

* Interface:

    * `fun load(config: Configuration)`
    * `val configuration: Configuration`
    * Default:

        * `fun <S, T, C, M> flag(key: Feature<S, T, C, M>): FlagDefinition<S, T, C, M>`

            * Implementation: `configuration.flags[key] as FlagDefinition<...>`.
        * `fun allFlags(): Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>> = configuration.flags`.
* Companion:

    * `operator fun invoke(configuration: Configuration = Configuration(emptyMap())): NamespaceRegistry`

        * Returns `InMemoryNamespaceRegistry().apply { load(configuration) }`.
    * `internal fun NamespaceRegistry.updateDefinition(definition: FlagDefinition<*, *, *, *>)`

        * Delegates to `InMemoryNamespaceRegistry` or `Namespace.registry`.

### 2. `InMemoryNamespaceRegistry`

* Internal class implementing `NamespaceRegistry`.
* State:

    * `private val current = AtomicReference(Configuration(emptyMap()))`
    * `private val overrides = ConcurrentHashMap<Feature<*, *, *, *>, Any>()`
* Overrides:

    * `load(config: Configuration)` → atomic set.
    * `override val configuration: Configuration get() = current.get()`
    * `override fun <S, T, C, M> flag(key: Feature<S, T, C, M>): FlagDefinition<S, T, C, M>`

        * If override present: wraps original `FlagDefinition` such that:

            * `values = emptyList()`, `defaultValue = override`, `isActive = true`.
        * Else: returns from configuration or throws `IllegalStateException` if missing.
* Internal methods:

    * `updatePatch(patch: ConfigurationPatch)` → `current.updateAndGet { patch.applyTo(it) }`.
    * `updateDefinition(definition: FlagDefinition<*, *, *, *>)` – single flag update.
    * Override management:

        * `setOverride(feature, value)`
        * `clearOverride(feature)`
        * `clearAllOverrides()`
        * `hasOverride(feature)`
        * `getOverride(feature)`

### 3. `DimensionRegistry`

* FQN: `io.amichne.konditional.core.registry.DimensionRegistry`
* Internal `object` mapping:

    * `byType: MutableMap<KClass<out DimensionKey>, Dimension<*>>`
* API:

    * `fun <T> register(axis: Dimension<T>)`

        * Ensures either new or same axis for that type; else require fails.
    * `fun <T> axisFor(type: KClass<T>): Dimension<T>? where T : DimensionKey, T : Enum<T>`
* Used by:

    * `RegisteredDimension<T>`
    * `Context.dimension<T>()` extension.
    * `DimensionBuilder.dimension<T>()`.

## VII. PACKAGE: `io.amichne.konditional.core.instance`

### 1. `Configuration`

* FQN: `io.amichne.konditional.core.instance.Configuration`
* Data class:

    * `val flags: Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>`
* Represents full snapshot of a namespace’s flag configuration.

### 2. `ConfigurationPatch`

* FQN: `io.amichne.konditional.core.instance.ConfigurationPatch`
* Data class:

    * `val flags: Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>`
    * `val removeKeys: Set<Feature<*, *, *, *>>`
* Methods:

    * `fun applyTo(configuration: Configuration): Configuration`

        * Remove `removeKeys`, then `putAll(flags)`.
* Companion:

    * `fun empty(): ConfigurationPatch`
    * `fun patch(builder: PatchBuilder.() -> Unit): ConfigurationPatch`
* Nested `PatchBuilder`:

    * Mutable `flags` and `removeKeys` with:

        * `fun <S, T, C> add(entry: FlagDefinition<S, T, C, *>)`
        * `fun remove(key: Feature<*, *, *, *>)`
* Used by:

    * `InMemoryNamespaceRegistry.updatePatch`.

## VIII. PACKAGE: `io.amichne.konditional.core.result`

### 1. `ParseError`

* Sealed interface with variants:

    * `InvalidHexId(input, message)`
    * `InvalidRollout(value, message)`
    * `InvalidVersion(input, message)`
    * `FeatureNotFound(key)`
    * `FlagNotFound(key)`
    * `InvalidSnapshot(reason)`
    * `InvalidJson(reason)`

### 2. `ParseResult<T>`

* Sealed interface:

    * `Success<T>(val value: T)`
    * `Failure(val error: ParseError)`
* Helpers in `core.result.utils.*`:

    * `fold`, `map`, `flatMap`, `getOrThrow`, `getOrNull`, `getOrDefault`, `getOrElse`, `onSuccess`, `onFailure`, `recover`, `toResult`.

### 3. `ParseException`

* Wrapper around `ParseError` for bridging to exception-oriented flows.

## IX. PACKAGE: `io.amichne.konditional.core.types`

### 1. `EncodableValue<T : Any>`

* Sealed interface:

    * `val value: T`
    * `val encoding: Encoding`
* `enum class Encoding` with values:

    * `BOOLEAN`, `STRING`, `INTEGER`, `DECIMAL`, `ENUM`, `JSON_OBJECT`, `JSON_ARRAY`, `CUSTOM`, `DATA_CLASS` (deprecated alias for `CUSTOM`).
* Companion:

    * `inline fun <reified T : Any> of(value: T, evidence: EncodableEvidence<T> = EncodableEvidence.get()): EncodableValue<T>`

        * Internally dispatches to `BooleanEncodeable`, `StringEncodeable`, `IntEncodeable`, `DecimalEncodeable`, `EnumEncodeable`, `JsonObjectEncodeable`, `JsonArrayEncodeable`, `DataClassEncodeable`.

### 2. `EncodableEvidence<T : Any>`

* Sealed interface capturing static proof that `T` is encodable.
* Companion:

    * `inline fun <reified T : Any> get(): EncodableEvidence<T>`

        * Recognizes primitives, enums, JSON types, `KotlinEncodeable`.
    * `inline fun <reified T : Any> isEncodable(): Boolean`
* Concrete objects: `BooleanEvidence`, `StringEvidence`, `IntEvidence`, `DoubleEvidence`, `JsonObjectEvidence`, `JsonArrayEvidence`, `CustomEvidence<T>`, deprecated `DataClassEvidence<T>`.

### 3. Primitive encodeables

* `BooleanEncodeable(Boolean)`
* `StringEncodeable(String)`
* `IntEncodeable(Int)`
* `DecimalEncodeable(Double)`

### 4. Enum encodeable

* `EnumEncodeable<E : Enum<*>>(value: E, enumClass: KClass<out E>)`

    * `fun toEncodedString(): String`
    * Companion:

        * `inline fun <reified E : Enum<E>> of(value: E)`
        * `fun <E : Enum<*>> fromString(name: String, enumClass: KClass<E>): EnumEncodeable<E>`

### 5. JSON encodeables

* `JsonEncodeable<J : JsonValue>` marker.
* `JsonObjectEncodeable(JsonObject, ObjectSchema)` + `of(value, schema)` with validation.
* `JsonArrayEncodeable(JsonArray, JsonSchema)` + `of(value, elementSchema)` with array validation.

### 6. Custom data class encodeables

* `KotlinEncodeable<out S : JsonSchema>`:

    * `val schema: S`
    * Alias: `typealias JsonSchemaClass = KotlinEncodeable<ObjectSchema>`.
* `DataClassEncodeable<T : KotlinEncodeable<ObjectSchema>>(value: T, schema: ObjectSchema)`

    * `fun toJsonValue(): JsonObject`
    * Companion: `fromJsonValue(jsonObject, schema)` returning `ParseResult<DataClassEncodeable<T>>`.
* `DataClassExtensions.kt`:

    * `fun KotlinEncodeable<ObjectSchema>.toJsonValue(schema: ObjectSchema? = null): JsonObject`
    * `inline fun <reified T : KotlinEncodeable<ObjectSchema>> JsonObject.parseAs(): ParseResult<T>`
    * Helpers `JsonValue.toKotlinValue`, `JsonValue.toPrimitiveValue`.

### 7. `Converter<I, O>`

* Simple bidirectional converter: `encode: I → O`, `decode: O → I`.

## X. PACKAGE: `io.amichne.konditional.rules`

### 1. `Rule<C : Context>`

* Data class:

    * `val rollout: Rampup`
    * `val note: String?`
    * `internal val baseEvaluable: BaseEvaluable<C>`
    * `val extension: Evaluable<C>`
* Secondary constructor (used by builder):

    * Accepts `locales`, `platforms`, `versionRange`, `dimensionConstraints`, `extension`.
* Behavior:

    * `override fun matches(context: C): Boolean = baseEvaluable.matches(context) && extension.matches(context)`
    * `override fun specificity(): Int = baseEvaluable.specificity() + extension.specificity()`

### 2. `ConditionalValue<S, T, C, M>`

* Internal data class: `val rule: Rule<C>, val value: T`
* Companion:

    * `internal fun Rule<C>.targetedBy(value: T): ConditionalValue<S, T, C, M>`

### 3. `BaseEvaluable<C : Context>`

* Fields:

    * `val locales: Set<AppLocale>`
    * `val platforms: Set<Platform>`
    * `val versionRange: VersionRange`
    * `val dimensionConstraints: List<DimensionConstraint>`
* `matches(context)`:

    * `locales.isEmpty() || context.locale in locales`
    * `platforms.isEmpty() || context.platform in platforms`
    * `!versionRange.hasBounds() || versionRange.contains(context.appVersion)`
    * `dimensionConstraints.all { (context.getDimension(it.axisId) ?: return false).id in it.allowedIds }`
* `specificity()`:

    * 1 for non-empty locales.
    * 1 for non-empty platforms.
    * 1 if version has bounds.
    * +1 per `DimensionConstraint`.

### 4. `DimensionConstraint`

* `internal data class DimensionConstraint(val axisId: String, val allowedIds: Set<String>)`
* Used by `BaseEvaluable` to check custom dimension rules.

### 5. `Evaluable<C : Context>`

* `fun interface Evaluable<in C : Context> : Specifier`

    * `fun matches(context: C): Boolean`
    * Companion:

        * `fun <C : Context> factory(matcher: (C) -> Boolean): Evaluable<C>`
* Default `specificity()` from `Specifier`.

### 6. `Placeholder`

* Singleton `object Placeholder : Evaluable<Context>`

    * Always matches (`true`) and specificity 0.

### 7. Version ranges (`rules.versions`)

* `sealed class VersionRange(type: Type, open val min: Version? = null, open val max: Version? = null)`

    * Types: `MIN_BOUND`, `MAX_BOUND`, `MIN_AND_MAX_BOUND`, `UNBOUNDED`.
    * `open fun contains(v: Version): Boolean`
    * `open fun hasBounds(): Boolean = min != null || max != null`
    * Companion:

        * `MIN_VERSION = Version(0, 0, 0)`
        * `MAX_VERSION = Version(2^32, 2^32, 2^32)`
* Subclasses:

    * `LeftBound(min: Version)` – min inclusive.
    * `RightBound(max: Version)` – max inclusive.
    * `FullyBound(min, max)` – both; requires `min <= max`.
    * `Unbounded` – overrides `contains` → always true; `hasBounds` → false.

## XI. PACKAGE: `io.amichne.konditional.internal.builders`

### 1. `DimensionBuilder`

* Implements `DimensionScope`.
* State: `private val values = mutableMapOf<String, DimensionKey>()`.
* Implementation:

    * `set(axis, value)` → `values[axis.id] = value`.
    * `setIfNotNull(axis, value)` → `set` when not null.
    * Inline helper:

        * `inline fun <reified T> DimensionBuilder.dimension(value: T)` (type-based setter)

            * Looks up axis via `DimensionRegistry.axisFor(T::class)` and calls `set(axis, value)`.
* `build(): Dimensions`:

    * If empty → `Dimensions.EMPTY`, else new `Dimensions(values.toMap())`.

### 2. `FlagBuilder<S, T, C, M>`

* Implements `FlagScope<S, T, C, M>`.
* State:

    * `override val default: T`
    * `private val feature: Feature<S, T, C, M>`
    * `private val conditionalValues = mutableListOf<ConditionalValue<S, T, C, M>>()`
    * `private var salt: String = "v1"`
    * `private var isActive: Boolean = true`
* Methods:

    * `active { ... }` sets `isActive`.
    * `salt(value: String)` sets salt.
    * `rule(build: RuleScope<C>.() -> Unit)`: builds `Rule` via `RuleBuilder`.
    * `Rule<C>.returns(value: T)` adds `ConditionalValue`.
* `build(): FlagDefinition<S, T, C, M>`:

    * Constructs with collected `conditionalValues`, `default`, `salt`, `isActive`.

### 3. `RuleBuilder<C : Context>`

* Implements `RuleScope<C>`.
* State:

    * `extension: Evaluable<C> = Placeholder`
    * `note: String?`
    * `range: VersionRange = Unbounded()`
    * `platforms: LinkedHashSet<Platform>`
    * `dimensionConstraints: MutableList<DimensionConstraint>`
    * `locales: LinkedHashSet<AppLocale>`
    * `rollout: Rampup?`
* Methods:

    * `locales(vararg appLocales)`, `platforms(vararg ps)`.
    * `versions(build)` → `VersionRangeBuilder`.
    * `extension(block: C.() -> Boolean)` → `Evaluable.factory(block)`.
    * DSL `dimensions(block: DimensionScope.() -> Unit)` (currently builds and discards `Dimensions`; targeting is via `dimension(...)` below).
    * `dimension(axis, vararg values)`:

        * Creates/merges `DimensionConstraint` per axisId, unioning `allowedIds`.
    * `note(text)`
    * `rollout(function)` → `Rampup.of(number.toDouble())`.
* `build(): Rule<C>`:

    * Instantiates `Rule` with `range`, `locales`, `platforms`, `dimensionConstraints`, `extension`, `rollout`.

### 4. `VersionRangeBuilder`

* Implements `VersionRangeScope`.
* State: `leftBound: Version = Version.default`, `rightBound: Version = Version.default`.
* `min(...)` and `max(...)` set these.
* `build(): VersionRange` selects:

    * Both set → `FullyBound`.
    * Only left ≠ default → `LeftBound`.
    * Only right ≠ default → `RightBound`.
    * Else → `Unbounded`.

## XII. PACKAGE: `io.amichne.konditional.internal.serialization`

### 1. `FlagValue<T : Any>`

* Internal sealed class representing serialized flag values.
* Variants:

    * `BooleanValue(Boolean)`
    * `StringValue(String)`
    * `IntValue(Int)`
    * `DoubleValue(Double)`
    * `EnumValue(value: String, enumClassName: String)`
    * `DataClassValue(value: Map<String, Any?>, dataClassName: String)`
* Methods:

    * `abstract fun toValueType(): ValueType`
* Companion:

    * `fun from(value: Any): FlagValue<*>`

        * Handles primitives, enums (by `name` + FQCN), custom `KotlinEncodeable` (to `JsonObject` → primitive map via `toPrimitiveValue()`).

### 2. `SerializableFlag`, `SerializableRule`, `SerializableSnapshot`, `SerializablePatch`

* `SerializableFlag`:

    * `key: String`, `defaultValue: FlagValue<*>`, `salt: String`, `isActive: Boolean`, `rules: List<SerializableRule>`.
* `SerializableRule`:

    * `value: FlagValue<*>`
    * `rampUp: Double`
    * `note: String?`
    * `locales: Set<String>`
    * `platforms: Set<String>`
    * `versionRange: VersionRange?`
* `SerializableSnapshot`:

    * `flags: List<SerializableFlag>`
* `SerializablePatch`:

    * `flags: List<SerializableFlag>`
    * `removeKeys: List<String>`

### 3. `FlagValueAdapter`

* Moshi `JsonAdapter<FlagValue<*>>`.
* `toJson` serializes `type` plus proper fields.
* `fromJson` decodes `type`, `value`, and associated metadata.
* Includes helper functions to serialize/deserialize arbitrary maps and values.

### 4. `VersionRangeAdapter`

* Custom Moshi adapter mapping JSON objects to `VersionRange` variants.
* Uses `type` + `min`/`max` fields.

## XIII. PACKAGE: `io.amichne.konditional.serialization`

### 1. `ConversionUtils` (`ConversionUtils.kt`)

* Extension `Configuration.toSerializable(): SerializableSnapshot`
* `FlagDefinition.toSerializable(flagKey: String): SerializableFlag`
* `ConditionalValue.toSerializable(): SerializableRule`
* `SerializableSnapshot.toSnapshot(): ParseResult<Configuration>`:

    * Resolves each flag via `FeatureRegistry`.
* `SerializableFlag.toFlagPair()`: returns `ParseResult<Pair<Feature, FlagDefinition>>`.
* `SerializableFlag.toFlagDefinition(Feature)`:

    * Extracts typed default via `FlagValue.extractValue<T>()`.
    * Builds `FlagDefinition` with `rules.map { it.toValue() }`.
* `SerializableRule.toValue()` and `.toRule()` bridging into `Rule<C>`.
* Patch conversion:

    * `ConfigurationPatch.toSerializable()`
    * `SerializablePatch.toPatch()`

### 2. `FeatureRegistry`

* Singleton registry for mapping flag keys (String) → `Feature<*, *, *, *>`.
* API:

    * `fun register(conditional: Feature<*, *, *, *>)`
    * `fun get(key: String): ParseResult<Feature<*, *, *, *>>`
    * `fun contains(key: String): Boolean`
    * `fun clear()`
* Required for deserialization: `SerializableFlag` needs to map key back to a `Feature` to construct `FlagDefinition`.

### 3. `SnapshotSerializer`

* Object providing storage-agnostic JSON serialization for entire `Configuration` snapshots and patches.
* Uses Moshi with:

    * `FlagValueAdapter.FACTORY`
    * `VersionRangeAdapter`
    * Polymorphic adapter for `VersionRange` subclasses.
    * `KotlinJsonAdapterFactory`.
* API:

    * `fun serialize(configuration: Configuration): String`
    * `fun fromJson(json: String): ParseResult<Configuration>`
    * `fun applyPatchJson(currentConfiguration: Configuration, patchJson: String): ParseResult<Configuration>`
* Internal:

    * `serializePatch`, `fromJsonPatch`, `applyPatch`.

### 4. `NamespaceSnapshotSerializer<M : Namespace>`

* Serializer bound to a single namespace.
* Constructor `(private val module: M, private val moshi: Moshi = SnapshotSerializer.defaultMoshi())`
* `toJson()`:

    * Serializes `module.configuration` to JSON.
* `fromJson(json)`:

    * Parses `SerializableSnapshot`, converts to `Configuration` with `toSnapshot()`, then `module.load(configuration)`.

### 5. `Serializer<T>`

* Interface:

    * `fun toJson(): String`
    * `fun fromJson(json: String): ParseResult<T>`
* `NamespaceSnapshotSerializer` implements `Serializer<Configuration>`.

## XIV. ID TYPES (`core.id`)

### 1. `HexId`

* Value class with:

    * `internal constructor(externalId: String)`
    * Derived `byteId` from hex string and `id` as canonical hex string.
* Invariants:

    * Not blank.
    * `id == externalId` after parsing ensures correct hex formatting.

### 2. `StableId`

* Sealed interface:

    * `val id: String`
    * `val hexId: HexId`
* Companion:

    * `fun of(input: String): StableId`

        * Requires `input` not blank.
        * Encodes `input.lowercase().encodeToByteArray()` to hex.
        * Ensures determinism & stability across processes.
* `StaticStableId` (test-only internal) for testing sealed hierarchy.

## XV. CORE EVALUATION CALL FLOW (END-TO-END)

1. **Feature definition (compile-time / startup)**

    * Consumer creates `object MyFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments)` and declares `val SOME_FLAG by boolean(default = ...) { ... }`.
    * On first access to `MyFeatures.SOME_FLAG`:

        * `ContainerFeaturePropertyDelegate` creates a `BooleanFeature<C, M>` with `key = "SOME_FLAG"`, `namespace = Namespace.Payments`.
        * Builds `FlagDefinition` through `FlagBuilder` and DSL (rules, rollout, notes, etc.).
        * Updates the namespace’s `NamespaceRegistry` (in-memory) with this definition.
        * Adds feature to `_features` for enumeration.

2. **Configuration loading / override**

    * Optionally, JSON snapshot is loaded via `SnapshotSerializer` or `NamespaceSnapshotSerializer` into a `Configuration`, then `Namespace.load(configuration)`.
    * Patches can be applied via `ConfigurationPatch` or JSON patch.

3. **Context creation**

    * Consumer builds a `Context` implementation:

        * Either `Context.Core(locale, platform, appVersion, stableId)`, or custom data class implementing `Context` with its own dimensions override.
    * For custom dimensions:

        * Consumer defines enums implementing `DimensionKey`.
        * Creates axes via `dimensionAxis<MyEnum>("axis-id")` or custom `RegisteredDimension<MyEnum>("axis-id", MyEnum::class)`.
        * Uses `dimensions { axis(this, MyEnum.VALUE) }` or `axis(builder, MyEnum.VALUE)` or type-based `DimensionBuilder.dimension(MyEnum.VALUE)` to populate `Dimensions`.
        * Implements `Context.dimensions` to return this `Dimensions`.

4. **Rule matching**

    * `FlagDefinition.evaluate(context)` sorts rules by specificity.
    * For each `ConditionalValue`:

        * `Rule.matches(context)`:

            * `BaseEvaluable.matches(...)` checks locales, platforms, versionRange, dimensionConstraints.
            * `extension.matches(context)` for custom predicate.
        * If matches:

            * `isInEligibleSegment(...)` uses `Context.stableId.hexId` and rollout to decide if user is in bucket.
        * First matching, bucket-eligible rule yields `value`.

5. **Result**

    * If no rule matches or rollout excludes user, returns `defaultValue`.
