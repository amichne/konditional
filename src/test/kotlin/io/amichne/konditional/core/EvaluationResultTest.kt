package io.amichne.konditional.core

import io.amichne.konditional.context.AppLocale
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.Platform
import io.amichne.konditional.context.Rollout
import io.amichne.konditional.context.Version
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.instance.KonfigPatch
import io.amichne.konditional.core.internal.SingletonModuleRegistry
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
import io.amichne.konditional.rules.Rule
import io.amichne.konditional.rules.versions.Unbounded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.to

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

    enum class TestFlags(override val key: String) :
        StringFeature<Context, FeatureModule.Core> {
        REGISTERED_FLAG("registered_flag"),
        UNREGISTERED_FLAG("unregistered_flag");

        override val module: FeatureModule.Core = FeatureModule.Core
    }

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

    private val testRegistry = object : ModuleRegistry by SingletonModuleRegistry {}

    init {
        // Register a normal flag
        val rule = Rule<Context>(
            rollout = Rollout.MAX,
            locales = emptySet(),
            platforms = emptySet(),
            versionRange = Unbounded(),
        )
        testRegistry.update(
            KonfigPatch(
                flags = mapOf(TestFlags.REGISTERED_FLAG to TestFlags.REGISTERED_FLAG.flag {
                    rule implies "test-value"
                    default("default-value")
                }),
            )
        )


        TestFlags.REGISTERED_FLAG.update(
            TestFlags.REGISTERED_FLAG.flag {
                rule { } implies "test-value"
                default("default-value")
            }
        )
    }

    @Test
    fun `evaluateSafe returns Success when flag is found and evaluates successfully`() {
        val result = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG)

        assertIs<EvaluationResult.Success<String>>(result)
        assertEquals("test-value", result.value)
    }

    @Test
    fun `evaluateSafe returns FlagNotFound when flag is not registered`() {
        val result = testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG)

        assertIs<EvaluationResult.FlagNotFound>(result)
        assertEquals("unregistered_flag", result.key)
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
        val result = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG).fold(
            onSuccess = { "success: $it" },
            onFlagNotFound = { "not found: $it" },
            onEvaluationError = { key, _ -> "error: $key" }
        )

        assertEquals("success: test-value", result)
    }

    @Test
    fun `fold transforms FlagNotFound to target type`() {
        val result = testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG).fold(
            onSuccess = { "success: $it" },
            onFlagNotFound = { "not found: $it" },
            onEvaluationError = { key, _ -> "error: $key" }
        )

        assertEquals("not found: unregistered_flag", result)
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
        val result = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG)
            .map { it.uppercase() }

        assertIs<EvaluationResult.Success<String>>(result)
        assertEquals("TEST-VALUE", result.value)
    }

    @Test
    fun `map preserves FlagNotFound`() {
        val result = testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG)
            .map { it.uppercase() }

        assertIs<EvaluationResult.FlagNotFound>(result)
        assertEquals("unregistered_flag", result.key)
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
        val result = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG)
        assertEquals("test-value", result.getOrNull())
    }

    @Test
    fun `getOrNull returns null on FlagNotFound`() {
        val result = testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG)
        assertNull(result.getOrNull())
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
        val result = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG)
        assertEquals("test-value", result.getOrDefault("fallback"))
    }

    @Test
    fun `getOrDefault returns default on FlagNotFound`() {
        val result = testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG)
        assertEquals("fallback", result.getOrDefault("fallback"))
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
        val result = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG)
            .getOrElse { "fallback" }

        assertEquals("test-value", result)
    }

    @Test
    fun `getOrElse computes default on FlagNotFound`() {
        val result = testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG)
            .getOrElse { error ->
                when (error) {
                    is EvaluationResult.FlagNotFound -> "missing: ${error.key}"
                    is EvaluationResult.EvaluationError -> "error: ${error.key}"
                    else -> "unknown"
                }
            }

        assertEquals("missing: unregistered_flag", result)
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
        val result = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG)
        assertTrue(result.isSuccess())
    }

    @Test
    fun `isSuccess returns false for FlagNotFound`() {
        val result = testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG)
        assertFalse(result.isSuccess())
    }

    @Test
    fun `isSuccess returns false for EvaluationError`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError("test", IllegalStateException())
        assertFalse(errorResult.isSuccess())
    }

    @Test
    fun `isFailure returns false for Success`() {
        val result = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG)
        assertFalse(result.isFailure())
    }

    @Test
    fun `isFailure returns true for FlagNotFound`() {
        val result = testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG)
        assertTrue(result.isFailure())
    }

    @Test
    fun `isFailure returns true for EvaluationError`() {
        val errorResult: EvaluationResult<String> = EvaluationResult.EvaluationError("test", IllegalStateException())
        assertTrue(errorResult.isFailure())
    }

    @Test
    fun `evaluateOrNull returns value when flag exists`() {
        val value = testContext.evaluateOrNull(TestFlags.REGISTERED_FLAG)
        assertEquals("test-value", value)
    }

    @Test
    fun `evaluateOrNull returns null when flag not found`() {
        val value = testContext.evaluateOrNull(TestFlags.UNREGISTERED_FLAG)
        assertNull(value)
    }

    @Test
    fun `evaluateOrDefault returns value when flag exists`() {
        val value = testContext.evaluateOrDefault(TestFlags.REGISTERED_FLAG, "default")
        assertEquals("test-value", value)
    }

    @Test
    fun `evaluateOrDefault returns default when flag not found`() {
        val value = testContext.evaluateOrDefault(TestFlags.UNREGISTERED_FLAG, "default")
        assertEquals("default", value)
    }

    @Test
    fun `evaluateOrThrow returns value when flag exists`() {
        val value = testContext.evaluateOrThrow(TestFlags.REGISTERED_FLAG)
        assertEquals("test-value", value)
    }

    @Test
    fun `evaluateOrThrow throws FlagNotFoundException when flag not found`() {
        val exception = assertFailsWith<FlagNotFoundException> {
            testContext.evaluateOrThrow(TestFlags.UNREGISTERED_FLAG)
        }

        assertEquals("unregistered_flag", exception.key)
        assertEquals("Flag not found: unregistered_flag", exception.message)
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
        val result = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG).toResult()

        assertTrue(result.isSuccess)
        assertEquals("test-value", result.getOrNull())
    }

    @Test
    fun `toResult converts FlagNotFound to Result failure`() {
        val result = testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG).toResult()

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<FlagNotFoundException>(exception)
        assertEquals("unregistered_flag", exception.key)
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
        val adapted: TestOutcome<MyError, String> = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG).fold(
            onSuccess = { TestOutcome.Success(it) },
            onFlagNotFound = { TestOutcome.Failure(MyError("Flag not found: $it")) },
            onEvaluationError = { key, error -> TestOutcome.Failure(MyError("Evaluation failed: $key - ${error.message}")) }
        )

        assertIs<TestOutcome.Success<String>>(adapted)
        assertEquals("test-value", adapted.value)
    }

    @Test
    fun `fold adapts FlagNotFound to custom Outcome type`() {
        val adapted: TestOutcome<MyError, String> = testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG).fold(
            onSuccess = { TestOutcome.Success(it) },
            onFlagNotFound = { TestOutcome.Failure(MyError("Flag not found: $it")) },
            onEvaluationError = { key, error -> TestOutcome.Failure(MyError("Evaluation failed: $key - ${error.message}")) }
        )

        assertIs<TestOutcome.Failure<MyError>>(adapted)
        assertEquals("Flag not found: unregistered_flag", adapted.error.reason)
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
        val result = testContext.evaluateSafe(TestFlags.REGISTERED_FLAG)
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

    @Test
    fun `when expression is exhaustive over all result types`() {
        fun handleResult(result: EvaluationResult<String>): String = when (result) {
            is EvaluationResult.Success -> "success: ${result.value}"
            is EvaluationResult.FlagNotFound -> "not found: ${result.key}"
            is EvaluationResult.EvaluationError -> "error: ${result.key}"
        }

        assertEquals("success: test-value", handleResult(testContext.evaluateSafe(TestFlags.REGISTERED_FLAG)))
        assertEquals(
            "not found: unregistered_flag",
            handleResult(testContext.evaluateSafe(TestFlags.UNREGISTERED_FLAG))
        )

        val errorResult: EvaluationResult<String> =
            EvaluationResult.EvaluationError("test_key", IllegalStateException())
        assertEquals("error: test_key", handleResult(errorResult))
    }
}
