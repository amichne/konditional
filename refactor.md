# Konditional API Refactoring: Maximum Type Safety & Minimal Surface

## Objective

Refactor the Konditional feature flag library to achieve:

1. **Iron-clad compile-time guarantees** - Make invalid states unrepresentable
2. **Minimal API surface** - Single-plane interface hiding all internal machinations
3. **Zero misuse potential** - API design prevents incorrect usage at compile time
4. **Maximum clarity** - Types and functions have obvious, unambiguous semantics

**Constraints:**
- Breaking changes are freely permitted
- Complexity is acceptable if it prevents runtime errors
- Target audience: Kotlin experts
- Documentation and examples will be comprehensive
- Focus on correctness over convenience

---

## Core Principle: Make Impossible States Impossible

The refactored API must enforce all invariants at compile time. If something can fail at runtime due to misuse, the API is wrong.

---

## 1. Single Entry Point: The `Konditional` Object

### Current Problem
- ~60 types exposed across 9 packages
- Multiple entry points (companion objects, top-level functions, factories)
- Internal types leak into public API (FlagDefinition, ConditionalValue, EncodableValue variants, serialization models)
- No obvious "start here" for users

### Required Changes

**Create a single `Konditional` facade object** that exposes ALL public functionality:

```kotlin
package io.amichne.konditional

/**
 * Single entry point for all Konditional operations.
 *
 * This object provides the complete API surface for:
 * - Defining feature flags
 * - Configuring flag behavior
 * - Evaluating flags in context
 * - Serializing/deserializing configurations
 */
object Konditional {
    // Feature flag definition factories
    fun <C : Context> boolean(key: String): BooleanFeature<C>
    fun <C : Context> string(key: String): StringFeature<C>
    fun <C : Context> int(key: String): IntFeature<C>
    fun <C : Context> double(key: String): DoubleFeature<C>
    fun <T : Any, C : Context> jsonObject(key: String): JsonObjectFeature<T, C>
    fun <T : Any, P : Any, C : Context> custom(key: String): CustomFeature<T, P, C>

    // Configuration DSL
    fun config(registry: FlagRegistry = FlagRegistry, block: ConfigScope.() -> Unit)
    fun buildConfig(block: ConfigScope.() -> Unit): Konfig
    fun patch(block: PatchScope.() -> Unit): KonfigPatch

    // Registry access
    val registry: FlagRegistry

    // Serialization (via extensions below)
}

// Serialization extensions on Konfig type
fun Konfig.toJson(): String
fun Konfig.Companion.fromJson(json: String): ParseResult<Konfig>

fun KonfigPatch.toJson(): String
fun KonfigPatch.Companion.fromJson(json: String): ParseResult<KonfigPatch>
```

**Action Items:**
1. Create `io.amichne.konditional.Konditional` object
2. Move all factory methods from companion objects to `Konditional`
3. Move all top-level functions (`boolean()`, `string()`, etc.) to `Konditional` methods
4. Deprecate old entry points with clear migration paths
5. Update all examples to use `Konditional` as entry point

---

## 2. Hide ALL Internal Types

### Types That MUST Be Internal

Move to `io.amichne.konditional.internal.*` and mark `internal`:

#### Core Implementation (`internal/core/`)
- `Conditional<S, T, C>` interface and all implementations
    - **Reasoning:** Users interact via typed wrappers (BooleanFeature, etc.), not raw Conditional
    - **Exception:** Keep type parameter for delegation pattern, but hide interface itself
- `FlagDefinition<S, T, C>`
    - **Currently:** In wrong package location; marked internal but exposed
    - **Fix:** Move to `internal/core/FlagDefinition.kt`
- `FeatureFlag<S, T, C>` sealed class
    - **Reasoning:** Users never construct or evaluate directly; only via DSL and Context extensions
    - **Make sealed class internal:** Users see typed feature interfaces, not the base class
- `SingletonFlagRegistry`
    - **Currently:** Already internal, but accessed via companion delegation
    - **Keep:** Good pattern; maintain as-is

#### Type Safety Machinery (`internal/types/`)
- **ALL** `EncodableValue` implementations:
    - `BooleanEncodeable`, `StringEncodeable`, `IntEncodeable`, `DecimalEncodeable`
    - `JsonObjectEncodeable`, `CustomEncodeable`
    - **Reasoning:** Created only by witness pattern; users never construct directly
- **ALL** `EncodableEvidence` witnesses:
    - `BooleanEvidence`, `StringEvidence`, `IntEvidence`, `DoubleEvidence`
    - **Reasoning:** Compile-time machinery; no runtime presence needed in public API

