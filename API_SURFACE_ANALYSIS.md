# Konditional API Surface Analysis
**Branch:** `amichn/module-support`
**Date:** 2025-11-11

---

## Executive Summary

The Konditional library currently exposes a well-architected API with strong type-safety guarantees via the witness pattern. However, there are significant opportunities to minimize the API surface, improve clarity, and create a single-plane API that hides all internal machinations from users.

**Current State:** ~60 public types/interfaces exposed across 9 packages
**Recommended Target:** ~15-20 types in 3-4 packages with a single primary entry point

---

## 1. Current API Surface Inventory

### 1.1 Public Packages & Types

#### **Core Types** (`io.amichne.konditional.core`)
- `Conditional<S, T, C>` - Sealed interface (3 variants)
- `BooleanFeature<C>`, `StringFeature<C>`, `IntFeature<C>`, `DoubleFeature<C>` - Primitive features
- `FeatureFlag<S, T, C>` - Sealed class (evaluation abstraction)
- `FlagRegistry` - Interface with companion object delegation
- `Module` - Interface for organizing flags
- `FlagDefinition<S, T, C>` - **⚠️ Internal class in public package**

#### **Builders** (`io.amichne.konditional.builders`)
- `ConfigBuilder` - Main DSL entry point
- `ModuleBuilder` - Module configuration
- `FlagBuilder<S, T, C>` - Flag configuration
- `RuleBuilder<C>` - Rule construction
- `PatchBuilder` - Patch construction
- `VersionRangeBuilder` - Version range DSL

#### **Context Types** (`io.amichne.konditional.context`)
- `Context` - Interface with factory
- `AppLocale` - Enum (4 values)
- `Platform` - Enum (3 values)
- `Version` - Data class
- `Rollout` - Value class
- Extension functions: `evaluate()` (2 variants)

#### **Configuration Instances** (`io.amichne.konditional.core.instance`)
- `Konfig` - Immutable configuration snapshot
- `KonfigPatch` - Incremental update
- `ModuleConfig` - Module's flag definitions

#### **Rules** (`io.amichne.konditional.rules`)
- `Rule<C>` - Composable rule implementation
- `Evaluable<C>` - Abstract evaluation logic
- `BaseEvaluable<C>` - Standard targeting
- `ConditionalValue<S, T, C>` - Rule + value pair
- `VersionRange` - Sealed class (4 variants)

#### **Result Types** (`io.amichne.konditional.core.result`)
- `ParseResult<T>` - Sealed class (2 variants)
- `EvaluationResult<S>` - Sealed class (3 variants)
- `ParseError` - Sealed interface (10 variants)
- Extension functions: `getOrThrow()`, `getOrNull()`, etc.

#### **Type Safety** (`io.amichne.konditional.core.types`)
- `EncodableValue<T>` - Sealed interface (6 variants)
- `EncodableEvidence<T>` - Sealed interface (4 singletons)
- Factory methods for custom/JSON encodings

#### **Serialization** (`io.amichne.konditional.serialization`)
- `SnapshotSerializer` - Singleton with 8 methods
- `SerializableSnapshot`, `SerializablePatch`, `SerializableFlag`, `SerializableRule` - DTOs
- `FlagValue` - Polymorphic value representation
- Custom Moshi adapters (implementation details)

#### **IDs** (`io.amichne.konditional.core.id`)
- `StableId` - Sealed interface with factory
- `HexId` - Internal type representation

---

## 2. Visibility & Exposure Analysis

### 2.1 **Critical Issues** 🔴

1. **`FlagDefinition` Location**
   - **File:** `src/main/kotlin/io/amichne/konditional/core/internal/FlagDefinition.kt`
   - **Issue:** Marked `internal` but lives in non-internal package path
   - **Impact:** May be accessible via reflection; confusing organization
   - **Fix:** Move to truly internal package or use better encapsulation

2. **`FeatureFlag` Public Abstract Class**
   - **File:** `src/main/kotlin/io/amichne/konditional/core/FeatureFlag.kt`
   - **Issue:** Public sealed class with internal `evaluate()` method
   - **Impact:** Users see the type but can't instantiate or evaluate directly
   - **Current State:**
     ```kotlin
     sealed class FeatureFlag<S, T, C>(
         val defaultValue: T,
         val isActive: Boolean,
         val conditional: Conditional<S, T, C>,
         val values: List<ConditionalValue<S, T, C>>,
         val salt: String = "v1",
     ) {
         internal abstract fun evaluate(context: C): T
     }
     ```
   - **Question:** Do users need to see this type at all? Evaluation happens via `Context.evaluate()` extension

