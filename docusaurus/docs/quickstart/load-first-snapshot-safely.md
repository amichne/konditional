# Load first snapshot safely

Load runtime snapshots through explicit result handling so invalid payloads do
not silently alter runtime behavior.

## Read this page when

- Feature declarations and local evaluation already work.
- You need to ingest external configuration safely.
- You want typed parse failures and last-known-good handling.

## Example

```kotlin
val loader = NamespaceSnapshotLoader(AppFeatures)
val result = loader.load(jsonSnapshot)

result.onSuccess { materialized ->
  AppFeatures.load(materialized)
}

result.onFailure {
  val parseError = result.parseErrorOrNull()
  logger.error("Snapshot rejected: ${parseError?.message}")
}
```

Snapshot loading uses result-based ingestion with codec-backed decode
[CLM-PR01-11A]. Boundary failures are modeled with typed parse errors wrapped
in boundary failures [CLM-PR01-11B].

## Next steps

1. Run validation checks in [Verify end-to-end](/quickstart/verify-end-to-end).
2. Review conceptual framing in [Why typed flags](/overview/why-typed-flags).
3. Plan operational rollout in [Adoption roadmap](/overview/adoption-roadmap).

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-11A | Snapshot loading API exposes Result-based ingestion with options and codec-backed decode. | `#example` | `/reference/claims-registry#clm-pr01-11a` |
| CLM-PR01-11B | Boundary failures are modeled as ParseError values wrapped by KonditionalBoundaryFailure. | `#example` | `/reference/claims-registry#clm-pr01-11b` |
