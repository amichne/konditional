---
name: configuration
description: Handle Konditional's runtime configuration lifecycle - loading JSON, validating, applying patches, and managing parse boundaries
---

# Konditional Configuration

## Instructions

### Configuration Boundary
**Compile-time**: Feature definitions, types, rule structure (compiler-guaranteed)
**Runtime**: JSON values, targeting rules, rollout percentages (parse-time validated)

Explicit boundary handling:
```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> namespace.load(result.value)
    is ParseResult.Failure -> handleError(result.error)
}
```

### Configuration Lifecycle Pattern
```kotlin
// 1. Fetch JSON
val json = fetchRemoteConfig()

// 2. Parse with validation
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> {
        // 3. Atomically update
        Namespace.Global.load(result.value)
        logInfo("Config loaded")
    }
    is ParseResult.Failure -> {
        // 4. Last-known-good remains active
        logError("Parse failed: ${result.error.message}")
    }
}
```

### Patch Updates
```kotlin
val currentConfig = Namespace.Global.configuration
when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> logError("Patch failed: ${result.error.message}")
}
```

### Configuration Export
```kotlin
val snapshot = SnapshotSerializer.serialize(Namespace.Global.configuration)
File("config.json").writeText(snapshot)
```

### Common Parse Errors
| Error | Cause | Fix |
|-------|-------|-----|
| `FeatureNotFound` | Unregistered feature in JSON | Initialize `FeatureContainer` first |
| `InvalidJson` | Malformed JSON | Validate JSON schema |
| `InvalidSnapshot` | Missing required fields | Check JSON structure |
| `TypeMismatch` | Value type mismatch | Verify value encoding |

## Examples

### Initial Configuration Load
```kotlin
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val darkMode by boolean<Context>(default = false)
    val apiEndpoint by string<Context>(default = "https://api.example.com")
}

fun loadConfig() {
    val json = """
    {
      "flags": [{
        "key": "feature::global::darkMode",
        "defaultValue": { "type": "BOOLEAN", "value": false },
        "salt": "v1",
        "isActive": true,
        "rules": [{
          "value": { "type": "BOOLEAN", "value": true },
          "rampUp": 50.0,
          "platforms": ["IOS"]
        }]
      }]
    }
    """.trimIndent()

    when (val result = SnapshotSerializer.fromJson(json)) {
        is ParseResult.Success -> Namespace.Global.load(result.value)
        is ParseResult.Failure -> println("Failed: ${result.error}")
    }
}
```

### Incremental Patch
```kotlin
fun updateRollout() {
    val patchJson = """
    {
      "flags": [{
        "key": "feature::global::darkMode",
        "rules": [{
          "value": { "type": "BOOLEAN", "value": true },
          "rampUp": 100.0,
          "platforms": ["IOS"]
        }]
      }]
    }
    """.trimIndent()

    val currentConfig = Namespace.Global.configuration
    when (val result = SnapshotSerializer.applyPatchJson(currentConfig, patchJson)) {
        is ParseResult.Success -> Namespace.Global.load(result.value)
        is ParseResult.Failure -> println("Patch rejected: ${result.error}")
    }
}
```

### Last-Known-Good on Parse Failure
```kotlin
// Load valid config
SnapshotSerializer.fromJson(validJson).let {
    if (it is ParseResult.Success) Namespace.Global.load(it.value)
}

// Invalid update arrives
when (val result = SnapshotSerializer.fromJson(invalidJson)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> {
        // Last-known-good remains active
        logError("Invalid config rejected: ${result.error}")
    }
}

// Evaluation continues with old config
val enabled = AppFeatures.darkMode.evaluate(context)
```

### Common Mistakes

**Wrong: Parse before container initialization**
```kotlin
val result = SnapshotSerializer.fromJson(json)  // FeatureNotFound!
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) { ... }
```

**Right: Initialize containers first**
```kotlin
object AppFeatures : FeatureContainer<Namespace.Global>(Namespace.Global) { ... }
val result = SnapshotSerializer.fromJson(json)  // Success
```

**Wrong: Ignore parse failures**
```kotlin
val config = SnapshotSerializer.fromJson(json) as ParseResult.Success  // Crash!
```

**Right: Handle both cases**
```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> Namespace.Global.load(result.value)
    is ParseResult.Failure -> logError(result.error.message)
}
```
