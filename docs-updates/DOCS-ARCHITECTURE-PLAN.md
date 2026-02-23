# Konditional Documentation Rewrite — Architecture Plan

> **Audience for this plan:** Agentic systems (Claude Code, Codex, Cursor, or human authors) that will produce individual pages.
> **Output format:** Docusaurus v3, MDX, deployed at `docs.konditional.dev` or similar.

---

## 1. Design Principles

### 1.1 Progressive Disclosure

Every reader enters with a different goal and tolerance for depth. The information architecture uses three disclosure tiers:

| Tier | Label | Reader intent | Depth (0–5 scale) |
|------|-------|--------------|-------------------|
| L0 | **Glance** | "Should I care?" | 0–1: tagline, value prop, one code block |
| L1 | **Orient** | "How does this work at a high level?" | 2–3: concepts, flows, happy-path walkthrough |
| L2 | **Master** | "I need to understand the guarantees and edge cases" | 4–5: invariants, proofs, failure modes, migration algebra |

Each directory (horizontal section) contains pages at all three tiers. Readers self-select depth via sidebar navigation; they are never forced to read L2 material to complete an L1 task.

### 1.2 Vertical Axes (Content Types)

Adapted from the Diátaxis framework with a Konditional-specific fourth axis:

| Axis | Purpose | Voice | Example |
|------|---------|-------|---------|
| **Orientation** | Understanding and decision-making | Explanatory, narrative | "Why Typed Flags" |
| **Procedural** | Doing a thing, start to finish | Imperative, copy-paste | "Define Your First Flag" |
| **Reference** | Looking up specifics | Terse, exhaustive | "Snapshot Load Options" |
| **Guarantee** | Trusting the framework under pressure | Formal, evidence-linked | "Determinism Proof" |

### 1.3 Horizontal Axes (Topic Domains)

Mapped to the framework's module boundaries, not to internal package structure:

| Domain | Primary module(s) | Claim topics covered |
|--------|-------------------|---------------------|
| **Declaration** | `konditional-core` | `type-safety`, `namespace-isolation` |
| **Evaluation** | `konditional-core` | `determinism`, `ramp-up` |
| **Boundary** | `konditional-serialization`, `konditional-runtime` | `parse-boundary`, `snapshot-loading` |
| **Operations** | `konditional-runtime` | `rollback`, `namespace-isolation` |
| **Integration** | `server/*`, `openfeature/`, `kontracts/` | (future) |

### 1.4 Implementation-Detail Exclusion

Documentation describes **behavioral contracts and public API shapes**. It must not expose:
- Internal class names from `internal/` packages
- Moshi adapter implementation details
- Builder/DSL internals (`FlagBuilder`, `RuleBuilder`, etc.)
- Serialization wire details beyond the documented JSON schema

Public signatures from the `claims-registry.json` and `claim-signature-links.json` define the boundary of what may be referenced.

---

## 2. Site Map

