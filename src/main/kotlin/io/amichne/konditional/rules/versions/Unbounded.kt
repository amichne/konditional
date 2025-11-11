package io.amichne.konditional.rules.versions

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import io.amichne.konditional.context.Version

class Unbounded : VersionRange(Type.UNBOUNDED, MIN_VERSION, MAX_VERSION) {
    override fun contains(v: Version): Boolean = true

    /**
     * Unbounded ranges have no bounds by definition, even though they have
     * min/max values for implementation purposes.
     */
    override fun hasBounds(): Boolean = false
}
