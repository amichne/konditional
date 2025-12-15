# Konditional: Type Safety Theory & Justification

**A Technical Brief on Compile-Time Guarantees in Feature Flag Systems**

-----

## Summary

Konditional makes a bold claim: *“If your code compiles, your flags work.”* 

This brief rigorously examines that assertion, delineating exactly which guarantees are enforced at compile-time, which are enforced at design-time via tooling, and which necessarily remain runtime concerns. The goal is not to prove Konditional is “perfectly safe,” but to provide a precise map of where trust is established and where vigilance remains necessary.

-----

## The Problem Konditional Solves

Traditional feature flag systems are stringly-typed: flags are identified by string keys and values are retrieved via type-casting APIs. This architecture introduces three categories of failure:

1. **Key mismatch**: A typo in `"dark_mode"` vs `"darkMode"` silently returns a default or null value.
1. **Type mismatch**: Retrieving a boolean flag as a string succeeds syntactically but produces incorrect behavior.
1. **Null propagation**: Missing configurations or evaluation failures surface as null values that propagate through business logic.

These failures share a common characteristic: they are *syntactically valid* at compile-time but *semantically incorrect* at runtime. Konditional’s design targets this gap.

-----

## Mechanism 1: Property Delegation as Type Binding

Konditional defines features as delegated properties within a `FeatureContainer`:

```kotlin
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val darkMode by boolean(default = false) { /* rules */ }
    val timeout by double(default = 30.0) { /* rules */ }
    val maxRetries by int(default = 3) { /* rules */ }
}
```

The delegation functions (`boolean()`, `double()`, `int()`, `string()`, `jsonObject<T>()`) are generic factory functions that return property delegates parameterized by the value type. When the Kotlin compiler processes `val darkMode by boolean(...)`, it infers:

- The property `darkMode` has type `Feature<Boolean>` (or an equivalent delegate type).
- Any access to `darkMode` within the type system carries the `Boolean` type parameter.

**Guarantee**: The declared type of a feature is statically known and cannot drift. If `darkMode` is defined via `boolean()`, no amount of refactoring can silently change its type without explicit code modification that the compiler will re-evaluate.

-----

## Mechanism 2: Type-Constrained Rule Returns

The rule DSL enforces that `returns` clauses match the feature’s declared type:

```kotlin
val timeout by double(default = 30.0) {
    rule {
        platforms(Platform.ANDROID)
    } returns 45.0  // Must be Double
}
```

This is enforced via generic constraints on the rule builder. The `returns` function is typed to accept only values matching the feature’s type parameter. Attempting to return a `String` from a `Double` feature results in a compilation error—not a runtime type-casting failure.

**Guarantee**: Rule return values are type-checked at compile-time. A rule cannot produce a value incompatible with its feature’s declared type.

**Boundary condition**: This guarantee applies to the statically-defined rules in code. Dynamically loaded rules from JSON (remote configuration) operate under different constraints discussed below.

-----

## Mechanism 3: Evaluation Site Type Propagation

The evaluation entry point preserves type information through the call site:

```kotlin
val isDarkMode: Boolean = feature { AppFeatures.darkMode }
val timeout: Double = feature { AppFeatures.timeout }
```

The `feature { }` function is a generic function that captures the type parameter from the accessed property. The return type is not `Any` requiring a cast—it is the concrete type declared at definition. This means:

- Assigning the result to an incompatible type is a compile error.
- IDE autocomplete correctly shows the return type.
- Refactoring tools propagate type changes correctly.

**Guarantee**: The call site type matches the definition site type with no manual casting required.

-----

## Mechanism 4: Required Defaults as Non-Null Enforcement

Every feature definition requires a default value:

```kotlin
val darkMode by boolean(default = false)  // default is mandatory
```

This default is used when no rules match the evaluation context. Because the default is required and typed, the evaluation function can guarantee a non-null return:

```kotlin
// Simplified signature (illustrative)
fun <T : Any> feature(accessor: () -> Feature<T>): T
```

The return type is `T`, not `T?`. The default value ensures that even in the worst case—no rules match, configuration is empty, context is unusual—a valid value of the correct type is returned.

**Guarantee**: Evaluation never returns null. Every call to `feature { }` produces a value of the declared type.

-----

## Mechanism 5: Namespace Isolation via Type Binding

