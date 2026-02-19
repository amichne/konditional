file=openfeature/src/test/kotlin/io/amichne/konditional/openfeature/KonditionalOpenFeatureProviderTest.kt
package=io.amichne.konditional.openfeature
imports=dev.openfeature.sdk.ErrorCode,dev.openfeature.sdk.EvaluationContext,dev.openfeature.sdk.ImmutableContext,dev.openfeature.sdk.Reason,dev.openfeature.sdk.Value,io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.context.Context,io.amichne.konditional.core.FlagDefinition,io.amichne.konditional.core.Namespace,io.amichne.konditional.core.features.Feature,io.amichne.konditional.core.registry.NamespaceRegistry,org.junit.jupiter.api.Assertions.assertEquals,org.junit.jupiter.api.Test
type=io.amichne.konditional.openfeature.KonditionalOpenFeatureProviderTest|kind=class|decl=class KonditionalOpenFeatureProviderTest
type=io.amichne.konditional.openfeature.Variant|kind=enum|decl=private enum class Variant
type=io.amichne.konditional.openfeature.TestFlags|kind=object|decl=private object TestFlags : Namespace.TestNamespaceFacade("openfeature-provider")
type=io.amichne.konditional.openfeature.CountingNamespaceRegistry|kind=class|decl=private class CountingNamespaceRegistry( private val delegate: NamespaceRegistry, ) : NamespaceRegistry by delegate
fields:
- private val provider
- private val context
- val enabled by boolean<Context>(default = true)
- val title by string<Context>(default = "hello")
- val retries by integer<Context>(default = 3)
- val multiplier by double<Context>(default = 1.5)
- val variant by enum<Variant, Context>(default = Variant.CONTROL)
- var allFlagsCalls: Int
methods:
- fun `boolean evaluation returns default with reason`()
- fun `string evaluation returns default with reason`()
- fun `integer evaluation returns default with reason`()
- fun `double evaluation returns default with reason`()
- fun `object evaluation returns type mismatch when value cannot be converted to OpenFeature Value`()
- fun `missing flag returns error`()
- fun `type mismatch returns error`()
- fun `missing targeting key returns invalid context via typed mapper result`()
- fun `blank targeting key returns invalid context via typed mapper result`()
- fun `provider resolves known keys without repeated all flags scans`()
- override fun allFlags(): Map<Feature<*, *, *>, FlagDefinition<*, *, *>>
