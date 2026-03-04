# Legacy Flag System Inventory Template

## Purpose

Document all existing feature flags before migration to establish:
- Complete catalog of flags to migrate
- Ownership and blast-radius boundaries
- Context/targeting requirements per flag
- Call site impact analysis

## Section 1: System Overview

### Legacy Implementation

**Primary Flag System:**
- [ ] LaunchDarkly
- [ ] Split
- [ ] Unleash
- [ ] ConfigCat
- [ ] Custom internal system
- [ ] Environment variables / config maps
- [ ] Other: ___________________

**SDK/Client Details:**
- Language: ___________________
- SDK Version: ___________________
- Configuration Source: ___________________

**Evaluation Pattern:**
- [ ] Synchronous blocking calls
- [ ] Async/callback-based
- [ ] Cached with polling refresh
- [ ] Direct remote calls
- [ ] Other: ___________________

**Failure Mode:**
- [ ] Returns default on error
- [ ] Throws exception
- [ ] Returns last-known-good
- [ ] Circuit breaker/fallback
- [ ] Other: ___________________

---

## Section 2: Flag Inventory

Template structure for each flag:

```yaml
flags:
  - legacy_key: "enable-new-checkout"
    type: boolean
    description: "Enables V2 checkout flow"
    default_value: false
    owner: checkout-team
    call_sites:
      - file: "src/main/kotlin/checkout/CheckoutController.kt"
        line: 45
        usage: "if (client.boolVariation('enable-new-checkout', false))"
      - file: "src/main/kotlin/cart/CartService.kt"
        line: 128
        usage: "val useV2 = client.boolVariation('enable-new-checkout', false)"
    targeting_attributes:
      - user.id
      - user.premium
      - user.region
    active_rules: "25% rollout in US region, 100% for premium users"
    risk_level: high  # impact if broken
    evaluation_frequency: per_request  # or cached, batch, etc.
    
  - legacy_key: "dark-mode-enabled"
    type: boolean
    description: "Show dark mode UI"
    default_value: false
    owner: frontend-team
    call_sites:
      - file: "src/main/kotlin/ui/ThemeProvider.kt"
        line: 22
        usage: "val darkMode = client.boolVariation('dark-mode-enabled', false)"
    targeting_attributes:
      - user.id
      - user.preferences.theme
    active_rules: "User preference override, else 10% rollout"
    risk_level: low
    evaluation_frequency: once_per_session
```

---

## Section 3: Context Attribute Analysis

Document all targeting attributes used across flags:

| Attribute | Type | Flags Using It | Source | Nullable? |
|-----------|------|----------------|--------|-----------|
| user.id | String | enable-new-checkout, dark-mode-enabled | Auth context | No |
| user.premium | Boolean | enable-new-checkout | DB/cache | No |
| user.region | String | enable-new-checkout | GeoIP service | Yes |
| user.preferences.theme | String | dark-mode-enabled | User settings | Yes |

**Derived Attributes:**
- Are any attributes computed/transformed before evaluation?
- Are there ephemeral attributes (for example A/B test assignments)?

---

## Section 4: Ownership Mapping

Group flags by team/domain ownership to define namespace boundaries:

### Checkout Team Namespace
**Proposed Namespace ID:** `checkout`
**Flags:**
- enable-new-checkout
- express-checkout-enabled
- checkout-timeout-ms

**Blast Radius:** High (revenue-impacting)
**Rollback Owner:** checkout-oncall@

---

### Frontend Team Namespace
**Proposed Namespace ID:** `frontend`
**Flags:**
- dark-mode-enabled
- new-header-design
- experimental-animations

**Blast Radius:** Low (UX only)
**Rollback Owner:** frontend-oncall@

---

## Section 5: Risk Assessment

### High-Risk Flags (must verify before promotion)
1. **enable-new-checkout**
   - Why: Revenue-impacting, 500+ eval/sec
   - Mitigation: Extended dual-read period, gradual promotion
   - Test plan: Load test at 2x peak traffic

2. **payment-provider-v2**
   - Why: External dependency, irreversible actions
   - Mitigation: Synthetic transaction monitoring during migration
   - Test plan: End-to-end payment flow tests

### Medium-Risk Flags
(Similar structure...)

### Low-Risk Flags
(Similar structure...)

---

## Section 6: Call Site Patterns

Document common patterns for automated refactoring:

### Pattern 1: Inline boolean checks
```kotlin
// Before:
if (ldClient.boolVariation("enable-feature", false)) {
    // feature code
}

// After:
if (MyFlags.enableFeature.evaluate(context)) {
    // feature code
}
```

### Pattern 2: Stored in variable
```kotlin
// Before:
val featureEnabled = ldClient.boolVariation("enable-feature", false)

// After:
val featureEnabled = MyFlags.enableFeature.evaluate(context)
```

### Pattern 3: Conditional assignment
```kotlin
// Before:
val provider = if (ldClient.boolVariation("use-new-provider", false)) {
    NewProvider()
} else {
    OldProvider()
}

// After:
val provider = if (MyFlags.useNewProvider.evaluate(context)) {
    NewProvider()
} else {
    OldProvider()
}
```

---

## Section 7: Dependencies and Integration Points

### External Systems
- Configuration source: ___________________
- Metrics/observability: ___________________
- A/B test platform: ___________________
- Admin UI: ___________________

### Internal Dependencies
- Services that evaluate flags: ___________________
- Services that manage flags: ___________________
- Batch jobs using flags: ___________________

---

## Section 8: Migration Readiness Checklist

- [ ] All active flags catalogued
- [ ] Call sites identified and counted
- [ ] Ownership assigned per flag
- [ ] Context attributes documented
- [ ] Risk levels assessed
- [ ] Default values verified
- [ ] Active targeting rules documented
- [ ] Namespace boundaries proposed
- [ ] High-risk flags identified with test plans
- [ ] Legacy client initialization understood
- [ ] Failure modes and fallbacks documented

---

## Section 9: Extraction Commands

Document commands used to build this inventory:

```bash
# Find all flag evaluation call sites
rg -t kotlin 'ldClient\.(bool|string|int|double|json)Variation'

# Count call sites per flag
rg -t kotlin "boolVariation\(['\"]([^'\"]+)" -o -r '$1' | sort | uniq -c | sort -nr

# Find context attribute usage
rg -t kotlin 'LDContext\.builder\(' -A 10

# Find flag key definitions
rg -t kotlin "const val.*FLAG.*=" 
```

---

## Next Steps

After completing inventory:
1. Review with product/engineering leads for completeness
2. Validate risk assessments with SRE team
3. Design Konditional namespace architecture
4. Create typed context models
5. Build migration adapter
6. Write equivalence tests based on active rules