3. **Serialization Models Exposure**
   - **Package:** `io.amichne.konditional.serialization.models`
   - **Issue:** `SerializableSnapshot`, `SerializablePatch`, `FlagValue`, etc. are all public
   - **Impact:** Users see internal serialization DTOs that are only used by `SnapshotSerializer`
   - **Recommendation:** Make package-private or move to internal package

4. **Builder Constructors**
   - **Issue:** All builders have `private` constructors, which is good
   - **But:** The builder classes themselves are public
   - **Impact:** Users see implementation details of the DSL
   - **Alternative:** Could hide builders entirely behind lambda receivers

### 2.2 **Moderate Issues** 🟡

1. **`ConditionalValue` Exposure**
   - **File:** `src/main/kotlin/io/amichne/konditional/rules/ConditionalValue.kt`
   - **Issue:** Public data class that's only used internally in `FeatureFlag.values`
   - **Reason for Exposure:** Appears in `FeatureFlag.values: List<ConditionalValue<S, T, C>>`
   - **Fix:** Hide this type or make it opaque

2. **Multiple Factory Entry Points**
   - **Issue:** Factory methods scattered across companion objects:
     - `Conditional.jsonObject()`, `Conditional.custom()`
     - `BooleanFeature` via `boolean()` top-level function
     - `Context()` invoke operator
     - `StableId.of()`
     - `Rollout.of()`
   - **Impact:** Discoverability issues; no single entry point
   - **Recommendation:** Consolidate behind a single facade

3. **`EncodableValue` Implementation Variants**
   - **Package:** `io.amichne.konditional.core.types`
   - **Issue:** 6 public implementations of `EncodableValue` sealed interface
   - **Current State:**
     ```kotlin
     sealed interface EncodableValue<T> {
         value class BooleanEncodeable(val value: Boolean)
         value class StringEncodeable(val value: String)
         value class IntEncodeable(val value: Int)
         value class DecimalEncodeable(val value: Double)
         data class JsonObjectEncodeable<T>(...)
         data class CustomEncodeable<T, P>(...)
     }
     ```
   - **Question:** Do users ever construct these directly, or only via the witness pattern?
   - **Recommendation:** If only via witness pattern, hide completely

4. **`BaseEvaluable` Exposure**
   - **File:** `src/main/kotlin/io/amichne/konditional/rules/evaluable/BaseEvaluable.kt`
   - **Issue:** Public class that's only used as a composed field in `Rule`
   - **Users Interface:** Via `RuleBuilder` DSL, not directly
   - **Recommendation:** Mark `internal`

### 2.3 **Minor Issues** 🟢

1. **`FeatureFlagDsl` Annotation**
   - **File:** `src/main/kotlin/io/amichne/konditional/core/FeatureFlagDsl.kt`
   - **Issue:** Public `@DslMarker` annotation
   - **Impact:** Low; idiomatic Kotlin but adds to public surface
   - **Recommendation:** Keep as-is (standard practice)

2. **Internal Implementation Classes Properly Hidden**
   - `SingletonFlagRegistry` - Marked `internal` ✅
   - `BooleanFeatureImpl`, `StringFeatureImpl`, etc. - `@PublishedApi internal` ✅
   - Factory invoke operators - `@PublishedApi internal` ✅
   - **Status:** Well done; this is the pattern to follow elsewhere

3. **Extension Functions on Public Types**
   - `Context.evaluate()` - Good, primary evaluation entry point ✅
   - `Module.moduleNameFromEnum` - Useful extension ✅
   - `ParseResult.getOrThrow()`, etc. - Standard Kotlin result pattern ✅

---

## 3. Ergonomics Analysis

### 3.1 **Strengths** ✅

1. **DSL is Intuitive and Type-Safe**
   ```kotlin
   config {
       module(MyModules.FEATURES) {
           MyFlags.NEW_FEATURE with {
               default(false)
               rule {
                   platforms(Platform.IOS)
                   rollout = Rollout.of(50.0)
               } implies true
           }
       }
   }
   ```
   - Clear, declarative syntax
   - Type-safe builders prevent errors
   - Good scoping with `@FeatureFlagDsl`

2. **Witness Pattern Enforces Type Safety at Compile Time**
   ```kotlin
   enum class MyFlags(override val key: String) : BooleanFeature<Context> by boolean(key) {
       FEATURE_A("feature_a")
   }
   ```
   - Prevents invalid feature types
   - Enforces proper `EncodableValue` constraints
   - `@SubclassOptInRequired` prevents direct implementation

3. **Parse, Don't Validate Pattern**
   ```kotlin
   val result: ParseResult<Konfig> = SnapshotSerializer.deserialize(json)
   when (result) {
       is ParseResult.Success -> konfig = result.value
       is ParseResult.Failure -> handleError(result.error)
   }
   ```
   - Structured error types via `ParseError` sealed interface
   - No exceptions for expected failures
   - Type-safe error handling

