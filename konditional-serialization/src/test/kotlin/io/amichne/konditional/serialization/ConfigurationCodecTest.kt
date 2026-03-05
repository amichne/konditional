@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.serialization

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.axis.axes
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.rules.targeting.scopes.constrain
import io.amichne.konditional.core.dsl.rules.targeting.scopes.whenContext
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.schema.CompiledNamespaceSchema
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
import io.amichne.konditional.fixtures.serializers.RetryPolicy
import io.amichne.konditional.fixtures.utilities.update
import io.amichne.konditional.runtime.json
import io.amichne.konditional.runtime.update
import io.amichne.konditional.rules.predicate.PredicateRef
import io.amichne.konditional.serialization.instance.Configuration
import io.amichne.konditional.serialization.options.SnapshotLoadOptions
import io.amichne.konditional.serialization.snapshot.ConfigurationCodec
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader
import io.amichne.konditional.values.FeatureId
import io.amichne.konditional.values.NamespaceId
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
@Suppress("LargeClass")
private sealed interface ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>

    data class Failure(val error: ParseError) : ParseResult<Nothing>

    companion object {
        fun <T> success(value: T): ParseResult<T> = Success(value)

        fun failure(error: ParseError): ParseResult<Nothing> = Failure(error)
    }
}

private fun <T> ParseResult<T>.getOrThrow(): T =
    when (this) {
        is ParseResult.Success -> value
        is ParseResult.Failure -> throw IllegalStateException(error.message)
    }

@Suppress("LargeClass")
class ConfigurationCodecTest {
    private object TestFeatures : Namespace.TestNamespaceFacade("snapshot-serializer") {
        val boolFlag by boolean<Context>(default = false)
        val iosOnly by predicate<Context> {
            (this as? Context.PlatformContext)?.platform == Platform.IOS
        }
        val stringFlag by string<Context>(default = "default")
        val intFlag by integer<Context>(default = 0)
        val doubleFlag by double<Context>(default = 0.0)
        val themeFlag by enum<Theme, Context>(default = Theme.LIGHT)
        val retryPolicyFlag by custom<RetryPolicy, Context>(default = RetryPolicy())
        val dynamicDependencyFlag by string<Context>(default = "dep-default") {
            rule { android() } yields "dep-android"
            rule { always() } yields "dep-catch-all"
        }
        val dynamicYieldFlag by string<Context>(default = "dynamic-default") {
            rule { android() } yields { dynamicDependencyFlag.evaluate() }
            rule { always() } yields "fallback"
        }
    }

    @BeforeEach
    fun setup() {
        // Reset the namespace registry before each test.
        loadMaterialized(declaredDefaultConfiguration())
    }

    private fun loadMaterialized(configuration: Configuration) {
        TestFeatures.update(configuration)
    }

    private fun declaredDefaultConfiguration(): Configuration {
        val schema = CompiledNamespaceSchema.from(TestFeatures)
        val flags = schema.entriesById.values.associate { entry -> entry.feature to entry.declaredDefinition }
        return Configuration(flags)
    }

    private fun decodeFeatureAware(
        json: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.fillMissingDeclaredFlags(),
    ): ParseResult<Configuration> =
        ConfigurationCodec
            .decode(
                json = json,
                namespace = TestFeatures,
                options = options,
            ).toParseResult()

    private fun applyPatchFeatureAware(
        currentConfiguration: Configuration,
        patchJson: String,
        options: SnapshotLoadOptions = SnapshotLoadOptions.fillMissingDeclaredFlags(),
    ): ParseResult<Configuration> =
        ConfigurationCodec
            .patch(
                current = currentConfiguration,
                patchJson = patchJson,
                namespace = TestFeatures,
                options = options,
            ).toParseResult()

    private fun Result<Configuration>.toParseResult(): ParseResult<Configuration> =
        fold(
            onSuccess = { configuration -> ParseResult.success(configuration) },
            onFailure = { error ->
                val parseError =
                    (error as? KonditionalBoundaryFailure)?.parseError
                        ?: ParseError.invalidSnapshot(error.message ?: "Unknown decode failure")
                ParseResult.failure(parseError)
            },
        )

    private enum class Theme {
        LIGHT,
        DARK,
    }

