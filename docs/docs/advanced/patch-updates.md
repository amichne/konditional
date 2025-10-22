---
title: Patch Updates
description: Learn how to update feature flag configurations dynamically using patches
---

# Patch Updates

Patch updates allow you to modify feature flag configurations dynamically without replacing the entire configuration. This is useful for:

- **Incremental updates**: Apply small changes without re-deploying entire configurations
- **Real-time adjustments**: Update specific flags in production
- **Efficient syncing**: Minimize bandwidth by sending only changes
- **Version control**: Track configuration changes over time

## What is a Patch?

A patch is a set of targeted changes to your flag configuration. Instead of sending a complete snapshot, you send only the differences:

```kotlin
import io.amichne.konditional.serialization.models.*

val patch = SerializablePatch(
    version = SerializableVersion("1.2.0"),
    changes = listOf(
        FlagChange.Update(
            flagId = "my-feature",
            newValue = true
        ),
        FlagChange.AddRule(
            flagId = "beta-feature",
            rule = SerializableRule(
                condition = /* ... */,
                value = true
            )
        )
    )
)
```

## Types of Changes

### Update Flag Value

Change a flag's default value:

```kotlin
FlagChange.Update(
    flagId = "max-items",
    newValue = 20
)
```

### Add Rule

Add a new rule to a flag:

```kotlin
FlagChange.AddRule(
    flagId = "premium-feature",
    rule = SerializableRule(
        condition = VersionCondition(
            range = VersionRange.from("2.0.0")
        ),
        value = true
    )
)
```

### Remove Rule

Remove a specific rule:

```kotlin
FlagChange.RemoveRule(
    flagId = "old-feature",
    ruleIndex = 0
)
```

### Toggle Flag

Enable or disable a flag:

```kotlin
FlagChange.Toggle(
    flagId = "maintenance-mode",
    enabled = true
)
```

## Applying Patches

### Using SnapshotSerializer

```kotlin
import io.amichne.konditional.serialization.SnapshotSerializer

val serializer = SnapshotSerializer()

// Get current snapshot
val currentSnapshot = serializer.serialize()

// Apply patch
val updatedSnapshot = serializer.applyPatch(currentSnapshot, patch)

// Load back into runtime
serializer.deserialize(updatedSnapshot)
```

### Patch Validation

Patches are validated before being applied:

```kotlin
try {
    serializer.applyPatch(snapshot, patch)
} catch (e: PatchValidationException) {
    println("Invalid patch: ${e.message}")
    // Handle validation error
}
```

## Best Practices

### Version Your Patches

Always include version information:

```kotlin
val patch = SerializablePatch(
    version = SerializableVersion("1.3.0"),
    previousVersion = SerializableVersion("1.2.0"),
    changes = changes
)
```

### Test Patches Locally

Before applying patches in production, test them locally:

```kotlin
// Clone current configuration
val testSnapshot = currentSnapshot.copy()

// Apply patch to test snapshot
val result = serializer.applyPatch(testSnapshot, patch)

// Validate result
assert(result.isValid())
```

### Atomic Application

Apply patches atomically to avoid partial states:

```kotlin
// Use transactions or locks
synchronized(configLock) {
    val newSnapshot = serializer.applyPatch(currentSnapshot, patch)
    updateConfiguration(newSnapshot)
}
```

### Keep Patch History

Maintain a history of patches for rollback and auditing:

```kotlin
data class PatchHistory(
    val patches: List<SerializablePatch>,
    val timestamps: List<Instant>
)

fun applyWithHistory(patch: SerializablePatch) {
    history.add(patch, Instant.now())
    serializer.applyPatch(currentSnapshot, patch)
}
```

## Example: Remote Configuration Updates

```kotlin
class RemoteConfigManager(
    private val serializer: SnapshotSerializer,
    private val httpClient: HttpClient
) {
    suspend fun checkForUpdates() {
        val currentVersion = getCurrentVersion()
        val patch = httpClient.fetchPatch(currentVersion)

        if (patch != null) {
            try {
                val snapshot = serializer.getCurrentSnapshot()
                val updated = serializer.applyPatch(snapshot, patch)
                serializer.load(updated)

                println("Applied patch: ${patch.version}")
            } catch (e: Exception) {
                println("Failed to apply patch: ${e.message}")
            }
        }
    }
}
```

## Common Use Cases

### A/B Test Updates

```kotlin
// Start A/B test
val startPatch = SerializablePatch(
    changes = listOf(
        FlagChange.AddRule(
            flagId = "experiment-feature",
            rule = SerializableRule(
                condition = UserBucketCondition(bucket = "A"),
                value = "variant-a"
            )
        )
    )
)

// Adjust test based on results
val adjustPatch = SerializablePatch(
    changes = listOf(
        FlagChange.Update(
            flagId = "experiment-feature",
            newValue = "variant-b"
        )
    )
)
```

### Emergency Disable

```kotlin
val emergencyPatch = SerializablePatch(
    version = SerializableVersion("hotfix-1"),
    changes = listOf(
        FlagChange.Toggle(
            flagId = "problematic-feature",
            enabled = false
        )
    )
)
```

## Next Steps

- Learn about [Custom Types](/advanced/custom-types/) for advanced flag values
- Explore [Migration](/advanced/migration/) strategies for evolving configurations
- Review the [Serialization API](/serialization/api/) for complete details
