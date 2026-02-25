# SchemaValueCodec Object Decode + Unified Dispatch + Surface Reduction

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix `SchemaValueCodec` to decode Kotlin `object` singletons, add a symmetric `decodeKonstrained` dispatch entry point, add comprehensive object encode/decode tests, and reduce the public surface by marking `SchemaValueCodec` and `extractSchema` as `internal`.

**Architecture:** Add `kClass.objectInstance` check before the `primaryConstructor` requirement in all `decode` overloads; add `decodeKonstrained(kClass, jsonValue): Result<T>` that mirrors `encodeKonstrained`; mark `SchemaValueCodec` and `extractSchema` as Kotlin `internal` since all callers are within the same module.

**Tech Stack:** Kotlin, JUnit 5, kontracts DSL (schema + value types), `kotlin.reflect.full`

---

### Task 1: Add Kotlin `object` singleton fixture and a failing decode test

**Files:**
- Modify: `konditional-serialization/src/test/kotlin/io/amichne/konditional/fixtures/serializers/TestSerializers.kt`
- Create: `konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/KonstrainedObjectTest.kt`

**Background:**
A Kotlin `object` declaration is a singleton — `kClass.objectInstance` returns the one instance,
and `kClass.primaryConstructor` returns `null`.  The current codec returns `ParseError.InvalidSnapshot`
when asked to decode into such a class.  We prove this with a red test before fixing.

**Step 1: Add a singleton fixture**

Append to `TestSerializers.kt`:

```kotlin
/**
 * A zero-field config singleton. Useful for testing Kotlin `object` round-trips.
 */
object DefaultConfig : Konstrained.Object<ObjectSchema> {
    override val schema: ObjectSchema = schema {}
}
```

**Step 2: Write the failing test file**

Create `KonstrainedObjectTest.kt`:

```kotlin
@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.fixtures.serializers.DefaultConfig
import io.amichne.konditional.fixtures.serializers.RetryPolicy
import io.amichne.konditional.fixtures.serializers.UserSettings
import io.amichne.kontracts.dsl.jsonObject
import io.amichne.kontracts.value.JsonBoolean
import io.amichne.kontracts.value.JsonNumber
import io.amichne.kontracts.value.JsonObject
import io.amichne.kontracts.value.JsonString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for Konstrained.Object encoding/decoding:
 * - Kotlin `object` singleton round-trip (the fix target)
 * - `data class` encode → decode roundtrip via [SchemaValueCodec.decode]
 * - [SchemaValueCodec.encodeKonstrained] dispatch through ObjectTraits
 * - Default-value and optional-field behaviour during decode
 */
class KonstrainedObjectTest {

    // =========================================================================
    // Kotlin `object` singleton
    // =========================================================================

    @Test
    fun `decode returns objectInstance for Kotlin object singleton with schema`() {
        val emptyJson = jsonObject {}
        val result = SchemaValueCodec.decode(DefaultConfig::class, emptyJson, DefaultConfig.schema)
        assertTrue(result.isSuccess)
        assertSame(DefaultConfig, result.getOrThrow())
    }

    @Test
    fun `decode returns objectInstance for Kotlin object singleton without schema`() {
        val emptyJson = jsonObject {}
        val result = SchemaValueCodec.decode(DefaultConfig::class, emptyJson)
        assertTrue(result.isSuccess)
        assertSame(DefaultConfig, result.getOrThrow())
    }

    @Test
    fun `encodeKonstrained dispatches Konstrained Object singleton through ObjectTraits`() {
        val encoded = SchemaValueCodec.encodeKonstrained(DefaultConfig)
        assertTrue(encoded is JsonObject)
        assertEquals(0, (encoded as JsonObject).fields.size)
    }
}
```

**Step 3: Run tests to verify they fail**

```bash
cd /Users/amichne/code/konditional
./gradlew :konditional-serialization:test --tests "io.amichne.konditional.serialization.KonstrainedObjectTest" 2>&1 | tail -30
```

Expected: two `decode` tests fail with `ParseError.InvalidSnapshot("...must have a primary constructor...")`.
The `encodeKonstrained` test should **pass** (encoding already works).

---

