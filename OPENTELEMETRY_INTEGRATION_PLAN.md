# OpenTelemetry Integration Plan for Konditional

**Scope:** Address operational monitoring and tracing gaps in Konditional
**Assumption:** Enterprise has functional OpenTelemetry infrastructure
**Timeline:** 2-3 weeks for Phase 1, 1-2 weeks for Phase 2
**Effort:** 1 engineer (backend), 0.5 engineer (SRE support)

---

## Executive Summary

This plan adds comprehensive OpenTelemetry instrumentation to Konditional without breaking existing API contracts. The implementation provides:

- **Distributed traces** for flag evaluations (correlate to business transactions)
- **Metrics** for evaluation performance, config health, and operational KPIs
- **Structured logging** with trace correlation
- **Zero overhead when disabled** (feature-flagged instrumentation)

**Key Principle:** All observability hooks are **opt-in and non-intrusive**.

---

## 1. Current State Analysis

### 1.1 Existing Observability Hooks

**What Konditional provides today:**

```kotlin
// Generic metrics interface (no implementation)
interface MetricsCollector {
    fun recordEvaluation(metric: Evaluation)
    fun recordConfigLoad(metric: ConfigLoadMetric)
    fun recordConfigRollback(metric: ConfigRollbackMetric)
}

// Generic logging interface
interface KonditionalLogger {
    fun warn(message: () -> String, throwable: Throwable?)
    fun info(message: () -> String)
    fun debug(message: () -> String)
}

// Hooks container
data class RegistryHooks(
    val logger: KonditionalLogger,
    val metrics: MetricsCollector
)
```

**Gaps:**
- ❌ No trace context propagation
- ❌ No span creation
- ❌ No semantic conventions
- ❌ No exemplar support (link metrics → traces)
- ❌ No resource attributes
- ❌ No baggage propagation

### 1.2 Evaluation Hot Path

```kotlin
// FlagDefinition.kt:69
internal fun evaluate(context: C): T {
    if (!isActive) return defaultValue
    return evaluateTrace(context).value
}

// Current instrumentation point:
internal fun evaluateTrace(context: C): Trace<T, C> {
    // ~10-50 iterations, ~2-5μs total
    for (candidate in valuesByPrecedence) {
        if (!candidate.rule.matches(context)) continue
        bucket = Bucketing.stableBucket(...)
        if (isInRampUp(bucket)) return Trace(...)
    }
}
```

**Instrumentation requirements:**
- Must NOT add >10% latency overhead
- Must support sampling (not every evaluation)
- Must capture evaluation decision path

---

## 2. Architecture Design

### 2.1 Instrumentation Layers

```
┌─────────────────────────────────────────────────┐
│  Application Code                               │
│  MyFlags.darkMode.evaluate(context)            │
└────────────────┬────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────┐
│  Public API Extension (NEW)                     │
│  evaluateWithTelemetry(context, span?)         │
└────────────────┬────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────┐
│  Telemetry Adapter (NEW)                        │
│  • Create spans                                 │
│  • Record metrics                               │
│  • Extract trace context                        │
└────────────────┬────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────┐
│  Core Evaluation (UNCHANGED)                    │
│  FlagDefinition.evaluateTrace()                │
└─────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────┐
│  OpenTelemetry SDK                              │
│  • Exporters → Backend (Tempo, Jaeger, etc.)   │
│  • Processors → OTLP                            │
└─────────────────────────────────────────────────┘
```

### 2.2 Module Structure

**New module:** `konditional-opentelemetry`

```
konditional-opentelemetry/
├── src/main/kotlin/io/amichne/konditional/otel/
│   ├── KonditionalTelemetry.kt           # Main entry point
│   ├── traces/
│   │   ├── FlagEvaluationTracer.kt       # Span creation
│   │   ├── SpanAttributes.kt             # Semantic conventions
│   │   └── TracingConfig.kt              # Sampling, filters
│   ├── metrics/
│   │   ├── OtelMetricsCollector.kt       # MetricsCollector impl
│   │   ├── FlagMetrics.kt                # Meter definitions
│   │   └── MetricsConfig.kt              # Cardinality limits
│   ├── logging/
│   │   ├── OtelLogger.kt                 # KonditionalLogger impl
│   │   └── StructuredLogEvent.kt         # Log event format
│   └── context/
│       ├── TelemetryContext.kt           # Trace + baggage
│       └── ContextPropagation.kt         # W3C propagation
└── build.gradle.kts
```

**Dependencies:**
```kotlin
dependencies {
    api(project(":konditional"))
    api("io.opentelemetry:opentelemetry-api:1.34.1")
    implementation("io.opentelemetry:opentelemetry-sdk:1.34.1")
    implementation("io.opentelemetry:opentelemetry-semconv:1.23.1-alpha")

    // Optional: for auto-instrumentation
    compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:2.0.0")
}
```

---

## 3. Detailed Implementation

### 3.1 Phase 1: Traces (Week 1-2)

#### 3.1.1 Span Model

