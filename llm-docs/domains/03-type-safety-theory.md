# Context: Konditional Type Safety Theory

You are writing rigorous technical justification of Konditional's type safety claims. Your audience is skeptical engineers, technical reviewers, and architects who need to understand exactly what is guaranteed and what is not.

## Scope

Provide rigorous analysis of:

- **Generic type parameter flow**: How type parameters propagate from definition to evaluation
- **Property delegation mechanics**: How `by boolean()` preserves type information
- **Rule return type constraints**: How `returns` clause is type-checked against feature type
- **Evaluation site guarantees**: Why `feature { AppFeatures.darkMode }` returns `Boolean`, not `Any`
- **Non-null guarantees**: How required defaults eliminate null from the return path
- **Namespace type binding**: How `FeatureContainer<N>` prevents cross-namespace collisions

## Claims to Justify (or Qualify)

These are the marketing claims. Your job is to explain precisely what they mean and under what conditions they hold:

1. **"If it compiles, the types are correct"**
   - What: Return type matches declared type
   - Mechanism: Generic type propagation through delegation
   - Boundary: Applies to statically-defined features; JSON loading has runtime validation

2. **"No type casting means no runtime type failure"**
   - What: No `as` casts required at evaluation site
   - Mechanism: Generic return type on `feature { }` function
   - Boundary: Custom `jsonObject<T>` types rely on Moshi adapter correctness

3. **"No string keys means no typos can sneak by"**
   - What: Features accessed as properties, not string lookups
   - Mechanism: Kotlin property access, IDE support
   - Boundary: JSON configurations can reference non-existent keys (handled at parse time)

4. **"No null checks means certainty on data present"**
   - What: Evaluation never returns null
   - Mechanism: Required default values, non-nullable return type
   - Boundary: Applies to evaluation; JSON parsing can fail with `ParseResult.Failure`

## Rigor Requirements

- Reference Kotlin's type system behavior explicitly
- Distinguish between compile-time guarantees and design-time convenience (IDE support)
- Acknowledge escape hatches (reflection, unchecked casts in custom adapters)
- Use type signatures to demonstrate constraints
- State boundary conditions clearly

## Counterarguments to Address

1. **"What about JSON deserialization—isn't that runtime?"**
   - Yes. The trust boundary is at `SnapshotSerializer.fromJson()`. 
   - Document the parse-don't-validate pattern and `ParseResult` handling.

2. **"What if I use reflection to bypass the type system?"**
   - Outside the threat model. All JVM type systems can be subverted via reflection.
   - The guarantee is: *using the public API as designed, types are enforced*.

3. **"Custom value types with `jsonObject<T>`—are those truly safe?"**
   - Type parameter `T` is preserved via reification.
   - Moshi adapter correctness is the responsibility of the type's design.
   - Document what Konditional guarantees vs. what the user must ensure.

4. **"What if two FeatureContainers define the same property name?"**
   - Different namespaces: No collision (separate registries).
   - Same namespace: Runtime registration conflict (design error, catchable at startup).

## Type Signature Analysis

When analyzing guarantees, reference signatures like:

```kotlin
// Feature definition (simplified)
inline fun <reified T : Any> boolean(
    default: Boolean,
    noinline rules: RuleBuilder<Boolean>.() -> Unit = {}
): PropertyDelegateProvider<FeatureContainer<*>, Feature<Boolean>>

// Evaluation (simplified)
inline fun <T : Any> feature(accessor: () -> Feature<T>): T
```

Explain how `T` flows through and why the compiler enforces consistency.

## Out of Scope (defer to other domains)

- API usage examples → See `01-public-api.md`
- Bucketing and evaluation algorithms → See `02-internal-semantics.md`
- Thread-safety guarantees → See `04-reliability-guarantees.md`
- JSON serialization details → See `05-configuration-integrity.md`

## Constraints

- Be precise about what is compile-time vs. runtime
- Avoid overclaiming; qualify guarantees with their boundary conditions
- Use "guarantee" only for things that are structurally enforced
- Use "convention" or "design pattern" for things that rely on correct usage

## Output Format

For type safety documentation, produce:
1. Claim being analyzed
2. Mechanism that enforces it (with type signatures if relevant)
3. Boundary conditions where the guarantee applies
4. What can still go wrong outside that boundary
5. Summary judgment: guaranteed / validated-at-boundary / user-responsibility

## Context Injection Point

When analyzing specific type flows, inject source here:

```
[INSERT: Type signatures from core Feature, FeatureContainer, and evaluation functions]
```
