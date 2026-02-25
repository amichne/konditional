# Design: SchemaValueCodec — Object Singleton Decode + Unified Dispatch + Surface Reduction

**Date:** 2026-02-24
**Scope:** `konditional-serialization`
**Status:** Approved

---

## Problem

### 1. Kotlin `object` singletons cannot be decoded

`SchemaValueCodec.decode()` (line ~282) and `decodeKonstrainedPrimitive()` (line ~153) both
branch to `parseFailure` when `kClass.primaryConstructor == null`.
Kotlin `object` declarations have no primary constructor accessible via reflection —
`kClass.objectInstance` is the correct recovery path, but it is never checked.

Any `Konstrained.Object` implemented as a Kotlin `object` singleton (e.g. a zero-field config
placeholder) round-trips encoding fine but returns `ParseError.InvalidSnapshot` on decode.

### 2. Asymmetric codec surface

`encodeKonstrained` provides a unified entry point that dispatches on schema type.
The decode side has two separate methods — `decode(kClass, json, schema)` for objects and
`decodeKonstrainedPrimitive(kClass, rawValue)` for primitives — and callers must choose.
This asymmetry makes the codec harder to use correctly and harder to test exhaustively.

### 3. Leaking internal surface

`SchemaValueCodec` and `extractSchema` are annotated `@KonditionalInternalApi` (opt-in
barrier) but remain Kotlin `public`.  Every caller is within the same
`konditional-serialization` module, so Kotlin `internal` is both sufficient and correct.
The private `ParameterResolution` sealed hierarchy currently leaks into public surface scanners
as a side-effect of the parent object being public.

---

## Chosen Approach: Option B

1. **`objectInstance` fix** — surgical: check `kClass.objectInstance` before requiring a
   primary constructor in all `decode` overloads.
2. **`decodeKonstrained` unified entry point** — add symmetric counterpart to `encodeKonstrained`
   that dispatches on the target class's schema type.
3. **`internal` visibility** — mark `SchemaValueCodec` and `extractSchema` as Kotlin `internal`,
   remove now-redundant `@KonditionalInternalApi` annotations from their members.

---

## Architecture

### Fix in `SchemaValueCodec.decode(kClass, json, schema)`

```kotlin
fun <T : Any> decode(kClass: KClass<T>, json: JsonObject, schema: ObjectSchema): Result<T> {
    // New: singletons have no constructor; return the existing instance.
    kClass.objectInstance?.let { return Result.success(it) }
    return kClass.primaryConstructor
        ?.let { constructor -> buildSchemaParameterMap(...) ... }
        ?: parseFailure(ParseError.InvalidSnapshot("..."))
}
```

Same pattern applied to `decode(kClass, json)` (no-schema overload) and
`decodeKonstrainedPrimitive`.

### New: `decodeKonstrained(kClass, jsonValue)`

```kotlin
@KonditionalInternalApi   // removed once object is internal
fun <T : Any> decodeKonstrained(kClass: KClass<T>, jsonValue: JsonValue): Result<T> =
    when (jsonValue) {
        is JsonObject -> decode(kClass, jsonValue)
        is JsonString -> decodeKonstrainedPrimitive(kClass, jsonValue.value)
        is JsonBoolean -> decodeKonstrainedPrimitive(kClass, jsonValue.value)
        is JsonNumber -> decodeKonstrainedPrimitive(kClass, jsonValue.toInt())  // or toDouble via schema
        is JsonArray -> decodeKonstrainedPrimitive(kClass, jsonValue.elements.map { ... })
        is JsonNull -> parseFailure(ParseError.InvalidSnapshot("Cannot decode null as $kClass"))
    }
```

The schema-based Int vs Double disambiguation uses `extractSchema(kClass)` — already available
in `SchemaExtraction.kt`.

### Visibility change

```kotlin
// Before
@KonditionalInternalApi
object SchemaValueCodec {
    @KonditionalInternalApi
    fun encodeKonstrained(...): JsonValue
    ...
}

// After
internal object SchemaValueCodec {
    fun encodeKonstrained(...): JsonValue  // @KonditionalInternalApi removed (redundant)
    ...
}
```

Same treatment for `extractSchema` in `SchemaExtraction.kt`.

---

## Test Plan (new file: `KonstrainedObjectTest.kt`)

| Test | Validates |
|------|-----------|
| `encodeKonstrained` routes `Konstrained.Object` (data class) via `ObjectTraits` path | dispatch correctness |
| `encodeKonstrained` on Kotlin `object` singleton returns correct `JsonObject` | singleton encode |
| `decode(kClass, json, schema)` returns `objectInstance` for Kotlin `object` | singleton decode fix |
| `decode(kClass, json, schema)` full encode→decode roundtrip for `data class` (all field types) | encode/decode roundtrip |
| `decode` applies schema `defaultValue` when field absent from JSON | default value |
| `decode` returns `ParameterResolution.Skip` for `isOptional` param absent from JSON | optional field |
| `decode` fails with `ParseError` when required field absent | error path |
| Nested `Konstrained.Object` inside another `Konstrained.Object` encodes and decodes | nesting |
| `FlagValue.from(dataClass)` → `DataClassValue` → `extractValue<T>(expectedSample)` | FlagValue roundtrip |
| `ConfigValue.from(dataClass)` → `DataClassValue` | ConfigValue dispatch |

Existing fixtures (`RetryPolicy`, `UserSettings`) are sufficient. Add one fixture for a Kotlin
`object` singleton.

---

## Files Changed

| File | Change |
|------|--------|
| `SchemaValueCodec.kt` | `objectInstance` fix in all 3 decode paths; add `decodeKonstrained`; mark `internal`; remove method-level `@KonditionalInternalApi` |
| `SchemaExtraction.kt` | Mark `extractSchema` as `internal`; remove `@KonditionalInternalApi` |
| `test/.../KonstrainedObjectTest.kt` | New file — all tests above |
| `test/.../TestSerializers.kt` | Add Kotlin `object` singleton fixture implementing `Konstrained.Object` |

---

## Invariants Preserved

- **Determinism**: no ambient state introduced; `objectInstance` check is pure.
- **Parse-don't-validate**: all paths return `Result`; no exceptions cross module boundaries.
- **Atomicity**: no shared mutable state touched.
- **Namespace isolation**: no cross-module changes.
