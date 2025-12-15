# Context Directory

This directory contains extracted context for LLM prompts.

## Files

- `core-types.kt` - Extracted type signatures from source code
- `public-api-surface.md` - Concatenated API documentation
- `.last-updated` - Timestamp of last extraction

## Regenerating

Run the extraction script after significant API changes:

```bash
./scripts/extract-llm-context.sh
```

## Usage

When a domain prompt includes a context injection point like:

```
[INSERT: Core type signatures from context/core-types.kt if needed]
```

Copy the relevant portions from these files into your prompt.
