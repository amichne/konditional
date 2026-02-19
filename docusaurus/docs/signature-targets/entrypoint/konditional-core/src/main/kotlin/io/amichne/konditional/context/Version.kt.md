---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/context/Version.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/context/Version.kt.sig
symbol_ids:
  - method:0a9b6d526808fdc0
claims:
  - claim_version_compareto_override
  - claim_version_compareto_version_input
  - claim_version_compareto_int_output
---

# Version entrypoint

## Inputs

`compareTo` accepts one argument named `other` with type `Version`.

## Outputs

`compareTo` returns `Int`.

## Determinism

The signature encodes comparison as a typed input-output contract with no
ambient parameters.

## Operational notes

The method is declared as `override`, and its parameter type is exactly
`Version`.
