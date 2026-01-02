# Core Type Signatures
# Extracted: 2026-01-02T09:39:16-05:00
# Source: modules/*/src/main/kotlin

# Module: core
core/src/main/kotlin/io/amichne/konditional/api/EvaluationResult.kt:15:data class EvaluationResult<T : Any> internal constructor(
core/src/main/kotlin/io/amichne/konditional/api/BucketInfo.kt:6:data class BucketInfo internal constructor(
core/src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt:13:object RampUpBucketing {
core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:30:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:58:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.explain(
core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:68:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithReason(
core/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:10:data class ShadowOptions internal constructor(
core/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:23:data class ShadowMismatch<T : Any> internal constructor(
core/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:41:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithShadow(
core/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:88:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateShadow(
core/src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt:39:interface AxisValuesScope {
core/src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt:79:inline fun <reified T> AxisValuesScope.axis(value: T) where T : AxisValue<T>, T : Enum<T> {
core/src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt:21:inline fun <reified T> Context.axis(): T? where T : AxisValue<T>, T : Enum<T> =
core/src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt:30:inline fun <reified T, reified C : Context> C.axis(axis: Axis<T>): T? where T : AxisValue<T>, T : Enum<T> =
core/src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt:48:inline fun axisValues(block: AxisValuesScope.() -> Unit): AxisValues =
core/src/main/kotlin/io/amichne/konditional/values/FeatureId.kt:6:value class FeatureId private constructor(
core/src/main/kotlin/io/amichne/konditional/core/ValueType.kt:6:enum class ValueType {
core/src/main/kotlin/io/amichne/konditional/core/dsl/FlagScope.kt:30:interface FlagScope<T : Any, C : Context> {
core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationDiff.kt:13:data class ConfigurationDiff internal constructor(
core/src/main/kotlin/io/amichne/konditional/serialization/snapshot/ConfigurationSnapshotCodec.kt:47:object ConfigurationSnapshotCodec : SnapshotCodec<Configuration> {
core/src/main/kotlin/io/amichne/konditional/core/types/Konstrained.kt:46:interface Konstrained<out S> where S : JsonSchema<*>, S : ObjectTraits {
core/src/main/kotlin/io/amichne/konditional/core/dsl/KonditionalDsl.kt:9:annotation class KonditionalDsl
core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationMetadata.kt:18:data class ConfigurationMetadata internal constructor(
core/src/main/kotlin/io/amichne/konditional/serialization/options/SnapshotLoadOptions.kt:10:data class SnapshotLoadOptions(
core/src/main/kotlin/io/amichne/konditional/rules/evaluable/Placeholder.kt:5:object Placeholder : Predicate<Context> {
core/src/main/kotlin/io/amichne/konditional/serialization/options/UnknownFeatureKeyStrategy.kt:3:sealed interface UnknownFeatureKeyStrategy {
core/src/main/kotlin/io/amichne/konditional/serialization/snapshot/NamespaceSnapshotLoader.kt:28:class NamespaceSnapshotLoader<M : Namespace>(
core/src/main/kotlin/io/amichne/konditional/core/instance/Configuration.kt:7:data class Configuration internal constructor(
core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:35:interface RuleScope<C : Context> {
core/src/main/kotlin/io/amichne/konditional/serialization/options/SnapshotWarning.kt:6:data class SnapshotWarning internal constructor(
core/src/main/kotlin/io/amichne/konditional/rules/evaluable/Specifier.kt:3:interface Specifier {
core/src/main/kotlin/io/amichne/konditional/core/dsl/VersionRangeScope.kt:21:interface VersionRangeScope {
core/src/main/kotlin/io/amichne/konditional/serialization/snapshot/SnapshotCodec.kt:13:interface SnapshotCodec<T> {
core/src/main/kotlin/io/amichne/konditional/core/result/ParseException.kt:7:class ParseException(val error: ParseError) : Exception(error.message)
core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationPatch.kt:21:data class ConfigurationPatch internal constructor(
core/src/main/kotlin/io/amichne/konditional/rules/evaluable/Predicate.kt:22:fun interface Predicate<in C : Context> : Specifier {
core/src/main/kotlin/io/amichne/konditional/context/RampUp.kt:10:value class RampUp private constructor(
core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValue.kt:30:interface AxisValue<T> where T : Enum<T> {
core/src/main/kotlin/io/amichne/konditional/serialization/snapshot/SnapshotLoader.kt:11:interface SnapshotLoader<T> {
core/src/main/kotlin/io/amichne/konditional/rules/versions/Unbounded.kt:5:class Unbounded : VersionRange(Type.UNBOUNDED, MIN_VERSION, MAX_VERSION) {
core/src/main/kotlin/io/amichne/konditional/context/axis/Axis.kt:47:abstract class Axis<T>(
core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigValue.kt:8:sealed interface ConfigValue {
core/src/main/kotlin/io/amichne/konditional/context/AppLocale.kt:8:enum class AppLocale(
core/src/main/kotlin/io/amichne/konditional/core/id/HexId.kt:11:value class HexId internal constructor(internal val externalId: String) {
core/src/main/kotlin/io/amichne/konditional/context/PlatformTag.kt:10:interface PlatformTag {
core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:15:sealed interface ParseResult<out T> {
core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:33:fun <T> ParseResult<T>.getOrThrow(): T = when (this) {
core/src/main/kotlin/io/amichne/konditional/context/Platform.kt:8:enum class Platform : PlatformTag {
core/src/main/kotlin/io/amichne/konditional/context/Context.kt:37:interface Context {
core/src/main/kotlin/io/amichne/konditional/context/Version.kt:9:data class Version(
core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValues.kt:35:class AxisValues internal constructor(
core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt:68:open class Namespace(
core/src/main/kotlin/io/amichne/konditional/core/id/StableId.kt:15:sealed interface StableId {
core/src/main/kotlin/io/amichne/konditional/context/LocaleTag.kt:10:interface LocaleTag {
core/src/main/kotlin/io/amichne/konditional/rules/versions/RightBound.kt:5:data class RightBound(
core/src/main/kotlin/io/amichne/konditional/rules/versions/FullyBound.kt:5:data class FullyBound(
core/src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt:11:sealed interface ParseError {
core/src/main/kotlin/io/amichne/konditional/rules/versions/VersionRange.kt:6:sealed class VersionRange(
core/src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt:27:data class FlagDefinition<T : Any, C : Context, M : Namespace> internal constructor(
core/src/main/kotlin/io/amichne/konditional/rules/versions/LeftBound.kt:5:data class LeftBound(
core/src/main/kotlin/io/amichne/konditional/core/features/IntFeature.kt:6:sealed interface IntFeature<C : Context, M : Namespace> : Feature<Int, C, M> {
core/src/main/kotlin/io/amichne/konditional/core/features/BooleanFeature.kt:6:sealed interface BooleanFeature<C : Context, M : Namespace> : Feature<Boolean, C, M> {
core/src/main/kotlin/io/amichne/konditional/rules/Rule.kt:66:data class Rule<C : Context> internal constructor(
core/src/main/kotlin/io/amichne/konditional/core/features/DoubleFeature.kt:6:sealed interface DoubleFeature<C : Context, M : Namespace> :
core/src/main/kotlin/io/amichne/konditional/core/features/Identifiable.kt:6:interface Identifiable {
core/src/main/kotlin/io/amichne/konditional/core/features/EnumFeature.kt:14:sealed interface EnumFeature<E : Enum<E>, C : Context, M : Namespace> :
core/src/main/kotlin/io/amichne/konditional/core/ops/KonditionalLogger.kt:3:interface KonditionalLogger {
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:19:inline fun <T, R> ParseResult<T>.fold(
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:33:inline fun <T, R> ParseResult<T>.map(transform: (T) -> R): ParseResult<R> = when (this) {
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:46:inline fun <T, R> ParseResult<T>.flatMap(transform: (T) -> ParseResult<R>): ParseResult<R> = when (this) {
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:55:fun <T> ParseResult<T>.getOrNull(): T? = when (this) {
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:63:fun <T> ParseResult<T>.getOrDefault(default: T): T = when (this) {
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:71:inline fun <T> ParseResult<T>.getOrElse(onFailure: (ParseError) -> T): T = when (this) {
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:79:fun <T> ParseResult<T>.isSuccess(): Boolean = this is ParseResult.Success
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:84:fun <T> ParseResult<T>.isFailure(): Boolean = this is ParseResult.Failure
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:96:inline fun <T> ParseResult<T>.onSuccess(action: (T) -> Unit): ParseResult<T> {
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:113:inline fun <T> ParseResult<T>.onFailure(action: (ParseError) -> Unit): ParseResult<T> {
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:130:inline fun <T> ParseResult<T>.recover(transform: (ParseError) -> T): T = when (this) {
core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:139:fun <T> ParseResult<T>.toResult(): Result<T> = fold(
core/src/main/kotlin/io/amichne/konditional/core/features/Feature.kt:36:sealed interface Feature<T : Any, C : Context, out M : Namespace> : Identifiable {
core/src/main/kotlin/io/amichne/konditional/core/ops/Metrics.kt:3:object Metrics {
core/src/main/kotlin/io/amichne/konditional/core/features/KotlinClassFeature.kt:38:sealed interface KotlinClassFeature<T : Konstrained<*>, C : Context, M : Namespace> :
core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:72:interface NamespaceRegistry {
core/src/main/kotlin/io/amichne/konditional/core/features/StringFeature.kt:6:sealed interface StringFeature<C : Context, M : Namespace> : Feature<String, C, M> {
core/src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt:3:interface MetricsCollector {
core/src/main/kotlin/io/amichne/konditional/core/ops/RegistryHooks.kt:11:data class RegistryHooks internal constructor(

# Module: config-metadata
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/dsl/ConfigMetadataDsl.kt:8:annotation class ConfigMetadataDsl
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/dsl/ConfigMetadataDsl.kt:11:class ConfigMetadataBuilder {
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/dsl/ConfigMetadataDsl.kt:37:fun configMetadata(block: ConfigMetadataBuilder.() -> Unit): ConfigMetadata =
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/SchemaRefDescriptor.kt:5:data class SchemaRefDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/MapConstraintsDescriptor.kt:5:data class MapConstraintsDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/EnumOption.kt:3:data class EnumOption(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/EnumOptionsDescriptor.kt:5:data class EnumOptionsDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/BindingType.kt:3:enum class BindingType {
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/NumberRangeDescriptor.kt:5:data class NumberRangeDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/SemverConstraintsDescriptor.kt:5:data class SemverConstraintsDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/ui/UiControlType.kt:3:enum class UiControlType {
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/BooleanDescriptor.kt:5:data class BooleanDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/ValueDescriptor.kt:5:sealed interface ValueDescriptor {
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/StringConstraintsDescriptor.kt:5:data class StringConstraintsDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/ui/UiHints.kt:3:data class UiHints(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/ConfigMetadata.kt:11:data class ConfigMetadata(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/ConfigMetadataResponse.kt:6:data class ConfigMetadataResponse<out S>(

# Module: kontracts
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ArraySchemaBuilder.kt:7:class ArraySchemaBuilder : JsonSchemaBuilder<List<Any>> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ObjectSchemaBuilder.kt:7:class ObjectSchemaBuilder : JsonSchemaBuilder<Map<String, Any?>> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomIntSchemaBuilder.kt:10:class CustomIntSchemaBuilder<V : Any> : IntSchemaBuilder() {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/DoubleSchemaBuilder.kt:6:open class DoubleSchemaBuilder : JsonSchemaBuilder<Double> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomStringSchemaBuilder.kt:11:class CustomStringSchemaBuilder<V : Any> : StringSchemaBuilder() {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaBuilder.kt:5:sealed interface JsonSchemaBuilder<out T : Any> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/NullSchemaBuilder.kt:6:class NullSchemaBuilder : JsonSchemaBuilder<Any> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/EnumSchemaBuilder.kt:7:class EnumSchemaBuilder<E : Enum<E>>(private val enumClass: KClass<E>) : JsonSchemaBuilder<E> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaBuilderDsl.kt:4:annotation class JsonSchemaBuilderDsl
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomDoubleSchemaBuilder.kt:10:class CustomDoubleSchemaBuilder<V : Any> : DoubleSchemaBuilder() {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/BooleanSchemaBuilder.kt:6:open class BooleanSchemaBuilder : JsonSchemaBuilder<Boolean> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/RootObjectSchemaBuilder.kt:8:class RootObjectSchemaBuilder {
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonNull.kt:11:object JsonNull : JsonValue {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/StringSchemaBuilder.kt:6:open class StringSchemaBuilder : JsonSchemaBuilder<String> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaBuilders.kt:179:fun schemaRoot(builder: RootObjectSchemaBuilder.() -> Unit) = RootObjectSchemaBuilder().apply(builder).build()
kontracts/src/main/kotlin/io/amichne/kontracts/schema/RootObjectSchema.kt:3:data class RootObjectSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/IntSchemaBuilder.kt:6:open class IntSchemaBuilder : JsonSchemaBuilder<Int> {
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonArray.kt:13:data class JsonArray(
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonString.kt:11:data class JsonString(val value: String) : JsonValue {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/ObjectTraits.kt:3:interface ObjectTraits {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomBooleanSchemaBuilder.kt:10:class CustomBooleanSchemaBuilder<V : Any> : BooleanSchemaBuilder() {
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonObject.kt:14:data class JsonObject(
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonValue.kt:18:sealed interface JsonValue {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/FieldSchema.kt:9:data class FieldSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonBoolean.kt:11:data class JsonBoolean(val value: Boolean) : JsonValue {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/JsonSchema.kt:6:sealed class JsonSchema<out T : Any> : OpenApi<T> {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/OneOfSchema.kt:6:data class OneOfSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/NullSchema.kt:6:data class NullSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonNumber.kt:11:data class JsonNumber(val value: Double) : JsonValue {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/BooleanSchema.kt:6:data class BooleanSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/MapSchema.kt:6:data class MapSchema<V : Any>(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/DoubleSchema.kt:7:data class DoubleSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/StringSchema.kt:7:data class StringSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/ValidationResult.kt:8:sealed class ValidationResult {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/EnumSchema.kt:11:data class EnumSchema<E : Enum<E>>(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/ArraySchema.kt:7:data class ArraySchema<E : Any>(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/ObjectSchema.kt:7:data class ObjectSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/AnySchema.kt:6:data class AnySchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/IntSchema.kt:7:data class IntSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/SchemaProvider.kt:26:interface SchemaProvider<out S : JsonSchema<*>> {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/AllOfSchema.kt:6:data class AllOfSchema(

# Module: openapi
openapi/src/main/kotlin/io/amichne/kontracts/schema/OpenApi.kt:9:interface OpenApi<out T : Any> {

# Module: opentelemetry
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/logging/OtelLogger.kt:18:class OtelLogger(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/FeatureTelemetryExtensions.kt:44:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetry(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/FeatureTelemetryExtensions.kt:65:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetryAndReason(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/FeatureTelemetryExtensions.kt:85:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithAutoSpan(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/KonditionalTelemetry.kt:48:class KonditionalTelemetry(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/FlagEvaluationTracer.kt:23:class FlagEvaluationTracer(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/SpanAttributes.kt:11:object KonditionalSemanticAttributes {
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/TracingConfig.kt:14:data class TracingConfig(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/TracingConfig.kt:35:sealed interface SamplingStrategy {
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/metrics/MetricsConfig.kt:9:data class MetricsConfig(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/metrics/OtelMetricsCollector.kt:21:class OtelMetricsCollector(

# --- End of extraction ---
