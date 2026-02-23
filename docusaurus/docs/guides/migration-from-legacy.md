---
title: Migration from Legacy
sidebar_position: 7
---

# Migration from Legacy

Migrate from string-key or enum-key flag systems without changing user-facing behavior during cutover.

**Prerequisites:** You can [load snapshots safely](/quickstart/load-first-snapshot-safely).

## Step 1: Mirror Legacy Flags as Typed Declarations

Declare equivalent features in Konditional, preserving semantic defaults.

## Step 2: Run Dual Evaluation (Baseline + Candidate)

```kotlin
import io.amichne.konditional.api.ShadowOptions
import io.amichne.konditional.api.evaluateWithShadow

val value = AppFeatures.newCheckout.evaluateWithShadow(
  context = ctx,
  candidateRegistry = konditionalCandidate,
  baselineRegistry = legacyBaseline,
  options = ShadowOptions.of(reportDecisionMismatches = true),
) { mismatch ->
  logger.warn("shadow mismatch key=${mismatch.featureKey} kinds=${mismatch.kinds}")
}
```

Use baseline return values in production behavior until mismatch rate is acceptable.

## Step 3: Cutover and Keep Rollback Ready

- Switch caller to Konditional baseline only after mismatch thresholds are met.
- Keep old system available until rollback criteria expire.

## Expected Outcome

After this guide, you can migrate safely using shadow comparison, explicit mismatch reporting, and reversible rollout controls.

## Next Steps

- [Theory: Migration and Shadowing](/theory/migration-and-shadowing)
- [Guide: Enterprise Adoption](/guides/enterprise-adoption)
