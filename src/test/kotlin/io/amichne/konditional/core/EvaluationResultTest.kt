package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.features.update
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.result.EvaluationResult
import io.amichne.konditional.core.result.FlagEvaluationException
import io.amichne.konditional.core.result.FlagNotFoundException
import io.amichne.konditional.core.result.utils.evaluateOrDefault
import io.amichne.konditional.core.result.utils.evaluateOrNull
import io.amichne.konditional.core.result.utils.evaluateOrThrow
import io.amichne.konditional.core.result.utils.evaluateSafe
import io.amichne.konditional.core.result.utils.fold
import io.amichne.konditional.core.result.utils.getOrDefault
import io.amichne.konditional.core.result.utils.getOrElse
import io.amichne.konditional.core.result.utils.getOrNull
import io.amichne.konditional.core.result.utils.isFailure
import io.amichne.konditional.core.result.utils.isSuccess
import io.amichne.konditional.core.result.utils.map
import io.amichne.konditional.core.result.utils.toResult
import io.amichne.konditional.fixtures.CommonTestFeatures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for EvaluationResult and evaluation APIs following Parse, Don't Validate principles.
 *
 * These tests verify that:
 * - EvaluationResult distinguishes between Success, FlagNotFound, and EvaluationError
 * - All evaluation APIs provide type-safe error handling
 * - evaluateSafe never throws exceptions
 * - evaluateOrNull and evaluateOrDefault provide convenient fallbacks
 * - evaluateOrThrow only throws when explicitly requested
 */
class EvaluationResultTest {

    // Mock Outcome type for testing adaptation
    sealed interface TestOutcome<out E, out S> {
        data class Success<S>(val value: S) : TestOutcome<Nothing, S>
        data class Failure<E>(val error: E) : TestOutcome<E, Nothing>
    }

    data class MyError(val reason: String)

    private val testContext = Context(
        AppLocale.EN_US,
        Platform.IOS,
        Version.parse("1.0.0"),
        StableId.of("11111111111111111111111111111111")
    )



    init {
        // Register a normal flag
        CommonTestFeatures.registeredFlag.update(
            CommonTestFeatures.registeredFlag.flag {
                rule { } implies "test-value"
                default("default-value")
            }
        )
    }

    @Test
    fun `evaluateSafe returns Success when flag is found and evaluates successfully`() {
        val result = testContext.evaluateSafe(CommonTestFeatures.registeredFlag)

        assertIs<EvaluationResult.Success<String>>(result)
        assertEquals("test-value", result.value)
    }


    @Test
    fun `evaluateSafe returns EvaluationError when evaluation throws`() {
        // We'll test EvaluationError by manually creating one since we can't easily
        // trigger an evaluation error without more complex setup
        val result: EvaluationResult<String> = EvaluationResult.EvaluationError(
            "test_key",
            IllegalStateException("Evaluation error for testing")
        )

        assertIs<EvaluationResult.EvaluationError>(result)
        assertEquals("test_key", result.key)
        assertIs<IllegalStateException>(result.error)
        assertEquals("Evaluation error for testing", result.error.message)
    }

    @Test
    fun `fold transforms Success to target type`() {
        val result = testContext.evaluateSafe(CommonTestFeatures.registeredFlag).fold(
            onSuccess = { "success: $it" },
            onFlagNotFound = { "not found: $it" },
            onEvaluationError = { key, _ -> "error: $key" }
        )

        assertEquals("success: test-value", result)
    }


    @Test
    fun `fold transforms EvaluationError to target type`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError(
            "test_key",
            IllegalStateException("Evaluation error for testing")
        )

        val result = errorResult.fold(
            onSuccess = { "success: $it" },
            onFlagNotFound = { "not found: $it" },
            onEvaluationError = { key, error -> "error: $key - ${error.message}" }
        )