```
docusaurus/docs/
├── index.md                          # L0 — Landing + value proposition
├── overview/                         # L0–L1 — Decision and orientation content
│   ├── start-here.md                 # L0 — One-page "what is this and why"
│   ├── why-typed-flags.md            # L1 — Problem/solution narrative
│   ├── product-value-fit.md          # L1 — Fit criteria (with self-disqualification)
│   ├── first-success-map.md          # L0 — Decision tree routing readers to the right path
│   ├── adoption-roadmap.md           # L1 — Phased adoption with rollback plan
│   └── competitive-positioning.md    # L1 — Konditional vs. alternatives (NEW)
├── quickstart/                       # L1 — Procedural, end-to-end happy path
│   ├── index.md                      # L1 — Quickstart landing + prerequisites
│   ├── install.md                    # L1 — Gradle dependency setup
│   ├── define-first-flag.md          # L1 — Namespace + Feature + FlagDefinition
│   ├── evaluate-in-app-code.md       # L1 — Context + evaluate()
│   ├── add-deterministic-ramp-up.md  # L1 — Bucketing + RampUp
│   ├── load-first-snapshot-safely.md # L1 — SnapshotLoader + Result boundary
│   └── verify-end-to-end.md          # L1 — Integration check + known-good assertion
├── concepts/                         # L1–L2 — Concept explanations (orientation axis)
│   ├── namespaces.md                 # L1 — Isolation model, ownership, blast radius
│   ├── features-and-types.md         # L1 — Feature<T>, value types, enum features
│   ├── rules-and-precedence.md       # L1 — Rule DSL, matching, ordering
│   ├── context-and-targeting.md      # L1 — Context, Axis, targeting scopes
│   ├── evaluation-model.md           # L2 — Total evaluation, deterministic guarantees
│   ├── parse-boundary.md             # L2 — Result types, ParseError, boundary failures
│   └── configuration-lifecycle.md    # L1 — Load, rollback, disable, atomic swap
├── guides/                           # L1–L2 — Task-oriented how-to content
│   ├── remote-configuration.md       # L1 — JSON loading pipeline
│   ├── incremental-updates.md        # L1 — Patch application
│   ├── custom-structured-values.md   # L2 — Konstrained, schema DSL
│   ├── custom-targeting-axes.md      # L2 — Axis<T>, AxisValue, registration
│   ├── namespace-per-team.md         # L1 — Multi-namespace setup for modular codebases
│   ├── testing-strategies.md         # L1 — Test fixtures, deterministic assertions
│   ├── migration-from-legacy.md      # L1 — Shadow evaluation, dual-run (NEW)
│   └── enterprise-adoption.md        # L1 — CI/CD integration, config delivery (NEW)
├── reference/                        # L2 — Exhaustive API and format reference
│   ├── api-surface.md                # L2 — Public type/method catalog
│   ├── snapshot-format.md            # L2 — JSON schema, field semantics
│   ├── patch-format.md               # L2 — Incremental update schema
│   ├── snapshot-load-options.md      # L2 — Strategy enums, warning model
│   ├── evaluation-diagnostics.md     # L2 — Diagnostic output model
│   └── module-dependency-map.md      # L2 — Which modules to depend on and why
├── theory/                           # L2 — Guarantee/proof content
│   ├── type-safety-boundaries.md     # L2 — Compile-time vs. runtime guarantees
│   ├── determinism-proofs.md         # L2 — Bucketing, evaluation, ordering
│   ├── namespace-isolation.md        # L2 — Isolation invariants
│   ├── parse-dont-validate.md        # L2 — Boundary philosophy
│   ├── atomicity-guarantees.md       # L2 — Linearizability, concurrent safety
│   └── migration-and-shadowing.md    # L2 — Shadow evaluation algebra
└── appendix/                         # L2 — Supplementary content
    ├── glossary.md                   # L2 — Canonical term definitions
    ├── faq.md                        # L1 — Common questions
    └── changelog.md                  # L0 — Release notes
```

---

## 3. Directory Specifications

### 3.1 `overview/` — Orientation, L0–L1

**Purpose:** Convince readers this framework is worth their time. Answer "should I adopt this?" without requiring them to read code.

**Content coverage: depth 1–2.** Each page should contain a single clear thesis, one or two code blocks maximum (illustrative, not instructional), and explicit cross-links to deeper material.

**Pages:**

| Page | Thesis | Claim IDs covered | Specificity |
|------|--------|-------------------|-------------|
| `start-here.md` | Konditional provides type-safe, deterministic feature management with explicit JSON boundaries | CLM-PR01-01A, CLM-PR01-01B | 1 |
| `why-typed-flags.md` | String-based flags are a class of production risk that compile-time types eliminate | CLM-PR01-03A, CLM-PR01-03B | 2 |
| `product-value-fit.md` | Decision criteria: when Konditional fits and when it doesn't (including self-disqualification) | CLM-PR01-02A, CLM-PR01-02B | 2 |
| `first-success-map.md` | Decision tree routing readers to quickstart, guides, or theory based on their goal | CLM-PR01-04A | 1 |
| `adoption-roadmap.md` | Three-phase adoption plan (PoC → operationalize → broad rollout) with rollback at each phase | CLM-PR01-05A | 2 |
| `competitive-positioning.md` **(NEW)** | Konditional vs. Togglz, FF4J, OpenFeature, LaunchDarkly — honest trade-off matrix | — | 2 |

**Constraint:** `competitive-positioning.md` directly addresses the Manus evaluation's §4.2 weakness ("lack of competitive positioning"). It must include a comparison table, acknowledge weaknesses (no GUI, Kotlin-only), and frame trade-offs rather than market.

### 3.2 `quickstart/` — Procedural, L1

**Purpose:** Get a reader from zero to working evaluation in a single sitting. Every page ends with a runnable state.

**Content coverage: depth 2–3.** Copy-paste code, expected outputs, explicit prerequisites. No theory. Link to `concepts/` and `theory/` for "why" questions.

