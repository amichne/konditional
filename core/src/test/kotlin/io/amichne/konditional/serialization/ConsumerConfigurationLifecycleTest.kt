package io.amichne.konditional.serialization

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.KotlinEncodeable
import io.amichne.kontracts.dsl.of
import io.amichne.kontracts.dsl.schemaRoot
import io.amichne.kontracts.schema.ObjectSchema
import io.amichne.kontracts.value.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ConsumerConfigurationLifecycleTest {
    enum class Theme {
        LIGHT,
        DARK,
    }

    data class UserSettings(
        val theme: String = "light",
        val maxRetries: Int = 3,
        val timeoutSeconds: Double = 30.0,
        val enabled: Boolean = true,
    ) : KotlinEncodeable<ObjectSchema> {
        override val schema =
            schemaRoot {
                ::theme of { minLength = 1 }
                ::maxRetries of { minimum = 0 }
                ::timeoutSeconds of { minimum = 0.0 }
                ::enabled of {}
            }
    }

    private fun ctx(
        platform: Platform,
        locale: AppLocale = AppLocale.UNITED_STATES,
        version: Version = Version.of(1, 0, 0),
        stableId: StableId = StableId.of("consumer-lifecycle"),
    ): Context =
        Context(
            locale = locale,
            platform = platform,
            appVersion = version,
            stableId = stableId,
        )

    @Test
    fun `consumer lifecycle supports dump load and patch`() {
        val namespaceId = "consumer-lifecycle-${UUID.randomUUID()}"
        val namespaceV1 =
            object : Namespace(namespaceId) {
                val darkMode by boolean<Context>(default = false) {
                    salt("v1")
                    rule(true) {
                        platforms(Platform.IOS)
                        rampUp { 100.0 }
                        note("iOS fully enabled")
                    }
                }

                val apiEndpoint by string<Context>(default = "https://api.example.com") {
                    rule("https://api-web.example.com") {
                        platforms(Platform.WEB)
                        rampUp { 100.0 }
                        note("Web endpoint override")
                    }
                }

                val maxRetries by integer<Context>(default = 3) {
                    rule(5) {
                        versions { min(2, 0, 0) }
                        rampUp { 100.0 }
                        note("More retries on v2+")
                    }
                }

                val theme by enum<Theme, Context>(default = Theme.LIGHT) {
                    rule(Theme.DARK) {
                        locales(AppLocale.FRANCE)
                        rampUp { 100.0 }
                        note("Dark theme for FR locale")
                    }
                }

                val userSettings by custom<UserSettings, Context>(default = UserSettings()) {
                    rule(UserSettings(theme = "dark", maxRetries = 5, timeoutSeconds = 10.0, enabled = false)) {
                        platforms(Platform.IOS)
                        rampUp { 100.0 }
                        note("Custom settings for iOS")
                    }
                }
            }

        val ios = ctx(platform = Platform.IOS, version = Version.of(2, 1, 0))
        val web = ctx(platform = Platform.WEB, version = Version.of(2, 1, 0))
        val france = ctx(platform = Platform.ANDROID, locale = AppLocale.FRANCE, version = Version.of(2, 1, 0))
        val v1 = ctx(platform = Platform.ANDROID, version = Version.of(1, 0, 0))
        val v2 = ctx(platform = Platform.ANDROID, version = Version.of(2, 0, 0))

        assertTrue(namespaceV1.darkMode.evaluate(ios))
        assertFalse(namespaceV1.darkMode.evaluate(web))
        assertEquals("https://api-web.example.com", namespaceV1.apiEndpoint.evaluate(web))
        assertEquals("https://api.example.com", namespaceV1.apiEndpoint.evaluate(ios))
        assertEquals(3, namespaceV1.maxRetries.evaluate(v1))
        assertEquals(5, namespaceV1.maxRetries.evaluate(v2))
        assertEquals(Theme.DARK, namespaceV1.theme.evaluate(france))
        assertEquals(
            UserSettings(theme = "dark", maxRetries = 5, timeoutSeconds = 10.0, enabled = false),
            namespaceV1.userSettings.evaluate(ios),
        )

        val dumpedJson = NamespaceSnapshotSerializer(namespaceV1).toJson()
        assertTrue(dumpedJson.contains(namespaceV1.darkMode.id.toString()))
        assertTrue(dumpedJson.contains(namespaceV1.apiEndpoint.id.toString()))
        assertTrue(dumpedJson.contains(namespaceV1.maxRetries.id.toString()))
        assertTrue(dumpedJson.contains(namespaceV1.theme.id.toString()))
        assertTrue(dumpedJson.contains(namespaceV1.userSettings.id.toString()))
        println(dumpedJson)

        // Simulate a fresh process (FeatureRegistry is process-global).
        FeatureRegistry.clear()

        val namespaceV2 =
            object : Namespace(namespaceId) {
                val darkMode by boolean<Context>(default = true) {
                    rule(false) { platforms(Platform.IOS) }
                }

                val apiEndpoint by string<Context>(default = "https://wrong.example.com") {}

                val maxRetries by integer<Context>(default = 0) {}

                val theme by enum<Theme, Context>(default = Theme.DARK) {}

                val userSettings by custom<UserSettings, Context>(
                    default = UserSettings(theme = "baseline", maxRetries = 0, timeoutSeconds = 0.0, enabled = true),
                ) {}
            }

        assertFalse(namespaceV2.darkMode.evaluate(ios), "baseline differs before load")
        assertEquals("https://wrong.example.com", namespaceV2.apiEndpoint.evaluate(web), "baseline differs before load")
        assertEquals(0, namespaceV2.maxRetries.evaluate(v2), "baseline differs before load")
        assertEquals(Theme.DARK, namespaceV2.theme.evaluate(france), "baseline differs before load")
        assertEquals(
            UserSettings(theme = "baseline", maxRetries = 0, timeoutSeconds = 0.0, enabled = true),
            namespaceV2.userSettings.evaluate(ios),
            "baseline differs before load",
        )

        when (val loaded = NamespaceSnapshotSerializer(namespaceV2).fromJson(dumpedJson)) {
            is ParseResult.Success -> assertNotNull(loaded.value)
            is ParseResult.Failure -> error("Failed to load dumped snapshot: ${loaded.error.message}")
        }

        assertTrue(namespaceV2.darkMode.evaluate(ios))
        assertFalse(namespaceV2.darkMode.evaluate(web))
        assertEquals("https://api-web.example.com", namespaceV2.apiEndpoint.evaluate(web))
        assertEquals("https://api.example.com", namespaceV2.apiEndpoint.evaluate(ios))
        assertEquals(3, namespaceV2.maxRetries.evaluate(v1))
        assertEquals(5, namespaceV2.maxRetries.evaluate(v2))
        assertEquals(Theme.DARK, namespaceV2.theme.evaluate(france))
        assertEquals(
            UserSettings(theme = "dark", maxRetries = 5, timeoutSeconds = 10.0, enabled = false),
            namespaceV2.userSettings.evaluate(ios),
        )

        val patchJson =
            """
            {
              "flags": [
                {
                  "key": "${namespaceV2.darkMode.id}",
                  "defaultValue": { "type": "BOOLEAN", "value": false },
                  "salt": "v1",
                  "isActive": false,
                  "rules": []
                },
                {
                  "key": "${namespaceV2.maxRetries.id}",
                  "defaultValue": { "type": "INT", "value": 7 },
                  "salt": "v1",
                  "isActive": true,
                  "rules": [
                    {
                      "value": { "type": "INT", "value": 11 },
                      "rampUp": 100.0,
                      "note": "More retries on v3+",
                      "locales": [],
                      "platforms": [],
                      "versionRange": { "type": "MIN_BOUND", "min": { "major": 3, "minor": 0, "patch": 0 } }
                    }
                  ]
                }
              ],
              "removeKeys": [
                "${namespaceV2.apiEndpoint.id}"
              ]
            }
            """.trimIndent()

        val patchedConfig =
            when (val patched = SnapshotSerializer.applyPatchJson(namespaceV2.configuration, patchJson)) {
                is ParseResult.Success -> patched.value
                is ParseResult.Failure -> error("Failed to apply patch: ${patched.error.message}")
            }

        namespaceV2.load(patchedConfig)

        assertFalse(namespaceV2.darkMode.evaluate(ios), "inactive flag returns default regardless create rules")
        assertEquals(7, namespaceV2.maxRetries.evaluate(v2), "default updated via patch")
        assertEquals(
            11,
            namespaceV2.maxRetries.evaluate(ctx(platform = Platform.ANDROID, version = Version.of(3, 0, 0))),
        )
        assertFalse(
            namespaceV2.configuration.flags.containsKey(namespaceV2.apiEndpoint),
            "flag removed via patch is absent from configuration",
        )
    }
}