### Task 2: Fix `decode` overloads for Kotlin `object` singletons

**Files:**
- Modify: `konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt`

**Background:**
`decode(kClass, json, schema)` and the private `decodeWithoutSchema` each branch to `parseFailure`
when `kClass.primaryConstructor == null`.  We add an `objectInstance` guard at the top of each.

**Step 1: Fix `decode(kClass, json, schema)` (the public schema-aware overload)**

Current code around line 281:
```kotlin
fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): Result<T> =
    kClass.primaryConstructor
        ?.let { constructor ->
            ...
        }
        ?: parseFailure(
            ParseError.InvalidSnapshot(
                "${kClass.qualifiedName} must have a primary constructor for deserialization",
            ),
        )
```

Replace the function body so that it checks `objectInstance` first:
```kotlin
fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): Result<T> {
    // Kotlin `object` singletons have no primary constructor; return the existing instance.
    kClass.objectInstance?.let { return Result.success(it) }
    return kClass.primaryConstructor
        ?.let { constructor ->
            buildSchemaParameterMap(constructor, json, schema, kClass, ::decodeValue)
                .fold(
                    onSuccess = { parameters ->
                        runCatching { constructor.callBy(parameters) }
                            .fold(
                                onSuccess = { Result.success(it) },
                                onFailure = { error ->
                                    parseFailure(
                                        ParseError.InvalidSnapshot(
                                            "Failed to instantiate ${kClass.qualifiedName}: ${error.message}",
                                        ),
                                    )
                                },
                            )
                    },
                    onFailure = { error -> Result.failure(error) },
                )
        }
        ?: parseFailure(
            ParseError.InvalidSnapshot(
                "${kClass.qualifiedName} must have a primary constructor for deserialization",
            ),
        )
}
```

**Step 2: Fix `decodeWithoutSchema` (private)**

Find `private fun <T : Any> decodeWithoutSchema(kClass: KClass<T>, json: JsonObject): Result<T>` (~line 405) and add the same guard as the first line of its body:

```kotlin
private fun <T : Any> decodeWithoutSchema(kClass: KClass<T>, json: JsonObject): Result<T> {
    kClass.objectInstance?.let { return Result.success(it) }
    return kClass.primaryConstructor
        ?.let { constructor ->
            ...  // existing body unchanged
        }
        ?: parseFailure(...)
}
```

**Step 3: Run the tests that were failing**

```bash
./gradlew :konditional-serialization:test --tests "io.amichne.konditional.serialization.KonstrainedObjectTest" 2>&1 | tail -20
```

Expected: all three tests pass.

**Step 4: Run the full module test suite to confirm no regressions**

```bash
./gradlew :konditional-serialization:test 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL.

**Step 5: Commit**

```bash
cd /Users/amichne/code/konditional
git add konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt \
        konditional-serialization/src/test/kotlin/io/amichne/konditional/fixtures/serializers/TestSerializers.kt \
        konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/KonstrainedObjectTest.kt
git commit -m "fix: decode Kotlin object singletons via objectInstance in SchemaValueCodec"
```

---

### Task 3: Add `decodeKonstrained` unified entry point (TDD)

**Files:**
- Modify: `konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/KonstrainedObjectTest.kt`
- Modify: `konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt`

**Background:**
`encodeKonstrained` dispatches on schema type and returns any `JsonValue`.
There is no symmetric `decodeKonstrained`.  Callers (FlagValue, ConfigValue, future codec consumers)
must know whether to call `decode(kClass, json, schema)` or `decodeKonstrainedPrimitive`.
We add `decodeKonstrained(kClass: KClass<T>, jsonValue: JsonValue): Result<T>` that routes correctly.

**Step 1: Write the failing tests**

Add to `KonstrainedObjectTest.kt` (new section after existing tests):

```kotlin
// =========================================================================
// decodeKonstrained — unified dispatch
// =========================================================================

