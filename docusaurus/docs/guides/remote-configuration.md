---
title: Remote Configuration
sidebar_position: 1
---

# Remote Configuration

Set up a safe fetch-parse-load loop for namespace configuration updates.

**Prerequisites:** You understand [Parse Boundary](/concepts/parse-boundary) and have a declared namespace.

## Step 1: Fetch Candidate JSON

Retrieve config from your storage or delivery endpoint. Keep transport retries and authentication outside Konditional.

## Step 2: Parse and Load Through Namespace Snapshot Loader

```kotlin
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader

val loader = NamespaceSnapshotLoader(AppFeatures)
val options = SnapshotLoadOptions.strict()

val result = loader.load(remoteJson, options)
result.onFailure { failure ->
  val parseError = failure.parseErrorOrNull()
  logger.error("config load failed: ${parseError?.message ?: failure.message}")
}
```

## Step 3: Keep Last-Known-Good Active

On failed load, do not clear namespace state. Continue evaluating against the previous successful snapshot.

## Expected Outcome

After this guide, your service can ingest remote config updates while rejecting malformed payloads without runtime drift.

## Next Steps

- [Reference: Snapshot Load Options](/reference/snapshot-load-options)
- [Guide: Incremental Updates](/guides/incremental-updates)
