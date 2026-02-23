---
title: Custom Structured Values
sidebar_position: 3
---

# Custom Structured Values

Use `Konstrained` types when primitive or enum values are not enough.

**Prerequisites:** You understand [Features and Types](/concepts/features-and-types).

## Step 1: Define a Structured Type

```kotlin
import io.amichne.konditional.core.types.Konstrained
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

data class CheckoutPolicy(
  val maxRetries: Int,
  val timeoutMillis: Int,
) : Konstrained.Object<ObjectSchema> {
  override val schema = schema {
    ::maxRetries of { minimum = 0 }
    ::timeoutMillis of { minimum = 100 }
  }
}
```

## Step 2: Declare Feature with `custom<...>`

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace

object CheckoutFlags : Namespace("checkout") {
  val policy by custom<CheckoutPolicy, Context>(
    default = CheckoutPolicy(maxRetries = 3, timeoutMillis = 1500),
  )
}
```

## Step 3: Evaluate as a Typed Object

```kotlin
val policy: CheckoutPolicy = CheckoutFlags.policy.evaluate(ctx)
```

## Expected Outcome

After this guide, your feature returns a schema-backed typed object with boundary-safe serialization behavior.

## Next Steps

- [Reference: Snapshot Format](/reference/snapshot-format)
- [Concept: Parse Boundary](/concepts/parse-boundary)
