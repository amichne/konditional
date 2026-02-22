# konditional-serialization

`konditional-serialization` is the untrusted-data boundary for Konditional. It
encodes snapshots and decodes payloads into trusted typed configurations.

## Read this page when

- You are loading configuration from JSON.
- You need patch application for incremental updates.
- You need explicit parse failure handling instead of exceptions.

## Concepts in scope

- **Snapshot codec**: serialize and deserialize full namespace snapshots.
- **Patch codec**: apply incremental changes to an existing snapshot.
- **Typed boundary**: decode APIs return `Result<MaterializedConfiguration>`.
- **Error typing**: boundary failures carry parse error details.

## Boundary notes

- This module parses untrusted data; runtime modules load trusted snapshots.
- A decode success means the payload has crossed into trusted model space.

## Related pages

- [Serialization reference](/serialization/reference)
- [Persistence format](/serialization/persistence-format)
- [Runtime lifecycle](/runtime/lifecycle)
- [Parse don’t validate](/theory/parse-dont-validate)

## Next steps

1. Implement decode flows with [Serialization reference](/serialization/reference).
2. Validate payload shapes with [Persistence format](/serialization/persistence-format).
3. Apply operational handling in [Configuration lifecycle](/learn/configuration-lifecycle).
