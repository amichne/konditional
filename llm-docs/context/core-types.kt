# Core Type Signatures
# Extracted: 2025-12-30T20:17:05-05:00
# Source: src/main/kotlin/io/amichne/konditional/

/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/context/LocaleTag.kt:10:interface LocaleTag {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/context/Context.kt:37:interface Context {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/context/Version.kt:9:data class Version(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/context/PlatformTag.kt:10:interface PlatformTag {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValues.kt:35:class AxisValues internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/context/axis/Axis.kt:47:abstract class Axis<T>(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValue.kt:30:interface AxisValue<T> where T : Enum<T> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/types/KotlinEncodeable.kt:61:interface KotlinEncodeable<out S> where S : JsonSchema<*>, S : ObjectTraits {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:43:fun KotlinEncodeable<*>.toJsonValue(schema: JsonSchema<*>? = null): JsonObject =
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:186:fun JsonValue.toPrimitiveValue(): Any? = when (this) {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:55:fun <T> ParseResult<T>.getOrNull(): T? = when (this) {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:63:fun <T> ParseResult<T>.getOrDefault(default: T): T = when (this) {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:79:fun <T> ParseResult<T>.isSuccess(): Boolean = this is ParseResult.Success
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:84:fun <T> ParseResult<T>.isFailure(): Boolean = this is ParseResult.Failure
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:139:fun <T> ParseResult<T>.toResult(): Result<T> = fold(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt:11:sealed interface ParseError {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:15:sealed interface ParseResult<out T> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:33:fun <T> ParseResult<T>.getOrThrow(): T = when (this) {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/result/ParseException.kt:7:class ParseException(val error: ParseError) : Exception(error.message)
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/features/Identifiable.kt:6:interface Identifiable {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/features/KotlinClassFeature.kt:38:sealed interface KotlinClassFeature<T : KotlinEncodeable<*>, C : Context, M : Namespace> :
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/features/EnumFeature.kt:14:sealed interface EnumFeature<E : Enum<E>, C : Context, M : Namespace> :
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/features/BooleanFeature.kt:6:sealed interface BooleanFeature<C : Context, M : Namespace> : Feature<Boolean, C, M> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/features/DoubleFeature.kt:6:sealed interface DoubleFeature<C : Context, M : Namespace> :
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/features/StringFeature.kt:6:sealed interface StringFeature<C : Context, M : Namespace> : Feature<String, C, M> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/features/Feature.kt:36:sealed interface Feature<T : Any, C : Context, out M : Namespace> : Identifiable {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/features/IntFeature.kt:6:sealed interface IntFeature<C : Context, M : Namespace> : Feature<Int, C, M> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt:69:open class Namespace(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt:27:data class FlagDefinition<T : Any, C : Context, M : Namespace> internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigValue.kt:6:sealed interface ConfigValue {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationPatch.kt:21:data class ConfigurationPatch internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/instance/Configuration.kt:7:data class Configuration internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationMetadata.kt:18:data class ConfigurationMetadata internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationDiff.kt:13:data class ConfigurationDiff internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:72:interface NamespaceRegistry {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/ops/Metrics.kt:3:object Metrics {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/ops/RegistryHooks.kt:11:data class RegistryHooks internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt:3:interface MetricsCollector {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/ops/KonditionalLogger.kt:3:interface KonditionalLogger {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/id/StableId.kt:15:sealed interface StableId {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/dsl/VersionRangeScope.kt:21:interface VersionRangeScope {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:35:interface RuleScope<C : Context> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/dsl/FlagScope.kt:30:interface FlagScope<T : Any, C : Context> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt:39:interface AxisValuesScope {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/options/UnknownFeatureKeyStrategy.kt:3:sealed interface UnknownFeatureKeyStrategy {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/options/SnapshotWarning.kt:6:data class SnapshotWarning internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/options/SnapshotLoadOptions.kt:10:data class SnapshotLoadOptions(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/Serializer.kt:54:interface Serializer<T> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/JsonObjectBuilder.kt:42:class JsonObjectBuilder {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/JsonObjectBuilder.kt:70:fun jsonObject(builder: JsonObjectBuilder.() -> Unit): JsonObject {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/JsonObjectBuilder.kt:82:fun jsonArray(vararg values: Any?): JsonArray {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/JsonObjectBuilder.kt:120:fun JsonValue.asInt(): Int? = when (this) {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/JsonObjectBuilder.kt:129:fun JsonValue.asDouble(): Double? = when (this) {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/JsonObjectBuilder.kt:138:fun JsonValue.asString(): String? = when (this) {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/JsonObjectBuilder.kt:147:fun JsonValue.asBoolean(): Boolean? = when (this) {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/JsonObjectBuilder.kt:156:fun JsonValue.asObject(): JsonObject? = this as? JsonObject
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/JsonObjectBuilder.kt:162:fun JsonValue.asArray(): JsonArray? = this as? JsonArray
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/SnapshotSerializer.kt:46:object SnapshotSerializer {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/TypeSerializer.kt:81:interface TypeSerializer<T : Any> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/SerializerRegistry.kt:62:object SerializerRegistry {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/serialization/NamespaceSnapshotSerializer.kt:65:class NamespaceSnapshotSerializer<M : Namespace>(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableSnapshotMetadata.kt:6:data class SerializableSnapshotMetadata(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableFlag.kt:13:data class SerializableFlag(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/internal/serialization/models/FlagValue.kt:21:sealed class FlagValue<out T : Any> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializablePatch.kt:11:data class SerializablePatch(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableRule.kt:13:data class SerializableRule(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableSnapshot.kt:10:data class SerializableSnapshot(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/rules/Rule.kt:66:data class Rule<C : Context> internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/rules/versions/RightBound.kt:5:data class RightBound(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/rules/versions/LeftBound.kt:5:data class LeftBound(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/rules/versions/FullyBound.kt:5:data class FullyBound(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/rules/versions/VersionRange.kt:6:sealed class VersionRange(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/rules/versions/Unbounded.kt:5:class Unbounded : VersionRange(Type.UNBOUNDED, MIN_VERSION, MAX_VERSION) {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/rules/evaluable/Predicate.kt:22:fun interface Predicate<in C : Context> : Specifier {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/rules/evaluable/Specifier.kt:3:interface Specifier {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/rules/evaluable/Placeholder.kt:5:object Placeholder : Predicate<Context> {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt:13:object RampUpBucketing {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:25:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:53:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.explain(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:83:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithReason(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:10:data class ShadowOptions internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:23:data class ShadowMismatch<T : Any> internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:41:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithShadow(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:88:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateShadow(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/RolloutBucketing.kt:7:data class BucketInfo internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/RolloutBucketing.kt:23:object RolloutBucketing {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/api/EvaluationResult.kt:15:data class EvaluationResult<T : Any> internal constructor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/UiHints.kt:3:data class UiHints(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/StringConstraintsDescriptor.kt:3:data class StringConstraintsDescriptor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/EnumOptionsDescriptor.kt:3:data class EnumOptionsDescriptor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/ConfigurationStateSupportedValuesCatalog.kt:6:object ConfigurationStateSupportedValuesCatalog {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/SemverConstraintsDescriptor.kt:3:data class SemverConstraintsDescriptor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/ConfigurationStateBindings.kt:11:object ConfigurationStateBindings {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/NumberRangeDescriptor.kt:3:data class NumberRangeDescriptor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/Option.kt:3:data class Option(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/BooleanDescriptor.kt:3:data class BooleanDescriptor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/ConfigurationStateResponse.kt:10:data class ConfigurationStateResponse(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/ConfigurationStateFactory.kt:6:object ConfigurationStateFactory {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/MapConstraintsDescriptor.kt:3:data class MapConstraintsDescriptor(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/FieldDescriptor.kt:3:sealed interface FieldDescriptor {
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/SupportedValues.kt:11:data class SupportedValues(
/Users/amichne/code/konditional/core/src/main/kotlin/io/amichne/konditional/configstate/SchemaRefDescriptor.kt:3:data class SchemaRefDescriptor(

# --- End of extraction ---

# Kontracts Submodule Types
/Users/amichne/code/konditional/kontracts/src/test/kotlin/io/amichne/kontracts/CustomTypeMappingTest.kt:23:class CustomTypeMappingTest {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonValue.kt:18:sealed class JsonValue {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonString.kt:11:data class JsonString(val value: String) : JsonValue() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonArray.kt:13:data class JsonArray(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonBoolean.kt:11:data class JsonBoolean(val value: Boolean) : JsonValue() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonObject.kt:13:data class JsonObject(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonNumber.kt:11:data class JsonNumber(val value: Double) : JsonValue() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonNull.kt:11:object JsonNull : JsonValue() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/MapSchema.kt:6:data class MapSchema<V : Any>(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/FieldSchema.kt:9:data class FieldSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/StringSchema.kt:7:data class StringSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/OneOfSchema.kt:6:data class OneOfSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/DoubleSchema.kt:7:data class DoubleSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/ValidationResult.kt:8:sealed class ValidationResult {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/NullSchema.kt:6:data class NullSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/JsonSchema.kt:6:sealed class JsonSchema<out T : Any> : OpenApiProps<T> {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/ObjectTraits.kt:3:interface ObjectTraits {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/AnySchema.kt:6:data class AnySchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/OpenApiProps.kt:6:interface OpenApiProps<out T : Any> {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/ArraySchema.kt:7:data class ArraySchema<E : Any>(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/BooleanSchema.kt:6:data class BooleanSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/ObjectSchema.kt:7:data class ObjectSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/IntSchema.kt:7:data class IntSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/EnumSchema.kt:11:data class EnumSchema<E : Enum<E>>(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/RootObjectSchema.kt:3:data class RootObjectSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/BooleanSchemaBuilder.kt:6:open class BooleanSchemaBuilder : JsonSchemaBuilder<Boolean> {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/IntSchemaBuilder.kt:6:open class IntSchemaBuilder : JsonSchemaBuilder<Int> {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/EnumSchemaBuilder.kt:7:class EnumSchemaBuilder<E : Enum<E>>(private val enumClass: KClass<E>) : JsonSchemaBuilder<E> {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaBuilder.kt:5:sealed interface JsonSchemaBuilder<out T : Any> {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/RootObjectSchemaBuilder.kt:8:class RootObjectSchemaBuilder {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/DoubleSchemaBuilder.kt:6:open class DoubleSchemaBuilder : JsonSchemaBuilder<Double> {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/NullSchemaBuilder.kt:6:class NullSchemaBuilder : JsonSchemaBuilder<Any> {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/StringSchemaBuilder.kt:6:open class StringSchemaBuilder : JsonSchemaBuilder<String> {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaBuilders.kt:186:fun schemaRoot(builder: RootObjectSchemaBuilder.() -> Unit) = RootObjectSchemaBuilder().apply(builder).build()
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomBooleanSchemaBuilder.kt:10:class CustomBooleanSchemaBuilder<V : Any> : BooleanSchemaBuilder() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomDoubleSchemaBuilder.kt:10:class CustomDoubleSchemaBuilder<V : Any> : DoubleSchemaBuilder() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomStringSchemaBuilder.kt:11:class CustomStringSchemaBuilder<V : Any> : StringSchemaBuilder() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomIntSchemaBuilder.kt:10:class CustomIntSchemaBuilder<V : Any> : IntSchemaBuilder() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ObjectSchemaBuilder.kt:7:class ObjectSchemaBuilder : JsonSchemaBuilder<Map<String, Any?>> {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ArraySchemaBuilder.kt:7:class ArraySchemaBuilder : JsonSchemaBuilder<List<Any>> {
