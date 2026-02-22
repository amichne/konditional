# Add deterministic ramp-up

Add ramp-up rules using stable identity tuples and deterministic bucket
assignment so rollout cohorts stay reproducible [CLM-PR01-10A].

## Example

```kotlin
rule(CheckoutVariant.EXPERIMENTAL) {
  rampUp { 25.0 }
}
```

Bucket assignment and in-ramp checks are deterministic for stable identity,
feature key, and salt inputs [CLM-PR01-10A].

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-10A | Ramp-up assignment is deterministic and exposed through stable bucket APIs. | `#example` | `/reference/claims-registry#clm-pr01-10a` |