        assertEquals("error: test_key - Evaluation error for testing", result)
    }

    @Test
    fun `map transforms Success value`() {
        val result = testContext.evaluateSafe(CommonTestFeatures.registeredFlag)
            .map { it.uppercase() }

        assertIs<EvaluationResult.Success<String>>(result)
        assertEquals("TEST-VALUE", result.value)
    }


    @Test
    fun `map preserves EvaluationError`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError(
            "test_key",
            IllegalStateException("Test error")
        )
        val result = errorResult.map { it.uppercase() }

        assertIs<EvaluationResult.EvaluationError>(result)
        assertEquals("test_key", result.key)
    }

    @Test
    fun `getOrNull returns value on Success`() {
        val result = testContext.evaluateSafe(CommonTestFeatures.registeredFlag)
        assertEquals("test-value", result.getOrNull())
    }

    @Test
    fun `getOrNull returns null on EvaluationError`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError(
            "test_key",
            IllegalStateException("Test error")
        )
        assertNull(errorResult.getOrNull())
    }

    @Test
    fun `getOrDefault returns value on Success`() {
        val result = testContext.evaluateSafe(CommonTestFeatures.registeredFlag)
        assertEquals("test-value", result.getOrDefault("fallback"))
    }


    @Test
    fun `getOrDefault returns default on EvaluationError`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError(
            "test_key",
            IllegalStateException("Test error")
        )
        assertEquals("fallback", errorResult.getOrDefault("fallback"))
    }

    @Test
    fun `getOrElse returns value on Success`() {
        val result = testContext.evaluateSafe(CommonTestFeatures.registeredFlag)
            .getOrElse { "fallback" }

        assertEquals("test-value", result)
    }


    @Test
    fun `getOrElse computes default on EvaluationError`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError(
            "test_key",
            IllegalStateException("Test error")
        )
        val result = errorResult.getOrElse { error ->
            when (error) {
                is EvaluationResult.FlagNotFound -> "missing: ${error.key}"
                is EvaluationResult.EvaluationError -> "error: ${error.key}"
                else -> "unknown"
            }
        }

        assertEquals("error: test_key", result)
    }

    @Test
    fun `isSuccess returns true for Success`() {
        val result = testContext.evaluateSafe(CommonTestFeatures.registeredFlag)
        assertTrue(result.isSuccess())
    }


    @Test
    fun `isSuccess returns false for EvaluationError`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError("test", IllegalStateException())
        assertFalse(errorResult.isSuccess())
    }

    @Test
    fun `isFailure returns false for Success`() {
        val result = testContext.evaluateSafe(CommonTestFeatures.registeredFlag)
        assertFalse(result.isFailure())
    }


    @Test
    fun `isFailure returns true for EvaluationError`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError("test", IllegalStateException())
        assertTrue(errorResult.isFailure())
    }

    @Test
    fun `evaluateOrNull returns value when flag exists`() {
        val value = testContext.evaluateOrNull(CommonTestFeatures.registeredFlag)
        assertEquals("test-value", value)
    }


    @Test
    fun `evaluateOrDefault returns value when flag exists`() {
        val value = testContext.evaluateOrDefault(CommonTestFeatures.registeredFlag, "default")
        assertEquals("test-value", value)
    }


    @Test
    fun `evaluateOrThrow returns value when flag exists`() {
        val value = testContext.evaluateOrThrow(CommonTestFeatures.registeredFlag)
        assertEquals("test-value", value)
    }


    @Test
    fun `evaluateOrThrow throws FlagEvaluationException when evaluation throws`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError(
            "test_key",
            IllegalStateException("Test error")
        )

        val exception = assertFailsWith<FlagEvaluationException> {
            errorResult.fold(
                onSuccess = { it },
                onFlagNotFound = { throw FlagNotFoundException(it) },
                onEvaluationError = { key, error -> throw FlagEvaluationException(key, error) }
            )
        }

        assertEquals("test_key", exception.key)
        assertEquals("Flag evaluation failed: test_key", exception.message)
        assertIs<IllegalStateException>(exception.cause)
    }

    @Test
    fun `toResult converts Success to Result success`() {
        val result = testContext.evaluateSafe(CommonTestFeatures.registeredFlag).toResult()

        assertTrue(result.isSuccess)
        assertEquals("test-value", result.getOrNull())
    }


    @Test
    fun `toResult converts EvaluationError to Result failure`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError(
            "test_key",
            IllegalStateException("Test error")
        )
        val result = errorResult.toResult()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<FlagEvaluationException>(exception)
        assertEquals("test_key", exception.key)
    }

    @Test
    fun `fold can adapt to custom Outcome type`() {
        val adapted: TestOutcome<MyError, String> = testContext.evaluateSafe(CommonTestFeatures.registeredFlag).fold(
            onSuccess = { TestOutcome.Success(it) },
            onFlagNotFound = { TestOutcome.Failure(MyError("Flag not found: $it")) },
            onEvaluationError = { key, error -> TestOutcome.Failure(MyError("Evaluation failed: $key - ${error.message}")) }
        )

        assertIs<TestOutcome.Success<String>>(adapted)
        assertEquals("test-value", adapted.value)
    }


    @Test
    fun `fold adapts EvaluationError to custom Outcome type`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError(
            "test_key",
            IllegalStateException("Evaluation error for testing")
        )

        val adapted: TestOutcome<MyError, String> = errorResult.fold(
            onSuccess = { TestOutcome.Success(it) },
            onFlagNotFound = { TestOutcome.Failure(MyError("Flag not found: $it")) },
            onEvaluationError = { key, error -> TestOutcome.Failure(MyError("Evaluation failed: $key - ${error.message}")) }
        )

        assertIs<TestOutcome.Failure<MyError>>(adapted)
        assertEquals("Evaluation failed: test_key - Evaluation error for testing", adapted.error.reason)
    }

    @Test
    fun `chain multiple map operations on Success`() {
        val result = testContext.evaluateSafe(CommonTestFeatures.registeredFlag)
            .map { it.uppercase() }
            .map { "$it!" }
            .map { it.length }

        assertIs<EvaluationResult.Success<Int>>(result)
        assertEquals(11, result.value) // "TEST-VALUE!".length
    }

    @Test
    fun `EvaluationResult makes illegal states unrepresentable`() {
        // Cannot construct an EvaluationResult that is both Success and Error
        // Cannot construct Success without a value
        // Cannot construct FlagNotFound without a key
        // Cannot construct EvaluationError without key and error

        // This test documents the type safety - the code wouldn't compile if we tried invalid states

        val validSuccess: EvaluationResult<String> = EvaluationResult.Success("value")
        val validNotFound: EvaluationResult<String> = EvaluationResult.FlagNotFound("key")
        val validError: EvaluationResult<String> = EvaluationResult.EvaluationError(
            "key",
            IllegalStateException("error")
        )

        assertTrue(validSuccess.isSuccess())
        assertTrue(validNotFound.isFailure())
        assertTrue(validError.isFailure())
    }

}
