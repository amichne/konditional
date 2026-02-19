# Core Concepts

This page defines the minimum vocabulary for reading Konditional guarantees and
operating runtime configuration safely.

## Namespace

A `Namespace` owns feature declarations, runtime lifecycle operations, and
isolation scope.

## Feature

A `Feature<T, C, M>` is a typed value contract with defaults and conditional
rules evaluated against `C`.

## Context

`Context` is the runtime input envelope for targeting and rollout decisions.

## Stable identifier and bucketing

`stableId` participates in deterministic ramp-up bucketing. Same identity tuple
yields stable assignment for a fixed snapshot.

## Compile-time versus boundary safety

- Compile-time domain: typed feature access and typed evaluation calls.
- Boundary domain: parse and materialization from untrusted payloads via
  `Result`.

## Related

- [Type safety](/learn/type-safety)
- [Evaluation model](/learn/evaluation-model)
- [Parse don’t validate](/theory/parse-dont-validate)

## Claim ledger

| claim_id | claim_statement | claim_kind | status |
| --- | --- | --- | --- |
| LRN-001-C1 | Namespaces are first-class registries that own lifecycle and evaluation scope. | mechanism | supported |
| LRN-001-C2 | Feature values flow through explicit type parameters instead of runtime casts. | guarantee | supported |
| LRN-001-C3 | Context is an explicit runtime input model for deterministic evaluation. | mechanism | supported |
| LRN-001-C4 | Runtime configuration ingestion is explicitly modeled as a result boundary. | boundary | supported |
