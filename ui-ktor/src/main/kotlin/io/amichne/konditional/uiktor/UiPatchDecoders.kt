package io.amichne.konditional.uiktor

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.amichne.konditional.uispec.JsonPointer
import io.amichne.konditional.uispec.UiArray
import io.amichne.konditional.uispec.UiBoolean
import io.amichne.konditional.uispec.UiDouble
import io.amichne.konditional.uispec.UiEnum
import io.amichne.konditional.uispec.UiInt
import io.amichne.konditional.uispec.UiMap
import io.amichne.konditional.uispec.UiNull
import io.amichne.konditional.uispec.UiObject
import io.amichne.konditional.uispec.UiPatchAdd
import io.amichne.konditional.uispec.UiPatchCopy
import io.amichne.konditional.uispec.UiPatchMove
import io.amichne.konditional.uispec.UiPatchOperation
import io.amichne.konditional.uispec.UiPatchRemove
import io.amichne.konditional.uispec.UiPatchReplace
import io.amichne.konditional.uispec.UiPatchTest
import io.amichne.konditional.uispec.UiString
import io.amichne.konditional.uispec.UiValue

class MoshiUiPatchDecoder(
    moshi: Moshi = defaultMoshi(),
) : UiPatchDecoder {
    private val adapter: JsonAdapter<List<PatchOperationPayload>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, PatchOperationPayload::class.java))
    private val envelopeAdapter: JsonAdapter<PatchEnvelope> =
        moshi.adapter(PatchEnvelope::class.java)

    override fun decode(raw: String): List<UiPatchOperation> =
        runCatching { adapter.fromJson(raw) }
            .getOrNull()
            ?.orEmpty()
            ?.map(::toUiPatchOperation)
            ?: envelopeAdapter.fromJson(raw)
                ?.operations()
                .orEmpty()
                .map(::toUiPatchOperation)

    private fun toUiPatchOperation(payload: PatchOperationPayload): UiPatchOperation =
        when (payload.op.lowercase()) {
            "add" -> UiPatchAdd(
                path = payload.path.asPointer(),
                value = payload.value.toUiValue(),
            )
            "remove" -> UiPatchRemove(path = payload.path.asPointer())
            "replace" -> UiPatchReplace(
                path = payload.path.asPointer(),
                value = payload.value.toUiValue(),
            )
            "move" -> UiPatchMove(
                path = payload.path.asPointer(),
                from = payload.from.asPointer(),
            )
            "copy" -> UiPatchCopy(
                path = payload.path.asPointer(),
                from = payload.from.asPointer(),
            )
            "test" -> UiPatchTest(
                path = payload.path.asPointer(),
                value = payload.value.toUiValue(),
            )
            else -> throw IllegalArgumentException("Unsupported JSON Patch op: ${payload.op}")
        }

    private fun String?.asPointer(): JsonPointer =
        JsonPointer(requireNotNull(this) { "JSON Patch pointer must be provided." })

    private fun Any?.toUiValue(): UiValue =
        when (this) {
            null -> UiNull
            is UiValue -> this
            is Boolean -> UiBoolean(this)
            is String -> UiString(this)
            is Int -> UiInt(this.toLong())
            is Long -> UiInt(this)
            is Double -> UiDouble(this)
            is Float -> UiDouble(this.toDouble())
            is Enum<*> -> UiEnum(this.name)
            is Map<*, *> -> UiObject(
                this.entries.associate { entry ->
                    entry.key.toString() to entry.value.toUiValue()
                },
            )
            is Iterable<*> -> UiArray(this.map { entry -> entry.toUiValue() })
            else -> UiString(this.toString())
        }

    private data class PatchOperationPayload(
        val op: String,
        val path: String?,
        val from: String? = null,
        val value: Any? = null,
    )

    private data class PatchEnvelope(
        val patch: List<PatchOperationPayload>? = null,
        val operations: List<PatchOperationPayload>? = null,
    ) {
        fun operations(): List<PatchOperationPayload> =
            patch ?: operations.orEmpty()
    }
}

private fun defaultMoshi(): Moshi =
    Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
