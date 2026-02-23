# Konditional Documentation — Task Manifest

> **Format:** Structured task list for agentic execution.
> **Each task is a self-contained unit of work** that produces one documentation page.
> **Dependencies are expressed as `requires`** — a task should not start until its dependencies are complete.

---

## Phase 1 — Foundation

### TASK-001: Glossary
```yaml
id: TASK-001
path: docusaurus/docs/appendix/glossary.md
tier: L2
axis: Reference
specificity: 3
claims: []
requires: []
brief: >
  Define canonical terms: Namespace, Feature, FlagDefinition, Context, Rule,
  RuleSet, Snapshot, Patch, ParseError, KonditionalBoundaryFailure, StableId,
  HexId, Bucketing, RampUp, ConfigurationView, MaterializedConfiguration,
  SnapshotLoadOptions. Source definitions from public API KDoc and AGENTS.md
  invariants. Each term gets a 1-2 sentence definition. No implementation details.
```

### TASK-002: Module Dependency Map
```yaml
id: TASK-002
path: docusaurus/docs/reference/module-dependency-map.md
tier: L2
axis: Reference
specificity: 4
claims: [CLM-PR01-07A]
requires: [TASK-001]
brief: >
  Table showing: konditional-core (declarations, evaluation),
  konditional-serialization (JSON codecs, schema), konditional-runtime
  (InMemoryNamespaceRegistry, NamespaceSnapshotLoader, NamespaceOperations).
  For each module: Gradle coordinate, what it provides, when to depend on it.
  Include the server/* modules as "integration" tier.
```

### TASK-003: API Surface Reference
```yaml
id: TASK-003
path: docusaurus/docs/reference/api-surface.md
tier: L2
axis: Reference
specificity: 5
claims: [CLM-PR01-01A, CLM-PR01-01B, CLM-PR01-02A, CLM-PR01-02B, CLM-PR01-03A, CLM-PR01-03B, CLM-PR01-04A, CLM-PR01-05A]
requires: [TASK-001, TASK-002]
brief: >
  Exhaustive catalog of public types and methods from claim-signature-links.json.
  Grouped by module, then by type. For each: fully qualified name, kind (type/method),
  one-line description, link to concept or guide page that explains usage.
  This is a lookup table, not a narrative. Use tables exclusively.
```

---

## Phase 2 — Orientation

### TASK-010: Start Here
```yaml
id: TASK-010
path: docusaurus/docs/overview/start-here.md
tier: L0
axis: Orientation
specificity: 1
claims: [CLM-PR01-01A, CLM-PR01-01B]
requires: []
brief: >
  One-page entry point. Three paragraphs max: what Konditional is, what problem
  it solves, who it's for. One code block showing a typed flag declaration and
  evaluation. Links to quickstart, why-typed-flags, and first-success-map.
  Must not require any prior reading.
```

### TASK-011: Why Typed Flags
```yaml
id: TASK-011
path: docusaurus/docs/overview/why-typed-flags.md
tier: L1
axis: Orientation
specificity: 2
claims: [CLM-PR01-03A, CLM-PR01-03B]
requires: [TASK-010]
brief: >
  Problem/solution narrative. Four failure modes: typo keys, type coercion,
  boolean explosion, inconsistent rollouts. For each: concrete before/after
  showing the string-based failure and the Konditional prevention.
  Introduce ParseError and KonditionalBoundaryFailure conceptually (link to
  concepts/parse-boundary for depth). Acknowledge trade-off: compile-time
  safety requires static declarations.
```

### TASK-012: First Success Map
```yaml
id: TASK-012
path: docusaurus/docs/overview/first-success-map.md
tier: L0
axis: Orientation
specificity: 1
claims: [CLM-PR01-04A]
requires: [TASK-010]
brief: >
  Decision tree (as a Mermaid diagram or structured list) routing readers:
  "I want to try it" → quickstart. "I want to understand how it works" → concepts.
  "I need to convince my team" → why-typed-flags + competitive-positioning.
  "I need formal guarantees" → theory. Maps to RampUpBucketing and
  NamespaceSnapshotLoader as the two primary API entry points.
```

---

## Phase 3 — Quickstart

### TASK-020: Quickstart Index
```yaml
id: TASK-020
path: docusaurus/docs/quickstart/index.md
tier: L1
axis: Procedural
specificity: 1
claims: [CLM-PR01-06A]
requires: [TASK-010]
brief: >
  Landing page for quickstart. Prerequisites: Kotlin 1.9+, Gradle.
  Outline of the 5-step path. Estimated time: 15 minutes.
  Each step links to its page with a one-line description of what it achieves.
```

### TASK-021: Install
```yaml
id: TASK-021
path: docusaurus/docs/quickstart/install.md
tier: L1
axis: Procedural
specificity: 2
claims: [CLM-PR01-07A]
requires: [TASK-020]
brief: >
  Gradle dependency block for konditional-core and konditional-runtime.
  Verify: project compiles. One code block, one verification step.
```

