# Safe Remote Config Loading + Rollback

Use `ParseResult` to enforce a hard boundary at the JSON parse step, and roll back on bad updates.

```kotlin
fun loadRemoteConfig() {
    val json = fetchRemoteConfig()
    val features = AppFeatures

    when (val result = ConfigurationSnapshotCodec.decode(json)) {
        is ParseResult.Success -> features.load(result.value)
        is ParseResult.Failure -> RecipeLogger.error { "Config rejected: ${result.error.message}" }
    }
}
```

If a later update causes issues:

{{recipe-6-rollback}}

- **Guarantee**: Invalid config never becomes active; swaps are atomic.
- **Mechanism**: `ParseResult` boundary + `Namespace.load(...)` atomic swap.
- **Boundary**: A valid config can still be logically wrong; rollback is the safe escape hatch.

---
