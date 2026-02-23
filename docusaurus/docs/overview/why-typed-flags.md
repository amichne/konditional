---
title: Why Typed Flags
sidebar_position: 2
---

# Why Typed Flags

String-key feature systems fail in predictable ways under scale. Konditional shifts those failures into compile-time or typed boundary outcomes.

<span id="claim-clm-pr01-03a"></span>
Feature declarations are modeled as typed entities under `Namespace`, `Feature`, and `FlagDefinition` abstractions.

<span id="claim-clm-pr01-03b"></span>
Parse boundary failures are represented as explicit `ParseError` values wrapped by `KonditionalBoundaryFailure`.

## Failure Mode 1: Key Typos

```kotlin
// String-key pattern
val enabled = flags["new-checkot"] as? Boolean ?: false // typo survives review

// Konditional pattern
val enabled: Boolean = CheckoutFeatures.newCheckout.evaluate(ctx)
// CheckoutFeatures.newCheckot -> compile error
```

## Failure Mode 2: Type Coercion Drift

```kotlin
// String-key pattern
val timeout = (flags["timeout"] as String).toInt()

// Konditional pattern
val timeout: Int = CheckoutFeatures.timeout.evaluate(ctx)
```

## Failure Mode 3: Boolean Explosion

String-key systems often multiply booleans for related behavior. Konditional encourages typed values (`enum`, structured types) so related states stay coherent.

## Failure Mode 4: Inconsistent Rollout Assignment

Deterministic bucketing uses stable identity and salt, so the same context yields stable inclusion decisions over time.

## Trade-off

Compile-time safety requires static declarations in source. If your primary requirement is non-code, ad-hoc flag creation by non-developers, you will need internal tooling around this API.

## Next Steps

- [Parse Boundary](/concepts/parse-boundary) - Go deeper on typed boundary failures.
- [Determinism Proofs](/theory/determinism-proofs) - Understand why rollout assignment stays stable.

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-03A | Feature declarations are modeled as typed entities under Namespace and Feature abstractions. |
| CLM-PR01-03B | Parse boundary failures are represented with explicit ParseError and KonditionalBoundaryFailure types. |
