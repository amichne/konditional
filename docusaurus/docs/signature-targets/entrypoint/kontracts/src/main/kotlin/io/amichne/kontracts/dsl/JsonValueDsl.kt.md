---
target_id: entrypoint:kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonValueDsl.kt
scope_sig_paths:
  - kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonValueDsl.kt.sig
symbol_ids:
  - method:2e65dc295699d9e1
  - method:3a290e0399b5f1e1
  - method:49c46cb4a299cc48
  - method:55c9f37ce20557da
  - method:6ddb0f2b29ee47a7
  - method:73d4e09f15448906
  - method:a755b58690ad585d
  - method:c1b2463e2f6d98a6
  - method:c52f983434247cac
  - method:d1d98e4ce51a12ec
  - method:e21aaf1dc04ed0d5
  - method:ebf0dd7eb7cfc463
  - method:ff6d50a65f426b1a
claims:
  - claim_213cb73659e9_an01
  - claim_213cb73659e9_an02
  - claim_213cb73659e9_an03
---

# JsonValueDsl entrypoint

## Inputs

This entrypoint exposes a `mutation/build surface`. The signature-declared method family
is `array`, `element`, `boolean`, `field`, `fields`, `number`, `obj`, `string`, `elements`, `nullValue`, with parameter/shape contracts defined by:

- `fun array(builder: JsonArrayBuilder.() -> Unit): JsonArray`
- `fun element(value: JsonValue)`
- `fun boolean(value: Boolean): JsonBoolean`
- `fun field(name: String, builder: JsonValueScope.() -> JsonValue)`
- `fun fields(values: Map<String, JsonValue>)`
- `fun number(value: Double): JsonNumber`
- `fun obj(builder: JsonObjectBuilder.() -> Unit): JsonObject`
- `fun number(value: Int): JsonNumber`
- `...`

## Outputs

Return projections declared in this surface include `JsonArray`, `JsonBoolean`, `JsonNumber`, `JsonObject`, `JsonString`, `JsonNull`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `JsonSchema`, `ObjectSchema`, `JsonArray`, `JsonBoolean`, `JsonNull`.
Category mix for this target: `construct, write`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