#### Rule System (`internal/rules/`)
- `ConditionalValue<S, T, C>`
    - **Reasoning:** Implementation detail of how rules map to values
    - **Currently:** Exposed via `FeatureFlag.values: List<ConditionalValue>`
    - **Fix:** Change to opaque type or hide FeatureFlag entirely
- `BaseEvaluable<C>`
    - **Reasoning:** Only used internally by Rule composition
    - **Users interface:** Via RuleBuilder DSL, never directly

#### Builders (`internal/builders/`)
- `ConfigBuilder`, `ModuleBuilder`, `FlagBuilder`, `RuleBuilder`, `PatchBuilder`, `VersionRangeBuilder`
    - **Reasoning:** Implementation of DSL; users see only lambda receivers
    - **Public API:** Only DSL scope types (`ConfigScope`, `ModuleScope`, etc.)
    - **Pattern:** Use `@PublishedApi internal` for inline DSL functions

#### Serialization (`internal/serialization/`)
- `SnapshotSerializer` implementation class
    - **Public API:** Extension functions on Konfig/KonfigPatch
    - **Internal:** All serializer implementation
- ALL serialization models:
    - `SerializableSnapshot`, `SerializablePatch`, `SerializableFlag`, `SerializableRule`, `FlagValue`
    - **Reasoning:** DTO types only used by serializer
- ALL Moshi adapters:
    - `FlagValueAdapter`, `VersionRangeAdapter`
    - **Reasoning:** Implementation details of JSON serialization

---

## 3. Strengthen Type Safety: Prevent Misuse at Compile Time

### 3.1 Make Flag Definitions Immutable and Type-Safe

**Current Issue:** Enum-based flag definitions with delegation pattern require manual key management:

```kotlin
enum class MyFlags(override val key: String) : BooleanFeature<Context> by boolean(key) {
    FEATURE_A("feature_a")  // Key appears twice (name + string)
}
```

**Problem:** Key duplication; easy to mistype; no compile-time verification that key matches enum name.

**Solution Option A: Property Delegation with Key Inference**

```kotlin
object MyFlags : FlagModule<Context>() {
    val FEATURE_A by boolean()  // Key automatically derived: "feature_a"
    val FEATURE_B by string()   // Key automatically derived: "feature_b"
}

// Implementation
abstract class FlagModule<C : Context> {
    protected inline fun <reified T : Any> boolean(): ReadOnlyProperty<FlagModule<C>, BooleanFeature<C>> {
        return FlagPropertyDelegate { propertyName ->
            Konditional.boolean(propertyName.toSnakeCase())
        }
    }
    // ... other types
}
```

**Solution Option B: Keep Enum Pattern but Remove Duplication**

```kotlin
enum class MyFlags : BooleanFeature<Context> by Konditional.boolean() {
    FEATURE_A,
    FEATURE_B;

    override val key: String get() = name.toSnakeCase()
}
```

**Recommendation:** Implement **both** patterns. Let users choose:
- Enum for exhaustive when/sealed pattern matching
- Object for more flexible property-based access

### 3.2 Make Module Organization Mandatory at Compile Time

**Current Issue:** Flags can be defined without module context; module enforcement happens at runtime in DSL.

**Solution: Flags Carry Module Information at Type Level**

```kotlin
interface Module {
    val moduleName: String
}

// Feature interfaces bound to module at definition time
interface BooleanFeature<C : Context> {
    val key: String
    val module: Module  // Enforced at compile time
}

// Usage enforces module at definition
enum class MyModules : Module {
    USER_FEATURES,
    PAYMENT_FEATURES
}

object UserFlags : FlagModule<Context>(MyModules.USER_FEATURES) {
    val ENABLE_PROFILE by boolean()
}

// Now impossible to define flags without module
```

### 3.3 Context Type Safety: Prevent Context Mismatch

**Current Issue:** Flag can be defined with `Context` but evaluated with incompatible context type.

**Solution: Phantom Type for Context Constraint**

```kotlin
// Flag definition encodes required context type
enum class UserFlags(override val key: String)
    : BooleanFeature<EnterpriseContext> by Konditional.boolean(key) {
    ENTERPRISE_FEATURE("enterprise_feature")
}

// This won't compile - context type mismatch
val basicContext: Context = Context(...)
val value = basicContext.evaluate(UserFlags.ENTERPRISE_FEATURE)  // ❌ Compile error

// This compiles - types match
val enterpriseContext: EnterpriseContext = EnterpriseContext(...)
val value = enterpriseContext.evaluate(UserFlags.ENTERPRISE_FEATURE)  // ✅
```

