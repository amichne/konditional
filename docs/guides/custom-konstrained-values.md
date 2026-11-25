# Custom Konstrained values

Use a custom flag when a caller needs several settings together. For example,
a retry loop can read one `RetryPolicy` with an attempt limit and a backoff.

## Start with an object-backed value

Implement `Konstrained.Object` for a value with named fields. The schema in
this example describes those fields for JSON snapshots.

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

## Use the value in a feature

Declare the Kotlin type and its default in the namespace.

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.Namespace

object RetryFlags : Namespace("retry") {
    val retryPolicy by custom<RetryPolicy, Context>(
        default = RetryPolicy(),
    )
}
```

## Other supported shapes

`Konstrained` also supports primitive-backed value classes, array-backed
values, and adapted primitive encodings such as `AsString` or `AsInt`. Reach
for those shapes when the wire form is a single primitive or a single list.

To export or load a policy with its namespace, see
[Load JSON safely](../quickstart/load-json-safely.md).

