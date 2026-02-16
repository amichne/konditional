package io.amichne.konditional.core

import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseErrorOrNull
import io.amichne.konditional.core.result.parseFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoundaryFailureResultTest {
    @Test
    fun `parseFailure wraps ParseError in KonditionalBoundaryFailure`() {
        val parseError = ParseError.invalidSnapshot("bad payload")
        val result: Result<String> = parseFailure(parseError)

        assertTrue(result.isFailure)
        val failure = assertIs<KonditionalBoundaryFailure>(result.exceptionOrNull())
        assertEquals(parseError, failure.parseError)
        assertEquals(parseError.message, failure.message)
    }

    @Test
    fun `parseErrorOrNull extracts parse error from throwable and result`() {
        val parseError = ParseError.invalidJson("boom")
        val throwable = KonditionalBoundaryFailure(parseError)
        val result: Result<Unit> = Result.failure(throwable)

        assertEquals(parseError, throwable.parseErrorOrNull())
        assertEquals(parseError, result.parseErrorOrNull())
        assertNull(IllegalStateException("x").parseErrorOrNull())
        assertNull(Result.failure<Unit>(IllegalStateException("x")).parseErrorOrNull())
    }
}
