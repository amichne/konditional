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
import io.amichne.konditional.api.EvaluationResult
import io.amichne.konditional.api.explain
import io.amichne.konditional.context.Context
import io.amichne.konditional.context.axis.AxisValues
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.id.StableId
import io.amichne.konditional.core.registry.NamespaceRegistry

fun interface KonditionalContextMapper<C : Context> {
    fun toKonditionalContext(context: EvaluationContext): C
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
    override fun toKonditionalContext(context: EvaluationContext): TargetingKeyContext =
        (context.targetingKey as String?)
            ?.takeIf { it.isNotBlank() }
            ?.let { targetingKey ->
                TargetingKeyContext(
                    stableId = StableId.of(targetingKey),
                    axisValues = axisValuesProvider(context),
                )
            }
            ?: error("OpenFeature targetingKey is required for Konditional evaluation")
}

class KonditionalOpenFeatureProvider<C : Context>(
    private val namespaceRegistry: NamespaceRegistry,
    private val contextMapper: KonditionalContextMapper<C>,
    private val metadata: Metadata = KonditionalProviderMetadata(),
) : FeatureProvider {
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
        runCatching { contextMapper.toKonditionalContext(ctx) }
            .fold(
                onSuccess = { context ->
                    runCatching { entry.featureAs<T>().explain(context, namespaceRegistry) }
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
                },
                onFailure = { error ->
                    errorEvaluation(
                        defaultValue = defaultValue,
                        errorCode = ErrorCode.INVALID_CONTEXT,
                        errorMessage = error.message ?: "OpenFeature context mapping failed",
                    )
                },
            )

    private fun reasonFor(decision: EvaluationResult.Decision): Reason =
        when (decision) {
            is EvaluationResult.Decision.RegistryDisabled -> Reason.DISABLED
            is EvaluationResult.Decision.Inactive -> Reason.DISABLED
            is EvaluationResult.Decision.Rule -> Reason.TARGETING_MATCH
            is EvaluationResult.Decision.Default -> Reason.DEFAULT
        }

    private fun variantFor(decision: EvaluationResult.Decision): String? =
        when (decision) {
            is EvaluationResult.Decision.RegistryDisabled -> "registry-disabled"
            is EvaluationResult.Decision.Inactive -> "inactive"
            is EvaluationResult.Decision.Rule -> decision.matched.rule.note ?: "rule"
            is EvaluationResult.Decision.Default -> "default"
        }

    private fun metadataFor(result: EvaluationResult<*>): ImmutableMetadata =
        ImmutableMetadata.builder()
            .addString("konditional.namespace", result.namespaceId)
            .addString("konditional.featureKey", result.featureKey)
            .addStringIfNotNull("konditional.configVersion", result.configVersion)
            .addString("konditional.decision", result.decision::class.simpleName ?: "unknown")
            .addDecisionMetadata(result.decision)
            .build()

    private fun ImmutableMetadata.ImmutableMetadataBuilder.addDecisionMetadata(
        decision: EvaluationResult.Decision,
    ): ImmutableMetadata.ImmutableMetadataBuilder =
        when (decision) {
            is EvaluationResult.Decision.Rule ->
                addInteger("konditional.rule.specificity", decision.matched.rule.totalSpecificity)
                    .addStringIfNotNull("konditional.rule.note", decision.matched.rule.note)
                    .addInteger("konditional.bucket", decision.matched.bucket.bucket)

            is EvaluationResult.Decision.Default ->
                addIntegerIfNotNull(
                    "konditional.bucket",
                    decision.skippedByRollout?.bucket?.bucket,
                )

            is EvaluationResult.Decision.RegistryDisabled -> this
            is EvaluationResult.Decision.Inactive -> this
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

    private fun resolveFlagEntry(flagKey: String): FlagEntry<C>? =
        namespaceRegistry
            .allFlags()
            .entries
            .firstOrNull { entry -> entry.key.key == flagKey }
            ?.let { entry ->
                FlagEntry(
                    feature = entry.key.toTypedFeature(),
                    valueType = FlagValueType.of(entry.value.defaultValue),
                )
            }

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
