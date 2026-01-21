# Multi-Value AxisValues Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Allow multiple values per axis so Statsig segment membership can be represented as a set within a single axis.

**Architecture:** Promote AxisValues to store a set of values per axis ID and update matchers/accessors to use set membership. Keep rule semantics intact by matching when any context value is in the allowed ID set.

**Tech Stack:** Kotlin/JVM, Gradle, JUnit5

### Task 1: Update AxisValues data model + builder

**Files:**
- Modify: `konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValues.kt`
- Modify: `konditional-core/src/main/kotlin/io/amichne/konditional/internal/builders/AxisValuesBuilder.kt`
- Modify: `konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt`
- Test: `konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisBuilderTest.kt`

**Step 1: Write the failing test**

Update AxisBuilderTest expectations for multi-valued axes and add a new test for multi-value accumulation:

```kotlin
@Test
fun `axisValues builder accumulates multiple values for same axis`() {
    val values = axisValues {
        environment(TestEnvironment.DEV)
        environment(TestEnvironment.PROD)
        tenant(TestTenant.SME)
        tenant(TestTenant.ENTERPRISE)
    }

    Assertions.assertEquals(
        setOf(TestEnvironment.DEV, TestEnvironment.PROD),
        values[TestAxes.Environment],
    )
    Assertions.assertEquals(
        setOf(TestTenant.SME, TestTenant.ENTERPRISE),
        values[TestAxes.Tenant],
    )
}
```

Update existing tests to expect `Set<T>` (e.g., `values[TestAxes.Environment] == setOf(...)`).

**Step 2: Run test to verify it fails**

Run: `./gradlew :konditional-core:test --tests 'io.amichne.konditional.dimensions.AxisBuilderTest'`
Expected: FAIL due to type mismatch and missing multi-value support.

**Step 3: Write minimal implementation**

- Change AxisValues to store `Map<String, Set<AxisValue<*>>>` and typed getter to return `Set<T>`.
- Update AxisValuesBuilder to accumulate values per axis ID.
- Update AxisValuesScope `set` to add to the set instead of overwrite.

Core shape:

```kotlin
class AxisValues internal constructor(
    private val values: Map<String, Set<AxisValue<*>>>,
) {
    internal operator fun get(axisId: String): Set<AxisValue<*>> = values[axisId].orEmpty()

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(axis: Axis<T>): Set<T> where T : AxisValue<T>, T : Enum<T> =
        AxisRegistry.axisIdsFor(axis)
            .flatMap { values[it].orEmpty() }
            .mapNotNull { it as? T }
            .toSet()
}
```

Builder accumulator:

```kotlin
private val map: MutableMap<String, MutableSet<AxisValue<*>>> = mutableMapOf()

override fun <T> set(axis: Axis<T>, value: T) where T : AxisValue<T>, T : Enum<T> {
    require(axis.valueClass == value::class) { ... }
    map.getOrPut(axis.id) { linkedSetOf() }.add(value)
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :konditional-core:test --tests 'io.amichne.konditional.dimensions.AxisBuilderTest'`
Expected: PASS

**Step 5: Commit**

```bash
git add konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValues.kt \
        konditional-core/src/main/kotlin/io/amichne/konditional/internal/builders/AxisValuesBuilder.kt \
        konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt \
        konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisBuilderTest.kt
git commit -m "feat: allow multiple values per axis"
```

### Task 2: Update axis access utilities and Context axis lookup

**Files:**
- Modify: `konditional-core/src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt`
- Modify: `konditional-core/src/main/kotlin/io/amichne/konditional/context/Context.kt`
- Test: `konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisBuilderTest.kt`

**Step 1: Write the failing test**

Add a test that verifies `Context.axis<T>()` returns a set:

```kotlin
@Test
fun `context axis returns set of values`() {
    val ctx = TestContext(
        axisValues = axisValues {
            environment(TestEnvironment.DEV)
            environment(TestEnvironment.PROD)
        }
    )

    Assertions.assertEquals(
        setOf(TestEnvironment.DEV, TestEnvironment.PROD),
        ctx.axis<TestEnvironment>(),
    )
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :konditional-core:test --tests 'io.amichne.konditional.dimensions.AxisBuilderTest'`
Expected: FAIL due to `Context.axis` returning single value.

