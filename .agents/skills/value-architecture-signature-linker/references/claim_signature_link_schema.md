# Claim-signature link schema

Use this schema for `docs/claim-trace/claim-signature-links.json`.

## Purpose

Represent explicit links between claim IDs and source signatures in a
machine-checkable shape. This map keeps documentation aligned with
implementation changes and provides deterministic retrieval hints.

## JSON contract

```json
{
  "documents": [
    {
      "document_id": "DOC-001",
      "path": "docusaurus/docs/how-to-guides/safe-remote-config.md",
      "title": "Safe remote config",
      "summary": "Load remote snapshots through parse-safe boundaries.",
      "claims": [
        {
          "claim_id": "CLM-TH-001",
          "statement": "Snapshot loading returns typed boundary failures on invalid payloads.",
          "links": [
            {
              "kind": "type",
              "signature": "io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader"
            },
            {
              "kind": "type",
              "signature": "io.amichne.konditional.core.result.ParseError"
            }
          ],
          "test_links": [
            {
              "module": "konditional-core",
              "path": "konditional-core/src/test/kotlin/io/amichne/konditional/core/BoundaryFailureResultTest.kt",
              "symbol": "BoundaryFailureResultTest",
              "kind": "unit"
            }
          ]
        }
      ]
    }
  ]
}
```

## Field rules

- `documents`: ordered list of document claim-link records.
- `document_id`: stable document identifier.
- `path`: repository-relative markdown path and must exist.
- `title`: short document title.
- `summary`: one-sentence objective summary.
- `claims`: ordered list of claim mappings for this document.
- `claims[].claim_id`: globally unique claim identifier.
- `claims[].statement`: explicit claim text used in citations.
- `claims[].links`: signature links backing the claim.
- `claims[].links[].kind`: one of `type`, `method`, `field`.
- `claims[].links[].signature`:
  - `type`: fully qualified class/interface/object name.
  - `method`: `<fqcn>#<normalized method signature from .sig>`.
  - `field`: `<fqcn>#<normalized field signature from .sig>`.
- `claims[].test_links` (optional): test evidence references.
- `claims[].test_links[].module`: module name.
- `claims[].test_links[].path`: repository-relative test file path.
- `claims[].test_links[].symbol` or `claims[].test_links[].test_id`: stable
  test identifier.
- `claims[].test_links[].kind`: one of
  `unit`, `integration`, `property`, `smoke`, `compatibility`, `e2e`.

## Compatibility

The validator also accepts `records` for compatibility payloads.

## Determinism constraints

- Keep document order stable by `document_id`.
- Keep claim order stable by `claim_id`.
- Keep link order stable by `kind`, then `signature`.
- Keep test link order stable by module, path, symbol/test_id.
- Store exact signature text copied from `.sig` output.
- Do not include timestamps or ephemeral metadata.
