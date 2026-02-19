---
target_id: entrypoint:kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaFactoryFunctions.kt
scope_sig_paths:
  - kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaFactoryFunctions.kt.sig
symbol_ids:
  - method:46463068f9ba4c50
claims:
  - claim_63194af1d492_an01
  - claim_63194af1d492_an02
  - claim_63194af1d492_an03
---

# JsonSchemaFactoryFunctions entrypoint

## Inputs

This entrypoint exposes a `construction/composition surface`. The signature-declared method family
is `discriminator`, with parameter/shape contracts defined by:

- `fun discriminator(builder: OneOfDiscriminatorBuilder.() -> Unit)`

## Outputs

Return projections declared in this surface include `(not-explicit-in-signature-snippet)`. When
multiple return projections are present, they define complementary
entrypoints within the same target-scoped API seam.

## Determinism

Determinism at this layer comes from explicit, typed callable signatures:
inputs and output types are declared up front, and there are no ambient
runtime parameters encoded in the symbol surface.

## Operational notes

Linked contract types visible from signatures: `FieldSchema`, `JsonSchema`, `MapSchema`, `ObjectSchema`, `OneOfSchema`.
Category mix for this target: `construct`.
This surface primarily enables: composable schema/value construction flows through constrained DSL entrypoints.
