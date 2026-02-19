# Atomicity Guarantees

Konditional runtime state updates are snapshot-based. Readers observe coherent
snapshots, not partially applied updates.

## Atomicity claim

Load, rollback, and history progression are linearizable at the registry
boundary.

## Mechanism

The default runtime registry maintains current state and bounded history with
atomic references, while write paths serialize snapshot transitions so readers
see complete states.

## Reader model

`Feature.evaluate(...)` consumes active snapshot state. It does not compose
partial objects from concurrent writes.

## Writer model

- `load(...)` publishes a full snapshot transition.
- `rollback(...)` restores a complete historical snapshot.
- `disableAll()/enableAll()` switch kill behavior coherently for the namespace.

## Boundaries and non-goals

- Atomicity does not make malformed payloads valid. Boundary validation still
  applies before load.
- External mutation of trusted snapshot objects is outside supported semantics.

## Related

- [Configuration lifecycle](/learn/configuration-lifecycle)
- [Namespace operations API](/reference/api/namespace-operations)
- [Determinism proofs](/theory/determinism-proofs)

## Claim ledger

| claim_id | claim_statement | claim_kind | status |
| --- | --- | --- | --- |
| TH-004-C1 | Runtime registry loads publish coherent snapshots that readers consume without partial visibility. | guarantee | supported |
| TH-004-C2 | Rollback operations preserve linearizable progression under read contention. | guarantee | supported |
| TH-004-C3 | Kill-switch toggles keep evaluation semantics coherent while enforcing declared defaults. | guarantee | supported |
| TH-004-C4 | History storage is bounded and only records complete snapshots. | performance | supported |
| TH-004-C5 | Read paths remain lock-free for active snapshot reads. | performance | supported |
| TH-004-C6 | Atomic load boundaries prevent mixed old/new snapshot views. | guarantee | supported |
