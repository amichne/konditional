# Why typed flags

Typed feature declarations let you enforce feature contracts at compile time and
keep runtime boundary failures explicit.

## Read this page when

- You need the rationale for typed declarations over string-keyed flags.
- You are defining safety expectations for your rollout system.
- You want to connect API design to boundary reliability.

## Compile-time declaration model

Namespaces and feature declarations encode ownership and value types directly in
the API surface, removing stringly typed identifiers from core logic
[CLM-PR01-03A].

## Boundary error semantics

Snapshot ingestion failures are represented as typed parse errors wrapped in
boundary failures, which makes failure handling explicit and testable
[CLM-PR01-03B].

## Practical implication

You design declarations once and evaluate with deterministic semantics; when
ingestion fails, behavior stays governed by explicit results rather than silent
coercion.

## Next steps

1. Use this model in a concrete setup via
   [Define first flag](/quickstart/define-first-flag).
2. Add boundary-safe ingestion via
   [Load first snapshot safely](/quickstart/load-first-snapshot-safely).
3. Validate the full behavior with
   [Verify end-to-end](/quickstart/verify-end-to-end).

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-03A | Feature declarations are modeled as typed entities under Namespace and Feature abstractions. | `#compile-time-declaration-model` | `/reference/claims-registry#clm-pr01-03a` |
| CLM-PR01-03B | Parse boundary failures are represented with explicit ParseError and KonditionalBoundaryFailure types. | `#boundary-error-semantics` | `/reference/claims-registry#clm-pr01-03b` |
