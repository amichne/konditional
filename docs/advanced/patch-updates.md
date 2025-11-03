---
title: Patch Updates
description: Learn how to update feature flag configurations efficiently using SnapshotPatch
---

# Patch Updates

Patch updates allow you to modify feature flag configurations incrementally without replacing the entire snapshot. This is useful for:

- **Incremental updates**: Apply small changes without re-deploying entire configurations
- **Real-time adjustments**: Update specific flags in production
- **Efficient syncing**: Minimize bandwidth by sending only changes
- **Version control**: Track configuration changes over time

## What is a SnapshotPatch?

A `SnapshotPatch` represents a set of incremental changes to apply to a `Snapshot`. Instead of sending a complete snapshot, you send only the differences:

```kotlin
import io.amichne.konditional.core.snapshot.SnapshotPatch
import io.amichne.konditional.core.snapshot.Snapshot

// Create a patch from current snapshot
val patch = SnapshotPatch.from(currentSnapshot) {
    // Add or update flags
    add(MY_FLAG to newFlagDefinition)
    add(OTHER_FLAG to anotherDefinition)

    // Remove flags
    remove(OLD_FLAG)
    remove(DEPRECATED_FLAG)
}
```

## Creating Patches

### Using the Builder DSL

The recommended way to create patches is using the builder DSL:

```kotlin
import io.amichne.konditional.builders.ConfigBuilder
import io.amichne.konditional.core.SingletonFlagRegistry

// Get current snapshot
val current = SingletonFlagRegistry.getCurrentSnapshot()

// Build patch with DSL
val patch = SnapshotPatch.from(current) {
    // Update a flag's definition
    add(Features.DARK_MODE to ConfigBuilder.buildDefinition(Features.DARK_MODE) {
        default(true)
        rule {
            platforms(Platform.ANDROID)
        } implies false
    })

    // Remove old flag
    remove(Features.OLD_FEATURE)
}
```

### Empty Patch

Create an empty patch with no changes:

```kotlin
val emptyPatch = SnapshotPatch.empty()
```

## Applying Patches

### To SingletonFlagRegistry

Apply a patch to the singleton registry:

```kotlin
import io.amichne.konditional.core.SingletonFlagRegistry

// Apply patch atomically
SingletonFlagRegistry.applyPatch(patch)

// Flags are immediately updated
```

### To Custom Registry

Apply a patch to any FlagRegistry implementation:

```kotlin
import io.amichne.konditional.core.FlagRegistry

val customRegistry: FlagRegistry = MyCustomRegistry()
customRegistry.applyPatch(patch)
```

### To a Snapshot

Apply a patch to create a new snapshot without loading it:

```kotlin
val currentSnapshot = SingletonFlagRegistry.getCurrentSnapshot()
val updatedSnapshot = patch.applyTo(currentSnapshot)

// Save to file, send over network, etc.
```

## Patch Structure

A `SnapshotPatch` contains:

```kotlin
data class SnapshotPatch(
    val flags: Map<Conditional<*, *>, ContextualFeatureFlag<*, *>>,
    val removeKeys: Set<Conditional<*, *>>
)
```

- **flags**: Map of flags to add or update
- **removeKeys**: Set of flag keys to remove

The patch application logic:
1. Removes all flags specified in `removeKeys`
2. Adds/updates all flags in the `flags` map
3. Leaves other flags unchanged

## Serialization

### Serialize a Patch

Convert a patch to JSON for storage or transmission:

```kotlin
import io.amichne.konditional.core.snapshot.SnapshotPatch.Companion.toJson
import io.amichne.konditional.serialization.SnapshotSerializer

val serializer = SnapshotSerializer.default
val patchJson = patch.toJson(serializer)

// Save to file
File("patch.json").writeText(patchJson)

// Send over network
httpClient.post("/patches", patchJson)
```

### Deserialize a Patch

Load a patch from JSON:

```kotlin
import io.amichne.konditional.core.snapshot.SnapshotPatch.Companion.fromJson

val patchJson = File("patch.json").readText()
val patch = SnapshotPatch.fromJson(patchJson, serializer)

// Apply to registry
SingletonFlagRegistry.applyPatch(patch)
```

## Best Practices

### 1. Atomic Updates

The `applyPatch` method uses atomic operations internally:

```kotlin
// ✅ Thread-safe - uses atomic compare-and-swap
SingletonFlagRegistry.applyPatch(patch)

// This is safe even with concurrent evaluations
val value = context.evaluate(MY_FLAG)
```

### 2. Version Control

Track patch history for auditing and rollback:

```kotlin
data class PatchRecord(
    val patch: SnapshotPatch,
    val timestamp: Instant,
    val author: String,
    val description: String
)

object PatchHistory {
    private val history = mutableListOf<PatchRecord>()

    fun applyAndRecord(patch: SnapshotPatch, author: String, description: String) {
        // Save snapshot before applying
        val before = SingletonFlagRegistry.getCurrentSnapshot()

        try {
            SingletonFlagRegistry.applyPatch(patch)
            history.add(PatchRecord(patch, Instant.now(), author, description))
        } catch (e: Exception) {
            // Rollback on error
            SingletonFlagRegistry.load(before)
            throw e
        }
    }
}
```

### 3. Test Patches Before Production

Always test patches in a non-production environment:

