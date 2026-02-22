# Evaluate in app code

Evaluate the typed feature in application code using stable context inputs so
results remain deterministic for the same inputs [CLM-PR01-09A].

## Example

```kotlin
val result = AppFeatures.checkoutVariant.evaluate(ctx)
```

Repeat evaluation for the same context values and verify outputs remain stable
for the same feature definition and rollout settings [CLM-PR01-09A].

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-09A | Feature evaluation follows core flag-definition evaluation semantics and deterministic bucketing behavior. | `#example` | `/reference/claims-registry#clm-pr01-09a` |
