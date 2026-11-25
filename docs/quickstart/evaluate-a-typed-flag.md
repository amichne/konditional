# Use an enum result

The [product eligibility flag](define-first-namespace.md) returns a
`ProductEligibility`. Use an exhaustive `when` to select the checkout message.
If you add another enum entry later, the compiler will flag the missing case here.

```kotlin
import io.amichne.konditional.api.evaluate

fun checkoutMessage(product: Product): String {
    val eligibility = EligibilityFlags.productEligibility.evaluate(CommerceContext(product))
    return when (eligibility) {
        ProductEligibility.ELIGIBLE -> "Continue to checkout"
        ProductEligibility.MANUAL_REVIEW -> "Request a review"
        ProductEligibility.NOT_ELIGIBLE -> "Checkout unavailable"
    }
}
```

For `Product.LENDING`, the message is `Request a review`.