```kotlin
// Create test snapshot
val testSnapshot = productionSnapshot.copy()

// Apply patch to test
val result = patch.applyTo(testSnapshot)

// Verify the result
val testContext = createTestContext()
result.flags.forEach { (key, flag) ->
    val value = flag.evaluate(testContext)
    println("$key -> $value")
}
```

### 4. Keep Patches Small

Prefer multiple small patches over large ones:

```kotlin
// ✅ Good: Small, focused patches
val patch1 = SnapshotPatch.from(current) {
    add(Features.NEW_UI to definition)
}

val patch2 = SnapshotPatch.from(current) {
    remove(Features.OLD_UI)
}

SingletonFlagRegistry.applyPatch(patch1)
SingletonFlagRegistry.applyPatch(patch2)

// ❌ Bad: Large, unfocused patch with many changes
val megaPatch = SnapshotPatch.from(current) {
    // 50+ changes...
}
```

## Common Use Cases

### Gradual Rollout Adjustment

Adjust rollout percentages dynamically:

```kotlin
// Start with 10% rollout
config {
    Features.NEW_CHECKOUT with {
        default(false)
        rule {
            rollout = Rollout.of(10.0)
        } implies true
    }
}

// Later, increase to 50%
val patch = SnapshotPatch.from(SingletonFlagRegistry.getCurrentSnapshot()) {
    add(Features.NEW_CHECKOUT to ConfigBuilder.buildDefinition(Features.NEW_CHECKOUT) {
        default(false)
        rule {
            rollout = Rollout.of(50.0)
        } implies true
    })
}
SingletonFlagRegistry.applyPatch(patch)
```

### Emergency Kill Switch

Quickly disable a problematic feature:

```kotlin
val emergencyPatch = SnapshotPatch.from(SingletonFlagRegistry.getCurrentSnapshot()) {
    add(Features.PROBLEMATIC_FEATURE to ConfigBuilder.buildDefinition(Features.PROBLEMATIC_FEATURE) {
        default(false)  // Force disable
    })
}

SingletonFlagRegistry.applyPatch(emergencyPatch)
```

### Remote Configuration Updates

Poll for updates from a remote server:

```kotlin
class RemoteConfigSync(
    private val apiClient: ApiClient,
    private val serializer: SnapshotSerializer
) {
    suspend fun sync() {
        try {
            // Fetch latest patch from server
            val patchJson = apiClient.fetchLatestPatch()

            // Deserialize and apply
            val patch = SnapshotPatch.fromJson(patchJson, serializer)
            SingletonFlagRegistry.applyPatch(patch)

            logger.info("Successfully applied remote patch")
        } catch (e: Exception) {
            logger.error("Failed to sync remote config", e)
        }
    }
}

// Schedule periodic sync
scheduler.scheduleAtFixedRate(
    initialDelay = 0,
    period = 5,
    unit = TimeUnit.MINUTES
) {
    remoteConfigSync.sync()
}
```

### A/B Test Lifecycle

Manage A/B tests through their lifecycle:

```kotlin
// Phase 1: Start test at 50/50
val startPatch = SnapshotPatch.from(current) {
    add(Features.EXPERIMENT to ConfigBuilder.buildDefinition(Features.EXPERIMENT) {
        default(false)
        rule {
            rollout = Rollout.of(50.0)
        } implies true
    })
}

// Phase 2: Winning variant found, roll out to 100%
val expandPatch = SnapshotPatch.from(current) {
    add(Features.EXPERIMENT to ConfigBuilder.buildDefinition(Features.EXPERIMENT) {
        default(true)  // Winner becomes default
    })
}

// Phase 3: Remove experiment, make it permanent
val finalizePatch = SnapshotPatch.from(current) {
    remove(Features.EXPERIMENT)
    add(Features.NEW_FEATURE to ConfigBuilder.buildDefinition(Features.NEW_FEATURE) {
        default(true)
    })
}
```

## Integration with Serialization

Patches integrate seamlessly with the serialization module:

```kotlin
import io.amichne.konditional.serialization.SnapshotSerializer
import io.amichne.konditional.core.snapshot.SnapshotPatch.Companion.toJson
import io.amichne.konditional.core.snapshot.SnapshotPatch.Companion.fromJson

val serializer = SnapshotSerializer.default

// Serialize
val json = patch.toJson(serializer)

// Deserialize
val loaded = SnapshotPatch.fromJson(json, serializer)

// Apply
SingletonFlagRegistry.applyPatch(loaded)
```

## Thread Safety

All patch operations are thread-safe:

```kotlin
// Thread 1: Apply patch
CoroutineScope(Dispatchers.IO).launch {
    SingletonFlagRegistry.applyPatch(patch)
}

// Thread 2: Evaluate flag (safe during patch application)
CoroutineScope(Dispatchers.Main).launch {
    val value = context.evaluate(Features.MY_FLAG)
    updateUI(value)
}
```

The `SingletonFlagRegistry` uses atomic operations (`AtomicReference.updateAndGet`) to ensure:
- Patches are applied atomically
- Readers never see partial updates
- Multiple concurrent patches are handled safely

## Next Steps

- Learn about [Custom Types](/advanced/custom-types/) for advanced flag values
- Explore [Context Polymorphism](/advanced/context-polymorphism/) for custom contexts
- Review the [Serialization API](/serialization/api/) for complete details
- See [Architecture](/advanced/architecture/) for implementation details
