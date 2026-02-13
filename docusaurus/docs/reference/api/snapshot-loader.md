---
title: NamespaceSnapshotLoader API
---

# NamespaceSnapshotLoader API

`NamespaceSnapshotLoader` is a namespace-scoped, side-effecting loader: it decodes JSON into a typed `Configuration` and, on success, loads that configuration into the target namespace runtime registry.

In this page you will find:

- Exact constructor and `load` signatures
- Success/failure semantics and namespace error context
- When to use this vs manual decode + load

---

## Type Signature

```kotlin
class NamespaceSnapshotLoader<M : Namespace>(
    private val namespace: M,
    private val codec: SnapshotCodec<Configuration> = ConfigurationSnapshotCodec,
) : SnapshotLoader<Configuration>
```

Evidence:

- `konditional-runtime/src/main/kotlin/io/amichne/konditional/serialization/snapshot/NamespaceSnapshotLoader.kt`
- `konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/snapshot/SnapshotLoader.kt`

---

## load(json, options)

```kotlin
override fun load(
    json: String,
    options: SnapshotLoadOptions = SnapshotLoadOptions.strict(),
): ParseResult<Configuration>
```

### Return value

- `ParseResult.Success(configuration)`: decode succeeded and the namespace registry was updated.
- `ParseResult.Failure(error)`: decode failed and registry state is unchanged.

### Semantics

- Decode path is namespace-scoped when the codec supports `FeatureAwareSnapshotCodec`.
- On success, loader performs `namespace.runtimeRegistry().load(config)`.
- For `InvalidJson` and `InvalidSnapshot`, errors are prefixed with namespace context.

---

## Unknown Feature Handling

`load` forwards `SnapshotLoadOptions` to decoding:

- `SnapshotLoadOptions.strict()` (default): unknown feature IDs fail the load.
- `SnapshotLoadOptions.skipUnknownKeys(...)`: unknown IDs are skipped and surfaced via warnings.

```kotlin
val result = NamespaceSnapshotLoader(AppFeatures).load(
    json = json,
    options = SnapshotLoadOptions.skipUnknownKeys { warning ->
        logger.warn { warning.message }
    },
)
```

---

## Runtime Preconditions and Gotchas

- `NamespaceSnapshotLoader` requires a runtime-capable registry.
- If `:konditional-runtime` is not on the classpath, `namespace.runtimeRegistry()` fails fast.
- Loader only guarantees parse/load behavior; it does not add retry, fetch, or rollout orchestration.

---

## Usage Example

```kotlin
val loader = NamespaceSnapshotLoader(AppFeatures)

when (val result = loader.load(fetchRemoteConfig())) {
    is ParseResult.Success -> {
        logger.info { "Loaded version=${result.value.metadata.version}" }
    }
    is ParseResult.Failure -> {
        logger.error { "Rejected config: ${result.error.message}" }
        // Last-known-good remains active.
    }
}
```

---

## Compare with Manual Flow

Use `NamespaceSnapshotLoader` when you want parse+load as one operation:

```kotlin
loader.load(json)
```

Use manual flow when you need to inspect/transform the `Configuration` before loading:

```kotlin
when (val parsed = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> {
        val patched = parsed.value.withMetadata(source = "manual")
        AppFeatures.load(patched)
    }
    is ParseResult.Failure -> Unit
}
```

---

## Related

- [Namespace Operations API](/reference/api/namespace-operations)
- [ParseResult API](/reference/api/parse-result)
- [Serialization API Reference](/serialization/reference)
- [Runtime Lifecycle](/runtime/lifecycle)
