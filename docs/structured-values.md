---
title: Structured Values (Custom Types)
---

# Structured Values (Custom Types)

Booleans and enums cover many flags, but production systems often need structured configuration:

- retry policies
- thresholds and timeouts
- per-tenant limits

Konditional supports typed structured values via `custom<T>` where `T` implements `Konstrained<Schema>`.

## Example: schema‑backed value

```kotlin
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
) : Konstrained<ObjectSchema> {
    override val schema = schemaRoot {
        ::maxAttempts of { minimum = 1 }
        ::backoffMs of { minimum = 0.0 }
    }
}

object PolicyFlags : Namespace("policy") {
    val retryPolicy by custom<RetryPolicy, Context>(default = RetryPolicy())
}
```

## Why schema-backed values

**Goal:** validation happens at the boundary (parsing/loading), not inside evaluation.

- Your app code gets a fully-typed value (`T`) with a stable default.
- Invalid remote values are rejected before they can influence runtime decisions.

## Boundary rule

Schema validation can prove structural correctness (types, ranges, required fields), but it cannot prove business
correctness (“is this backoff appropriate?”). Treat structured flags as configuration with guardrails.

:::warning Semantic correctness is on you
Schema validation prevents malformed input, not poor business decisions. Keep operational guardrails (limits, alerts,
dashboards) around structured flags that can materially impact users.
:::

Next:

- [Parsing & Errors](/parsing-and-errors)
- [Recipes](/recipes) for a full example
