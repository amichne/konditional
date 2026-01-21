---
title: Load Remote Configuration Safely
---

# Load Remote Configuration Safely

## Summary

Load feature flag configuration from JSON (remote service, file, database) with explicit validation at the trust boundary. Invalid configuration is rejected without affecting production. This takes 10-15 minutes to set up.

**When to use:** You need to update feature flags without redeploying code.

## Prerequisites

- **Konditional modules**: `konditional-runtime`, `konditional-serialization`
- **Namespace defined**: At least one `Namespace` with features
- **JSON source**: Remote API, file system, or database returning JSON

## Happy Path

### Step 1: Add Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.amichne:konditional-runtime:0.1.0")
    implementation("io.amichne:konditional-serialization:0.1.0")
}
```

**Expected output**: Dependencies resolve successfully.

### Step 2: Prepare JSON Configuration

Valid JSON snapshot format:

```json
{
  "metadata": {
    "version": "1.0.0",
    "generatedAtEpochMillis": 1704067200000,
    "source": "remote-config-service"
  },
  "flags": [
    {
      "key": "feature::app::darkMode",
      "active": true,
      "salt": "default",
      "defaultValue": { "type": "BOOLEAN", "value": false },
      "rules": [
        {
          "value": { "type": "BOOLEAN", "value": true },
          "predicates": {
            "platforms": ["IOS"]
          }
        }
      ]
    }
  ]
}
```

**Evidence**: `konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableSnapshot.kt:21`

### Step 3: Load Configuration with ParseResult Boundary

```kotlin
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
import io.amichne.konditional.runtime.load

fun loadRemoteConfig() {
    val json = fetchConfigFromRemote() // Your HTTP/file/DB fetch

    when (val result = ConfigurationSnapshotCodec.decode(json)) {
        is ParseResult.Success -> {
            AppFeatures.load(result.value)
            logger.info("Configuration loaded successfully")
        }
        is ParseResult.Failure -> {
            logger.error("Configuration rejected: ${result.error.message}")
            // Last-known-good remains active, no production impact
        }
    }
}
```

**Expected behavior**:
- Valid JSON: Configuration loads, evaluations use new rules
- Invalid JSON: Parse fails, evaluations continue with last-known-good

**Evidence**: `konditional-runtime/src/main/kotlin/io/amichne/konditional/runtime/NamespaceOperations.kt:16`

## Annotated Example

Complete example with error handling and rollback:

```kotlin
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.serialization.snapshot.ConfigurationSnapshotCodec
import io.amichne.konditional.runtime.load
import io.amichne.konditional.runtime.rollback

class ConfigurationService {
    fun refreshConfig() {
        val json = try {
            httpClient.get("https://config.example.com/features.json")
        } catch (e: Exception) {
            logger.warn("Config fetch failed, keeping current", e)
            return // Keep last-known-good
        }

        when (val result = ConfigurationSnapshotCodec.decode(json)) {
            is ParseResult.Success -> {
                AppFeatures.load(result.value)
                logger.info("Config loaded: ${result.value.metadata.version}")
            }
            is ParseResult.Failure -> {
                logger.error("Config parse failed: ${result.error.message}")
                metrics.increment("config.parse_failure")
                alertOps("Configuration rejected", result.error)
            }
        }
    }

