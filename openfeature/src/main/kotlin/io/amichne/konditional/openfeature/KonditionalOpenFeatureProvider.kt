@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.openfeature

import dev.openfeature.sdk.ErrorCode
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.FeatureProvider
import dev.openfeature.sdk.ImmutableMetadata
import dev.openfeature.sdk.Metadata
import dev.openfeature.sdk.ProviderEvaluation
import dev.openfeature.sdk.ProviderState
import dev.openfeature.sdk.Reason
import dev.openfeature.sdk.Value
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.api.evaluateInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.ops.Metrics
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.registry.NamespaceRegistry
import io.amichne.konditional.internal.evaluation.EvaluationDiagnostics

/**
 * Maps untrusted OpenFeature [EvaluationContext] to a trusted Konditional [Context].
 *
 * Implementations must not throw for control flow. Parse failures are returned as
 * [KonditionalContextMappingResult.Failure] with a typed [KonditionalContextMappingError].
 */
fun interface KonditionalContextMapper<C : Context> {
    /**
     * Converts [context] into a typed Konditional context.
     */
    fun toKonditionalContext(context: EvaluationContext): KonditionalContextMappingResult<C>
}

/**
 * Result of converting an OpenFeature context into a trusted Konditional context.
 */
sealed interface KonditionalContextMappingResult<out C : Context> {
    /**
     * Successful mapping with a trusted domain context.
     */
    @ConsistentCopyVisibility
    data class Success<C : Context> @PublishedApi internal constructor(
        val value: C,
    ) : KonditionalContextMappingResult<C>

    /**
     * Failed mapping with a typed boundary error.
     */
    @ConsistentCopyVisibility
    data class Failure @PublishedApi internal constructor(
        val error: KonditionalContextMappingError,
    ) : KonditionalContextMappingResult<Nothing>

    companion object {
        fun <C : Context> success(value: C): KonditionalContextMappingResult<C> = Success(value)

        fun failure(error: KonditionalContextMappingError): KonditionalContextMappingResult<Nothing> = Failure(error)
    }
}

/**
 * Typed errors produced during OpenFeature context mapping.
 */
sealed interface KonditionalContextMappingError {
    val message: String

    /**
     * OpenFeature did not provide a targeting key.
     */
    object MissingTargetingKey : KonditionalContextMappingError {
        override val message: String = "OpenFeature targetingKey is required for Konditional evaluation"
    }

    /**
     * OpenFeature provided a targeting key that contained only whitespace.
     */
    object BlankTargetingKey : KonditionalContextMappingError {
        override val message: String = "OpenFeature targetingKey must not be blank for Konditional evaluation"
    }
}

data class KonditionalProviderMetadata(private val providerName: String = "Konditional") : Metadata {
    override fun getName(): String = providerName
}

data class TargetingKeyContext(
    override val stableId: StableId,
    override val axisValues: AxisValues = AxisValues.EMPTY,
) : Context, Context.StableIdContext

class TargetingKeyContextMapper(
    private val axisValuesProvider: (EvaluationContext) -> AxisValues = { AxisValues.EMPTY },
) : KonditionalContextMapper<TargetingKeyContext> {
    override fun toKonditionalContext(context: EvaluationContext): KonditionalContextMappingResult<TargetingKeyContext> =
        when (val targetingKey = context.targetingKey) {
            null -> KonditionalContextMappingResult.failure(KonditionalContextMappingError.MissingTargetingKey)
            else ->
                targetingKey
                    .takeIf { it.isNotBlank() }
                    ?.let { normalizedTargetingKey ->
                        KonditionalContextMappingResult.success(
                            TargetingKeyContext(
                                stableId = StableId.of(normalizedTargetingKey),
                                axisValues = axisValuesProvider(context),
                            ),
                        )
                    }
                    ?: KonditionalContextMappingResult.failure(KonditionalContextMappingError.BlankTargetingKey)
        }
}

