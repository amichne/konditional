# Story-signature link schema

Use this schema for `docs/user-stories/story-signature-links.json`.

## Purpose

Represent explicit links between user stories and source signatures in a
deterministic machine-checkable shape.

## JSON contract

```json
{
  "stories": [
    {
      "story_id": "US-001",
      "title": "Enable deterministic shadow mismatch reporting",
      "links": [
        {
          "kind": "type",
          "signature": "io.amichne.konditional.observability.ShadowMismatch"
        },
        {
          "kind": "method",
          "signature": "io.amichne.konditional.observability.ShadowEvaluator#evaluateWithShadow(context: C, candidateRegistry: NamespaceRegistry, baselineRegistry: NamespaceRegistry = namespace, options: ShadowOptions = ShadowOptions.defaults(), onMismatch: (ShadowMismatch<T>) -> Unit): T"
        }
      ]
    }
  ]
}
```

## Field rules

- `stories`: ordered list of story link records.
- `story_id`: stable identifier for a story.
- `title`: short story title.
- `links`: ordered list of signature links.
- `links[].kind`: one of `type`, `method`, `field`.
- `links[].signature`:
  - `type`: fully-qualified class/interface/object name.
  - `method`: `<fqcn>#<normalized method signature from .sig>`.
  - `field`: `<fqcn>#<normalized field signature from .sig>`.

## Determinism constraints

- Keep story order stable by `story_id`.
- Keep link order stable by `kind`, then `signature`.
- Store exact signature text copied from `.sig` output.
- Do not include timestamps or ephemeral metadata.
