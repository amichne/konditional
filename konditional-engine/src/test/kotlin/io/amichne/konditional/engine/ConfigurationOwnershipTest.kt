package io.amichne.konditional.engine

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationMetadata
import io.amichne.konditional.fixtures.TestContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfigurationOwnershipTest {
    @Test
    fun installedAndRestoredConfigurationsOwnTheirFlagEntries() {
        val namespace = object : Namespace.TestNamespaceFacade("configuration-ownership") {
            val number by integer<Context>(default = 1)
        }
        val flags = mutableMapOf<Feature<*, *, *>, FlagDefinition<*, *, *>>(
            namespace.number to FlagDefinition(feature = namespace.number, bounds = emptyList(), defaultValue = 2),
        )
        val config = Configuration(flags, ConfigurationMetadata(version = "v3"))
        val copied = config.copy(flags = flags)
        namespace.load(config)
        flags.clear()

        assertEquals(2, namespace.number.evaluate(TestContext()))
        assertEquals("v3", namespace.registry.configuration.metadata.version)
        assertEquals(config, copied)
        assertFailsWith<UnsupportedOperationException> { (config.flags as MutableMap<*, *>).clear() }

        namespace.load(Configuration(emptyMap()))
        assertTrue(namespace.rollback())
        assertEquals(2, namespace.number.evaluate(TestContext()))
        assertEquals("v3", namespace.registry.configuration.metadata.version)
    }
}
