package io.amichne.konditional.core

import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseException
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.result.utils.flatMap
import io.amichne.konditional.core.result.utils.fold
import io.amichne.konditional.core.result.utils.getOrDefault
import io.amichne.konditional.core.result.utils.getOrElse
import io.amichne.konditional.core.result.utils.getOrNull
import io.amichne.konditional.core.result.utils.isFailure
import io.amichne.konditional.core.result.utils.isSuccess
import io.amichne.konditional.core.result.utils.map
import io.amichne.konditional.core.result.utils.toResult
import io.amichne.konditional.values.Identifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ParseResult following Parse, Don't Validate principles.
 *
 * These tests verify that:
 * - ParseResult provides type-safe error handling
 * - All operations preserve the success/failure state correctly
 * - Adapters work with various error handling systems
 * - No exceptions are thrown during normal operation
 */
class ParseResultTest {

    // Mock Outcome type for testing adaptation
    sealed interface TestOutcome<out E, out S> {
        data class Success<S>(val value: S) : TestOutcome<Nothing, S>
        data class Failure<E>(val error: E) : TestOutcome<E, Nothing>
    }

    data class MyError(val reason: String)

    private val successResult: ParseResult<Int> = ParseResult.Success(42)
    private val failureResult: ParseResult<Int> = ParseResult.Failure(
        ParseError.InvalidHexId("invalid", "Invalid hex format")
    )

    @Test
    fun `Success contains value`() {
        val result = ParseResult.Success("test-value")
        assertIs<ParseResult.Success<String>>(result)
        assertEquals("test-value", result.value)
    }

    @Test
    fun `Failure contains error`() {
        val error = ParseError.FlagNotFound(Identifier("my-flag"))
        val result = ParseResult.Failure(error)
        assertIs<ParseResult.Failure>(result)
        assertEquals(error, result.error)
    }

    @Test
    fun `fold transforms Success to target type`() {
        val result = successResult.fold(
            onSuccess = { "success: $it" },
            onFailure = { "failure: ${it.message}" }
        )
        assertEquals("success: 42", result)
    }

    @Test
    fun `fold transforms Failure to target type`() {
        val result = failureResult.fold(
            onSuccess = { "success: $it" },
            onFailure = { "failure: ${it.message}" }
        )
        assertEquals("failure: Invalid hex format", result)
    }

    @Test
    fun `map transforms Success value`() {
        val result = successResult.map { it * 2 }
        assertIs<ParseResult.Success<Int>>(result)
        assertEquals(84, result.value)
    }

    @Test
    fun `map preserves Failure`() {
        val result = failureResult.map { it * 2 }
        assertIs<ParseResult.Failure>(result)
        assertEquals("Invalid hex format", result.error.message)
    }

    @Test
    fun `flatMap chains successful operations`() {
        val result = successResult.flatMap { value ->
            ParseResult.Success(value.toString())
        }
        assertIs<ParseResult.Success<String>>(result)
        assertEquals("42", result.value)
    }

    @Test
    fun `flatMap short-circuits on Failure`() {
        val result = failureResult.flatMap { value ->
            ParseResult.Success(value.toString())
        }
        assertIs<ParseResult.Failure>(result)
        assertEquals("Invalid hex format", result.error.message)
    }

    @Test
    fun `flatMap propagates failure from chained operation`() {
        val result = successResult.flatMap {
            ParseResult.Failure(ParseError.InvalidVersion("1.x", "Invalid format"))
        }
        assertIs<ParseResult.Failure>(result)
        assertIs<ParseError.InvalidVersion>(result.error)
    }

    @Test
    fun `getOrNull returns value on Success`() {
        assertEquals(42, successResult.getOrNull())
    }

    @Test
    fun `getOrNull returns null on Failure`() {
        assertNull(failureResult.getOrNull())
    }

    @Test
    fun `getOrDefault returns value on Success`() {
        assertEquals(42, successResult.getOrDefault(100))
    }

    @Test
    fun `getOrDefault returns default on Failure`() {
        assertEquals(100, failureResult.getOrDefault(100))
    }

    @Test
    fun `getOrElse returns value on Success`() {
        val result = successResult.getOrElse { _ -> -1 }
        assertEquals(42, result)
    }

    @Test
    fun `getOrElse computes default on Failure`() {
        val result = failureResult.getOrElse { error ->
            when (error) {
                is ParseError.InvalidHexId -> -1
                else -> -2
            }
        }
        assertEquals(-1, result)
    }

    @Test
    fun `isSuccess returns true for Success`() {
        assertTrue(successResult.isSuccess())
    }

    @Test
    fun `isSuccess returns false for Failure`() {
        assertFalse(failureResult.isSuccess())
    }

