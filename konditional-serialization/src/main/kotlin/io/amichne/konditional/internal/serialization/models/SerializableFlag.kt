@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.core.schema.CompiledNamespaceSchema
import io.amichne.konditional.core.types.Konstrained
import io.amichne.konditional.core.types.asObjectSchema
import io.amichne.kontracts.schema.ObjectTraits
import io.amichne.konditional.internal.SerializedFlagDefinitionMetadata
import io.amichne.konditional.internal.SerializedFlagRuleSpec
import io.amichne.konditional.internal.flagDefinitionFromSerialized
import io.amichne.konditional.internal.toSerializedMetadata
import io.amichne.konditional.internal.toSerializedRules
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
        schema: CompiledNamespaceSchema,
    ): Result<Pair<Feature<*, *, *>, FlagDefinition<*, *, *>>> =
        resolveFeature(schema)
            .fold(
                onSuccess = { conditional ->
                    runCatching { conditional to toFlagDefinition(conditional = conditional) }
                        .fold(
                            onSuccess = { Result.success(it) },
                            onFailure = {
                                parseFailure(
                                    ParseError.InvalidSnapshot(
                                        it.message ?: "Failed to decode flag",
                                    ),
                                )
                            },
                        )
                },
                onFailure = { Result.failure(it) },
            )

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
        schema: CompiledNamespaceSchema,
    ): Result<Feature<*, *, *>> {
        val featuresById = schema.entriesById.mapValues { (_, entry) -> entry.feature }
        return when {
            featuresById.isEmpty() ->
                parseFailure(
                    ParseError.invalidSnapshot(
                        "Feature-aware decode requires explicit feature scope for key '$key'. " +
                            "Use ConfigurationSnapshotCodec.decode(json, featuresById, options).",
                    ),
                )

            else ->
                featuresById[key]?.let { Result.success(it) }
                    ?: parseFailure(ParseError.featureNotFound(key))
        }
    }

    private fun <T : Any, C : Context, M : Namespace> toFlagDefinition(
        conditional: Feature<T, C, M>,
    ): FlagDefinition<T, C, M> {
        val expectedSample =
            conditional.expectedDefaultValueOrNull()
                ?: conditional.declaredDefaultValueOrNull()

        return defaultValue
            .extractValue<T>(
                expectedSample = expectedSample,
            )
            .let { decodedDefault ->
                // Only extract and validate an ObjectSchema for object-backed Konstrained types.
                // Primitive/array-backed Konstrained do not use ObjectSchema validation.
                val schema =
                    (decodedDefault as? Konstrained<*>)
                        ?.schema
                        ?.takeIf { it is ObjectTraits }
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
