# Run your first flag

Enable a new checkout on iOS and keep it off on Android. You need a Kotlin/JVM
project with the Konditional engine. [Add the engine to your project](install.md).

## Declare the flag

Create `src/main/kotlin/Checkout.kt` in your application module. A namespace
groups related flags. Here, `CheckoutFlags` owns one flag, and `Device` supplies
the platform its rule reads.

```kotlin
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.core.Namespace

data class Device(override val platform: Platform) : Context.PlatformContext

object CheckoutFlags : Namespace("checkout") {
    val newCheckout by boolean<Device>(default = false) {
        rule(true) { platforms(Platform.IOS) }
    }
}
```

## Evaluate it

Add this function to the same file. Run `main()` from your IDE.

```kotlin
fun main() {
    for (platform in listOf(Platform.IOS, Platform.ANDROID)) {
        val showNewCheckout: Boolean = CheckoutFlags.newCheckout.evaluate(Device(platform))
        println("$platform: $showNewCheckout")
    }
}
```

```text
IOS: true
ANDROID: false
```

The iOS device matches the rule. The Android device gets the default. Change
the rule to `platforms(Platform.ANDROID)` and run it again to reverse the results.

When you need a second use case, choose a guide from [Advanced usage](../guides/index.md).

