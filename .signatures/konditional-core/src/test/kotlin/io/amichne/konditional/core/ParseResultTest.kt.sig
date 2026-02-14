file=konditional-core/src/test/kotlin/io/amichne/konditional/core/ParseResultTest.kt
package=io.amichne.konditional.core
imports=io.amichne.konditional.core.result.ParseError,io.amichne.konditional.core.result.ParseException,io.amichne.konditional.core.result.ParseResult,io.amichne.konditional.core.result.utils.flatMap,io.amichne.konditional.core.result.utils.fold,io.amichne.konditional.core.result.utils.getOrDefault,io.amichne.konditional.core.result.utils.getOrElse,io.amichne.konditional.core.result.utils.getOrNull,io.amichne.konditional.core.result.utils.isFailure,io.amichne.konditional.core.result.utils.isSuccess,io.amichne.konditional.core.result.utils.map,io.amichne.konditional.core.result.utils.toResult,io.amichne.konditional.values.FeatureId,kotlin.test.Test,kotlin.test.assertEquals,kotlin.test.assertFailsWith,kotlin.test.assertFalse,kotlin.test.assertIs,kotlin.test.assertNull,kotlin.test.assertTrue
type=io.amichne.konditional.core.ParseResultTest|kind=class|decl=class ParseResultTest
type=io.amichne.konditional.core.TestOutcome|kind=interface|decl=sealed interface TestOutcome<out E, out S>
type=io.amichne.konditional.core.Success|kind=class|decl=data class Success<S>(val value: S) : TestOutcome<Nothing, S>
type=io.amichne.konditional.core.Failure|kind=class|decl=data class Failure<E>(val error: E) : TestOutcome<E, Nothing>
type=io.amichne.konditional.core.MyError|kind=class|decl=data class MyError(val reason: String)
