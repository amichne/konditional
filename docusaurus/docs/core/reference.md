# Core API Reference

Reference for evaluating features and retrieving explainable decisions.

## `Feature.evaluate(context, registry): T`

Standard evaluation API.

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
    context: C,
    registry: NamespaceRegistry = namespace,
): T
```

**Guarantee**: Returns a value of type `T` and never returns `null`.

**Mechanism**: Evaluation returns the first matching rule value or the declared default.

**Boundary**: Throws `IllegalStateException` if the feature is not registered in the registry.

### Example

```kotlin
val enabled: Boolean = AppFeatures.darkMode.evaluate(context)
```

### Behavior

1. Resolve the feature definition from the registry.
2. If the registry kill-switch is enabled, return the default.
3. If the flag is inactive, return the default.
4. Evaluate rules by descending specificity.
5. Apply ramp-up to the first matching rule.
6. Return the rule value or the default.

---

<details>
<summary>Advanced Options</summary>

## `Feature.explain(context, registry): EvaluationResult<T>`

Explainable evaluation for debugging and observability.

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.explain(
    context: C,
    registry: NamespaceRegistry = namespace,
): EvaluationResult<T>
```

### Example

```kotlin
val result = AppFeatures.darkMode.explain(context)

when (val decision = result.decision) {
    EvaluationResult.Decision.RegistryDisabled -> println("Registry disabled")
    EvaluationResult.Decision.Inactive -> println("Flag inactive")
    is EvaluationResult.Decision.Rule -> println("Matched: ${decision.matched.rule.note}")
    is EvaluationResult.Decision.Default -> println("Default returned")
}
```

---

## `Feature.evaluateWithReason(...)`

Deprecated alias for `explain(...)`.

```kotlin
@Deprecated("Use explain() instead")
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithReason(
    context: C,
    registry: NamespaceRegistry = namespace,
): EvaluationResult<T>
```

</details>

---

## Next steps

- [Core types](/core/types)
- [Observability](/observability/)
- [Runtime operations](/runtime/)
