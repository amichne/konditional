# Evaluate a typed flag

Create `src/main/kotlin/Eligibility.kt`. Pass a product context to `evaluate`. The compiler checks the return type against `ProductEligibility`.

```kotlin
import io.amichne.konditional.api.evaluate

fun eligibility(product: Product): ProductEligibility =
    CheckoutFlags.productEligibility.evaluate(CommerceContext(product))

fun checkoutRoute(product: Product): String =
    when (eligibility(product)) {
        ProductEligibility.ELIGIBLE -> "self-service"
        ProductEligibility.MANUAL_REVIEW -> "manual review"
        ProductEligibility.NOT_ELIGIBLE -> "unavailable"
    }
```

The `when` expression covers every eligibility value. If you add an enum entry, the compiler identifies the missing branch.
