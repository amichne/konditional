---
name: llm-native-signature-spec
description: Generate and maintain LLM-native, high-density repository documentation using a cascading referential signatures tree. Use when Codex needs to create or refresh compact signature-level docs (especially for JVM code with FQCNs), enforce deterministic spec format, reduce full-file reads, and prevent documentation drift as code evolves.
---

# LLM Native Signature Spec

## Overview
Create a repository-root `signatures/` tree that mirrors the source layout and stores compressed type-level metadata for fast relevance filtering. Use this skill to generate, validate, and refresh signature artifacts so downstream reasoning can resolve references without loading full source files unless needed.

## Workflow
1. Confirm the repository root and identify JVM-heavy source directories.
2. Read `references/signature_spec.md` to enforce output schema and invariants.
3. Run `scripts/generate_signatures.py` from repo root.
4. Inspect `signatures/INDEX.sig` and a sample of generated files for correctness.
5. Re-run generation whenever source changes; do not hand-edit generated signature artifacts.

## Generation Command
Run:

```bash
skills/llm-native-signature-spec/scripts/generate_signatures.sh --repo-root . --output-dir signatures
```

## Validation Checklist
- Ensure `signatures/` exists at repository root.
- Ensure generated paths mirror source paths.
- Ensure each `.sig` includes package metadata and type lines when types exist.
- Ensure JVM FQCNs appear in `type=` entries.
- Ensure `INDEX.sig` includes every generated signature file.
- Ensure output remains deterministic across repeated runs.

## Maintenance Rules
- Treat `signatures/` as generated artifacts.
- Regenerate after merges, refactors, or package renames.
- Keep extraction dense and minimal; avoid prose in generated files.
- Extend parser rules only when new syntax materially affects type/method discoverability.

## Resources
- `references/signature_spec.md`: canonical schema and drift controls.
- `scripts/generate_signatures.py`: deterministic generator for JVM-oriented signature extraction.