`FeatureContainer` is parameterized by its namespace:

```kotlin
object AuthFeatures : FeatureContainer<Namespace.Authentication>(Namespace.Authentication)
object PaymentFeatures : FeatureContainer<Namespace.Payments>(Namespace.Payments)
```

This type binding means features are registered to their namespace at the type level. Cross-namespace collisions are not merely unlikely—they are structurally impossible because the registries are distinct objects accessed through typed containers.

**Guarantee**: Feature key collisions across namespaces cannot occur.

-----

## The Trust Boundary: Where Compile-Time Guarantees End

The guarantees above apply to *statically defined* configurations. Konditional also supports *dynamic configuration* via JSON loading:

```kotlin
val json = File("flags.json").readText()
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> logError("Parse failed: ${result.error}")
}
```

At this boundary, compile-time guarantees necessarily yield to runtime validation. The system addresses this via:

1. **Schema validation**: The deserializer validates that JSON structure conforms to expected schema.
1. **Type checking**: Values in JSON are checked against the declared types of registered features.
1. **Result types**: `ParseResult.Success | ParseResult.Failure` forces callers to handle validation failures explicitly.

**Critical distinction**: A JSON payload that passes `fromJson` validation will produce type-correct configurations. The guarantee is not “JSON is always valid” but rather “invalid JSON is detected and rejected before it can affect evaluation.” This is the *parse-don’t-validate* pattern—transforming unstructured external data into typed internal structures with validation at the boundary.

**What can still go wrong**:

- A JSON payload could define a feature key that doesn’t exist in code (typically ignored or logged).
- A JSON payload could be syntactically valid but semantically incorrect (wrong rollout percentage, inverted boolean logic).
- The system consuming JSON must correctly handle `ParseResult.Failure`.

These are *semantic* errors in configuration authoring, not *type* errors that the compiler could catch.

-----

## Addressing Common Counterarguments

### “What about reflection-based bypass?”

Kotlin’s type system can be circumvented via reflection. However, this is true of *all* compile-time type systems in JVM languages. The guarantee is: *if you use the public API as designed, types are enforced.* Deliberately subverting the type system via reflection is outside the threat model.

### “What about `jsonObject<T>()` custom types?”

Custom types use Moshi for serialization. The type parameter `T` is preserved through reification, and Moshi adapters enforce type correctness during deserialization. However, if a custom `T` has mutable state or complex invariants beyond JSON structure, those invariants are the responsibility of the type’s design, not Konditional’s.

### “What if I define the same feature name in two containers?”

Features are scoped to their `FeatureContainer` instance, which is bound to a namespace. Two containers in different namespaces can have identically-named properties because they exist in separate registries. Two containers in the *same* namespace would create a conflict—this is a design error that would manifest at runtime during registration, not at compile time. A lint rule or startup validation can catch this.

-----

## Summary of Guarantees

|Claim                          |Guarantee Level        |Mechanism                             |
|-------------------------------|-----------------------|--------------------------------------|
|No typos in flag keys          |**Compile-time**       |Property access, not strings          |
|Return type matches declaration|**Compile-time**       |Generic type propagation              |
|Rule return types are correct  |**Compile-time**       |Constrained rule builder              |
|Evaluation is never null       |**Compile-time**       |Required defaults, non-nullable return|
|Namespace isolation            |**Compile-time**       |Type-parameterized containers         |
|Remote config type safety      |**Runtime (validated)**|Parse-don’t-validate with Result types|
|Semantic correctness of configs|**Not guaranteed**     |Human/process responsibility          |

-----

## Conclusion

Konditional’s type safety is not magic—it is the systematic application of Kotlin’s type system to eliminate categories of runtime failure. By using property delegation, generic constraints, required defaults, and typed namespaces, the library ensures that correctly-compiled code cannot suffer from key typos, type mismatches, or null propagation in flag evaluation.

The boundary of these guarantees is clearly defined: statically defined configurations are compile-time safe; dynamically loaded configurations are runtime-validated with explicit error handling. Within this boundary, the claim holds: *if it compiles, it works.*

What remains the responsibility of the developer is *semantic* correctness—ensuring that the right rules target the right users with the right values. No type system can verify business logic. But Konditional ensures that the *mechanics* of flag definition and evaluation are structurally sound, freeing developers to focus on the decisions that matter.
@amichne
Comment
