# Journey-signature link schema

Use this schema for `docs/value-journeys/journey-signature-links.json`.

## Purpose

Represent explicit links between journey narratives and source signatures in a
machine-checkable shape.

This file lets warm, persuasive prose stay lightweight while preserving hard
traceability in structured artifacts.

Use this schema together with `docs/value-journeys/journey-claims.json`:

- `journey-signature-links.json`: technical linkage and doc anchors.
- `journey-claims.json`: claim statements, tests, status, and ownership.

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
          "signature": "io.amichne.konditional.observability.ShadowMismatch",
          "claim_refs": [
            "JV-001-C1"
          ],
          "citation_refs": [
            "TH-003-C1"
          ]
        },
        {
          "kind": "method",
          "signature": "io.amichne.konditional.observability.ShadowEvaluator#evaluateWithShadow(context: C, candidateRegistry: NamespaceRegistry, baselineRegistry: NamespaceRegistry = namespace, options: ShadowOptions = ShadowOptions.defaults(), onMismatch: (ShadowMismatch<T>) -> Unit): T",
          "claim_refs": [
            "JV-001-C1"
          ],
          "doc_refs": [
            {
              "page": "value-journeys/jv-001-confident-rollouts",
              "anchor": "proof-point-determinism",
              "role": "citation"
            },
            {
              "page": "value-journeys/jv-001-confident-rollouts-technical",
              "anchor": "code-jv-001-c1",
              "role": "code_example"
            }
          ]
        }
      ]
    }
  ]
}
```

## Field rules

- `journeys`: ordered list of journey records.
- `journey_id`: stable identifier for a journey.
- `title`: short journey title.
- `value_proposition`: one-sentence value promise.
- `journey_stages`: ordered stage names for the narrative arc.
- `links`: ordered list of signature links.
- `links[].kind`: one of `type`, `method`, `field`.
- `links[].signature`:
  - `type`: fully-qualified class/interface/object name.
  - `method`: `<fqcn>#<normalized method signature from .sig>`.
  - `field`: `<fqcn>#<normalized field signature from .sig>`.
- `links[].claim_refs` (optional): claim IDs supported by this signature.
- `links[].citation_refs` (optional): citation tokens used in prose sections.
- `links[].doc_refs` (optional): page anchors connecting evidence to markdown.
- `links[].doc_refs[].role` (optional): use `citation`, `code_example`,
  `claim_detail`, or `justification`.

`citation_refs` and `doc_refs` are optional metadata for narrative workflows.
They do not replace the required claim registry.

## Legacy compatibility

The validator accepts legacy `stories` payloads using `story_id` and `title`.
When both `journeys` and `stories` are present, `journeys` is authoritative.

## Determinism constraints

- Keep journey order stable by `journey_id`.
- Keep link order stable by `kind`, then `signature`.
- Keep `claim_refs` sorted lexicographically.
- Keep `citation_refs` sorted lexicographically.
- Keep `doc_refs` sorted by `page`, then `anchor`, then `role`.
- Store exact signature text copied from `.sig` output.
- Do not include timestamps or ephemeral metadata.