**Action:** Ensure type parameter `C : Context` is preserved and checked throughout evaluation chain.

### 3.4 Make Registry Operations Type-Safe

**Current Issue:** Registry methods use type erasure (`Conditional<*, *, *>`, `FeatureFlag<*, *, *>`).

**Solution: Use Reified Generics and Type-Safe Registry Operations**

```kotlin
interface FlagRegistry {
    // Type-safe retrieval
    fun <S : EncodableValue<T>, T : Any, C : Context> get(
        flag: Feature<S, T, C>
    ): FlagDefinition<S, T, C>?

    // Type-safe updates
    fun <S : EncodableValue<T>, T : Any, C : Context> update(
        flag: Feature<S, T, C>,
        definition: FlagDefinition<S, T, C>
    )

    // No type erasure in return types
    fun allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *>>  // Still star-projected but better than current
}

// Even better: Use sealed class for flag queries to avoid star projection
sealed class FlagQuery<out T : Any> {
    data class Boolean<C : Context>(val flag: BooleanFeature<C>) : FlagQuery<kotlin.Boolean>()
    data class String<C : Context>(val flag: StringFeature<C>) : FlagQuery<kotlin.String>()
    // ...
}

fun <T : Any> FlagRegistry.query(query: FlagQuery<T>): T?
```

### 3.5 Prevent Invalid Rule Construction

**Current Issue:** Rules can be constructed in invalid states (e.g., empty version range bounds).

**Solution: Builder Pattern with State Machine**

```kotlin
// Current: Mutable builder
class RuleBuilder<C> {
    var rollout: Rollout? = null  // Can be set multiple times
    // ...
}

// Improved: State machine for rule building
sealed interface RuleBuilderState<C : Context> {
    interface Empty<C : Context> : RuleBuilderState<C> {
        fun platforms(vararg ps: Platform): WithTargeting<C>
        fun locales(vararg ls: AppLocale): WithTargeting<C>
        fun versions(builder: VersionRangeBuilder.() -> Unit): WithTargeting<C>
        fun extension(evaluable: Evaluable<C>): WithExtension<C>
        fun rollout(percentage: Double): WithRollout<C>
    }

    interface WithTargeting<C : Context> : RuleBuilderState<C> {
        fun rollout(percentage: Double): Complete<C>
        fun build(): Rule<C>
    }

    interface WithRollout<C : Context> : RuleBuilderState<C> {
        fun platforms(vararg ps: Platform): Complete<C>
        fun build(): Rule<C>
    }

    interface Complete<C : Context> : RuleBuilderState<C> {
        fun build(): Rule<C>
    }
}

// Usage ensures valid combinations
rule {
    platforms(Platform.IOS)  // Returns WithTargeting
        .rollout(50.0)       // Returns Complete
}  // Must call build() to get Rule
```

**Consideration:** This adds complexity. **Evaluate trade-off:**
- **Pro:** Impossible to construct invalid rules
- **Con:** More verbose API, complex builder implementation
- **Decision:** Only implement if we've seen production issues with invalid rules

### 3.6 Version Range Type Safety

**Current Issue:** Version ranges can be constructed in invalid states (min > max).

**Solution: Smart Constructor Pattern**

```kotlin
sealed class VersionRange private constructor() {
    data object Unbounded : VersionRange()
    data class LeftBound private constructor(val min: Version) : VersionRange()
    data class RightBound private constructor(val max: Version) : VersionRange()
    data class FullyBound private constructor(val min: Version, val max: Version) : VersionRange()

    companion object {
        fun unbounded(): VersionRange = Unbounded

        fun leftBound(min: Version): VersionRange = LeftBound(min)

        fun rightBound(max: Version): VersionRange = RightBound(max)

        fun fullyBound(min: Version, max: Version): ParseResult<VersionRange> {
            return if (min <= max) {
                ParseResult.Success(FullyBound(min, max))
            } else {
                ParseResult.Failure(
                    ParseError.InvalidVersion("min ($min) must be <= max ($max)")
                )
            }
        }
    }
}
```

**Action:** Ensure all factory methods validate inputs and return `ParseResult` when validation can fail.

---

## 4. Improve Naming for Clarity

### Types to Rename

