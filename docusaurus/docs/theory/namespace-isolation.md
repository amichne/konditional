# Namespace isolation

This page is the canonical isolation model for limiting blast radius in
Konditional.

## Read this page when

- You are deciding namespace boundaries.
- You are designing team ownership of feature sets.
- You are auditing failure isolation for multi-domain systems.

## Concepts in scope

- **Per-namespace state**: each namespace owns its own lifecycle state.
- **Stable feature identity**: feature IDs are namespaced and collision-safe.
- **Scoped operations**: load, rollback, and kill-switch operate per namespace.
- **Failure isolation**: parse/load failures in one namespace do not mutate
  another namespace.

## Governance guidance

- Group features by operational ownership and rollback needs.
- Avoid one global namespace for unrelated domains.
- Avoid over-segmentation that creates needless coordination overhead.

## Related pages

- [Core best practices](/core/best-practices)
- [Runtime operations](/runtime/operations)
- [Type safety boundaries](/theory/type-safety-boundaries)
- [Migration and shadowing](/theory/migration-and-shadowing)

## Next steps

1. Define namespace ownership boundaries in your architecture docs.
2. Add isolation tests for concurrent multi-namespace updates.
3. Validate key format expectations in [Persistence format](/serialization/persistence-format).