| Page | Outcome when complete | Claim IDs | Specificity |
|------|----------------------|-----------|-------------|
| `index.md` | Reader knows prerequisites and path through quickstart | CLM-PR01-06A | 1 |
| `install.md` | Gradle dependencies added, project compiles | CLM-PR01-07A | 2 |
| `define-first-flag.md` | Namespace + Feature declared, compiles | CLM-PR01-08A, CLM-PR01-08B | 3 |
| `evaluate-in-app-code.md` | `evaluate(ctx)` returns expected value | CLM-PR01-09A | 3 |
| `add-deterministic-ramp-up.md` | Rollout bucketing returns stable results | CLM-PR01-10A | 3 |
| `load-first-snapshot-safely.md` | JSON parsed via Result boundary, loaded into registry | CLM-PR01-11A, CLM-PR01-11B | 3 |
| `verify-end-to-end.md` | Assertion checklist: determinism, boundary rejection, namespace independence | CLM-PR01-12A | 3 |

**Constraint:** Each page must show exactly one concept. No page should introduce more than one new type from the public API.

### 3.3 `concepts/` — Orientation, L1–L2

**Purpose:** Explain how Konditional works at the model level. Reader understands the "shape" of the system without needing to read source code.

**Content coverage: depth 2–4.** Diagrams encouraged. Code blocks show API usage patterns, not implementation. Each page links to the corresponding `theory/` page for formal guarantees.

| Page | Core question answered | Specificity |
|------|----------------------|-------------|
| `namespaces.md` | How does isolation work? What's a namespace boundary? | 3 |
| `features-and-types.md` | What types can a feature carry? How do enums and custom types work? | 3 |
| `rules-and-precedence.md` | How are rules evaluated? What wins when multiple match? | 3 |
| `context-and-targeting.md` | What runtime inputs drive evaluation? How do custom axes work? | 3 |
| `evaluation-model.md` | What does "total evaluation" mean? What are the determinism boundaries? | 4 |
| `parse-boundary.md` | How does the JSON trust boundary work? What happens on failure? | 4 |
| `configuration-lifecycle.md` | Load, rollback, disable — what's the state machine? | 3 |

### 3.4 `guides/` — Procedural, L1–L2

**Purpose:** Task-oriented recipes for specific scenarios beyond the quickstart.

**Content coverage: depth 2–4.** Each guide is self-contained with prerequisites stated up front. Includes two NEW pages that address the Manus evaluation's §4.2 gaps.

| Page | Scenario | Specificity |
|------|----------|-------------|
| `remote-configuration.md` | Set up a JSON loading pipeline (fetch, parse, load) | 3 |
| `incremental-updates.md` | Apply patches without full snapshot reload | 3 |
| `custom-structured-values.md` | Define a `Konstrained` data class as a feature value | 4 |
| `custom-targeting-axes.md` | Register a custom `Axis<T>` and use it in rules | 4 |
| `namespace-per-team.md` | Multi-team namespace setup in a modular codebase | 3 |
| `testing-strategies.md` | Test fixtures, deterministic assertions, fixture-based golden tests | 3 |
| `migration-from-legacy.md` **(NEW)** | Shadow evaluation, dual-run, mismatch reporting during migration | 3 |
| `enterprise-adoption.md` **(NEW)** | CI/CD integration patterns, config delivery strategies, internal tooling | 3 |

**Constraint:** `migration-from-legacy.md` directly addresses the evaluation's §4.2 weakness ("insufficient enterprise guidance"). It must describe a concrete path from enum-based or string-based flags to Konditional, including shadow evaluation and rollback.

### 3.5 `reference/` — Reference, L2

**Purpose:** Exhaustive, terse lookup. No narrative. Tables, type signatures, enumerations.

**Content coverage: depth 4–5.** Every public type and method in the claims registry must appear here. Organized by module, then by type.

| Page | Coverage | Specificity |
|------|----------|-------------|
| `api-surface.md` | All public types/methods from `claim-signature-links.json`, grouped by module | 5 |
| `snapshot-format.md` | JSON schema with field descriptions, valid ranges, examples | 5 |
| `patch-format.md` | Patch JSON schema, merge semantics, error conditions | 5 |
| `snapshot-load-options.md` | `SnapshotLoadOptions`, `UnknownFeatureKeyStrategy`, `MissingDeclaredFlagStrategy`, `SnapshotWarning` | 5 |
| `evaluation-diagnostics.md` | `EvaluationDiagnostics` output model, interpretation guide | 4 |
| `module-dependency-map.md` | Which Gradle modules to depend on for which capability | 4 |

### 3.6 `theory/` — Guarantee, L2

**Purpose:** Formal proofs and invariant documentation for architects and security reviewers. These are the framework's "trust contracts."

**Content coverage: depth 5.** Each page states invariants, describes mechanisms (not implementations), and links to test classes that prove the invariant.