**Span hierarchy:**
```
┌─ feature.evaluation (INTERNAL)
│  ├─ Attributes:
│  │  • feature.key = "darkMode"
│  │  • feature.namespace = "app"
│  │  • feature.type = "boolean"
│  │  • feature.default = false
│  │  • evaluation.result.value = true
│  │  • evaluation.result.decision = "rule_matched"
│  │  • evaluation.rule.note = "ios-rollout"
│  │  • evaluation.rule.specificity = 2
│  │  • evaluation.bucket = 4523
│  │  • evaluation.ramp_up = 50.0
│  │  • evaluation.config_version = "v1.2.3"
│  │  • context.platform = "IOS"
│  │  • context.locale = "en_US"
│  │  • context.version = "2.1.0"
│  │  • context.stable_id = "abc123..." (hashed)
│  ├─ Events:
│  │  • rule_skipped (timestamp, rule_note, reason="ramp_up_excluded")
│  └─ Status: OK
```

**Semantic conventions:**
```kotlin
// SpanAttributes.kt
object KonditionalSemanticAttributes {
    // Namespace
    const val FEATURE_NAMESPACE = "feature.namespace"
    const val FEATURE_KEY = "feature.key"
    const val FEATURE_TYPE = "feature.type"
    const val FEATURE_DEFAULT = "feature.default"

    // Evaluation result
    const val EVALUATION_VALUE = "evaluation.result.value"
    const val EVALUATION_DECISION = "evaluation.result.decision"
    const val EVALUATION_DURATION_NS = "evaluation.duration_ns"
    const val EVALUATION_CONFIG_VERSION = "evaluation.config_version"

    // Rule matching
    const val EVALUATION_RULE_NOTE = "evaluation.rule.note"
    const val EVALUATION_RULE_SPECIFICITY = "evaluation.rule.specificity"
    const val EVALUATION_BUCKET = "evaluation.bucket"
    const val EVALUATION_RAMP_UP = "evaluation.ramp_up"

    // Context (sanitized)
    const val CONTEXT_PLATFORM = "context.platform"
    const val CONTEXT_LOCALE = "context.locale"
    const val CONTEXT_VERSION = "context.version"
    const val CONTEXT_STABLE_ID_HASH = "context.stable_id.sha256"  // First 8 chars

    // Decision types
    object DecisionType {
        const val DEFAULT = "default"
        const val RULE_MATCHED = "rule_matched"
        const val INACTIVE = "inactive"
        const val REGISTRY_DISABLED = "registry_disabled"
    }
}
```

#### 3.1.2 Tracer Implementation

```kotlin
// FlagEvaluationTracer.kt
class FlagEvaluationTracer(
    private val tracer: Tracer,
    private val config: TracingConfig
) {
    fun <T : Any, C : Context> traceEvaluation(
        feature: Feature<T, C, *>,
        context: C,
        parentSpan: Span? = null,
        block: () -> EvaluationResult<T>
    ): T {
        // Check if tracing is enabled
        if (!config.enabled) {
            return block().value
        }

        // Check sampling decision
        if (!shouldSample(feature, context)) {
            return block().value
        }

        val spanBuilder = tracer.spanBuilder("feature.evaluation")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute(FEATURE_KEY, feature.key)
            .setAttribute(FEATURE_NAMESPACE, feature.namespace.id)

        // Link to parent span if provided
        parentSpan?.let { spanBuilder.setParent(Context.current().with(it)) }

        val span = spanBuilder.startSpan()

        return try {
            Context.current().with(span).makeCurrent().use {
                val result = block()

                // Populate span with evaluation details
                populateSpanFromResult(span, result, context)

                result.value
            }
        } catch (e: Exception) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR, e.message ?: "Evaluation failed")
            throw e
        } finally {
            span.end()
        }
    }

    private fun <T : Any> populateSpanFromResult(
        span: Span,
        result: EvaluationResult<T>,
        context: Context
    ) {
        // Result value (sanitized for cardinality)
        span.setAttribute(EVALUATION_VALUE, sanitizeValue(result.value))
        span.setAttribute(EVALUATION_DECISION, result.decision.toSpanValue())
        span.setAttribute(EVALUATION_CONFIG_VERSION, result.configVersion ?: "unknown")
        span.setAttribute(EVALUATION_DURATION_NS, result.durationNanos)

        // Context attributes (PII-safe)
        span.setAttribute(CONTEXT_PLATFORM, context.platform.id)
        span.setAttribute(CONTEXT_LOCALE, context.locale.id)
        span.setAttribute(CONTEXT_VERSION, context.appVersion.toString())
        span.setAttribute(CONTEXT_STABLE_ID_HASH,
            context.stableId.hexId.id.take(8))  // First 8 chars only

        // Rule details if matched
        when (val decision = result.decision) {
            is Decision.Rule -> {
                val matched = decision.matched
                matched.rule.note?.let {
                    span.setAttribute(EVALUATION_RULE_NOTE, it)
                }
                span.setAttribute(EVALUATION_RULE_SPECIFICITY,
                    matched.rule.totalSpecificity.toLong())
                span.setAttribute(EVALUATION_BUCKET, matched.bucket.bucket.toLong())
                span.setAttribute(EVALUATION_RAMP_UP, matched.rule.rollout.value)

                // Add event for skipped rules
                decision.skippedByRollout?.let { skipped ->
                    span.addEvent("rule_skipped", Attributes.of(
                        AttributeKey.stringKey("rule_note"), skipped.rule.note ?: "unnamed",
                        AttributeKey.stringKey("reason"), "ramp_up_excluded",
                        AttributeKey.longKey("bucket"), skipped.bucket.bucket.toLong(),
                        AttributeKey.doubleKey("ramp_up"), skipped.rule.rollout.value
                    ))
                }
            }
            is Decision.Default -> {
                decision.skippedByRollout?.let { skipped ->
                    span.addEvent("rule_skipped", Attributes.of(
                        AttributeKey.stringKey("rule_note"), skipped.rule.note ?: "unnamed",
                        AttributeKey.stringKey("reason"), "ramp_up_excluded"
                    ))
                }
            }
            else -> { /* No additional attributes */ }
        }
    }

    private fun shouldSample(feature: Feature<*, *, *>, context: Context): Boolean {
        // Sampling strategies
        return when (config.samplingStrategy) {
            SamplingStrategy.ALWAYS -> true
            SamplingStrategy.NEVER -> false
            SamplingStrategy.PARENT_BASED -> Span.current().spanContext.isSampled
            is SamplingStrategy.RATIO -> {
                // Use stable ID for consistent sampling per user
                val hash = context.stableId.hexId.id.hashCode()
                (hash % 100) < (config.samplingStrategy as SamplingStrategy.RATIO).percentage
            }
            is SamplingStrategy.FEATURE_FILTER -> {
                config.samplingStrategy.predicate(feature)
            }
        }
    }

    private fun <T : Any> sanitizeValue(value: T): String {
        // Prevent cardinality explosion
        return when (value) {
            is Boolean -> value.toString()
            is Number -> value.toString()
            is Enum<*> -> value.name
            is String -> if (value.length > 50) "${value.take(47)}..." else value
            else -> value::class.simpleName ?: "unknown"
        }
    }
}

// TracingConfig.kt
data class TracingConfig(
    val enabled: Boolean = true,
    val samplingStrategy: SamplingStrategy = SamplingStrategy.PARENT_BASED,
    val includeContextAttributes: Boolean = true,
    val includeRuleDetails: Boolean = true,
    val sanitizePii: Boolean = true
)

sealed class SamplingStrategy {
    object ALWAYS : SamplingStrategy()
    object NEVER : SamplingStrategy()
    object PARENT_BASED : SamplingStrategy()
    data class RATIO(val percentage: Int) : SamplingStrategy()
    data class FEATURE_FILTER(val predicate: (Feature<*, *, *>) -> Boolean) : SamplingStrategy()
}
```

