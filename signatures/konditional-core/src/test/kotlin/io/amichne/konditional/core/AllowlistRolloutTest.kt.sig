file=konditional-core/src/test/kotlin/io/amichne/konditional/core/AllowlistRolloutTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.api.evaluate,io.amichne.konditional.context.AppLocale,io.amichne.konditional.context.Context,io.amichne.konditional.context.Platform,io.amichne.konditional.context.Version,io.amichne.konditional.core.dsl.enable,io.amichne.konditional.core.id.StableId,kotlin.test.assertFalse,kotlin.test.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.core.AllowlistRolloutTest|kind=class|decl=class AllowlistRolloutTest
methods:
- private fun ctx(stableId: StableId): Context
- fun `rule allowlist bypasses rollout`()
- fun `flag allowlist bypasses rollout`()
