# Type System Architecture

## Type Parameter Evolution

### Historical (Pre-2025-12-06)
```kotlin
Feature<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>  // 4 parameters
FlagDefinition<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>
```

### Current (Post-Simplification)
```kotlin
Feature<T : Any, M : Namespace>                      // 2 parameters ✅
FlagDefinition<T : Any, C : Context, M : Namespace>  // 3 parameters
```

## Type Parameter Meanings

### T : Any (Value Type)
**Purpose**: The actual value type the flag returns/stores

**Examples**:
- `Boolean` - for boolean flags
- `String` - for string flags
- `Int` - for integer flags
- `Double` - for decimal flags
- `E : Enum<E>` - for enum flags (specific enum type)
- `T : DataClassWithSchema` - for structured config

**Constraints**:
- Must be non-nullable (`T : Any`, not `T : Any?`)
- Must be encodable (enforced via `EncodableEvidence` internally)
- Must have a default value

### C : Context (Evaluation Context Type)
**Purpose**: The context type used for rule matching and evaluation

**Location**: Only appears in evaluation-related types:
- `FlagDefinition<T, C, M>` - Needs C to evaluate
- `Rule<C>` - Needs C to match
- `FlagScope<T, C, M>` - Needs C to define rules
- `FlagBuilder<T, C, M>` - Needs C to build rules

**Not in Feature**: Removed from `Feature` to allow polymorphic evaluation

**Base Type**:
```kotlin
interface Context {
    val locale: AppLocale
    val platform: Platform
    val appVersion: Version
    val stableId: StableId
}
```

**Custom Contexts**:
```kotlin
data class EnterpriseContext(
    override val locale: AppLocale,
    override val platform: Platform,
    override val appVersion: Version,
    override val stableId: StableId,
    val organizationId: String,
    val subscriptionTier: Tier
) : Context
```

**Polymorphism**: Any `C : Context` can evaluate any `Feature<T, M>`

### M : Namespace (Isolation Boundary)
**Purpose**: Phantom type providing compile-time namespace isolation

**Phantom Type**: Appears in type signature but has no runtime representation beyond `namespace.id`

**Benefits**:
1. Compile-time check: Cannot mix flags from different namespaces
2. Type-driven dispatch: Correct registry selected automatically
3. Team boundaries: Each namespace is independently versioned/deployed
4. Zero runtime cost: Just a `val namespace: M` reference

**Sealed Hierarchy**:
```kotlin
sealed class Namespace(val id: String, internal val registry: NamespaceRegistry)
├── Global
├── Authentication
├── Payments
├── Messaging
├── Search
└── Recommendations
```

**Usage**:
```kotlin
object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments)
//                                        ^^^^^^^^^^^^^^^^^^  Type parameter
//                                                            ^^^^^^^^^^^^^^^^^  Value
```

## Type Relationships

### Feature Hierarchy
```
Feature<T, M>                           // Base sealed interface
├── BooleanFeature<M>                   // Equivalent to Feature<Boolean, M>
│   └── BooleanFeatureImpl<M>           // Internal data class
├── StringFeature<M>                    // Equivalent to Feature<String, M>
│   └── StringFeatureImpl<M>
├── IntFeature<M>                       // Equivalent to Feature<Int, M>
│   └── IntFeatureImpl<M>
├── DoubleFeature<M>                    // Equivalent to Feature<Double, M>
│   └── DoubleFeatureImpl<M>
├── EnumFeature<E : Enum<E>, M>         // Equivalent to Feature<E, M>
│   └── EnumFeatureImpl<E, M>
└── DataClassFeature<T : DataClassWithSchema, M>  // Equivalent to Feature<T, M>
    └── DataClassFeatureImpl<T, M>
```

### Type Flow Through System

```
User Code:
  val DARK_MODE by boolean(default = false) { ... }
  Type: BooleanFeature<Namespace.Payments>

Property Delegation:
  ContainerFeaturePropertyDelegate<
    F = BooleanFeature<M>,
    T = Boolean,
    C = Context,           // From flagScope parameter
    M = Namespace.Payments // From container
  >

Feature Creation:
  BooleanFeature(name, namespace)
  Returns: BooleanFeature<Namespace.Payments>

Flag Building:
  FlagBuilder<Boolean, Context, Namespace.Payments>
  Returns: FlagDefinition<Boolean, Context, Namespace.Payments>

Registry Storage:
  Map<Feature<*, *>, FlagDefinition<*, *, *>>
  Actual: Map<BooleanFeature<Payments>, FlagDefinition<Boolean, Context, Payments>>

Evaluation:
  context.evaluateSafe(DARK_MODE)
  Returns: EvaluationResult<Boolean>
```