#### 3.1.3 Public API Extension

```kotlin
// Add to konditional-opentelemetry module
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetry(
    context: C,
    telemetry: KonditionalTelemetry = KonditionalTelemetry.global(),
    registry: NamespaceRegistry = namespace,
    parentSpan: Span? = null
): T {
    return telemetry.tracer.traceEvaluation(this, context, parentSpan) {
        evaluateInternal(context, registry, mode = Metrics.Evaluation.EvaluationMode.NORMAL)
    }.also { result ->
        // Record metric with exemplar linking to trace
        telemetry.metrics.recordEvaluation(
            feature = this,
            result = result,
            traceId = Span.current().spanContext.traceId,
            spanId = Span.current().spanContext.spanId
        )
    }
}

// Convenience: auto-detect parent span from current context
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetry(
    context: C,
    telemetry: KonditionalTelemetry = KonditionalTelemetry.global()
): T = evaluateWithTelemetry(context, telemetry, parentSpan = Span.current())
```

#### 3.1.4 Usage Example

```kotlin
// Setup (once at app startup)
val otel = OpenTelemetry.noop()  // Or configured instance
val telemetry = KonditionalTelemetry(
    otel = otel,
    tracingConfig = TracingConfig(
        samplingStrategy = SamplingStrategy.RATIO(10)  // 10% sampling
    )
)

// Install globally
KonditionalTelemetry.install(telemetry)

// Usage (drop-in replacement)
// Before:
val enabled = AppFlags.darkMode.evaluate(context)

// After:
val enabled = AppFlags.darkMode.evaluateWithTelemetry(context)

// Or with explicit parent span:
tracer.spanBuilder("checkout.process").use { span ->
    val enabled = AppFlags.darkMode.evaluateWithTelemetry(
        context = context,
        parentSpan = span
    )
}
```

---

### 3.2 Phase 2: Metrics (Week 2-3)

#### 3.2.1 Metrics Model

**Metric categories:**

```yaml
# Evaluation performance
feature.evaluation.duration:
  type: histogram
  unit: milliseconds
  description: Time spent evaluating a feature flag
  attributes:
    - feature.namespace
    - feature.key
    - evaluation.decision
  buckets: [0.1, 0.5, 1, 2, 5, 10, 25, 50, 100]

feature.evaluation.count:
  type: counter
  unit: evaluations
  description: Total number of flag evaluations
  attributes:
    - feature.namespace
    - feature.key
    - evaluation.decision
    - context.platform

feature.evaluation.value:
  type: counter
  unit: evaluations
  description: Count of evaluations by result value
  attributes:
    - feature.namespace
    - feature.key
    - evaluation.value  # Only for low-cardinality values

# Configuration health
feature.config.load.duration:
  type: histogram
  unit: milliseconds
  description: Time to load configuration
  attributes:
    - feature.namespace

feature.config.age:
  type: gauge
  unit: seconds
  description: Age of current configuration
  attributes:
    - feature.namespace
    - config.version

feature.config.flags.count:
  type: gauge
  unit: flags
  description: Number of flags in current configuration
  attributes:
    - feature.namespace

feature.config.rollback.count:
  type: counter
  unit: rollbacks
  description: Count of configuration rollbacks
  attributes:
    - feature.namespace
    - rollback.reason

# Rule evaluation
feature.rule.matched.count:
  type: counter
  unit: matches
  description: Count of rule matches
  attributes:
    - feature.namespace
    - feature.key
    - rule.note

feature.rule.skipped.count:
  type: counter
  unit: skips
  description: Count of rules skipped by ramp-up
  attributes:
    - feature.namespace
    - feature.key
    - rule.note

feature.bucket.distribution:
  type: histogram
  unit: bucket
  description: Distribution of bucket assignments
  attributes:
    - feature.namespace
    - feature.key
  buckets: [0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000]

# Shadow evaluation
feature.shadow.mismatch.count:
  type: counter
  unit: mismatches
  description: Count of shadow evaluation mismatches
  attributes:
    - feature.namespace
    - feature.key
    - mismatch.kind  # value, decision

# Registry operations
feature.registry.override.active.count:
  type: gauge
  unit: overrides
  description: Number of active test overrides
  attributes:
    - feature.namespace
```

