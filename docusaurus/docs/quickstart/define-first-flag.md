# Define first flag

Define a namespace-owned typed feature so compile-time contracts govern
evaluation behavior from the start.

## Read this page when

- Installation is complete and dependencies compile.
- You want a minimal, typed feature declaration.
- You need declarations that align with snapshot boundary codecs.

## Example

```kotlin
enum class CheckoutVariant { CLASSIC, OPTIMIZED }

object AppFeatures : Namespace("app") {
  val checkoutVariant by enum<CheckoutVariant, Context>(
    default = CheckoutVariant.CLASSIC,
  )
}
```

This declaration establishes typed feature ownership through namespace, feature,
and flag-definition types [CLM-PR01-08A]. The namespace shape also becomes the
compiled schema used by loaders and codecs [CLM-PR01-08B].

## Next steps

1. Evaluate the feature in
   [Evaluate in app code](/quickstart/evaluate-in-app-code).
2. Add controlled rollout with
   [Add deterministic ramp-up](/quickstart/add-deterministic-ramp-up).
3. Load runtime config with
   [Load first snapshot safely](/quickstart/load-first-snapshot-safely).

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-08A | Feature declarations are represented by Namespace, Feature, and FlagDefinition types. | `#example` | `/reference/claims-registry#clm-pr01-08a` |
| CLM-PR01-08B | Namespaces provide a compiled schema used by boundary codecs and loaders. | `#example` | `/reference/claims-registry#clm-pr01-08b` |
