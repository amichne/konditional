# Parse Don’t Validate

Konditional treats all external payloads as untrusted input. The boundary
contract is to parse JSON into a trusted, typed model before any runtime state
mutation occurs.

## Claim: boundary parsing protects runtime semantics

Remote payloads do not enter evaluation directly. A payload must decode into a
`MaterializedConfiguration` first, or it is rejected.

## Mechanism

1. Schema plane: `CompiledNamespaceSchema` derived from compile-time feature
   declarations.
2. Data plane: incoming JSON snapshot or patch payload.
3. Materialization plane: typed decode to
   `Result<MaterializedConfiguration>`.
4. Runtime activation plane: `NamespaceSnapshotLoader(...).load(...)` atomically
   swaps active state only on success.

## Evidence

- `NamespaceSnapshotLoader` is the namespace-scoped boundary API.
- `ConfigurationSnapshotCodec` requires schema-aware decode for trusted
  materialization.
- Boundary failures are represented by `ParseError` and
  `KonditionalBoundaryFailure`.

## Boundary policies

`SnapshotLoadOptions` controls strictness for edge behavior at ingest time.

- `unknownFeatureKeyStrategy` controls unknown keys.
- `missingDeclaredFlagStrategy` controls absent declared flags.
- `onWarning` is an explicit, caller-supplied warning hook.

## Failure semantics

A failed decode returns `Result.failure(...)` and leaves active runtime state
unchanged. This preserves last-known-good behavior under malformed,
out-of-scope, or incompatible payloads.

## Related

- [Type safety boundaries](/theory/type-safety-boundaries)
- [Configuration lifecycle](/learn/configuration-lifecycle)
- [Serialization reference](/serialization/reference)

## Claim ledger

| claim_id | claim_statement | claim_kind | status |
| --- | --- | --- | --- |
| TH-001-C1 | The parse boundary decodes untrusted JSON into typed materialized snapshots before runtime load. | boundary | supported |
| TH-001-C2 | Boundary failures are represented as typed parse errors instead of untyped exceptions. | failure_mode | supported |
| TH-001-C3 | Namespace loaders attach namespace context to parse failures for safer operations triage. | mechanism | supported |
| TH-001-C4 | Runtime load accepts materialized configurations and rejects boundary-invalid payloads. | boundary | supported |
| TH-001-C5 | Missing declared flag handling is explicit policy configured through snapshot load options. | mechanism | supported |
| TH-001-C6 | Schema-less decode is intentionally unsupported to prevent unscoped snapshot ingestion. | boundary | supported |