@Test
fun `decodeKonstrained dispatches JsonObject to decode for data class`() {
    val json = jsonObject {
        field("maxAttempts") { number(5.0) }
        field("backoffMs") { number(500.0) }
        field("enabled") { boolean(true) }
        field("mode") { string("linear") }
    }
    val result = SchemaValueCodec.decodeKonstrained(RetryPolicy::class, json)
    assertTrue(result.isSuccess)
    assertEquals(RetryPolicy(maxAttempts = 5, backoffMs = 500.0, enabled = true, mode = "linear"), result.getOrThrow())
}

@Test
fun `decodeKonstrained dispatches JsonObject to objectInstance for singleton`() {
    val result = SchemaValueCodec.decodeKonstrained(DefaultConfig::class, jsonObject {})
    assertTrue(result.isSuccess)
    assertSame(DefaultConfig, result.getOrThrow())
}

@Test
fun `decodeKonstrained dispatches JsonString to decodeKonstrainedPrimitive for Email`() {
    val result = SchemaValueCodec.decodeKonstrained(
        io.amichne.konditional.fixtures.serializers.Email::class,
        jsonValue { string("test@example.com") },
    )
    assertTrue(result.isSuccess)
    assertEquals(io.amichne.konditional.fixtures.serializers.Email("test@example.com"), result.getOrThrow())
}

@Test
fun `decodeKonstrained dispatches JsonBoolean to decodeKonstrainedPrimitive for FeatureEnabled`() {
    val result = SchemaValueCodec.decodeKonstrained(
        io.amichne.konditional.fixtures.serializers.FeatureEnabled::class,
        jsonValue { boolean(true) },
    )
    assertTrue(result.isSuccess)
    assertEquals(io.amichne.konditional.fixtures.serializers.FeatureEnabled(true), result.getOrThrow())
}

@Test
fun `decodeKonstrained dispatches JsonNumber to decodeKonstrainedPrimitive for RetryCount (Int)`() {
    val result = SchemaValueCodec.decodeKonstrained(
        io.amichne.konditional.fixtures.serializers.RetryCount::class,
        jsonValue { number(3) },
    )
    assertTrue(result.isSuccess)
    assertEquals(io.amichne.konditional.fixtures.serializers.RetryCount(3), result.getOrThrow())
}

@Test
fun `decodeKonstrained dispatches JsonNumber to decodeKonstrainedPrimitive for Percentage (Double)`() {
    val result = SchemaValueCodec.decodeKonstrained(
        io.amichne.konditional.fixtures.serializers.Percentage::class,
        jsonValue { number(75.5) },
    )
    assertTrue(result.isSuccess)
    assertEquals(io.amichne.konditional.fixtures.serializers.Percentage(75.5), result.getOrThrow())
}
```

Also add the import at the top of the test file:
```kotlin
import io.amichne.kontracts.dsl.jsonValue
```

**Step 2: Run to verify failure**

```bash
./gradlew :konditional-serialization:test --tests "io.amichne.konditional.serialization.KonstrainedObjectTest" 2>&1 | tail -20
```

Expected: new tests fail with `Unresolved reference: decodeKonstrained`.

**Step 3: Implement `decodeKonstrained` in `SchemaValueCodec`**

Add the following after the existing `decode(kClass, json)` overload (around line 315):

```kotlin
/**
 * Unified decode entry point symmetric with [encodeKonstrained].
 *
 * Dispatches on the runtime type of [jsonValue]:
 * - [JsonObject] → [decode] (object-schema path; handles both data classes and singletons)
 * - All other [JsonValue] variants → [decodeKonstrainedPrimitive] after extracting the raw
 *   Kotlin primitive.
 *
 * For [JsonNumber] the target class is inspected to resolve Int vs Double:
 * classes implementing [Konstrained.Primitive.Int] or [Konstrained.AsInt] receive an [Int],
 * all others receive a [Double].
 */
fun <T : Any> decodeKonstrained(kClass: KClass<T>, jsonValue: JsonValue): Result<T> =
    when (jsonValue) {
        is JsonObject -> decode(kClass, jsonValue)
        is JsonNull ->
            parseFailure(ParseError.InvalidSnapshot("Cannot decode null as ${kClass.qualifiedName}"))
        else ->
            decodeKonstrainedPrimitive(kClass, jsonValue.toKotlinPrimitive(kClass))
    }