    @Test
    fun `Given feature-aware decode context, When decoded, Then decode succeeds`() {
        TestFeatures.boolFlag.update(true) {}

        val result = decodeFeatureAware(json = TestFeatures.json)

        assertIs<ParseResult.Success<Configuration>>(result)
        assertTrue(result.value.flags.containsKey(TestFeatures.boolFlag))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `Given forged enum class name in payload, When decoding feature-aware, Then trusted feature type is used`() {
        TestFeatures.themeFlag.update(Theme.DARK) {}
        val json = TestFeatures.json
            .replace(Theme::class.java.name, "evil.payload.FakeEnum")

        val result = decodeFeatureAware(json = json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val decoded = result.value.flags[TestFeatures.themeFlag] as FlagDefinition<Theme, Context, Namespace>
        assertEquals(Theme.DARK, decoded.defaultValue)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `Given forged data class name in payload, When decoding feature-aware, Then trusted feature type is used`() {
        val expected = RetryPolicy(maxAttempts = 9, backoffMs = 1500.0, enabled = false, mode = "linear")
        TestFeatures.retryPolicyFlag.update(expected) {}
        val json = TestFeatures.json.replace(RetryPolicy::class.java.name, "evil.payload.FakePolicy")

        val result = decodeFeatureAware(json = json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val decoded =
            result.value.flags[TestFeatures.retryPolicyFlag] as FlagDefinition<RetryPolicy, Context, Namespace>
        assertEquals(expected, decoded.defaultValue)
    }

    private fun ctx(
        idHex: String,
        locale: AppLocale = AppLocale.UNITED_STATES,
        platform: Platform = Platform.IOS,
        version: String = "1.0.0",
    ) = Context(locale, platform, Version.parse(version).getOrThrow(), StableId.of(idHex))

    private fun ctxWithTestEnvironment(env: TestEnvironment = TestEnvironment.PROD): Context = TestContext(
        locale = AppLocale.UNITED_STATES,
        platform = Platform.IOS,
        appVersion = Version.of(1, 0, 0),
        stableId = StableId.of("axis-user"),
        axes = axes(env)
    )

    private fun boolFlagSnapshotWithPredicateRef(
        namespaceId: NamespaceId,
        predicateId: String,
    ): String =
        """
        {
          "flags": [
            {
              "key": "${TestFeatures.boolFlag.id}",
              "defaultValue": {
                "type": "BOOLEAN",
                "value": false
              },
              "rules": [
                {
                  "value": {
                    "type": "BOOLEAN",
                    "value": true
                  },
                  "type": "STATIC",
                   "predicateRefs": [
                     {
                       "type": "REGISTERED",
                       "namespaceId": "${namespaceId.value}",
                       "id": "$predicateId"
                     }
                   ]
                 }
              ]
            }
          ]
        }
        """.trimIndent()

    @Test
    fun `Given deferred yields rule, When serialized, Then snapshot encodes placeholder instead of failing`() {
        val json = TestFeatures.json

        assertTrue(json.contains("\"type\": \"CONTEXTUAL\""))
    }

    @Test
    fun `Given deferred yields rule snapshot, When decoded and re-encoded, Then contextual type remains contextual`() {
        val encoded = TestFeatures.json
        NamespaceSnapshotLoader.forNamespace(TestFeatures).load(encoded).getOrThrow()
        val reEncoded = TestFeatures.json
        assertTrue(reEncoded.contains("\"type\": \"CONTEXTUAL\""))
    }

    @Test
    fun `Given deferred yields rule snapshot, When decoded, Then rule uses declared default placeholder`() {
        val json = TestFeatures.json

        val decoded = decodeFeatureAware(json = json).getOrThrow()

        @Suppress("UNCHECKED_CAST")
        val definition = decoded.flags[TestFeatures.dynamicYieldFlag] as FlagDefinition<String, Context, Namespace>
        assertEquals("dynamic-default", definition.values.first().value)
    }

    // ========== Serialization Tests ==========

    @Test
    fun `Given empty Konfig, When serialized, Then produces valid JSON with empty flags array`() {
        val configuration = Configuration(emptyMap())

        val json = ConfigurationCodec.encode(configuration)

        assertNotNull(json)
        assertTrue(json.contains("\"flags\""))
        assertTrue(json.contains("[]"))
    }

    @Test
    fun `Given Konfig with boolean flag, When serialized, Then includes flag with correct type`() {
        TestFeatures.boolFlag.update(true) {}

        val json = TestFeatures.json

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

        val json = ConfigurationCodec.encode(configuration)

        assertNotNull(json)
        assertTrue(json.contains("\"key\": \"${TestFeatures.stringFlag.id}\""))
        assertTrue(json.contains("\"type\": \"STRING\""))
        assertTrue(json.contains("\"value\": \"test-value\""))
    }

    @Test
    fun `Given DSL rule with predicate ref, When serialized, Then snapshot includes predicateRefs`() {
        TestFeatures.boolFlag.update(default = false) {
            rule(true) {
                require(TestFeatures.iosOnly)
            }
        }

        val json = TestFeatures.json

        assertTrue(json.contains("\"predicateRefs\""))
        assertTrue(json.contains("\"id\": \"${TestFeatures.iosOnly.id.value}\""))
        assertTrue(json.contains("\"namespaceId\": \"${TestFeatures.iosOnly.namespaceId.value}\""))
    }

    @Test
    fun `Given mixed named and anonymous require predicates, When round-tripped, Then both remain enforced`() {
        TestFeatures.boolFlag.update(default = false) {
            rule(true) {
                require(TestFeatures.iosOnly)
                require {
                    whenContext<Context> { axes.isEmpty() }
                }
                require {
                    whenContext<Context.LocaleContext> { locale == AppLocale.UNITED_STATES }
                }
            }
        }

        val json = TestFeatures.json
        assertTrue(json.contains("\"ruleId\""))
        assertTrue(json.contains("\"predicateRefs\""))
        assertFalse(json.contains("__inline_require__"))
        assertFalse(
            TestFeatures.boolFlag.evaluate(
                ctx(
                    "dddddddddddddddddddddddddddddddd",
                    platform = Platform.IOS,
                    locale = AppLocale.FRANCE,
                ),
            ),
        )

        val decoded = decodeFeatureAware(json)
        assertIs<ParseResult.Success<Configuration>>(decoded)
        loadMaterialized(decoded.value)

        assertTrue(
            TestFeatures.boolFlag.evaluate(
                ctx(
                    "cccccccccccccccccccccccccccccccc",
                    platform = Platform.IOS,
                    locale = AppLocale.UNITED_STATES,
                ),
            ),
        )
        assertFalse(
            TestFeatures.boolFlag.evaluate(
                ctxWithTestEnvironment(TestEnvironment.PROD),
            ),
        )
        val reEncoded = ConfigurationCodec.encode(decoded.value)
        assertTrue(reEncoded.contains("\"ruleId\""))
        assertTrue(reEncoded.contains("\"predicateRefs\""))
        assertFalse(reEncoded.contains("__inline_require__"))
    }

    @Test
    fun `Given Konfig with int flag, When serialized, Then includes flag with correct type`() {
        TestFeatures.intFlag.update(42) {}
        val json = TestFeatures.json

        assertNotNull(json)
        println(json)
        assertTrue(json.contains("\"key\": \"${TestFeatures.intFlag.id}\""))
        assertTrue(json.contains("\"type\": \"INT\""))
        assertTrue(json.contains("\"value\": 42"))
    }

    @Test
    fun `Given Konfig with double flag, When serialized, Then includes flag with correct type`() {
        TestFeatures.doubleFlag.update(3.14) {}

        val json = TestFeatures.json

        assertNotNull(json)
        assertTrue(json.contains("\"key\": \"${TestFeatures.doubleFlag.id}\""))
        assertTrue(json.contains("\"type\": \"DOUBLE\""))
        assertTrue(json.contains("\"value\": 3.14"))
    }

    @Test
    fun `Given Konfig with complex rules, When serialized, Then includes all rule attributes`() {
        TestFeatures.boolFlag.update(false) {
            enable {
                rampUp { 50.0 }
                note("TestNamespace rule")
                locales(AppLocale.UNITED_STATES, AppLocale.FRANCE)
                platforms(Platform.IOS, Platform.ANDROID)
                versions {
                    min(1, 0, 0)
                    max(2, 0, 0)
                }
            }
        }

        val json = TestFeatures.json

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
            enable {
                rampUp { 0.0 }
            }
        }

        assertTrue(TestFeatures.boolFlag.evaluate(ctx(allowlistedId)))
        assertFalse(TestFeatures.boolFlag.evaluate(ctx(otherId)))

        val json = TestFeatures.json
        val parsed = decodeFeatureAware(json).getOrThrow()

        loadMaterialized(Configuration(emptyMap()))
        loadMaterialized(parsed)

        assertTrue(TestFeatures.boolFlag.evaluate(ctx(allowlistedId)))
        assertFalse(TestFeatures.boolFlag.evaluate(ctx(otherId)))
    }

    @Test
    fun `Given Konfig with axis targeting, When serialized and round-tripped, Then axes constraints are preserved`() {
        TestFeatures.boolFlag.update(false) {
            enable {
                constrain(TestEnvironment.PROD, TestEnvironment.STAGE)
            }
        }

        val json = TestFeatures.json
        assertTrue(json.contains("\"axes\""))
        assertTrue(json.contains("\"${TestEnvironment::class.java.name}\""))
        assertTrue(json.contains("prod"))
        assertTrue(json.contains("stage"))

        val parsed = decodeFeatureAware(json).getOrThrow()

        loadMaterialized(Configuration(emptyMap()))
        loadMaterialized(parsed)

        assertTrue(TestFeatures.boolFlag.evaluate(ctxWithTestEnvironment(TestEnvironment.PROD)))
        assertTrue(TestFeatures.boolFlag.evaluate(ctxWithTestEnvironment(TestEnvironment.STAGE)))
        assertFalse(TestFeatures.boolFlag.evaluate(ctxWithTestEnvironment(TestEnvironment.DEV)))
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
                constrain(TestEnvironment.PROD, TestEnvironment.STAGE)
                constrain(TestTenant.ENTERPRISE)
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

        val current = TestFeatures.configuration
        val config =
            Configuration(
                flags = mapOf(
                    TestFeatures.themeFlag to checkNotNull(current.flags[TestFeatures.themeFlag]),
                    TestFeatures.retryPolicyFlag to checkNotNull(current.flags[TestFeatures.retryPolicyFlag]),
                ),
                metadata = (current as Configuration).metadata.copy(
                    version = "rev-123",
                    generatedAtEpochMillis = 1734480000000,
                    source = "s3://configs/global.json",
                ),
            )

        val json = ConfigurationCodec.encode(config)
        println(json)

        val normalized =
            json
                .replace(namespaceSeedRegex, "feature::NAMESPACE::")
                .replace(Regex("\"ruleId\": \"[^\"]+\""), "\"ruleId\": \"RULE_ID\"")

        val expected = maximalExpectedJson

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

        val json = ConfigurationCodec.encode(configuration)

        assertNotNull(json)
        assertTrue(json.contains(TestFeatures.boolFlag.id.toString()))
        assertTrue(json.contains(TestFeatures.stringFlag.id.toString()))
        assertTrue(json.contains(TestFeatures.intFlag.id.toString()))
    }

    companion object {
        private val namespaceSeedRegex = Regex("feature::[^:]+::")

        private val maximalExpectedJson: String =
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
                      "type": "STATIC",
                      "ruleId": "RULE_ID",
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
                        "${TestEnvironment::class.java.name}": [
                          "prod",
                          "stage"
                        ],
                        "${TestTenant::class.java.name}": [
                          "enterprise"
                        ]
                      },
                      "predicateRefs": []
                    }
                  ]
                },
                {
                  "key": "feature::NAMESPACE::retryPolicyFlag",
                  "defaultValue": {
                    "type": "DATA_CLASS",
                    "dataClassName": "${RetryPolicy::class.java.name}",
                    "value": {
                      "maxAttempts": 3,
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
                          "maxAttempts": 9,
                          "backoffMs": 2500.0,
                          "enabled": false,
                          "mode": "linear"
                        }
                      },
                      "type": "STATIC",
                      "ruleId": "RULE_ID",
                      "rampUp": 99.0,
                      "rampUpAllowlist": [],
                      "note": "policy-rule",
                      "locales": [],
                      "platforms": [],
                      "versionRange": {
                        "type": "UNBOUNDED"
                      },
                      "axes": {},
                      "predicateRefs": []
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        private val complexRuleJson: String =
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
                        "min" : { "major" : 1, "minor" : 0, "patch" : 0 },
                        "max" : { "major" : 2, "minor" : 0, "patch" : 0 }
                      }
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
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

        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        assertEquals(TestFeatures.allFeatures().size, result.value.flags.size)
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

        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val konfig = result.value
        assertEquals(TestFeatures.allFeatures().size, konfig.flags.size)

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

