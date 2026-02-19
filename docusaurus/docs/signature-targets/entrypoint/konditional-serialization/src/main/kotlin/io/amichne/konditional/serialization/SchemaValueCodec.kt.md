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
  - claim_13e1787889e3_an01
  - claim_13e1787889e3_an02
  - claim_13e1787889e3_an03
---

# SchemaValueCodec entrypoint

## Inputs

This entrypoint exposes a `boundary transformation surface`. The signature-declared method family
is `decodeKonstrainedPrimitive`, `encodeKonstrained`, `encode`, `decode`, with parameter/shape contracts defined by:

- `fun <T : Any> decodeKonstrainedPrimitive(kClass: KClass<T>, rawValue: Any): Result<T>`
- `fun encodeKonstrained(konstrained: Konstrained<*>): JsonValue`
- `fun <T : Any> encode(value: T, schema: ObjectSchema): JsonObject`
- `fun <T : Any> decode(kClass: KClass<T>, json: JsonObject): Result<T>`
- `fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): Result<T>`

## Outputs

Return projections declared in this surface include `Result<T>`, `JsonValue`, `JsonObject`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `KonditionalInternalApi`, `ParseError`, `parseFailure`, `Konstrained`, `asObjectSchema`.
Category mix for this target: `boundary`.
This surface primarily enables: bidirectional boundary transformations between typed models and serialized representations.
