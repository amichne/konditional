# Migration Quick Reference

Fast lookup guide for common migration tasks.

## Phase Checklist

```
Phase 0: Discovery ✓
├─ Flag inventory complete
├─ Ownership mapped
└─ Context attributes documented

Phase 1: Design ✓
├─ Namespaces defined
├─ Context models implemented
└─ Type mappings documented

Phase 2: Dual-Read ✓
├─ Migration adapter deployed
├─ Mismatch telemetry active
└─ Baseline always returned

Phase 3: Verification ✓
├─ Equivalence tests passing
├─ Mismatch rate < 0.01%
└─ Load tests completed

Phase 4: Promotion ✓
├─ Gradual rollout executed
├─ Monitoring confirmed
└─ Rollback tested

Phase 5: Cleanup ✓
├─ Legacy client removed
├─ Direct evaluation only
└─ Dependencies updated
```

---

## Common Commands

### Discovery

```bash
# Find all flag keys (LaunchDarkly example)
rg -t kotlin 'Variation\(["\']([^"\']+)["\']' -o -r '$1' | sort -u > flags.txt

# Count call sites per flag
rg "boolVariation\(['\"]enable-checkout" | wc -l

# Find context attributes
rg 'LDContext\.builder' -A 5
```

### Testing

```bash
# Run migration tests only
./gradlew test --tests '*Migration*'

# Run equivalence tests for one namespace
./gradlew :app:test --tests '*CheckoutMigrationTest'

# Check for determinism violations
./gradlew test --tests '*Determinism*' --rerun-tasks
```

### Deployment

```bash
# Deploy with dual-read enabled
helm upgrade app ./chart \
  --set migration.enabled=true \
  --set migration.promotion.percentage=0

# Promote to 10%
kubectl patch configmap app-migration \
  --patch '{"data":{"enable-checkout.promotion": "10"}}'

# Rollback
kubectl patch configmap app-migration \
  --patch '{"data":{"enable-checkout.promotion": "0"}}'
```

---

## Code Snippets

### Define Namespace

```kotlin
object CheckoutFlags : Namespace("checkout") {
    val enableNewCheckout by boolean<CheckoutContext>(default = false) {
        rule(true) { rampUp(percentage = 25) }
    }
}
```

### Define Context

```kotlin
data class CheckoutContext(
    override val stableId: StableId?,
    val cartTotal: Double,
    val region: String,
    val isPremium: Boolean
) : Context, StableIdContext
```

### Dual-Read Evaluation

```kotlin
// Phase 2: Always return baseline
val result = adapter.evaluateBoolean(
    context = CheckoutContext(
        stableId = user.id,
        cartTotal = cart.total,
        region = user.region,
        isPremium = user.isPremium
    ),
    legacyKey = "enable-new-checkout",
    kandidate = CheckoutFlags.enableNewCheckout
)
```

### Direct Evaluation (Post-Migration)

```kotlin
// Phase 5: Legacy removed
val result = CheckoutFlags.enableNewCheckout.evaluate(
    CheckoutContext(
        stableId = user.id,
        cartTotal = cart.total,
        region = user.region,
        isPremium = user.isPremium
    )
)
```

### Equivalence Test

```kotlin
@Test
fun `migrated flag matches legacy for known scenarios`() {
    val historicalCases = loadTestCases("enable-new-checkout.json")
    
    historicalCases.forEach { case ->
        val actual = CheckoutFlags.enableNewCheckout.evaluate(case.context)
        assertEquals(
            expected = case.expectedValue,
            actual = actual,
            message = "Mismatch for ${case.context}"
        )
    }
}
```

### Determinism Test

```kotlin
@Test
fun `same context produces same result`() {
    val ctx = CheckoutContext(
        stableId = StableId("user-123"),
        cartTotal = 99.99,
        region = "us",
        isPremium = true
    )
    
    val first = CheckoutFlags.enableNewCheckout.evaluate(ctx)
    repeat(1000) {
        assertEquals(first, CheckoutFlags.enableNewCheckout.evaluate(ctx))
    }
}
```

---

## Mismatch Analysis

### Query Mismatches (OpenTelemetry example)

```promql
# Count mismatches per flag
sum by (flag_legacy_key) (
    rate(konditional_migration_mismatch_total[5m])
)

# Mismatch percentage
(
    rate(konditional_migration_mismatch_total[5m])
    /
    rate(konditional_migration_evaluation_total[5m])
) * 100
```

### Common Mismatch Root Causes

1. **Context mapping error**
   - Legacy: `user.country`
   - Konditional: `user.region`
   - Fix: Ensure attribute names/values match exactly

2. **Rule logic difference**
   - Legacy: "25% of premium users in US"
   - Konditional: "25% of all users" AND "region = us"
   - Fix: Replicate exact rule structure with `allOf`