## Type Variance

### Covariance (out T)
```kotlin
sealed interface EvaluationResult<out S> {
    data class Success<S>(val value: S) : EvaluationResult<S>
    data class FlagNotFound(val key: String) : EvaluationResult<Nothing>
}
```
**Why**: Success produces S, error cases produce Nothing (bottom type)

### Contravariance (in C)
```kotlin
fun interface Evaluable<in C : Context> {
    fun matches(context: C): Boolean
}
```
**Why**: Evaluable consumes C, so more specific context works with less specific evaluable

### Invariance (T, M)
Most types are invariant:
```kotlin
Feature<T : Any, M : Namespace>  // Invariant in T and M
```
**Why**: Features both produce and consume T, namespaces are identity types

## Type Erasure Handling

### The Problem
```kotlin
val flags: Map<Feature<*, *>, FlagDefinition<*, *, *>>
```
At runtime, this is just `Map` - all type parameters erased

### The Solution: Safe Casting
```kotlin
@Suppress("UNCHECKED_CAST")
fun <T : Any, C : Context, M : Namespace> flag(
    key: Feature<T, M>
): FlagDefinition<T, C, M>? =
    configuration.flags[key] as? FlagDefinition<T, C, M>
```

**Safety Guarantee**:
1. Map is only populated via `updateDefinition(definition: FlagDefinition<T, C, M>)`
2. Definition always stored with key `definition.feature`
3. Therefore: `flags[feature]` has same types as `feature`
4. Cast is safe due to structural guarantee

### EncodableValue - Internal Type Evidence

**Location**: `src/main/kotlin/io/amichne/konditional/core/types/EncodableValue.kt`

```kotlin
sealed interface EncodableValue<T : Any> {
    val value: T
    val encoding: Encoding

    enum class Encoding {
        BOOLEAN, STRING, INTEGER, DECIMAL,
        ENUM, JSON_OBJECT, JSON_ARRAY, DATA_CLASS
    }
}
```

**Implementations**:
```kotlin
data class BooleanEncodeable(override val value: Boolean) : EncodableValue<Boolean>
data class StringEncodeable(override val value: String) : EncodableValue<String>
data class IntEncodeable(override val value: Int) : EncodableValue<Int>
data class DecimalEncodeable(override val value: Double) : EncodableValue<Double>
data class EnumEncodeable<E : Enum<E>>(override val value: E, ...) : EncodableValue<E>
data class DataClassEncodeable<T : DataClassWithSchema>(override val value: T, ...) : EncodableValue<T>
```

**Usage (Internal Only)**:
```kotlin
// Serialization knows how to encode based on sealed type
when (encodable) {
    is BooleanEncodeable -> json { "value" to encodable.value }
    is StringEncodeable -> json { "value" to encodable.value }
    // ... etc
}
```

**Evidence Pattern**:
```kotlin
inline fun <reified T : Any> of(
    value: T,
    evidence: EncodableEvidence<T> = EncodableEvidence.get()
): EncodableValue<T>
```

## Type Constraints

### Feature Constraints
```kotlin
sealed interface Feature<T : Any, M : Namespace>
```
- `T : Any` - No nullable feature values
- `M : Namespace` - Must be a namespace type

### FlagDefinition Constraints
```kotlin
data class FlagDefinition<T : Any, C : Context, M : Namespace>
```
- `T : Any` - No nullable values
- `C : Context` - Must be evaluable context
- `M : Namespace` - Must match feature's namespace

### Enum Constraints
```kotlin
sealed interface EnumFeature<E : Enum<E>, M : Namespace> : Feature<E, M>
```
- `E : Enum<E>` - Recursive bound ensures E is an enum type
- Enables `enumValues<E>()` and enum-specific operations

### DataClass Constraints
```kotlin
sealed interface DataClassFeature<T : DataClassWithSchema, M : Namespace> : Feature<T, M>
```
- `T : DataClassWithSchema` - Must have schema for validation
- Enables structured configuration with type-safe fields

