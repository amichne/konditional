---
title: Troubleshooting
---

# Troubleshooting

Use this page when behavior in production does not match what you expect.
Each section focuses on one symptom and gives a code-accurate diagnosis path.

## Start here

Choose the section that matches your first failure signal.

- Config payload rejected: [Parsing issues](#parsing-issues)
- Same user flips rollout state: [Bucketing issues](#bucketing-issues)
- Returned value is unexpected: [Evaluation issues](#evaluation-issues)
- Runtime behavior remains stale: [Integration issues](#integration-issues)

## Evaluation issues

This section helps when `evaluate(...)` returns a value that looks wrong.

### Wrong value returned

**Symptom**: A feature evaluates to an unexpected value.

**Likely causes**:
1. Context does not match the intended rule criteria.
2. Another rule is more specific and wins first.
3. Namespace kill-switch is enabled.
4. Feature is inactive in loaded configuration.

**Fix**:
```kotlin
val controlCtx = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(2, 0, 0),
    stableId = StableId.of("control"),
)

val candidateCtx = controlCtx.copy(stableId = StableId.of("candidate"))

println("control=${AppFeatures.darkMode.evaluate(controlCtx)}")
println("candidate=${AppFeatures.darkMode.evaluate(candidateCtx)}")
```

**Verification**: Targeted test contexts produce expected values consistently.

### All features return defaults

**Symptom**: Every feature evaluates to its default value.

**Likely causes**:
1. Namespace kill-switch was enabled via `disableAll()`.
2. Loaded snapshot marks flags as inactive.
3. No rules match the current context.

**Fix**:
```kotlin
if (AppFeatures.isAllDisabled) {
    AppFeatures.enableAll()
}

val value = AppFeatures.darkMode.evaluate(ctx)
println("darkMode=$value")
```

**Verification**: `AppFeatures.isAllDisabled == false` and at least one known
matching context returns a non-default value.

## Bucketing issues

This section helps when rollout assignment appears unstable.

### Non-deterministic ramp-ups

**Symptom**: The same user appears in different rollout states over time.

**Likely causes**:
1. `stableId` is generated per request/session.
2. `salt(...)` changed unexpectedly.
3. Different code paths use different user identifiers.

**Fix**:
```kotlin
val stable = StableId.of(userId) // persistent ID from durable storage
val ctx = Context(
    locale = AppLocale.UNITED_STATES,
    platform = Platform.IOS,
    appVersion = Version.of(1, 0, 0),
    stableId = stable,
)

val results = (1..100).map { AppFeatures.darkMode.evaluate(ctx) }
check(results.all { it == results.first() })
```

**Verification**: Repeated evaluation for the same `(featureKey, stableId,
salt)` remains stable.

### Wrong percentage distribution

**Symptom**: A rollout percentage is far from expected over large samples.

**Fix**:
```kotlin
val sampleSize = 10_000
val inTreatment = (0 until sampleSize).count { i ->
    val ctx = Context(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(1, 0, 0),
        stableId = StableId.of("user-$i"),
    )
    AppFeatures.darkMode.evaluate(ctx)
}

val actualPct = (inTreatment.toDouble() / sampleSize) * 100
println("actual=$actualPct")
```

**Verification**: Distribution is close to target across a large sample.

## Parsing issues

This section helps when remote JSON fails before activation.

### JSON fails to load

**Symptom**: `NamespaceSnapshotLoader.load(...)` returns `Result.failure`.

**Fix**:
```kotlin
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader

val result = NamespaceSnapshotLoader(AppFeatures).load(json)
if (result.isFailure) {
    when (val error = result.parseErrorOrNull()) {
        is ParseError.InvalidJson -> println("Malformed JSON: ${error.reason}")
        is ParseError.FeatureNotFound -> println("Unknown feature: ${error.key}")
        is ParseError.InvalidSnapshot -> println("Schema/type mismatch: ${error.reason}")
        else -> println("Load failed: ${result.exceptionOrNull()?.message}")
    }
}
```

**Verification**: Load succeeds and evaluation reflects the new snapshot.

## Integration issues

This section helps when load succeeds but behavior seems unchanged.

### Configuration appears stale after load

**Symptom**: New payload was accepted but values look old.

**Likely causes**:
1. You loaded a different namespace than the one you evaluate.
2. Evaluation context does not actually match changed rules.
3. You expected decode-only behavior from `NamespaceSnapshotLoader`.

**Fix**:
```kotlin
val load = NamespaceSnapshotLoader(AppFeatures).load(json)
check(load.isSuccess) { "Load failed: ${load.exceptionOrNull()?.message}" }

val valueAfterLoad = AppFeatures.darkMode.evaluate(ctx)
println("valueAfterLoad=$valueAfterLoad")
```

**Verification**: Known test contexts return post-load values immediately after
successful load.

## Next steps

- [Core DSL best practices](/core/best-practices)
- [Failure modes](/production-operations/failure-modes)
- [Runtime operations](/runtime/operations)
