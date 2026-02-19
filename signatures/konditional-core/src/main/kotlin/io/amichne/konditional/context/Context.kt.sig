file=konditional-core/src/main/kotlin/io/amichne/konditional/context/Context.kt
package=io.amichne.konditional.context
imports=io.amichne.konditional.context.axis.AxisValue,io.amichne.konditional.context.axis.AxisValues,io.amichne.konditional.core.id.StableId
type=io.amichne.konditional.context.Context|kind=interface|decl=interface Context
type=io.amichne.konditional.context.LocaleContext|kind=interface|decl=interface LocaleContext : Context
type=io.amichne.konditional.context.PlatformContext|kind=interface|decl=interface PlatformContext : Context
type=io.amichne.konditional.context.VersionContext|kind=interface|decl=interface VersionContext : Context
type=io.amichne.konditional.context.StableIdContext|kind=interface|decl=interface StableIdContext : Context
type=io.amichne.konditional.context.Core|kind=class|decl=data class Core( override val locale: LocaleTag, override val platform: PlatformTag, override val appVersion: Version, override val stableId: StableId, ) : LocaleContext, PlatformContext, VersionContext, StableIdContext
fields:
- val locale: LocaleTag
- val platform: PlatformTag
- val appVersion: Version
- val stableId: StableId
