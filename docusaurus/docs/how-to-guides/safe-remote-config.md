# How-to: load remote configuration safely

Use this page to apply remote snapshot updates through a typed parse boundary
with atomic activation and deterministic rollback.

## Read this page when

- You are ingesting configuration from network, file, or object storage.
- You need strict parse failures without partial activation.
- You need operational controls for rollback and emergency shutdown.

## Prerequisites

- Statically declared namespace features.
- Access to remote payloads and transport retries.
- Logging and alerting for load failures.

## Deterministic steps

1. Keep schema-defining features in code.

```kotlin
object AppFeatures : Namespace("app") {
    val darkMode by boolean<Context>(default = false)
    val checkoutVariant by enum<CheckoutVariant, Context>(default = CheckoutVariant.CONTROL)
}
```

2. Fetch remote payload as untrusted input.

Treat all payloads as untrusted until parsing succeeds.

3. Parse into trusted configuration with typed errors.

```kotlin
val result = ConfigurationSnapshotCodec.decode(json, AppFeatures.compiledSchema())
```

4. Activate only on success.

```kotlin
if (result.isSuccess) {
    AppFeatures.load(result.getOrThrow())
}
```

5. Keep last-known-good on failure and emit error class.

```kotlin
if (result.isFailure) {
    when (val error = result.parseErrorOrNull()) {
        is ParseError.InvalidJson -> log("invalid_json", error.reason)
        is ParseError.FeatureNotFound -> log("feature_not_found", error.key)
        is ParseError.InvalidSnapshot -> log("invalid_snapshot", error.reason)
        else -> log("unknown", result.exceptionOrNull()?.message ?: "n/a")
    }
}
```

6. Document rollback and kill-switch usage.

- Use `rollback(steps)` for known-good reversion.
- Use `disableAll()` only for controlled emergency fallback.

## Safety checklist

- [ ] All external payloads are parsed before activation.
- [ ] Parse failures return typed errors with actionable fields.
- [ ] Readers never observe partial updates.
- [ ] Last-known-good behavior is preserved on parse failure.
- [ ] Rollback and kill-switch runbooks are documented and tested.

## Next steps

- [Handling failures](/how-to-guides/handling-failures)
- [Failure modes](/production-operations/failure-modes)
- [Thread safety](/production-operations/thread-safety)
