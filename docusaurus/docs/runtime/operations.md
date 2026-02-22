# Runtime operations

This page is the runtime API reference for namespace lifecycle control.

## Read this page when

- You need exact behavior for load, rollback, and kill-switch operations.
- You are building operational automation around namespace updates.
- You are reviewing concurrency assumptions in runtime code paths.

## APIs in scope

### `Namespace.load(configuration)`

Atomically replaces the active snapshot for one namespace.

### `Namespace.configuration: ConfigurationView`

Returns the currently active snapshot view.

### `Namespace.rollback(steps: Int = 1): Boolean`

Moves back through bounded history when a previous snapshot exists.

### `Namespace.historyMetadata: List<ConfigurationMetadataView>`

Returns metadata for rollback history entries.

### `Namespace.disableAll()` / `Namespace.enableAll()`

Toggles namespace-level kill-switch behavior.

### `Namespace.setHooks(hooks: RegistryHooks)`

Registers logging/metrics hooks for runtime events.

## Operational semantics

- Readers observe complete snapshots only.
- Write operations are namespace-scoped.
- Kill-switch changes evaluation outcome, not persisted definitions.

## Related pages

- [Runtime lifecycle](/runtime/lifecycle)
- [Serialization reference](/serialization/reference)
- [Atomicity guarantees](/theory/atomicity-guarantees)
- [Namespace isolation](/theory/namespace-isolation)

## Next steps

1. Pair this API with parse boundaries in [Serialization reference](/serialization/reference).
2. Confirm failure strategy in [Configuration lifecycle](/learn/configuration-lifecycle).
3. Use shadow rollout practices from [Migration and shadowing](/theory/migration-and-shadowing).
