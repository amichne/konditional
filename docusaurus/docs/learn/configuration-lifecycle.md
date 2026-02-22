# Configuration Lifecycle

This page describes how remote payloads move from untrusted JSON to trusted
runtime snapshots.

## Five-step lifecycle

### 1. Payload retrieval

Configuration is fetched from file, remote service, or stream.

### 2. Boundary validation and materialization

Use `NamespaceSnapshotLoader(namespace).load(json)` to validate and load in a
single scoped operation.

### 3. Atomic activation on success

Successful load publishes a complete snapshot transition.

### 4. Rejection on failure

Boundary failure returns typed parse diagnostics and preserves last-known-good
state.

### 5. Evaluation against active snapshot

Feature evaluation reads currently active runtime snapshot state.

## Optional operational patterns

- Polling or push-based refresh loops.
- Patch-based updates with schema-aware codec operations.
- Rollback using bounded history metadata.

## Drift guardrails

- Do not rely on a `fromJson(...)` lifecycle API name for runtime ingestion.
- Use schema-scoped decode/materialization semantics for manual codec flows.
- Treat materialized boundary types as the trusted handoff to runtime load.

## Related

- [Parse don’t validate](/theory/parse-dont-validate)
- [Atomicity guarantees](/theory/atomicity-guarantees)
- [Runtime lifecycle](/runtime/lifecycle)

## Claim ledger

| claim_id | claim_statement | claim_kind | status |
| --- | --- | --- | --- |
| LRN-004-C1 | Configuration lifecycle starts with explicit loader validation before activation. | boundary | supported |
| LRN-004-C2 | Failure paths expose typed parse diagnostics without mutating active state. | failure_mode | supported |
| LRN-004-C3 | Runtime load and rollback operations provide operational recovery over bounded snapshot history. | mechanism | supported |
| LRN-004-C4 | Patch and decode operations preserve schema-scoped materialization requirements. | boundary | supported |