| Current Name | Proposed Name | Rationale |
|--------------|---------------|-----------|
| `Conditional<S, T, C>` | `Feature<S, T, C>` or `FlagKey<S, T, C>` | "Conditional" sounds like a rule/condition, not an identifier |
| `FeatureFlag<S, T, C>` | `FlagDefinition<S, T, C>` | More explicit about what it represents |
| `Conditional.OfJsonObject<T, C>` | `JsonObjectFeature<T, C>` | Remove awkward "Of" prefix; simpler name |
| `Conditional.OfCustom<T, P, C>` | `CustomFeature<T, P, C>` | Remove awkward "Of" prefix; simpler name |
| `EncodableValue<T>` | Keep or rename to `EncodedValue<T>` | Less important since internal |
| `EncodableEvidence<T>` | `TypeWitness<T>` | More accurate name for witness pattern |
| `Konfig` | Keep (distinctive) | Or `Configuration` if avoiding "K" prefix |
| `KonfigPatch` | Keep (distinctive) | Or `ConfigurationPatch` |
| `ConditionalValue<S, T, C>` | `TargetedValue<S, T, C>` | More descriptive of rule → value mapping |
| `BaseEvaluable<C>` | `StandardTargeting<C>` | Clearer what "base" means |

**Action Items:**
1. Perform global rename for each type
2. Add type aliases for backward compatibility during deprecation:
   ```kotlin
   @Deprecated("Renamed to Feature", ReplaceWith("Feature"))
   typealias Conditional<S, T, C> = Feature<S, T, C>
   ```
3. Update all documentation and examples

---

## 5. Consolidate DSL and Improve Scoping

### Current Issues
- Builder classes exposed in public API
- No clear scoping boundaries (can call wrong functions in wrong scope)
- `@FeatureFlagDsl` marker helps but doesn't prevent all misuse

### Solution: Sealed DSL Scopes with Phantom Types

```kotlin
// DSL scopes are interfaces, not classes - can't be instantiated
@FeatureFlagDsl
sealed interface ConfigScope {
    fun module(module: Module, block: ModuleScope.() -> Unit)
}

@FeatureFlagDsl
sealed interface ModuleScope {
    // Infix operator for configuration
    infix fun <S : EncodableValue<T>, T : Any, C : Context> Feature<S, T, C>.with(
        block: FlagScope<S, T, C>.() -> Unit
    )
}

@FeatureFlagDsl
sealed interface FlagScope<S : EncodableValue<T>, T : Any, C : Context> {
    fun default(value: T, coverage: Double? = null)
    fun salt(value: String)
    infix fun rule(block: RuleScope<C>.() -> Unit): Rule<C>
    infix fun Rule<C>.implies(value: T)
}

@FeatureFlagDsl
sealed interface RuleScope<C : Context> {
    fun locales(vararg appLocales: AppLocale)
    fun platforms(vararg ps: Platform)
    fun versions(build: VersionRangeScope.() -> Unit)
    fun extension(function: () -> Evaluable<C>)
    fun note(text: String)
    var rollout: Rollout?
}

@FeatureFlagDsl
sealed interface VersionRangeScope {
    fun min(major: Int, minor: Int = 0, patch: Int = 0)
    fun max(major: Int, minor: Int = 0, patch: Int = 0)
}

// Internal implementations hidden
@PublishedApi
internal class ConfigScopeImpl : ConfigScope { /* ... */ }

@PublishedApi
internal class ModuleScopeImpl : ModuleScope { /* ... */ }

// Etc.
```

**Benefits:**
- Scopes cannot be instantiated by users
- Clear boundary between public API (scopes) and internal implementation (scope implementations)
- IDE autocomplete only shows relevant functions in each scope
- `@FeatureFlagDsl` prevents nesting wrong scopes

**Action Items:**
1. Convert all builder classes to sealed scope interfaces
2. Create internal implementations with `@PublishedApi internal`
3. Ensure inline DSL functions reference internal implementations
4. Add comprehensive DSL scope tests

---

## 6. Serialization: Hide Implementation Details

### Current Issues
- `SnapshotSerializer` is public singleton with 8 methods
- All serialization DTOs are public (`SerializableSnapshot`, `SerializablePatch`, `FlagValue`, etc.)
- Moshi adapters exposed in public API
- Users must understand serializer internals

### Solution: Extension Functions + Internal Serializer

