---
name: llm-native-signature-spec
description: Navigate the repository using the signatures tree as the primary and authoritative JVM symbol map. Use when Codex must minimize token use while preserving accurate, deterministic, symbol-level context across the codebase.
---

# LLM Native Signature Spec

## Overview
Use `signatures/` as the authoritative context layer for JVM symbol discovery
and repository navigation. This skill is navigation-focused.

## Hard contract (non-negotiable)
1. Accuracy is mandatory. Symbol references derived from signatures must be
   treated as ground truth until contradicted by targeted symbol-level checks.
2. Symbol fidelity is 1:1 with concrete JVM symbols that matter for code
   reasoning (`type=`, `fields`, `methods`, packages, imports, FQCNs).
3. Signature traversal is the primary navigation method across the repository.
4. Signature traversal drives subagent scoping and work decomposition.
5. Signature traversal is the only method for gaining broad codebase context.
   Anything else risks context rot and is invalid for wide-context discovery.

## Required navigation workflow
1. Start with `signatures/INDEX.sig` and build a candidate set by path/module.
2. Read only relevant `.sig` files to narrow context using symbol metadata.
3. Rank candidates deterministically:
   - exact path/name match
   - package/module proximity
   - import connectivity
   - lexicographic path tie-breaker
4. Produce a symbol-scoped working set before any source-level deep dive.
5. Allow source reads only as targeted follow-up on already selected symbols.
   Source reads must never replace signatures for broad discovery.

## Subagent traversal rules
- The parent agent must assign subagents from signature-derived scopes only.
- Each subagent assignment must include concrete symbol/file targets from `.sig`
  data, not open-ended directory exploration.
- Subagents must report findings against the same signature-derived scope before
  expanding.

## Prohibited behavior
- Source-first repository walking for context discovery.
- Directory-wide source reads to infer architecture when signatures exist.
- Symbol guesses that are not anchored in signature entries.
- Broad context claims without an explicit signature-backed trail.

## Resources
- `references/signature_spec.md`: canonical symbol schema and navigation
  semantics.
