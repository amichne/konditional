---
title: Parsing & Errors (Hard Boundaries)
---

# Parsing & Errors

Konditional treats configuration as a **hard boundary**:

- Parsing returns typed results.
- Invalid inputs produce typed failures.
- The active configuration is not partially mutated by a bad update.

:::danger Boundary discipline
If parsing fails, the update is rejected and the last-known-good configuration remains active.
:::

## ParseResult / ParseError philosophy

Parsing is modeled as a value:

- `ParseResult.Success(value)`
- `ParseResult.Failure(error)`

The goal is to keep evaluation and application logic free of “maybe it parsed” states.

```kotlin
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> AppFeatures.load(result.value)
    is ParseResult.Failure -> logger.error(result.error.message)
}
```

## Common error classes

While the exact error set depends on which codec you use at the boundary, common categories include:

- invalid ramp-up percentage
- invalid versions / version ranges
- unknown or malformed feature keys
- invalid axis IDs or axis values

## What is validated (and what is not)

**Validated at the boundary:**

- JSON syntax and shape
- Feature existence (keys must be registered)
- Type correctness (values match definitions)
- Ramp-up values and versions (where applicable)

**Not validated:**

- Semantic correctness (whether a value is “right” for the business)
- Business logic correctness (extension predicates are code)

## Comparison: boundary handling

| Situation                         | String‑keyed SDKs                  | Konditional boundary                          |
|----------------------------------|------------------------------------|-----------------------------------------------|
| Wrong type in JSON               | Coerce, default, or crash          | `ParseResult.Failure`, rejected update        |
| Unknown feature key              | Ignore silently                    | `ParseError.FeatureNotFound`                  |
| Invalid ramp‑up percentage       | Undefined behavior                 | Parse failure                                 |
| Partial update                   | SDK‑dependent                      | Atomic swap or reject, never partial          |

Next:

- [Registry & Configuration](/registry-and-configuration)
- [FAQ](/faq)