#### 3.2.2 Metrics Implementation

```kotlin
// OtelMetricsCollector.kt
class OtelMetricsCollector(
    private val meter: Meter,
    private val config: MetricsConfig
) : MetricsCollector {

    private val evaluationDuration: Histogram by lazy {
        meter.histogramBuilder("feature.evaluation.duration")
            .setDescription("Time spent evaluating a feature flag")
            .setUnit("ms")
            .build()
    }

    private val evaluationCount: LongCounter by lazy {
        meter.counterBuilder("feature.evaluation.count")
            .setDescription("Total number of flag evaluations")
            .build()
    }

    private val configAge: LongGauge by lazy {
        meter.gaugeBuilder("feature.config.age")
            .setDescription("Age of current configuration")
            .setUnit("s")
            .ofLongs()
            .buildObserver()
    }

    // ... other metrics

    override fun recordEvaluation(metric: Metrics.Evaluation) {
        if (!config.enabled) return

        val attrs = Attributes.builder()
            .put(FEATURE_NAMESPACE, metric.namespaceId)
            .put(FEATURE_KEY, metric.featureKey)
            .put(EVALUATION_DECISION, metric.decision)
            .put(CONTEXT_PLATFORM, metric.contextPlatform)

        // Add exemplar with trace context if available
        if (config.enableExemplars && metric.traceId != null) {
            attrs.put("trace_id", metric.traceId)
                .put("span_id", metric.spanId)
        }

        evaluationDuration.record(
            metric.durationNanos / 1_000_000.0,  // Convert to ms
            attrs.build()
        )

        evaluationCount.add(1, attrs.build())

        // Record value distribution (only for low-cardinality)
        if (shouldRecordValue(metric)) {
            valueDistribution.add(1, attrs
                .put(EVALUATION_VALUE, sanitizeValue(metric.value))
                .build())
        }
    }

    override fun recordConfigLoad(metric: Metrics.ConfigLoadMetric) {
        val attrs = Attributes.builder()
            .put(FEATURE_NAMESPACE, metric.namespaceId)
            .put(CONFIG_VERSION, metric.version ?: "unknown")
            .build()

        configLoadDuration.record(metric.durationMs, attrs)

        flagsCount.record(metric.featureCount.toLong(), attrs)
    }

    private fun shouldRecordValue(metric: Metrics.Evaluation): Boolean {
        // Only record values for low-cardinality types
        return when (metric.valueType) {
            "boolean", "enum" -> true
            "string", "int", "double" -> config.recordHighCardinalityValues
            else -> false
        }
    }

    // Periodic callback to update gauge metrics
    fun registerCallbacks(registry: NamespaceRegistry) {
        meter.gaugeBuilder("feature.config.age")
            .setDescription("Age of current configuration")
            .setUnit("s")
            .buildWithCallback { measurement ->
                val ageSeconds = (System.currentTimeMillis() -
                    registry.configuration.metadata.generatedAtEpochMillis) / 1000

                measurement.record(
                    ageSeconds,
                    Attributes.of(
                        AttributeKey.stringKey(FEATURE_NAMESPACE), registry.namespaceId,
                        AttributeKey.stringKey(CONFIG_VERSION),
                            registry.configuration.metadata.version ?: "unknown"
                    )
                )
            }
    }
}

// MetricsConfig.kt
data class MetricsConfig(
    val enabled: Boolean = true,
    val enableExemplars: Boolean = true,
    val recordHighCardinalityValues: Boolean = false,
    val cardinalityLimits: Map<String, Int> = mapOf(
        "feature.evaluation.value" to 100,
        "rule.note" to 500
    )
)
```

#### 3.2.3 Exemplar Support (Link Metrics → Traces)

```kotlin
// Extension to add trace context to metrics
fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.evaluateWithTelemetry(
    context: C,
    telemetry: KonditionalTelemetry
): T {
    val span = Span.current()
    val traceId = span.spanContext.traceId
    val spanId = span.spanContext.spanId

    return telemetry.tracer.traceEvaluation(this, context) { result ->
        // Record metric WITH exemplar
        telemetry.metrics.recordEvaluation(
            Metrics.Evaluation(
                // ... standard fields
                traceId = traceId,      // Link to trace!
                spanId = spanId
            )
        )
        result
    }
}
```

**Exemplar visualization:**
```
# In Grafana/Prometheus:
# Click on metric spike → "View trace" → opens Jaeger/Tempo with exact evaluation
```

