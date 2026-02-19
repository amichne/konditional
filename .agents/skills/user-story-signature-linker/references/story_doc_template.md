# User story doc template

Use this template for files in `docs/user-stories/`.

```markdown
# <story id>: <title>

## User story

As a <role>, I want <capability>, so that <outcome>.

## Context

- Domain:
- Boundary inputs:
- Determinism assumptions:
- Namespace scope:

## Acceptance criteria

1. <criterion>
2. <criterion>
3. <criterion>

## Signature links

- kind: type
  signature: <fqcn>
  status: linked
- kind: method
  signature: <fqcn>#<method signature>
  status: linked

## Migration and shadowing impact

- Baseline behavior:
- Candidate behavior:
- Mismatch expectations:

## Open questions

- <question or unresolved signature gap>
```

Keep "Signature links" synchronized with
`docs/user-stories/story-signature-links.json`.