    @Test
    fun `isFailure returns false for Success`() {
        assertFalse(successResult.isFailure())
    }

    @Test
    fun `isFailure returns true for Failure`() {
        assertTrue(failureResult.isFailure())
    }

    @Test
    fun `toResult converts Success to Result success`() {
        val result = successResult.toResult()
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `toResult converts Failure to Result failure with ParseException`() {
        val result = failureResult.toResult()
        assertTrue(result.isFailure)

        val exception = result.exceptionOrNull()
        assertIs<ParseException>(exception)
        assertEquals("Invalid hex format", exception.message)
        assertIs<ParseError.InvalidHexId>(exception.error)
    }

    @Test
    fun `ParseError FeatureNotFound generates message`() {
        val error = ParseError.FeatureNotFound(Identifier("test-key"))
        assertEquals("Feature not found: value::test-key", error.message)
    }

    @Test
    fun `ParseError FlagNotFound generates message`() {
        val error = ParseError.FlagNotFound(Identifier("test-flag"))
        assertEquals("Flag not found: value::test-flag", error.message)
    }

    @Test
    fun `chain multiple map operations`() {
        val result = successResult
            .map { it * 2 }
            .map { it + 10 }
            .map { it.toString() }

        assertIs<ParseResult.Success<String>>(result)
        assertEquals("94", result.value)
    }

    @Test
    fun `chain map and flatMap operations`() {
        val result = successResult
            .map { it * 2 }
            .flatMap { ParseResult.Success("value: $it") }
            .map { it.uppercase() }

        assertIs<ParseResult.Success<String>>(result)
        assertEquals("VALUE: 84", result.value)
    }

    @Test
    fun `Failure in chain stops further processing`() {
        var mapCalled = false
        var flatMapCalled = false

        val result = failureResult
            .map {
                mapCalled = true
                it * 2
            }
            .flatMap {
                flatMapCalled = true
                ParseResult.Success(it.toString())
            }

        assertFalse(mapCalled, "map should not be called on Failure")
        assertFalse(flatMapCalled, "flatMap should not be called on Failure")
        assertIs<ParseResult.Failure>(result)
    }

    @Test
    fun `fold can adapt to custom error type`() {
        val adapted: TestOutcome<MyError, Int> = successResult.fold(
            onSuccess = { TestOutcome.Success(it) },
            onFailure = { TestOutcome.Failure(MyError(it.message)) }
        )

        assertIs<TestOutcome.Success<Int>>(adapted)
        assertEquals(42, adapted.value)
    }

    @Test
    fun `fold can adapt Failure to custom error type`() {
        val adapted: TestOutcome<MyError, Int> = failureResult.fold(
            onSuccess = { TestOutcome.Success(it) },
            onFailure = { TestOutcome.Failure(MyError(it.message)) }
        )

        assertIs<TestOutcome.Failure<MyError>>(adapted)
        assertEquals("Invalid hex format", adapted.error.reason)
    }

    @Test
    fun `ParseException contains original ParseError`() {
        val error = ParseError.InvalidRollout(150.0, "Rampup must be 0-100")
        val exception = ParseException(error)

        assertEquals("Rampup must be 0-100", exception.message)
        assertIs<ParseError.InvalidRollout>(exception.error)
    }

    @Suppress("KotlinConstantConditions")
    @Test
    fun `Success and Failure are distinct types`() {
        val success: ParseResult<String> = ParseResult.Success("value")
        val failure: ParseResult<String> = ParseResult.Failure(ParseError.FlagNotFound(Identifier("key")))

        // Type system enforces exhaustive when
        val result = when (success) {
            is ParseResult.Success -> "success"
            is ParseResult.Failure -> "failure"
        }
        assertEquals("success", result)

        val result2 = when (failure) {
            is ParseResult.Success -> "success"
            is ParseResult.Failure -> "failure"
        }
        assertEquals("failure", result2)
    }

    @Test
    fun `ParseResult makes illegal states unrepresentable`() {
        // Cannot construct a ParseResult that is both Success and Failure
        // Cannot construct a Success without a value
        // Cannot construct a Failure without an error

        // This test documents the type safety - the code wouldn't compile if we tried:
        // val invalid = ParseResult.Success(null) // Won't compile if T is non-nullable
        // val invalid = ParseResult.Failure(null) // Won't compile

        // We can only construct valid states:
        val validSuccess: ParseResult<Int> = ParseResult.Success(42)
        val validFailure: ParseResult<Int> = ParseResult.Failure(
            ParseError.InvalidHexId("bad", "Invalid")
        )

        assertTrue(validSuccess.isSuccess())
        assertTrue(validFailure.isFailure())
    }
}
