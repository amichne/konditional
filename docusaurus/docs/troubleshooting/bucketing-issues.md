---
title: Bucketing issues (legacy bridge)
description: Legacy route that bridges to canonical bucketing diagnosis.
unlisted: true
---

This legacy page is a bridge to canonical bucketing troubleshooting.

## Read this page when

- You followed an old link for bucketing or ramp-up instability.
- The same user appears to switch rollout state unexpectedly.

## Deterministic steps

1. Start with [Troubleshooting](/troubleshooting/#symptom-routing-table) and
   select the bucketing symptom route.
2. Run [Debugging determinism](/how-to-guides/debugging-determinism) to verify
   `stableId`, `salt`, and bucket calculations.
3. Escalate to [Operational debugging](/production-operations/debugging) if the
   issue persists in production traffic.

## Completion checklist

- [ ] Deterministic tuple `(stableId, featureKey, salt)` is stable.
- [ ] Ramp percentages are cumulative and rule ordering is explicit.
- [ ] A determinism regression test was added.

## Next steps

- [Debugging determinism](/how-to-guides/debugging-determinism)
- [Operational debugging](/production-operations/debugging)
- [Troubleshooting](/troubleshooting/)
