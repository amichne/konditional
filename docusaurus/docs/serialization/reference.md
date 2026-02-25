# Serialization API Reference

JSON snapshot and patch APIs at the untrusted boundary.

## `ConfigurationSnapshotCodec.encode(...)`

```kotlin
object ConfigurationSnapshotCodec {
    fun encode(value: ConfigurationView): String
}
```

## `ConfigurationSnapshotCodec.decode(...)`

```kotlin
object ConfigurationSnapshotCodec {
    fun decode(
        json: String,
        schema: CompiledNamespaceSchema,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<Configuration>
}
```

Notes:

- Decode is schema-required and deterministic.
- Successful decode returns trusted `Configuration`.
- Failures are `Result.failure(KonditionalBoundaryFailure(parseError))`.

## `ConfigurationSnapshotCodec.patch(...)`

```kotlin
object ConfigurationSnapshotCodec {
    fun patch(
        current: Configuration,
        patchJson: String,
        namespace: Namespace,
        options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
    ): Result<Configuration>
}
```

## `SnapshotLoadOptions`

```kotlin
data class SnapshotLoadOptions(
    val unknownFeatureKeyStrategy: UnknownFeatureKeyStrategy = UnknownFeatureKeyStrategy.Fail,
    val missingDeclaredFlagStrategy: MissingDeclaredFlagStrategy = MissingDeclaredFlagStrategy.Reject,
    val onWarning: (SnapshotWarning) -> Unit = {},
)
```

Factory modes:

- `SnapshotLoadOptions.strict()`
- `SnapshotLoadOptions.skipUnknownKeys(...)`
- `SnapshotLoadOptions.fillMissingDeclaredFlags(...)`

`MissingDeclaredFlagStrategy`:

- `Reject` (default)
- `FillFromDeclaredDefaults`

## Error Introspection

```kotlin
val result = ConfigurationSnapshotCodec.decode(json, schema)
val parseError: ParseError? = result.parseErrorOrNull()
```

## Structured value decode semantics

When snapshot payloads include custom `Konstrained` values, decode dispatch is
deterministic by JSON shape and target type.

- `JsonObject` values decode through object-schema mapping.
- Kotlin `object` singletons decode to their existing `objectInstance` (no
  primary constructor is required).
- `JsonString`, `JsonBoolean`, `JsonNumber`, and `JsonArray` values decode
  through primitive/array reconstruction.
- For `JsonNumber`, Int-backed custom types receive `Int`; other numeric custom
  types receive `Double`.
- Missing required object fields fail with typed parse errors; schema defaults
  and Kotlin constructor defaults are applied before failure.

## Related

- [NamespaceSnapshotLoader API](/reference/api/snapshot-loader)
- [Boundary Result API](/reference/api/parse-result)
- [Parse Don’t Validate Theory](/theory/parse-dont-validate)
