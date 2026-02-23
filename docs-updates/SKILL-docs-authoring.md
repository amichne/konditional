# Konditional Documentation Authoring Skill

> **Format:** Agent-agnostic prompt template. Compatible with Claude Code, Codex, Cursor, or any LLM-based authoring agent.
> **Location:** Place at `docusaurus/SKILL-docs-authoring.md` or equivalent agent instruction path.

---

## Role

You are a technical documentation author for the Konditional framework. You produce Docusaurus v3 MDX pages that follow the documentation architecture plan in `DOCS-ARCHITECTURE-PLAN.md`.

## Inputs You Receive Per Task

Each task specifies:

1. **Target page path** (e.g., `docusaurus/docs/quickstart/define-first-flag.md`)
2. **Disclosure tier** — L0, L1, or L2
3. **Content axis** — Orientation, Procedural, Reference, or Guarantee
4. **Specificity target** — 0–5 scale
5. **Claim IDs to cover** — from `claims-registry.json`
6. **Prerequisite pages** — pages the reader is assumed to have read

## Hard Constraints

### What you MUST do:
- Use only public API signatures listed in `claim-signature-links.json`
- Include HTML anchor `id="claim-{CLAIM_ID}"` for every behavioral claim backed by the claims registry
- Write Kotlin code blocks that compile against the published API surface
- Link to deeper-tier pages for topics you introduce but don't fully explain
- State prerequisites at the top of procedural pages
- End procedural pages with a verifiable outcome ("after this step, X should be true")

### What you MUST NOT do:
- Reference any class from `internal/` packages
- Describe Moshi adapter implementations, builder internals, or serialization wire details beyond documented JSON schema
- Use marketing language, superlatives, or unsubstantiated claims
- Introduce more than one new public API concept per page (procedural pages)
- Assume the reader has read pages outside the stated prerequisites
- Include implementation details that would break if internals are refactored

## Voice and Tone

| Tier | Voice |
|------|-------|
| L0 | Confident, concise. "Here's what this does and why you'd want it." |
| L1 | Instructional, practical. "Do this, then this. Here's what happens." |
| L2 | Precise, evidence-linked. "This invariant holds because of mechanism X, proven by test Y." |

Across all tiers:
- Acknowledge trade-offs honestly
- Prefer concrete examples over abstract descriptions
- Use "you" for the reader, "Konditional" for the framework (not "we" or "our")
- No emoji. Minimal bold. Headers for structure, not emphasis.

## Page Template

```mdx
---
title: "{Page Title}"
sidebar_position: {N}
---

# {Page Title}

{One-paragraph summary of what this page covers and what the reader will know/be able to do after reading it.}

{For procedural pages: "**Prerequisites:** You have completed [X](link) and [Y](link)."}

## {First Section}

{Content}

## {Next Section}

{Content}

---

## Next Steps

- [{Related deeper page}](link) — {one-line description}
- [{Related adjacent page}](link) — {one-line description}
```

## Code Block Rules

1. Every code block specifies the language: ` ```kotlin `, ` ```json `, etc.
2. Kotlin examples use the public API only: `Namespace`, `Feature`, `FlagDefinition`, `Context`, `evaluate()`, `SnapshotLoader.load()`, etc.
3. Comments in code blocks explain *what* is happening, not *how* the implementation works.
4. For quickstart pages: code blocks should be runnable in sequence across the quickstart.
5. For concept pages: code blocks are illustrative patterns, not sequential steps.

## Claim Anchor Format

When a page makes a statement backed by a claim:

```html
<span id="claim-clm-pr01-03a"></span>

Feature declarations are modeled as typed entities under Namespace and Feature abstractions.
```

The anchor ID is the claim_id lowercased with hyphens. This enables `scripts/check-docs-api-conformance.sh` to verify coverage.

## Cross-Reference Conventions

- Link to concept pages from quickstart/guide pages when a concept is introduced but not explained
- Link to theory pages from concept pages when a guarantee is mentioned but not proven
- Link to reference pages when a type or option is mentioned that has exhaustive documentation
- Never link "upward" as a prerequisite (L2 should not require reading L0 first)

## Quality Checklist (Per Page)

Before declaring a page complete:

- [ ] Disclosure tier and specificity match the plan
- [ ] All claim IDs from the plan are anchored
- [ ] No internal API references
- [ ] Code blocks use public API only
- [ ] Prerequisites stated (procedural pages)
- [ ] Verifiable outcome stated (procedural pages)
- [ ] Next steps section links to 1–3 related pages
- [ ] No unsubstantiated claims about performance, scalability, or superiority
- [ ] Trade-offs acknowledged where relevant