### TASK-022: Define First Flag
```yaml
id: TASK-022
path: docusaurus/docs/quickstart/define-first-flag.md
tier: L1
axis: Procedural
specificity: 3
claims: [CLM-PR01-08A, CLM-PR01-08B]
requires: [TASK-021]
brief: >
  Declare a Namespace (sealed class), a FeatureContainer object, and one
  boolean flag with a default. Introduce Feature and FlagDefinition types
  by usage only. Verify: code compiles, property is accessible.
  Link to concepts/namespaces and concepts/features-and-types.
```

### TASK-023: Evaluate in App Code
```yaml
id: TASK-023
path: docusaurus/docs/quickstart/evaluate-in-app-code.md
tier: L1
axis: Procedural
specificity: 3
claims: [CLM-PR01-09A]
requires: [TASK-022]
brief: >
  Create a Context with locale, platform, version, stableId. Call evaluate().
  Show the deterministic result. Verify: evaluation returns expected default.
  Introduce FlagDefinition.evaluate() and Bucketing conceptually.
```

### TASK-024: Add Deterministic Ramp-Up
```yaml
id: TASK-024
path: docusaurus/docs/quickstart/add-deterministic-ramp-up.md
tier: L1
axis: Procedural
specificity: 3
claims: [CLM-PR01-10A]
requires: [TASK-023]
brief: >
  Add a rollout rule with a percentage. Demonstrate stable bucketing:
  same stableId → same result across evaluations. Show RampUpBucketing.bucket()
  and Bucketing.stableBucket() by effect, not by calling them directly.
  Verify: same user always gets same bucket.
```

### TASK-025: Load First Snapshot Safely
```yaml
id: TASK-025
path: docusaurus/docs/quickstart/load-first-snapshot-safely.md
tier: L1
axis: Procedural
specificity: 3
claims: [CLM-PR01-11A, CLM-PR01-11B]
requires: [TASK-024]
brief: >
  Load a JSON string via NamespaceSnapshotLoader.load(). Handle Result success
  and failure. Show that invalid JSON produces ParseError wrapped in
  KonditionalBoundaryFailure. Show that last-known-good survives bad input.
  Verify: good JSON loads; bad JSON rejected; evaluation still works after rejection.
```

### TASK-026: Verify End-to-End
```yaml
id: TASK-026
path: docusaurus/docs/quickstart/verify-end-to-end.md
tier: L1
axis: Procedural
specificity: 3
claims: [CLM-PR01-12A]
requires: [TASK-025]
brief: >
  Checklist with assertion code for each guarantee: determinism (same input →
  same output), boundary rejection (bad JSON → failure, not crash),
  namespace independence (loading namespace A doesn't affect namespace B).
  Links to theory/ pages for formal proofs.
```

---

## Phase 4 — Concepts (any order within phase)

### TASK-030 through TASK-036
```yaml
# TASK-030: concepts/namespaces.md
# TASK-031: concepts/features-and-types.md
# TASK-032: concepts/rules-and-precedence.md
# TASK-033: concepts/context-and-targeting.md
# TASK-034: concepts/evaluation-model.md
# TASK-035: concepts/parse-boundary.md
# TASK-036: concepts/configuration-lifecycle.md
#
# Each follows the pattern in DOCS-ARCHITECTURE-PLAN.md §3.3.
# Tier: L1-L2, Axis: Orientation, Specificity: 3-4.
# Each links to corresponding theory/ page for formal guarantees.
# Each uses the SKILL-docs-authoring.md constraints.
```

---

## Phase 5 — Guides (any order within phase)

### TASK-040 through TASK-047
```yaml
# TASK-040: guides/remote-configuration.md
# TASK-041: guides/incremental-updates.md
# TASK-042: guides/custom-structured-values.md
# TASK-043: guides/custom-targeting-axes.md
# TASK-044: guides/namespace-per-team.md
# TASK-045: guides/testing-strategies.md
# TASK-046: guides/migration-from-legacy.md         (NEW — addresses eval §4.2)
# TASK-047: guides/enterprise-adoption.md            (NEW — addresses eval §4.2)
#
# Each follows the pattern in DOCS-ARCHITECTURE-PLAN.md §3.4.
# Tier: L1-L2, Axis: Procedural, Specificity: 3-4.
```

---

## Phase 6 — Theory and Positioning

### TASK-050 through TASK-057
```yaml
# TASK-050: theory/type-safety-boundaries.md
# TASK-051: theory/determinism-proofs.md
# TASK-052: theory/namespace-isolation.md
# TASK-053: theory/parse-dont-validate.md
# TASK-054: theory/atomicity-guarantees.md
# TASK-055: theory/migration-and-shadowing.md
# TASK-056: overview/competitive-positioning.md      (NEW — addresses eval §4.2)
# TASK-057: overview/adoption-roadmap.md
# TASK-058: overview/product-value-fit.md
#
# Theory pages: Tier L2, Axis Guarantee, Specificity 5.
# Overview pages: Tier L1, Axis Orientation, Specificity 2.
```

---

## Phase 7 — Reference Completion and Appendix

### TASK-060 through TASK-066
```yaml
# TASK-060: reference/snapshot-format.md
# TASK-061: reference/patch-format.md
# TASK-062: reference/snapshot-load-options.md
# TASK-063: reference/evaluation-diagnostics.md
# TASK-064: appendix/faq.md
# TASK-065: appendix/changelog.md
# TASK-066: index.md (final landing page rewrite)
```