/**
 * Converts a non-object [JsonValue] to the Kotlin primitive expected by
 * [decodeKonstrainedPrimitive].
 *
 * For [JsonNumber], chooses Int when the target class is [Konstrained.Primitive.Int] or
 * [Konstrained.AsInt]; chooses Double otherwise.
 */
private fun <T : Any> JsonValue.toKotlinPrimitive(targetClass: KClass<T>): Any =
    when (this) {
        is JsonString -> value
        is JsonBoolean -> value
        is JsonNumber -> if (targetClass.isIntKonstrained()) toInt() else toDouble()
        is JsonArray ->
            elements.map { elem ->
                when (elem) {
                    is JsonString -> elem.value
                    is JsonBoolean -> elem.value
                    is JsonNumber ->
                        if (elem.toDouble() == elem.toInt().toDouble()) elem.toInt() else elem.toDouble()
                    else -> error("Unsupported array element type: ${elem::class.simpleName}")
                }
            }
        else -> error("Cannot convert ${this::class.simpleName} to Kotlin primitive for ${targetClass.qualifiedName}")
    }

private fun KClass<*>.isIntKonstrained(): Boolean =
    isSubclassOf(Konstrained.Primitive.Int::class) || isSubclassOf(Konstrained.AsInt::class)
```

Add the necessary imports if not already present:
```kotlin
import kotlin.reflect.full.isSubclassOf
```

**Step 4: Run the new tests**

```bash
./gradlew :konditional-serialization:test --tests "io.amichne.konditional.serialization.KonstrainedObjectTest" 2>&1 | tail -20
```

Expected: all tests pass.

**Step 5: Run full module suite**

```bash
./gradlew :konditional-serialization:test 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL.

**Step 6: Commit**

```bash
git add konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt \
        konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/KonstrainedObjectTest.kt
git commit -m "feat: add decodeKonstrained unified entry point symmetric with encodeKonstrained"
```

---

### Task 4: Add comprehensive object encode/decode tests

**Files:**
- Modify: `konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/KonstrainedObjectTest.kt`

**Background:**
The existing `KonstrainedIntegrationTest` exercises a basic encode/decode roundtrip for `UserSettings`.
We add the remaining invariants: field-by-field encode verification, schema default values, optional
constructor params, required-field failure, and the `FlagValue`/`ConfigValue` dispatch paths.

**Step 1: Add encode-dispatch and roundtrip tests**

Append the following test methods to `KonstrainedObjectTest`:

