# Load remote configuration

Your application fetches a JSON snapshot for one namespace. Pass the payload to `fromJson`. On success, the namespace installs the configuration. On failure, it keeps its previous snapshot.

```kotlin
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.fromJson

fun refreshCheckout(payload: String) {
    when (val result = CheckoutFlags.fromJson(payload)) {
        is ParseResult.Success -> println("Loaded checkout configuration")
        is ParseResult.Failure -> println("Rejected snapshot: ${result.error.message}")
    }
}
```

Handle transport failures and retry decisions in the application. If the payload includes a version, read it from `result.value.metadata.version` on success.
