# Deterministic Ramp-Up with Resettable Salt

Gradually roll out a feature without reshuffling users; use `salt(...)` when you need a clean resample.

```kotlin
object RampUpFlags : Namespace("ramp-up") {
    val newCheckout by boolean<Context>(default = false) {
        salt("v1")
        rule(true) { rampUp { 10.0 } }
    }
}

fun isCheckoutEnabled(context: Context): Boolean =
    RampUpFlags.newCheckout.evaluate(context)
```

To restart the experiment with a fresh sample:

{{recipe-2-reset}}

- **Guarantee**: Same `(stableId, flagKey, salt)` always yields the same bucket.
- **Mechanism**: SHA-256 deterministic bucketing in `RampUpBucketing`.
- **Boundary**: Changing `salt` intentionally redistributes buckets.

---
