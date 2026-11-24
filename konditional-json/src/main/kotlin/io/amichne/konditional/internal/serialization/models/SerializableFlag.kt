@file:OptIn(KonditionalInternalApi::class)

package io.amichne.konditional.internal.serialization.models

import com.squareup.moshi.JsonClass
import io.amichne.konditional.api.KonditionalInternalApi
import io.amichne.konditional.context.Context
import io.amichne.konditional.core.FlagDefinition
import io.amichne.konditional.core.Namespace
import io.amichne.konditional.core.features.Feature
import io.amichne.konditional.core.result.KonditionalBoundaryFailure
import io.amichne.konditional.core.result.ParseError
import io.amichne.konditional.core.result.parseFailure
import io.amichne.konditional.core.schema.CompiledNamespaceSchema
import io.amichne.konditional.internal.SerializedFlagDefinitionMetadata
import io.amichne.konditional.internal.SerializedFlagRuleSpec
import io.amichne.konditional.internal.SerializedRuleValueType
import io.amichne.konditional.internal.flagDefinitionFromSerialized
import io.amichne.konditional.internal.toSerializedMetadata
import io.amichne.konditional.internal.toSerializedRules
import io.amichne.konditional.values.RuleId
import io.amichne.konditional.values.FeatureId

/**
 * Serializable representation of a single flag configuration.
 * Contains all the data needed to reconstruct a FlagDefinition with its Feature.
 *
 * Now uses type-safe FlagValue instead create type-erased Any values.
 */
@KonditionalInternalApi
@JsonClass(generateAdapter = true)
internal data class SerializableFlag(
    val key: FeatureId,
    val defaultValue: FlagValue<*>,
    val salt: String = "v1",
    val isActive: Boolean = true,
    val rampUpAllowlist: Set<String> = emptySet(),
    val rules: List<SerializableRule> = emptyList(),
) {
    fun toFlagPair(
        schema: CompiledNamespaceSchema,
    ): Result<Pair<Feature<*, *, *>, FlagDefinition<*, *, *>>> {
        val declaration = resolveDeclaration(schema).getOrElse { return Result.failure(it) }
        return try {
            toFlagDefinition(declaration).map { declaration.feature to it }
        } catch (failure: KonditionalBoundaryFailure) {
            Result.failure(failure)
        } catch (invalid: IllegalArgumentException) {
            // Object schema validation and rule construction can reject wire values.
            parseFailure(ParseError.invalidSnapshot(invalid.message ?: "Invalid value for feature '$key'"))
        }
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

    private fun resolveDeclaration(
        schema: CompiledNamespaceSchema,
    ): Result<FlagDefinition<*, *, *>> {
        return when {
            schema.entriesById.isEmpty() ->
                parseFailure(
                    ParseError.invalidSnapshot(
                        "Feature-aware decode requires explicit feature scope for key '$key'. " +
                            "Use ConfigurationCodec.decode(json, schema, options).",
                    ),
                )

            else ->
                schema.entriesById[key]?.declaredDefinition?.let { Result.success(it) }
                    ?: parseFailure(ParseError.featureNotFound(key))
        }
    }

    private fun <T : Any, C : Context, M : Namespace> toFlagDefinition(
        declaration: FlagDefinition<T, C, M>,
    ): Result<FlagDefinition<T, C, M>> {
        val expected = declaration.defaultValue
        val decodedDefault = defaultValue.extractValue(expected).getOrElse { return Result.failure(it) }
        val ruleSpecs: List<SerializedFlagRuleSpec<T>> = rules.mapIndexed { index, rule ->
            val fallbackId = RuleId.forFeatureRule(declaration.feature.id, index)
            when (rule.type) {
                SerializedRuleValueType.STATIC -> {
                    val value = rule.value.extractValue(expected).getOrElse { return Result.failure(it) }
                    rule.toSpec(value = value, fallbackRuleId = fallbackId)
                }
                SerializedRuleValueType.CONTEXTUAL -> return parseFailure(
                    ParseError.invalidSnapshot(
                        "Feature '$key' rule '${rule.ruleId ?: fallbackId}' is contextual and cannot be restored from JSON",
                    ),
                )
            }
        }
        val definition = flagDefinitionFromSerialized(
            feature = declaration.feature,
            defaultValue = decodedDefault,
            metadata = SerializedFlagDefinitionMetadata(
                salt = salt,
                isActive = isActive,
                rampUpAllowlist = rampUpAllowlist,
            ),
            rules = ruleSpecs,
        )
        return Result.success(definition)
    }
}
