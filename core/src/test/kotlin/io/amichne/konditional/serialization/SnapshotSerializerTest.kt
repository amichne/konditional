package io.amichne.konditional.serialization

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.axis.Axis
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.getOrThrow
import io.amichne.konditional.fixtures.serializers.RetryPolicy
import io.amichne.konditional.fixtures.utilities.localeIds
import io.amichne.konditional.fixtures.utilities.platformIds
import io.amichne.konditional.fixtures.utilities.update
import io.amichne.konditional.internal.serialization.models.SerializablePatch
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.values.FeatureId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for SnapshotSerializer.
 *
 * Tests both serialization and deserialization in both directions,
 * including round-trip tests, error cases, and patch functionality.
 */
class SnapshotSerializerTest {
    private object TestFeatures : Namespace.TestNamespaceFacade("snapshot-serializer") {
        val boolFlag by boolean<Context>(default = false)
        val stringFlag by string<Context>(default = "default")
        val intFlag by integer<Context>(default = 0)
        val doubleFlag by double<Context>(default = 0.0)
        val themeFlag by enum<Theme, Context>(default = Theme.LIGHT)
        val retryPolicyFlag by custom<RetryPolicy, Context>(default = RetryPolicy())
    }

    @BeforeEach
    fun setup() {
        // Register type serializers
        SerializerRegistry.register(RetryPolicy::class, RetryPolicy.serializer)

        // Force axis registration for type-based axis() usage in rule builders.
        @Suppress("UnusedExpression")
        Axes.EnvironmentAxis
        @Suppress("UnusedExpression")
        Axes.TenantAxis

        // Clear both FeatureRegistry and the namespace registry before each test
        FeatureRegistry.clear()
        TestFeatures.load(Configuration(emptyMap()))

        // Register test features
        FeatureRegistry.register(TestFeatures.boolFlag)
        FeatureRegistry.register(TestFeatures.stringFlag)
        FeatureRegistry.register(TestFeatures.intFlag)
        FeatureRegistry.register(TestFeatures.doubleFlag)
        FeatureRegistry.register(TestFeatures.themeFlag)
        FeatureRegistry.register(TestFeatures.retryPolicyFlag)
    }

    @AfterEach
    fun cleanup() {
        SerializerRegistry.clear()
    }

    private enum class Environment(override val id: String) : AxisValue<Environment> {
        PROD("prod"),
        STAGE("stage"),
        DEV("dev"),
    }

    private enum class Tenant(override val id: String) : AxisValue<Tenant> {
        ENTERPRISE("enterprise"),
        SMB("smb"),
    }

    private object Axes {
        data object EnvironmentAxis : Axis<Environment>("snapshot-environment", Environment::class)
        data object TenantAxis : Axis<Tenant>("snapshot-tenant", Tenant::class)
    }

