file=openfeature/src/test/kotlin/io/amichne/konditional/openfeature/KonditionalOpenFeatureProviderTest.kt
package=io.amichne.konditional.openfeature
imports=dev.openfeature.sdk.ErrorCode,dev.openfeature.sdk.ImmutableContext,dev.openfeature.sdk.Reason,io.amichne.konditional.context.Context,io.amichne.konditional.core.Namespace,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Test
type=io.amichne.konditional.openfeature.KonditionalOpenFeatureProviderTest|kind=class|decl=class KonditionalOpenFeatureProviderTest
type=io.amichne.konditional.openfeature.TestFlags|kind=object|decl=private object TestFlags : Namespace.TestNamespaceFacade("openfeature-provider")
fields:
- private val provider
- private val context
- val enabled by boolean<Context>(default = true)
- val title by string<Context>(default = "hello")
- val retries by integer<Context>(default = 3)
- val multiplier by double<Context>(default = 1.5)
methods:
- fun `boolean evaluation returns default with reason`()
- fun `missing flag returns error`()
- fun `type mismatch returns error`()
- fun `invalid context returns error`()
