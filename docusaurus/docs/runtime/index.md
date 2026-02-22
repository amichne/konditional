# konditional-runtime

`konditional-runtime` adds namespace lifecycle operations on top of core
definitions: atomic loading, rollback history, kill-switch control, and hooks.

## Read this page when

- You need dynamic configuration updates without redeploy.
- You need rollback and emergency controls per namespace.
- You are operating Konditional in multi-threaded services.

## Concepts in scope

- **Atomic snapshot swap** through runtime registry operations.
- **Rollback history** with bounded retained snapshots.
- **Kill-switch controls** that force default returns without deleting config.
- **Registry hooks** for logging and metrics without changing semantics.

## Boundary notes

- Runtime consumes trusted snapshots; it does not parse raw JSON.
- Parse/don’t-validate boundaries live in serialization APIs.

## Related pages

- [Runtime lifecycle](/runtime/lifecycle)
- [Runtime operations](/runtime/operations)
- [Serialization module](/serialization)
- [Atomicity guarantees](/theory/atomicity-guarantees)

## Next steps

1. Follow [Runtime lifecycle](/runtime/lifecycle) for update flow.
2. Use [Runtime operations](/runtime/operations) for API details.
3. Validate isolation expectations in [Namespace isolation](/theory/namespace-isolation).
