# Evaluate a typed eligibility flag

This quickstart gets you from a team-owned Gradle module to evaluating a typed
eligibility flag. It stays on the smallest supported path: inline rules,
typed contexts, and direct `evaluate(...)` calls. Remote config and JSON
parsing come later.

## Add Konditional to one module

Start in the Gradle module that owns the feature and the namespace. Keep the
first adoption local to one team-owned module, and only extract shared flag
definitions into a common module when another team truly needs the same typed
contract.

```kotlin
dependencies {
    implementation("io.amichne.konditional:konditional-engine:<version>")
}
```

That dependency is enough to define flags and evaluate them. Add
`konditional-json` later when you are ready to parse external snapshots.

## Model the product dimension

Start with a typed domain value that matters to the business. In this example,
the product itself is part of the evaluation context, and the flag returns a
typed eligibility result instead of another boolean.

```kotlin
import io.amichne.konditional.context.axis.AxisValue

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
```

This shape is the real advantage. Callers receive a domain value that the
compiler can track, and `when` statements stay exhaustive.

## Build a minimal context

The public `Context` API is intentionally small. For this quickstart, the
context only needs to carry a product axis.

```kotlin
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.axes

data class CommerceContext(
    val product: Product,
) : Context {
    override val axes = axes(product)
}
```

You can add locale, platform, version, or stable ID mix-ins later when the
flag needs them. You do not need them for a product-only rule.

## Declare the namespace and flag

Define the namespace in the same module that owns the feature. The flag stays
typed from declaration through evaluation.

```kotlin
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.rules.targeting.scopes.constrain

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

Inline declarations are immediately evaluable. You do not need a separate
runtime load step to start using the namespace.

## Evaluate the flag

Evaluation returns the declared enum type directly. That keeps the result
readable in the caller and avoids encoding domain states as boolean control
flow.

```kotlin
import io.amichne.konditional.api.evaluate

val paymentsContext = CommerceContext(product = Product.PAYMENTS)
val lendingContext = CommerceContext(product = Product.LENDING)

val paymentsEligibility: ProductEligibility =
    CheckoutFlags.productEligibility.evaluate(paymentsContext)

val lendingEligibility: ProductEligibility =
    CheckoutFlags.productEligibility.evaluate(lendingContext)
```

At the call site, use the enum as a normal Kotlin value.

```kotlin
when (CheckoutFlags.productEligibility.evaluate(paymentsContext)) {
    ProductEligibility.ELIGIBLE -> showSelfServePath()
    ProductEligibility.MANUAL_REVIEW -> routeToOpsReview()
    ProductEligibility.NOT_ELIGIBLE -> hideExperience()
}
```

## Recommended placement in a monorepo

The cleanest rollout is one namespace in one team-owned module. That keeps the
ownership line obvious and avoids inventing a shared flag catalog too early.

```text
:checkout:flags
  Product.kt
  ProductEligibility.kt
  CommerceContext.kt
  CheckoutFlags.kt
```

Keep that module focused on the team that owns the decision. Share via a
common module only if another team needs the same typed namespace or the same
domain enums. If two teams need different ownership or release cadence, keep
their namespaces separate, even when the feature names look similar.

## Next steps

This quickstart stops before remote config on purpose. Once the typed
evaluation path feels right, the next step is to add parsed external snapshots
at the boundary instead of reworking the namespace model itself.

- [Back to the overview](../index.md)