4. **Module-Based Organization**
   ```kotlin
   enum class MyModules : Module {
       USER_FEATURES,
       PAYMENT_FEATURES
   }
   ```
   - Forces logical grouping
   - Type-safe module names via enums
   - Prevents orphaned flags

### 3.2 **Weaknesses** ⚠️

1. **Boilerplate in Flag Definitions**
   - **Current:**
     ```kotlin
     enum class MyFlags(override val key: String) : BooleanFeature<Context> by boolean(key) {
         FEATURE_A("feature_a"),
         FEATURE_B("feature_b")
     }
     ```
   - **Issue:** Key appears twice (enum name + string key)
   - **Alternative Pattern:**
     ```kotlin
     object MyFlags : FeatureModule<Context>() {
         val FEATURE_A by boolean()  // Derives key from property name
         val FEATURE_B by boolean()
     }
     ```
   - **Trade-off:** Current pattern is more explicit; alternative reduces duplication

2. **Understanding the Witness Pattern Requires Kotlin Knowledge**
   - **Issue:** `by boolean(key)` delegation is not obvious to newcomers
   - **Why It Exists:** Prevents direct implementation, enforces type constraints
   - **Mitigation:** Could provide better documentation and examples
   - **Alternative:** Could hide behind more conventional factory pattern, but loses type safety

3. **Multiple Concepts to Understand**
   - `Conditional` - The key/identifier for a flag
   - `FeatureFlag` - The definition with rules and default
   - `BooleanFeature` / `StringFeature` / etc. - Type-specific conditionals
   - `Module` - Grouping mechanism
   - **Issue:** Cognitive overhead; users must understand 4-5 concepts to define a flag
   - **Recommendation:** Better onboarding documentation; potentially consolidate types

4. **Registry Pattern is Opaque**
   - **Current:**
     ```kotlin
     val FlagRegistry.Companion : FlagRegistry by SingletonFlagRegistry
     ```
   - **Issue:** Delegation to singleton is clever but potentially confusing
   - **Users Must Know:** `FlagRegistry` is both an interface and a companion object
   - **Recommendation:** Document clearly or provide explicit singleton access

5. **Serialization API is Low-Level**
   - **Current:**
     ```kotlin
     SnapshotSerializer.default.serialize(konfig)
     val result = SnapshotSerializer.default.deserialize(json)
     ```
   - **Issue:** Users must deal with `SnapshotSerializer` directly
   - **Alternative:** Could provide high-level extension functions:
     ```kotlin
     konfig.toJson()
     Konfig.fromJson(json)
     ```

6. **Context Extension Evaluation is Discoverable but Non-Standard**
   - **Current:**
     ```kotlin
     val value: Boolean = context.evaluate(MyFlags.FEATURE_A)
     ```
   - **Good:** Natural syntax
   - **Issue:** Not obvious this is an extension function
   - **Alternative Syntax:**
     ```kotlin
     val value: Boolean = MyFlags.FEATURE_A.evaluate(context)
     // or
     val value: Boolean = FlagRegistry.evaluate(MyFlags.FEATURE_A, context)
     ```
   - **Trade-off:** Current syntax reads well but extension might be unexpected

---

## 4. Clarity Issues

### 4.1 **Type Naming and Semantics**

1. **`Conditional` vs `FeatureFlag` Distinction**
   - **`Conditional`**: The identifier/key for a flag (e.g., `MyFlags.FEATURE_A`)
   - **`FeatureFlag`**: The definition with rules, values, and default
   - **Issue:** Users might confuse "conditional" as a rule or condition
   - **Alternative Names:**
     - `FlagKey` / `FlagDefinition`
     - `FlagIdentifier` / `Flag`
     - `Feature` / `FeatureConfiguration`
   - **Recommendation:** Consider renaming `Conditional` to something more explicit

2. **`OfJsonObject` vs `OfCustom` Naming**
   - **Current:**
     ```kotlin
     interface OfJsonObject<T, C> : Conditional<...>
     interface OfCustom<T, P, C> : Conditional<...>
     ```
   - **Issue:** "Of" prefix is unconventional; not immediately clear what these represent
   - **Alternative:**
     ```kotlin
     interface JsonObjectConditional<T, C>
     interface CustomConditional<T, P, C>
     // or
     interface ComplexValueFeature<T, C>
     interface WrapperFeature<T, P, C>
     ```

3. **`EncodableValue` vs `EncodableEvidence` Confusion**
   - **`EncodableValue<T>`**: The wrapper for actual values (e.g., `BooleanEncodeable`)
   - **`EncodableEvidence<T>`**: The witness for type-checking at compile time
   - **Issue:** Similar names, different purposes; easy to confuse
   - **Recommendation:** Rename `EncodableEvidence` to `TypeWitness` or hide entirely

