---
slug: /legacy/getting-started-installation
unlisted: true
---

# Installation

For the default Konditional experience (define namespaces and evaluate features), install:
- `konditional-core`
- `konditional-runtime`

Replace `VERSION` with the latest published version.

---

## Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
  implementation("io.amichne:konditional-core:VERSION")
  implementation("io.amichne:konditional-runtime:VERSION")
}
```

`konditional-runtime` provides the default `NamespaceRegistryFactory` implementation used by `Namespace(...)`.

If you intentionally run without `konditional-runtime`, you must provide your own `NamespaceRegistry` implementation
when constructing a namespace.

## Test Fixtures (Optional)

Konditional provides test helpers for common testing scenarios. Add the `testFixtures` dependency to your test
configuration:

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
  testImplementation(testFixtures("io.amichne:konditional-core:VERSION"))
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

## Verification

After syncing Gradle, verify your project resolves Konditional artifacts and compiles.

If your build succeeds, installation is complete.

---

## Next steps

- Build one typed feature: [Your First Feature](/quickstart/define-first-flag)
- Learn why typing matters at boundaries: [Type-Safety Boundaries](/theory/type-safety-boundaries)

That is enough to define and evaluate typed features in code. If you need remote configuration, JSON serialization, or
observability utilities, see:

- [Runtime](/runtime/)
- [Serialization](/serialization/)
- [Observability](/observability/)
