package io.amichne.konditional.core

import io.amichne.konditional.api.evaluate
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.axis.Axes
import io.amichne.konditional.context.axis.KonditionalExplicitId
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.context.axis.axes
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.dsl.ruleSet
import io.amichne.konditional.core.dsl.rules.targeting.scopes.constrain
import io.amichne.konditional.core.dsl.rules.targeting.scopes.whenContext
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.serialization.snapshot.ConfigurationCodec
import io.amichne.konditional.serialization.snapshot.NamespaceSnapshotLoader
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsumerDslEndToEndTest {

    private enum class SubscriptionTier {
        BASIC,
        PREMIUM,
        ENTERPRISE,
    }

    private enum class Environment : AxisValue<Environment> {
        DEV,
        PROD,
    }

    private enum class CheckoutVariant {
        DEFAULT,
        IOS_LOCAL,
        ANDROID_CONTEXT_KCLASS,
        FRANCE_CONTEXT_REIFIED,
        EMPLOYEE_NAMESPACE_LOCAL,
        PROD_NAMESPACE_KCLASS,
        ENTERPRISE_NAMESPACE_REIFIED,
    }

    private data class CommerceContext(
        override val locale: AppLocale,
        override val platform: Platform,
        override val appVersion: Version,
        override val stableId: StableId,
        val subscriptionTier: SubscriptionTier,
        val isEmployee: Boolean,
        val tenantId: String,
        override val axes: Axes = Axes.EMPTY,
    ) : Context,
        Context.LocaleContext,
        Context.PlatformContext,
        Context.VersionContext,
        Context.StableIdContext

    @Suppress("LongParameterList")
    private fun commerceContext(
        stableId: StableId = StableId.of("consumer-default-user"),
        locale: AppLocale = AppLocale.UNITED_STATES,
        platform: Platform = Platform.IOS,
        appVersion: Version = Version.of(3, 0, 0),
        subscriptionTier: SubscriptionTier = SubscriptionTier.BASIC,
        isEmployee: Boolean = false,
        tenantId: String = "ent-001",
        axes: Axes = Axes.EMPTY,
    ): CommerceContext = CommerceContext(
        locale = locale,
        platform = platform,
        appVersion = appVersion,
        stableId = stableId,
        subscriptionTier = subscriptionTier,
        isEmployee = isEmployee,
        tenantId = tenantId,
        axes = axes,
    )

    private class SnapshotPaymentsNamespace(id: String) : Namespace(id) {
        val isPremium by predicate<CommerceContext> {
            subscriptionTier == SubscriptionTier.PREMIUM
        }

        val applePay by boolean<CommerceContext>(default = false) {
            active { true }
            salt("apple-pay-v1")
            allowlist(StableId.of("snapshot-flag-vip"))
            rule(true) {
                note("premium ios prod employee tenant")
                locales(AppLocale.UNITED_STATES)
                platforms(Platform.IOS)
                versions {
                    min(2, 0, 0)
                    max(4, 0, 0)
                }
                constrain(Environment.PROD)
                extension { isEmployee }
                whenContext<CommerceContext> { tenantId.startsWith("ent-") }
                require(isPremium)
                rampUp { 100.0 }
                allowlist(StableId.of("snapshot-rule-vip"))
            }
        }
    }

    private class SnapshotAnonymousRequireNamespace(id: String) : Namespace(id) {
        val isPremium by predicate<CommerceContext> {
            subscriptionTier == SubscriptionTier.PREMIUM
        }

        val anonymousRequireFeature by boolean<CommerceContext>(default = false) {
            rule(true) {
                require(isPremium)
                require { isEmployee }
            }
        }
    }

    private fun printSnapshot(label: String, json: String) {
        println("===== $label =====")
        println(json)
        println("======================")
    }

    private fun snapshotWithNoRules(featureId: String): String =
        """
        {
          "flags": [
            {
              "key": "$featureId",
              "defaultValue": {
                "type": "BOOLEAN",
                "value": false
              },
              "salt": "apple-pay-v1",
              "isActive": true,
              "rules": []
            }
          ]
        }
        """.trimIndent()

    @Test
    fun `flag DSL supports active, salt, rule note, rollout, and allowlists end-to-end`() {
        val flagAllowlisted = StableId.of("flag-allowlisted-user")
        val ruleAllowlisted = StableId.of("rule-allowlisted-user")
        val otherUser = StableId.of("other-user")

        val namespace = object : Namespace("consumer-dsl-rollout-${UUID.randomUUID()}") {
            val inactiveFeature by boolean<CommerceContext>(default = false) {
                active { false }
                enable { always() }
            }
            val rolloutFeature by boolean<CommerceContext>(default = false) {
                active { true }
                salt("consumer-rollout-v2")
                allowlist(flagAllowlisted)
                enable {
                    note("rollout gate")
                    rampUp { 0.0 }
                    allowlist(ruleAllowlisted)
                    matchAll()
                }
            }
        }

        assertFalse(namespace.inactiveFeature.evaluate(commerceContext()))
        assertTrue(namespace.rolloutFeature.evaluate(commerceContext(stableId = flagAllowlisted)))
        assertTrue(namespace.rolloutFeature.evaluate(commerceContext(stableId = ruleAllowlisted)))
        assertFalse(namespace.rolloutFeature.evaluate(commerceContext(stableId = otherUser)))
    }

    @Test
    fun `criteria-first rule syntax with static and deferred yields works for consumers`() {
        val namespace = object : Namespace("consumer-dsl-yields-${UUID.randomUUID()}") {
            val dependency by string<CommerceContext>(default = "dep-default") {
                rule { android() } yields "dep-android"
                rule { always() } yields "dep-catch-all"
            }
            val composed by string<CommerceContext>(default = "composed-default") {
                rule { android() } yields { dependency.evaluate() }
                rule { matchAll() } yields "fallback"
            }
        }

        assertEquals("dep-android", namespace.composed.evaluate(commerceContext(platform = Platform.ANDROID)))
        assertEquals("fallback", namespace.composed.evaluate(commerceContext(platform = Platform.IOS)))
    }

    @Test
    fun `rule targeting DSL supports locales platforms versions axes extensions whenContext and named predicates`() {
        val namespace = object : Namespace("consumer-dsl-targeting-${UUID.randomUUID()}") {
            val isPremium by predicate<CommerceContext> {
                subscriptionTier == SubscriptionTier.PREMIUM
            }

            val targetedFeature by boolean<CommerceContext>(default = false) {
                rule(true) {
                    note("complex targeting")
                    anyOf {
                        ios()
                        constrain(Environment.PROD)
                    }
                    locales(AppLocale.UNITED_STATES)
                    platforms(Platform.IOS, Platform.ANDROID)
                    versions {
                        min(2, 0, 0)
                        max(4, 0, 0)
                    }
                    extension { isEmployee }
                    whenContext<CommerceContext> { tenantId.startsWith("ent-") }
                    require(isPremium)
                }
            }
        }

        val iosMatch = commerceContext(
            platform = Platform.IOS,
            locale = AppLocale.UNITED_STATES,
            appVersion = Version.of(3, 0, 0),
            subscriptionTier = SubscriptionTier.PREMIUM,
            isEmployee = true,
            tenantId = "ent-123",
            axes = axes(Environment.DEV),
        )
        val prodAxisMatch = commerceContext(
            platform = Platform.ANDROID,
            locale = AppLocale.UNITED_STATES,
            appVersion = Version.of(3, 0, 0),
            subscriptionTier = SubscriptionTier.PREMIUM,
            isEmployee = true,
            tenantId = "ent-999",
            axes = axes(Environment.PROD),
        )
        val missingPredicateMatch = commerceContext(
            subscriptionTier = SubscriptionTier.BASIC,
            isEmployee = true,
            tenantId = "ent-123",
        )
        val missingNarrowingMatch = commerceContext(
            subscriptionTier = SubscriptionTier.PREMIUM,
            isEmployee = true,
            tenantId = "tenant-123",
        )

        assertTrue(namespace.targetedFeature.evaluate(iosMatch))
        assertTrue(namespace.targetedFeature.evaluate(prodAxisMatch))
        assertFalse(namespace.targetedFeature.evaluate(missingPredicateMatch))
        assertFalse(namespace.targetedFeature.evaluate(missingNarrowingMatch))
    }

    @Test
    fun `namespace predicate delegates are consumable through require in rule DSL`() {
        val payments = object : Namespace("payments-dsl-${UUID.randomUUID()}") {
            val isPremium by predicate<CommerceContext> {
                subscriptionTier == SubscriptionTier.PREMIUM
            }

            val applePay by boolean<CommerceContext>(default = false) {
                rule(true) { require(isPremium) }
            }
        }

        assertTrue(
            payments.applePay.evaluate(
                commerceContext(subscriptionTier = SubscriptionTier.PREMIUM),
            ),
        )
        assertFalse(
            payments.applePay.evaluate(
                commerceContext(subscriptionTier = SubscriptionTier.BASIC),
            ),
        )
    }

    @Test
    fun `multiple named and anonymous require blocks compose with AND semantics`() {
        val namespace = object : Namespace("payments-mixed-require-${UUID.randomUUID()}") {
            val isPremium by predicate<CommerceContext> {
                subscriptionTier == SubscriptionTier.PREMIUM
            }
            val enterpriseTenant by predicate<CommerceContext> {
                tenantId.startsWith("ent-")
            }

            val targetedFeature by boolean<CommerceContext>(default = false) {
                rule(true) {
                    require(isPremium)
                    require { isEmployee }
                    require(enterpriseTenant)
                    require { platform == Platform.IOS }
                }
            }
        }

        assertTrue(
            namespace.targetedFeature.evaluate(
                commerceContext(
                    subscriptionTier = SubscriptionTier.PREMIUM,
                    isEmployee = true,
                    tenantId = "ent-77",
                    platform = Platform.IOS,
                ),
            ),
        )
        assertFalse(
            namespace.targetedFeature.evaluate(
                commerceContext(
                    subscriptionTier = SubscriptionTier.PREMIUM,
                    isEmployee = false,
                    tenantId = "ent-77",
                    platform = Platform.IOS,
                ),
            ),
        )
        assertFalse(
            namespace.targetedFeature.evaluate(
                commerceContext(
                    subscriptionTier = SubscriptionTier.PREMIUM,
                    isEmployee = true,
                    tenantId = "tenant-77",
                    platform = Platform.IOS,
                ),
            ),
        )
        assertFalse(
            namespace.targetedFeature.evaluate(
                commerceContext(
                    subscriptionTier = SubscriptionTier.PREMIUM,
                    isEmployee = true,
                    tenantId = "ent-77",
                    platform = Platform.ANDROID,
                ),
            ),
        )
    }

    @Test
    fun `snapshot dump prints serialized shape for named predicates and consumer rule metadata`() {
        val namespace = SnapshotPaymentsNamespace("consumer-dsl-snapshot-dump-${UUID.randomUUID()}")

        val dumpedJson = ConfigurationCodec.encode(namespace)
        printSnapshot("CONSUMER DSL SNAPSHOT DUMP", dumpedJson)

        assertTrue(dumpedJson.contains("\"key\": \"${namespace.applePay.id}\""))
        assertTrue(dumpedJson.contains("\"salt\": \"apple-pay-v1\""))
        assertTrue(dumpedJson.contains("\"isActive\": true"))
        assertTrue(dumpedJson.contains("\"note\": \"premium ios prod employee tenant\""))
        assertTrue(dumpedJson.contains("\"rampUp\": 100.0"))
        assertTrue(dumpedJson.contains("\"locales\": ["))
        assertTrue(dumpedJson.contains("\"platforms\": ["))
        assertTrue(dumpedJson.contains("\"versionRange\":"))
        assertTrue(dumpedJson.contains("\"axes\":"))
        assertTrue(dumpedJson.contains("\"predicateRefs\": ["))
        assertTrue(dumpedJson.contains("\"type\": \"REGISTERED\""))
        assertTrue(dumpedJson.contains("\"namespaceId\": \"${namespace.id.value}\""))
        assertTrue(dumpedJson.contains("\"id\": \"${namespace.isPremium.id.value}\""))
    }

    @Test
    fun `snapshot load prints before and after shapes and restores runtime behavior`() {
        val namespaceId = "consumer-dsl-snapshot-load-${UUID.randomUUID()}"
        val producer = SnapshotPaymentsNamespace(namespaceId)
        val consumer = SnapshotPaymentsNamespace(namespaceId)
        val matchingContext = commerceContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.of(3, 1, 0),
            subscriptionTier = SubscriptionTier.PREMIUM,
            isEmployee = true,
            tenantId = "ent-123",
            axes = axes(Environment.PROD),
        )
        val nonMatchingContext = commerceContext(
            locale = AppLocale.UNITED_STATES,
            platform = Platform.IOS,
            appVersion = Version.of(3, 1, 0),
            subscriptionTier = SubscriptionTier.BASIC,
            isEmployee = true,
            tenantId = "ent-123",
            axes = axes(Environment.PROD),
        )

        val dumpedJson = ConfigurationCodec.encode(producer)
        printSnapshot("SNAPSHOT BEFORE LOAD", dumpedJson)

        val noRulesJson = snapshotWithNoRules(consumer.applePay.id.toString())
        printSnapshot("SNAPSHOT BASELINE (NO RULES)", noRulesJson)
        NamespaceSnapshotLoader.forNamespace(consumer).load(noRulesJson).getOrThrow()
        assertFalse(consumer.applePay.evaluate(matchingContext))
        assertFalse(consumer.applePay.evaluate(nonMatchingContext))

        NamespaceSnapshotLoader.forNamespace(consumer).load(dumpedJson).getOrThrow()

        val loadedJson = ConfigurationCodec.encode(consumer)
        printSnapshot("SNAPSHOT AFTER LOAD", loadedJson)

        assertTrue(consumer.applePay.evaluate(matchingContext))
        assertFalse(consumer.applePay.evaluate(nonMatchingContext))

        assertTrue(loadedJson.contains("\"predicateRefs\": ["))
        assertTrue(loadedJson.contains("\"namespaceId\": \"${consumer.id.value}\""))
        assertTrue(loadedJson.contains("\"id\": \"${consumer.isPremium.id.value}\""))
        assertTrue(loadedJson.contains("\"salt\": \"apple-pay-v1\""))
        assertTrue(loadedJson.contains("\"note\": \"premium ios prod employee tenant\""))
    }

    @Test
    fun `snapshot load preserves mixed named and anonymous require predicates`() {
        val namespaceId = "consumer-dsl-anonymous-require-${UUID.randomUUID()}"
        val producer = SnapshotAnonymousRequireNamespace(namespaceId)
        val consumer = SnapshotAnonymousRequireNamespace(namespaceId)

        val matchingContext = commerceContext(
            subscriptionTier = SubscriptionTier.PREMIUM,
            isEmployee = true,
        )
        val missingAnonymousRequire = commerceContext(
            subscriptionTier = SubscriptionTier.PREMIUM,
            isEmployee = false,
        )

        assertTrue(producer.anonymousRequireFeature.evaluate(matchingContext))
        assertFalse(producer.anonymousRequireFeature.evaluate(missingAnonymousRequire))

        val dumpedJson = ConfigurationCodec.encode(producer)
        NamespaceSnapshotLoader.forNamespace(consumer).load(dumpedJson).getOrThrow()

        assertTrue(consumer.anonymousRequireFeature.evaluate(matchingContext))
        assertFalse(consumer.anonymousRequireFeature.evaluate(missingAnonymousRequire))
    }

    @Test
    @Suppress("LongMethod")
    fun `feature and namespace ruleSet overloads compose through include in consumer flows`() {
        val namespace = object : Namespace("consumer-dsl-rulesets-${UUID.randomUUID()}") {
            val template by enum<CheckoutVariant, CommerceContext>(default = CheckoutVariant.DEFAULT)
            private val feature: Feature<CheckoutVariant, CommerceContext, Namespace> = template

            private val localFeatureRuleSet = feature.ruleSet {
                rule(CheckoutVariant.IOS_LOCAL) {
                    ios()
                    locales(AppLocale.UNITED_STATES)
                }
            }
            private val kClassFeatureRuleSet = feature.ruleSet(Context::class) {
                rule(CheckoutVariant.ANDROID_CONTEXT_KCLASS) {
                    android()
                    locales(AppLocale.UNITED_STATES)
                }
            }
            private val reifiedFeatureRuleSet =
                feature.ruleSet<Context, CheckoutVariant, CommerceContext, Namespace> {
                    rule(CheckoutVariant.FRANCE_CONTEXT_REIFIED) {
                        locales(AppLocale.FRANCE)
                    }
                }

            @KonditionalExplicitId("local-namespace-checkout-rules")
            private val localNamespaceRuleSet by ruleSet<CheckoutVariant, CommerceContext, Namespace> {
                rule(CheckoutVariant.EMPLOYEE_NAMESPACE_LOCAL) {
                    locales(AppLocale.CANADA)
                    extension { isEmployee }
                }
            }
            @KonditionalExplicitId("kclass-namespace-checkout-rules")
            private val kClassNamespaceRuleSet by ruleSet<CheckoutVariant, Context, CommerceContext, Namespace> {
                rule(CheckoutVariant.PROD_NAMESPACE_KCLASS) {
                    locales(AppLocale.CANADA)
                    constrain(Environment.PROD)
                }
            }
            @KonditionalExplicitId("reified-namespace-checkout-rules")
            private val reifiedNamespaceRuleSet by ruleSet<CheckoutVariant, Context, CommerceContext, Namespace> {
                    rule(CheckoutVariant.ENTERPRISE_NAMESPACE_REIFIED) {
                        locales(AppLocale.CANADA)
                        whenContext<CommerceContext> {
                            subscriptionTier == SubscriptionTier.ENTERPRISE
                        }
                    }
                }

            val variant by enum<CheckoutVariant, CommerceContext>(default = CheckoutVariant.DEFAULT) {
                include(localFeatureRuleSet)
                include(kClassFeatureRuleSet)
                include(reifiedFeatureRuleSet)
                include(localNamespaceRuleSet)
                include(kClassNamespaceRuleSet)
                include(reifiedNamespaceRuleSet)
            }
        }

        assertEquals(
            CheckoutVariant.IOS_LOCAL,
            namespace.variant.evaluate(
                commerceContext(
                    locale = AppLocale.UNITED_STATES,
                    platform = Platform.IOS,
                    isEmployee = false,
                    subscriptionTier = SubscriptionTier.BASIC,
                ),
            ),
        )
        assertEquals(
            CheckoutVariant.ANDROID_CONTEXT_KCLASS,
            namespace.variant.evaluate(
                commerceContext(
                    locale = AppLocale.UNITED_STATES,
                    platform = Platform.ANDROID,
                    isEmployee = false,
                    subscriptionTier = SubscriptionTier.BASIC,
                ),
            ),
        )
        assertEquals(
            CheckoutVariant.FRANCE_CONTEXT_REIFIED,
            namespace.variant.evaluate(
                commerceContext(
                    locale = AppLocale.FRANCE,
                    platform = Platform.IOS,
                ),
            ),
        )
        assertEquals(
            CheckoutVariant.EMPLOYEE_NAMESPACE_LOCAL,
            namespace.variant.evaluate(
                commerceContext(
                    locale = AppLocale.CANADA,
                    platform = Platform.IOS,
                    isEmployee = true,
                    subscriptionTier = SubscriptionTier.BASIC,
                    axes = axes(Environment.DEV),
                ),
            ),
        )
        assertEquals(
            CheckoutVariant.PROD_NAMESPACE_KCLASS,
            namespace.variant.evaluate(
                commerceContext(
                    locale = AppLocale.CANADA,
                    platform = Platform.IOS,
                    isEmployee = false,
                    subscriptionTier = SubscriptionTier.BASIC,
                    axes = axes(Environment.PROD),
                ),
            ),
        )
        assertEquals(
            CheckoutVariant.ENTERPRISE_NAMESPACE_REIFIED,
            namespace.variant.evaluate(
                commerceContext(
                    locale = AppLocale.CANADA,
                    platform = Platform.IOS,
                    isEmployee = false,
                    subscriptionTier = SubscriptionTier.ENTERPRISE,
                    axes = axes(Environment.DEV),
                ),
            ),
        )
    }
}