---

### 3.3 Phase 3: Structured Logging (Week 3)

#### 3.3.1 Log Event Model

```kotlin
// StructuredLogEvent.kt
sealed class KonditionalLogEvent {
    abstract val timestamp: Instant
    abstract val level: Level
    abstract val message: String
    abstract val attributes: Map<String, Any?>

    enum class Level { ERROR, WARN, INFO, DEBUG, TRACE }

    data class EvaluationEvent(
        override val timestamp: Instant,
        override val level: Level,
        override val message: String,
        val namespaceId: String,
        val featureKey: String,
        val decision: String,
        val value: String,
        val durationNanos: Long,
        val traceId: String?,
        val spanId: String?
    ) : KonditionalLogEvent() {
        override val attributes = mapOf(
            "event.type" to "evaluation",
            "feature.namespace" to namespaceId,
            "feature.key" to featureKey,
            "evaluation.decision" to decision,
            "evaluation.value" to value,
            "evaluation.duration_ns" to durationNanos,
            "trace_id" to traceId,
            "span_id" to spanId
        )
    }

    data class ConfigLoadEvent(
        override val timestamp: Instant,
        override val level: Level,
        override val message: String,
        val namespaceId: String,
        val version: String?,
        val flagCount: Int,
        val durationMs: Long
    ) : KonditionalLogEvent() {
        override val attributes = mapOf(
            "event.type" to "config_load",
            "feature.namespace" to namespaceId,
            "config.version" to version,
            "config.flag_count" to flagCount,
            "config.load_duration_ms" to durationMs
        )
    }

    data class ShadowMismatchEvent(
        override val timestamp: Instant,
        override val level: Level,
        override val message: String,
        val namespaceId: String,
        val featureKey: String,
        val baselineValue: String,
        val candidateValue: String,
        val mismatchKinds: Set<String>
    ) : KonditionalLogEvent() {
        override val attributes = mapOf(
            "event.type" to "shadow_mismatch",
            "feature.namespace" to namespaceId,
            "feature.key" to featureKey,
            "baseline.value" to baselineValue,
            "candidate.value" to candidateValue,
            "mismatch.kinds" to mismatchKinds.joinToString(",")
        )
    }
}
```

#### 3.3.2 OTel Logger Implementation

```kotlin
// OtelLogger.kt
class OtelLogger(
    private val logger: io.opentelemetry.api.logs.Logger,
    private val config: LoggingConfig
) : KonditionalLogger {

    override fun warn(message: () -> String, throwable: Throwable?) {
        if (!config.enabled || config.level < Level.WARN) return

        emit(Level.WARN, message(), throwable)
    }

    override fun info(message: () -> String) {
        if (!config.enabled || config.level < Level.INFO) return

        emit(Level.INFO, message(), null)
    }

    override fun debug(message: () -> String) {
        if (!config.enabled || config.level < Level.DEBUG) return

        emit(Level.DEBUG, message(), null)
    }

    private fun emit(level: Level, message: String, throwable: Throwable?) {
        val logRecordBuilder = logger.logRecordBuilder()
            .setSeverity(level.toOtelSeverity())
            .setBody(message)
            .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)

        // Add trace context automatically
        val span = Span.current()
        if (span.spanContext.isValid) {
            logRecordBuilder.setSpanContext(span.spanContext)
        }

        // Add exception if present
        throwable?.let {
            logRecordBuilder.setAttribute(
                AttributeKey.stringKey("exception.type"),
                it::class.qualifiedName ?: "unknown"
            )
            logRecordBuilder.setAttribute(
                AttributeKey.stringKey("exception.message"),
                it.message ?: ""
            )
            logRecordBuilder.setAttribute(
                AttributeKey.stringArrayKey("exception.stacktrace"),
                it.stackTraceToString().lines()
            )
        }

        logRecordBuilder.emit()
    }

    // Convenience for structured events
    fun emit(event: KonditionalLogEvent) {
        if (!config.enabled || config.level < event.level) return

        val logRecordBuilder = logger.logRecordBuilder()
            .setSeverity(event.level.toOtelSeverity())
            .setBody(event.message)
            .setTimestamp(event.timestamp.toEpochMilli(), TimeUnit.MILLISECONDS)

        // Add all attributes
        event.attributes.forEach { (key, value) ->
            when (value) {
                is String -> logRecordBuilder.setAttribute(AttributeKey.stringKey(key), value)
                is Long -> logRecordBuilder.setAttribute(AttributeKey.longKey(key), value)
                is Double -> logRecordBuilder.setAttribute(AttributeKey.doubleKey(key), value)
                is Boolean -> logRecordBuilder.setAttribute(AttributeKey.booleanKey(key), value)
                null -> { /* Skip null values */ }
            }
        }

        logRecordBuilder.emit()
    }
}

data class LoggingConfig(
    val enabled: Boolean = true,
    val level: Level = Level.INFO,
    val includeTraceContext: Boolean = true
)
```

---

## 4. Integration Patterns

### 4.1 Automatic Instrumentation (Spring Boot Example)