```kotlin
// =========================================================================
// encodeKonstrained dispatch via ObjectTraits
// =========================================================================

@Test
fun `encodeKonstrained dispatches data class through ObjectTraits path`() {
    val policy = RetryPolicy(maxAttempts = 5, backoffMs = 250.0, enabled = false, mode = "linear")
    val encoded = SchemaValueCodec.encodeKonstrained(policy)
    assertTrue(encoded is JsonObject)
    val fields = (encoded as JsonObject).fields
    assertEquals(5.0, (fields["maxAttempts"] as? JsonNumber)?.toDouble())
    assertEquals(250.0, (fields["backoffMs"] as? JsonNumber)?.toDouble())
    assertEquals(false, (fields["enabled"] as? JsonBoolean)?.value)
    assertEquals("linear", (fields["mode"] as? JsonString)?.value)
}

@Test
fun `encode then decode roundtrip preserves all field types`() {
    val original = UserSettings(theme = "dark", notificationsEnabled = false, maxRetries = 7, timeout = 60.0)
    val jsonObject = SchemaValueCodec.encode(original, original.schema)
    val result = SchemaValueCodec.decode(UserSettings::class, jsonObject, original.schema)
    assertTrue(result.isSuccess)
    assertEquals(original, result.getOrThrow())
}

// =========================================================================
// Default-value and optional-field decode behaviour
// =========================================================================

@Test
fun `decode applies schema defaultValue when field is absent from JSON`() {
    // RetryPolicy schema declares ::enabled of { default = true }
    // Omitting enabled from the JSON should fall back to the schema default.
    val json = jsonObject {
        field("maxAttempts") { number(1.0) }
        field("backoffMs") { number(100.0) }
        // "enabled" intentionally absent
        field("mode") { string("fixed") }
    }
    val result = SchemaValueCodec.decode(RetryPolicy::class, json, RetryPolicy().schema)
    assertTrue(result.isSuccess, "Expected success; got: ${result.exceptionOrNull()?.message}")
    assertEquals(true, result.getOrThrow().enabled)
}

@Test
fun `decode uses constructor default when field absent and no schema default`() {
    // UserSettings.maxRetries default in constructor is 3; no JSON field present.
    // The no-schema decode path should use the constructor default (isOptional == true).
    val json = jsonObject {
        field("theme") { string("auto") }
        field("notificationsEnabled") { boolean(true) }
        // maxRetries and timeout absent
    }
    val result = SchemaValueCodec.decode(UserSettings::class, json)
    assertTrue(result.isSuccess, "Expected success; got: ${result.exceptionOrNull()?.message}")
    val settings = result.getOrThrow()
    assertEquals(3, settings.maxRetries)
    assertEquals(30.0, settings.timeout)
}

// =========================================================================
// Required-field failure
// =========================================================================

@Test
fun `decode fails when a required field is absent from JSON`() {
    // RetryPolicy::mode has minLength=1 and is required.
    // mode absent + no default → ParseError.
    val json = jsonObject {
        field("maxAttempts") { number(3.0) }
        field("backoffMs") { number(1000.0) }
        field("enabled") { boolean(true) }
        // "mode" intentionally absent, no default in schema
    }
    val result = SchemaValueCodec.decode(RetryPolicy::class, json, RetryPolicy().schema)
    assertTrue(result.isFailure, "Expected failure for missing required field 'mode'")
}

// =========================================================================
// FlagValue round-trip for DataClassValue
// =========================================================================

@Test
fun `FlagValue from RetryPolicy produces DataClassValue`() {
    val policy = RetryPolicy(maxAttempts = 5)
    val fv = FlagValue.from(policy)
    assertInstanceOf(FlagValue.DataClassValue::class.java, fv)
    fv as FlagValue.DataClassValue
    assertEquals(RetryPolicy::class.java.name, fv.dataClassName)
    assertEquals(5, fv.value["maxAttempts"])
}

@Test
fun `FlagValue DataClassValue extractValue roundtrips RetryPolicy`() {
    val original = RetryPolicy(maxAttempts = 9, backoffMs = 2500.0, enabled = false, mode = "linear")
    val fv = FlagValue.from(original) as FlagValue.DataClassValue
    val decoded = fv.extractValue<RetryPolicy>(expectedSample = RetryPolicy())
    assertEquals(original, decoded)
}

// =========================================================================
// ConfigValue dispatch for DataClassValue
// =========================================================================

@Test
fun `ConfigValue from RetryPolicy produces DataClassValue`() {
    val policy = RetryPolicy(maxAttempts = 2)
    val cv = ConfigValue.from(policy)
    assertInstanceOf(ConfigValue.DataClassValue::class.java, cv)
    cv as ConfigValue.DataClassValue
    assertEquals(RetryPolicy::class.java.name, cv.dataClassName)
}
```

Add any missing imports at the top:
```kotlin
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.serialization.instance.ConfigValue
import org.junit.jupiter.api.Assertions.assertInstanceOf
```

**Step 2: Run the tests**

```bash
./gradlew :konditional-serialization:test --tests "io.amichne.konditional.serialization.KonstrainedObjectTest" 2>&1 | tail -30
```

Expected: all tests pass. If `decode fails when a required field is absent` fails (meaning
no schema default is configured for `mode`), verify `RetryPolicy`'s schema doesn't set a default
for `mode` and adjust the fixture or test expectation accordingly.

**Step 3: Run full suite**

```bash
./gradlew :konditional-serialization:test 2>&1 | tail -20
```

**Step 4: Commit**

```bash
git add konditional-serialization/src/test/kotlin/io/amichne/konditional/serialization/KonstrainedObjectTest.kt
git commit -m "test: comprehensive object encode/decode coverage for SchemaValueCodec"
```

---

### Task 5: Make `SchemaValueCodec` and `extractSchema` internal

