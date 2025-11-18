# Engineering Deep Dive: Type System

**Navigate**: [← Previous: Fundamentals](02-fundamentals.md) | [Next: Evaluation Engine →](04-evaluation-engine.md)

---

## The Foundation of Compile-Time Safety

Konditional's type system is what makes "if it compiles, it works" possible. This chapter dissects how Kotlin's generics enforce safety throughout the system.

## Type Parameters Explained

Every major component in Konditional uses four type parameters:

```kotlin
Feature<S, T, C, M>
FlagDefinition<S, T, C, M>
ConditionalValue<S, T, C, M>
```

Let's understand each:

### S: EncodableValue<T>

**What**: Wrapper type for serialization
**Why**: Provides type-safe JSON conversion
**Example**: `BooleanEncodeable`, `StringEncodeable`, `IntEncodeable`, `DecimalEncodeable`

```kotlin
sealed class EncodableValue<out T : Any> {
    data class BooleanEncodeable(val value: Boolean) : EncodableValue<Boolean>()
    data class StringEncodeable(val value: String) : EncodableValue<String>()
    data class IntEncodeable(val value: Int) : EncodableValue<Int>()
    data class DecimalEncodeable(val value: Double) : EncodableValue<Double>()
}
```

**Key insight**: `S` is rarely used directly in application code. It's an implementation detail that enables type-safe serialization.

### T: Any

**What**: The actual value type returned by evaluation
**Why**: The type your application code sees
**Example**: `Boolean`, `String`, `Int`, `Double`

```kotlin
val DARK_MODE by boolean(default = false)
//                       ^^^^^ T = Boolean

val enabled: Boolean = context.evaluate(DARK_MODE)
//           ^^^^^^^                     ^^^^^^^^^^
//           T                           Feature<_, Boolean, _, _>
```

**Key insight**: `T` is what your code works with. The compiler ensures the return type always matches.

### C: Context

**What**: The evaluation context type
**Why**: Constrains which contexts can evaluate this feature
**Example**: `Context`, `EnterpriseContext`

```kotlin
val BASIC_FEATURE by boolean<Context>(default = false)
//                            ^^^^^^^
//                            Requires Context (or subtype)

val ENTERPRISE_FEATURE by boolean<EnterpriseContext>(default = false)
//                                ^^^^^^^^^^^^^^^^^^
//                                Requires EnterpriseContext (more specific)
```

**Key insight**: Type parameter `C` enables compile-time enforcement of context requirements.

### M: Namespace

**What**: The namespace this feature belongs to
**Why**: Provides compile-time isolation between domains
**Example**: `Namespace.Global`, `Namespace.Payments`

```kotlin
object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments) {
//                                        ^^^^^^^^^^^^^^^^^^
//                                        M = Namespace.Payments
    val APPLE_PAY by boolean(default = false)
}

// APPLE_PAY's full type:
// Feature<BooleanEncodeable, Boolean, Context, Namespace.Payments>
//                                              ^^^^^^^^^^^^^^^^^^
//                                              M binds to Namespace.Payments
```

**Key insight**: Namespace binding at compile time prevents cross-namespace contamination.

---

## Type Flow Through the System

Let's trace how types flow from definition to evaluation:

### Step 1: Feature Definition

```kotlin
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val DARK_MODE by boolean(default = false)
}

// Compiler infers:
// DARK_MODE: BooleanFeature<Context, Namespace.Global>
//
// Which expands to:
// Feature<BooleanEncodeable, Boolean, Context, Namespace.Global>
//          ^^^^^^^^^^^^^^^^^  ^^^^^^^  ^^^^^^^  ^^^^^^^^^^^^^^^^
//          S                  T        C        M
```

### Step 2: FlagDefinition Creation

```kotlin
// Internally, `boolean(default = false)` creates:
FlagDefinition<BooleanEncodeable, Boolean, Context, Namespace.Global>(
    feature = DARK_MODE,
    defaultValue = false,  // T = Boolean
    values = listOf(),
    isActive = true,
    salt = "v1"
)
```

**Type constraints enforced**:
- `defaultValue: T` must be `Boolean` (from `T` parameter)
- `feature` must match all type parameters
- `values` must be `List<ConditionalValue<S, T, C, M>>`

### Step 3: Evaluation

```kotlin
val context: Context = Context(...)
val enabled: Boolean = context.evaluate(AppFeatures.DARK_MODE)
//           ^^^^^^^                    ^^^^^^^^^^^^^^^^^^^^
//           Inferred from T            Feature<_, Boolean, Context, _>
```

