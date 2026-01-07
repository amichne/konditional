# Final Phases: Documentation Reorganization

Phase 3: Progressive Disclosure (Polish)

Goal

Add collapsible sections to API reference documentation so essential content is visible while advanced details are accessible but not overwhelming.

Tasks

1. Enhance Core API Reference

File: docs/core/reference.md

Add collapsible sections using Docusaurus details/summary:

## Essential APIs

### Feature.evaluate()
[Always visible core content]

### Namespace.load()
[Always visible core content]

  <details>
  <summary>Advanced Options</summary>

### Feature.explain()
[Collapsed by default]

### Custom Predicates
[Collapsed by default]

  </details>

  <details>
  <summary>Guarantees & Boundaries</summary>

### Type Safety Guarantee
[Collapsed by default]

### Determinism Guarantee
[Collapsed by default]

  </details>

2. Enhance Core Types Reference

File: docs/core/types.md

Same pattern:
- Essential types (Feature, Context, Namespace) - always visible
- Advanced types - collapsed
- Type constraints - collapsed

3. Enhance Module Reference Files

Files to update:
- docs/runtime/operations.md
- docs/serialization/reference.md
- docs/observability/reference.md
- docs/opentelemetry/reference.md
- docs/config-metadata/reference.md

Each should have:
- Essential APIs (5-8 items) - always visible
- Advanced options - collapsed
- Guarantees & boundaries - collapsed

  ---
Phase 4: Content Integration (Completeness)

Goal

Fix broken links, enhance module context, simplify theory docs to focus on proofs.

Tasks

1. Fix Broken Links (~47 links)

High Priority Links (appear on many pages):
- /observability/index/ → /observability/
- /serialization/index/ → /serialization/
- /runtime/index/ → /runtime/

Deleted Pages to Update References For:
- /rules-and-targeting/rule-composition/ → /core/rules/
- /rules-and-targeting/rollout-strategies/ → /how-to-guides/rolling-out-gradually/
- /api-reference/observability/ → /observability/reference/
- /fundamentals/definition-vs-initialization/ → Remove or redirect
- /fundamentals/refresh-safety/ → /production-operations/thread-safety/
- /fundamentals/failure-modes/ → /production-operations/failure-modes/
- /fundamentals/trust-boundaries/ → /fundamentals/type-safety/
- /api-reference/serialization/ → /serialization/reference/
- /persistence-format/ → /serialization/persistence-format/
- /advanced/shadow-evaluation/ → /observability/shadow-evaluation/
- /getting-started/loading-from-json/ → Remove or integrate into quick-start

Systematic Approach:
1. Run search for each broken link
2. Update to new path or remove if no longer relevant
3. Re-test build

2. Enhance Module Index Pages with "Why Use This"

Add context to each module index explaining when to use it:

Template:
# [Module Name]

[Current overview - keep]

## When to Use This Module

You should use `konditional-[module]` when you need to:
- [Primary use case]
- [Secondary use case]
- [Tertiary use case]

## What You Get

[Current content - keep]

## Alternatives

Without this module, you would need to:
- [Alternative 1] (more work, less type-safe)
- [Alternative 2] (vendor lock-in)

[Rest of current content]

Files to enhance (6 modules):
1. docs/runtime/index.md - "Use when you need remote configuration"
2. docs/serialization/index.md - "Use when you need to persist configuration"
3. docs/observability/index.md - "Use when you need instrumentation without vendor lock-in"
4. docs/opentelemetry/index.md - "Use when you need OpenTelemetry integration"
5. docs/config-metadata/index.md - "Use when you need to attach metadata"
6. docs/kontracts/index.md - "Use when you need custom JSON schema validation"

3. Simplify Theory Docs (Remove Practical Intros, Keep Proofs)

Files to simplify (6 files):

Pattern: Remove the "What this means" sections, keep only the formal proofs/mechanisms.

1. docs/theory/type-safety-boundaries.md
    - Remove: Practical implications intro (lines 1-97 approximately)
    - Keep: Mechanism details, type flow diagrams, formal boundaries
    - Link to: /fundamentals/type-safety/ for practical guide
2. docs/theory/parse-dont-validate.md
    - Remove: Intro explaining the concept
    - Keep: Philosophy, formal proof of boundary enforcement
    - Link to: Configuration Lifecycle for practical application
