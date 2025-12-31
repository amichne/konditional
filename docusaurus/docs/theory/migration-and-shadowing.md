# Migration and Shadowing

How `evaluateWithShadow` enables safe A/B testing of configuration changes and migration between flag systems.

---

## The Migration Problem

When updating configuration or migrating between flag systems, you need confidence that the new system produces the same results as the old system.

**Traditional approach:**
1. Deploy new config
2. Hope it works
3. Monitor for issues
4. Rollback if problems detected

**Issues:**
- No advance warning of mismatches
- Production is the first test
- Rollback is reactive (damage may already be done)

---

## Shadow Evaluation

**Shadow evaluation** means: evaluate against two configurations (primary + shadow) and compare results without affecting production.

```kotlin
val primaryValue = feature.evaluate(context)  // Returned to caller

val shadowValue = evaluateAgainstShadowConfig(feature, context)  // For comparison only

if (primaryValue != shadowValue) {
    logMismatch(feature, context, primaryValue, shadowValue)
}

return primaryValue  // Production uses primary
```

**Key insight:** Production behavior is unchanged (primary value is returned), but mismatches are logged for analysis.

---

## Konditional's Shadow API

### `evaluateWithShadow(context, shadowOptions): T`

Evaluate against primary (returned) and shadow (comparison only):

```kotlin
val shadowConfig = SnapshotSerializer.fromJson(candidateJson).value

val value = AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    shadowOptions = ShadowEvaluationOptions(
        shadowRegistry = shadowRegistry,
        onMismatch = { mismatch ->
            logger.warn("""
                Shadow mismatch:
                Feature: ${mismatch.featureKey}
                Primary: ${mismatch.primary}
                Shadow: ${mismatch.shadow}
                User: ${mismatch.context.stableId}
            """.trimIndent())
        }
    )
)

// value is from primary registry (production unchanged)
applyDarkMode(value)
```

**Behavior:**
1. Evaluate against primary registry → `primaryValue`
2. Evaluate against shadow registry → `shadowValue`
3. If `primaryValue ≠ shadowValue`, invoke `onMismatch` callback
4. Return `primaryValue` (production unaffected)

---

## Use Case 1: Configuration Changes

You want to update ramp-up percentages or targeting criteria. Before rolling out, validate that the new config produces expected results.

### Example: Increasing Ramp-Up

**Current config:**
```kotlin
val newFeature by boolean<Context>(default = false) {
    rule(true) { rampUp { 10.0 } }  // 10% rollout
}
```

**Candidate config (JSON):**
```json
{
  "flags": [
    {
      "key": "feature::app::newFeature",
      "rules": [
        { "value": { "type": "BOOLEAN", "value": true }, "rampUp": 25.0 }
      ]
    }
  ]
}
```

**Shadow evaluation:**
```kotlin
val candidateConfig = SnapshotSerializer.fromJson(candidateJson).value
val candidateRegistry = NamespaceRegistry.from(candidateConfig)

// Evaluate sample of users
users.forEach { user ->
    val ctx = buildContext(user)

    val mismatch = AppFeatures.newFeature.evaluateShadow(
        context = ctx,
        shadowOptions = ShadowEvaluationOptions(shadowRegistry = candidateRegistry)
    )

    mismatch?.let {
        logger.info("User ${user.id}: primary=${it.primary}, shadow=${it.shadow}")
    }
}
```

**Analysis:**
- Users with `primary=false, shadow=true` → will be newly enabled by the candidate config
- Verify this matches the expected 15% increase (10% → 25%)

---

## Use Case 2: Migration Between Flag Systems

You're migrating from another flag system to Konditional. You want to verify that Konditional produces the same results as the old system.

### Migration Flow

1. **Define flags in Konditional** (statically)
2. **Load baseline config from old system** (via JSON)
3. **Run shadow evaluation** (old system = primary, Konditional = shadow)
4. **Log mismatches**, investigate differences
5. **Once confident**, flip: Konditional = primary, old system = shadow
6. **Monitor for regressions**
7. **Decommission old system**

