---
name: codebase-uplift
description: Systematically migrate legacy feature flag systems to Konditional's typed, safe approach. Analyzes existing patterns, designs namespace mappings, implements dual-read strategies, and validates migration correctness with zero behavior drift. Use when modernizing string-key flags, config maps, or vendor SDK wrappers to gain compile-time safety, determinism, and observability.
---

# Codebase Uplift — Legacy to Typed Feature Flags

## Objective

Transform legacy feature flag systems into type-safe, deterministic Konditional
implementations while preserving existing behavior, maintaining zero downtime,
and establishing verifiable migration gates.

## Trigger When

- User asks to "migrate to Konditional" or "modernize our flag system"
- Legacy system uses string keys, untyped maps, or vendor-specific SDKs
- Team needs compile-time safety, determinism, or better observability
- Request mentions "dual read", "shadow mode", "gradual migration"
- Current system has reliability, consistency, or audit issues
- Team wants to reduce runtime errors from typos or type mismatches

## Primary Outcomes

1. **Discovery**: Complete inventory of legacy flags, call sites, and ownership
2. **Design**: Typed namespace architecture aligned to team boundaries
3. **Safety**: Dual-read implementation with mismatch detection
4. **Validation**: Automated tests proving migration correctness
5. **Promotion**: Gradual rollout with rollback capability
6. **Completion**: Legacy system removal with verified behavior preservation

## Execution Philosophy

**Zero behavior drift.** Every evaluation must produce identical results between
legacy and candidate systems until explicit promotion gates pass.

**Safety over speed.** Introduce Konditional alongside legacy, not as a
replacement, until verification confirms equivalence.

**Observability as foundation.** Make mismatches visible before they become
incidents.

**Team ownership preserved.** Namespace boundaries map to existing ownership,
not arbitrary technical boundaries.

## Migration Framework

### Phase 0: Discovery and Inventory

**Goal:** Build a complete, deterministic map of the legacy system.

#### Actions

1. **Identify the legacy implementation pattern**
   - String-key SDK calls (LaunchDarkly, Split, etc.)?
   - Config map or environment variable lookups?
   - Custom internal flag service?
   - Multiple sources or vendors?

2. **Extract flag inventory**
   ```kotlin
   // Build a structured inventory
   data class LegacyFlag(
       val key: String,
       val type: FlagType,  // boolean, string, number, json
       val defaultValue: Any?,
       val owner: String,
       val callSites: List<CodeLocation>,
       val context: Set<String>  // targeting attributes used
   )
   ```

3. **Map evaluation patterns**
   - What context/attributes drive targeting?
   - Are evaluations synchronous or async?
   - Where is configuration loaded from?
   - What happens on load failures?

4. **Identify ownership boundaries**
   - Which teams own which flag groups?
   - What are the natural namespace boundaries?
   - Which flags share similar context needs?

#### Deliverables

- `legacy-inventory.md`: Complete flag catalog
- `ownership-map.md`: Team/namespace mapping
- `context-analysis.md`: Required targeting attributes per namespace

#### Validation Gate

Cannot proceed until:
- [ ] All active flags catalogued with owners
- [ ] Call site count verified for critical flags
- [ ] Context/targeting attribute inventory complete

---

### Phase 1: Namespace Design

**Goal:** Map legacy flags to typed Konditional namespaces that preserve
ownership boundaries and prepare for gradual migration.

#### Actions

1. **Define namespace boundaries**
   ```kotlin
   // Group by team/ownership, not by technical similarity
   object CheckoutFlags : Namespace("checkout") {
       // All flags owned by checkout team
   }
   
   object PersonalizationFlags : Namespace("personalization") {
       // All flags owned by personalization team
   }
   ```

2. **Type legacy flags as delegated properties**
   ```kotlin
   // Legacy: ldClient.boolVariation("enable-new-checkout", false)
   // Konditional:
   object CheckoutFlags : Namespace("checkout") {
       val enableNewCheckout by boolean<CheckoutContext>(default = false)
   }
   ```

3. **Design context models**
   ```kotlin
   // Consolidate legacy attribute sets into typed models
   data class CheckoutContext(
       override val stableId: StableId?,
       val cartTotal: Double,
       val region: String,
       val isPremium: Boolean
   ) : Context, StableIdContext
   ```

