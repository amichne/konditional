---
name: value-journey-signature-linker
description: Update technical documentation with deterministic signature-linked evidence, including value journeys and general docs. Use when creating or revising docs that must stay synchronized with `signatures/*.sig` (or `.signatures/*.sig`), validating claim-to-signature coverage, and retrieving pseudo-RAG context for accurate edits.
---

# Value journey signature linker

## Overview

Use this skill to keep documentation persuasive and technically grounded.
Anchor documentation claims to explicit signatures, then retrieve deterministic
context from signatures and linked docs before writing updates.

## Operating modes

1. Journey mode (legacy): maintain `docs/value-journeys/*.md` and journey link
   maps.
2. General docs mode: update any documentation path and track claim links in a
   generic doc/signature map.

Both modes are supported by the same validator and retrieval workflow.

## Required artifacts

Produce these artifacts for each documentation batch:

1. Updated target documents (any path, for example `docusaurus/docs/**/*.md`).
2. A link map:
   - preferred: `docs/traceability/doc-signature-links.json`
   - legacy supported: `docs/value-journeys/journey-signature-links.json`
3. A validation report (for example
   `docs/traceability/doc-signature-report.json`).
4. Optional retrieval report for pseudo-RAG context (for example
   `docs/traceability/doc-evidence-query.json`).

## Workflow

1. Ensure signatures exist.
2. Build public-surface bootstrap context.
3. Define the document intent and adoption outcome:
   - who benefits
   - which pain is reduced
   - which gain is created
4. Update docs using evidence-linked claims.
5. Validate link coverage and auto-refresh signatures if needed.
6. Retrieve ranked evidence context before final wording changes.
7. Continue generation even if some links remain unresolved.
8. Record unresolved links in the report and in each document's open questions.

## Link-map contract

For journey-first docs, use
`references/journey_signature_link_schema.md`.

For generic documentation updates, use
`references/doc_signature_link_schema.md`.

The validator accepts these top-level containers:

- `documents` (preferred for general docs)
- `journeys` (current journey schema)
- `stories` (legacy compatibility)
- `records` (generic compatibility)

## Pseudo-RAG contract

Use `scripts/query_doc_evidence.py` as a deterministic retriever:

- corpus: signatures + linked docs (+ optional explicit doc globs)
- ranking: lexical overlap with stable deterministic tie-breakers
- output: JSON evidence pack suitable for grounding doc edits

This is intentionally deterministic and auditable, not embedding-based retrieval.

## Storytelling contract

Every adoption-oriented document must answer:

- Why this matters now (business and user urgency).
- What changes for the user (before versus after).
- How behavior proves the claim (signature-linked evidence).
- What success looks like (observable adoption signals).

Use concrete verbs and outcome statements, not feature-list prose.

## Commands

1. Generate or refresh signatures:

```bash
.agents/skills/llm-native-signature-spec/scripts/generate_signatures.sh --repo-root . --output-dir signatures
```

2. Build public-surface context:

```bash
python3 .agents/skills/public-surface-init-context/scripts/build_public_surface_context.py \
  --repo-root . \
  --signatures-dir signatures \
  --output signatures/PUBLIC_SURFACE.ctx
```

3. Validate generic documentation/signature links (preferred):

```bash
python3 .agents/skills/value-journey-signature-linker/scripts/validate_doc_signature_links.py \
  --repo-root . \
  --links-file docs/traceability/doc-signature-links.json \
  --report-out docs/traceability/doc-signature-report.json \
  --auto-refresh
```

4. Validate journey links (legacy path retained):

```bash
python3 .agents/skills/value-journey-signature-linker/scripts/validate_journey_signature_links.py \
  --repo-root . \
  --links-file docs/value-journeys/journey-signature-links.json \
  --report-out docs/value-journeys/journey-signature-report.json \
  --auto-refresh
```

5. Retrieve pseudo-RAG evidence before doc edits:

```bash
python3 .agents/skills/value-journey-signature-linker/scripts/query_doc_evidence.py \
  --repo-root . \
  --links-file docs/traceability/doc-signature-links.json \
  --docs-glob 'docusaurus/docs/**/*.md' \
  --query 'shadow mismatch rollout safety evidence' \
  --top-k 12 \
  --report-out docs/traceability/doc-evidence-query.json
```

6. Use `--strict` only when you intentionally want missing links (or empty
   retrieval results) to fail the run, for example CI gating.

## Missing-signature protocol

When a required symbol is not found:

1. Regenerate signatures and public-surface context.
2. Re-run validation.
3. If still unresolved, keep generating docs and mark the link as `missing`.
4. Add a deterministic gap note with:
   - entity id (`journey_id`, `document_id`, and so on)
   - requested symbol
   - search scope (`signatures` or `.signatures`)
   - next action

Do not stop execution solely because one signature link is unresolved.

## Determinism and boundary rules

- Treat link-map JSON and retrieval queries as untrusted boundary input.
- Parse into typed structures before deriving reports.
- Keep ordering stable:
  - missing findings by entity id, then kind, then signature
  - retrieval results by descending score, then stable keys
- Keep JSON deterministic (`sort_keys=true`).
- Never add timestamps or random IDs to generated linkage artifacts.
