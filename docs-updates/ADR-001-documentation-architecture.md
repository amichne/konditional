# ADR-001: Documentation Information Architecture

**Status:** Proposed
**Date:** 2026-02-22
**Context:** Pre-publication documentation rewrite for Konditional framework

## Decision

Adopt a **progressive-disclosure, multi-axis** information architecture using three disclosure tiers (L0/L1/L2) crossed with four content axes (Orientation/Procedural/Reference/Guarantee), organized into seven top-level directories.

## Rationale

### Problem
The existing documentation (flat numbered files: `01-getting-started.md` through `09-why-konditional.md`) conflates orientation, procedural, and reference content within single pages. An external evaluation (Manus AI, Jan 2026) identified three specific gaps: no competitive positioning, insufficient enterprise guidance, and no migration path documentation.

### Why progressive disclosure
Enterprise documentation serves readers with fundamentally different intents: executives evaluating adoption (L0), engineers implementing integration (L1), and architects validating guarantees (L2). A flat structure forces all readers through the same depth, overwhelming casual evaluators and frustrating deep researchers.

### Why multi-axis over flat taxonomy
The Diátaxis framework's four-quadrant model (tutorials, how-to, reference, explanation) maps well to Konditional's needs, but the framework adds a fifth concern: **provable guarantees**. The `theory/` directory serves this need — it's not explanation (which is conceptual) but evidence-linked trust documentation. This is critical for enterprise adoption where architects need to verify claims, not just understand concepts.

### Why claims traceability
The `claims-registry.json` and `claim-signature-links.json` establish a machine-checkable link between documentation assertions and code signatures. By embedding anchor IDs in pages and running conformance checks, documentation drift becomes a CI-detectable defect rather than a silent regression.

## Consequences

### Positive
- Readers self-select depth; no one is overwhelmed
- New pages (competitive positioning, migration, enterprise adoption) directly address evaluation weaknesses
- Claims traceability prevents documentation drift
- Phased delivery allows incremental publication
- Agent-agnostic skill file enables any LLM system to produce conforming pages

### Negative
- More pages to maintain (35 vs. current 9)
- Cross-linking discipline required to prevent orphan pages
- Claims registry must be updated when API surface changes

### Risks
- Custom structured values (`KotlinEncodeable`) documentation may need revision once the API stabilizes
- `server/*` and `openfeature/` integration pages are scoped out of Phase 1 — these will need their own plan when those modules are ready for public documentation

## Alternatives Considered

1. **Expand existing flat structure** — rejected because it doesn't solve the disclosure problem; adding competitive positioning to a flat list doesn't help readers find it at the right moment.

2. **Pure Diátaxis four-quadrant** — rejected because the guarantee/proof content doesn't fit cleanly into "explanation" (which is conceptual) or "reference" (which is exhaustive lookup). A dedicated `theory/` axis serves this better.

3. **Single-page documentation** — rejected for enterprise audience; the evaluation explicitly calls for a modular, navigable structure.