### Example

```kotlin
// Old system (primary)
val oldSystemValue = oldFlagClient.getBool("dark_mode", false)

// Konditional (shadow)
val mismatch = AppFeatures.darkMode.evaluateShadow(
    context = context,
    shadowOptions = ShadowEvaluationOptions(
        shadowRegistry = konditionalRegistry,
        onMismatch = { m ->
            logger.error("Migration mismatch: old=${oldSystemValue}, konditional=${m.shadow}")
        }
    )
)

// Use old system value for now (primary)
applyDarkMode(oldSystemValue)
```

**Progression:**
- **Phase 1:** Old system = primary, Konditional = shadow (log mismatches)
- **Phase 2:** Konditional = primary, old system = shadow (verify no regressions)
- **Phase 3:** Decommission old system

---

## Mechanism: Dual Evaluation

### Implementation (Simplified)

```kotlin
fun <T : Any> Feature<T>.evaluateWithShadow(
    context: Context,
    shadowOptions: ShadowEvaluationOptions
): T {
    val primaryValue = this.evaluate(context)  // Primary registry

    val shadowValue = shadowOptions.shadowRegistry
        .getFlag(this.key)
        ?.evaluate(context)

    if (shadowValue != null && primaryValue != shadowValue) {
        shadowOptions.onMismatch(ShadowMismatch(
            featureKey = this.key,
            primary = primaryValue,
            shadow = shadowValue,
            context = context
        ))
    }

    return primaryValue  // Always return primary (production unchanged)
}
```

**Guarantees:**
1. Primary value is returned (production behavior unchanged)
2. Shadow evaluation happens **after** primary (if shadow throws, primary is unaffected)
3. Mismatch callback is invoked **after** both evaluations (no impact on latency if callback is async)

---

## Performance Considerations

### Overhead

Shadow evaluation doubles the evaluation work:
- Primary evaluation: ~O(n) where n = rules per flag
- Shadow evaluation: ~O(n) where n = rules per flag
- Total: ~O(2n)

**Mitigations:**
1. **Sampling** — Only shadow-evaluate a percentage of requests
2. **Async logging** — `onMismatch` callback should be non-blocking
3. **Time-boxing** — Run shadow evaluation for limited time period (e.g., 24 hours)

### Example: Sampled Shadow Evaluation

```kotlin
val shouldShadow = Random.nextDouble() < 0.10  // 10% sampling

val value = if (shouldShadow) {
    AppFeatures.darkMode.evaluateWithShadow(context, shadowOptions)
} else {
    AppFeatures.darkMode.evaluate(context)
}
```

---

## Mismatch Analysis

### Common Causes of Mismatches

1. **Ramp-up percentage changed** — Users move in/out of ramp-up
2. **Targeting criteria changed** — Rules match different users
3. **Rule ordering changed** — Different rule wins due to specificity
4. **Salt changed** — Bucket assignment redistributed
5. **Configuration drift** — Shadow config is stale

### Debugging Mismatches

```kotlin
val mismatch = AppFeatures.darkMode.evaluateWithShadow(
    context = context,
    shadowOptions = ShadowEvaluationOptions(
        shadowRegistry = candidateRegistry,
        onMismatch = { m ->
            val primaryReason = AppFeatures.darkMode.explain(context)
            val shadowReason = candidateRegistry
                .getFlag(AppFeatures.darkMode.key)
                ?.explain(context)

            logger.error("""
                Mismatch detected:
                Primary: ${m.primary} (decision: ${primaryReason.decision})
                Shadow: ${m.shadow} (decision: ${shadowReason?.decision})
                User: ${m.context.stableId}
            """.trimIndent())
        }
    )
)
```

---

## Next Steps

- [Advanced: Shadow Evaluation](/advanced/shadow-evaluation) — Practical migration patterns
- [API Reference: Feature Operations](/api-reference/feature-operations) — `evaluateWithShadow` API
- [Migration Guide](/migration) — Migrating from other flag systems