4. **Map complex targeting to rules**
   ```kotlin
   // Legacy: complex vendor-UI configured rules
   // Konditional: explicit, versioned Kotlin DSL
   val enableNewCheckout by boolean<CheckoutContext>(default = false) {
       rule(true) {
           rampUp(percentage = 25)
           axis("region") { anyOf("us", "ca") }
       }
       rule(false) { disable }  // Kill switch ready
   }
   ```

#### Deliverables

- `namespace-design.kt`: Complete namespace declarations
- `context-models.kt`: Typed context classes per namespace
- `migration-mapping.md`: `legacy_key -> Namespace.feature` table

#### Validation Gate

Cannot proceed until:
- [ ] Every legacy flag has a typed Konditional equivalent
- [ ] Context models cover all targeting attributes
- [ ] Namespace ownership aligns with team structure
- [ ] Default values match legacy defaults exactly

---

### Phase 2: Dual-Read Implementation

**Goal:** Add Konditional evaluation alongside legacy without changing behavior.

#### Actions

1. **Create migration adapter**
   ```kotlin
   class FlagMigrationAdapter(
       private val legacyClient: LegacyFlagClient,
       private val telemetry: MismatchTelemetry
   ) {
       fun evaluateCheckoutFlag(
           context: CheckoutContext,
           legacyKey: String,
           kandidate: Feature<CheckoutContext, Boolean>
       ): Boolean {
           val baseline = legacyClient.boolVariation(legacyKey, kandidate.default)
           val candidate = kandidate.evaluate(context)
           
           if (baseline != candidate) {
               telemetry.recordMismatch(
                   legacyKey = legacyKey,
                   baseline = baseline,
                   candidate = candidate,
                   context = context
               )
           }
           
           return baseline  // Always return legacy (baseline)
       }
   }
   ```

