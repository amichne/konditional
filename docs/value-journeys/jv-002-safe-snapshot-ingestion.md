# JV-002: Trusted snapshot ingestion with typed failure paths

## Value proposition

For teams operating remote configuration, this journey delivers faster recovery
and clearer incident handling by reducing parse ambiguity and creating a typed,
traceable boundary from JSON payload to runtime materialization.

## Journey narrative

### Before

Snapshot ingestion failures are hard to diagnose when parse errors are generic
or detached from namespace context.

### Turning point

The team standardizes ingestion around a namespace-aware loader and a shared
codec contract, so parse and materialization results are explicit and typed.

### After

Failures are easier to triage, and successful payloads reach runtime through a
consistent, auditable flow that supports migration and shadow validation.

## Journey stages

1. Parse with context: Decode incoming JSON with namespace-aware error mapping.
2. Materialize safely: Produce typed materialized configurations from trusted
   schema-aware decoding.
3. Operate confidently: Load materialized snapshots into runtime with known
   failure semantics.

## Technical evidence (signature links)

- kind: type
  signature: io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader
  claim_supported: Namespace-scoped ingestion entry point.
  status: linked
- kind: method
  signature: io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader#override fun load( json: String, options: SnapshotLoadOptions, ): Result<MaterializedConfiguration>
  claim_supported: Typed load result with explicit boundary semantics.
  status: linked
- kind: type
  signature: io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
  claim_supported: Central codec contract for materialized snapshot handling.
  status: linked
- kind: method
  signature: io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec#override fun decode( json: String, options: SnapshotLoadOptions, ): Result<MaterializedConfiguration>
  claim_supported: Deterministic decode path with typed success or failure.
  status: linked

## Adoption signals

- Primary KPI: Mean time to root-cause parsing failures.
- Secondary KPI: Percentage of snapshots ingested without manual intervention.
- Early warning metric: Growth rate of typed parse failures by namespace.

## Migration and shadowing impact

- Baseline behavior: Current ingestion path remains active.
- Candidate behavior: Namespace loader plus shared codec handles ingestion.
- Mismatch expectations: Failure categories become more specific, not noisier.

## Open questions

- Should warning callbacks emit structured telemetry by parse-error kind?
