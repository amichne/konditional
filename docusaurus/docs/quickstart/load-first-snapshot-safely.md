# Load first snapshot safely

Load runtime snapshots through result-based APIs that keep parse behavior
explicit and inspectable [CLM-PR01-11A].

## Example

```kotlin
val load = snapshotLoader.load(json, options)
```

Treat parse and boundary failures as typed results and keep last-known-good
runtime state active when ingestion fails [CLM-PR01-11B].

## Claim citations

| Claim ID | Explicit claim | Local evidence linkage | Registry link |
|---|---|---|---|
| CLM-PR01-11A | Snapshot loading API exposes Result-based ingestion with options and codec-backed decode. | `#example` | `/reference/claims-registry#clm-pr01-11a` |
| CLM-PR01-11B | Boundary failures are modeled as ParseError values wrapped by KonditionalBoundaryFailure. | `#example` | `/reference/claims-registry#clm-pr01-11b` |
