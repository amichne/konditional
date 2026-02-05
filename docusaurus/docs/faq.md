---
title: FAQ / Pitfalls
---

# FAQ / Pitfalls

## Why is rule order based on specificity, not declaration order?

Because declaration order becomes an implicit policy that breaks composition.
Specificity-based precedence lets you reuse rule sets and include shared policies without “who included what first?”
becoming a production incident.

## Why must axis IDs be stable?

Axis IDs (and axis value IDs) are part of the configuration contract.
If an ID changes (including via obfuscation), existing configuration may no longer target what you think it targets.

## How does salt affect rollout distribution?

Salt is an explicit input to bucketing. Changing it intentionally re-samples the population for the same feature key and
stable ID.

## Why is `explain` more expensive than `evaluate`?

`explain` returns additional data (decision kind, matched rule, bucket info) and may emit debug logging signals via
registry hooks. Use it for debugging and tooling, not for every evaluation on a hot path.

## Why is evaluation always total (never null)?

Defaults are required at definition time, so evaluation always returns a value. This prevents null propagation and
runtime exceptions in feature‑gated code.

## Can I mutate configuration after load?

No. Configuration snapshots are intended to be immutable. Mutating a snapshot breaks the atomic swap model and can
produce inconsistent evaluations.

## Why are configuration updates atomic?

Atomic snapshot replacement ensures readers see either the old or the new config — never a partial update. This keeps
evaluation deterministic under concurrent refresh.