**Compiler checks**:
1. `DARK_MODE` requires `Context` (C type parameter)
2. `context` is `Context` ✓
3. Return type is `Boolean` (T type parameter) ✓

---

## Property Delegation Deep Dive

How does `by boolean(default = false)` work?

### The Delegation Mechanism

```kotlin
val DARK_MODE by boolean(default = false)
//            ^^
//            Property delegation operator
```

This desugars to:

```kotlin
// What you write:
val DARK_MODE by boolean(default = false)

// What the compiler sees:
private val DARK_MODE$delegate = boolean(default = false)

val DARK_MODE: BooleanFeature<Context, Namespace.Global>
    get() = DARK_MODE$delegate.getValue(this, ::DARK_MODE)
```

### The Delegate Class

```kotlin
// Simplified implementation
class BooleanFeatureDelegate<C : Context, M : Namespace>(
    private val container: FeatureContainer<M>,
    private val defaultValue: Boolean,
    private val flagScope: FlagScope<BooleanEncodeable, Boolean, C, M>.() -> Unit
) : ReadOnlyProperty<Any?, BooleanFeature<C, M>> {

    private var cached: BooleanFeature<C, M>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): BooleanFeature<C, M> {
        return cached ?: createFeature(property).also {
            cached = it
            container.register(it)  // Register on first access
        }
    }

    private fun createFeature(property: KProperty<*>): BooleanFeature<C, M> {
        val key = property.name  // "DARK_MODE"
        return object : BooleanFeature<C, M> {
            override val key = key
            override val namespace = container.namespace
        }
    }
}
```

**Key behaviors**:
1. **Lazy initialization**: Feature created on first access
2. **Property name capture**: `property.name` becomes the key
3. **Automatic registration**: Delegate registers feature in container
4. **Type inference**: Compiler infers all type parameters

---

## Type Constraints and Bounds

### Constraint 1: S must be EncodableValue<T>

```kotlin
sealed interface Feature<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>
//                        ^^^^^^^^^^^^^^^^^^^^
//                        Upper bound constraint
```

**Effect**: You cannot create a Feature with mismatched S and T:

```kotlin
// ✓ Valid: BooleanEncodeable wraps Boolean
Feature<BooleanEncodeable, Boolean, Context, Namespace.Global>

// ✗ Invalid: BooleanEncodeable doesn't wrap String
Feature<BooleanEncodeable, String, Context, Namespace.Global>
//                         ^^^^^^
//                         Compile error: type mismatch
```

### Constraint 2: T must be Any

```kotlin
sealed interface Feature<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>
//                                              ^^^^^^
//                                              Non-null constraint
```

**Effect**: Feature values cannot be nullable:

```kotlin
// ✓ Valid: Boolean is non-null
Feature<BooleanEncodeable, Boolean, Context, Namespace.Global>

// ✗ Invalid: Boolean? is nullable
Feature<BooleanEncodeable, Boolean?, Context, Namespace.Global>
//                         ^^^^^^^^
//                         Compile error: Boolean? doesn't conform to Any
```

This is why evaluation never returns null - the type system prevents it.

### Constraint 3: C must be Context

```kotlin
sealed interface Feature<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>
//                                                        ^^^^^^^^^^
//                                                        Interface constraint
```

**Effect**: Only Context subtypes can be used for evaluation:

```kotlin
// ✓ Valid: Context implements Context interface
Feature<_, _, Context, _>

// ✓ Valid: EnterpriseContext extends Context
data class EnterpriseContext(...) : Context
Feature<_, _, EnterpriseContext, _>

// ✗ Invalid: String doesn't implement Context
Feature<_, _, String, _>
//            ^^^^^^
//            Compile error: String doesn't conform to Context
```

### Constraint 4: M must be Namespace

```kotlin
sealed interface Feature<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>
//                                                                     ^^^^^^^^^^^^
//                                                                     Sealed class constraint
```

**Effect**: Features must belong to a defined namespace:

```kotlin
// ✓ Valid: Namespace.Global is a Namespace
Feature<_, _, _, Namespace.Global>

// ✗ Invalid: String is not a Namespace
Feature<_, _, _, String>
//               ^^^^^^
//               Compile error: String doesn't conform to Namespace
```

---

## Variance and Type Safety

### Why `out T` in EncodableValue?

```kotlin
sealed class EncodableValue<out T : Any>
//                          ^^^
//                          Covariant position
```

**Covariance** (`out T`) means `EncodableValue<Boolean>` is a subtype of `EncodableValue<Any>`.

**Why this matters**:

