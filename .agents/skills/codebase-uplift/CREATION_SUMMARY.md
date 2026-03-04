# Codebase Uplift Skill — Creation Summary

**Agent Personality for Legacy Feature Flag Migration**

## What Was Created

A comprehensive agent skill focused on systematically migrating legacy feature flag systems to Konditional's typed, deterministic architecture.

### Core Skill File

**`.agents/skills/codebase-uplift/SKILL.md`**

A 700+ line skill definition covering:

#### Execution Framework
- **Phase 0: Discovery** — Inventory existing flags, call sites, and ownership
- **Phase 1: Namespace Design** — Map legacy keys to typed Konditional features
- **Phase 2: Dual-Read Implementation** — Add Konditional alongside legacy with mismatch tracking
- **Phase 3: Verification** — Prove migration correctness through automated tests
- **Phase 4: Promotion** — Gradual rollout with monitoring and rollback capability
- **Phase 5: Legacy Removal** — Clean removal after verified migration

#### Key Capabilities
- Deterministic inventory extraction from existing codebases
- Namespace boundary design aligned to team ownership
- Dual-read adapter pattern with baseline-always-wins semantics
- Mismatch telemetry and analysis workflows
- Gradual promotion with percentage-based rollout
- Emergency rollback procedures
- Comprehensive test strategies (equivalence, determinism, isolation, boundary)

### Supporting Resources

**`.agents/skills/codebase-uplift/resources/`** contains:

#### 1. `migration-adapter.kt` (~350 lines)
Production-ready Kotlin template implementing:
- `FlagMigrationAdapter<T>` — Core dual-read orchestration
- `LegacyEvaluator<T>` — Abstraction over legacy SDK
- `MismatchTelemetry` — Structured mismatch reporting interface
- `PromotionRegistry` — Gradual rollout control
- Example implementations for LaunchDarkly and OpenTelemetry
- Deterministic bucketing for percentage-based promotion

#### 2. `inventory-template.md` (~300 lines)
Comprehensive template for documenting existing systems:
- System overview (SDK, patterns, failure modes)
- Flag-by-flag catalog structure
- Context attribute analysis
- Ownership mapping to namespace boundaries
- Risk assessment framework
- Call site pattern documentation
- Migration readiness checklist
- Extraction command examples

#### 3. `quick-reference.md` (~450 lines)
Operational playbook for day-to-day migration work:
- Phase checklist tracker
- Common discovery/test/deployment commands
- Code snippet library (namespaces, contexts, tests)
- Mismatch analysis queries (Prometheus/OpenTelemetry)
- Rollout strategies (conservative vs aggressive timelines)
- Monitoring dashboard specifications
- Troubleshooting guide (high mismatch rate, performance, rollback)
- Emergency rollback procedures
- Success criteria gates per phase

#### 4. `migration-tests.kt` (~400 lines)
Complete test suite template demonstrating:
- **Equivalence Tests**: Prove Konditional matches legacy results
- **Determinism Tests**: Same context → same result always
- **Isolation Tests**: Namespace changes don't cross-contaminate
- **Boundary Tests**: Invalid config rejected with typed errors
- **Mismatch Analysis Tests**: Adapter behavior verification
- Example namespace, context, and test fixture patterns
- Parameterized test structures for historical cases

#### 5. `README.md`
Resource guide explaining:
- Purpose of each resource file
- Integration with main skill execution phases
- Customization points for different legacy systems
- Testing support patterns
- Quick start instructions

### Integration Updates

**`SKILLS_QUICKREF.md`** updated with:
- New "Integration & Migration" skill category
- `codebase-uplift` skill entry
- "Migrating Legacy Flag Systems" workflow example

## Design Philosophy

### Zero Behavior Drift
Always return baseline (legacy) value until explicit promotion gates pass. Mismatches are telemetry-only until promotion.

### Safety Over Speed
Comprehensive verification before each promotion step. Rollback capability tested before advancing.

### Observable Migration
Mismatch events structured for analysis. Root cause identification before promotion.

### Team Ownership Preserved
Namespace boundaries map to existing ownership, not arbitrary technical divisions.

### Gradual Rollout
Percentage-based promotion with monitoring at each step. Emergency rollback always available.

## Use Cases

### New User Integration
While primarily focused on migration, the skill also supports:
- First-time Konditional adoption
- Greenfield namespace design
- Typed context modeling
- Rule DSL usage

### Enterprise Migration
Designed for:
- Large-scale legacy system replacement (LaunchDarkly, Split, Unleash, etc.)
- Multi-team coordination with blast-radius containment
- Phased rollout with SLO-aligned gates
- Production safety with kill-switch capability

### Common Legacy Patterns Addressed
- String-key flag lookups → Typed delegated properties
- Untyped context maps → Typed `Context` subtypes
- Vendor SDK coupling → Abstracted boundary-safe loading
- Manual testing → Automated equivalence verification
- One-shot migration → Gradual dual-read promotion

