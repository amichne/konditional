# Structured Values with Schema Validation

Use `custom<T>` for structured configuration that must be validated at the JSON boundary.

```kotlin
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMs: Double = 1000.0,
    val enabled: Boolean = true,
) : Konstrained<ObjectSchema> {
    override val schema = schemaRoot {
        ::maxAttempts of { minimum = 1 }
        ::backoffMs of { minimum = 0.0 }
        ::enabled of { default = true }
    }
}

object PolicyFlags : Namespace("policy") {
    val retryPolicy by custom<RetryPolicy, Context>(default = RetryPolicy()) {
        rule(RetryPolicy(maxAttempts = 5, backoffMs = 2000.0)) { platforms(Platform.ANDROID) }
    }
}
```

- **Guarantee**: Invalid structured config is rejected before it reaches evaluation.
- **Mechanism**: Kontracts schema validation at `ConfigurationSnapshotCodec.decode(...)`.
- **Boundary**: Semantic correctness of field values (e.g., "appropriate backoff") remains a human responsibility.

---