```kotlin
// Auto-configuration for Spring Boot
@Configuration
@ConditionalOnClass(OpenTelemetry::class)
@ConditionalOnProperty("konditional.telemetry.enabled", havingValue = "true", matchIfMissing = true)
class KonditionalTelemetryAutoConfiguration {

    @Bean
    fun konditionalTelemetry(
        openTelemetry: OpenTelemetry,
        @Value("\${konditional.telemetry.tracing.sampling-ratio:0.1}") samplingRatio: Double,
        @Value("\${konditional.telemetry.metrics.exemplars:true}") enableExemplars: Boolean
    ): KonditionalTelemetry {
        return KonditionalTelemetry(
            otel = openTelemetry,
            tracingConfig = TracingConfig(
                samplingStrategy = SamplingStrategy.RATIO((samplingRatio * 100).toInt())
            ),
            metricsConfig = MetricsConfig(
                enableExemplars = enableExemplars
            )
        )
    }

    @Bean
    fun registryHooksCustomizer(telemetry: KonditionalTelemetry): BeanPostProcessor {
        return object : BeanPostProcessor {
            override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
                if (bean is Namespace) {
                    bean.setHooks(
                        RegistryHooks.of(
                            logger = telemetry.logger,
                            metrics = telemetry.metrics
                        )
                    )
                }
                return bean
            }
        }
    }
}
```

**application.yml:**
```yaml
konditional:
  telemetry:
    enabled: true
    tracing:
      sampling-ratio: 0.1  # 10% of evaluations
      include-context: true
      include-rules: true
    metrics:
      enabled: true
      exemplars: true
      high-cardinality-values: false
    logging:
      level: INFO
      include-trace-context: true
```

### 4.2 Manual Instrumentation

```kotlin
// Setup
val otel = GlobalOpenTelemetry.get()
val telemetry = KonditionalTelemetry(otel)
KonditionalTelemetry.install(telemetry)

// Per-namespace hooks
AppFlags.setHooks(
    RegistryHooks.of(
        logger = telemetry.logger,
        metrics = telemetry.metrics
    )
)

// Usage: Automatic parent span detection
fun handleRequest(request: Request): Response {
    // OpenTelemetry auto-instrumentation creates request span
    // Flag evaluation automatically links as child span
    val enabled = AppFlags.darkMode.evaluateWithTelemetry(context)

    if (enabled) {
        // ...
    }
}
```

### 4.3 Trace Context Propagation

```kotlin
// Propagate context across service boundaries
class FlagEvaluationClient(
    private val httpClient: HttpClient,
    private val telemetry: KonditionalTelemetry
) {
    suspend fun evaluateRemote(
        featureKey: String,
        context: Context
    ): Boolean {
        val span = telemetry.tracer.startSpan("remote_flag_evaluation")

        return span.use {
            // Inject trace context into HTTP headers
            val headers = W3CTraceContextPropagator.getInstance()
                .inject(Context.current().with(span)) { carrier, key, value ->
                    carrier[key] = value
                    mutableMapOf<String, String>()
                }

            httpClient.post("/api/flags/evaluate") {
                headers.forEach { (k, v) -> header(k, v) }
                body = EvaluationRequest(featureKey, context)
            }.body()
        }
    }
}
```

---

## 5. Dashboard & Alerting

### 5.1 Grafana Dashboards

**Dashboard 1: Flag Evaluation Performance**
```json
{
  "title": "Konditional - Flag Evaluations",
  "panels": [
    {
      "title": "Evaluation Rate",
      "targets": [{
        "expr": "rate(feature_evaluation_count[5m])",
        "legendFormat": "{{feature_namespace}}.{{feature_key}}"
      }]
    },
    {
      "title": "P99 Evaluation Latency",
      "targets": [{
        "expr": "histogram_quantile(0.99, rate(feature_evaluation_duration_bucket[5m]))",
        "legendFormat": "{{feature_key}}"
      }]
    },
    {
      "title": "Decision Distribution",
      "targets": [{
        "expr": "sum by (evaluation_decision) (rate(feature_evaluation_count[5m]))"
      }]
    },
    {
      "title": "Bucket Distribution",
      "targets": [{
        "expr": "histogram_quantile(0.5, rate(feature_bucket_distribution_bucket[5m]))"
      }]
    }
  ]
}
```

**Dashboard 2: Configuration Health**
```json
{
  "title": "Konditional - Configuration Health",
  "panels": [
    {
      "title": "Config Age",
      "targets": [{
        "expr": "feature_config_age{feature_namespace='$namespace'}",
        "legendFormat": "{{config_version}}"
      }],
      "alert": {
        "condition": "feature_config_age > 3600",  // 1 hour
        "message": "Configuration is stale (>1h old)"
      }
    },
    {
      "title": "Rollback Rate",
      "targets": [{
        "expr": "rate(feature_config_rollback_count[5m])"
      }]
    },
    {
      "title": "Shadow Mismatches",
      "targets": [{
        "expr": "rate(feature_shadow_mismatch_count[5m])"
      }]
    }
  ]
}
```

### 5.2 Alerting Rules

```yaml
# Prometheus alerting rules
groups:
  - name: konditional
    interval: 30s
    rules:
      # High error rate
      - alert: FlagEvaluationErrorRate
        expr: |
          rate(feature_evaluation_count{evaluation_decision="error"}[5m]) > 0.01
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High flag evaluation error rate"
          description: "{{ $labels.feature_key }} has {{ $value }} errors/sec"

      # Slow evaluations
      - alert: FlagEvaluationSlow
        expr: |
          histogram_quantile(0.99,
            rate(feature_evaluation_duration_bucket[5m])
          ) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Flag evaluation P99 latency > 10ms"
          description: "{{ $labels.feature_key }} P99: {{ $value }}ms"

      # Stale configuration
      - alert: StaleConfiguration
        expr: feature_config_age > 3600
        for: 5m
        labels:
          severity: info
        annotations:
          summary: "Configuration is stale"
          description: "{{ $labels.feature_namespace }} config age: {{ $value }}s"

      # Shadow mode mismatches
      - alert: ShadowMismatchRate
        expr: |
          rate(feature_shadow_mismatch_count[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High shadow evaluation mismatch rate"
          description: "{{ $labels.feature_key }}: {{ $value }} mismatches/sec"
```

