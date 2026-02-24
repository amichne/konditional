---
title: Type Safety Boundaries
sidebar_position: 1
---

# Type Safety Boundaries

## Invariant

For declared features, evaluation is compile-time type-safe at call sites: feature key, value type, and context type are bound through Kotlin generics.

## What Is Guaranteed

- Declaration-time typing of values (`boolean`, `integer`, `enum`, `custom`).
- Evaluation return type consistency (`Feature<T, ...>.evaluate(...) -> T`).
- Namespace ownership encoded in type relationships.

## What Is Not Guaranteed

- Business correctness of rule criteria.
- Validity of untrusted JSON before boundary parsing.
- Runtime values outside declared schema.

## Boundary Contract

Untrusted inputs are parsed into typed results; invalid payloads become `ParseError` and do not enter trusted runtime state.

## Test Evidence

| Test | Evidence |
| --- | --- |
| `FlagEntryTypeSafetyTest` | Declared feature/value type combinations enforce compile-time and runtime-safe shape. |
| `BoundaryFailureResultTest` | Boundary failures are typed and carried through `Result` failure channel. |

## Next Steps

- [Concept: Features and Types](/concepts/features-and-types)
- [Theory: Parse Don\'t Validate](/theory/parse-dont-validate)