    fun rollbackToPrevious() {
        val success = AppFeatures.rollback(steps = 1)
        if (success) {
            logger.info("Rolled back to previous configuration")
        } else {
            logger.warn("Rollback failed: insufficient history")
        }
    }
}
```

**File path**: Example at `docusaurus/docs/examples/remote-config-example.md`

## Options

| Option | Purpose | Default | When to Change |
|--------|---------|---------|----------------|
| `SnapshotLoadOptions` | Control unknown key handling | Reject unknown keys | Use `skipUnknownKeys()` for forward compatibility |
| Rollback history size | Number of configs retained | 10 (configurable) | Increase for more rollback options |
| Parse timeout | Max time for decode | None | Add for large configs or slow parsing |
| Fetch retry policy | Retry failed HTTP requests | Your HTTP client | Add exponential backoff |

## Caveats / Footguns

- **ParseResult must be handled**: Don't ignore `Failure` cases or you won't know config didn't load.
  - Fix: Always pattern-match on `ParseResult` and log/alert on `Failure`
  - Why: Silent failures cause drift between expected and actual config

- **Namespace must be initialized before JSON load**: Features must be registered first.
  - Fix: Reference namespace objects at app startup before loading config
  - Why: [Uninitialized namespace failure mode](/production-operations/failure-modes#uninitialized-namespace)

- **JSON format must match exactly**: Extra fields, wrong types, or schema violations cause parse failures.
  - Fix: Validate JSON schema in CI/CD before deployment
  - Why: [Type safety boundaries](/learn/type-safety)

- **Concurrent loads race (last-write-wins)**: Multiple threads loading config simultaneously.
  - Fix: Coordinate config loads through single source (avoid concurrent calls)
  - Why: [Thread safety model](/production-operations/thread-safety)

## Performance & Security Notes

**Performance**:
- Parsing cost: O(F × R) where F = features, R = rules per feature (typically < 1ms for 100 features)
- Loading cost: Atomic pointer swap (< 1μs)
- No GC pressure (reuses immutable structures)

**Security**:
- **Validate JSON source**: Ensure HTTPS, authentication, integrity checks
- **No code execution**: JSON cannot execute arbitrary code
- **Schema validation**: Only known feature keys accepted (unknown keys rejected by default)
- **Type safety**: Values validated against declared types before activation

## Troubleshooting

### Symptom: `ParseResult.Failure` with "Feature not found: feature::app::unknownFlag"

**Causes**:
- JSON references feature that doesn't exist in code
- Namespace not initialized before load
- Typo in feature key

**Fix**:
```kotlin
// Ensure namespace initialized
val _ = AppFeatures

// Use lenient mode for forward compatibility
val options = SnapshotLoadOptions.skipUnknownKeys()
val result = ConfigurationSnapshotCodec.decode(json, options)
```

**Verification**: Parse succeeds, unknown keys logged but skipped.

**Related**: [Troubleshooting: Parsing Issues](/troubleshooting/parsing-issues#feature-not-found)

### Symptom: `ParseResult.Failure` with "Type mismatch"

**Causes**:
- JSON specifies wrong value type (e.g., string instead of boolean)
- Schema validation failed for custom data class

**Fix**:
- Validate JSON schema in CI/CD before deployment
- Use generated JSON from `ConfigurationSnapshotCodec.encode(namespace.configuration)` as source

**Verification**: Parse succeeds with corrected JSON.

**Related**: [Production Operations: Failure Modes](/production-operations/failure-modes#type-mismatches)

### Symptom: Config loads but evaluations still return old values

**Causes**:
- Wrong namespace loaded (multiple namespaces in use)
- Cached context or evaluation results
- Load didn't complete before evaluation

**Fix**:
```kotlin
// Verify load completed
when (val result = ConfigurationSnapshotCodec.decode(json)) {
    is ParseResult.Success -> {
        AppFeatures.load(result.value)
        // Evaluation after load sees new config
        val value = AppFeatures.darkMode.evaluate(ctx)
    }
}
```

**Verification**: Call `AppFeatures.darkMode.explain(ctx)` to inspect active rule.

**Related**: [Reference: Namespace Operations](/reference/api/namespace-operations)

## Next Steps

- [Guide: Roll Out Gradually](/guides/roll-out-gradually) — Update percentages via remote config
- [Learn: Configuration Lifecycle](/learn/configuration-lifecycle) — JSON → ParseResult → load flow
- [Reference: ParseResult API](/reference/api/parse-result) — Utilities for result handling
- [Reference: Namespace Operations](/reference/api/namespace-operations) — load(), rollback() details
- [Production Operations: Failure Modes](/production-operations/failure-modes) — What can go wrong
