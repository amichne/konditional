# Add deterministic ramp-up

Add ramp-up targeting so rollout assignment stays stable for the same identity,
salt, and feature key inputs.

## Read this page when

- Basic evaluation works for your first feature.
- You need safe gradual rollout behavior.
- You want reproducible cohorts for analysis and rollback.

## Example

```kotlin
object AppFeatures : Namespace("app") {
  val checkoutVariant by enum<CheckoutVariant, Context>(
    default = CheckoutVariant.CLASSIC,
  ) {
    rule(CheckoutVariant.OPTIMIZED) {
      rampUp { 25.0 }
    }
  }
}
```

Ramp-up assignment is deterministic and exposed through stable bucket APIs
[CLM-PR01-10A].

## Next steps

1. Add boundary-safe config loading in
   [Load first snapshot safely](/quickstart/load-first-snapshot-safely).
2. Validate deterministic behavior in
   [Verify end-to-end](/quickstart/verify-end-to-end).
3. Plan phased rollout in [Adoption roadmap](/overview/adoption-roadmap).

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-10A | Ramp-up assignment is deterministic and exposed through stable bucket APIs. | `#example` | `/reference/claims-registry#clm-pr01-10a` |
