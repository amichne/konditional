# Core API Reference

Reference for evaluating features in a total, deterministic runtime path.

## `Feature.evaluate(context, registry): T`

Standard evaluation API.

```kotlin
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
    context: C,
    registry: NamespaceRegistry = namespace,
): T
```

- **Guarantee**: Returns a value of type `T` and never returns `null` for supported runtime usage.

- **Mechanism**: Evaluation returns the first matching rule value or the declared default.

- **Boundary**: Throws `IllegalStateException` if the feature is not registered in the registry.

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

### Observability and explain diagnostics

- Public explain APIs were removed.
- Internal diagnostics are still produced for sibling modules (OpenFeature, observability, telemetry) via internal API opt-in.
- Application code should use `evaluate(...)` as the public entrypoint.

---

## Next steps

- [Core types](/core/types)
- [Observability](/observability/)
- [Runtime operations](/runtime/)
