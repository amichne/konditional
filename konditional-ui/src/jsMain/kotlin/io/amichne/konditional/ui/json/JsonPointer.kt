package io.amichne.konditional.ui.json

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Minimal RFC-6901 JSON Pointer implementation for configuration editing.
 *
 * Notes:
 * - Supports absolute pointers (must start with `/`) and the empty pointer `""` (the whole document).
 * - Supports template segments of `*` for wildcard matching against arrays and objects.
 * - Only implements the subset needed for Konditional configstate bindings.
 */
object JsonPointer {
    data class Segment(
        val raw: String,
    ) {
        val isWildcard: Boolean get() = raw == "*"

        fun unescaped(): String = raw.replace("~1", "/").replace("~0", "~")
    }

    fun parse(pointer: String): List<Segment> =
        when {
            pointer.isEmpty() -> emptyList()
            pointer == "/" -> listOf(Segment(""))
            pointer.startsWith("/") -> pointer.removePrefix("/").split("/").map(::Segment)
            else -> throw IllegalArgumentException("JSON pointer must be absolute (start with '/'), got '$pointer'")
        }

    fun get(
        root: JsonElement,
        pointer: String,
    ): Result<JsonElement> = get(root, parse(pointer))

    fun get(
        root: JsonElement,
        segments: List<Segment>,
    ): Result<JsonElement> =
        segments.fold(Result.success(root)) { acc, seg ->
            acc.fold(
                onSuccess = { current ->
                    when (current) {
                        is JsonObject -> current[seg.unescaped()]?.let(Result.Companion::success)
                            ?: Result.failure(NoSuchElementException("Missing object key '${seg.unescaped()}'"))

                        is JsonArray -> {
                            val index = seg.unescaped().toIntOrNull()
                                ?: return@fold Result.failure(IllegalArgumentException("Expected array index but was '${seg.unescaped()}'"))
                            current.getOrNull(index)?.let(Result.Companion::success)
                                ?: Result.failure(IndexOutOfBoundsException("Array index out of bounds: $index"))
                        }

                        else -> Result.failure(IllegalStateException("Cannot dereference through primitive/null at segment '${seg.raw}'"))
                    }
                },
                onFailure = Result.Companion::failure,
            )
        }

    fun set(
        root: JsonElement,
        pointer: String,
        newValue: JsonElement,
    ): Result<JsonElement> = set(root, parse(pointer), newValue)

    fun set(
        root: JsonElement,
        segments: List<Segment>,
        newValue: JsonElement,
    ): Result<JsonElement> =
        if (segments.isEmpty()) {
            Result.success(newValue)
        } else {
            setRec(root, segments, 0, newValue)
        }

    private fun setRec(
        current: JsonElement,
        segments: List<Segment>,
        index: Int,
        newValue: JsonElement,
    ): Result<JsonElement> {
        val seg = segments[index]
        val isLast = index == segments.lastIndex
        val keyOrIndex = seg.unescaped()

        return when (current) {
            is JsonObject -> {
                val existing = current[keyOrIndex]
                    ?: return Result.failure(NoSuchElementException("Missing object key '$keyOrIndex'"))

                val updatedChildResult =
                    if (isLast) {
                        Result.success(newValue)
                    } else {
                        setRec(existing, segments, index + 1, newValue)
                    }

                updatedChildResult.map { updatedChild ->
                    JsonObject(current.toMutableMap().apply { put(keyOrIndex, updatedChild) })
                }
            }

            is JsonArray -> {
                val arrayIndex = keyOrIndex.toIntOrNull()
                    ?: return Result.failure(IllegalArgumentException("Expected array index but was '$keyOrIndex'"))

                val existing = current.getOrNull(arrayIndex)
                    ?: return Result.failure(IndexOutOfBoundsException("Array index out of bounds: $arrayIndex"))

                val updatedChildResult =
                    if (isLast) {
                        Result.success(newValue)
                    } else {
                        setRec(existing, segments, index + 1, newValue)
                    }

                updatedChildResult.map { updatedChild ->
                    JsonArray(
                        current.mapIndexed { i, el -> if (i == arrayIndex) updatedChild else el },
                    )
                }
            }

            else -> Result.failure(IllegalStateException("Cannot set into primitive/null at segment '${seg.raw}'"))
        }
    }

    /**
     * Appends a new element to an array at the given pointer.
     */
    fun append(
        root: JsonElement,
        pointer: String,
        newItem: JsonElement,
    ): Result<JsonElement> {
        val segments = parse(pointer)
        if (segments.isEmpty()) {
            return when (root) {
                is JsonArray -> Result.success(JsonArray(root + newItem))
                else -> Result.failure(IllegalStateException("Cannot append to non-array"))
            }
        }

        val parentPointer = segments.dropLast(1)
        val lastSeg = segments.last().unescaped()

        return get(root, parentPointer).flatMap { parent ->
            when (parent) {
                is JsonObject -> {
                    val array = parent[lastSeg] as? JsonArray
                        ?: return@flatMap Result.failure(IllegalStateException("Expected array at '$lastSeg'"))
                    val newArray = JsonArray(array + newItem)
                    val updatedParent = JsonObject(parent.toMutableMap().apply { put(lastSeg, newArray) })
                    set(root, parentPointer, updatedParent)
                }
                is JsonArray -> {
                    val arrayIndex = lastSeg.toIntOrNull()
                        ?: return@flatMap Result.failure(IllegalArgumentException("Expected array index"))
                    val array = parent.getOrNull(arrayIndex) as? JsonArray
                        ?: return@flatMap Result.failure(IllegalStateException("Expected array at index $arrayIndex"))
                    val newArray = JsonArray(array + newItem)
                    val updatedParent = JsonArray(parent.mapIndexed { i, el -> if (i == arrayIndex) newArray else el })
                    set(root, parentPointer, updatedParent)
                }
                else -> Result.failure(IllegalStateException("Cannot navigate through primitive"))
            }
        }
    }