4. **`Konfig` vs `KonfigPatch` Naming**
   - **Issue:** "K" spelling is cute but potentially confusing
   - **Alternative:** `Configuration` / `ConfigurationPatch` or `Config` / `ConfigPatch`
   - **Trade-off:** "Konfig" avoids naming conflicts and is memorable
   - **Recommendation:** Keep current naming but ensure consistency

### 4.2 **Documentation Gaps**

1. **Package-Level Documentation Missing**
   - **Issue:** No `package-info.kt` files explaining each package's purpose
   - **Impact:** Users must piece together architecture from individual types
   - **Recommendation:** Add package documentation with:
     - Package purpose
     - Key types and their relationships
     - Usage examples

2. **Witness Pattern Not Explained**
   - **Issue:** `@SubclassOptInRequired` and delegation pattern is used but not documented
   - **Impact:** Users may try to implement interfaces directly and fail
   - **Recommendation:** Add explicit documentation to each primitive feature interface

3. **Module Pattern Not Explained**
   - **Issue:** Why modules are required (vs. flat flag registration) is not documented
   - **Recommendation:** Document the design decision and benefits

### 4.3 **Inconsistent Visibility Patterns**

1. **Mixed Use of `internal` vs Package Structure**
   - **Some internals:** Marked `internal` (e.g., `SingletonFlagRegistry`)
   - **Other internals:** In `internal` package (e.g., `FlagDefinition`)
   - **Issue:** Two patterns for hiding implementation
   - **Recommendation:** Standardize on one approach (prefer package structure)

2. **Factory Methods in Multiple Locations**
   - **Companion objects:** `Conditional.jsonObject()`, `StableId.of()`
   - **Top-level functions:** `boolean()`, `string()`, `int()`, `double()`
   - **Invoke operators:** `Context()`, `FeatureFlag()`
   - **Issue:** No clear pattern for where factories live
   - **Recommendation:** Consolidate or document rationale

---

## 5. Single-Plane API Proposal

### 5.1 **Goal: Minimal, Unified Entry Point**

**Objective:** Hide all internal machinations behind a single facade, exposing only what users need to:
1. Define flags
2. Configure flags
3. Evaluate flags
4. Serialize/deserialize configurations

### 5.2 **Proposed Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                    PUBLIC API SURFACE                        │
│  (Single entry point: io.amichne.konditional.Konditional)   │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  1. Flag Definition DSL                                      │
│     - boolean(), string(), int(), double()                   │
│     - jsonObject(), custom()                                 │
│     - module()                                               │
│                                                               │
│  2. Configuration DSL                                        │
│     - config { ... }                                         │
│     - patch { ... }                                          │
│                                                               │
│  3. Evaluation API                                           │
│     - context.evaluate(flag)                                 │
│     - context.evaluateAll()                                  │
│                                                               │
│  4. Serialization API                                        │
│     - konfig.toJson()                                        │
│     - Konfig.fromJson()                                      │
│                                                               │
│  5. Essential Types (minimized)                              │
│     - Context, AppLocale, Platform, Version                  │
│     - Module (for grouping)                                  │
│     - ParseResult, EvaluationResult (for error handling)     │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ (internal implementation)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   INTERNAL IMPLEMENTATION                    │
│         (Hidden from users; package-private)                 │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  - Conditional (interface and implementations)               │
│  - FeatureFlag (sealed class)                               │
│  - FlagDefinition (concrete implementation)                  │
│  - SingletonFlagRegistry                                     │
│  - EncodableValue variants                                   │
│  - EncodableEvidence witnesses                               │
│  - Builders (ConfigBuilder, ModuleBuilder, etc.)             │
│  - Rule evaluation (Rule, Evaluable, BaseEvaluable)          │
│  - ConditionalValue                                          │
│  - Serialization models and adapters                         │
│  - All internal machinery                                    │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 **Proposed Public API** (Target: ~15 Types)

#### **Package: `io.amichne.konditional`** (Main Entry Point)

