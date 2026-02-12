@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.ParseResult
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.core.types.asObjectSchema
import io.amichne.konditional.internal.SerializedFlagDefinitionMetadata
import io.amichne.konditional.internal.SerializedFlagRuleSpec
import io.amichne.konditional.internal.flagDefinitionFromSerialized
import io.amichne.konditional.internal.toSerializedMetadata
import io.amichne.konditional.internal.toSerializedRules
import io.amichne.konditional.serialization.FeatureRegistry
import io.amichne.konditional.values.FeatureId

/**
 * Serializable representation of a single flag configuration.
 * Contains all the data needed to reconstruct a FlagDefinition with its Feature.
 *
 * Now uses type-safe FlagValue instead create type-erased Any values.
 */
@KonditionalInternalApi
@JsonClass(generateAdapter = true)
data class SerializableFlag(
    val key: FeatureId,
    val defaultValue: FlagValue<*>,
    val salt: String = "v1",
    val isActive: Boolean = true,
    val rampUpAllowlist: Set<String> = emptySet(),
    val rules: List<SerializableRule> = emptyList(),
) {
    fun toFlagPair(
        featuresById: Map<FeatureId, Feature<*, *, *>> = emptyMap(),
    ): ParseResult<Pair<Feature<*, *, *>, FlagDefinition<*, *, *>>> =
        when (val conditionalResult = resolveFeature(featuresById)) {
            is ParseResult.Success -> {
                val conditional = conditionalResult.value
                runCatching {
                    toFlagDefinition(
                        conditional = conditional,
                        allowHintFallback = featuresById.isEmpty(),
                    )
                }
                    .fold(
                        onSuccess = { ParseResult.success(conditional to it) },
                        onFailure = {
                            ParseResult.failure(
                                ParseError.InvalidSnapshot(
                                    it.message ?: "Failed to decode flag"
                                )
                            )
                        },
                    )
            }

            is ParseResult.Failure -> ParseResult.failure(conditionalResult.error)
        }

    companion object {
        fun from(flagDefinition: FlagDefinition<*, *, *>, flagKey: FeatureId): SerializableFlag {
            val defaultValue = requireNotNull(flagDefinition.defaultValue) {
                "FlagDefinition must not hold a null defaultValue"
            }
            val metadata = flagDefinition.toSerializedMetadata()

            return SerializableFlag(
                key = flagKey,
                defaultValue = FlagValue.from(defaultValue),
                salt = metadata.salt,
                isActive = metadata.isActive,
                rampUpAllowlist = metadata.rampUpAllowlist,
                rules = flagDefinition.toSerializedRules().map { SerializableRule.fromSpec(it) },
            )
        }
    }

    private fun resolveFeature(
        featuresById: Map<FeatureId, Feature<*, *, *>>,
    ): ParseResult<Feature<*, *, *>> =
        if (featuresById.isEmpty()) {
            FeatureRegistry.get(key)
        } else {
            featuresById[key]?.let { ParseResult.success(it) }
                ?: ParseResult.failure(ParseError.featureNotFound(key))
        }

    private fun <T : Any, C : Context, M : Namespace> toFlagDefinition(
        conditional: Feature<T, C, M>,
        allowHintFallback: Boolean,
    ): FlagDefinition<T, C, M> {
        val expectedSample =
            conditional.expectedDefaultValueOrNull()
                ?: conditional.declaredDefaultValueOrNull()
                ?: if (allowHintFallback) FeatureRegistry.defaultSample(key) else null

        return defaultValue
            .extractValue<T>(
                expectedSample = expectedSample,
                allowHintFallback = allowHintFallback,
            )
            .let { decodedDefault ->
                val schema =
                    (decodedDefault as? Konstrained<*>)
                        ?.schema
                        ?.asObjectSchema()

                if (schema != null) {
                    defaultValue.validate(schema)
                }

                val ruleSpecs: List<SerializedFlagRuleSpec<T>> =
                    rules.map { rule ->
                        rule.toSpec(
                            rule.value.extractValue<T>(
                                expectedSample = decodedDefault,
                                schema = schema,
                                allowHintFallback = allowHintFallback,
                            ),
                        )
                    }

                flagDefinitionFromSerialized(
                    feature = conditional,
                    defaultValue = decodedDefault,
                    metadata =
                        SerializedFlagDefinitionMetadata(
                            salt = salt,
                            isActive = isActive,
                            rampUpAllowlist = rampUpAllowlist,
                        ),
                    rules = ruleSpecs,
                )
            }
    }

    private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.expectedDefaultValueOrNull(): T? =
        runCatching { namespace.flag(this).defaultValue }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.declaredDefaultValueOrNull(): T? =
        namespace.declaredDefault(this) as? T
}