| Page | Invariant domain | Test evidence |
|------|-----------------|---------------|
| `type-safety-boundaries.md` | Compile-time key/value safety; runtime boundary rejection | `FlagEntryTypeSafetyTest`, `BoundaryFailureResultTest` |
| `determinism-proofs.md` | Stable bucketing, evaluation ordering, salt semantics | `MissingStableIdBucketingTest`, `ConditionEvaluationTest` |
| `namespace-isolation.md` | Cross-namespace blast radius, independent lifecycle | `NamespaceLinearizabilityTest`, `NamespaceFeatureDefinitionTest` |
| `parse-dont-validate.md` | Boundary philosophy, Result-based error propagation | `BoundaryFailureResultTest`, `ConfigurationSnapshotCodecTest` |
| `atomicity-guarantees.md` | Linearizable load/read, no partial state observation | `NamespaceLinearizabilityTest`, `ConcurrencyAttacksTest` |
| `migration-and-shadowing.md` | Shadow evaluation algebra, mismatch detection | `KillSwitchTest` |

### 3.7 `appendix/`

| Page | Purpose | Specificity |
|------|---------|-------------|
| `glossary.md` | Canonical definitions for Namespace, Feature, FlagDefinition, Context, Rule, Snapshot, Patch, ParseError, etc. | 3 |
| `faq.md` | "Does it work with Java?", "Where's the UI?", "How does it compare to LaunchDarkly?" | 2 |
| `changelog.md` | Release notes | 1 |

---

## 4. Cross-Cutting Concerns

### 4.1 Claim Traceability

Every page that makes a behavioral claim must include an HTML anchor following the pattern `id="claim-{CLAIM_ID}"` where `CLAIM_ID` matches the `claims-registry.json`. This enables automated conformance checking via `scripts/check-docs-api-conformance.sh`.

### 4.2 Addressing Evaluation Weaknesses

The Manus AI evaluation identified three documentation weaknesses. The plan addresses each:

| Weakness | Section reference | Addressing pages |
|----------|------------------|-----------------|
| No competitive positioning | §4.2 | `overview/competitive-positioning.md`, `appendix/faq.md` |
| Insufficient enterprise guidance | §4.2 | `guides/enterprise-adoption.md`, `guides/migration-from-legacy.md`, `overview/adoption-roadmap.md` |
| Silent on Java interoperability | §4.2 | `appendix/faq.md` (scoped answer; full guide deferred until Java wrapper exists) |

### 4.3 Navigation Architecture

Sidebar navigation uses two organizing principles:

**Vertical (primary):** Reader intent → "Getting Started", "Understand", "Do", "Look Up", "Trust"
**Horizontal (secondary):** Topic domain → namespaces, evaluation, boundaries, operations

The sidebar config file (`sidebars.ts`) should be generated from the site map above.

---

## 5. Page Production Constraints

These constraints apply to all pages and should be embedded in any agentic prompt or skill that produces content:

1. **No implementation details.** Reference public API signatures only. Never mention classes from `internal/` packages.
2. **Code blocks must compile.** Every Kotlin code block should be extractable and compilable against the published API.
3. **Progressive linking.** L0 pages link to L1. L1 pages link to L2. Never link upward as a prerequisite.
4. **Claim anchors.** Every behavioral statement backed by a claim in the registry gets an anchor.
5. **Single responsibility.** Each page introduces at most one new concept or completes one task.
6. **Self-contained guides.** Guides state prerequisites; they don't assume prior reading of other guides.
7. **Honest trade-offs.** When discussing strengths, acknowledge the corresponding limitation. (No GUI → opportunity for internal tooling. Kotlin-only → natural for Kotlin shops, wrapper needed for Java.)

---

## 6. Delivery Sequence

Pages should be produced in dependency order. Later pages may reference earlier ones.

**Phase 1 — Foundation (blocks nothing else):**
- `appendix/glossary.md`
- `reference/module-dependency-map.md`
- `reference/api-surface.md`

**Phase 2 — Orientation (enables quickstart and guides):**
- `overview/start-here.md`
- `overview/why-typed-flags.md`
- `overview/first-success-map.md`

**Phase 3 — Quickstart (end-to-end procedural path):**
- All `quickstart/` pages in order

**Phase 4 — Concepts (fills in understanding):**
- All `concepts/` pages (any order)

**Phase 5 — Guides (task-oriented expansion):**
- All `guides/` pages (any order)

**Phase 6 — Theory and Positioning (trust and decision content):**
- All `theory/` pages
- `overview/competitive-positioning.md`
- `overview/adoption-roadmap.md`
- `overview/product-value-fit.md`

**Phase 7 — Reference completion:**
- Remaining `reference/` pages
- `appendix/faq.md`
- `appendix/changelog.md`