---

## 6. Performance Impact Analysis

### 6.1 Overhead Measurements

**Baseline (no instrumentation):**
```
Flag evaluation: 2-5μs per evaluation
Throughput: 200K-500K evals/sec/core
```

**With full instrumentation:**
```
Span creation: ~500ns
Attribute population: ~200ns
Metric recording: ~100ns
Total overhead: ~800ns
```

**Impact:**
- **Latency:** +800ns (+16-40% for simple flags)
- **Throughput:** Still >150K evals/sec/core
- **Memory:** +50 bytes per active span

**Mitigation:**
```kotlin
// Sampling reduces overhead
TracingConfig(
    samplingStrategy = SamplingStrategy.RATIO(10)  // Only 10% traced
)
// Effective overhead: ~80ns average (10% × 800ns)
```

### 6.2 Cardinality Management

**High-risk dimensions:**
```kotlin
// ❌ BAD: Unbounded cardinality
.setAttribute("evaluation.value", userInput)  // Could be millions of values

// ✅ GOOD: Bounded cardinality
.setAttribute("evaluation.value_type", "string")  // Only ~6 values
.setAttribute("evaluation.decision", decision.type)  // Only ~4 values
```

**Cardinality limits:**
```
feature.namespace: ~10-100 (# of namespaces)
feature.key: ~100-10,000 (# of flags)
evaluation.decision: 4 (default, rule_matched, inactive, disabled)
context.platform: ~3-5 (IOS, ANDROID, WEB)
context.locale: ~20-200 (supported locales)
rule.note: ~100-1000 (# of unique rule notes)
```

**Total metric series estimate:**
```
feature.evaluation.count =
  namespaces × flags × decisions × platforms × locales
  = 10 × 1000 × 4 × 5 × 50
  = 10M series (too high!)
```

**Reduction strategies:**
1. **Namespace-level aggregation:** Record per-namespace, not per-flag
2. **Sampling:** Only record 10% of evaluations
3. **Label allowlist:** Only add platform/locale for critical flags
4. **Exemplars:** Use exemplars to drill into specifics without high cardinality

---

## 7. Testing Strategy

### 7.1 Unit Tests

```kotlin
class OtelMetricsCollectorTest {
    private val meterProvider = SdkMeterProvider.builder()
        .registerMetricReader(InMemoryMetricReader.create())
        .build()

    @Test
    fun `records evaluation metrics with correct attributes`() {
        val collector = OtelMetricsCollector(
            meter = meterProvider.get("test"),
            config = MetricsConfig(enabled = true)
        )

        collector.recordEvaluation(
            Metrics.Evaluation(
                namespaceId = "test",
                featureKey = "darkMode",
                decision = "rule_matched",
                // ...
            )
        )

        val metrics = meterProvider.collectAllMetrics()
        val evaluationCount = metrics.find { it.name == "feature.evaluation.count" }

        assertNotNull(evaluationCount)
        assertEquals(1L, evaluationCount!!.longSumData.points.first().value)
    }
}
```

### 7.2 Integration Tests

```kotlin
@SpringBootTest
@AutoConfigureOpenTelemetry
class KonditionalTelemetryIntegrationTest {

    @Autowired
    lateinit var telemetry: KonditionalTelemetry

    @Test
    fun `creates evaluation spans with trace context`() {
        val tracer = GlobalOpenTelemetry.getTracer("test")
        val parentSpan = tracer.spanBuilder("parent").startSpan()

        parentSpan.use {
            val result = TestFlags.myFlag.evaluateWithTelemetry(
                context = testContext,
                telemetry = telemetry
            )

            // Verify span was created as child
            val spans = spanExporter.finishedSpans
            assertEquals(2, spans.size)  // parent + evaluation

            val evalSpan = spans.find { it.name == "feature.evaluation" }
            assertNotNull(evalSpan)
            assertEquals(parentSpan.spanContext.traceId, evalSpan!!.traceId)
        }
    }
}
```

### 7.3 Performance Tests

```kotlin
@Test
fun `instrumentation overhead is less than 20 percent`() {
    val iterations = 100_000

    // Baseline
    val baselineStart = System.nanoTime()
    repeat(iterations) {
        TestFlags.myFlag.evaluate(context)
    }
    val baselineDuration = System.nanoTime() - baselineStart

    // With instrumentation
    val instrumentedStart = System.nanoTime()
    repeat(iterations) {
        TestFlags.myFlag.evaluateWithTelemetry(context, telemetry)
    }
    val instrumentedDuration = System.nanoTime() - instrumentedStart

    val overhead = (instrumentedDuration - baselineDuration).toDouble() / baselineDuration
    assertTrue(overhead < 0.20, "Overhead was ${overhead * 100}%")
}
```

---

## 8. Migration Guide

### 8.1 Phased Rollout

