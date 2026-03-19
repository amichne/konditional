@file:OptIn(io.amichne.konditional.api.KonditionalInternalApi::class)

package io.amichne.konditional.core

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.instance.ConfigurationMetadata
import io.amichne.konditional.core.registry.InMemoryNamespaceRegistry
import io.amichne.konditional.fixtures.RetryPolicy
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.values.NamespaceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NamespaceDeclarationTest {

    private object SimpleNs : Namespace("decl-simple") {
        val boolFlag by boolean<TestContext>(default = true) {
            enable { platforms(Platform.IOS) }
        }
        val strFlag by string<Context>(default = "hello")
        val intFlag by integer<Context>(default = 42)
        val dblFlag by double<Context>(default = 3.14)
        val enumFlag by enum<Platform, Context>(default = Platform.IOS)
    }

    @Test
    fun `allFeatures returns all declared features`() {
        val features = SimpleNs.allFeatures()
        assertEquals(5, features.size)
    }

    @Test
    fun `declaredDefault returns the default value for each feature`() {
        assertEquals(true, SimpleNs.declaredDefault(SimpleNs.boolFlag))
        assertEquals("hello", SimpleNs.declaredDefault(SimpleNs.strFlag))
        assertEquals(42, SimpleNs.declaredDefault(SimpleNs.intFlag))
        assertEquals(3.14, SimpleNs.declaredDefault(SimpleNs.dblFlag))
        assertEquals(Platform.IOS, SimpleNs.declaredDefault(SimpleNs.enumFlag))
    }

    @Test
    fun `declaredDefinition returns a non-null definition for declared features`() {
        assertNotNull(SimpleNs.declaredDefinition(SimpleNs.boolFlag))
        assertNotNull(SimpleNs.declaredDefinition(SimpleNs.strFlag))
    }

    @Test
    fun `findFlag returns null when feature is not in registry`() {
        val freshNs = object : Namespace.TestNamespaceFacade("find-flag-test") {
            val flag by boolean<Context>(default = false)
        }
        // No configuration loaded — findFlag on the registry layer returns null,
        // but Namespace overrides it to fall back to declaredDefinition
        // For an isolated registry lookup, check via registry directly
        val registry = freshNs.registry as InMemoryNamespaceRegistry
        assertNull(registry.findFlag(freshNs.flag))
    }

    @Test
    fun `findFlag via Namespace falls back to declared definitions`() {
        val ns = object : Namespace.TestNamespaceFacade("find-flag-fallback") {
            val flag by boolean<Context>(default = false)
        }
        // No load → declared definition is the fallback
        assertNotNull(ns.findFlag(ns.flag))
    }

    @Test
    fun `configuration merges declared definitions with registry flags`() {
        val ns = object : Namespace.TestNamespaceFacade("config-merge") {
            val declared by boolean<Context>(default = true)
        }
        val registry = ns.registry as InMemoryNamespaceRegistry

        val loadedConfig = Configuration(
            flags = mapOf(ns.declared to FlagDefinition(feature = ns.declared, bounds = emptyList(), defaultValue = false)),
            metadata = ConfigurationMetadata(version = "v1"),
        )
        registry.load(loadedConfig)

        // The merged configuration should include both declared AND registry flags
        val mergedFlags = ns.configuration.flags
        assertTrue(mergedFlags.containsKey(ns.declared))
    }

    @Test
    fun `loaded configuration value takes precedence over declared default`() {
        val ns = object : Namespace.TestNamespaceFacade("config-precedence") {
            val flag by boolean<Context>(default = false)
        }
        val ctx = TestContext()

        assertFalse(ns.flag.evaluate(ctx))

        ns.load(
            Configuration(
                flags = mapOf(ns.flag to FlagDefinition(feature = ns.flag, bounds = emptyList(), defaultValue = true)),
                metadata = ConfigurationMetadata(version = "v1"),
            ),
        )

        assertTrue(ns.flag.evaluate(ctx))
    }

    @Test
    fun `boolean features evaluate correctly`() {
        val ns = object : Namespace.TestNamespaceFacade("bool-eval") {
            val flag by boolean<TestContext>(default = false) {
                enable { platforms(Platform.IOS) }
            }
        }
        assertTrue(ns.flag.evaluate(TestContext(platform = Platform.IOS)))
        assertFalse(ns.flag.evaluate(TestContext(platform = Platform.ANDROID)))
    }

    @Test
    fun `string features evaluate correctly`() {
        val ns = object : Namespace.TestNamespaceFacade("string-eval") {
            val greeting by string<Context>(default = "hello")
        }
        assertEquals("hello", ns.greeting.evaluate(TestContext()))
    }

    @Test
    fun `integer features evaluate correctly`() {
        val ns = object : Namespace.TestNamespaceFacade("int-eval") {
            val limit by integer<Context>(default = 10)
        }
        assertEquals(10, ns.limit.evaluate(TestContext()))
    }

    @Test
    fun `double features evaluate correctly`() {
        val ns = object : Namespace.TestNamespaceFacade("double-eval") {
            val threshold by double<Context>(default = 0.5)
        }
        assertEquals(0.5, ns.threshold.evaluate(TestContext()))
    }

    @Test
    fun `enum features evaluate correctly`() {
        val ns = object : Namespace.TestNamespaceFacade("enum-eval") {
            val platform by enum<Platform, Context>(default = Platform.IOS)
        }
        assertEquals(Platform.IOS, ns.platform.evaluate(TestContext()))
    }

    @Test
    fun `namespace equals is id-based`() {
        val ns1 = object : Namespace.TestNamespaceFacade("same-id") {}
        val ns2 = object : Namespace.TestNamespaceFacade("same-id") {}
        val ns3 = object : Namespace.TestNamespaceFacade("different-id") {}

        assertEquals<Namespace>(ns1, ns2)
        assertFalse(ns1 == ns3 as Any)
    }

    @Test
    fun `namespace hashCode is id-based`() {
        val ns1 = object : Namespace.TestNamespaceFacade("hash-ns") {}
        val ns2 = object : Namespace.TestNamespaceFacade("hash-ns") {}

        assertEquals(ns1.hashCode(), ns2.hashCode())
    }

    @Test
    fun `namespace toString contains id`() {
        val ns = object : Namespace.TestNamespaceFacade("my-namespace") {}
        assertTrue(ns.toString().contains("my-namespace"))
    }

    @Test
    fun `TestNamespaceFacade constructor with NamespaceId works`() {
        val id = NamespaceId("namespaceId-from-id")
        val ns = object : Namespace.TestNamespaceFacade(id) {}
        assertEquals(id, ns.id)
    }

    @Test
    fun `custom Konstrained feature evaluates correctly`() {
        val ns = object : Namespace.TestNamespaceFacade("custom-eval") {
            val config by custom<RetryPolicy, Context>(default = RetryPolicy(maxAttempts = 3))
        }

        assertEquals(RetryPolicy(maxAttempts = 3), ns.config.evaluate(TestContext()))
    }
}