3. **Default value mismatch**
   - Legacy: `defaultValue = true`
   - Konditional: `default = false`
   - Fix: Match defaults exactly

4. **Targeting vs fallback confusion**
   - Legacy returns targeting rule result
   - Konditional returns default when no rules match
   - Fix: Add catch-all rule that replicates legacy fallback

5. **Bucketing/hashing difference**
   - Legacy uses proprietary hash
   - Konditional uses stable consistent hash
   - Fix: Accept as expected during dual-read, verify distribution similar

---

## Rollout Strategy

### Conservative (recommended for high-risk flags)

```
Day 0:  Deploy dual-read, 0% promotion
Day 1:  Mismatch analysis, fix mapping errors
Day 2:  Promote to 1%
Day 3:  Monitor, validate metrics unchanged
Day 5:  Promote to 10%
Day 7:  Monitor, validate no incidents
Day 10: Promote to 25%
Day 14: Monitor, validate load test results
Day 17: Promote to 50%
Day 21: Monitor, validate business metrics
Day 24: Promote to 100%
Day 31: Remove legacy client (if all flags at 100%)
```

### Aggressive (for low-risk flags)

```
Day 0: Deploy dual-read, 0% promotion
Day 1: Promote to 10%
Day 2: Promote to 50%
Day 3: Promote to 100%
Day 10: Remove legacy client
```

---

## Monitoring Dashboards

### Migration Health Dashboard

**Panels:**
1. Mismatch rate per flag (target: < 0.01%)
2. Promotion percentage per flag
3. Evaluation latency comparison (baseline vs candidate)
4. Error rate comparison
5. Rollback events

**Alerts:**
- Mismatch rate > 1% for any flag
- Evaluation latency > 10ms P99
- Error rate increase > 10%
- Promotion rollback occurred

---

## Troubleshooting

### High mismatch rate

1. Check context mapping correctness
2. Verify default values match
3. Extract actual evaluations from both systems for same context
4. Compare rule-by-rule

### Performance regression

1. Check evaluation caching strategy
2. Verify snapshot loading not on hot path
3. Profile evaluation under load
4. Compare with legacy client benchmarks

### Promotion not taking effect

1. Verify promotion registry updated
2. Check hash-based bucketing implementation
3. Confirm stable ID available in context
4. Review promotion registry logs

### Rollback not working

1. Test promotion registry rollback in isolation
2. Verify adapter respects 0% promotion
3. Check configuration refresh timing
4. Confirm baseline path still functional

---

## Safety Pre-Flight

Before any promotion step:

```bash
# 1. Verify dual-read active
curl -s localhost:8080/metrics | grep konditional_migration_evaluation_total

# 2. Check mismatch rate
curl -s localhost:8080/metrics | grep konditional_migration_mismatch_total

# 3. Confirm rollback tested recently
grep "promotion_rollback_test" deployment.log | tail -1

# 4. Validate monitoring alerts configured
curl -s localhost:9090/api/v1/rules | jq '.data.groups[].rules[] | select(.alert | contains("migration"))'

# 5. Confirm on-call acknowledged
echo "On-call engineer: $(cat .oncall) - acknowledged? [y/n]"
```

---

## Emergency Procedures

### Immediate Rollback

```bash
# Option 1: Application-level (recommended)
kubectl exec -it app-pod -- curl -X POST \
  http://localhost:8080/admin/migration/rollback/enable-checkout

# Option 2: Configuration-level
kubectl patch configmap app-migration \
  --patch '{"data":{"*.promotion": "0"}}'  # All flags

# Option 3: Kill switch (last resort)
kubectl set env deployment/app MIGRATION_ENABLED=false
```

### Incident Response

1. **Detect**: Alert on mismatch rate or error rate increase
2. **Rollback**: Execute immediate rollback procedure above
3. **Validate**: Confirm metrics return to baseline
4. **Investigate**: Extract mismatch events for root cause
5. **Fix**: Correct mapping/rule/context issue
6. **Re-verify**: Run equivalence tests again
7. **Resume**: Restart promotion from 0% after fix validated

---

## Success Criteria

### Per-Flag Promotion Gate

Can promote to next percentage when:
- [ ] Mismatch rate < 0.01% for 24+ hours
- [ ] No error rate increase detected
- [ ] Evaluation latency within 5% of baseline
- [ ] Business metrics stable (no attribution to change)
- [ ] Rollback tested successfully in past 7 days

### Full Migration Complete

Can remove legacy system when:
- [ ] All flags at 100% promotion for 14+ days
- [ ] No incidents attributed to migration
- [ ] Equivalence tests in CI passing
- [ ] Runbook updated for Konditional-only ops
- [ ] Team trained on new system
- [ ] Legacy client dependencies removed from build
