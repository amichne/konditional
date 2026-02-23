---
name: docs-authoring
description: "Author and revise Konditional Docusaurus v3 MDX pages using the docs architecture plan, disclosure tiers (L0-L2), and claim-trace artifacts. Use when creating or updating docs pages that need claim anchors, public-API-only Kotlin examples, prerequisite-aware flow, cross-links, and conformance checks."
---

# Docs Authoring

## Objective

Produce Konditional documentation pages that follow the architecture plan and remain tightly aligned to public API claims.

## Inputs Per Task

Require these inputs before writing:

1. Target page path (for example `docusaurus/docs/quickstart/define-first-flag.md`)
2. Disclosure tier (`L0`, `L1`, or `L2`)
3. Content axis (`Orientation`, `Procedural`, `Reference`, or `Guarantee`)
4. Specificity target (`0` to `5`)
5. Claim IDs to cover (from `claims-registry.json`)
6. Prerequisite pages readers are assumed to have completed

## Primary Sources

Load these before drafting when they are present:

- `DOCS-ARCHITECTURE-PLAN.md`
- `docs/claim-trace/claims-registry.json`
- `docs/claim-trace/claim-signature-links.json`
- Any page-specific prerequisites referenced by the task

## Hard Constraints

### Do

- Use only public API signatures represented by `claim-signature-links.json`.
- Add an HTML anchor with `id="claim-{CLAIM_ID}"` for every claim-backed behavioral statement.
- Write Kotlin snippets that compile against the published public API surface.
- Add links to deeper-tier pages for introduced concepts that are not fully explained on the current page.
- Put prerequisites at the top of procedural pages.
- End procedural pages with a verifiable outcome.

### Do Not

- Reference classes from `internal/` packages.
- Describe Moshi adapters, builder internals, or serialization wire details beyond documented schema.
- Use marketing language, superlatives, or unsupported claims.
- Introduce more than one new public API concept per procedural page.
- Assume reading outside explicitly stated prerequisites.
- Depend on implementation details likely to change under internal refactors.

## Voice By Tier

- `L0`: concise and outcome-oriented.
- `L1`: stepwise and practical.
- `L2`: precise, evidence-linked, and mechanism-aware.

Across all tiers:

- Prefer concrete examples over abstraction.
- Address readers as `you` and refer to the framework as `Konditional`.
- Keep formatting clean: minimal emphasis, structural headers, no emoji.
- State trade-offs directly when relevant.

## Page Skeleton

```mdx
---
title: "{Page Title}"
sidebar_position: {N}
---

# {Page Title}

{One-paragraph summary of what this page covers and what the reader gains.}

{For procedural pages: "**Prerequisites:** You have completed [X](link) and [Y](link)."}

## {First Section}

{Content}

## {Next Section}

{Content}

---

## Next Steps

- [{Related deeper page}](link) - {one-line description}
- [{Related adjacent page}](link) - {one-line description}
```

## Code Block Rules

- Always set code fence languages (`kotlin`, `json`, and so on).
- Use public API only (`Namespace`, `Feature`, `FlagDefinition`, `Context`, `evaluate()`, `SnapshotLoader.load()`).
- Keep comments focused on intent and behavior, not internals.
- Keep quickstart snippets runnable in sequence.
- Keep concept/reference snippets illustrative instead of procedural unless explicitly required.

## Claim Anchors

When writing a claim-backed statement, emit a preceding anchor:

```html
<span id="claim-clm-pr01-03a"></span>
```

Then state the claim in normal prose. Normalize claim IDs to lowercase with hyphens in anchor IDs.

## Cross-Linking Rules

- Link quickstarts/guides to concept pages when introducing terms.
- Link concept pages to theory pages for guarantees and proofs.
- Link to reference pages when mentioning types/options with exhaustive docs.
- Avoid upward prerequisite coupling (for example, do not require `L0` as a prerequisite for `L2`).

## Definition of Done

Before completion, confirm all are true:

- Disclosure tier and specificity match the architecture plan.
- All required claim IDs are represented with anchors.
- No internal API references exist.
- Kotlin code uses public API only.
- Procedural prerequisites are explicit.
- Procedural pages end with a verifiable expected outcome.
- `Next Steps` includes 1 to 3 relevant links.
- No unsubstantiated claims about performance, scale, or superiority.
- Trade-offs are explicitly documented where needed.

## Optional Validation

If available, run:

```bash
scripts/check-docs-api-conformance.sh
```

Fix any claim-anchor or API-surface violations before finalizing edits.
