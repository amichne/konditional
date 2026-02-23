---
title: Features and Types
sidebar_position: 2
---

# Features and Types

A feature declaration is a type-level contract: value type, context type, and namespace type are bound together.

## Core Shape

`Feature<T, C, M>` means:

- `T`: value returned by `evaluate(...)`
- `C`: required context type
- `M`: owning namespace type

## Built-in Feature Value Kinds

- `boolean<Context>(default = false)`
- `string<Context>(default = "...")`
- `integer<Context>(default = 0)`
- `double<Context>(default = 0.0)`
- `enum<MyEnum, Context>(default = MyEnum.X)`
- `custom<MyKonstrainedType, Context>(default = MyKonstrainedType(...))`

## Example

```kotlin
enum class CheckoutVariant { CLASSIC, SMART }

object AppFeatures : Namespace("app") {
  val darkMode by boolean<Context>(default = false)
  val retryLimit by integer<Context>(default = 3)
  val checkoutVariant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CLASSIC)
}
```

## Trade-off

You get compile-time guarantees only for declared features. Dynamic feature creation at runtime is intentionally outside this model.

## Next Steps

- [Custom Structured Values Guide](/guides/custom-structured-values) - Add schema-backed custom value types.
- [Type Safety Boundaries](/theory/type-safety-boundaries) - Formal guarantee limits.
