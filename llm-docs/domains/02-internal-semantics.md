# Context: Konditional Internal Semantics

You are documenting the internal behavioral semantics of Konditional. Your audience is developers who need to understand *how* the system behaves, not just *how to use* it.

## Scope

Focus on explaining:

- **Rule specificity calculation**: How criteria count determines rule priority
- **Specificity ordering**: How rules are sorted before evaluation
- **Evaluation algorithm**: Rule iteration, matching, first-match-wins, fallback to default
- **SHA-256 bucketing**: Input construction (`stableId + flagKey + salt`), hash computation, percentage mapping
- **Per-flag isolation**: Why flags don't interfere with each other's bucket assignments
- **Salt mechanics**: Purpose, when redistribution occurs, how to trigger re-bucketing
- **Criteria matching**: How platform, locale, version, and rollout criteria are evaluated

## Out of Scope (defer to other domains)

- API syntax and DSL usage → See `01-public-api.md`
- Type safety proofs → See `03-type-safety-theory.md`
- Thread-safety implementation → See `04-reliability-guarantees.md`
- JSON serialization → See `05-configuration-integrity.md`

## Audience

Developers who need to:
- Debug unexpected flag evaluations
- Reason about rollout percentages and user distribution
- Understand why the same user always gets the same result
- Predict behavior under edge cases
- Explain system behavior to stakeholders

## Key Invariants to Document

1. **Specificity ordering is deterministic and stable**: Given the same set of rules, they will always be evaluated in the same order.

2. **Bucketing is deterministic**: Same `(stableId, flagKey, salt)` → same bucket value, always, on any platform.

3. **Bucket distribution is uniform**: Users are distributed evenly across the `[0, 100)` range (within statistical bounds).

4. **First match wins**: Rules are evaluated in specificity-descending order; the first rule whose criteria all match determines the result.

5. **Default is the final fallback**: If no rules match, the feature's declared default value is returned.

## Evaluation Algorithm (Pseudocode)

Document this algorithm precisely:

```
evaluate(feature, context):
    rules = feature.rules.sortedByDescending { it.specificity }
    for rule in rules:
        if rule.matches(context):
            return rule.value
    return feature.default
```

## Bucketing Algorithm (Pseudocode)

Document this algorithm precisely:

```
bucket(stableId, flagKey, salt):
    input = "$stableId:$flagKey:$salt"
    hash = SHA256(input.toByteArray())
    value = hash.first8Bytes.asLong().absoluteValue
    percentage = (value % 10000) / 100.0  # [0.00, 100.00)
    return percentage
```

## Specificity Calculation

Document how specificity is computed:

```
specificity(rule):
    count = 0
    if rule.platforms.isNotEmpty(): count++
    if rule.locales.isNotEmpty(): count++
    if rule.versions.hasConstraints(): count++
    if rule.rollout != null: count++
    return count
```

## Constraints

- Use precise language; avoid "usually" or "typically" for deterministic behaviors
- Include pseudocode for algorithms
- Explain *why* design decisions were made, not just *what* they are
- Address edge cases explicitly (empty rule sets, 0% rollout, 100% rollout)

## Example Scenarios to Address

1. **Two rules with same specificity**: Document tie-breaking behavior (definition order? explicit priority?)

2. **Rollout at boundary**: User whose bucket is exactly 50.0 with a 50% rollout—included or excluded?

3. **Empty criteria**: A rule with no platforms, locales, or versions specified—what does it match?

4. **Salt change**: What happens to user bucket assignments when salt changes?

## Context Injection Point

When explaining specific algorithms, inject implementation details here:

```
[INSERT: Relevant source code snippets for bucketing, matching, or evaluation]
```

## Output Format

For internal documentation, produce:
1. Clear statement of the behavior being documented
2. Algorithm or pseudocode (if applicable)
3. Explanation of why this design was chosen
4. Edge cases and their handling
5. Implications for users of the library
