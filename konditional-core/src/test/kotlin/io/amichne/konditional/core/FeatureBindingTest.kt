package io.amichne.konditional.core

import io.amichne.konditional.api.bind
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.dsl.enable
import io.amichne.konditional.core.id.StableId
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureBindingTest {
    private data class BindingContext(
        val plan: String,
        override val locale: AppLocale = AppLocale.UNITED_STATES,
        override val platform: Platform = Platform.ANDROID,
        override val appVersion: Version = Version.parse("1.0.0").getOrThrow(),
        override val stableId: StableId = StableId.of("deadbeef"),
        override val axisValues: AxisValues = AxisValues.EMPTY,
    ) : Context,
        Context.LocaleContext,
        Context.PlatformContext,
        Context.VersionContext,
        Context.StableIdContext

    private data class ExtendedContext(
        val base: BindingContext,
        val tenantId: String,
    ) : Context,
        Context.LocaleContext,
        Context.PlatformContext,
        Context.VersionContext,
        Context.StableIdContext {
        override val locale: AppLocale = base.locale
        override val platform: Platform = base.platform
        override val appVersion: Version = base.appVersion
        override val stableId: StableId = base.stableId
        override val axisValues: AxisValues = base.axisValues
    }

    private object BindingFeatures : Namespace.TestNamespaceFacade("binding-features") {
        val isEnabled by boolean<BindingContext>(default = false) {
            enable()
        }

        val variant by string<BindingContext>(default = "control") {
            rule("treatment") {
                platforms(Platform.IOS)
            }
        }

        val fallbackVariant by string<BindingContext>(default = "fallback")
    }

    @Test
    fun `bind exposes dependency evaluation results`() {
        val context = BindingContext(plan = "premium")

        val result = BindingFeatures.isEnabled.bind().evaluate(context)

        assertEquals(true, result.value)
        assertEquals(1, result.dependencies.size)
        assertEquals(BindingFeatures.isEnabled.key, result.dependencies.single().featureKey)
    }

    @Test
    fun `map can build models from context and value`() {
        val context = BindingContext(plan = "enterprise")

        val ref =
            BindingFeatures.isEnabled
                .bind()
                .map { enabled, ctx -> "${ctx.plan}-$enabled" }

        val result = ref.evaluate(context)

        assertEquals("enterprise-true", result.value)
        assertEquals(1, result.dependencies.size)
    }

    @Test
    fun `zip combines dependencies from both references`() {
        val context = BindingContext(plan = "starter", platform = Platform.IOS)

        val result = BindingFeatures.isEnabled.bind().zip(BindingFeatures.variant.bind()).evaluate(context)

        assertEquals(true to "treatment", result.value)
        assertEquals(
            listOf(BindingFeatures.isEnabled.key, BindingFeatures.variant.key),
            result.dependencies.map { it.featureKey },
        )
    }

    @Test
    fun `flatMap chains dependent references deterministically`() {
        val context = BindingContext(plan = "basic", platform = Platform.ANDROID)

        val ref =
            BindingFeatures.isEnabled
                .bind()
                .thenUse { enabled, _ ->
                    if (enabled) BindingFeatures.variant.bind() else BindingFeatures.fallbackVariant.bind()
                }

        val result = ref.evaluate(context)

        assertEquals("control", result.value)
        assertEquals(2, result.dependencies.size)
    }

    @Test
    fun `contraMapContext adapts references to extended contexts`() {
        val context = ExtendedContext(BindingContext(plan = "basic"), tenantId = "tenant-1")

        val result = BindingFeatures.variant.bind().contraMapContext<ExtendedContext> { it.base }.evaluate(context)

        assertEquals("control", result.value)
        assertEquals(1, result.dependencies.size)
    }
}