    /**
     * Removes an element at the given array index pointer.
     */
    fun removeAt(
        root: JsonElement,
        pointer: String,
    ): Result<JsonElement> {
        val segments = parse(pointer)
        if (segments.isEmpty()) {
            return Result.failure(IllegalStateException("Cannot remove root"))
        }

        val parentPointer = segments.dropLast(1)
        val lastSeg = segments.last().unescaped()
        val arrayIndex = lastSeg.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("removeAt requires array index, got '$lastSeg'"))

        return get(root, parentPointer).flatMap { parent ->
            when (parent) {
                is JsonArray -> {
                    if (arrayIndex < 0 || arrayIndex >= parent.size) {
                        return@flatMap Result.failure(IndexOutOfBoundsException("Index $arrayIndex out of bounds"))
                    }
                    val newArray = JsonArray(parent.filterIndexed { i, _ -> i != arrayIndex })
                    set(root, parentPointer, newArray)
                }
                else -> Result.failure(IllegalStateException("Expected array parent for removeAt"))
            }
        }
    }

    /**
     * Moves an array element from one index to another.
     */
    fun move(
        root: JsonElement,
        arrayPointer: String,
        fromIndex: Int,
        toIndex: Int,
    ): Result<JsonElement> =
        get(root, arrayPointer).flatMap { element ->
            when (element) {
                is JsonArray -> {
                    if (fromIndex < 0 || fromIndex >= element.size) {
                        return@flatMap Result.failure(IndexOutOfBoundsException("fromIndex $fromIndex out of bounds"))
                    }
                    if (toIndex < 0 || toIndex >= element.size) {
                        return@flatMap Result.failure(IndexOutOfBoundsException("toIndex $toIndex out of bounds"))
                    }
                    if (fromIndex == toIndex) {
                        return@flatMap Result.success(root)
                    }

                    val items = element.toMutableList()
                    val item = items.removeAt(fromIndex)
                    items.add(toIndex, item)
                    set(root, arrayPointer, JsonArray(items))
                }
                else -> Result.failure(IllegalStateException("Expected array at '$arrayPointer'"))
            }
        }

    /**
     * Expands a template pointer (with `*`) to all matching concrete pointers in [root].
     *
     * Example template: `/flags/<any>/rules/<any>/rampUp` expands to `/flags/0/rules/0/rampUp`, ...
     */
    fun expandTemplate(
        root: JsonElement,
        templatePointer: String,
    ): Result<List<String>> = expandTemplate(root, parse(templatePointer), currentPointer = "")

    private fun expandTemplate(
        current: JsonElement,
        templateSegments: List<Segment>,
        currentPointer: String,
    ): Result<List<String>> =
        if (templateSegments.isEmpty()) {
            Result.success(listOf(currentPointer.ifEmpty { "" }))
        } else {
            val head = templateSegments.first()
            val tail = templateSegments.drop(1)

            when {
                head.isWildcard -> expandWildcard(current, tail, currentPointer)
                else -> expandConcrete(current, head, tail, currentPointer)
            }
        }

    private fun expandConcrete(
        current: JsonElement,
        head: Segment,
        tail: List<Segment>,
        currentPointer: String,
    ): Result<List<String>> {
        val segmentValue = head.unescaped()
        val nextPointer = "$currentPointer/${head.raw}"

        val nextElementResult =
            when (current) {
                is JsonObject -> current[segmentValue]?.let(Result.Companion::success)
                    ?: Result.failure(NoSuchElementException("Missing object key '$segmentValue'"))

                is JsonArray -> {
                    val arrayIndex = segmentValue.toIntOrNull()
                        ?: return Result.failure(IllegalArgumentException("Expected array index but was '$segmentValue'"))
                    current.getOrNull(arrayIndex)?.let(Result.Companion::success)
                        ?: Result.failure(IndexOutOfBoundsException("Array index out of bounds: $arrayIndex"))
                }

                else -> Result.failure(IllegalStateException("Cannot expand through primitive/null at segment '${head.raw}'"))
            }

        return nextElementResult.flatMap { next -> expandTemplate(next, tail, nextPointer) }
    }

    private fun expandWildcard(
        current: JsonElement,
        tail: List<Segment>,
        currentPointer: String,
    ): Result<List<String>> =
        when (current) {
            is JsonArray -> {
                val expansions =
                    current.indices.map { i ->
                        expandTemplate(current[i], tail, "$currentPointer/$i")
                    }
                merge(expansions)
            }

            is JsonObject -> {
                val expansions =
                    current.keys.sorted().map { key ->
                        val child = current[key] ?: JsonNull
                        expandTemplate(child, tail, "$currentPointer/${escape(key)}")
                    }
                merge(expansions)
            }

            else -> Result.failure(IllegalStateException("Wildcard expansion requires array/object but was ${current::class.simpleName}"))
        }

    private fun escape(segment: String): String = segment.replace("~", "~0").replace("/", "~1")

    private fun <T> merge(results: List<Result<List<T>>>): Result<List<T>> =
        results.fold(Result.success(emptyList())) { acc, next ->
            acc.flatMap { a -> next.map { b -> a + b } }
        }
}

fun JsonElement.asStringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

fun JsonElement.asDoubleOrNull(): Double? = (this as? JsonPrimitive)?.doubleOrNull

private inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    fold(
        onSuccess = transform,
        onFailure = Result.Companion::failure,
    )
