# Core Type Signatures
# Extracted: 2025-12-17T17:28:13-05:00
# Source: src/main/kotlin/io/amichne/konditional/

/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/context/Context.kt:33:interface Context {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/context/ContextAware.kt:11:fun interface ContextAware<out C : Context> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/context/Version.kt:9:data class Version(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/context/axis/AxisValues.kt:34:class AxisValues internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/context/axis/Axis.kt:42:abstract class Axis<T>(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/context/axis/AxisValue.kt:28:interface AxisValue {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/types/KotlinEncodeable.kt:61:interface KotlinEncodeable<out S : JsonSchema> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:28:fun KotlinEncodeable<ObjectSchema>.toJsonValue(schema: ObjectSchema? = null): JsonObject = JsonObject(let {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/types/KotlinClassExtensions.kt:139:fun JsonValue.toPrimitiveValue(): Any? = when (this) {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:55:fun <T> ParseResult<T>.getOrNull(): T? = when (this) {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:63:fun <T> ParseResult<T>.getOrDefault(default: T): T = when (this) {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:79:fun <T> ParseResult<T>.isSuccess(): Boolean = this is ParseResult.Success
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:84:fun <T> ParseResult<T>.isFailure(): Boolean = this is ParseResult.Failure
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:139:fun <T> ParseResult<T>.toResult(): Result<T> = fold(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt:11:sealed interface ParseError {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:15:sealed interface ParseResult<out T> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:33:fun <T> ParseResult<T>.getOrThrow(): T = when (this) {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/result/ParseException.kt:7:class ParseException(val error: ParseError) : Exception(error.message)
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/features/Identifiable.kt:6:interface Identifiable {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/features/KotlinClassFeature.kt:46:sealed interface KotlinClassFeature<T : KotlinEncodeable<ObjectSchema>, C : Context, M : Namespace> :
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/features/FeatureContainer.kt:60:abstract class FeatureContainer<M : Namespace>(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/features/EnumFeature.kt:14:sealed interface EnumFeature<E : Enum<E>, C : Context, M : Namespace> :
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/features/BooleanFeature.kt:6:sealed interface BooleanFeature<C : Context, M : Namespace> : Feature<Boolean, C, M> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/features/DoubleFeature.kt:6:sealed interface DoubleFeature<C : Context, M : Namespace> :
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/features/StringFeature.kt:6:sealed interface StringFeature<C : Context, M : Namespace> : Feature<String, C, M> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/features/FeatureAware.kt:13:interface FeatureAware<M : Namespace> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/features/Feature.kt:36:sealed interface Feature<T : Any, C : Context, out M : Namespace> : Identifiable {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/features/IntFeature.kt:6:sealed interface IntFeature<C : Context, M : Namespace> : Feature<Int, C, M> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/Namespace.kt:71:open class Namespace(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt:27:data class FlagDefinition<T : Any, C : Context, M : Namespace> internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/instance/ConfigValue.kt:8:sealed interface ConfigValue {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationPatch.kt:21:data class ConfigurationPatch internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/instance/Configuration.kt:7:data class Configuration internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationMetadata.kt:18:data class ConfigurationMetadata internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationDiff.kt:15:data class ConfigurationDiff internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:82:interface NamespaceRegistry {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/ops/Metrics.kt:3:object Metrics {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/ops/RegistryHooks.kt:11:data class RegistryHooks internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt:3:interface MetricsCollector {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/ops/KonditionalLogger.kt:3:interface KonditionalLogger {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/id/StableId.kt:13:sealed interface StableId {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/dsl/VersionRangeScope.kt:21:interface VersionRangeScope {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:35:interface RuleScope<C : Context> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/dsl/FlagScope.kt:30:interface FlagScope<T : Any, C : Context> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt:38:interface AxisValuesScope {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/serialization/Serializer.kt:54:interface Serializer<T> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/serialization/SnapshotSerializer.kt:45:object SnapshotSerializer {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/serialization/SnapshotLoadOptions.kt:12:data class SnapshotLoadOptions(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/serialization/SnapshotLoadOptions.kt:24:sealed interface UnknownFeatureKeyStrategy {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/serialization/SnapshotLoadOptions.kt:31:data class SnapshotWarning internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/serialization/NamespaceSnapshotSerializer.kt:64:class NamespaceSnapshotSerializer<M : Namespace>(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/values/TypedId.kt:21:interface KonditionalId<T : KonditionalId<T>> : Comparable<T> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/values/TypedId.kt:78:class TFactory<T : KonditionalId<*>>(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/rules/Rule.kt:68:data class Rule<C : Context> internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/rules/versions/RightBound.kt:5:data class RightBound(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/rules/versions/LeftBound.kt:5:data class LeftBound(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/rules/versions/FullyBound.kt:5:data class FullyBound(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/rules/versions/VersionRange.kt:6:sealed class VersionRange(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/rules/versions/Unbounded.kt:5:class Unbounded : VersionRange(Type.UNBOUNDED, MIN_VERSION, MAX_VERSION) {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/rules/evaluable/Specifier.kt:3:interface Specifier {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/rules/evaluable/Placeholder.kt:5:object Placeholder : Evaluable<Context> {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/rules/evaluable/Evaluable.kt:22:fun interface Evaluable<in C : Context> : Specifier {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/api/FeatureUtilities.kt:37:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/api/FeatureUtilities.kt:42:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithReason(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:10:data class ShadowOptions internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:23:data class ShadowMismatch<T : Any> internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:41:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithShadow(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:88:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateShadow(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/api/RolloutBucketing.kt:8:data class BucketInfo internal constructor(
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/api/RolloutBucketing.kt:23:object RolloutBucketing {
/Users/amichne/code/konditional/src/main/kotlin/io/amichne/konditional/api/EvaluationResult.kt:17:data class EvaluationResult<T : Any> internal constructor(

# --- End of extraction ---

# Kontracts Submodule Types
/Users/amichne/code/konditional/kontracts/src/test/kotlin/io/amichne/kontracts/CustomTypeMappingTest.kt:22:class CustomTypeMappingTest {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonValue.kt:18:sealed class JsonValue {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonString.kt:11:data class JsonString(val value: String) : JsonValue() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonArray.kt:13:data class JsonArray(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonBoolean.kt:11:data class JsonBoolean(val value: Boolean) : JsonValue() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonObject.kt:13:data class JsonObject(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonNumber.kt:11:data class JsonNumber(val value: Double) : JsonValue() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonNull.kt:11:object JsonNull : JsonValue() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/FieldSchema.kt:9:data class FieldSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/StringSchema.kt:7:data class StringSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/DoubleSchema.kt:7:data class DoubleSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/ValidationResult.kt:8:sealed class ValidationResult {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/NullSchema.kt:6:data class NullSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/JsonSchema.kt:6:sealed class JsonSchema : OpenApiProps {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/ObjectTraits.kt:3:interface ObjectTraits {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/OpenApiProps.kt:6:interface OpenApiProps {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/ArraySchema.kt:7:data class ArraySchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/BooleanSchema.kt:6:data class BooleanSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/ObjectSchema.kt:7:data class ObjectSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/IntSchema.kt:7:data class IntSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/EnumSchema.kt:11:data class EnumSchema<E : Enum<E>>(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/schema/RootObjectSchema.kt:3:data class RootObjectSchema(
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/BooleanSchemaBuilder.kt:6:open class BooleanSchemaBuilder : JsonSchemaBuilder {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/IntSchemaBuilder.kt:6:open class IntSchemaBuilder : JsonSchemaBuilder {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/EnumSchemaBuilder.kt:7:class EnumSchemaBuilder<E : Enum<E>>(private val enumClass: KClass<E>) : JsonSchemaBuilder {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaBuilder.kt:3:sealed interface JsonSchemaBuilder
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/RootObjectSchemaBuilder.kt:8:class RootObjectSchemaBuilder {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/DoubleSchemaBuilder.kt:6:open class DoubleSchemaBuilder : JsonSchemaBuilder {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/NullSchemaBuilder.kt:6:class NullSchemaBuilder {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/StringSchemaBuilder.kt:6:open class StringSchemaBuilder : JsonSchemaBuilder {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaBuilders.kt:186:fun schemaRoot(builder: RootObjectSchemaBuilder.() -> Unit) = RootObjectSchemaBuilder().apply(builder).build()
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomBooleanSchemaBuilder.kt:10:class CustomBooleanSchemaBuilder<V : Any> : BooleanSchemaBuilder() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomDoubleSchemaBuilder.kt:10:class CustomDoubleSchemaBuilder<V : Any> : DoubleSchemaBuilder() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomStringSchemaBuilder.kt:11:class CustomStringSchemaBuilder<V : Any> : StringSchemaBuilder() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomIntSchemaBuilder.kt:10:class CustomIntSchemaBuilder<V : Any> : IntSchemaBuilder() {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ObjectSchemaBuilder.kt:7:class ObjectSchemaBuilder : JsonSchemaBuilder {
/Users/amichne/code/konditional/kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ArraySchemaBuilder.kt:7:class ArraySchemaBuilder : JsonSchemaBuilder {
