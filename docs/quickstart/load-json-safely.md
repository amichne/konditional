# Load JSON safely

Add the [JSON dependency](install.md#add-json-support). Create `src/main/kotlin/Snapshots.kt`.

Export the checkout configuration with `toJson()`. On import, `fromJson` loads the decoded configuration or returns a typed error. A failed import keeps the active configuration.

```kotlin
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.fromJson
import io.amichne.konditional.serialization.toJson

fun checkSnapshots() {
    val json = CheckoutFlags.toJson()
    val loaded = CheckoutFlags.fromJson(json)
    check(loaded is ParseResult.Success)
    check(CheckoutFlags.toJson() == json)

    val before = CheckoutFlags.currentSnapshot
    val rejected = CheckoutFlags.fromJson("{ not-json")
    check(rejected is ParseResult.Failure)
    check(CheckoutFlags.currentSnapshot == before)
}
```

[Run the examples](verify-behavior.md).
