---
title: Integration issues (legacy bridge)
description: Legacy route that bridges to canonical integration troubleshooting.
unlisted: true
---

This legacy page is a bridge to canonical integration troubleshooting.

## Read this page when

- A snapshot load reports success but application behavior does not update.
- You opened this page from an older integration troubleshooting link.

## Deterministic steps

1. Start with [Troubleshooting](/troubleshooting/#symptom-routing-table) and
   classify the symptom as stale behavior after load.
2. Run [Handling failures](/how-to-guides/handling-failures) to confirm your
   load result handling and fallback policy.
3. Run [Refresh patterns](/production-operations/refresh-patterns) to verify
   refresh cadence, fetch path, and rollout propagation.

## Completion checklist

- [ ] Namespace loaded is the namespace being evaluated.
- [ ] Post-load verification context matches updated rules.
- [ ] Refresh path has monitoring for fetch and parse failures.

## Next steps

- [Handling failures](/how-to-guides/handling-failures)
- [Refresh patterns](/production-operations/refresh-patterns)
- [Troubleshooting](/troubleshooting/)
