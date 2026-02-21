# Product value and fit

Use this page to decide whether Konditional fits your technical and operational
constraints.

## What you will achieve

You will:

- Evaluate Konditional against concrete adoption criteria;
- Identify where Konditional is a strong fit and where it is not;
- Decide your pilot scope.

## Prerequisites

You need to know your team goals for feature delivery, experimentation, and
runtime operations.

## Main content

Konditional is best when you need correctness and operational predictability
from feature configuration.

## Strong-fit scenarios

Konditional is a strong fit when you need:

- Compile-time safety for feature keys and value types;
- Deterministic rollouts tied to stable user identity;
- Explicit runtime parse failures instead of implicit coercion;
- Namespace isolation across teams or domains;
- Migration-safe shadow evaluation.

## Poor-fit scenarios

Konditional is likely not the right fit when you need:

- Fully dynamic, unknown-at-compile-time feature schemas;
- Vendor-hosted experimentation UI as the primary requirement;
- Runtime type flexibility over compile-time contracts.

## Decision checklist

Use this checklist before adopting:

1. Do you want typed values beyond booleans?
2. Do you require deterministic rollout behavior?
3. Do you need explicit parse error handling for remote config?
4. Do you need namespace-level blast-radius isolation?
5. Can your team declare core features in code?

If most answers are yes, run a pilot using the [Quickstart](/quickstart/).

## Recommended pilot scope

Start with one namespace and one non-critical flow:

1. define one boolean and one enum feature;
2. add a 10% ramp-up rule with stable IDs;
3. load one remote snapshot through `NamespaceSnapshotLoader`;
4. instrument parse failures and rollout metrics.

## Next steps

- [Why typed flags](/overview/why-typed-flags)
- [Adoption roadmap](/overview/adoption-roadmap)
- [Quickstart](/quickstart/)