class KonditionalOpenFeatureProvider<C : Context>(
    private val namespaceRegistry: NamespaceRegistry,
    private val contextMapper: KonditionalContextMapper<C>,
    private val metadata: Metadata = KonditionalProviderMetadata(),
) : FeatureProvider {
    private val flagsByKey: Map<String, FlagEntry<C>> =
        namespaceRegistry
            .allFlags()
            .entries
            .asSequence()
            .sortedBy { entry -> entry.key.key }
            .fold(linkedMapOf<String, FlagEntry<C>>()) { index, entry ->
                index.putIfAbsent(
                    entry.key.key,
                    FlagEntry(
                        feature = entry.key.toTypedFeature(),
                        valueType = FlagValueType.of(entry.value.defaultValue),
                    ),
                )
                index
            }

    override fun getMetadata(): Metadata = metadata

    override fun getState(): ProviderState = ProviderState.READY

    override fun getBooleanEvaluation(
        key: String,
        defaultValue: Boolean,
        ctx: EvaluationContext,
    ): ProviderEvaluation<Boolean> =
        evaluateTyped(
            key = key,
            defaultValue = defaultValue,
            ctx = ctx,
            expectedType = FlagValueType.BOOLEAN,
            transformValue = { value -> value as? Boolean },
        )

    override fun getStringEvaluation(
        key: String,
        defaultValue: String,
        ctx: EvaluationContext,
    ): ProviderEvaluation<String> =
        evaluateTyped(
            key = key,
            defaultValue = defaultValue,
            ctx = ctx,
            expectedType = FlagValueType.STRING,
            transformValue = { value -> value as? String },
        )

    override fun getIntegerEvaluation(
        key: String,
        defaultValue: Int,
        ctx: EvaluationContext,
    ): ProviderEvaluation<Int> =
        evaluateTyped(
            key = key,
            defaultValue = defaultValue,
            ctx = ctx,
            expectedType = FlagValueType.INTEGER,
            transformValue = { value -> value as? Int },
        )

    override fun getDoubleEvaluation(
        key: String,
        defaultValue: Double,
        ctx: EvaluationContext,
    ): ProviderEvaluation<Double> =
        evaluateTyped(
            key = key,
            defaultValue = defaultValue,
            ctx = ctx,
            expectedType = FlagValueType.DOUBLE,
            transformValue = { value -> value as? Double },
        )

    override fun getObjectEvaluation(
        key: String,
        defaultValue: Value,
        ctx: EvaluationContext,
    ): ProviderEvaluation<Value> =
        evaluateTyped(
            key = key,
            defaultValue = defaultValue,
            ctx = ctx,
            expectedType = FlagValueType.OBJECT,
            transformValue = { value -> runCatching { Value.objectToValue(value) }.getOrNull() },
        )

    private fun <T : Any> evaluateTyped(
        key: String,
        defaultValue: T,
        ctx: EvaluationContext,
        expectedType: FlagValueType,
        transformValue: (Any) -> T?,
    ): ProviderEvaluation<T> =
        resolveFlagEntry(key).let { entry ->
            when {
                entry == null -> errorEvaluation(
                    defaultValue = defaultValue,
                    errorCode = ErrorCode.FLAG_NOT_FOUND,
                    errorMessage = "Flag not found: $key",
                )

                entry.valueType != expectedType -> errorEvaluation(
                    defaultValue = defaultValue,
                    errorCode = ErrorCode.TYPE_MISMATCH,
                    errorMessage = "Flag '$key' is ${entry.valueType.displayName()} but $expectedType was requested",
                )

                else -> evaluateEntry(
                    key = key,
                    entry = entry,
                    defaultValue = defaultValue,
                    ctx = ctx,
                    transformValue = transformValue,
                )
            }
        }

    private fun <T : Any> evaluateEntry(
        key: String,
        entry: FlagEntry<C>,
        defaultValue: T,
        ctx: EvaluationContext,
        transformValue: (Any) -> T?,
    ): ProviderEvaluation<T> =
        when (val mappedContext = contextMapper.toKonditionalContext(ctx)) {
            is KonditionalContextMappingResult.Failure ->
                errorEvaluation(
                    defaultValue = defaultValue,
                    errorCode = ErrorCode.INVALID_CONTEXT,
                    errorMessage = mappedContext.error.message,
                )

            is KonditionalContextMappingResult.Success ->
                evaluateMappedEntry(
                    key = key,
                    entry = entry,
                    context = mappedContext.value,
                    defaultValue = defaultValue,
                    transformValue = transformValue,
                )
        }

    private fun <T : Any> evaluateMappedEntry(
        key: String,
        entry: FlagEntry<C>,
        context: C,
        defaultValue: T,
        transformValue: (Any) -> T?,
    ): ProviderEvaluation<T> =
        runCatching {
            entry.featureAs<T>().evaluateInternalApi(
                context = context,
                registry = namespaceRegistry,
                mode = Metrics.Evaluation.EvaluationMode.EXPLAIN,
            )
        }
            .fold(
                onSuccess = { result ->
                    transformValue(result.value)?.let { value ->
                        ProviderEvaluation.builder<T>()
                            .value(value)
                            .reason(reasonFor(result.decision).name)
                            .variantOrNull(variantFor(result.decision))
                            .flagMetadata(metadataFor(result))
                            .build()
                    }
                        ?: errorEvaluation(
                            defaultValue = defaultValue,
                            errorCode = ErrorCode.TYPE_MISMATCH,
                            errorMessage = "Flag '$key' produced a value of an unexpected type",
                        )
                },
                onFailure = { error ->
                    errorEvaluation(
                        defaultValue = defaultValue,
                        errorCode = ErrorCode.GENERAL,
                        errorMessage = error.message ?: "Failed to evaluate flag '$key'",
                        )
                },
            )

    private fun reasonFor(decision: EvaluationDiagnostics.Decision): Reason =
        when (decision) {
            is EvaluationDiagnostics.Decision.RegistryDisabled -> Reason.DISABLED
            is EvaluationDiagnostics.Decision.Inactive -> Reason.DISABLED
            is EvaluationDiagnostics.Decision.Rule -> Reason.TARGETING_MATCH
            is EvaluationDiagnostics.Decision.Default -> Reason.DEFAULT
        }

    private fun variantFor(decision: EvaluationDiagnostics.Decision): String? =
        when (decision) {
            is EvaluationDiagnostics.Decision.RegistryDisabled -> "registry-disabled"
            is EvaluationDiagnostics.Decision.Inactive -> "inactive"
            is EvaluationDiagnostics.Decision.Rule -> decision.matched.rule.note ?: "rule"
            is EvaluationDiagnostics.Decision.Default -> "default"
        }

    private fun metadataFor(result: EvaluationDiagnostics<*>): ImmutableMetadata =
        ImmutableMetadata.builder()
            .addString("konditional.namespace", result.namespaceId)
            .addString("konditional.featureKey", result.featureKey)
            .addStringIfNotNull("konditional.configVersion", result.configVersion)
            .addString("konditional.decision", result.decision::class.simpleName ?: "unknown")
            .addDecisionMetadata(result.decision)
            .build()

    private fun ImmutableMetadata.ImmutableMetadataBuilder.addDecisionMetadata(
        decision: EvaluationDiagnostics.Decision,
    ): ImmutableMetadata.ImmutableMetadataBuilder =
        when (decision) {
            is EvaluationDiagnostics.Decision.Rule ->
                addInteger("konditional.rule.specificity", decision.matched.rule.totalSpecificity)
                    .addStringIfNotNull("konditional.rule.note", decision.matched.rule.note)
                    .addInteger("konditional.bucket", decision.matched.bucket.bucket)

            is EvaluationDiagnostics.Decision.Default ->
                addIntegerIfNotNull(
                    "konditional.bucket",
                    decision.skippedByRollout?.bucket?.bucket,
                )

            is EvaluationDiagnostics.Decision.RegistryDisabled -> this
            is EvaluationDiagnostics.Decision.Inactive -> this
        }

    private fun ImmutableMetadata.ImmutableMetadataBuilder.addStringIfNotNull(
        key: String,
        value: String?,
    ): ImmutableMetadata.ImmutableMetadataBuilder =
        value?.let { addString(key, it) } ?: this

    private fun ImmutableMetadata.ImmutableMetadataBuilder.addIntegerIfNotNull(
        key: String,
        value: Int?,
    ): ImmutableMetadata.ImmutableMetadataBuilder =
        value?.let { addInteger(key, it) } ?: this

    private fun <T : Any> errorEvaluation(
        defaultValue: T,
        errorCode: ErrorCode,
        errorMessage: String,
    ): ProviderEvaluation<T> =
        ProviderEvaluation.builder<T>()
            .value(defaultValue)
            .reason(Reason.ERROR.name)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .build()

    private fun resolveFlagEntry(flagKey: String): FlagEntry<C>? = flagsByKey[flagKey]

    @Suppress("UNCHECKED_CAST")
    private fun Feature<*, *, *>.toTypedFeature(): Feature<*, C, *> = this as Feature<*, C, *>

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> FlagEntry<C>.featureAs(): Feature<T, C, Namespace> =
        feature as Feature<T, C, Namespace>

    private fun <T> ProviderEvaluation.ProviderEvaluationBuilder<T>.variantOrNull(
        variant: String?,
    ): ProviderEvaluation.ProviderEvaluationBuilder<T> =
        variant?.let { variant(it) } ?: this

    private enum class FlagValueType {
        BOOLEAN,
        STRING,
        INTEGER,
        DOUBLE,
        OBJECT,
        ;

        fun displayName(): String =
            when (this) {
                BOOLEAN -> "boolean"
                STRING -> "string"
                INTEGER -> "integer"
                DOUBLE -> "double"
                OBJECT -> "object"
            }

        companion object {
            fun of(value: Any): FlagValueType =
                when (value) {
                    is Boolean -> BOOLEAN
                    is String -> STRING
                    is Int -> INTEGER
                    is Double -> DOUBLE
                    else -> OBJECT
                }
        }
    }

    private data class FlagEntry<C : Context>(
        val feature: Feature<*, C, *>,
        val valueType: FlagValueType,
    )
}
