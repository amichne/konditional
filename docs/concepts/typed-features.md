# Typed features

A `Feature<T, C, M>` belongs to namespace type `M`. Its `evaluate` function accepts context type `C` and returns value type `T`.

| Value | Declaration | Example |
| --- | --- | --- |
| Primitive | `boolean`, `string`, `integer`, `double` | Turn a checkout upsell on or off. |
| Enum | `enum` | Select self-service, manual review, or no access. |
| Custom | `custom` with a `Konstrained` type | Return a retry policy with a maximum attempt count and delay. |