3. docs/theory/atomicity-guarantees.md
    - Remove: "What atomicity means" section
    - Keep: Formal proof, sequence diagrams, memory model
    - Link to: /production-operations/thread-safety/ for practical guide
4. docs/theory/determinism-proofs.md
    - Remove: Practical "why determinism matters"
    - Keep: SHA-256 bucketing proof, mathematical guarantees
    - Link to: /how-to-guides/rolling-out-gradually/ for practical application
5. docs/theory/namespace-isolation.md
    - Remove: "When to use namespaces" (already in how-to guide)
    - Keep: Formal isolation guarantees, state independence proof
    - Link to: /how-to-guides/namespace-isolation/ for practical guide
6. docs/theory/migration-and-shadowing.md
    - Keep as-is (already focused on theory)

For each file, add at top:
> **Looking for practical guidance?** See [relevant how-to guide link]

4. Enhance docs/fundamentals/core-primitives.md

Add "Why These Guarantees Matter" subsections:

## Feature

[Current content]

### Why This Matters
- Compile-time safety prevents typos
- Type flow eliminates casts
- [Link to type-safety fundamentals]

## Context

[Current content]

### Why This Matters
- Deterministic evaluation requires stable IDs
- [Link to debugging determinism]

[Continue pattern for each primitive]

  ---
Phase 3 Checklist

- Enhance docs/core/reference.md with collapsible sections
- Enhance docs/core/types.md with collapsible sections
- Enhance docs/runtime/operations.md with collapsible sections
- Enhance docs/serialization/reference.md with collapsible sections
- Enhance docs/observability/reference.md with collapsible sections
- Enhance docs/opentelemetry/reference.md with collapsible sections
- Enhance docs/config-metadata/reference.md with collapsible sections
- Test all collapsible sections render correctly

  ---
Phase 4 Checklist

Link Fixes

- Fix /observability/index/ → /observability/ (8 occurrences)
- Fix /serialization/index/ → /serialization/ (6 occurrences)
- Fix /runtime/index/ → /runtime/ (4 occurrences)
- Update all deleted page references (see list above)
- Run build and verify 0 broken links

Module Enhancements

- Enhance docs/runtime/index.md with "Why Use This"
- Enhance docs/serialization/index.md with "Why Use This"
- Enhance docs/observability/index.md with "Why Use This"
- Enhance docs/opentelemetry/index.md with "Why Use This"
- Enhance docs/config-metadata/index.md with "Why Use This"
- Enhance docs/kontracts/index.md with "Why Use This"

Theory Simplification

- Simplify docs/theory/type-safety-boundaries.md
- Simplify docs/theory/parse-dont-validate.md
- Simplify docs/theory/atomicity-guarantees.md
- Simplify docs/theory/determinism-proofs.md
- Simplify docs/theory/namespace-isolation.md
- Add practical guide links to all theory pages

Content Integration

- Enhance docs/fundamentals/core-primitives.md with "Why This Matters"
- Final build test
- Navigation flow test (click through all sections)
- Search test (verify glossary terms searchable)

  ---
Estimated Effort

- Phase 3: ~2-3 hours (progressive disclosure implementation)
- Phase 4: ~3-4 hours (link fixes + enhancements)
- Total: ~5-7 hours

  ---
Success Criteria

After Phases 3-4 are complete:

✅ Build passes with 0 broken links
✅ Progressive disclosure works (essential content visible, advanced collapsed)
✅ Module context clear ("Why use this module" answered for all 6 modules)
✅ Theory focused on proofs (practical content moved to fundamentals/how-tos)
✅ Navigation flows smoothly (Quick Start → Fundamentals → How-To → Reference → Theory)
✅ Approachability maintained (new users can navigate without overwhelm)
✅ Completeness preserved (production users find operational depth)

  ---
Quick Start for Phase 3

# Start with core reference
vim docusaurus/docs/core/reference.md

# Add collapsible sections:
  <details>
  <summary>Advanced Options</summary>
  ...content...
  </details>

# Test locally
npm --prefix docusaurus run start

# Verify collapsible sections work

Quick Start for Phase 4

# Fix most common broken links first
grep -r "/observability/index/" docusaurus/docs/
# Replace with /observability/

grep -r "/serialization/index/" docusaurus/docs/
# Replace with /serialization/

# Then tackle individual broken links
npm --prefix docusaurus run build 2>&1 | grep "Broken link"
