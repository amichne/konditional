# Doc-signature link schema

Use this schema for `docs/traceability/doc-signature-links.json` when updating
non-journey documentation.

## Purpose

Represent explicit links between documentation claims and source signatures in a
machine-checkable shape. This map keeps documentation aligned with implementation
changes and supports deterministic retrieval for pseudo-RAG workflows.

## JSON contract

```json
{
  "documents": [
    {
      "document_id": "DOC-001",
      "path": "docusaurus/docs/guides/shadow-rollout.md",
      "title": "Shadow rollout guide",
      "summary": "Ship safer changes by comparing baseline and candidate behavior.",
      "claims": [
        {
          "claim_id": "CLM-001",
          "statement": "Shadow evaluation detects baseline versus candidate mismatches.",
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
      ],
      "links": [
        {
          "kind": "field",
          "signature": "io.amichne.konditional.observability.ShadowMismatch#candidateValue: T"
        }
      ]
    }
  ]
}
```

## Field rules

- `documents`: ordered list of document link records.
- `document_id`: stable identifier for the document record.
- `path`: repository-relative markdown path.
- `title`: short document title.
- `summary`: one-sentence user/business value statement.
- `claims`: ordered list of claim-level mappings.
- `claims[].claim_id`: stable claim identifier.
- `claims[].statement`: claim text to ground.
- `claims[].links`: ordered list of signature links for the claim.
- `links`: optional document-level signature links.
- `links[].kind`: one of `type`, `method`, `field`.
- `links[].signature`:
  - `type`: fully qualified class/interface/object name.
  - `method`: `<fqcn>#<normalized method signature from .sig>`.
  - `field`: `<fqcn>#<normalized field signature from .sig>`.

## Compatibility

The validator also accepts journey-oriented payloads (`journeys` and legacy
`stories`) and generic compatibility payloads (`records`).

## Determinism constraints

- Keep document order stable by `document_id`.
- Keep claim order stable by `claim_id`.
- Keep link order stable by `kind`, then `signature`.
- Store exact signature text copied from `.sig` output.
- Do not include timestamps or ephemeral metadata.
