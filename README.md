# Konditional

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Codebase](https://github.com/amichne/konditional/actions/workflows/ci.yml/badge.svg)](https://github.com/amichne/konditional/actions/workflows/ci.yml)

Konditional is a feature-flag library for Kotlin/JVM. A namespace declares each feature's context, rules, and return type. Callers get a Kotlin value from `evaluate`.

[Install the engine and run the checkout example](docs/quickstart/install.md).

## Evaluate checkout eligibility

Payments and reporting qualify for checkout. Lending needs manual review.

```kotlin
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.AxisValue
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

val eligibility =
    CheckoutFlags.productEligibility.evaluate(CommerceContext(Product.PAYMENTS))
```

Here, `eligibility` is `ProductEligibility.ELIGIBLE`. An exhaustive `when` can handle each eligibility value.

## Adopt one namespace

Keep the first namespace, its types, and its caller in the Gradle module that owns the decision. If another module needs the same contract, move the shared types or namespace into a common module.

Use `konditional-engine` for code-defined features. Add `konditional-json` to import or export snapshots.

- [Decide whether Konditional fits](docs/start-here/when-it-fits.md).
- [Read the runtime guarantees](docs/guarantees/index.md).
- [Look up the public API](docs/reference/api-surface.md).

## Project resources

- [Contributing guide](CONTRIBUTING.md)
- [Code of conduct](CODE_OF_CONDUCT.md)
- [Changelog](CHANGELOG.md)
- [License](LICENSE)
