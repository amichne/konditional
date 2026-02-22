# Evaluate in app code

Evaluate your typed feature against stable context inputs so repeated calls with
the same inputs produce the same result.

## Read this page when

- You already declared your first feature.
- You need to wire evaluation into application code.
- You want deterministic behavior before adding rollout rules.

## Example

```kotlin
val ctx = Context(
  locale = AppLocale.UNITED_STATES,
  platform = Platform.IOS,
  appVersion = Version.of(1, 0, 0),
  stableId = StableId.of("user-123"),
)

val variant = AppFeatures.checkoutVariant.evaluate(ctx)
```

Evaluation follows the core flag semantics and deterministic bucketing behavior
for stable inputs [CLM-PR01-09A].

## Next steps

1. Add rollout rules in
   [Add deterministic ramp-up](/quickstart/add-deterministic-ramp-up).
2. Add snapshot ingestion in
   [Load first snapshot safely](/quickstart/load-first-snapshot-safely).
3. Validate behavior in [Verify end-to-end](/quickstart/verify-end-to-end).

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-09A | Feature evaluation follows core flag-definition evaluation semantics and deterministic bucketing behavior. | `#example` | `/reference/claims-registry#clm-pr01-09a` |