**Files:**
- Modify: `konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt`
- Modify: `konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaExtraction.kt`

**Background:**
Every caller of `SchemaValueCodec` and `extractSchema` is within `konditional-serialization`.
Making them Kotlin `internal` closes accidental external use without breaking any existing code.
The `ParameterResolution` sealed interface and its `Value`/`Skip` subtypes currently appear
in public surface scanners as a side-effect of the parent being `public`; this is fixed
automatically once `SchemaValueCodec` is `internal`.

**Step 1: Mark `SchemaValueCodec` as `internal`**

In `SchemaValueCodec.kt`, change:

```kotlin
// Before
@KonditionalInternalApi
@Suppress("TooManyFunctions")
object SchemaValueCodec {

    fun <T : Any> encode(value: T, schema: ObjectSchema): JsonObject { ... }

    @KonditionalInternalApi
    fun encodeKonstrained(konstrained: Konstrained<*>): JsonValue = ...

    @KonditionalInternalApi
    @Suppress("ReturnCount")
    fun <T : Any> decodeKonstrainedPrimitive(kClass: KClass<T>, rawValue: Any): Result<T> { ... }
```

To:

```kotlin
// After
@Suppress("TooManyFunctions")
internal object SchemaValueCodec {

    fun <T : Any> encode(value: T, schema: ObjectSchema): JsonObject { ... }

    fun encodeKonstrained(konstrained: Konstrained<*>): JsonValue = ...

    @Suppress("ReturnCount")
    fun <T : Any> decodeKonstrainedPrimitive(kClass: KClass<T>, rawValue: Any): Result<T> { ... }
```

i.e.: add `internal` to the object, remove `@KonditionalInternalApi` from the object AND from
`encodeKonstrained` and `decodeKonstrainedPrimitive` (those annotations are now redundant).

Do not remove `@KonditionalInternalApi` from usages in other files — only from `SchemaValueCodec.kt`.

**Step 2: Mark `extractSchema` as `internal`**

In `SchemaExtraction.kt`, change:

```kotlin
// Before
@KonditionalInternalApi
fun extractSchema(kClass: KClass<*>): ObjectSchema? {
```

To:

```kotlin
// After
internal fun extractSchema(kClass: KClass<*>): ObjectSchema? {
```

Remove the `@KonditionalInternalApi` annotation from the function.

Also remove the import if it is now unused (check whether anything else in `SchemaExtraction.kt`
still uses `@KonditionalInternalApi`):
```kotlin
// Remove if nothing else in the file uses it:
import io.amichne.konditional.api.KonditionalInternalApi
```

**Step 3: Check for compilation errors**

```bash
./gradlew :konditional-serialization:compileKotlin :konditional-serialization:compileTestKotlin 2>&1 | tail -30
```

Expected: no errors. If callers in other files used `@OptIn(KonditionalInternalApi::class)` for
`SchemaValueCodec` specifically, they may show warnings (not errors) about now-unnecessary opt-ins.
Those are safe to leave; they may be suppressed with `@file:Suppress("OPT_IN_USAGE")` if noisy.

**Step 4: Run full test suite**

```bash
./gradlew :konditional-serialization:test 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL.

**Step 5: Run detekt**

```bash
./gradlew :konditional-serialization:detekt 2>&1 | tail -20
```

Fix any new findings. If `detekt-baseline.xml` has stale entries from old
`@KonditionalInternalApi` annotations, regenerate the baseline:
```bash
./gradlew :konditional-serialization:detektBaseline
```

**Step 6: Commit**

```bash
git add konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt \
        konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaExtraction.kt \
        konditional-serialization/detekt-baseline.xml
git commit -m "refactor: make SchemaValueCodec and extractSchema internal, remove redundant @KonditionalInternalApi"
```

---

### Task 6: Final verification

**Step 1: Build and test everything**

```bash
./gradlew build 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL with no test failures.

**Step 2: Commit design doc**

```bash
git add docs/plans/2026-02-24-schema-value-codec-object-decode.md \
        docs/plans/2026-02-24-schema-value-codec-object-decode-plan.md
git commit -m "docs: design and implementation plan for SchemaValueCodec object decode + surface reduction"
```