2. **Instrument call sites**
   ```kotlin
   // Before:
   val useNewCheckout = ldClient.boolVariation("enable-new-checkout", false)
   
   // During migration:
   val useNewCheckout = adapter.evaluateCheckoutFlag(
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

3. **Add mismatch observability**
   ```kotlin
   interface MismatchTelemetry {
       fun recordMismatch(
           legacyKey: String,
           baseline: Any?,
           candidate: Any?,
           context: Context
       )
   }
   
   // Emit structured events for analysis
   class OpenTelemetryMismatchTelemetry : MismatchTelemetry {
       override fun recordMismatch(...) {
           span.addEvent("flag.migration.mismatch") {
               setAttribute("flag.legacy_key", legacyKey)
               setAttribute("flag.baseline_value", baseline.toString())
               setAttribute("flag.candidate_value", candidate.toString())
               setAttribute("context.stable_id", context.stableId?.value)
           }
       }
   }
   ```

#### Deliverables

- `FlagMigrationAdapter.kt`: Dual-read orchestration
- `MismatchTelemetry.kt`: Structured mismatch reporting
- Instrumented call sites returning baseline value

#### Validation Gate

Cannot proceed until:
- [ ] Adapter deployed with mismatch telemetry
- [ ] Baseline value always returned (no behavior change)
- [ ] Mismatches visible in observability platform
- [ ] Kill switch tested for adapter bypass

---

### Phase 3: Verification and Analysis

**Goal:** Prove migration correctness through automated tests and production
mismatch analysis.

#### Actions

1. **Write equivalence tests**
   ```kotlin
   @Test
   fun `migrated flag produces identical results for known scenarios`() {
       val scenarios = loadHistoricalEvaluations("enable-new-checkout")
       
       scenarios.forEach { (legacyContext, expectedValue) ->
           val kandidContext = legacyContext.toCheckoutContext()
           val actualValue = CheckoutFlags.enableNewCheckout.evaluate(kandidContext)
           
           assertEquals(
               expected = expectedValue,
               actual = actualValue,
               message = "Mismatch for context: $kandidContext"
           )
       }
   }
   ```

2. **Test determinism**
   ```kotlin
   @Test
   fun `same context always produces same result`() {
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

3. **Analyze production mismatches**
   - Query mismatch events from telemetry backend
   - Categorize by root cause:
     - Context mapping errors
     - Rules not equivalent
     - Legacy system inconsistencies
     - Timing/race conditions
   - Fix until mismatch rate < target threshold (for example 0.01%)

4. **Load test under dual-read**
   - Verify no performance degradation
   - Confirm atomicity under concurrent snapshot loads
   - Test namespace isolation under mixed load

#### Deliverables

- `MigrationTest.kt`: Comprehensive equivalence suite
- `mismatch-analysis-report.md`: Root cause breakdown
- Performance benchmark comparison

#### Validation Gate

Cannot proceed until:
- [ ] Equivalence tests pass for all historical scenarios
- [ ] Determinism verified for representative contexts
- [ ] Production mismatch rate < 0.01% for 7 days
- [ ] Performance within 5% of baseline
- [ ] Rollback procedure tested successfully

---

### Phase 4: Promotion

**Goal:** Switch from baseline (legacy) to candidate (Konditional) with
incremental rollout.

#### Actions

1. **Add promotion control**
   ```kotlin
   class FlagMigrationAdapter(
       private val legacyClient: LegacyFlagClient,
       private val telemetry: MismatchTelemetry,
       private val promotionRegistry: PromotionRegistry
   ) {
       fun evaluateCheckoutFlag(
           context: CheckoutContext,
           legacyKey: String,
           kandidate: Feature<CheckoutContext, Boolean>
       ): Boolean {
           val baseline = legacyClient.boolVariation(legacyKey, kandidate.default)
           val candidate = kandidate.evaluate(context)
           
           val isPromoted = promotionRegistry.isPromoted(legacyKey, context)
           
           return if (isPromoted) {
               candidate
           } else {
               baseline
           }.also {
               if (baseline != candidate) {
                   telemetry.recordMismatch(
                       legacyKey = legacyKey,
                       baseline = baseline,
                       candidate = candidate,
                       returned = it
                   )
               }
           }
       }
   }
   ```

2. **Incremental promotion**
   ```kotlin
   // Start: 1% traffic to candidate
   promotionRegistry.promote("enable-new-checkout", percentage = 1)
   
   // After 24h stability: increase
   promotionRegistry.promote("enable-new-checkout", percentage = 10)
   
   // After 72h stability: increase
   promotionRegistry.promote("enable-new-checkout", percentage = 50)
   
   // After 1w stability: full promotion
   promotionRegistry.promote("enable-new-checkout", percentage = 100)
   ```

3. **Monitor for regressions**
   - Alert on increased mismatch rate
   - Alert on evaluation latency changes
   - Alert on unexpected null/error rates
   - Correlate with business metrics

4. **Test rollback at each step**
   ```kotlin
   promotionRegistry.rollback("enable-new-checkout")  // Instant back to baseline
   ```

#### Deliverables

- `PromotionRegistry.kt`: Gradual promotion control
- Promotion runbook with rollback procedures
- Automated alerts for promotion monitoring

#### Validation Gate

Cannot proceed until:
- [ ] Promotion registry tested with instant rollback
- [ ] Each promotion step monitored for 24h+ without regression
- [ ] Rollback drill executed successfully
- [ ] Business metrics stable across promotion curve

---

### Phase 5: Legacy Removal

**Goal:** Remove legacy system dependencies while preserving Konditional
implementation.

#### Actions

1. **Remove adapter layer**
   ```kotlin
   // Before (dual-read):
   val useNewCheckout = adapter.evaluateCheckoutFlag(...)
   
   // After (direct):
   val useNewCheckout = CheckoutFlags.enableNewCheckout.evaluate(context)
   ```

2. **Remove legacy client initialization**
   ```kotlin
   // Delete:
   // val ldClient = LDClient(sdkKey)
   ```

3. **Remove legacy dependencies**
   ```kotlin
   // build.gradle.kts
   dependencies {
       // Remove:
       // implementation("com.launchdarkly:launchdarkly-java-server-sdk:x.y.z")
       
       // Keep:
       implementation("io.amichne:konditional-core:x.y.z")
       implementation("io.amichne:konditional-runtime:x.y.z")
       implementation("io.amichne:konditional-serialization:x.y.z")
   }
   ```

4. **Archive migration artifacts**
   - Keep migration tests for regression suite
   - Archive inventory and analysis documents
   - Document migration learnings

#### Deliverables

- Clean Konditional-only implementation
- Migration retrospective document
- Updated dependency declarations

#### Validation Gate

Cannot proceed until:
- [ ] All legacy client code removed
- [ ] All call sites use direct Konditional evaluation
- [ ] Build no longer depends on legacy SDK
- [ ] Production traffic 100% on Konditional for 2+ weeks
- [ ] Regression suite passing with Konditional baseline

---

## Token-Efficient Execution

1. **Always start with inventory**
   - Use `grep_search` for flag key patterns
   - Load `.signatures/` files for symbol-aware refactoring
   - Build structured inventory before proposing changes

2. **Leverage IntelliJ semantic tools when available**
   ```
   ide_find_references(symbol) → all call sites
   ide_find_definition(symbol) → implementation
   ide_refactor_rename(old, new) → safe rename across project
   ```

3. **Reuse adapter patterns**
   - Don't reinvent dual-read orchestration
   - Reference `skill/resources/migration-adapter.kt` template

4. **Validate incrementally**
   - Write tests for each namespace before proceeding
   - Run focused test suites, not full builds, during iteration

## Response Contract

For every uplift engagement, provide:

1. **Current State Analysis**
   - Legacy pattern identification
   - Flag inventory summary
   - Ownership boundary map

2. **Migration Architecture**
   - Namespace design with rationale
   - Context model definitions
   - Dual-read adapter design

3. **Safety Plan**
   - Mismatch detection strategy
   - Rollback procedures
   - Promotion gates with thresholds

4. **Verification Strategy**
   - Equivalence test approach
   - Determinism validation
   - Load test plan

5. **Rollout Timeline**
   - Phase-by-phase milestones
   - Success criteria per phase
   - Risk mitigation at each gate

## Hard Constraints

### Do

- Preserve exact legacy behavior until explicit promotion
- Make mismatches observable before they affect users
- Design namespaces around team ownership
- Test rollback capability before each promotion step
- Keep baseline system as source of truth until 100% promoted
- Add comprehensive equivalence tests

### Do Not

- Change behavior before verification gates pass
- Promote without mismatch analysis
- Remove legacy system before 100% promoted for 2+ weeks
- Skip determinism or isolation tests
- Design namespaces around technical boundaries instead of ownership
- Trust "it looks right" — demand measurement

## Completion Gate

Cannot declare migration complete until:

- [ ] All legacy flags mapped to typed Konditional features
- [ ] Dual-read adapter implemented with mismatch telemetry
- [ ] Equivalence tests prove correctness for representative scenarios
- [ ] Production mismatch rate < threshold for 7+ days
- [ ] Gradual promotion executed with monitoring at each step
- [ ] Rollback capability tested and verified
- [ ] Legacy system removed from production
- [ ] Code compiled and tests passing with Konditional only
- [ ] Migration retrospective documented

## Resources

- **Migration adapter template:** `skill/resources/migration-adapter.kt`
- **Inventory extraction scripts:** `scripts/extract-legacy-flags.sh`
- **Evidence map:** `skill/resources/evidence-map.md`
- **Equivalence test patterns:** `konditional-core/src/test/kotlin/migration/`

## Quick Reference

**Discovery:**
```bash
# Find legacy flag call sites
rg -t kotlin 'ldClient\.(bool|string|int|double|json)Variation'
```

**Design:**
```kotlin
object MyFlags : Namespace("my-team") {
    val myFeature by boolean<MyContext>(default = false)
}
```

**Dual-Read:**
```kotlin
val baseline = legacyClient.boolVariation(key, default)
val candidate = MyFlags.myFeature.evaluate(context)
if (baseline != candidate) telemetry.recordMismatch(...)
return baseline  // Always baseline until promoted
```

**Promotion:**
```kotlin
promotionRegistry.promote(key, percentage = 10)  // Start small
// Monitor for 24h, then increase
```

**Verification:**
```kotlin
@Test fun `equivalence for known scenarios`() {
    historicalEvaluations.forEach { (ctx, expected) →
        assertEquals(expected, MyFlags.myFeature.evaluate(ctx))
    }
}
```
