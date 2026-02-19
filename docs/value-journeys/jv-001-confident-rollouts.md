# JV-001: Confident rollouts without reader-side inconsistency

## Value proposition

For platform engineers and release managers, this journey delivers safer
progressive delivery by reducing rollback risk and creating confidence that
reads observe full snapshots, not partial updates.

## Journey narrative

### Before

Configuration updates feel risky during high-traffic windows because readers can
observe mixed state when updates are not linearizable.

### Turning point

The team adopts an atomic snapshot registry strategy where each update swaps a
full immutable configuration and history is tracked for targeted rollback.

### After

Rollouts become operationally safer. You can update or roll back quickly while
maintaining consistent reads and clear lifecycle control.

## Journey stages

1. Stabilize runtime state: Move namespace state handling onto the in-memory
   runtime registry.
2. Control rollout risk: Use load, disable, and rollback operations to adjust
   behavior safely.
3. Verify consistency: Confirm readers observe valid whole snapshots under
   concurrent load.

## Technical evidence (signature links)

- kind: type
  signature: io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
  claim_supported: Atomic, namespace-scoped runtime state and lifecycle control.
  status: linked
- kind: method
  signature: io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun load(config: ConfigurationView)
  claim_supported: Full snapshot replacement for update operations.
  status: linked
- kind: method
  signature: io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun rollback(steps: Int): Boolean
  claim_supported: Controlled rollback path during incident mitigation.
  status: linked
- kind: method
  signature: io.amichne.konditional.core.registry.InMemoryNamespaceRegistry#override fun disableAll()
  claim_supported: Fast kill-switch behavior when risk spikes.
  status: linked

## Adoption signals

- Primary KPI: Change failure rate for configuration-only releases.
- Secondary KPI: Mean time to safe rollback after mismatch detection.
- Early warning metric: Unexpected value divergence during shadow checks.

## Migration and shadowing impact

- Baseline behavior: Existing registry remains authoritative.
- Candidate behavior: In-memory atomic lifecycle path handles updates.
- Mismatch expectations: No value drift for equivalent snapshots.

## Open questions

- Do we want explicit dashboards for rollback frequency by namespace?