```kotlin
// Public API - simple extension functions
fun Konfig.toJson(): String

fun Konfig.Companion.fromJson(json: String): ParseResult<Konfig>

fun KonfigPatch.toJson(): String

fun KonfigPatch.Companion.fromJson(json: String): ParseResult<KonfigPatch>

// All serialization internals hidden
// internal/serialization/
//   ├── SnapshotSerializer.kt (internal)
//   ├── models/ (all internal)
//   │   ├── SerializableSnapshot.kt
//   │   ├── SerializablePatch.kt
//   │   ├── SerializableFlag.kt
//   │   └── FlagValue.kt
//   └── adapters/ (all internal)
//       ├── FlagValueAdapter.kt
//       └── VersionRangeAdapter.kt
```

**Action Items:**
1. Create extension functions on `Konfig` and `KonfigPatch`
2. Move `SnapshotSerializer` to `internal` package
3. Move all serialization models to `internal/serialization/models`
4. Move all adapters to `internal/serialization/adapters`
5. Mark all serialization types `internal`
6. Update documentation to show extension function usage

---

## 7. Error Handling: Structured and Actionable

### Current State
- `ParseResult<T>` and `EvaluationResult<T>` - Good pattern ✅
- `ParseError` sealed interface with 10 variants - Could be refined

### Improvements

#### 7.1 Consolidate Error Types

```kotlin
sealed interface ParseError {
    val message: String
    val suggestion: String?

    // Domain errors
    data class InvalidFormat(
        val input: String,
        val expected: String,
        override val message: String,
        override val suggestion: String? = null
    ) : ParseError

    data class NotFound(
        val key: String,
        override val message: String,
        override val suggestion: String? = "Check that flag '$key' is defined in config"
    ) : ParseError

    data class TypeMismatch(
        val expected: String,
        val actual: String,
        val context: String,
        override val message: String,
        override val suggestion: String? = null
    ) : ParseError

    data class ConstraintViolation(
        val constraint: String,
        val value: Any?,
        override val message: String,
        override val suggestion: String? = null
    ) : ParseError

    // Catch-all for unexpected errors
    data class UnexpectedError(
        val cause: Throwable,
        override val message: String = cause.message ?: "Unexpected error",
        override val suggestion: String? = null
    ) : ParseError
}
```

**Action:** Add `suggestion` field to all error types to guide users toward fixes.

#### 7.2 Make Error Handling Ergonomic

```kotlin
// Add Result-like extension functions
inline fun <T, R> ParseResult<T>.map(transform: (T) -> R): ParseResult<R>

inline fun <T, R> ParseResult<T>.flatMap(transform: (T) -> ParseResult<R>): ParseResult<R>

inline fun <T> ParseResult<T>.onSuccess(action: (T) -> Unit): ParseResult<T>

inline fun <T> ParseResult<T>.onFailure(action: (ParseError) -> Unit): ParseResult<T>

inline fun <T> ParseResult<T>.recover(transform: (ParseError) -> T): T

// Usage
val result = Konfig.fromJson(json)
    .onFailure { error -> logger.error(error.message, error.suggestion) }
    .recover { Konfig.empty() }
```

---

## 8. Context and Evaluation: Simplify and Strengthen

### Current State
- `Context` interface with factory ✅
- Extension functions for evaluation ✅
- StableId with HexId internal type ✅

### Improvements

#### 8.1 Make Context Creation Type-Safe

```kotlin
// Current: Separate StableId factory
val id = StableId.of("user-123")
val context = Context(locale, platform, version, id)

// Improved: Single factory with validation
sealed interface StableId {
    companion object {
        fun of(id: String): ParseResult<StableId>  // Validates format
    }
}

// Or inline in Context factory with validation
fun Context(
    locale: AppLocale,
    platform: Platform,
    appVersion: Version,
    stableId: String  // Validates internally
): ParseResult<Context>
```

**Consideration:** If validation fails, return `ParseResult` rather than throwing. Maintains "parse, don't validate" principle.

#### 8.2 Evaluation Type Safety

```kotlin
// Current: Generic evaluate with type parameter
inline fun <reified T : Any, S : EncodableValue<T>, C : Context> C.evaluate(
    flag: Feature<S, T, C>
): T

// Issue: Requires understanding of EncodableValue system

// Improved: Specific overloads for clarity
fun <C : Context> C.evaluate(flag: BooleanFeature<C>): Boolean
fun <C : Context> C.evaluate(flag: StringFeature<C>): String
fun <C : Context> C.evaluate(flag: IntFeature<C>): Int
fun <C : Context> C.evaluate(flag: DoubleFeature<C>): Double
fun <T : Any, C : Context> C.evaluate(flag: JsonObjectFeature<T, C>): T
fun <T : Any, P : Any, C : Context> C.evaluate(flag: CustomFeature<T, P, C>): T

// Also provide Result-based variants for error handling
fun <C : Context> C.tryEvaluate(flag: BooleanFeature<C>): EvaluationResult<Boolean>
```

