package io.amichne.konditional.configstate

import io.amichne.konditional.api.axisValues
import io.amichne.konditional.api.invoke
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.RampUp
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.unaryPlus
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.Configuration
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.fixtures.TestAxes
import io.amichne.konditional.fixtures.TestContext
import io.amichne.konditional.fixtures.TestEnvironment
import io.amichne.konditional.fixtures.TestTenant
import io.amichne.konditional.fixtures.serializers.RetryPolicy
import io.amichne.konditional.fixtures.utilities.localeIds
import io.amichne.konditional.fixtures.utilities.platformIds
import io.amichne.konditional.internal.serialization.models.FlagValue
import io.amichne.konditional.internal.serialization.models.SerializableSnapshot
import io.amichne.konditional.rules.ConditionalValue.Companion.targetedBy
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.evaluable.AxisConstraint
import io.amichne.konditional.rules.versions.FullyBound
import io.amichne.konditional.serialization.FeatureRegistry
import io.amichne.konditional.serialization.SerializerRegistry
import io.amichne.konditional.serialization.SnapshotSerializer
import io.amichne.konditional.serialization.toSerializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConfigurationStateFactoryRoundTripTest {
    private enum class Theme {
        LIGHT,
        DARK,
    }

    private object TestFeatures : Namespace.TestNamespaceFacade("configstate-roundtrip-${UUID.randomUUID()}") {
        val boolFlag by boolean<TestContext>(default = false)
        val themeFlag by enum<Theme, TestContext>(default = Theme.LIGHT)
        val retryPolicyFlag by custom<RetryPolicy, TestContext>(default = RetryPolicy())
    }

    private val stableIdFlagAllowlisted = StableId.fromHex("deadbeef")
    private val stableIdRuleAllowlisted = StableId.fromHex("cafebabe")
    private val stableIdNeither = StableId.fromHex("0123456789abcdef")

    @BeforeEach
    fun setup() {
        TestAxes.Environment
        TestAxes.Tenant

        SerializerRegistry.register(RetryPolicy::class, RetryPolicy.serializer)
        FeatureRegistry.clear()
        TestFeatures.load(Configuration(emptyMap()))
        FeatureRegistry.register(TestFeatures.boolFlag)
        FeatureRegistry.register(TestFeatures.themeFlag)
        FeatureRegistry.register(TestFeatures.retryPolicyFlag)
    }

    @AfterEach
    fun cleanup() {
        SerializerRegistry.clear()
    }

    @Test
    fun `snapshot roundtrip via ConfigurationStateFactory preserves evaluation semantics`() {
        val initial = configWithComplexRules()
        TestFeatures.load(initial)
        println(snapshotJson(initial.toSerializable()))

        val prodAllowlistedByFlag =
            ctx(env = TestEnvironment.PROD, tenant = TestTenant.CONSUMER, stableId = stableIdFlagAllowlisted)
        val prodAllowlistedByRule =
            ctx(env = TestEnvironment.PROD, tenant = TestTenant.CONSUMER, stableId = stableIdRuleAllowlisted)
        val prodNeitherAllowlisted =
            ctx(env = TestEnvironment.PROD, tenant = TestTenant.CONSUMER, stableId = stableIdNeither)
        val devAllowlistedByFlag =
            ctx(env = TestEnvironment.DEV, tenant = TestTenant.CONSUMER, stableId = stableIdFlagAllowlisted)
        val prodWrongVersionAllowlistedByFlag =
            ctx(
                env = TestEnvironment.PROD,
                tenant = TestTenant.CONSUMER,
                stableId = stableIdFlagAllowlisted,
                version = Version.of(0, 9, 0),
            )

        val expectedBoolResults =
            listOf(
                TestFeatures.boolFlag(prodAllowlistedByFlag),
                TestFeatures.boolFlag(prodAllowlistedByRule),
                TestFeatures.boolFlag(prodNeitherAllowlisted),
                TestFeatures.boolFlag(devAllowlistedByFlag),
                TestFeatures.boolFlag(prodWrongVersionAllowlistedByFlag),
            )

        val expectedTheme = TestFeatures.themeFlag(prodAllowlistedByFlag)
        val expectedRetryPolicyConsumer = TestFeatures.retryPolicyFlag(prodAllowlistedByFlag)
        val expectedRetryPolicyEnterprise =
            TestFeatures.retryPolicyFlag(
                ctx(env = TestEnvironment.PROD, tenant = TestTenant.ENTERPRISE, stableId = stableIdNeither),
            )

        val response = ConfigurationStateFactory.from(TestFeatures.configuration)
        val json = snapshotJson(response.currentState)

        println(json)

        val parsed = SnapshotSerializer.fromJson(json)
        assertIs<ParseResult.Success<Configuration>>(parsed)
        TestFeatures.load(parsed.value)

        val actualBoolResults =
            listOf(
                TestFeatures.boolFlag(prodAllowlistedByFlag),
                TestFeatures.boolFlag(prodAllowlistedByRule),
                TestFeatures.boolFlag(prodNeitherAllowlisted),
                TestFeatures.boolFlag(devAllowlistedByFlag),
                TestFeatures.boolFlag(prodWrongVersionAllowlistedByFlag),
            )

        assertEquals(expectedBoolResults, actualBoolResults)
        assertEquals(expectedTheme, TestFeatures.themeFlag(prodAllowlistedByFlag))
        assertEquals(expectedRetryPolicyConsumer, TestFeatures.retryPolicyFlag(prodAllowlistedByFlag))
        assertEquals(
            expectedRetryPolicyEnterprise,
            TestFeatures.retryPolicyFlag(
                ctx(
                    env = TestEnvironment.PROD,
                    tenant = TestTenant.ENTERPRISE,
                    stableId = stableIdNeither,
                ),
            ),
        )

        assertTrue(TestFeatures.boolFlag(prodAllowlistedByFlag))
        assertTrue(TestFeatures.boolFlag(prodAllowlistedByRule))
        assertFalse(TestFeatures.boolFlag(prodNeitherAllowlisted))
        assertFalse(TestFeatures.boolFlag(devAllowlistedByFlag))
        assertFalse(TestFeatures.boolFlag(prodWrongVersionAllowlistedByFlag))
    }

    @Test
    fun `external snapshot update via factory output is reflected after reload`() {
        TestFeatures.load(configWithComplexRules())

        val baselineContext = ctx(env = TestEnvironment.PROD, tenant = TestTenant.CONSUMER, stableId = stableIdNeither)
        assertFalse(TestFeatures.boolFlag(baselineContext))

        val response = ConfigurationStateFactory.from(TestFeatures.configuration)
        val updatedSnapshot =
            response.currentState.updatedFlag(TestFeatures.boolFlag.id.plainId) { flag ->
                flag.copy(
                    isActive = false,
                    defaultValue = FlagValue.BooleanValue(true),
                )
            }

        val updatedJson = snapshotJson(updatedSnapshot)
        val parsed = SnapshotSerializer.fromJson(updatedJson)
        assertIs<ParseResult.Success<Configuration>>(parsed)
        TestFeatures.load(parsed.value)

        assertTrue(
            TestFeatures.boolFlag(baselineContext),
            "Inactive flags must return the updated default value",
        )
    }

    @Test
    fun `external snapshot update can retarget rule axes and rampUp`() {
        TestFeatures.load(configWithComplexRules())

        val prodAllowlistedByFlag =
            ctx(env = TestEnvironment.PROD, tenant = TestTenant.CONSUMER, stableId = stableIdFlagAllowlisted)
        val devNeitherAllowlisted =
            ctx(env = TestEnvironment.DEV, tenant = TestTenant.CONSUMER, stableId = stableIdNeither)

        assertTrue(TestFeatures.boolFlag(prodAllowlistedByFlag))
        assertFalse(TestFeatures.boolFlag(devNeitherAllowlisted))

        val response = ConfigurationStateFactory.from(TestFeatures.configuration)
        val updatedSnapshot =
            response.currentState.updatedFlag(TestFeatures.boolFlag.id.plainId) { flag ->
                val updatedRules =
                    flag.rules.mapIndexed { index, rule ->
                        if (index != 0) rule else
                            rule.copy(
                                rampUp = 100.0,
                                rampUpAllowlist = emptySet(),
                                axes = mapOf("environment" to setOf(TestEnvironment.DEV.id)),
                            )
                    }

                flag.copy(
                    rampUpAllowlist = emptySet(),
                    rules = updatedRules,
                )
            }

        val updatedJson = snapshotJson(updatedSnapshot)
        val parsed = SnapshotSerializer.fromJson(updatedJson)
        assertIs<ParseResult.Success<Configuration>>(parsed)
        TestFeatures.load(parsed.value)

        assertFalse(
            TestFeatures.boolFlag(prodAllowlistedByFlag),
            "After retargeting axes to DEV and removing allowlists, PROD must no longer match",
        )
        assertTrue(
            TestFeatures.boolFlag(devNeitherAllowlisted),
            "After retargeting axes to DEV with rampUp=100%, DEV must match",
        )
    }

    private fun snapshotJson(snapshot: SerializableSnapshot): String =
        SnapshotSerializer
            .defaultMoshi()
            .adapter(SerializableSnapshot::class.java)
            .indent("  ")
            .toJson(snapshot)

    private fun ctx(
        env: TestEnvironment,
        tenant: TestTenant,
        stableId: StableId,
        locale: AppLocale = AppLocale.UNITED_STATES,
        platform: Platform = Platform.IOS,
        version: Version = Version.of(1, 5, 0),
    ): TestContext =
        TestContext(
            locale = locale,
            platform = platform,
            appVersion = version,
            stableId = stableId,
            axisValues =
                axisValues {
                    +env
                    +tenant
                },
        )

    private fun configWithComplexRules(): Configuration {
        val boolRule =
            Rule<TestContext>(
                rampUp = RampUp.of(0.0),
                rolloutAllowlist = setOf(stableIdRuleAllowlisted.hexId),
                note = "rule note",
                locales = localeIds(AppLocale.UNITED_STATES, AppLocale.FRANCE),
                platforms = platformIds(Platform.IOS, Platform.ANDROID),
                versionRange = FullyBound(Version.of(1, 0, 0), Version.of(2, 0, 0)),
                axisConstraints = listOf(
                    AxisConstraint(
                        axisId = "environment",
                        allowedIds = setOf(TestEnvironment.PROD.id)
                    )
                ),
            )

        val boolDefinition =
            FlagDefinition(
                feature = TestFeatures.boolFlag,
                bounds = listOf(boolRule.targetedBy(true)),
                defaultValue = false,
                salt = "salt-v1",
                isActive = true,
                rampUpAllowlist = setOf(stableIdFlagAllowlisted.hexId),
            )

        val themeRule =
            Rule<TestContext>(
                rampUp = RampUp.of(100.0),
                note = "theme override",
                locales = localeIds(AppLocale.UNITED_STATES),
            )
        val themeDefinition =
            FlagDefinition(
                feature = TestFeatures.themeFlag,
                bounds = listOf(themeRule.targetedBy(Theme.DARK)),
                defaultValue = Theme.LIGHT,
            )

        val retryPolicyRule =
            Rule<TestContext>(
                rampUp = RampUp.of(100.0),
                axisConstraints = listOf(
                    AxisConstraint(
                        axisId = "tenant",
                        allowedIds = setOf(TestTenant.ENTERPRISE.id)
                    )
                ),
            )
        val retryPolicyDefinition =
            FlagDefinition(
                feature = TestFeatures.retryPolicyFlag,
                bounds = listOf(retryPolicyRule.targetedBy(RetryPolicy(maxAttempts = 9))),
                defaultValue = RetryPolicy(maxAttempts = 3),
            )

        return Configuration(
            flags =
                linkedMapOf(
                    TestFeatures.boolFlag to boolDefinition,
                    TestFeatures.themeFlag to themeDefinition,
                    TestFeatures.retryPolicyFlag to retryPolicyDefinition,
                ),
        )
    }

    private fun SerializableSnapshot.updatedFlag(
        flagKey: String,
        update: (io.amichne.konditional.internal.serialization.models.SerializableFlag) -> io.amichne.konditional.internal.serialization.models.SerializableFlag,
    ): SerializableSnapshot =
        copy(
            flags =
                flags.map { flag ->
                    if (flag.key.plainId == flagKey) update(flag) else flag
                },
        )
}
