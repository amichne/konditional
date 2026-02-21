# Documentation RAG update template

Use this template for markdown pages that require retrieval-grounded updates.

```markdown
# <document title>

## Objective

This page answers: <user question or operational objective>.

## Scope

- Audience:
- In scope:
- Out of scope:

## Retrieval inputs

- Query set:
  - <query 1>
  - <query 2>
- Corpus:
  - signatures
  - linked docs
  - optional glob: <glob>

## Grounded guidance

1. <recommendation derived from evidence>
2. <recommendation derived from evidence>
3. <recommendation derived from evidence>

## Evidence links

- claim_id: <claim id>
  statement: <claim statement>
  links:
    - kind: type
      signature: <fqcn>
      status: linked
    - kind: method
      signature: <fqcn>#<normalized method signature>
      status: linked

## Gaps and follow-up

- <missing signature or unresolved ambiguity>
```

Keep evidence links synchronized with
`docs/claim-trace/claim-signature-links.json`.