```kotlin
// ===== Primary Entry Point =====
object Konditional {
    // Flag definition factories
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

    // Serialization
    fun Konfig.toJson(): String
    fun fromJson(json: String): ParseResult<Konfig>

    // Registry access (optional; could be hidden)
    val registry: FlagRegistry
}

// ===== Essential Types =====

// Flag definition (returned by factories; users use via delegation)
interface BooleanFeature<C : Context>
interface StringFeature<C : Context>
interface IntFeature<C : Context>
interface DoubleFeature<C : Context>
interface JsonObjectFeature<T : Any, C : Context>
interface CustomFeature<T : Any, P : Any, C : Context>

// Module organization
interface Module {
    val moduleName: String
}

// Configuration instances
class Konfig internal constructor(...)
class KonfigPatch internal constructor(...)

// ===== Context & Targeting =====

interface Context {
    val locale: AppLocale
    val platform: Platform
    val appVersion: Version
    val stableId: StableId
}

fun Context(locale: AppLocale, platform: Platform, appVersion: Version, stableId: StableId): Context

enum class AppLocale { EN_US, ES_US, EN_CA, HI_IN }
enum class Platform { IOS, ANDROID, WEB }
data class Version(val major: Int, val minor: Int, val patch: Int)

interface StableId
fun StableId(id: String): StableId

@JvmInline value class Rollout internal constructor(val value: Double)
fun Rollout(value: Double): Rollout

// ===== Evaluation =====

fun <T : Any> Context.evaluate(feature: BooleanFeature<Context>): Boolean
fun <T : Any> Context.evaluate(feature: StringFeature<Context>): String
// ... (overloads for each feature type)

fun Context.evaluateAll(registry: FlagRegistry = FlagRegistry): Map<String, Any>

// ===== Result Types =====

sealed class ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>()
    data class Failure(val error: ParseError) : ParseResult<Nothing>()
}

sealed interface ParseError {
    // Minimal set of error types
    data class InvalidFormat(val reason: String) : ParseError
    data class NotFound(val key: String) : ParseError
    data class TypeMismatch(val expected: String, val actual: String) : ParseError
}

sealed class EvaluationResult<out T> {
    data class Success<T>(val value: T) : EvaluationResult<T>()
    data class FlagNotFound(val key: String) : EvaluationResult<Nothing>()
    data class Error(val key: String, val error: Throwable) : EvaluationResult<Nothing>()
}
```

#### **Package: `io.amichne.konditional.rules`** (Rule Extension Point)

```kotlin
// For advanced users who need custom evaluation logic
abstract class Evaluable<C : Context> {
    abstract fun matches(context: C): Boolean
    open fun specificity(): Int = 0
}
```

**Total Public Types: ~15 (vs. ~60 currently)**

### 5.4 **Benefits of Single-Plane API**

1. **Reduced Surface Area**
   - From ~60 types to ~15 types
   - From 9 packages to 2 packages
   - Single entry point (`Konditional` object)

2. **Improved Discoverability**
   - All factories in one place
   - No hunting for companion objects
   - IDE autocomplete shows everything

3. **Hidden Implementation Details**
   - `Conditional` hidden (users never see it)
   - `FeatureFlag` hidden (evaluation via extensions)
   - `EncodableValue` hidden (witness pattern internal)
   - `ConditionalValue` hidden (DSL creates automatically)
   - All builders hidden behind DSL scopes

4. **Simplified Mental Model**
   ```
   Define flags → Configure flags → Evaluate flags
        ↓               ↓                ↓
   boolean()        config { }      context.evaluate()
   ```

5. **Backward Compatibility via Type Aliases**
   - Could provide type aliases for migration:
     ```kotlin
     @Deprecated("Use Konditional.boolean() instead")
     typealias BooleanConditional<C> = BooleanFeature<C>
     ```

6. **Better Testability**
   - Clear separation between public API and internals
   - Can evolve internals without breaking users
   - Easier to mock/stub for testing

### 5.5 **Implementation Strategy**

#### **Phase 1: Refactor Package Structure** (No API Changes)
1. Create `io.amichne.konditional.internal` package
2. Move all internal types to internal package:
   - `FlagDefinition`
   - `SingletonFlagRegistry`
   - `ConditionalValue`
   - `BaseEvaluable`
   - Serialization models
   - Builder implementations
3. Mark types `internal` and use package-private visibility
4. Ensure tests still pass

#### **Phase 2: Create Facade** (Additive Changes)
1. Create `Konditional` object as single entry point
2. Delegate to existing implementations
3. Add convenience extensions:
   - `Konfig.toJson()` → `SnapshotSerializer.serialize()`
   - `Konfig.fromJson()` → `SnapshotSerializer.deserialize()`
4. Add type aliases for compatibility
5. Update documentation to promote new entry point

#### **Phase 3: Deprecate Old Entry Points** (Deprecation Warnings)
1. Mark old factories as `@Deprecated`
2. Mark exposed builder classes as `@Deprecated`
3. Provide clear migration paths in deprecation messages
4. Update examples to use new API

#### **Phase 4: Remove Deprecated APIs** (Breaking Changes)
1. Remove deprecated types from public API
2. Move to internal packages
3. Major version bump (e.g., 1.0.0 → 2.0.0)

### 5.6 **Example Migration**

