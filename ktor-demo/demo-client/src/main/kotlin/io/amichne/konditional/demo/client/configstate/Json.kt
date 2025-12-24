package io.amichne.konditional.demo.client.configstate

import io.amichne.konditional.demo.client.JSON
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import kotlin.js.Json
import kotlin.js.json

object Json {
    internal fun stableString(value: dynamic): String =
        when (value) {
            null -> ""
            else -> value.toString()
        }

    internal fun stableJson(value: dynamic): String =
        JSON.stringify(value.unsafeCast<Json>(), null, 2)

    internal fun deepCopy(value: dynamic): dynamic =
        JSON.parse(stableJson(value))

    internal fun dynamicStringArray(value: dynamic): List<String> =
        (value as? Array<*>)?.mapNotNull { it?.toString() }.orEmpty()

    internal fun setAtJsonPointer(
        root: dynamic,
        pointer: String,
        value: dynamic,
    ) {
        val parts = pointer.split("/").drop(1)
        if (parts.isEmpty()) {
            return
        }

        var current: dynamic = root
        for (index in 0 until parts.size - 1) {
            val key = parts[index]
            current =
                if (isArray(current)) {
                    current[key.toInt()]
                } else {
                    current[key]
                }
        }

        val last = parts.last()
        if (isArray(current)) {
            current[last.toInt()] = value
        } else {
            current[last] = value
        }
    }

    internal fun isArray(value: dynamic): Boolean =
        js("Array.isArray(value)") as Boolean

    internal fun newEmptyRuleBasedOnFlag(flag: dynamic): dynamic {
        val defaultValue = flag.defaultValue ?: json("type" to "BOOLEAN", "value" to false)
        return json(
            "value" to defaultValue,
            "rampUp" to 100.0,
            "rampUpAllowlist" to emptyArray<String>(),
            "note" to null,
            "locales" to emptyArray<String>(),
            "platforms" to emptyArray<String>(),
            "versionRange" to null,
            "axes" to json(),
        )
    }

    internal fun readVersion(wrap: HTMLElement?): dynamic {
        if (wrap == null) {
            return json("major" to 0, "minor" to 0, "patch" to 0)
        }
        val inputs = wrap.querySelectorAll("input")
        val major = (inputs.item(0) as? HTMLInputElement)?.value?.toIntOrNull() ?: 0
        val minor = (inputs.item(1) as? HTMLInputElement)?.value?.toIntOrNull() ?: 0
        val patch = (inputs.item(2) as? HTMLInputElement)?.value?.toIntOrNull() ?: 0
        return json("major" to major, "minor" to minor, "patch" to patch)
    }

    internal fun org.w3c.dom.NodeList.asElements(): List<HTMLElement> =
        (0 until length).mapNotNull { index -> item(index) as? HTMLElement }
}
