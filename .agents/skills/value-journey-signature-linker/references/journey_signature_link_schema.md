# Journey-signature link schema

Use this schema for `docs/value-journeys/journey-signature-links.json`.

## Purpose

Represent explicit links between value journeys and source signatures in a
machine-checkable shape. The link map gives narrative claims a stable technical
anchor so storytelling stays aligned with implementation changes.

## JSON contract

```json
{
  "journeys": [
    {
      "journey_id": "JV-001",
      "title": "Ship safer rollouts with confidence",
      "value_proposition": "Release targeted changes faster while reducing incident risk.",
      "journey_stages": [
        "Discover",
        "Decide",
        "Deliver",
        "Validate"
      ],
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

- `journeys`: ordered list of value journey records.
- `journey_id`: stable identifier for a journey.
- `title`: short journey title.
- `value_proposition`: one-sentence promise of user and business value.
- `journey_stages`: ordered stage names for the narrative arc.
- `links`: ordered list of signature links.
- `links[].kind`: one of `type`, `method`, `field`.
- `links[].signature`:
  - `type`: fully-qualified class/interface/object name.
  - `method`: `<fqcn>#<normalized method signature from .sig>`.
  - `field`: `<fqcn>#<normalized field signature from .sig>`.

## Legacy compatibility

The validator also accepts legacy `stories` payloads using `story_id` and
`title`. When both `journeys` and `stories` are present, `journeys` is treated
as authoritative.

## Determinism constraints

- Keep journey order stable by `journey_id`.
- Keep link order stable by `kind`, then `signature`.
- Store exact signature text copied from `.sig` output.
- Do not include timestamps or ephemeral metadata.
