# Core Type Signatures
# Extracted: 2026-01-17T23:45:01-05:00
# Source: modules/*/src/main/kotlin

# Module: konditional-core
konditional-core/src/main/kotlin/io/amichne/konditional/api/EvaluationResult.kt:15:data class EvaluationResult<T : Any> internal constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/api/BucketInfo.kt:6:data class BucketInfo internal constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt:21:inline fun <reified T> Context.axis(): T? where T : AxisValue<T>, T : Enum<T> =
konditional-core/src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt:30:inline fun <reified T, reified C : Context> C.axis(axis: Axis<T>): T? where T : AxisValue<T>, T : Enum<T> =
konditional-core/src/main/kotlin/io/amichne/konditional/api/AxisUtilities.kt:48:inline fun axisValues(block: AxisValuesScope.() -> Unit): AxisValues =
konditional-core/src/main/kotlin/io/amichne/konditional/api/RampUpBucketing.kt:13:object RampUpBucketing {
konditional-core/src/main/kotlin/io/amichne/konditional/values/FeatureId.kt:6:value class FeatureId private constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/core/types/Konstrained.kt:46:interface Konstrained<out S> where S : JsonSchema<*>, S : ObjectTraits {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/DslSugar.kt:20:class YieldingScope<T : Any, C : Context> internal constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/DslSugar.kt:43:class ContextYieldingScope<T : Any, C : Context> internal constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/DslSugar.kt:60:interface YieldingScopeHost {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/DslSugar.kt:67:class PendingYieldToken internal constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/DslSugar.kt:87:fun <C : Context> FlagScope<Boolean, C>.enable(build: RuleScope<C>.() -> Unit = {}) =
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/DslSugar.kt:90:fun <C : Context> FlagScope<Boolean, C>.disable(build: RuleScope<C>.() -> Unit = {}) =
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/DslSugar.kt:96:fun <C : Context> FlagScope<Boolean, C>.enableScoped(build: ContextRuleScope<C>.() -> Unit = {}) =
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/DslSugar.kt:99:fun <C : Context> FlagScope<Boolean, C>.disableScoped(build: ContextRuleScope<C>.() -> Unit = {}) =
konditional-core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:32:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluate(
konditional-core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:60:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.explain(
konditional-core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:70:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithReason(
konditional-core/src/main/kotlin/io/amichne/konditional/api/FeatureEvaluation.kt:196:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateInternalApi(
konditional-core/src/main/kotlin/io/amichne/konditional/api/KonditionalInternalApi.kt:26:annotation class KonditionalInternalApi
konditional-core/src/main/kotlin/io/amichne/konditional/core/types/ObjectSchemaConversions.kt:9:fun JsonSchema<*>.asObjectSchema(): ObjectSchema =
konditional-core/src/main/kotlin/io/amichne/konditional/rules/ConditionalValue.kt:15:data class ConditionalValue<T : Any, C : Context> private constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/internal/FlagDefinitionInternal.kt:25:data class SerializedFlagDefinitionMetadata(
konditional-core/src/main/kotlin/io/amichne/konditional/internal/FlagDefinitionInternal.kt:33:data class SerializedFlagRuleSpec<T : Any>(
konditional-core/src/main/kotlin/io/amichne/konditional/internal/FlagDefinitionInternal.kt:45:fun <T : Any, C : Context, M : Namespace> flagDefinitionFromSerialized(
konditional-core/src/main/kotlin/io/amichne/konditional/internal/FlagDefinitionInternal.kt:75:fun FlagDefinition<*, *, *>.toSerializedMetadata(): SerializedFlagDefinitionMetadata =
konditional-core/src/main/kotlin/io/amichne/konditional/internal/FlagDefinitionInternal.kt:83:fun FlagDefinition<*, *, *>.toSerializedRules(): List<SerializedFlagRuleSpec<Any>> =
konditional-core/src/main/kotlin/io/amichne/konditional/context/Platform.kt:8:enum class Platform : PlatformTag {
konditional-core/src/main/kotlin/io/amichne/konditional/context/RampUp.kt:10:value class RampUp private constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValue.kt:30:interface AxisValue<T> where T : Enum<T> {
konditional-core/src/main/kotlin/io/amichne/konditional/context/PlatformTag.kt:10:interface PlatformTag {
konditional-core/src/main/kotlin/io/amichne/konditional/context/LocaleTag.kt:10:interface LocaleTag {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt:39:interface AxisValuesScope {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/AxisValuesScope.kt:79:inline fun <reified T> AxisValuesScope.axis(value: T) where T : AxisValue<T>, T : Enum<T> {
konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/Axis.kt:48:abstract class Axis<T>(
konditional-core/src/main/kotlin/io/amichne/konditional/context/AppLocale.kt:8:enum class AppLocale(
konditional-core/src/main/kotlin/io/amichne/konditional/rules/evaluable/Placeholder.kt:5:object Placeholder : Predicate<Context> {
konditional-core/src/main/kotlin/io/amichne/konditional/context/Version.kt:9:data class Version(
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/VersionRangeScope.kt:21:interface VersionRangeScope {
konditional-core/src/main/kotlin/io/amichne/konditional/rules/Rule.kt:66:data class Rule<C : Context> internal constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/context/Context.kt:38:interface Context {
konditional-core/src/main/kotlin/io/amichne/konditional/core/id/StaticStableId.kt:15:interface StaticStableId : StableId
konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:19:interface NamespaceRegistry {
konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistry.kt:59:interface NamespaceRegistryRuntime : NamespaceRegistry {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/FlagScope.kt:30:interface FlagScope<T : Any, C : Context> {
konditional-core/src/main/kotlin/io/amichne/konditional/context/axis/AxisValues.kt:34:class AxisValues internal constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/core/id/HexId.kt:13:value class HexId @KonditionalInternalApi constructor(internal val externalId: String) {
konditional-core/src/main/kotlin/io/amichne/konditional/rules/evaluable/Specifier.kt:3:interface Specifier {
konditional-core/src/main/kotlin/io/amichne/konditional/rules/evaluable/Predicate.kt:22:fun interface Predicate<in C : Context> : Specifier {
konditional-core/src/main/kotlin/io/amichne/konditional/core/registry/NamespaceRegistryFactory.kt:11:fun interface NamespaceRegistryFactory {
konditional-core/src/main/kotlin/io/amichne/konditional/core/Namespace.kt:71:open class Namespace(
konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/Unbounded.kt:5:class Unbounded : VersionRange(Type.UNBOUNDED, MIN_VERSION, MAX_VERSION) {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/KonditionalDsl.kt:9:annotation class KonditionalDsl
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:35:interface RuleScopeBase<C : Context> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:54:interface LocaleTargetingScope<C : Context> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:69:interface PlatformTargetingScope<C : Context> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:98:interface VersionTargetingScope<C : Context> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:119:interface StableIdTargetingScope<C : Context> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:150:interface AxisTargetingScope<C : Context> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:176:interface ExtensionTargetingScope<C : Context> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:199:interface NoteScope<C : Context> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:215:interface ContextRuleScope<C : Context> :
konditional-core/src/main/kotlin/io/amichne/konditional/core/dsl/RuleScope.kt:246:interface RuleScope<C : Context> :
konditional-core/src/main/kotlin/io/amichne/konditional/core/id/StableId.kt:18:sealed interface StableId {
konditional-core/src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt:31:data class FlagDefinition<T : Any, C : Context, M : Namespace>(
konditional-core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationView.kt:14:interface ConfigurationView {
konditional-core/src/main/kotlin/io/amichne/konditional/core/instance/ConfigurationView.kt:22:interface ConfigurationMetadataView {
konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/VersionRange.kt:6:sealed class VersionRange(
konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/KonditionalLogger.kt:3:interface KonditionalLogger {
konditional-core/src/main/kotlin/io/amichne/konditional/core/features/IntFeature.kt:6:sealed interface IntFeature<C : Context, M : Namespace> : Feature<Int, C, M> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/ValueType.kt:6:enum class ValueType {
konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/FullyBound.kt:5:data class FullyBound(
konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/MetricsCollector.kt:3:interface MetricsCollector {
konditional-core/src/main/kotlin/io/amichne/konditional/core/spi/FeatureRegistrationHook.kt:11:fun interface FeatureRegistrationHook {
konditional-core/src/main/kotlin/io/amichne/konditional/core/features/Identifiable.kt:6:interface Identifiable {
konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/RegistryHooks.kt:11:data class RegistryHooks internal constructor(
konditional-core/src/main/kotlin/io/amichne/konditional/core/features/EnumFeature.kt:14:sealed interface EnumFeature<E : Enum<E>, C : Context, M : Namespace> :
konditional-core/src/main/kotlin/io/amichne/konditional/core/features/Feature.kt:36:sealed interface Feature<T : Any, C : Context, out M : Namespace> : Identifiable {
konditional-core/src/main/kotlin/io/amichne/konditional/core/features/DoubleFeature.kt:6:sealed interface DoubleFeature<C : Context, M : Namespace> :
konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/LeftBound.kt:5:data class LeftBound(
konditional-core/src/main/kotlin/io/amichne/konditional/core/features/KotlinClassFeature.kt:38:sealed interface KotlinClassFeature<T : Konstrained<*>, C : Context, M : Namespace> :
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseError.kt:11:sealed interface ParseError {
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseException.kt:7:class ParseException(val error: ParseError) : Exception(error.message)
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/ParseResult.kt:15:sealed interface ParseResult<out T> {
konditional-core/src/main/kotlin/io/amichne/konditional/rules/versions/RightBound.kt:5:data class RightBound(
konditional-core/src/main/kotlin/io/amichne/konditional/core/features/StringFeature.kt:6:sealed interface StringFeature<C : Context, M : Namespace> : Feature<String, C, M> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/ops/Metrics.kt:3:object Metrics {
konditional-core/src/main/kotlin/io/amichne/konditional/core/features/BooleanFeature.kt:6:sealed interface BooleanFeature<C : Context, M : Namespace> : Feature<Boolean, C, M> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:19:inline fun <T, R> ParseResult<T>.fold(
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:33:inline fun <T, R> ParseResult<T>.map(transform: (T) -> R): ParseResult<R> = when (this) {
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:46:inline fun <T, R> ParseResult<T>.flatMap(transform: (T) -> ParseResult<R>): ParseResult<R> = when (this) {
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:55:fun <T> ParseResult<T>.getOrNull(): T? = when (this) {
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:63:fun <T> ParseResult<T>.getOrDefault(default: T): T = when (this) {
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:71:inline fun <T> ParseResult<T>.getOrElse(onFailure: (ParseError) -> T): T = when (this) {
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:79:fun <T> ParseResult<T>.isSuccess(): Boolean = this is ParseResult.Success
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:84:fun <T> ParseResult<T>.isFailure(): Boolean = this is ParseResult.Failure
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:96:inline fun <T> ParseResult<T>.onSuccess(action: (T) -> Unit): ParseResult<T> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:113:inline fun <T> ParseResult<T>.onFailure(action: (ParseError) -> Unit): ParseResult<T> {
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:130:inline fun <T> ParseResult<T>.recover(transform: (ParseError) -> T): T = when (this) {
konditional-core/src/main/kotlin/io/amichne/konditional/core/result/utils/ParseResultUtils.kt:139:fun <T> ParseResult<T>.toResult(): Result<T> = fold(

# Module: konditional-runtime
konditional-runtime/src/main/kotlin/io/amichne/konditional/core/registry/InMemoryNamespaceRegistry.kt:29:class InMemoryNamespaceRegistry(
konditional-runtime/src/main/kotlin/io/amichne/konditional/runtime/NamespaceOperations.kt:16:fun Namespace.load(configuration: ConfigurationView) {
konditional-runtime/src/main/kotlin/io/amichne/konditional/runtime/NamespaceOperations.kt:20:fun Namespace.rollback(steps: Int = 1): Boolean =
konditional-runtime/src/main/kotlin/io/amichne/konditional/serialization/snapshot/NamespaceSnapshotLoader.kt:18:class NamespaceSnapshotLoader<M : Namespace>(
konditional-runtime/src/main/kotlin/io/amichne/konditional/core/registry/InMemoryNamespaceRegistryFactory.kt:6:class InMemoryNamespaceRegistryFactory : NamespaceRegistryFactory {

# Module: konditional-serialization
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/FlagValueAdapter.kt:24:class FlagValueAdapter : JsonAdapter<FlagValue<*>>() {
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/FlagValueAdapter.kt:93:object FlagValueAdapterFactory : JsonAdapter.Factory {
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/VersionRangeAdapter.kt:26:class VersionRangeAdapter(moshi: Moshi) {
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/FlagValueJsonMaps.kt:9:fun serializeMap(
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/FlagValueJsonMaps.kt:22:fun serializeValue(
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/FlagValueJsonMaps.kt:45:fun deserializeMap(reader: JsonReader): Map<String, Any?> {
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/FlagValueJsonMaps.kt:58:fun deserializeValue(reader: JsonReader): Any? =
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/ValueClassAdapterFactory.kt:14:object ValueClassAdapterFactory : JsonAdapter.Factory {
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableSnapshot.kt:21:data class SerializableSnapshot(
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/adapters/IdentifierJsonAdapter.kt:20:object IdentifierJsonAdapter : JsonAdapter.Factory {
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/FlagValue.kt:31:sealed class FlagValue<out T : Any> {
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableRule.kt:19:data class SerializableRule(
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableSnapshotMetadata.kt:9:data class SerializableSnapshotMetadata(
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaValueCodec.kt:27:object SchemaValueCodec {
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializableFlag.kt:31:data class SerializableFlag(
konditional-serialization/src/main/kotlin/io/amichne/konditional/internal/serialization/models/SerializablePatch.kt:13:data class SerializablePatch(
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/snapshot/ConfigurationSnapshotCodec.kt:53:object ConfigurationSnapshotCodec : SnapshotCodec<Configuration> {
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/snapshot/SnapshotCodec.kt:13:interface SnapshotCodec<T> {
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/options/SnapshotLoadOptions.kt:10:data class SnapshotLoadOptions(
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/ConfigurationDiff.kt:17:data class ConfigurationDiff internal constructor(
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/ConfigValue.kt:9:sealed interface ConfigValue {
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/options/SnapshotWarning.kt:6:data class SnapshotWarning internal constructor(
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/snapshot/SnapshotLoader.kt:11:interface SnapshotLoader<T> {
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/SchemaExtraction.kt:27:fun extractSchema(kClass: KClass<*>): ObjectSchema? {
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/options/UnknownFeatureKeyStrategy.kt:3:sealed interface UnknownFeatureKeyStrategy {
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/Configuration.kt:11:data class Configuration(
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/Configuration.kt:33:data class ConfigurationMetadata(
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/internal/KonstrainedPrimitiveMap.kt:11:fun Konstrained<*>.toPrimitiveMap(): Map<String, Any?> =
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/FeatureRegistry.kt:23:object FeatureRegistry {
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/internal/SerializationFeatureRegistrationHook.kt:12:class SerializationFeatureRegistrationHook : FeatureRegistrationHook {
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/internal/JsonValueConversions.kt:20:fun Any?.toJsonValue(): JsonValue =
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/internal/JsonValueConversions.kt:47:fun JsonValue.toPrimitiveValue(): Any? =
konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/ConfigurationPatch.kt:13:data class ConfigurationPatch(

# Module: konditional-observability
konditional-observability/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:12:data class ShadowOptions internal constructor(
konditional-observability/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:25:data class ShadowMismatch<T : Any> internal constructor(
konditional-observability/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:43:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithShadow(
konditional-observability/src/main/kotlin/io/amichne/konditional/api/ShadowEvaluation.kt:90:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateShadow(

# Module: config-metadata
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/dsl/ConfigMetadataDsl.kt:8:annotation class ConfigMetadataDsl
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/dsl/ConfigMetadataDsl.kt:11:class ConfigMetadataBuilder {
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/dsl/ConfigMetadataDsl.kt:37:fun configMetadata(block: ConfigMetadataBuilder.() -> Unit): ConfigMetadata =
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/SchemaRefDescriptor.kt:5:data class SchemaRefDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/EnumOption.kt:3:data class EnumOption(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/MapConstraintsDescriptor.kt:5:data class MapConstraintsDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/StringConstraintsDescriptor.kt:5:data class StringConstraintsDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/SemverConstraintsDescriptor.kt:5:data class SemverConstraintsDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/BooleanDescriptor.kt:5:data class BooleanDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/EnumOptionsDescriptor.kt:5:data class EnumOptionsDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/NumberRangeDescriptor.kt:5:data class NumberRangeDescriptor(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/BindingType.kt:3:enum class BindingType {
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/ConfigMetadata.kt:11:data class ConfigMetadata(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/ui/UiControlType.kt:3:enum class UiControlType {
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/descriptor/ValueDescriptor.kt:5:sealed interface ValueDescriptor {
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/ui/UiHints.kt:3:data class UiHints(
config-metadata/src/main/kotlin/io/amichne/konditional/configmetadata/contract/ConfigMetadataResponse.kt:6:data class ConfigMetadataResponse<out S>(

# Module: kontracts
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ArraySchemaBuilder.kt:7:class ArraySchemaBuilder : JsonSchemaBuilder<List<Any>> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/ObjectSchemaBuilder.kt:7:class ObjectSchemaBuilder : JsonSchemaBuilder<Map<String, Any?>> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/EnumSchemaBuilder.kt:7:class EnumSchemaBuilder<E : Enum<E>>(private val enumClass: KClass<E>) : JsonSchemaBuilder<E> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/NullSchemaBuilder.kt:6:class NullSchemaBuilder : JsonSchemaBuilder<Any> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/BooleanSchemaBuilder.kt:6:open class BooleanSchemaBuilder : JsonSchemaBuilder<Boolean> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/IntSchemaBuilder.kt:6:open class IntSchemaBuilder : JsonSchemaBuilder<Int> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/DoubleSchemaBuilder.kt:6:open class DoubleSchemaBuilder : JsonSchemaBuilder<Double> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaBuilderDsl.kt:4:annotation class JsonSchemaBuilderDsl
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaBuilder.kt:5:sealed interface JsonSchemaBuilder<out T : Any> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomIntSchemaBuilder.kt:10:class CustomIntSchemaBuilder<V : Any> : IntSchemaBuilder() {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/JsonSchemaBuilders.kt:179:fun schemaRoot(builder: RootObjectSchemaBuilder.() -> Unit) = RootObjectSchemaBuilder().apply(builder).build()
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/StringSchemaBuilder.kt:6:open class StringSchemaBuilder : JsonSchemaBuilder<String> {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/RootObjectSchemaBuilder.kt:8:class RootObjectSchemaBuilder {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomDoubleSchemaBuilder.kt:10:class CustomDoubleSchemaBuilder<V : Any> : DoubleSchemaBuilder() {
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonNull.kt:11:object JsonNull : JsonValue {
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonArray.kt:13:data class JsonArray(
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomStringSchemaBuilder.kt:11:class CustomStringSchemaBuilder<V : Any> : StringSchemaBuilder() {
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonValue.kt:18:sealed interface JsonValue {
kontracts/src/main/kotlin/io/amichne/kontracts/dsl/custom/CustomBooleanSchemaBuilder.kt:10:class CustomBooleanSchemaBuilder<V : Any> : BooleanSchemaBuilder() {
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonBoolean.kt:11:data class JsonBoolean(val value: Boolean) : JsonValue {
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonNumber.kt:11:data class JsonNumber(val value: Double) : JsonValue {
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonString.kt:11:data class JsonString(val value: String) : JsonValue {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/BooleanSchema.kt:6:data class BooleanSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/OneOfSchema.kt:6:data class OneOfSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/value/JsonObject.kt:14:data class JsonObject(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/ArraySchema.kt:7:data class ArraySchema<E : Any>(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/RootObjectSchema.kt:3:data class RootObjectSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/ObjectSchema.kt:7:data class ObjectSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/FieldSchema.kt:9:data class FieldSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/ValidationResult.kt:8:sealed class ValidationResult {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/ObjectTraits.kt:3:interface ObjectTraits {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/StringSchema.kt:7:data class StringSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/IntSchema.kt:7:data class IntSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/EnumSchema.kt:11:data class EnumSchema<E : Enum<E>>(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/NullSchema.kt:6:data class NullSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/AnySchema.kt:6:data class AnySchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/DoubleSchema.kt:7:data class DoubleSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/MapSchema.kt:6:data class MapSchema<V : Any>(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/SchemaProvider.kt:26:interface SchemaProvider<out S : JsonSchema<*>> {
kontracts/src/main/kotlin/io/amichne/kontracts/schema/AllOfSchema.kt:6:data class AllOfSchema(
kontracts/src/main/kotlin/io/amichne/kontracts/schema/JsonSchema.kt:6:sealed class JsonSchema<out T : Any> : OpenApi<T> {

# Module: openapi
openapi/src/main/kotlin/io/amichne/kontracts/schema/OpenApi.kt:9:interface OpenApi<out T : Any> {

# Module: opentelemetry
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/logging/OtelLogger.kt:17:class OtelLogger(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/FeatureTelemetryExtensions.kt:43:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetry(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/FeatureTelemetryExtensions.kt:64:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetryAndReason(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/FeatureTelemetryExtensions.kt:84:fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithAutoSpan(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/KonditionalTelemetry.kt:46:class KonditionalTelemetry(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/FlagEvaluationTracer.kt:33:class FlagEvaluationTracer(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/metrics/MetricsConfig.kt:9:data class MetricsConfig(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/metrics/OtelMetricsCollector.kt:21:class OtelMetricsCollector(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/TracingConfig.kt:14:data class TracingConfig(
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/TracingConfig.kt:35:sealed interface SamplingStrategy {
opentelemetry/src/main/kotlin/io/amichne/konditional/otel/traces/SpanAttributes.kt:11:object KonditionalSemanticAttributes {

# --- End of extraction ---