**Action:** Provide both throwing and Result-returning variants for evaluation.

---

## 9. Advanced Features: Custom Evaluators

### Current State
- `Evaluable<C>` abstract class for custom evaluation logic ✅
- Users can extend for domain-specific targeting ✅

### Improvements

#### 9.1 Make Evaluable More Discoverable

```kotlin
// Current: Abstract class
abstract class Evaluable<C : Context> {
    abstract fun matches(context: C): Boolean
    open fun specificity(): Int = 0
}

// Improved: Functional interface for simple cases
fun interface SimpleEvaluable<C : Context> {
    fun matches(context: C): Boolean
}

// Complex cases use full class
abstract class CustomEvaluable<C : Context> : SimpleEvaluable<C> {
    abstract override fun matches(context: C): Boolean
    open fun specificity(): Int = 0
    open fun description(): String? = null
}

// Usage in DSL
rule {
    extension { context -> context.organizationId == "enterprise-123" }
}
```

#### 9.2 Provide Common Evaluable Builders

```kotlin
object Evaluables {
    // Predicate-based
    fun <C : Context> predicate(
        specificity: Int = 1,
        description: String? = null,
        predicate: (C) -> Boolean
    ): Evaluable<C>

    // Composition
    fun <C : Context> allOf(vararg evaluables: Evaluable<C>): Evaluable<C>
    fun <C : Context> anyOf(vararg evaluables: Evaluable<C>): Evaluable<C>
    fun <C : Context> not(evaluable: Evaluable<C>): Evaluable<C>

    // Common patterns
    fun <C : Context> always(): Evaluable<C>
    fun <C : Context> never(): Evaluable<C>
}
```

---

## 10. Registry: Make It Explicit and Type-Safe

### Current Issues
- Companion object delegation to singleton is clever but opaque
- Type erasure in registry methods
- No clear way to create test registries

### Solutions

#### 10.1 Explicit Singleton Access

```kotlin
// Current: Companion object delegates to singleton
interface FlagRegistry {
    companion object : FlagRegistry by SingletonFlagRegistry
}

// Improved: Explicit singleton + factory
interface FlagRegistry {
    // ... methods ...

    companion object {
        val Default: FlagRegistry = SingletonFlagRegistry

        fun create(): FlagRegistry = DefaultFlagRegistry()

        fun createInMemory(): FlagRegistry = InMemoryFlagRegistry()
    }
}

// Usage
val registry = FlagRegistry.Default  // Explicit
val testRegistry = FlagRegistry.createInMemory()  // For testing
```

#### 10.2 Type-Safe Registry Operations

```kotlin
interface FlagRegistry {
    fun load(config: Konfig)
    fun update(patch: KonfigPatch)

    // Type-safe get
    operator fun <S : EncodableValue<T>, T : Any, C : Context> get(
        flag: Feature<S, T, C>
    ): FlagDefinition<S, T, C>?

    // Type-safe set
    operator fun <S : EncodableValue<T>, T : Any, C : Context> set(
        flag: Feature<S, T, C>,
        definition: FlagDefinition<S, T, C>
    )

    // Snapshot
    fun snapshot(): Konfig

    // Bulk operations with type safety
    fun bulkGet(flags: Set<Feature<*, *, *>>): Map<Feature<*, *, *>, FlagDefinition<*, *, *>>
}
```

---

## 11. Implementation Roadmap

### Phase 1: Internal Reorganization (Non-Breaking)
**Estimated Effort: 1-2 days**

1. Create `io.amichne.konditional.internal` package structure
2. Move all mis-placed internal types:
    - `FlagDefinition` → `internal/core/`
    - Serialization models → `internal/serialization/models/`
    - Adapters → `internal/serialization/adapters/`
    - Builder implementations → `internal/builders/`
3. Mark types `internal`:
    - `ConditionalValue`
    - `BaseEvaluable`
    - All `EncodableValue` variants
    - All `EncodableEvidence` singletons
    - All serialization types
4. Verify tests pass
5. Run existing examples to ensure no breakage

**Success Criteria:**
- All internal types hidden from public API
- No breaking changes to existing usage
- All tests pass

---

### Phase 2: Create Single Entry Point (Additive, Non-Breaking)
**Estimated Effort: 2-3 days**

1. Create `Konditional` facade object
2. Implement factory methods delegating to existing implementations
3. Create extension functions for serialization:
    - `Konfig.toJson()`
    - `Konfig.fromJson()`