```kotlin
val boolValue: EncodableValue<Boolean> = BooleanEncodeable(true)
val anyValue: EncodableValue<Any> = boolValue  // ✓ Valid with 'out'

// Without 'out', this would be a compile error
```

This enables generic operations over any `EncodableValue` without knowing the specific type.

### Invariance in Feature

```kotlin
sealed interface Feature<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>
//                       No variance annotation
```

**Invariance** means `Feature<_, Boolean, _, _>` is NOT a subtype of `Feature<_, Any, _, _>`.

**Why**: Evaluation must return exactly `T`, not a subtype or supertype:

```kotlin
val boolFeature: Feature<_, Boolean, _, _> = ...
val anyFeature: Feature<_, Any, _, _> = boolFeature  // ✗ Compile error

// This is correct - we want to know the exact type
val enabled: Boolean = context.evaluate(boolFeature)
//           ^^^^^^^
//           Must be Boolean, not Any
```

---

## Context Type Constraints

### Basic Context

```kotlin
val FEATURE by boolean<Context>(default = false)
//                     ^^^^^^^
//                     C = Context

// Any Context (or subtype) can evaluate:
val basic: Context = Context(...)
val enterprise: EnterpriseContext = EnterpriseContext(...)

basic.evaluate(FEATURE)       // ✓
enterprise.evaluate(FEATURE)  // ✓ (EnterpriseContext is Context)
```

### Custom Context

```kotlin
val ENTERPRISE_FEATURE by boolean<EnterpriseContext>(default = false)
//                                ^^^^^^^^^^^^^^^^^^
//                                C = EnterpriseContext

// Only EnterpriseContext (or subtype) can evaluate:
val basic: Context = Context(...)
val enterprise: EnterpriseContext = EnterpriseContext(...)

basic.evaluate(ENTERPRISE_FEATURE)       // ✗ Compile error
enterprise.evaluate(ENTERPRISE_FEATURE)  // ✓
```

**Compiler check**:
```kotlin
fun <C : Context> Context.evaluate(feature: Feature<_, _, C, _>): T
//   ^^^^^^^^^^                                        ^
//   Type parameter                                    Must match

// When called:
basic.evaluate(ENTERPRISE_FEATURE)
//    ^^^^^^^  ^^^^^^^^^^^^^^^^^^^
//    Context  Feature<_, _, EnterpriseContext, _>
//             ↑ EnterpriseContext is not assignable from Context
//             Compile error!
```

---

## Type Inference

Kotlin's type inference minimizes verbosity while maintaining safety.

### Inference in FeatureContainer

```kotlin
val DARK_MODE by boolean(default = false)
//  ^^^^^^^^^
//  Compiler infers: BooleanFeature<Context, Namespace.Global>

// How:
// 1. `boolean()` returns delegate typed to BooleanFeature<C, M>
// 2. Container is FeatureContainer<Namespace.Global>, so M = Namespace.Global
// 3. No explicit C, so defaults to Context
// 4. Result: BooleanFeature<Context, Namespace.Global>
```

### Inference in Evaluation

```kotlin
val enabled = context.evaluate(AppFeatures.DARK_MODE)
//  ^^^^^^^
//  Compiler infers: Boolean

// How:
// 1. AppFeatures.DARK_MODE is Feature<_, Boolean, _, _>
// 2. evaluate<T>() returns T
// 3. T = Boolean
// 4. Result: Boolean
```

### Explicit Type Parameters

You rarely need explicit types, but you can provide them:

```kotlin
// Explicit context type
val FEATURE by boolean<EnterpriseContext>(default = false)
//                     ^^^^^^^^^^^^^^^^^^
//                     Explicitly set C

// Explicit return type (redundant, inferred from feature)
val enabled: Boolean = context.evaluate(AppFeatures.DARK_MODE)
//           ^^^^^^^
//           Redundant but valid
```

---

## Reified Types and Inline Functions

### Where Reification Is Used

```kotlin
inline fun <reified T : Any> parseValue(json: String): ParseResult<T>
//          ^^^^^^^^
//          Reified type parameter
```

**Reified** means the type `T` is available at runtime (normally erased in JVM).

**Used in serialization**:
```kotlin
// Can check type at runtime
when (T::class) {
    Boolean::class -> parseBoolean(json)
    String::class -> parseString(json)
    Int::class -> parseInt(json)
    Double::class -> parseDouble(json)
}
```

**Not used in evaluation**:
```kotlin
// Type parameters NOT reified - no runtime overhead
fun <S : EncodableValue<T>, T : Any, C : Context, M : Namespace>
    C.evaluate(feature: Feature<S, T, C, M>): T
```

