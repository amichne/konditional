file=konditional-core/src/test/kotlin/io/amichne/konditional/adversarial/ConcurrencyAttacksTest.kt
package=io.amichne.konditional.adversarial
imports=io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.dsl.disable,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,java.util.concurrent.ConcurrentHashMap,java.util.concurrent.CountDownLatch,java.util.concurrent.Executors,java.util.concurrent.TimeUnit,kotlin.test.assertEquals,kotlin.test.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.adversarial.ConcurrencyAttacksTest|kind=class|decl=class ConcurrencyAttacksTest
type=io.amichne.konditional.adversarial.MutableContext|kind=class|decl=data class MutableContext( override var locale: AppLocale, override var platform: Platform, override var appVersion: Version, override val stableId: StableId, ) : Context, Context.LocaleContext, Context.PlatformContext, Context.VersionContext, Context.StableIdContext
fields:
- var readerSawFeature
methods:
- fun `ATTACK - concurrent access to eagerly registered features`()
- fun `ATTACK - concurrent flag evaluation with different contexts`()
- fun `ATTACK - concurrent SHA-256 digest usage in bucketing`()
- fun `ATTACK - concurrent registration in same namespace`()
- fun `ATTACK - memory visibility of eager initialization across threads`()
- fun `ATTACK - modification during iteration of conditional values`()
- fun `ATTACK - mutating context during evaluation if mutable implementation used`()
- fun `ATTACK - concurrent calls to allFeatures() during registration`()