4. Add comprehensive tests for new entry point
5. Update examples to show new API alongside old
6. Document both APIs during transition period

**Success Criteria:**
- `Konditional` object provides all public functionality
- Old API still works
- New API is more discoverable
- Documentation covers both approaches

---

### Phase 3: Rename Types for Clarity (Breaking)
**Estimated Effort: 1-2 days**

1. Rename core types:
    - `Conditional` → `Feature`
    - `FeatureFlag` → `FlagDefinition`
    - `OfJsonObject` → `JsonObjectFeature`
    - `OfCustom` → `CustomFeature`
    - Others per table in Section 4
2. Add type aliases for backward compatibility:
   ```kotlin
   @Deprecated("Renamed to Feature", ReplaceWith("Feature<S, T, C>"))
   typealias Conditional<S, T, C> = Feature<S, T, C>
   ```
3. Update all internal references
4. Update all documentation
5. Update all examples
6. Create migration guide

**Success Criteria:**
- All renames complete
- Type aliases provide compatibility
- Clear deprecation warnings with ReplaceWith
- Migration guide published

---

### Phase 4: Strengthen Type Safety (Breaking)
**Estimated Effort: 3-5 days**

1. Implement sealed DSL scopes (Section 5)
2. Add smart constructors with validation (Section 3.6)
3. Improve error types with suggestions (Section 7)
4. Add Result-returning evaluation methods (Section 8.2)
5. Make registry operations type-safe (Section 10.2)
6. Comprehensive test coverage for all compile-time guarantees

**Success Criteria:**
- DSL scopes prevent misuse
- Invalid states cannot be represented
- All validation returns ParseResult
- Tests verify compile-time safety

---

### Phase 5: Improve Ergonomics (Additive)
**Estimated Effort: 2-3 days**

1. Implement property delegation for flags (Section 3.1)
2. Add common evaluable builders (Section 9.2)
3. Add Result extension functions (Section 7.2)
4. Create explicit registry factories (Section 10.1)
5. Add convenience methods based on user feedback

**Success Criteria:**
- Less boilerplate in common cases
- More patterns for custom evaluation
- Better error handling ergonomics
- Clear registry creation for testing

---

### Phase 6: Deprecate Old APIs (Breaking)
**Estimated Effort: 1 day**

1. Mark old entry points `@Deprecated`
2. Set deprecation level to WARNING
3. Provide clear migration paths in messages
4. Update documentation to promote new API only
5. Announce deprecation timeline

**Success Criteria:**
- All old APIs marked deprecated
- Clear migration instructions
- Timeline communicated (e.g., 3 months to removal)

---

### Phase 7: Remove Deprecated APIs (Breaking - Major Version)
**Estimated Effort: 1-2 days**

1. Remove all deprecated types and functions
2. Remove type aliases
3. Bump major version (2.0.0)
4. Final API surface audit
5. Comprehensive documentation review

**Success Criteria:**
- Clean API surface
- No deprecated code
- Full documentation
- Major version released

---

## 12. Final Target API Surface

### Public Packages (2 total)

#### `io.amichne.konditional`

**Main Entry Point:**
- `Konditional` object (1)

**Feature Types (6):**
- `BooleanFeature<C>`
- `StringFeature<C>`
- `IntFeature<C>`
- `DoubleFeature<C>`
- `JsonObjectFeature<T, C>`
- `CustomFeature<T, P, C>`

**Module Organization (1):**
- `Module` interface

**Configuration (3):**
- `Konfig` class
- `KonfigPatch` class
- `FlagRegistry` interface

**Context & Targeting (7):**
- `Context` interface
- `AppLocale` enum
- `Platform` enum
- `Version` data class
- `Rollout` value class
- `StableId` sealed interface
- Extension functions for evaluation

**Result Types (3):**
- `ParseResult<T>` sealed class
- `EvaluationResult<T>` sealed class
- `ParseError` sealed interface

**DSL Scopes (5):**
- `ConfigScope` sealed interface
- `ModuleScope` sealed interface
- `FlagScope<S, T, C>` sealed interface
- `RuleScope<C>` sealed interface
- `VersionRangeScope` sealed interface

**Total: ~27 public types**

#### `io.amichne.konditional.rules`

**Custom Evaluation (2):**
- `SimpleEvaluable<C>` fun interface
- `CustomEvaluable<C>` abstract class

**Total: 2 public types**

### Internal Package (Everything Else Hidden)
- `io.amichne.konditional.internal.*` - All implementation details

---

## 13. Success Metrics

