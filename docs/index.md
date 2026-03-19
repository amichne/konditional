# Konditional

Konditional gives Kotlin teams a typed way to evaluate dynamic feature flags
without falling back to string keys, silent coercion, or opaque runtime
behavior. You define features as Kotlin properties inside a namespace,
evaluate them against typed contexts, and keep compile-time guarantees visible
at the call site.

## Why engineers adopt it

Konditional is useful when you want dynamic behavior without handing the whole
problem over to runtime strings and conventions. It keeps the important
guarantees in code.

- **Typed definitions and typed results.** `enum`, `boolean`, `integer`, and
  `custom` flags stay typed from definition to call site.
- **Readable rules.** Rules live beside the flag definition and read like a
  small Kotlin DSL.
- **Deterministic evaluation.** The same context and the same snapshot produce
  the same result.
- **Namespace isolation.** Flags are scoped to a `Namespace`, so teams can keep
  ownership local instead of creating a shared global registry too early.
- **Atomic runtime updates.** When you introduce runtime loading later, readers
  observe a whole snapshot, not partial state.

## A small, typed example

The quickest way to see the difference is to start with a real domain value,
not a boolean. A typed eligibility flag can return an enum that the compiler
forces you to handle explicitly.

```kotlin
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

val eligibility: ProductEligibility =
    CheckoutFlags.productEligibility.evaluate(context)
```

## Start focused

The cleanest adoption path is a single namespace inside a team-owned Gradle
module. Keep the flag definitions close to the code that owns the decision,
ship one product or workflow behind it, and let the first caller evaluate a
typed result. If another team later needs the same contract, extract the
shared namespace or shared enums into a common module at that point. Start
with one namespace inside a team-owned module, sharing via a common module
only if required, but keep the surface focused where possible.

## What Konditional guarantees

Konditional is designed for teams that care about architectural behavior, not
just syntax. The current public surface and tests back these guarantees.

- **Compile-time binding.** Feature definitions are Kotlin properties on
  `Namespace`, not runtime string lookups.
- **Deterministic bucketing and diagnostics.** Evaluation and `explain(...)`
  reuse the same semantics.
- **Atomic namespace state.** Runtime loads exchange whole snapshots.
- **Isolated namespaces.** The same feature name can exist in separate
  namespaces without cross-talk.

## Next steps

The quickstart shows the full example with imports, a minimal context, and a
recommended monorepo placement.

- [Evaluate a typed eligibility flag](quickstart/typed-eligibility.md)
