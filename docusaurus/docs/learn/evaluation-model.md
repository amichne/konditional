# Evaluation Model

This page explains how Konditional chooses a value when multiple rules exist.

## Total evaluation {#total-evaluation}

### 1. Total: Always returns a value {#1-total-always-returns-a-value}

Evaluation always returns a value. If no rule matches, the default is returned.

## Deterministic evaluation

### 2. Deterministic: Same inputs = same outputs {#2-deterministic-same-inputs--same-outputs}

Given the same inputs and configuration, evaluation yields the same output.

## Evaluation order

When you call `feature.evaluate(context)`:

1. Rules are sorted by **specificity** (highest first).
2. Each rule is checked against the Context.
3. If a rule matches, ramp-up is applied (if configured).
4. The first matching rule that passes ramp-up wins.
5. If nothing matches, the default value is returned.

## Conjunctive rule construction

Within a single rule, targeting criteria compose as a conjunction.

- `platforms(...)`, `locales(...)`, `versions { ... }`, `axis(...)`, and each
  `extension { ... }` block must all match for the rule to match.
- Repeating `axis(...)` for the same axis id merges values with OR semantics
  for that axis.
- Targeting different axis ids still composes with AND semantics.

## Structured targeting model

Konditional evaluates a structural targeting hierarchy, not a flat predicate
chain.

- A rule is modeled as `Targeting.All`.
- Built-in criteria map to leaf nodes (`Locale`, `Platform`, `Version`,
  `Axis`).
- Extension criteria map to `Custom` leaves.
- Capability-narrowed criteria use guarded leaves and return `false` when the
  required context capability is absent.

## Specificity system

Specificity is the sum of targeting leaves.

**Base targeting specificity** (0-3+):

- `locales(...)` adds 1 if non-empty
- `platforms(...)` adds 1 if non-empty
- `versions { ... }` adds 1 if bounded
- `axis(...)` adds 1 per axis constraint

**Custom targeting specificity**:

- Each `extension { ... }` block contributes 1 specificity by default.
- Each `whenContext<R> { ... }` block contributes 1 specificity by default.
- Multiple extension leaves on one rule are AND-composed, and their
  specificity is cumulative.

- **Guarantee**: More specific rules are evaluated before less specific rules.

- **Mechanism**: Rules are sorted by `rule.specificity()` in descending order before evaluation.

- **Boundary**: Ramp-up percentage does not affect specificity.

## Deterministic ramp-ups

Ramp-ups are deterministic and reproducible.

- **Guarantee**: The same `(stableId, featureKey, salt)` always yields the same bucket assignment.

- **Mechanism**:

1. Hash the UTF-8 bytes of `"$salt:$featureKey:${stableId.hexId.id}"` with SHA-256.
2. Convert the first 4 bytes to an unsigned 32-bit integer.
3. Bucket = `hash % 10_000` (range `[0, 9999]`).
4. Threshold = `(rampUp.value * 100.0).roundToInt()` (basis points).
5. In ramp-up if `bucket < threshold`.

- **Boundary**: Changing `stableId`, `featureKey`, or `salt` changes the bucket assignment.

## Example

```kotlin
object AppFeatures : Namespace("app") {
    val checkout by string<Context>(default = "v1") {
        rule("v3") { platforms(Platform.IOS); versions { min(3, 0, 0) } } // specificity 2
        rule("v2") { platforms(Platform.IOS) }                            // specificity 1
        rule("v1") { always() }                                           // specificity 0
    }
}
```

For an iOS user on version 3.1.0, the `v3` rule is evaluated first and wins if it matches.
