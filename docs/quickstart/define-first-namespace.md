# Define your first namespace

Checkout accepts payments and reporting products. Lending needs manual review. The default for other products is `NOT_ELIGIBLE`.

Create `src/main/kotlin/CheckoutFlags.kt` in the application. The product axis identifies the product in each request. The rules map that product to a `ProductEligibility` value.

```kotlin
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.axes
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.rules.targeting.scopes.constrain

enum class Product : AxisValue<Product> {
    PAYMENTS,
    LENDING,
    REPORTING,
}

enum class ProductEligibility {
    NOT_ELIGIBLE,
    ELIGIBLE,
    MANUAL_REVIEW,
}

data class CommerceContext(
    val product: Product,
) : Context {
    override val axes = axes(product)
}

object CheckoutFlags : Namespace("checkout") {
    val productEligibility by enum<ProductEligibility, CommerceContext>(
        default = ProductEligibility.NOT_ELIGIBLE,
    ) {
        rule(ProductEligibility.ELIGIBLE) {
            constrain(Product.PAYMENTS, Product.REPORTING)
        }
        rule(ProductEligibility.MANUAL_REVIEW) {
            constrain(Product.LENDING)
        }
    }
}
```

[Evaluate a payments request and a lending request](evaluate-a-typed-flag.md).
