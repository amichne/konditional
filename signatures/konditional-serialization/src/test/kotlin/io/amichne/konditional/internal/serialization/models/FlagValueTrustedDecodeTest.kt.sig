file=konditional-serialization/src/test/kotlin/io/amichne/konditional/internal/serialization/models/FlagValueTrustedDecodeTest.kt
package=io.amichne.konditional.internal.serialization.models
imports=io.amichne.konditional.api.KonditionalInternalApi,io.amichne.konditional.fixtures.serializers.RetryPolicy,kotlin.test.assertEquals,kotlin.test.assertFailsWith,kotlin.test.assertTrue,org.junit.jupiter.api.Test
type=io.amichne.konditional.internal.serialization.models.FlagValueTrustedDecodeTest|kind=class|decl=class FlagValueTrustedDecodeTest
type=io.amichne.konditional.internal.serialization.models.Theme|kind=enum|decl=private enum class Theme
methods:
- fun `enum decode uses trusted metadata and ignores payload class hint`()
- fun `enum decode without trusted metadata fails`()
- fun `data class decode uses trusted metadata and ignores payload class hint`()
- fun `data class decode without trusted metadata fails`()
