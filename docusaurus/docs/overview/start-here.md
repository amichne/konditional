# Start here

Use this page to choose the shortest path to your first successful outcome with
Konditional.

## What you will achieve

You will:

- understand what Konditional solves and where it fits;
- pick a first-success path based on your current goal;
- know which pages to read next for implementation depth.

## Prerequisites

You need basic Kotlin and Gradle familiarity. You do not need prior
Konditional experience.

## Main content

Konditional is a compile-time-safe feature flag and configuration system for
Kotlin. Its model is strict:

- flags are typed Kotlin properties, not runtime string keys;
- evaluation is deterministic for the same context and snapshot;
- remote input crosses an explicit parse boundary and returns typed failures.

For implementation details, see the theory pages:

- [Type safety boundaries](/theory/type-safety-boundaries)
- [Determinism proofs](/theory/determinism-proofs)
- [Atomicity guarantees](/theory/atomicity-guarantees)

## Choose your first-success path

1. If you want to ship one flag quickly, start with
   [Quickstart](/quickstart/).
2. If you need runtime JSON updates first, go to
   [Load first snapshot safely](/quickstart/load-first-snapshot-safely).
3. If you are evaluating team adoption, go to
   [Product value and fit](/overview/product-value-fit).
4. If you are migrating from another system, go to
   [Adoption roadmap](/overview/adoption-roadmap).

## Core guarantees and boundaries

- **Total evaluation:** A declared feature always returns a typed value.
- **Determinism:** Same context and same active snapshot produce the same
  result.
- **Atomic updates:** Runtime readers observe old or new snapshots, never
  partial state.
- **Boundary safety:** Untrusted JSON is parsed before ingest and can fail with
  typed errors.

These guarantees assume stable identifiers and valid namespace definitions.

## Next steps

- [Product value and fit](/overview/product-value-fit)
- [Why typed flags](/overview/why-typed-flags)
- [First success map](/overview/first-success-map)
- [Quickstart](/quickstart/)
