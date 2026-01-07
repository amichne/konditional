# Controlled Migrations with Shadow Evaluation

Compare a candidate configuration to baseline behavior without changing production outputs.

```kotlin
fun evaluateWithShadowedConfig(context: Context): Boolean {
    val candidateJson = fetchCandidateConfig()
    val candidateConfig = ConfigurationSnapshotCodec.decode(candidateJson).getOrThrow()
    val candidateRegistry =
        InMemoryNamespaceRegistry(namespaceId = AppFeatures.namespaceId).apply {
            load(candidateConfig)
        }

    val value =
        AppFeatures.darkMode.evaluateWithShadow(
            context = context,
            candidateRegistry = candidateRegistry,
            onMismatch = { mismatch ->
                RecipeLogger.warn {
                    "shadowMismatch key=${mismatch.featureKey} kinds=${mismatch.kinds} baseline=${mismatch.baseline.value} candidate=${mismatch.candidate.value}"
                }
            },
        )

    return applyDarkMode(value)
}
```

- **Guarantee**: Production behavior stays pinned to baseline while candidate is evaluated.
- **Mechanism**: `evaluateWithShadow(...)` evaluates baseline + candidate but returns baseline value.
- **Boundary**: Shadow evaluation is inline and adds extra work to the hot path; sample if needed.

---