        val result = decodeFeatureAware(json)

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

        val result = decodeFeatureAware(json)

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

        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val flag = result.value.flags[TestFeatures.doubleFlag]
        assertNotNull(flag)
        assertEquals(3.14, flag.defaultValue)
    }

    @Test
    fun `Given JSON with complex rule, When deserialized, Then returns success with all rule attributes`() {
        val json = complexRuleJson

        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val flag = result.value.flags[TestFeatures.boolFlag]
        assertNotNull(flag)
        assertEquals(1, flag.values.size)

        val rule = flag.values.first().rule
        assertEquals(50.0, rule.rampUp.value)
        assertEquals("TestNamespace rule", rule.note)
        val encoded = ConfigurationCodec.encode(result.value)
        assertTrue(encoded.contains(AppLocale.UNITED_STATES.id))
        assertTrue(encoded.contains(AppLocale.FRANCE.id))
        assertTrue(encoded.contains(Platform.IOS.id))
        assertTrue(encoded.contains(Platform.ANDROID.id))
        assertTrue(encoded.contains("MIN_AND_MAX_BOUND"))
    }

    @Test
    fun `Given invalid JSON, When deserialized, Then returns failure with InvalidJson error`() {
        val json = "not valid json at all"

        val result = decodeFeatureAware(json)

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

        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.FeatureNotFound>(result.error)
        val error = result.error
        assertEquals(FeatureId.create(NamespaceId("global"), "unregistered_feature"), error.key)
    }

    @Test
    fun `Given registered predicate ref in snapshot, When deserialized, Then it resolves and is preserved`(
    ) {
        val json = boolFlagSnapshotWithPredicateRef(
            namespaceId = TestFeatures.id,
            predicateId = TestFeatures.iosOnly.id.value,
        )

        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val decodedFlag = checkNotNull(result.value.flags[TestFeatures.boolFlag])
        val decodedRule = decodedFlag.values.first().rule
        assertEquals(1, decodedRule.predicateRefs.size)
        val decodedRef = assertIs<PredicateRef.Registered>(decodedRule.predicateRefs.single())
        assertEquals(TestFeatures.id, decodedRef.namespaceId)
        assertEquals(TestFeatures.iosOnly.id, decodedRef.id)
        loadMaterialized(result.value)
        assertTrue(TestFeatures.boolFlag.evaluate(ctx("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", platform = Platform.IOS)))
        assertFalse(
            TestFeatures.boolFlag.evaluate(
                ctx("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", platform = Platform.ANDROID),
            ),
        )

        val encoded = ConfigurationCodec.encode(result.value)
        assertTrue(encoded.contains("\"predicateRefs\""))
        assertTrue(encoded.contains("\"id\": \"${TestFeatures.iosOnly.id.value}\""))
    }

    @Test
    fun `Given snapshot rule with unknown predicate ref, When deserialized, Then UnknownPredicate is returned`() {
        val missingId = "missing-ios-only-ref"
        val json = boolFlagSnapshotWithPredicateRef(
            namespaceId = TestFeatures.id,
            predicateId = missingId,
        )

        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Failure>(result)
        val error = assertIs<ParseError.UnknownPredicate>(result.error)
        val ref = assertIs<PredicateRef.Registered>(error.ref)
        assertEquals(TestFeatures.id, ref.namespaceId)
        assertEquals(missingId, ref.id.value)
    }

    // ========== Round-Trip Tests ==========

    @Test
    fun `Given boolean flag, When round-tripped, Then deserialized value equals original`() {
        val originalFlag = FlagDefinition(feature = TestFeatures.boolFlag, defaultValue = true)
        val originalConfiguration = Configuration(mapOf(TestFeatures.boolFlag to originalFlag))

        val json = ConfigurationCodec.encode(originalConfiguration)
        val result = decodeFeatureAware(json)

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

        val json = ConfigurationCodec.encode(originalConfiguration)
        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedFlag = result.value.flags[TestFeatures.stringFlag]
        assertNotNull(deserializedFlag)
        assertEquals(originalFlag.defaultValue, deserializedFlag.defaultValue)
    }

    @Test
    fun `Given int flag, When round-tripped, Then deserialized value equals original`() {
        val originalFlag = FlagDefinition(feature = TestFeatures.intFlag, defaultValue = 42)
        val originalConfiguration = Configuration(mapOf(TestFeatures.intFlag to originalFlag))

        val json = ConfigurationCodec.encode(originalConfiguration)
        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedFlag = result.value.flags[TestFeatures.intFlag]
        assertNotNull(deserializedFlag)
        assertEquals(originalFlag.defaultValue, deserializedFlag.defaultValue)
    }

    @Test
    fun `Given double flag, When round-tripped, Then deserialized value equals original`() {
        val originalFlag = FlagDefinition(feature = TestFeatures.doubleFlag, defaultValue = 3.14)
        val originalConfiguration = Configuration(mapOf(TestFeatures.doubleFlag to originalFlag))

        val json = ConfigurationCodec.encode(originalConfiguration)
        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedFlag = result.value.flags[TestFeatures.doubleFlag]
        assertNotNull(deserializedFlag)
        assertEquals(originalFlag.defaultValue, deserializedFlag.defaultValue)
    }

    @Test
    fun `Given flag with complex rules, When round-tripped, Then all rule attributes are preserved`() {
        TestFeatures.boolFlag.update(false) {
            enable {
                rampUp { 75.0 }
                note("Complex rule")
                locales(AppLocale.UNITED_STATES)
                platforms(Platform.ANDROID)
                versions {
                    min(2, 0, 0)
                    max(3, 0, 0)
                }
                constrain(TestEnvironment.DEV, TestEnvironment.PROD)
                constrain(TestTenant.CONSUMER)
            }
        }

        val json = TestFeatures.json
        val result = decodeFeatureAware(json)
        println(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedFlag = result.value.flags[TestFeatures.boolFlag]
        assertNotNull(deserializedFlag)
        assertEquals(1, deserializedFlag.values.size)

        val deserializedRule = deserializedFlag.values.first().rule
        assertEquals(75.0, deserializedRule.rampUp.value)
        assertEquals("Complex rule", deserializedRule.note)
        val encoded = ConfigurationCodec.encode(result.value)
        assertTrue(encoded.contains(AppLocale.UNITED_STATES.id))
        assertTrue(encoded.contains(Platform.ANDROID.id))
        assertTrue(encoded.contains("MIN_AND_MAX_BOUND"))
        assertTrue(encoded.contains("\"major\": 2"))
        assertTrue(encoded.contains("\"major\": 3"))
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

        val json = ConfigurationCodec.encode(originalConfiguration)
        val result = decodeFeatureAware(json)

        assertIs<ParseResult.Success<Configuration>>(result)
        val deserializedKonfig = result.value
        assertEquals(TestFeatures.allFeatures().size, deserializedKonfig.flags.size)

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

        val result =
            applyPatchFeatureAware(
                currentConfiguration = originalConfiguration,
                patchJson = patchJson,
            )

        assertIs<ParseResult.Success<Configuration>>(result)
        val patchedKonfig = result.value
        assertEquals(TestFeatures.allFeatures().size, patchedKonfig.flags.size)
        assertEquals(
            true,
            checkNotNull(patchedKonfig.flags[TestFeatures.boolFlag]).defaultValue,
        )
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

        val result =
            applyPatchFeatureAware(
                currentConfiguration = originalConfiguration,
                patchJson = patchJson,
            )

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

        val result =
            applyPatchFeatureAware(
                currentConfiguration = originalConfiguration,
                patchJson = patchJson,
            )

        assertIs<ParseResult.Success<Configuration>>(result)
        val patchedKonfig = result.value
        assertEquals(TestFeatures.allFeatures().size, patchedKonfig.flags.size)
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

        val result =
            applyPatchFeatureAware(
                currentConfiguration = originalConfiguration,
                patchJson = patchJson,
            )

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

        // Removed declared flag is restored from compile-time defaults in fill mode.
        assertEquals(
            "default",
            checkNotNull(patchedKonfig.flags[TestFeatures.stringFlag]).defaultValue,
        )

        // Total count
        assertEquals(TestFeatures.allFeatures().size, patchedKonfig.flags.size)
    }

    @Test
    fun `Given invalid patch JSON, When applied, Then returns failure`() {
        val originalConfiguration = Configuration(emptyMap())
        val invalidPatchJson = "not valid json"

        val result = applyPatchFeatureAware(originalConfiguration, invalidPatchJson)

        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.InvalidJson>(result.error)
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

        val result =
            applyPatchFeatureAware(
                currentConfiguration = originalConfiguration,
                patchJson,
                options = SnapshotLoadOptions.fillMissingDeclaredFlags(),
            )

        assertIs<ParseResult.Success<Configuration>>(result)
    }
}
