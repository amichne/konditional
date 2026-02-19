file=konditional-core/src/test/kotlin/io/amichne/konditional/core/BoundaryFailureResultTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.core.result.KonditionalBoundaryFailure,io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.parseErrorOrNull,io.amichne.konditional.core.result.parseFailure,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertIs,kotlin.test.assertNull,kotlin.test.assertTrue
type=io.amichne.konditional.core.BoundaryFailureResultTest|kind=class|decl=class BoundaryFailureResultTest
methods:
- fun `parseFailure wraps ParseError in KonditionalBoundaryFailure`()
- fun `parseErrorOrNull extracts parse error from throwable and result`()
