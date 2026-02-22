# Documentation update template (claim-citation mode)

Use this template for markdown pages that require deterministic claim citations.

```markdown
# <document title>

## Purpose

This page helps <audience> achieve <outcome> by reducing <risk/pain>.

## Main content

Write concise guidance and include inline claim tokens for non-trivial claims,
for example: `... deterministic for stable IDs [CLM-TH-003].`

## Evidence notes

- Keep heavy evidence details in the claim registry.
- Keep inline prose focused on user decisions and implementation actions.

## Claim citation footer

| Claim ID | Explicit Claim | Local Evidence Linkage | Registry Link |
|---|---|---|---|
| CLM-TH-003 | <explicit statement> | `#section-anchor` | `/reference/claims-registry#clm-th-003` |
```

Keep claim citations synchronized with:

- `docs/claim-trace/claim-signature-links.json`
- `docs/claim-trace/claims-registry.json`
