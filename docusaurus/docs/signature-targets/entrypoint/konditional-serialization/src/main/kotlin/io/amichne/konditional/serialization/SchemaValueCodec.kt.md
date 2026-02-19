---
target_id: entrypoint:konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt
scope_sig_paths:
  - konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt.sig
symbol_ids:
  - method:0e94d3014feef5fe
  - method:1c13e0136d289aa0
  - method:686545544b7e7800
  - method:9fc7ba83b6c5e6f8
  - method:c7d35bb0c4b69783
claims:
  - claim_13e1787889e3_01
  - claim_13e1787889e3_02
  - claim_13e1787889e3_03
---

# SchemaValueCodec entrypoint

## Inputs

Input contracts are defined by the signature declarations in this target:

- `fun <T : Any> decodeKonstrainedPrimitive(kClass: KClass<T>, rawValue: Any): Result<T>`
- `fun encodeKonstrained(konstrained: Konstrained<*>): JsonValue`
- `fun <T : Any> encode(value: T, schema: ObjectSchema): JsonObject`
- `fun <T : Any> decode(kClass: KClass<T>, json: JsonObject): Result<T>`
- `fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): Result<T>`

## Outputs

Output contracts are the return types encoded directly in these method
declarations.

## Determinism

This documentation is constrained to signature-level API contracts, where
callable behavior is represented by explicit typed declarations.

## Operational notes

Symbol IDs in this target scope: `method:0e94d3014feef5fe`, `method:1c13e0136d289aa0`, `method:686545544b7e7800`, `method:9fc7ba83b6c5e6f8`, `method:c7d35bb0c4b69783`.