After refactoring, the API must achieve:

### Compile-Time Safety
- [ ] Zero runtime type errors possible in production (all caught at compile time)
- [ ] Invalid rule combinations impossible to construct
- [ ] Version ranges validated at construction
- [ ] Context type mismatches caught at compile time
- [ ] All factory methods return `ParseResult` when validation can fail

### API Surface
- [ ] Reduced from ~60 to ~29 public types (52% reduction)
- [ ] Single entry point (`Konditional` object) for all operations
- [ ] No internal types visible in public API
- [ ] All implementation details hidden

### Clarity
- [ ] Type names clearly indicate purpose (no ambiguity)
- [ ] DSL scopes prevent calling wrong functions
- [ ] Error messages include actionable suggestions
- [ ] Documentation covers all public types with examples

### Testability
- [ ] Easy to create test registries
- [ ] Easy to mock/stub for testing
- [ ] Clear boundaries for unit testing each component

### Performance
- [ ] Zero-cost abstractions (no runtime overhead from refactoring)
- [ ] Inline functions where appropriate
- [ ] No reflection in hot paths

---

## 14. Key Decision Points Requiring Input

Before starting implementation, decide:

1. **Flag Definition Pattern:**
    - Option A: Keep enum delegation pattern as-is
    - Option B: Add property delegation via `FlagModule` abstract class
    - Option C: Support both patterns
    - **Recommendation:** Option C (both)

2. **Type Renaming Aggressiveness:**
    - Option A: Rename all types per Section 4 table
    - Option B: Rename only most confusing types (`Conditional`, `OfJsonObject`, `OfCustom`)
    - Option C: Keep all current names
    - **Recommendation:** Option A (rename all for clarity)

3. **DSL Scope Strictness:**
    - Option A: Sealed interfaces (strictest, most compile-time safety)
    - Option B: Abstract classes with internal constructors
    - Option C: Keep current open classes with `@DslMarker`
    - **Recommendation:** Option A (sealed interfaces)

4. **Rule Builder State Machine:**
    - Option A: Implement full state machine (Section 3.5)
    - Option B: Keep current mutable builder with validation at build time
    - **Recommendation:** Option B (state machine adds complexity without proven value)

5. **Serialization API:**
    - Option A: Extension functions only (hide SnapshotSerializer completely)
    - Option B: Extension functions + keep SnapshotSerializer public for advanced use
    - **Recommendation:** Option A (extension functions only)

6. **Breaking Change Timeline:**
    - Option A: Fast deprecation (1 month warning, then remove)
    - Option B: Standard deprecation (3 months warning, then remove)
    - Option C: Long deprecation (6+ months warning)
    - **Recommendation:** Option B (3 months is standard in Kotlin ecosystem)

---

## 15. Implementation Notes

### Constraints
- Maintain binary compatibility where possible using `@PublishedApi internal`
- Use inline functions to avoid boxing/unboxing of primitives
- Ensure all public APIs have comprehensive KDoc
- Add `@Since` annotations for new APIs
- Use `@RequiresOptIn` for experimental features

### Testing Strategy
- **Unit tests:** Every public function and class
- **Integration tests:** Complete workflows (define → configure → evaluate)
- **Compile-time tests:** Use compile-only test modules to verify type errors where expected
- **Serialization tests:** Round-trip serialization for all configuration types

### Documentation
- Package-level documentation (`package-info.kt`) for all public packages
- Comprehensive KDoc for all public types and functions
- "Getting Started" guide showing typical workflows
- Advanced usage guide for custom evaluators
- Architecture decision records (ADRs) for major design choices

---

## 16. Summary

This refactoring will transform Konditional from a well-architected library with some exposure issues into an **iron-clad, minimally exposed, maximally effective** feature flag framework.

**Key Changes:**
1. **Single entry point** via `Konditional` object
2. **Hide all internal types** (~60 → ~29 public types)
3. **Strengthen compile-time guarantees** via sealed scopes, smart constructors, validation
4. **Improve clarity** via type renames and better error messages
5. **Simplify serialization** via extension functions
6. **Make registry explicit** with clear factory methods

**Result:**
- **Impossible to misuse** - Invalid states cannot be represented
- **Clear semantics** - Types and functions have obvious meanings
- **Minimal surface** - Only essential types exposed
- **Maximum safety** - All invariants enforced at compile time
- **Easy to test** - Clear boundaries and test utilities
- **Zero runtime surprises** - All errors caught at compile time or returned as `ParseResult`

**Ready to implement with full breaking change authorization.**
