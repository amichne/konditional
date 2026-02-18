---
name: llm-native-signature-spec
description: Generate and maintain LLM-native, high-density repository documentation using a cascading referential signatures tree. Use when Codex needs to create or refresh compact signature-level docs (especially for JVM code with FQCNs), enforce deterministic spec format, optimize tree walking, reduce token usage, and prevent documentation drift as code evolves.
---

# LLM Native Signature Spec

## Overview
Create a repository-root `signatures/` tree that mirrors the source layout and
stores compressed type-level metadata for fast relevance filtering. Use this
skill to generate, validate, and refresh signature artifacts so downstream
reasoning can resolve references without loading full source files unless
needed.

This skill also defines a deterministic tree-walk strategy so repository context
is reconstructed from signatures first, then escalated to source reads only when
required to answer unresolved semantics.

## Workflow
1. Confirm the repository root and identify JVM-heavy source directories.
2. Read `references/signature_spec.md` to enforce output schema and invariants.
3. Run `scripts/generate_signatures.py` from repo root.
4. Inspect `signatures/INDEX.sig` and a sample of generated files for correctness.
5. Walk the signatures tree using the token-efficient traversal protocol below.
6. Re-run generation whenever source changes; do not hand-edit generated signature artifacts.

## Token-efficient traversal protocol
Use this procedure whenever you need repository understanding with minimal token
cost.

1. Start with `signatures/INDEX.sig` only, and build a candidate list by path,
   module, and naming relevance.
2. Read candidate `.sig` files before any source file. Prefer breadth-first
   narrowing (many tiny signature reads) over deep source reads.
3. Rank candidates using deterministic tie-breakers:
   - exact path/name match
   - same package/module
   - direct import references
   - lexicographic path order
4. Stop early when confidence is sufficient for the task. Document unresolved
   gaps explicitly instead of loading unrelated files.
5. Escalate to source reads only for unresolved method bodies, algorithm details,
   or runtime behavior not represented in signatures.
6. If escalating, read only targeted slices instead of whole files.

## Token guardrails
These rules prevent context over-use.

- Never start with raw source tree traversal when signatures exist.
- Never read entire source files to answer symbol-location or API-shape
  questions.
- Keep an explicit working set of relevant `.sig` files and drop irrelevant
  candidates immediately.
- Reuse already read signatures during the session; avoid re-reading identical
  artifacts.
- When uncertainty remains, report what is known from signatures and what needs
  targeted source confirmation.

## Generation Command
Run:

```bash
.agents/skills/llm-native-signature-spec/scripts/generate_signatures.sh --repo-root . --output-dir signatures
```

## Validation Checklist
- Ensure `signatures/` exists at repository root.
- Ensure generated paths mirror source paths.
- Ensure each `.sig` includes package metadata and type lines when types exist.
- Ensure JVM FQCNs appear in `type=` entries.
- Ensure `INDEX.sig` includes every generated signature file.
- Ensure output remains deterministic across repeated runs.
- Ensure `INDEX.sig` is lexicographically sorted for stable traversal.

## Maintenance Rules
- Treat `signatures/` as generated artifacts.
- Regenerate after merges, refactors, or package renames.
- Keep extraction dense and minimal; avoid prose in generated files.
- Extend parser rules only when new syntax materially affects type/method discoverability.
- Keep traversal deterministic and token-aware; optimize candidate narrowing
  before source reads.

## Resources
- `references/signature_spec.md`: canonical schema and drift controls.
- `scripts/generate_signatures.py`: deterministic generator for JVM-oriented signature extraction.