#### **Before (Current API)**

```kotlin
import io.amichne.konditional.core.BooleanFeature
import io.amichne.konditional.core.Module
import io.amichne.konditional.core.boolean
import io.amichne.konditional.builders.ConfigBuilder
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.evaluate

enum class MyFlags(override val key: String) : BooleanFeature<Context> by boolean(key) {
    FEATURE_A("feature_a")
}

enum class MyModules : Module {
    FEATURES
}

ConfigBuilder.config {
    module(MyModules.FEATURES) {
        MyFlags.FEATURE_A with {
            default(false)
        }
    }
}

val value: Boolean = context.evaluate(MyFlags.FEATURE_A)
```

#### **After (Single-Plane API)**

```kotlin
import io.amichne.konditional.Konditional
import io.amichne.konditional.Module
import io.amichne.konditional.Context

enum class MyFlags(override val key: String) : BooleanFeature<Context> by Konditional.boolean(key) {
    FEATURE_A("feature_a")
}

enum class MyModules : Module {
    FEATURES
}

Konditional.config {
    module(MyModules.FEATURES) {
        MyFlags.FEATURE_A with {
            default(false)
        }
    }
}

val value: Boolean = context.evaluate(MyFlags.FEATURE_A)
```

**Changes:**
- Single import: `io.amichne.konditional.Konditional`
- Factories via `Konditional` object
- DSL via `Konditional.config { }`
- Evaluation unchanged (extension function)

---

## 6. Specific Recommendations by Priority

### 6.1 **High Priority (Do Now)** 🔴

1. **Move `FlagDefinition` to Proper Internal Package**
   - **Action:** Move to `io.amichne.konditional.internal.FlagDefinition`
   - **Rationale:** Currently in wrong location; may be accessible via reflection
   - **Effort:** Low (1 hour)

2. **Hide Serialization Models**
   - **Action:** Move `SerializableSnapshot`, `SerializablePatch`, etc. to `internal` package
   - **Rationale:** Implementation details users should never see
   - **Effort:** Low (1-2 hours)

3. **Make `BaseEvaluable` Internal**
   - **Action:** Mark `internal` or move to internal package
   - **Rationale:** Users never construct this directly; only via DSL
   - **Effort:** Low (30 minutes)

4. **Hide `ConditionalValue`**
   - **Action:** Mark `internal`
   - **Rationale:** Only used in `FeatureFlag.values`, which could be opaque
   - **Challenge:** Must change `FeatureFlag` signature to hide type
   - **Alternative:** Make `FeatureFlag.values` return `List<*>` or hide entirely
   - **Effort:** Medium (2-3 hours)

5. **Add Package Documentation**
   - **Action:** Create `package-info.kt` for each public package
   - **Content:** Package purpose, key types, usage examples
   - **Effort:** Medium (4-6 hours)

### 6.2 **Medium Priority (Do Next)** 🟡

1. **Create `Konditional` Facade Object**
   - **Action:** Implement single entry point as proposed above
   - **Rationale:** Improve discoverability and consolidate API
   - **Effort:** High (1-2 days)
   - **Impact:** Major UX improvement

2. **Hide `EncodableValue` Implementations**
   - **Action:** Move all `EncodableValue` variants to internal package
   - **Rationale:** Users never construct these directly; witness pattern creates them
   - **Effort:** Medium (3-4 hours)
   - **Challenge:** May need to expose minimal type for advanced users

3. **Add Convenience Serialization Extensions**
   - **Action:**
     ```kotlin
     fun Konfig.toJson(): String
     fun Konfig.Companion.fromJson(json: String): ParseResult<Konfig>
     ```
   - **Rationale:** Hide `SnapshotSerializer` complexity
   - **Effort:** Low (1 hour)

4. **Improve Error Messages**
   - **Action:** Audit all `require()` and `error()` calls for clarity
   - **Add:** Suggestions for how to fix errors
   - **Example:**
     ```kotlin
     require(moduleName !in modules) {
         "Duplicate module '$moduleName'. " +
         "Module names must be unique. " +
         "Did you call module($moduleName) twice?"
     }
     ```
   - **Effort:** Medium (3-4 hours)

5. **Consolidate Factory Patterns**
   - **Action:** Decide on standard pattern for factories:
     - Companion object `invoke` operator? (e.g., `Context()`)
     - Companion object named function? (e.g., `StableId.of()`)
     - Top-level function? (e.g., `boolean()`)
   - **Recommendation:** Use `Konditional` object for all (single-plane API)
   - **Effort:** Low (2 hours for planning; 4-6 hours for implementation)

### 6.3 **Low Priority (Future Enhancements)** 🟢

