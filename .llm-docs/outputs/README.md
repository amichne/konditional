# Outputs Directory

This directory contains artifacts generated using the domain prompts.

## Purpose

When you use a domain prompt to generate documentation, briefs, or other artifacts, save them here for reference.

## Commit Policy

**Option A: Ephemeral (gitignore)**
- Outputs are regenerated as needed
- Not version controlled
- Add `outputs/*.md` to `.gitignore`

**Option B: Canonical (committed)**
- Outputs become official documentation
- Version controlled alongside prompts
- Creates audit trail of what prompts produce

Choose based on whether generated artifacts should be authoritative or disposable.

## Naming Convention

Use descriptive names that indicate the domain and purpose:

```
type-safety-brief.md           # From 03-type-safety-theory.md
api-reference-features.md      # From 01-public-api.md
reliability-invariants.md      # From 04-reliability-guarantees.md
```