**Step 3: Write minimal implementation**

Update `Context.getAxisValue` to return `Set<AxisValue<*>>` and `Context.axis<T>()` to return `Set<T>`:

```kotlin
internal fun Context.getAxisValue(axisId: String): Set<AxisValue<*>> =
    AxisRegistry.axisIdsFor(axisId)
        .flatMap { axisValues[it] }
        .toSet()

inline fun <reified T> Context.axis(): Set<T> where T : AxisValue<T>, T : Enum<T> =
    AxisRegistry.axisFor(T::class)?.let { axisValues[it] }.orEmpty()
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :konditional-core:test --tests 'io.amichne.konditional.dimensions.AxisBuilderTest'`
Expected: PASS

**Step 5: Commit**

```bash
git add konditional-core/src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt \
        konditional-core/src/main/kotlin/io/amichne/konditional/context/Context.kt \
        konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisBuilderTest.kt
git commit -m "feat: return multi-value axis sets from context"
```

### Task 3: Update rule evaluation to use multi-value axis matching

**Files:**
- Modify: `konditional-core/src/main/kotlin/io/amichne/konditional/rules/evaluable/BasePredicate.kt`
- Test: `konditional-core/src/testFixtures/kotlin/io/amichne/konditional/fixtures/TestAxis.kt`
- Test: `konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisBuilderTest.kt` or a new focused test

**Step 1: Write the failing test**

Add a test that ensures axis constraints match if any context value is allowed:

```kotlin
@Test
fun `axis constraints match when any value is allowed`() {
    val ctx = TestContext(
        axisValues = axisValues {
            environment(TestEnvironment.DEV)
            environment(TestEnvironment.PROD)
        }
    )

    val enabled = FeaturesWithAxis.envScopedFlag.evaluate(ctx)
    Assertions.assertTrue(enabled)
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :konditional-core:test --tests 'io.amichne.konditional.dimensions.AxisBuilderTest'`
Expected: FAIL until predicate uses multi-value matching.

**Step 3: Write minimal implementation**

Update axis constraint matching in `BasePredicate.matches`:

```kotlin
axisConstraints.all { constraint ->
    context.getAxisValue(constraint.axisId)
        .any { it.id in constraint.allowedIds }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :konditional-core:test --tests 'io.amichne.konditional.dimensions.AxisBuilderTest'`
Expected: PASS

**Step 5: Commit**

```bash
git add konditional-core/src/main/kotlin/io/amichne/konditional/rules/evaluable/BasePredicate.kt \
        konditional-core/src/testFixtures/kotlin/io/amichne/konditional/fixtures/TestAxis.kt \
        konditional-core/src/test/kotlin/io/amichne/konditional/dimensions/AxisBuilderTest.kt
git commit -m "feat: match axis constraints against value sets"
```

### Task 4: Update fixtures and call sites to new Set-based APIs

**Files:**
- Modify: `konditional-core/src/testFixtures/kotlin/io/amichne/konditional/fixtures/TestAxis.kt`
- Modify: `konditional-core/src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt`
- Modify: any other call sites found via indexer/compile errors

**Step 1: Write the failing test**

Run a broader test sweep to surface remaining call-site breakages:

Run: `./gradlew :konditional-core:test`
Expected: FAIL with compile errors for old single-value API usage.

**Step 2: Fix call sites with minimal edits**

Update usages from `T?` to `Set<T>` semantics, e.g.:

```kotlin
val env = context.axis<TestEnvironment>()
val isProd = TestEnvironment.PROD in env
```

**Step 3: Run test to verify it passes**

Run: `./gradlew :konditional-core:test`
Expected: PASS

**Step 4: Commit**

```bash
git add konditional-core/src/testFixtures/kotlin/io/amichne/konditional/fixtures/TestAxis.kt \
        konditional-core/src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt
git commit -m "chore: update axis call sites for set semantics"
```
