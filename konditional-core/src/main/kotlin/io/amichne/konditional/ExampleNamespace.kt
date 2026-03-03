@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional

import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.LocaleTag
import io.amichne.konditional.context.PlatformTag
import io.amichne.konditional.context.Version
import io.amichne.konditional.context.axis.AxisValue
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.dsl.rules.targeting.scopes.constrain
import io.amichne.konditional.core.id.StableId

@KonditionalInternalApi
data class ContextSample(
    override val locale: LocaleTag,
    override val stableId: StableId,
    override val appVersion: Version,
    override val platform: PlatformTag
) : Context.LocaleContext, Context.PlatformContext, Context.StableIdContext, Context.VersionContext

@KonditionalInternalApi
enum class AxisDef : AxisValue<AxisDef> {
    EXAMPLE_AXIS,
    SECOND_VALUE;
}

class ExampleNamespace : Namespace() {
    val exampleFlag by boolean<ContextSample>(default = true) {
        rule {
            locales(AppLocale.UNITED_STATES)
            constrain(AxisDef.EXAMPLE_AXIS)
        }
    }
}