1. **Consider Renaming `Conditional`**
   - **Options:** `FlagKey`, `Feature`, `FlagIdentifier`
   - **Rationale:** "Conditional" might confuse users (sounds like a rule)
   - **Effort:** High (major refactor; breaking change)
   - **Defer:** Until major version bump

2. **Reduce Boilerplate in Flag Definitions**
   - **Current:**
     ```kotlin
     enum class MyFlags(override val key: String) : BooleanFeature<Context> by boolean(key) {
         FEATURE_A("feature_a")
     }
     ```
   - **Alternative:**
     ```kotlin
     object MyFlags : FeatureModule<Context>() {
         val FEATURE_A by boolean()  // Key derived from property name
     }
     ```
   - **Trade-off:** More magic vs. less boilerplate
   - **Effort:** High (design new pattern; breaking change)

3. **Hide `FeatureFlag` Class Entirely**
   - **Current:** Public sealed class with internal evaluate
   - **Alternative:** Make internal; users never see the type
   - **Challenge:** How do users specify flag definitions in DSL?
   - **Effort:** High (may require major refactor)

4. **Explore Alternative to Witness Pattern**
   - **Current:** Delegation pattern with `by boolean(key)`
   - **Alternative:** Could use type-safe builders or sealed classes
   - **Trade-off:** Current pattern is idiomatic Kotlin but requires understanding
   - **Effort:** Very High (fundamental design change)

5. **Add Metrics/Telemetry Hooks**
   - **Action:** Allow users to observe flag evaluations for analytics
   - **API:**
     ```kotlin
     Konditional.onEvaluate { flag, context, value ->
         analytics.track("feature_flag_evaluated", ...)
     }
     ```
   - **Effort:** Medium (2-3 days)

---

## 7. API Surface Comparison

### 7.1 **Current API Surface**

```
io.amichne.konditional.core/
├── Conditional (+ 2 nested interfaces)
├── BooleanFeature, StringFeature, IntFeature, DoubleFeature
├── FeatureFlag
├── FlagRegistry
├── Module
└── internal/
    ├── FlagDefinition ⚠️ (in wrong location)
    └── SingletonFlagRegistry

io.amichne.konditional.builders/
├── ConfigBuilder
├── ModuleBuilder
├── FlagBuilder
├── RuleBuilder
├── PatchBuilder
└── VersionRangeBuilder

io.amichne.konditional.context/
├── Context
├── AppLocale, Platform, Version, Rollout
└── Extension functions

io.amichne.konditional.core.instance/
├── Konfig
├── KonfigPatch
└── ModuleConfig

io.amichne.konditional.rules/
├── Rule
├── Evaluable
├── BaseEvaluable
├── ConditionalValue ⚠️
└── versions/VersionRange

io.amichne.konditional.core.result/
├── ParseResult
├── EvaluationResult
└── ParseError

io.amichne.konditional.core.types/
├── EncodableValue (+ 6 implementations) ⚠️
└── EncodableEvidence (+ 4 singletons) ⚠️

io.amichne.konditional.serialization/
├── SnapshotSerializer
├── models/ ⚠️ (all public DTOs)
└── adapters/ ⚠️ (implementation details)

io.amichne.konditional.core.id/
├── StableId
└── HexId ⚠️

Total: ~60 public types across 9 packages
⚠️ = Should be internal
```

### 7.2 **Proposed API Surface** (Single-Plane)

```
io.amichne.konditional/
├── Konditional (single entry point)
├── BooleanFeature, StringFeature, IntFeature, DoubleFeature
├── JsonObjectFeature, CustomFeature
├── Module
├── Konfig, KonfigPatch
├── Context, AppLocale, Platform, Version, StableId, Rollout
├── ParseResult, EvaluationResult, ParseError
└── Extension functions

io.amichne.konditional.rules/
└── Evaluable (for advanced custom rules)

io.amichne.konditional.internal/
└── [Everything else hidden]

Total: ~15 public types across 2 packages
```

**Reduction: 60 → 15 types (75% reduction)**

---

## 8. Concrete Next Steps

### Immediate Actions (This Week)

1. ✅ **Audit Current Visibility**
   - [x] Document all public types
   - [x] Identify mis-placed internals
   - [x] Map dependencies between types

2. **Create `internal` Package Structure**
   ```
   src/main/kotlin/io/amichne/konditional/internal/
   ├── core/
   │   ├── FlagDefinition.kt
   │   ├── SingletonFlagRegistry.kt
   │   └── ConditionalImpl.kt
   ├── builders/
   │   └── [All builder implementations]
   ├── rules/
   │   ├── ConditionalValue.kt
   │   └── BaseEvaluableImpl.kt
   ├── types/
   │   ├── EncodableValueImpl.kt
   │   └── EncodableEvidenceImpl.kt
   └── serialization/
       ├── SnapshotSerializerImpl.kt
       ├── models/
       └── adapters/
   ```

