---
target_id: entrypoint:konditional-core/src/main/kotlin/io/amichne/konditional/context/RampUp.kt
scope_sig_paths:
  - konditional-core/src/main/kotlin/io/amichne/konditional/context/RampUp.kt.sig
symbol_ids:
  - method:5b077fe70be1e2a9
claims:
  - claim_rampup_compareto_override
  - claim_rampup_compareto_number_input
  - claim_rampup_compareto_int_output
---

# RampUp entrypoint

## Inputs

`compareTo` accepts one argument named `other` with type `Number`.

## Outputs

`compareTo` returns `Int`.

## Determinism

The API surface for this entrypoint is pure value comparison by explicit input
and return types.

## Operational notes

The method is declared as `override`, which anchors it to inherited comparison
behavior.
