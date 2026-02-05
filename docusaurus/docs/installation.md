# Installation

Konditional Core is a single dependency.

Replace `VERSION` with the latest published version.

## Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
  implementation("io.github.amichne:konditional-core:VERSION")
}
```

## Test Fixtures (Optional)

Konditional provides test helpers for common testing scenarios. Add the `testFixtures` dependency to your test
configuration:

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
  testImplementation(testFixtures("io.github.amichne:konditional-core:VERSION"))
}
```

**Available test helpers:**

- `CommonTestFeatures` — Pre-configured feature flags for common testing scenarios
- `EnterpriseTestFeatures` — Enterprise-tier feature flags for advanced testing
- `TestAxis` — Axis definitions for testing multi-dimensional targeting
- `TestNamespace` — Namespace implementations for testing
- `TestStableId` — StableId utilities for deterministic test buckets
- `TargetingIds` — Pre-computed IDs for specific bucket targeting
- `FeatureMutators` — Utilities for modifying feature configurations in tests

See [How-To: Test Your Feature Flags](/how-to-guides/testing-features) for usage examples.

---

That is enough to define features and evaluate them in code. If you need remote configuration, JSON serialization, or
observability utilities, see the module docs:

[//]: # (- [Runtime]&#40;/runtime/&#41;)
- [Serialization](/serialization/)
- [Observability](/observability/)
