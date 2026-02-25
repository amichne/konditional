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

You can also model a zero-field singleton with Kotlin `object` when you need a
single shared typed value:

```kotlin
import io.amichne.konditional.core.types.Konstrained
import io.amichne.kontracts.dsl.schema
import io.amichne.kontracts.schema.ObjectSchema

object DefaultCheckoutPolicy : Konstrained.Object<ObjectSchema> {
  override val schema = schema {}
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

## Decode behavior at the boundary

When Konditional parses custom structured values from JSON, it uses deterministic
field resolution rules.

1. Use the JSON field value when the field exists and is non-null.
2. Use the schema `defaultValue` when the field is missing and the schema
   defines a default.
3. Use the Kotlin constructor default when the field is missing and the
   constructor parameter is optional.
4. Return `Result.failure` with `ParseError.InvalidSnapshot` when a required
   field is missing and no default applies.

For Kotlin `object` singletons, decode returns the existing singleton instance
without requiring a primary constructor.

## Expected Outcome

After this guide, your feature returns a schema-backed typed object with boundary-safe serialization behavior.

## Next Steps

- [Reference: Snapshot Format](/reference/snapshot-format)
- [Concept: Parse Boundary](/concepts/parse-boundary)
