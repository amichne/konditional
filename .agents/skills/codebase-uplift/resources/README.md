# Codebase Uplift Resources

Supporting materials for migrating legacy feature flag systems to Konditional.

## Contents

### [migration-adapter.kt](./migration-adapter.kt)

Production-ready dual-read adapter template with:
- **FlagMigrationAdapter**: Core orchestration for baseline/candidate evaluation
- **LegacyEvaluator**: Abstraction over legacy SDK calls
- **MismatchTelemetry**: Structured mismatch event reporting
- **PromotionRegistry**: Gradual rollout control with rollback
- **Example implementations** for LaunchDarkly and OpenTelemetry

**Usage:** Copy and adapt to your specific legacy SDK. Implement the interfaces
for your telemetry backend and promotion storage.

---

### [inventory-template.md](./inventory-template.md)

Comprehensive template for documenting existing flag systems before migration.

**Sections:**
1. System overview (SDK, evaluation pattern, failure modes)
2. Flag-by-flag inventory (keys, types, call sites, targeting)
3. Context attribute analysis
4. Ownership mapping to namespaces
5. Risk assessment
6. Call site patterns for refactoring
7. Dependencies and integration points
8. Migration readiness checklist

**Usage:** Fill out during Phase 0 (Discovery) to establish ground truth before
any code changes.

---

### [quick-reference.md](./quick-reference.md)

Field guide for day-to-day migration execution.

**Contents:**
- Phase checklist tracker
- Common discovery/test/deployment commands
- Code snippet library (namespaces, contexts, dual-read, tests)
- Mismatch analysis queries
- Rollout strategies (conservative vs aggressive)
- Monitoring dashboard specifications
- Troubleshooting guide
- Emergency rollback procedures
- Success criteria per gate

**Usage:** Keep open during implementation and rollout as operational playbook.

---

## Quick Start

1. **Discovery Phase**
   - Use `inventory-template.md` to catalog existing system
   - Run discovery commands from `quick-reference.md`
   - Document all flags, call sites, and ownership

2. **Design Phase**
   - Map inventory to Konditional namespaces
   - Define typed context models
   - Create migration table (legacy_key → Namespace.feature)

3. **Implementation Phase**
   - Copy `migration-adapter.kt` as starting point
   - Implement your specific `LegacyEvaluator`
   - Wire up telemetry and promotion registry
   - Use code snippets from `quick-reference.md`

4. **Verification Phase**
   - Write equivalence tests for each flag
   - Deploy dual-read with 0% promotion
   - Analyze mismatch telemetry
   - Fix context mapping or rule differences

5. **Rollout Phase**
   - Follow rollout strategy from `quick-reference.md`
   - Monitor dashboards per flag
   - Use emergency procedures if needed
   - Validate success criteria before each promotion step

6. **Cleanup Phase**
   - Remove adapter layer
   - Delete legacy client code
   - Update dependencies
   - Archive migration artifacts

---

## Integration with Main Skill

These resources support the [../SKILL.md](../SKILL.md) execution framework:

- **Phase 0**: Use `inventory-template.md`
- **Phase 1**: Reference namespace design patterns
- **Phase 2**: Copy `migration-adapter.kt` implementation
- **Phase 3**: Use test patterns from `quick-reference.md`
- **Phase 4**: Follow rollout strategy and monitoring specs
- **Phase 5**: Use cleanup checklist

---

## Customization Points

### For Different Legacy Systems

**LaunchDarkly:**
- Adapter provided as example in `migration-adapter.kt`
- Map LDContext ↔ Konditional Context

**Split:**
- Implement `LegacyEvaluator` for Split client
- Handle Split's Traffic Type concept

**Unleash:**
- Implement `LegacyEvaluator` for Unleash client
- Map Unleash Context to Konditional Context

**Custom/Config Maps:**
- Implement simple `LegacyEvaluator` for map lookups
- May not need complex promotion logic

### For Different Telemetry Backends

**OpenTelemetry:**
- Example implementation in `migration-adapter.kt`
- Use span events or metrics as preferred

**Prometheus:**
- Emit mismatch counter with labels
- Example queries in `quick-reference.md`

**DataDog:**
- Use custom metrics or events
- Tag with flag key, namespace, promotion percentage

**CloudWatch:**
- Use EMF (Embedded Metric Format)
- Create dashboard from `quick-reference.md` specifications

---

## Testing Support

All code templates include:
- Determinism test patterns
- Equivalence test structures
- Load test considerations
- Rollback verification approaches

Expand tests based on your specific risk profile and SLOs.

---

## Contributing Improvements

If you find gaps or have better patterns from your migration:
1. Document the issue or enhancement
2. Propose change with rationale
3. Include example code if applicable
4. Submit for skill maintainer review

These resources evolve based on real-world migration experiences.