## Token-Efficiency Features

- Progressive disclosure: overview → phase detail → specific action
- Reusable code templates (copy and adapt, don't reinvent)
- Structured artifact contracts (inventory, design, tests)
- IntelliJ semantic tool integration for symbol-aware refactoring
- Focused validation commands (narrow test targets, not full builds)

## Quality Gates

The skill enforces completion criteria per phase:
- Phase 0: Inventory complete, ownership assigned
- Phase 1: All legacy flags mapped to typed features
- Phase 2: Dual-read deployed, baseline always returned
- Phase 3: Mismatch rate < 0.01% for 7+ days
- Phase 4: Gradual promotion with rollback tested
- Phase 5: Legacy system removed, tests passing

## Response Contract

For every uplift engagement, the agent provides:
1. **Current State Analysis** — Legacy pattern, inventory, ownership map
2. **Migration Architecture** — Namespace design, context models, adapter
3. **Safety Plan** — Mismatch detection, rollback procedures, promotion gates
4. **Verification Strategy** — Equivalence tests, determinism validation, load tests
5. **Rollout Timeline** — Phase milestones, success criteria, risk mitigation

## Example Invocation

```
User: "Help me migrate from LaunchDarkly to Konditional"

Agent (with codebase-uplift skill):
1. Discovers LaunchDarkly SDK usage via grep_search
2. Extracts flag inventory from call sites
3. Proposes namespace design based on flag groupings
4. Provides migration-adapter.kt template adapted for LaunchDarkly
5. Generates equivalence test structure
6. Designs dual-read deployment plan
7. Specifies mismatch telemetry implementation
8. Creates gradual promotion roadmap with gates
9. Documents rollback procedures
10. Produces phased migration timeline
```

## Extensibility

The skill and resources are designed for customization:

### Different Legacy Systems
- LaunchDarkly (example provided)
- Split (implement `LegacyEvaluator`)
- Unleash (implement `LegacyEvaluator`)
- Custom systems (adapt evaluator interface)

### Different Telemetry Backends
- OpenTelemetry (example provided)
- Prometheus (query examples in quick-reference)
- DataDog (adapt `MismatchTelemetry`)
- CloudWatch (adapt `MismatchTelemetry`)

### Different Risk Profiles
- Conservative rollout (high-risk flags, 21+ day timeline)
- Aggressive rollout (low-risk flags, 3-10 day timeline)
- Custom gates (adjust per organization SLOs)

## Relationship to Other Skills

### Complementary Skills
- **`kotlin-mastery`** — Used for implementation quality
- **`technical-review`** — Used for architecture validation
- **`kotlin-jvm-lsp-gradle-debug`** — Used for tooling setup
- **`public-surface-init-context`** — Used for understanding Konditional APIs

### Skill Modes Referenced
1. **Kotlin mastery mode** (default for all code changes)
2. **Architecture review mode** (for enterprise readiness assessment)
3. **IntelliJ semantic mode** (for symbol-aware refactoring)
4. **Gradle/LSP/debug mode** (for tooling validation)

## Future Enhancements

Potential additions based on real-world usage:
- Migration cost/effort estimation models
- Automated inventory extraction scripts
- CI pipeline integration templates
- Observability dashboard templates (Grafana/Datadog JSON)
- Legacy-to-Konditional rule translation automations
- Mismatch root cause classifier ML model

## Success Metrics

A successful migration using this skill should achieve:
- ✅ Zero production incidents attributed to migration
- ✅ < 0.01% mismatch rate before 100% promotion
- ✅ Rollback capability tested and verified
- ✅ All equivalence tests passing
- ✅ Legacy system removed within planned timeline
- ✅ Team trained on Konditional operations
- ✅ Documentation updated for new system

## Files Created

```
.agents/skills/codebase-uplift/
├── SKILL.md                          # Main skill definition (700+ lines)
└── resources/
    ├── README.md                     # Resource guide
    ├── migration-adapter.kt          # Dual-read adapter template (350 lines)
    ├── inventory-template.md         # Discovery template (300 lines)
    ├── quick-reference.md            # Operational playbook (450 lines)
    └── migration-tests.kt            # Test suite template (400 lines)

Updated:
SKILLS_QUICKREF.md                    # Added new skill entry and workflow
```

**Total Lines of Code/Documentation:** ~2,700 lines

---

## How to Use This Skill

### For AI Agents
```
When user requests migration or modernization:
1. Load .agents/skills/codebase-uplift/SKILL.md
2. Follow execution framework phases
3. Reference resources/ templates as needed
4. Apply Kotlin mastery mode for all code
```

### For Engineers
```bash
# Invoke via Copilot
/skill:codebase-uplift How do I migrate from LaunchDarkly?

# Reference resources directly
cat .agents/skills/codebase-uplift/resources/quick-reference.md
```

---

**The codebase-uplift skill is now ready for use in migrating legacy feature flag systems to Konditional with safety, observability, and systematic verification.**
