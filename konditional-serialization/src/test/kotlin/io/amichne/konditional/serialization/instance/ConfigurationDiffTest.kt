@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization.instance

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigurationDiffTest {
    @Test
    fun `diff reports added removed and changed flags`() {
        val ns = object : Namespace.TestNamespaceFacade("diff-test") {
            val A by boolean<Context>(default = false)
            val B by boolean<Context>(default = false)
        }

        val before = ns.configuration as Configuration
        val after = Configuration(
            flags = mapOf(
                ns.A to FlagDefinition(
                    feature = ns.A,
                    bounds = emptyList(),
                    defaultValue = true,
                )
            ),
            metadata = ConfigurationMetadata(version = "rev-2"),
        )

        val diff = before.diff(after)

        assertEquals("rev-2", diff.after.version)
        assertEquals(listOf(ns.B.id), diff.removed.map { it.id })
        assertEquals(listOf(ns.A.id), diff.changed.map { it.id })
        assertEquals(true, (diff.changed.single().after.defaultValue as ConfigValue.BooleanValue).value)
    }
}
