# Custom Konstrained values

`Konstrained` is the contract for custom feature values that are not built-in
primitives or enums. Use it when the feature value is a real domain object and
you still want JSON round-tripping through `konditional-json`.

## Start with an object-backed value

A retry policy groups a maximum attempt count, delay, mode, and enabled state.

```kotlin
import io.amichne.konditional.core.types.Konstrained
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schema

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
    val enabled: Boolean = true,
    val mode: String = "exponential",
) : Konstrained.Object {
    val schema =
        schema {
            ::maxAttempts of { minimum = 1 }
            ::backoffMs of { minimum = 0.0 }
            ::enabled of { default = true }
            ::mode of { minLength = 1 }
        }
}
```

The schema specifies the minimum count, minimum delay, and minimum mode length.

## Use the value in a feature

Declare the feature with `custom<T, C>`.

```kotlin
object CheckoutFlags : Namespace("checkout") {
    val retryPolicy by custom<RetryPolicy, Context>(
        default = RetryPolicy(),
    )
}
```

## Other supported shapes

`Konstrained` also supports primitive-backed value classes, array-backed
values, and adapted primitive encodings such as `AsString` or `AsInt`. Use those shapes when the wire form is a single primitive or a single list.
