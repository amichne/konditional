# Why typed flags

Typed feature declarations keep feature state and runtime evaluation aligned
with compile-time contracts [CLM-PR01-03A].

Parse-boundary failures stay explicit and inspectable through typed parse-error
results, so bad payloads do not silently mutate runtime behavior
[CLM-PR01-03B].

## Compile-time declaration model

Namespaces and features encode feature ownership and value types directly in the
API surface [CLM-PR01-03A].

## Boundary error semantics

Boundary parsing failures are represented as explicit parse-error values wrapped
in boundary failure results [CLM-PR01-03B].

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-03A | Feature declarations are modeled as typed entities under Namespace and Feature abstractions. | `#compile-time-declaration-model` | `/reference/claims-registry#clm-pr01-03a` |
| CLM-PR01-03B | Parse boundary failures are represented with explicit ParseError and KonditionalBoundaryFailure types. | `#boundary-error-semantics` | `/reference/claims-registry#clm-pr01-03b` |