Evaluation doesn't need runtime type information because all types are known at compile time.

---

## Type Aliases

### Simplifying Complex Types

```kotlin
// Instead of:
val feature: Feature<BooleanEncodeable, Boolean, Context, Namespace.Global>

// Use type alias:
typealias BooleanFeature<C, M> = Feature<BooleanEncodeable, Boolean, C, M>

val feature: BooleanFeature<Context, Namespace.Global>
//           ^^^^^^^^^^^^^^
//           Much cleaner
```

**Provided aliases**:
```kotlin
typealias BooleanFeature<C, M> = Feature<BooleanEncodeable, Boolean, C, M>
typealias StringFeature<C, M> = Feature<StringEncodeable, String, C, M>
typealias IntFeature<C, M> = Feature<IntEncodeable, Int, C, M>
typealias DoubleFeature<C, M> = Feature<DecimalEncodeable, Double, C, M>
```

---

## Phantom Types

### What Are Phantom Types?

Type parameters that don't appear in the class body:

```kotlin
sealed interface Feature<S : EncodableValue<T>, T : Any, C : Context, M : Namespace> {
    val key: String
    val namespace: M
    //           ^
    //           Only M appears in body
    //           S, T, C are "phantom" - used only for type checking
}
```

### Why Use Phantom Types?

They enable compile-time constraints without runtime overhead:

```kotlin
// S and T constrain what values are valid
Feature<BooleanEncodeable, Boolean, _, _>  // Only Boolean values valid
Feature<StringEncodeable, String, _, _>     // Only String values valid

// But at runtime, Feature has no reference to S or T
// The constraint is purely compile-time
```

**Benefit**: Type safety with zero runtime cost.

---

## Putting It All Together

### Complete Type Flow Example

```kotlin
// 1. Feature definition
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val TIMEOUT by int(default = 30)
    //  ^^^^^^^    ^^^
    //  Key        Delegate factory
}

// 2. Compiler infers full type
// TIMEOUT: IntFeature<Context, Namespace.Global>
// Which expands to:
// Feature<IntEncodeable, Int, Context, Namespace.Global>

// 3. FlagDefinition created internally
FlagDefinition<IntEncodeable, Int, Context, Namespace.Global>(
    feature = TIMEOUT,
    defaultValue = 30,  // Must be Int (T = Int)
    // ...
)

// 4. Evaluation
val context: Context = Context(...)
val timeout: Int = context.evaluate(AppFeatures.TIMEOUT)
//           ^^^                    ^^^^^^^^^^^^^^^^^^^
//           T = Int                Feature<_, Int, Context, _>
//           ↑_________________________________↑
//                    Types match, compiler happy
```

### Type Safety Guarantees

At every step, the compiler ensures:

1. **Value type matches** (`defaultValue: T`, return type `T`)
2. **Context type matches** (Context `C` can evaluate Feature requiring `C`)
3. **Namespace matches** (Feature in namespace `M` uses registry of `M`)
4. **Non-null** (`T : Any` prevents nullable types)
5. **Serializability** (`S : EncodableValue<T>` ensures JSON conversion)

**Result**: If it compiles, these guarantees hold. Runtime violations are impossible.

---

## Review

### The Four Type Parameters

| Parameter | Bound | Purpose | Example |
|-----------|-------|---------|---------|
| `S` | `EncodableValue<T>` | Serialization wrapper | `BooleanEncodeable` |
| `T` | `Any` | Actual value type | `Boolean` |
| `C` | `Context` | Evaluation context | `EnterpriseContext` |
| `M` | `Namespace` | Feature namespace | `Namespace.Payments` |

### Key Type System Features

- **Property delegation**: Captures property names as keys
- **Type constraints**: Enforces compatibility at compile time
- **Type inference**: Minimizes verbosity
- **Phantom types**: Compile-time safety, zero runtime cost
- **Variance**: Enables generic operations (covariance in `EncodableValue`)
- **Invariance**: Preserves exact types (invariance in `Feature`)

---

## Next Steps

Now that you understand how types enforce safety, we can explore what happens at runtime.

**Next chapter**: [Evaluation Engine](04-evaluation-engine.md)
- How FlagDefinition evaluates contexts
- Step-by-step evaluation flow
- Rule matching algorithm
- Default value resolution

The type system ensures evaluation is safe. Now let's see how it works.

---

**Navigate**: [← Previous: Fundamentals](02-fundamentals.md) | [Next: Evaluation Engine →](04-evaluation-engine.md)