## Type-Level Guarantees

### Namespace Isolation (Compile-Time)
```kotlin
object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
    val APPLE_PAY by boolean(default = false)
}

object AuthFeatures : FeatureContainer<Namespace.Authentication>(Namespace.Authentication) {
    val SSO_ENABLED by boolean(default = false)
}

// ✅ Compiles
PaymentFeatures.APPLE_PAY.evaluate(context)

// ❌ Won't compile - type mismatch
val mixed: List<Feature<Boolean, Namespace.Payments>> = listOf(
    PaymentFeatures.APPLE_PAY,  // ✅ Namespace.Payments
    AuthFeatures.SSO_ENABLED     // ❌ Namespace.Authentication
)
```

### Value Type Safety (Compile-Time)
```kotlin
val APPLE_PAY: BooleanFeature<Namespace.Payments> =
    PaymentFeatures.APPLE_PAY

val result: Boolean = context.evaluate(APPLE_PAY)  // ✅ Returns Boolean

// ❌ Won't compile - type mismatch
val wrong: String = context.evaluate(APPLE_PAY)
```

### Context Polymorphism (Runtime)
```kotlin
// Base context
val baseContext: Context = Context(...)
baseContext.evaluate(APPLE_PAY)  // ✅ Works

// Extended context
val enterpriseContext: EnterpriseContext = EnterpriseContext(...)
enterpriseContext.evaluate(APPLE_PAY)  // ✅ Also works

// Feature doesn't care about context type
// Only rules within the feature care
```

## Type Parameter Design Rationale

### Why Remove S from Feature?
**Before**: `Feature<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>`
**After**: `Feature<T : Any, M : Namespace>`

**Reasons**:
1. EncodableValue is implementation detail for serialization
2. Users only care about value type T, not wrapper S
3. S adds no compile-time safety for users (validated internally)
4. Removes 25% of type parameters from public API
5. Cleaner type signatures: `Feature<Boolean, Payments>` vs `Feature<BooleanEncodeable, Boolean, Context, Payments>`

### Why Remove C from Feature?
**Before**: `Feature<..., C : Context, ...>`
**After**: Feature has no C parameter

**Reasons**:
1. Features themselves don't evaluate - FlagDefinitions do
2. Allows polymorphic evaluation: any Context can evaluate any Feature
3. Rules within flag can still be context-specific (FlagDefinition has C)
4. Simplifies delegation: no need to specify context type when defining features
5. Most features use base Context anyway

### Why Keep C in FlagDefinition?
**FlagDefinition** still has: `<T : Any, C : Context, M : Namespace>`

**Reasons**:
1. Rules need to match against specific context types
2. Custom contexts (EnterpriseContext) need compile-time validation
3. Evaluation is type-safe: wrong context won't compile
4. Internal type - complexity acceptable for correctness

### Why Keep M Everywhere?
**M : Namespace** appears in Feature, FlagDefinition, FlagScope, etc.

**Reasons**:
1. Compile-time namespace isolation is core value prop
2. Zero runtime cost (phantom type)
3. Enables type-safe registry selection
4. Prevents accidental flag mixing across teams
5. Type-driven dispatch for configuration

## Type Inference

### Successful Inference
```kotlin
// T inferred from default value
val APPLE_PAY by boolean(default = false)  // T = Boolean

// E inferred from default value
val LOG_LEVEL by enum(default = LogLevel.INFO)  // E = LogLevel

// T inferred from default value
val CONFIG by dataClass(default = PaymentConfig())  // T = PaymentConfig
```

### Explicit Type Parameters (When Needed)
```kotlin
// Custom context requires explicit C
val ENTERPRISE_FLAG by boolean<EnterpriseContext>(default = false) {
    rule {
        // Can access EnterpriseContext fields
        custom { subscriptionTier == Tier.PREMIUM }
    } returns true
}
```

## Type Aliases (None Currently)

No type aliases used to avoid hiding complexity. Full signatures preferred for clarity.

Potential future additions:
```kotlin
// Could simplify common types
typealias BooleanFlag<M> = Feature<Boolean, M>
typealias StringFlag<M> = Feature<String, M>

// But decided against: hides important type information
// Better to see full Feature<Boolean, M> signature
```
