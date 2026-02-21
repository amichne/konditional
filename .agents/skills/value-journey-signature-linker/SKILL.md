---
name: value-journey-signature-linker
description: Retrieve and ground documentation updates with deterministic signature-linked evidence. Use when creating or revising docs that must stay synchronized with `signatures/*.sig` (or `.signatures/*.sig`), validating claim-to-signature coverage, and building pseudo-RAG evidence packs for accurate edits.
---

# Signature doc RAG linker

## Overview

Use this skill to update documentation with auditable evidence.
Retrieve deterministic context from signatures and linked markdown content,
then apply updates that stay grounded in implementation reality.

## Required artifacts

Produce these artifacts for each documentation batch:

1. Updated target documents (for example `docusaurus/docs/**/*.md`).
2. A link map at `docs/traceability/doc-signature-links.json`.
3. A validation report (for example
   `docs/traceability/doc-signature-report.json`).
4. A retrieval report (for example
   `docs/traceability/doc-evidence-query.json`).

## Workflow

1. Ensure signatures exist.
2. Build public-surface bootstrap context.
3. Define the documentation objective and evidence scope.
4. Retrieve pseudo-RAG evidence for the objective.
5. Draft or revise docs using retrieved evidence.
6. Validate signature coverage for all linked claims.
7. Record unresolved links and continue generation.

## Link-map contract

Use `references/doc_signature_link_schema.md`.

Supported top-level containers:

- `documents` (preferred)
- `records` (compatibility)

## Pseudo-RAG contract

Use `scripts/query_doc_evidence.py` as a deterministic retriever:

- corpus: signatures + linked docs + optional doc globs
- ranking: lexical overlap with stable deterministic tie-breakers
- output: machine-readable JSON evidence pack for grounding edits

This is deterministic and auditable retrieval, not embedding-based retrieval.

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

3. Validate documentation/signature links:

```bash
python3 .agents/skills/value-journey-signature-linker/scripts/validate_doc_signature_links.py \
  --repo-root . \
  --links-file docs/traceability/doc-signature-links.json \
  --report-out docs/traceability/doc-signature-report.json \
  --auto-refresh
```

4. Retrieve pseudo-RAG evidence:

```bash
python3 .agents/skills/value-journey-signature-linker/scripts/query_doc_evidence.py \
  --repo-root . \
  --links-file docs/traceability/doc-signature-links.json \
  --docs-glob 'docusaurus/docs/**/*.md' \
  --query 'shadow mismatch rollout safety evidence' \
  --top-k 12 \
  --report-out docs/traceability/doc-evidence-query.json
```

5. Use `--strict` only when you intentionally want missing links or empty
   retrieval results to fail the run (for example CI gating).

## Missing-link protocol

When a required symbol is not found:

1. Regenerate signatures and public-surface context.
2. Re-run validation.
3. If still unresolved, keep generating docs and mark the link as `missing`.
4. Add a deterministic gap note with:
   - entity id
   - requested symbol
   - search scope (`signatures` or `.signatures`)
   - next action

Do not stop execution solely because one link is unresolved.

## Determinism and boundary rules

- Treat link-map JSON and retrieval queries as untrusted boundary input.
- Parse into typed structures before deriving reports.
- Keep ordering stable:
  - missing findings by entity id, then kind, then signature
  - retrieval results by descending score, then stable keys
- Keep JSON deterministic (`sort_keys=true`).
- Never add timestamps or random IDs to generated artifacts.