**Phase 1: Metrics Only (Week 1)**
```kotlin
// Add dependency
dependencies {
    implementation("io.github.amichne:konditional-opentelemetry:0.1.0")
}

// Configure
val telemetry = KonditionalTelemetry(
    otel = GlobalOpenTelemetry.get(),
    tracingConfig = TracingConfig(enabled = false),  // Traces off
    metricsConfig = MetricsConfig(enabled = true)    // Metrics on
)

AppFlags.setHooks(RegistryHooks.of(metrics = telemetry.metrics))
```

**Phase 2: Tracing at 1% (Week 2)**
```kotlin
tracingConfig = TracingConfig(
    enabled = true,
    samplingStrategy = SamplingStrategy.RATIO(1)  // 1% sampling
)
```

**Phase 3: Increase to 10% (Week 3)**
```kotlin
samplingStrategy = SamplingStrategy.RATIO(10)  // 10% sampling
```

**Phase 4: Full Rollout (Week 4)**
```kotlin
samplingStrategy = SamplingStrategy.PARENT_BASED  // Use parent decision
```

### 8.2 Backward Compatibility

```kotlin
// Old code still works (no breaking changes)
val enabled = AppFlags.darkMode.evaluate(context)

// New code is opt-in
val enabled = AppFlags.darkMode.evaluateWithTelemetry(context)

// Can mix both in same codebase during migration
```

---

## 9. Cost Estimation

### 9.1 Infrastructure Costs

**Assumptions:**
- 1000 flags
- 10,000 requests/sec
- 10% trace sampling
- 30-day retention

**Trace storage:**
```
Traces/day = 10,000 req/sec × 86,400 sec/day × 0.10 sampling
           = 86.4M traces/day

Span size = ~2KB per evaluation span
Storage/day = 86.4M × 2KB = 172GB/day
30-day retention = 5.2TB

Cost (Grafana Cloud Tempo): ~$100/month for 5TB
Cost (Self-hosted Tempo + S3): ~$50/month
```

**Metric storage:**
```
Series count = ~100K (with cardinality limits)
Samples/sec = 10K evaluations × 3 metrics = 30K samples/sec

Cost (Grafana Cloud Metrics): ~$50/month
Cost (Prometheus + Thanos): ~$20/month (self-hosted)
```

**Total monthly cost: $100-150/month** (cloud) or **$70/month** (self-hosted)

### 9.2 Performance Cost

**CPU overhead:**
```
Instrumentation overhead: ~800ns per evaluation
At 10% sampling: 80ns average overhead
10K req/sec × 80ns = 0.08% CPU usage

Negligible impact.
```

---

## 10. Deliverables Checklist

### Phase 1: Traces (Weeks 1-2)
- [ ] `konditional-opentelemetry` module setup
- [ ] `FlagEvaluationTracer` implementation
- [ ] `SpanAttributes` semantic conventions
- [ ] `evaluateWithTelemetry()` extension function
- [ ] `TracingConfig` with sampling strategies
- [ ] Unit tests for tracer
- [ ] Integration tests with test spans
- [ ] Performance benchmark (<20% overhead)
- [ ] Documentation and examples

### Phase 2: Metrics (Week 2-3)
- [ ] `OtelMetricsCollector` implementation
- [ ] All 12 metric definitions
- [ ] Exemplar support (link to traces)
- [ ] `MetricsConfig` with cardinality limits
- [ ] Gauge callbacks for config age
- [ ] Unit tests for metrics
- [ ] Cardinality validation tests
- [ ] Grafana dashboard JSON templates
- [ ] Prometheus alerting rules

### Phase 3: Logging (Week 3)
- [ ] `OtelLogger` implementation
- [ ] `StructuredLogEvent` model
- [ ] Trace context injection
- [ ] `LoggingConfig` setup
- [ ] Unit tests for logger
- [ ] Log correlation examples

### Phase 4: Integration (Week 3)
- [ ] Spring Boot auto-configuration
- [ ] Manual setup examples
- [ ] Migration guide
- [ ] Performance impact analysis
- [ ] Cost estimation
- [ ] Dashboard templates
- [ ] Alerting rule templates

---

## 11. Success Metrics

### 11.1 Adoption Metrics

- [ ] 100% of namespaces have hooks configured
- [ ] 90% of evaluations use `evaluateWithTelemetry()`
- [ ] 80% of engineers can navigate traces in Grafana

### 11.2 Performance Metrics

- [ ] <10% increase in P99 evaluation latency
- [ ] <1% increase in CPU utilization
- [ ] <5TB trace storage per month

### 11.3 Operational Metrics

- [ ] MTTD (Mean Time to Detect) for flag issues: <5 minutes
- [ ] MTTR (Mean Time to Resolve) for misconfigurations: <15 minutes
- [ ] 100% of production incidents have trace links

---

## 12. Future Enhancements

### 12.1 Short-term (3-6 months)
- **Automatic anomaly detection:** ML-based alerts for unusual evaluation patterns
- **Flag impact analysis:** Correlate flag changes to business metrics
- **Trace sampling intelligence:** Sample more during incidents

### 12.2 Long-term (6-12 months)
- **Distributed configuration tracing:** Trace config propagation across services
- **Real-time debugging:** Live evaluation viewer in admin UI
- **A/B test analysis integration:** Export evaluation data to analytics platforms

---

**Plan Complete**
**Next Steps:** Review with SRE team, prioritize phases, assign resources
