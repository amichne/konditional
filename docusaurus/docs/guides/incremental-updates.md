---
title: Incremental Updates
sidebar_position: 2
---

# Incremental Updates

Apply patch payloads when you want to change a subset of flags without rebuilding full snapshots.

**Prerequisites:** You can already [load remote snapshots](/guides/remote-configuration).

## Step 1: Keep Current Configuration

Store the current `Configuration` after successful load.

## Step 2: Apply Patch JSON

```kotlin
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.snapshot.ConfigurationCodec

val patched = ConfigurationSnapshotCodec.patch(
  current = currentConfiguration,
  patchJson = patchJson,
  namespace = AppFeatures,
  options = SnapshotLoadOptions.strict(),
)
```

## Step 3: Load Patched Configuration

If patch application succeeds, load the resulting trusted configuration into the namespace runtime.

## Safety Notes

- Patch parsing is boundary-typed like snapshot parsing.
- Invalid patch payloads are rejected; do not partially apply.

## Expected Outcome

After this guide, you can ship partial configuration updates while preserving deterministic runtime behavior.

## Next Steps

- [Reference: Patch Format](/reference/patch-format)
- [Theory: Atomicity Guarantees](/theory/atomicity-guarantees)
