# Typed Variants Instead of Boolean Explosion

When you have multiple rollout variants, model them as a typed value (enum or string) rather than composing booleans.

```kotlin
enum class CheckoutVariant { CLASSIC, FAST_PATH, NEW_UI }

object CheckoutFlags : Namespace("checkout") {
    val variant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC) {
        rule(CheckoutVariant.FAST_PATH) { rampUp { 10.0 } }
        rule(CheckoutVariant.NEW_UI) { rampUp { 1.0 } }
    }
}

fun renderCheckout(context: Context) {
    when (CheckoutFlags.variant.evaluate(context)) {
        CheckoutVariant.CLASSIC -> renderClassic()
        CheckoutVariant.FAST_PATH -> renderFastPath()
        CheckoutVariant.NEW_UI -> renderNewUi()
    }
}
```

- **Guarantee**: Variant values are compile-time correct and exhaustively handled.
- **Mechanism**: Enum-typed feature delegates (`enum<...>`) and Kotlin `when` exhaustiveness.
- **Boundary**: Remote JSON can only select enum constants already compiled into the binary.

---
