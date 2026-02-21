# Load first snapshot safely

Load remote configuration through a typed boundary so invalid payloads are
rejected before runtime state changes.

## What you will achieve

You will decode and ingest one JSON snapshot with explicit success and failure
handling.

## Prerequisites

Complete [Add deterministic ramp-up](/quickstart/add-deterministic-ramp-up).

## Main content

Use `NamespaceSnapshotLoader`:

```kotlin
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader

val result = NamespaceSnapshotLoader(AppFeatures).load(json)

result
    .onSuccess { materialized ->
        logger.info("Loaded config version=${materialized.configuration.metadata.version}")
    }
    .onFailure { failure ->
        val parseError = failure.parseErrorOrNull()
        logger.error(parseError?.message ?: failure.message.orEmpty())
    }
```

Semantics:

- on success, the namespace snapshot updates atomically;
- on failure, the active snapshot remains unchanged.

## Verify

Test one valid and one invalid payload:

1. Load a valid payload and confirm success.
2. Load an invalid payload and confirm:
   - `Result.failure` is returned;
   - parse error details are available;
   - prior behavior remains active.

## Common issues

- swallowing `Result.failure` without alerting;
- assuming JSON decode success means business behavior is correct;
- loading the wrong namespace for a given payload.

## Next steps

- [Verify end-to-end](/quickstart/verify-end-to-end)
- [Snapshot loader API](/reference/api/snapshot-loader)
- [Boundary result API](/reference/api/parse-result)
