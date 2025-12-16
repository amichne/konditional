# Konditional LLM Documentation System

This directory contains domain-specific prompts for LLM-assisted development, documentation, and code review of the Konditional feature flag library.

## Quick Start

1. Identify which domain your task falls under (see Domain Map below)
2. Copy the relevant prompt from `domains/`
3. Append current context from `context/` if the prompt requests it
4. Include specific code snippets, questions, or constraints

## Domain Map

| Domain | Use When... | File |
|--------|-------------|------|
| **Public API** | Documenting user-facing DSL, writing examples, getting-started guides, API reference | [`01-public-api.md`](domains/01-public-api.md) |
| **Internal Semantics** | Explaining evaluation logic, bucketing algorithms, rule matching, specificity | [`02-internal-semantics.md`](domains/02-internal-semantics.md) |
| **Type Safety Theory** | Justifying compile-time guarantees, writing technical briefs, addressing skeptics | [`03-type-safety-theory.md`](domains/03-type-safety-theory.md) |
| **Reliability Guarantees** | Documenting thread-safety, determinism, atomicity, invariants | [`04-reliability-guarantees.md`](domains/04-reliability-guarantees.md) |
| **Configuration Integrity** | Remote config lifecycle, JSON serialization, validation, hot-reload | [`05-configuration-integrity.md`](domains/05-configuration-integrity.md) |
| **Kontracts** | JSON Schema DSL submodule documentation | [`06-kontracts.md`](domains/06-kontracts.md) |
| **Critical Evaluation** | Production-readiness assessment, complexity audit, migration due diligence | [`07-critical-evaluation.md`](domains/07-critical-evaluation.md) |

## Reading Order

For comprehensive understanding, domains are numbered by conceptual dependency:

```
01 Public API          → "How do I use this?"
       ↓
02 Internal Semantics  → "What happens when I do X?"
       ↓
03 Type Safety Theory  → "Why can I trust this?"
       ↓
04 Reliability         → "What invariants hold?"
       ↓
05 Configuration       → "How do updates work safely?"
       ↓
06 Kontracts           → "How does the schema DSL work?"

07 Critical Evaluation → "Should we bet production on this?"
   (standalone assessment prompt—does not depend on others)
```

## Directory Structure

```
.llm-docs/
├── README.md              # This file
├── CONVENTIONS.md         # Cross-cutting rules for all prompts
├── domains/               # Domain-specific prompts
│   ├── 01-public-api.md
│   ├── 02-internal-semantics.md
│   ├── 03-type-safety-theory.md
│   ├── 04-reliability-guarantees.md
│   ├── 05-configuration-integrity.md
│   └── 06-kontracts.md
├── context/               # Grounding material for prompts
│   ├── core-types.kt      # Extracted type signatures
│   └── .gitkeep
└── outputs/               # Generated artifacts
    └── .gitkeep
```

## Updating Context

When core types or APIs change, regenerate context files:

```bash
./scripts/extract-llm-context.sh
```

## Tool Integration

### Claude (claude.ai, API, Claude Code)

Reference domain prompts directly in conversation:

> "Using the prompt from `.llm-docs/domains/03-type-safety-theory.md`, write a technical brief on..."

### Cursor

Add to `.cursorrules`:

```
When documenting Konditional, read the appropriate prompt from .llm-docs/domains/ first.
Domain selection: API usage → 01, internals → 02, type safety → 03, reliability → 04, remote config → 05, kontracts → 06.
```

### GitHub Copilot / Other Tools

Include the relevant prompt as a code comment or in a separate context file that the tool can reference.

## Contributing New Prompts

1. Identify if the topic fits an existing domain or requires a new one
2. Follow the structure in `CONVENTIONS.md`
3. Include clear scope boundaries (what's in, what's out)
4. Add examples of good and bad outputs where possible
5. Update this README's domain map

## Repository Reference

- Main repository: https://github.com/amichne/konditional
- Documentation site: https://amichne.github.io/konditional/