    private enum class Theme {
        LIGHT,
        DARK,
    }

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.UNITED_STATES,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
    ) = Context(locale, platform, Version.parseUnsafe(version), StableId.of(idHex))

    private fun ctxWithEnvironment(env: Environment): Context =
        object : Context {
            override val locale: AppLocale = AppLocale.UNITED_STATES
            override val platform: Platform = Platform.IOS
            override val appVersion: Version = Version.of(1, 0, 0)
            override val stableId: StableId = StableId.of("axis-user")
            override val axisValues =
                axisValues {
                    this[Axes.EnvironmentAxis] = env
                }
        }

    // ========== Serialization Tests ==========

    @Test
    fun `Given empty Konfig, When serialized, Then produces valid JSON with empty flags array`() {
        val configuration = Configuration(emptyMap())

        val json = SnapshotSerializer.serialize(configuration)

        assertNotNull(json)
        assertTrue(json.contains("\"flags\""))
        assertTrue(json.contains("[]"))
    }

    @Test
    fun `Given Konfig with boolean flag, When serialized, Then includes flag with correct type`() {
        TestFeatures.boolFlag.update(true) {}

        val json = SnapshotSerializer.serialize(TestFeatures.configuration)

        assertNotNull(json)
        assertTrue(json.contains("\"key\": \"${TestFeatures.boolFlag.id}\""))
        assertTrue(json.contains("\"type\": \"BOOLEAN\""))
        assertTrue(json.contains("\"value\": true"))
    }

    @Test
    fun `Given Konfig with string flag, When serialized, Then includes flag with correct type`() {
        val flag =
            FlagDefinition(
                feature = TestFeatures.stringFlag,
                defaultValue = "test-value",
            )
        val configuration = Configuration(mapOf(TestFeatures.stringFlag to flag))

        val json = SnapshotSerializer.serialize(configuration)

        assertNotNull(json)
        assertTrue(json.contains("\"key\": \"${TestFeatures.stringFlag.id}\""))
        assertTrue(json.contains("\"type\": \"STRING\""))
        assertTrue(json.contains("\"value\": \"test-value\""))
    }

    @Test
    fun `Given Konfig with int flag, When serialized, Then includes flag with correct type`() {
        TestFeatures.intFlag.update(42) {}
        val json = SnapshotSerializer.serialize(TestFeatures.configuration)

        assertNotNull(json)
        println(json)
        assertTrue(json.contains("\"key\": \"${TestFeatures.intFlag.id}\""))
        assertTrue(json.contains("\"type\": \"INT\""))
        assertTrue(json.contains("\"value\": 42"))
    }

    @Test
    fun `Given Konfig with double flag, When serialized, Then includes flag with correct type`() {
        TestFeatures.doubleFlag.update(3.14) {}

        val json = SnapshotSerializer.serialize(TestFeatures.configuration)

        assertNotNull(json)
        assertTrue(json.contains("\"key\": \"${TestFeatures.doubleFlag.id}\""))
        assertTrue(json.contains("\"type\": \"DOUBLE\""))
        assertTrue(json.contains("\"value\": 3.14"))
    }

    @Test
    fun `Given Konfig with complex rules, When serialized, Then includes all rule attributes`() {
        val rule =
            Rule<Context>(
                rampUp = RampUp.of(50.0),
                note = "TestNamespace rule",
                locales = localeIds(AppLocale.UNITED_STATES, AppLocale.FRANCE),
                platforms = platformIds(Platform.IOS, Platform.ANDROID),
                versionRange = FullyBound(Version(1, 0, 0), Version(2, 0, 0)),
            )

        val flag =
            FlagDefinition(
                feature = TestFeatures.boolFlag,
                bounds = listOf(rule.targetedBy(true)),
                defaultValue = false,
            )
        val configuration = Configuration(mapOf(TestFeatures.boolFlag to flag))

        val json = SnapshotSerializer.serialize(configuration)

        assertNotNull(json)
        assertTrue(json.contains("\"rampUp\": 50.0"))
        assertTrue(json.contains("\"note\": \"TestNamespace rule\""))
        assertTrue(json.contains("UNITED_STATES"))
        assertTrue(json.contains("FRANCE"))
        assertTrue(json.contains("IOS"))
        assertTrue(json.contains("ANDROID"))
        assertTrue(json.contains("MIN_AND_MAX_BOUND"))
    }

    @Test
    fun `Given Konfig with rollout allowlists, When round-tripped, Then allowlists are preserved`() {
        val allowlistedId = "allowlisted-user"
        val otherId = "other-user"
        val allowlisted = StableId.of(allowlistedId)

        TestFeatures.boolFlag.update(false) {
            allowlist(allowlisted)
            rule(true) {
                rampUp { 0.0 }
            }
        }

        assertTrue(TestFeatures.boolFlag.evaluate(ctx(allowlistedId)))
        assertFalse(TestFeatures.boolFlag.evaluate(ctx(otherId)))

        val json = SnapshotSerializer.serialize(TestFeatures.configuration)
        val parsed = SnapshotSerializer.fromJson(json).getOrThrow()

        TestFeatures.load(Configuration(emptyMap()))
        TestFeatures.load(parsed)

        assertTrue(TestFeatures.boolFlag.evaluate(ctx(allowlistedId)))
        assertFalse(TestFeatures.boolFlag.evaluate(ctx(otherId)))
    }

    @Test
    fun `Given Konfig with axis targeting, When serialized and round-tripped, Then axes constraints are preserved`() {
        TestFeatures.boolFlag.update(false) {
            rule(true) {
                this.axis(Environment.PROD, Environment.STAGE)
            }
        }

        val json = SnapshotSerializer.serialize(TestFeatures.configuration)
        assertTrue(json.contains("\"axes\""))
        assertTrue(json.contains("\"snapshot-environment\""))
        assertTrue(json.contains("prod"))
        assertTrue(json.contains("stage"))

        val parsed = SnapshotSerializer.fromJson(json).getOrThrow()

        TestFeatures.load(Configuration(emptyMap()))
        TestFeatures.load(parsed)

        assertTrue(TestFeatures.boolFlag.evaluate(ctxWithEnvironment(Environment.PROD)))
        assertTrue(TestFeatures.boolFlag.evaluate(ctxWithEnvironment(Environment.STAGE)))
        assertFalse(TestFeatures.boolFlag.evaluate(ctxWithEnvironment(Environment.DEV)))
    }

    @Test
    fun `Given maximal snapshot, When serialized, Then output includes all supported fields`() {
        val flagAllowlistA = StableId.of("allowlisted-user") // 616c6c6f776c69737465642d75736572
        val flagAllowlistB = StableId.of("flag-allowlist") // 666c61672d616c6c6f776c697374
        val ruleAllowlist = StableId.of("rule-allowlist") // 72756c652d616c6c6f776c697374

        TestFeatures.themeFlag.update(Theme.LIGHT) {
            salt("salt-v2")
            active { false }
            allowlist(flagAllowlistA, flagAllowlistB)
            rule(Theme.DARK) {
                allowlist(ruleAllowlist)
                note("maximal-rule")
                locales(AppLocale.UNITED_STATES, AppLocale.FRANCE)
                platforms(Platform.IOS, Platform.ANDROID)
                versions { min(1, 0, 0); max(2, 0, 0) }
                axis(Environment.PROD, Environment.STAGE)
                axis(Tenant.ENTERPRISE)
                rampUp { 12.34 }
            }
        }

        TestFeatures.retryPolicyFlag.update(RetryPolicy()) {
            salt("policy-salt")
            allowlist(flagAllowlistA)
            rule(RetryPolicy(maxAttempts = 9, backoffMs = 2500.0, enabled = false, mode = "linear")) {
                note("policy-rule")
                rampUp { 99.0 }
            }
        }

        val config =
            TestFeatures.configuration.withMetadata(
                version = "rev-123",
                generatedAtEpochMillis = 1734480000000,
                source = "s3://configs/global.json",
            )

        val json = SnapshotSerializer.serialize(config)
        println(json)

        val normalized =
            json.replace(namespaceSeedRegex, "feature::NAMESPACE::")

        val expected =
            """
            {
              "meta": {
                "version": "rev-123",
                "generatedAtEpochMillis": 1734480000000,
                "source": "s3://configs/global.json"
              },
              "flags": [
                {
                  "key": "feature::NAMESPACE::themeFlag",
                  "defaultValue": {
                    "type": "ENUM",
                    "value": "LIGHT",
                    "enumClassName": "${Theme::class.java.name}"
                  },
                  "salt": "salt-v2",
                  "isActive": false,
                  "rampUpAllowlist": [
                    "616c6c6f776c69737465642d75736572",
                    "666c61672d616c6c6f776c697374"
                  ],
                  "rules": [
                    {
                      "value": {
                        "type": "ENUM",
                        "value": "DARK",
                        "enumClassName": "${Theme::class.java.name}"
                      },
                      "rampUp": 12.34,
                      "rampUpAllowlist": [
                        "72756c652d616c6c6f776c697374"
                      ],
                      "note": "maximal-rule",
                      "locales": [
                        "UNITED_STATES",
                        "FRANCE"
                      ],
                      "platforms": [
                        "IOS",
                        "ANDROID"
                      ],
                      "versionRange": {
                        "type": "MIN_AND_MAX_BOUND",
                        "min": {
                          "major": 1,
                          "minor": 0,
                          "patch": 0
                        },
                        "max": {
                          "major": 2,
                          "minor": 0,
                          "patch": 0
                        }
                      },
                      "axes": {
                        "snapshot-environment": [
                          "prod",
                          "stage"
                        ],
                        "snapshot-tenant": [
                          "enterprise"
                        ]
                      }
                    }
                  ]
                },
                {
                  "key": "feature::NAMESPACE::retryPolicyFlag",
                  "defaultValue": {
                    "type": "DATA_CLASS",
                    "dataClassName": "${RetryPolicy::class.java.name}",
                    "value": {
                      "maxAttempts": 3.0,
                      "backoffMs": 1000.0,
                      "enabled": true,
                      "mode": "exponential"
                    }
                  },
                  "salt": "policy-salt",
                  "isActive": true,
                  "rampUpAllowlist": [
                    "616c6c6f776c69737465642d75736572"
                  ],
                  "rules": [
                    {
                      "value": {
                        "type": "DATA_CLASS",
                        "dataClassName": "${RetryPolicy::class.java.name}",
                        "value": {
                          "maxAttempts": 9.0,
                          "backoffMs": 2500.0,
                          "enabled": false,
                          "mode": "linear"
                        }
                      },
                      "rampUp": 99.0,
                      "rampUpAllowlist": [],
                      "note": "policy-rule",
                      "locales": [],
                      "platforms": [],
                      "versionRange": {
                        "type": "UNBOUNDED"
                      },
                      "axes": {}
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        assertEquals(expected, normalized)
    }

    @Test
    fun `Given Konfig with multiple flags, When serialized, Then includes all flags`() {
        val boolFlag = FlagDefinition(feature = TestFeatures.boolFlag, defaultValue = true)
        val stringFlag = FlagDefinition(feature = TestFeatures.stringFlag, defaultValue = "test")
        val intFlag = FlagDefinition(feature = TestFeatures.intFlag, defaultValue = 10)

        val configuration =
            Configuration(
                mapOf(
                    TestFeatures.boolFlag to boolFlag,
                    TestFeatures.stringFlag to stringFlag,
                    TestFeatures.intFlag to intFlag,
                ),
            )

        val json = SnapshotSerializer.serialize(configuration)

        assertNotNull(json)
        assertTrue(json.contains(TestFeatures.boolFlag.id.toString()))
        assertTrue(json.contains(TestFeatures.stringFlag.id.toString()))
        assertTrue(json.contains(TestFeatures.intFlag.id.toString()))
    }

    companion object {
        private val namespaceSeedRegex = Regex("feature::[a-f0-9\\-]+::")
    }

    // ========== Deserialization Tests ==========

    @Test
    fun `Given valid JSON with empty flags, When deserialized, Then returns success with empty Konfig`() {
        val json =
            """
            {
              "flags" : []
            }
            """.trimIndent()

        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        assertEquals(0, result.value.flags.size)
    }

    @Test
    fun `Given valid JSON with boolean flag, When deserialized, Then returns success with correct flag`() {
        val json =
            """
            {
              "flags" : [
                {
                  "key" : "${TestFeatures.boolFlag.id}",
                  "defaultValue" : {
                    "type" : "BOOLEAN",
                    "value" : true
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                }
              ]
            }
            """.trimIndent()

        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val konfig = result.value
        assertEquals(1, konfig.flags.size)

        val flag = konfig.flags[TestFeatures.boolFlag]
        assertNotNull(flag)
        assertEquals(true, flag.defaultValue)
    }

    @Test
    fun `Given valid JSON with string flag, When deserialized, Then returns success with correct flag`() {
        val json =
            """
            {
              "flags" : [
                {
                  "key" : "${TestFeatures.stringFlag.id}",
                  "defaultValue" : {
                    "type" : "STRING",
                    "value" : "test-value"
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                }
              ]
            }
            """.trimIndent()

        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val flag = result.value.flags[TestFeatures.stringFlag]
        assertNotNull(flag)
        assertEquals("test-value", flag.defaultValue)
    }

    @Test
    fun `Given valid JSON with int flag, When deserialized, Then returns success with correct flag`() {
        val json =
            """
            {
              "flags" : [
                {
                  "key" : "${TestFeatures.intFlag.id}",
                  "defaultValue" : {
                    "type" : "INT",
                    "value" : 42
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                }
              ]
            }
            """.trimIndent()

        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val flag = result.value.flags[TestFeatures.intFlag]
        assertNotNull(flag)
        assertEquals(42, flag.defaultValue)
    }

    @Test
    fun `Given valid JSON with double flag, When deserialized, Then returns success with correct flag`() {
        val json =
            """
            {
              "flags" : [
                {
                  "key" : "${TestFeatures.doubleFlag.id}",
                  "defaultValue" : {
                    "type" : "DOUBLE",
                    "value" : 3.14
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                }
              ]
            }
            """.trimIndent()

        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val flag = result.value.flags[TestFeatures.doubleFlag]
        assertNotNull(flag)
        assertEquals(3.14, flag.defaultValue)
    }

    @Test
    fun `Given JSON with complex rule, When deserialized, Then returns success with all rule attributes`() {
        val json =
            """
            {
              "flags" : [
                {
                  "key" : "${TestFeatures.boolFlag.id}",
                  "defaultValue" : {
                    "type" : "BOOLEAN",
                    "value" : false
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : [
                    {
                      "value" : {
                        "type" : "BOOLEAN",
                        "value" : true
                      },
                      "rampUp" : 50.0,
                      "note" : "TestNamespace rule",
                      "locales" : ["UNITED_STATES", "FRANCE"],
                      "platforms" : ["IOS", "ANDROID"],
                      "versionRange" : {
                        "type" : "MIN_AND_MAX_BOUND",
                        "min" : {
                          "major" : 1,
                          "minor" : 0,
                          "patch" : 0
                        },
                        "max" : {
                          "major" : 2,
                          "minor" : 0,
                          "patch" : 0
                        }
                      }
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val flag = result.value.flags[TestFeatures.boolFlag]
        assertNotNull(flag)
        assertEquals(1, flag.values.size)

        val rule = flag.values.first().rule
        assertEquals(50.0, rule.rampUp.value)
        assertEquals("TestNamespace rule", rule.note)
        assertEquals(
            setOf(AppLocale.UNITED_STATES.id, AppLocale.FRANCE.id),
            rule.targeting.locales,
        )
        assertEquals(
            setOf(Platform.IOS.id, Platform.ANDROID.id),
            rule.targeting.platforms,
        )
        assertIs<FullyBound>(rule.targeting.versionRange)
    }

    @Test
    fun `Given invalid JSON, When deserialized, Then returns failure with InvalidJson error`() {
        val json = "not valid json at all"

        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.InvalidJson>(result.error)
    }

    @Test
    fun `Given JSON with unregistered feature, When deserialized, Then returns failure with FeatureNotFound error`() {
        val json =
            """
            {
              "flags" : [
                {
                  "key" : "feature::global::unregistered_feature",
                  "defaultValue" : {
                    "type" : "BOOLEAN",
                    "value" : true
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                }
              ]
            }
            """.trimIndent()

        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.FeatureNotFound>(result.error)
        assertEquals(FeatureId.create("global", "unregistered_feature"), result.error.key)
    }

    // ========== Round-Trip Tests ==========

    @Test
    fun `Given boolean flag, When round-tripped, Then deserialized value equals original`() {
        val originalFlag = FlagDefinition(feature = TestFeatures.boolFlag, defaultValue = true)
        val originalConfiguration = Configuration(mapOf(TestFeatures.boolFlag to originalFlag))

        val json = SnapshotSerializer.serialize(originalConfiguration)
        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedFlag = result.value.flags[TestFeatures.boolFlag]
        assertNotNull(deserializedFlag)
        assertEquals(originalFlag.defaultValue, deserializedFlag.defaultValue)
        assertEquals(originalFlag.salt, deserializedFlag.salt)
        assertEquals(originalFlag.isActive, deserializedFlag.isActive)
    }

    @Test
    fun `Given string flag, When round-tripped, Then deserialized value equals original`() {
        val originalFlag = FlagDefinition(feature = TestFeatures.stringFlag, defaultValue = "test-value")
        val originalConfiguration = Configuration(mapOf(TestFeatures.stringFlag to originalFlag))

        val json = SnapshotSerializer.serialize(originalConfiguration)
        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedFlag = result.value.flags[TestFeatures.stringFlag]
        assertNotNull(deserializedFlag)
        assertEquals(originalFlag.defaultValue, deserializedFlag.defaultValue)
    }

    @Test
    fun `Given int flag, When round-tripped, Then deserialized value equals original`() {
        val originalFlag = FlagDefinition(feature = TestFeatures.intFlag, defaultValue = 42)
        val originalConfiguration = Configuration(mapOf(TestFeatures.intFlag to originalFlag))

        val json = SnapshotSerializer.serialize(originalConfiguration)
        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedFlag = result.value.flags[TestFeatures.intFlag]
        assertNotNull(deserializedFlag)
        assertEquals(originalFlag.defaultValue, deserializedFlag.defaultValue)
    }

    @Test
    fun `Given double flag, When round-tripped, Then deserialized value equals original`() {
        val originalFlag = FlagDefinition(feature = TestFeatures.doubleFlag, defaultValue = 3.14)
        val originalConfiguration = Configuration(mapOf(TestFeatures.doubleFlag to originalFlag))

        val json = SnapshotSerializer.serialize(originalConfiguration)
        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedFlag = result.value.flags[TestFeatures.doubleFlag]
        assertNotNull(deserializedFlag)
        assertEquals(originalFlag.defaultValue, deserializedFlag.defaultValue)
    }

    @Test
    fun `Given flag with complex rules, When round-tripped, Then all rule attributes are preserved`() {
        val rule =
            Rule<Context>(
                rampUp = RampUp.of(75.0),
                note = "Complex rule",
                locales = localeIds(AppLocale.UNITED_STATES, AppLocale.UNITED_STATES),
                platforms = platformIds(Platform.WEB),
                versionRange = FullyBound(Version(2, 0, 0), Version(3, 0, 0)),
            )

        val originalFlag =
            FlagDefinition(
                feature = TestFeatures.boolFlag,
                bounds = listOf(rule.targetedBy(true)),
                defaultValue = false,
            )
        val originalConfiguration = Configuration(mapOf(TestFeatures.boolFlag to originalFlag))

        val json = SnapshotSerializer.serialize(originalConfiguration)
        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedFlag = result.value.flags[TestFeatures.boolFlag]
        assertNotNull(deserializedFlag)
        assertEquals(1, deserializedFlag.values.size)

        val deserializedRule = deserializedFlag.values.first().rule
        assertEquals(75.0, deserializedRule.rampUp.value)
        assertEquals("Complex rule", deserializedRule.note)
        assertEquals(
            setOf(AppLocale.UNITED_STATES.id, AppLocale.UNITED_STATES.id),
            deserializedRule.targeting.locales,
        )
        assertEquals(
            setOf(Platform.WEB.id),
            deserializedRule.targeting.platforms,
        )

        val versionRange = deserializedRule.targeting.versionRange
        assertIs<FullyBound>(versionRange)
        assertEquals(Version(2, 0, 0), versionRange.min)
        assertEquals(Version(3, 0, 0), versionRange.max)
    }

    @Test
    fun `Given multiple flags, When round-tripped, Then all flags are preserved`() {
        val boolFlag = FlagDefinition(feature = TestFeatures.boolFlag, defaultValue = true)
        val stringFlag = FlagDefinition(feature = TestFeatures.stringFlag, defaultValue = "test")
        val intFlag = FlagDefinition(feature = TestFeatures.intFlag, defaultValue = 10)
        val doubleFlag = FlagDefinition(feature = TestFeatures.doubleFlag, defaultValue = 2.5)

        val originalConfiguration =
            Configuration(
                mapOf(
                    TestFeatures.boolFlag to boolFlag,
                    TestFeatures.stringFlag to stringFlag,
                    TestFeatures.intFlag to intFlag,
                    TestFeatures.doubleFlag to doubleFlag,
                ),
            )

        val json = SnapshotSerializer.serialize(originalConfiguration)
        val result = SnapshotSerializer.fromJson(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedKonfig = result.value
        assertEquals(4, deserializedKonfig.flags.size)

        assertNotNull(deserializedKonfig.flags[TestFeatures.boolFlag])
        assertNotNull(deserializedKonfig.flags[TestFeatures.stringFlag])
        assertNotNull(deserializedKonfig.flags[TestFeatures.intFlag])
        assertNotNull(deserializedKonfig.flags[TestFeatures.doubleFlag])
    }

    // ========== Patch Tests ==========

    @Test
    fun `Given patch with new flag, When applied, Then new flag is added to konfig`() {
        val originalConfiguration = Configuration(emptyMap())

        val newFlagJson =
            """
            {
              "key" : "${TestFeatures.boolFlag.id}",
              "defaultValue" : {
                "type" : "BOOLEAN",
                "value" : true
              },
              "salt" : "v1",
              "isActive" : true,
              "rules" : []
            }
            """.trimIndent()

        val patchJson =
            """
            {
              "flags" : [$newFlagJson],
              "removeKeys" : []
            }
            """.trimIndent()

        val result = SnapshotSerializer.applyPatchJson(originalConfiguration, patchJson)

        assertIs<ParseResult.Success<Configuration>>(result)
        val patchedKonfig = result.value
        assertEquals(1, patchedKonfig.flags.size)
        assertNotNull(patchedKonfig.flags[TestFeatures.boolFlag])
    }

    @Test
    fun `Given patch with updated flag, When applied, Then flag is updated in konfig`() {
        val originalFlag = FlagDefinition(feature = TestFeatures.boolFlag, defaultValue = false)
        val originalConfiguration = Configuration(mapOf(TestFeatures.boolFlag to originalFlag))

        val updatedFlagJson =
            """
            {
              "key" : "${TestFeatures.boolFlag.id}",
              "defaultValue" : {
                "type" : "BOOLEAN",
                "value" : true
              },
              "salt" : "v2",
              "isActive" : true,
              "rules" : []
            }
            """.trimIndent()

        val patchJson =
            """
            {
              "flags" : [$updatedFlagJson],
              "removeKeys" : []
            }
            """.trimIndent()

        val result = SnapshotSerializer.applyPatchJson(originalConfiguration, patchJson)

        assertIs<ParseResult.Success<Configuration>>(result)
        val patchedFlag = result.value.flags[TestFeatures.boolFlag]
        assertNotNull(patchedFlag)
        assertEquals(true, patchedFlag.defaultValue)
        assertEquals("v2", patchedFlag.salt)
    }

    @Test
    fun `Given patch with remove key, When applied, Then flag is removed from konfig`() {
        val originalFlag = FlagDefinition(feature = TestFeatures.boolFlag, defaultValue = true)
        val originalConfiguration = Configuration(mapOf(TestFeatures.boolFlag to originalFlag))

        val patchJson =
            """
            {
              "flags" : [],
              "removeKeys" : ["${TestFeatures.boolFlag.id}"]
            }
            """.trimIndent()

        val result = SnapshotSerializer.applyPatchJson(originalConfiguration, patchJson)

        assertIs<ParseResult.Success<Configuration>>(result)
        val patchedKonfig = result.value
        assertEquals(0, patchedKonfig.flags.size)
    }

    @Test
    fun `Given patch with multiple operations, When applied, Then all operations are executed`() {
        val existingFlag = FlagDefinition(feature = TestFeatures.boolFlag, defaultValue = false)
        val toRemoveFlag = FlagDefinition(feature = TestFeatures.stringFlag, defaultValue = "remove-me")
        val originalConfiguration =
            Configuration(
                mapOf(
                    TestFeatures.boolFlag to existingFlag,
                    TestFeatures.stringFlag to toRemoveFlag,
                ),
            )

        val patchJson =
            """
            {
              "flags" : [
                {
                  "key" : "${TestFeatures.boolFlag.id}",
                  "defaultValue" : {
                    "type" : "BOOLEAN",
                    "value" : true
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                },
                {
                  "key" : "${TestFeatures.intFlag.id}",
                  "defaultValue" : {
                    "type" : "INT",
                    "value" : 100
                  },
                  "salt" : "v1",
                  "isActive" : true,
                  "rules" : []
                }
              ],
              "removeKeys" : ["${TestFeatures.stringFlag.id}"]
            }
            """.trimIndent()

        val result = SnapshotSerializer.applyPatchJson(originalConfiguration, patchJson)

        assertIs<ParseResult.Success<Configuration>>(result)
        val patchedKonfig = result.value

        // Updated flag
        val updatedFlag = patchedKonfig.flags[TestFeatures.boolFlag]
        assertNotNull(updatedFlag)
        assertEquals(true, updatedFlag.defaultValue)

        // New flag
        val newFlag = patchedKonfig.flags[TestFeatures.intFlag]
        assertNotNull(newFlag)
        assertEquals(100, newFlag.defaultValue)

        // Removed flag
        assertEquals(null, patchedKonfig.flags[TestFeatures.stringFlag])

        // Total count
        assertEquals(2, patchedKonfig.flags.size)
    }

    @Test
    fun `Given invalid patch JSON, When applied, Then returns failure`() {
        val originalConfiguration = Configuration(emptyMap())
        val invalidPatchJson = "not valid json"

        val result = SnapshotSerializer.applyPatchJson(originalConfiguration, invalidPatchJson)

        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.InvalidJson>(result.error)
    }

    @Test
    fun `Given patch deserialization, When valid, Then returns SerializablePatch`() {
        val patchJson =
            """
            {
              "flags" : [],
              "removeKeys" : ["feature::global::test_key"]
            }
            """.trimIndent()

        val result = SnapshotSerializer.fromJsonPatch(patchJson)

        assertIs<ParseResult.Success<SerializablePatch>>(result)
        assertEquals(0, result.value.flags.size)
        assertEquals(listOf(FeatureId.create("global", "test_key")), result.value.removeKeys)
    }

    @Test
    fun `Given direct patch application, When valid, Then applies patch correctly`() {
        val originalConfiguration = Configuration(emptyMap())

        val patchJson =
            """
            {
              "flags" : [],
              "removeKeys" : []
            }
            """.trimIndent()

        val patchResult = SnapshotSerializer.fromJsonPatch(patchJson)
        assertIs<ParseResult.Success<SerializablePatch>>(patchResult)

        val result =
            SnapshotSerializer.applyPatch(originalConfiguration, patchResult.value, SnapshotLoadOptions.strict())

        assertIs<ParseResult.Success<Configuration>>(result)
    }
}
