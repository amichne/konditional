---
name: public-surface-init-context
description: Build a deterministic, LLM-optimized initialization context that captures only the repository public surface from llm-native-signature-spec artifacts (`signatures/*.sig`). Use when starting a new session, preparing downstream agents before deep signature/source investigation, or refreshing compact public API context after interface changes.
---

# Public Surface Init Context

## Overview

Generate a compact context artifact that contains only public API signals. Use
this artifact as the first read in new sessions, before opening
`INDEX.sig` or individual `.sig` files.

## Workflow

1. Ensure signatures exist by running:

```bash
.agents/skills/llm-native-signature-spec/scripts/generate_signatures.sh --repo-root . --output-dir signatures
```

2. Build the public-surface context:

```bash
.agents/skills/public-surface-init-context/scripts/build_public_surface_context.py --repo-root . --signatures-dir signatures --output signatures/PUBLIC_SURFACE.ctx
```

3. Start session understanding from `signatures/PUBLIC_SURFACE.ctx`.
4. Escalate to `signatures/INDEX.sig` and targeted `.sig` files only when
   details are still unresolved.

## Public-surface constraints

- Include only declarations inferred as public.
- Exclude private, protected, and internal declarations.
- Treat Kotlin declarations without explicit visibility as public.
- Treat Java/Scala declarations without explicit `public` as non-public.
- Include only primary source sets by default (`src/main`, `commonMain`,
  `jvmMain`, `jsMain`, `nativeMain`).
- Exclude internal package/location markers (`.internal.` and `/internal/`).
- Keep output deterministic by using stable sorting and no timestamps.

Use `--include-non-primary-sources` only when you explicitly want context from
tests or auxiliary source sets.

## Output shape

The generated file is a dense key-value context document with:

- repository metadata and scope marker
- module-level public type counts
- lexicographically ordered public type entries
- file-level public member summaries

See `references/public_context_schema.md` for the exact schema and filtering
rules.
