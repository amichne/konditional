---
target_id: entrypoint:kontracts/src/main/kotlin/io/amichne/kontracts/dsl/RootObjectSchemaBuilder.kt
scope_sig_paths:
  - kontracts/src/main/kotlin/io/amichne/kontracts/dsl/RootObjectSchemaBuilder.kt.sig
symbol_ids:
  - method:6103f348125bdfa6
  - method:e1154b5926906d03
  - method:e4577ddbdc34c72c
claims:
  - claim_a90cac89b0c0_an01
  - claim_a90cac89b0c0_an02
  - claim_a90cac89b0c0_an03
---

# RootObjectSchemaBuilder entrypoint

## Inputs

This entrypoint exposes a `mutation/build surface`. The signature-declared method family
is `required`, `build`, `optional`, with parameter/shape contracts defined by:

- `fun required( name: String, schema: JsonSchema<*>, description: String? = null, defaultValue: Any? = null, deprecated: Boolean = false )`
- `fun build(): ObjectSchema`
- `fun optional( name: String, schema: JsonSchema<*>, description: String? = null, defaultValue: Any? = null, deprecated: Boolean = false )`

## Outputs

Return projections declared in this surface include `ObjectSchema`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `FieldSchema`, `JsonSchema`, `ObjectSchema`.
Category mix for this target: `construct, write`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