3. **Move Mis-Placed Internals**
   - Move `FlagDefinition` to `internal/core/`
   - Move serialization models to `internal/serialization/models/`
   - Move adapters to `internal/serialization/adapters/`

4. **Mark Types Internal**
   - `ConditionalValue` → `internal`
   - `BaseEvaluable` → `internal`
   - All `EncodableValue` variants → `internal`
   - All `EncodableEvidence` singletons → `internal`

5. **Add Package Documentation**
   - Write `package-info.kt` for each public package
   - Include purpose, key types, examples

### Short-Term (Next 2 Weeks)

1. **Design `Konditional` Facade**
   - Sketch API surface
   - Prototype implementation
   - Get feedback

2. **Implement Convenience Extensions**
   - `Konfig.toJson()`
   - `Konfig.fromJson()`
   - Simplify serialization API

3. **Improve Error Messages**
   - Audit all error messages
   - Add suggestions for fixes
   - Improve require/check messages

### Mid-Term (Next Month)

1. **Implement Single-Plane API**
   - Create `Konditional` object
   - Delegate to existing implementations
   - Maintain backward compatibility

2. **Write Migration Guide**
   - Document old vs. new API
   - Provide code examples
   - Explain benefits

3. **Update All Examples**
   - Rewrite README examples
   - Update tests to use new API
   - Create before/after comparisons

### Long-Term (Next Quarter)

1. **Deprecate Old Entry Points**
   - Add `@Deprecated` annotations
   - Provide clear migration paths
   - Set timeline for removal

2. **Gather User Feedback**
   - Create survey for API usability
   - Conduct user interviews
   - Iterate based on feedback

3. **Plan Major Version**
   - Remove deprecated APIs
   - Finalize single-plane surface
   - Publish 2.0.0

---

## 9. Risk Assessment

### 9.1 **Risks of API Changes**

| Risk | Severity | Mitigation |
|------|----------|------------|
| Breaking existing users | High | Use deprecation cycle; maintain backward compat |
| Confusion during transition | Medium | Clear documentation; migration guide |
| Type safety weakened | Low | Maintain witness pattern internally |
| Performance regression | Low | Facade is zero-cost; no runtime overhead |
| Increased maintenance burden | Medium | Consolidate reduces maintenance over time |

### 9.2 **Risks of Not Changing**

| Risk | Severity | Impact |
|------|----------|--------|
| Poor discoverability | High | Users struggle to find features |
| Exposure of internals | High | Can't evolve without breaking users |
| Cognitive overload | Medium | High learning curve discourages adoption |
| Inconsistent patterns | Medium | Confusion about how to use library |
| Difficult maintenance | Medium | Large surface area hard to maintain |

**Recommendation:** Proceed with changes. Benefits outweigh risks, especially if done incrementally with deprecation cycle.

---

## 10. Conclusion

### Key Findings

1. **Current API is well-architected** with strong type safety, but **exposes too many internal details** (~60 types across 9 packages)

2. **Several mis-placed internal types** (FlagDefinition, serialization models) should be hidden

3. **Multiple entry points** (companion objects, top-level functions, factories) reduce discoverability

4. **Witness pattern is effective** for type safety but adds cognitive overhead for newcomers

5. **DSL is intuitive and well-scoped** but builders leak into public API

### Recommendations Summary

#### **High Priority:**
- Move `FlagDefinition` to proper internal package
- Hide serialization models and adapters
- Make `BaseEvaluable` and `ConditionalValue` internal
- Add comprehensive package documentation

#### **Medium Priority:**
- Create `Konditional` facade object as single entry point
- Hide `EncodableValue` implementations
- Add convenience serialization extensions
- Improve error messages with suggestions

#### **Low Priority:**
- Consider renaming `Conditional` to `FlagKey` or `Feature`
- Reduce boilerplate in flag definitions
- Explore hiding `FeatureFlag` class entirely
- Add metrics/telemetry hooks

### Target API Surface

**From:** ~60 types in 9 packages → **To:** ~15 types in 2 packages (75% reduction)

**Entry Point:** Single `Konditional` object for all factories and DSL

**Hidden:** All builders, internal implementations, serialization models, type witnesses

### Next Steps

1. Create `io.amichne.konditional.internal` package structure
2. Move mis-placed internals and mark types `internal`
3. Add package documentation
4. Design and prototype `Konditional` facade
5. Implement convenience extensions
6. Plan deprecation cycle for old APIs

---

**This analysis provides a roadmap for achieving an iron-clad, minimally exposed, maximally effective API with a single plane visible to users.**
