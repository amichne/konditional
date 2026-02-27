# Enterprise Architecture Patterns

## Observability Integration

### OpenTelemetry Hooks

APIs should expose integration points for observability without coupling to specific implementations.

**Key patterns:**

#### Tracing Context Propagation
```kotlin
// Good: Accept context as parameter
suspend fun process(
    request: Request,
    context: TracingContext = TracingContext.current()
): Result

// Alternative: Context receiver
context(TracingContext)
suspend fun process(request: Request): Result
```

#### Span Creation Hooks
```kotlin
interface SpanFactory {
    fun startSpan(name: String): Span
    fun startSpan(name: String, parent: Span): Span
}

// Allow injection
class Service(private val spanFactory: SpanFactory = NoOpSpanFactory) {
    fun operation() {
        spanFactory.startSpan("operation").use { span ->
            // work
            span.addEvent("milestone")
        }
    }
}
```

#### Metrics Exposition
```kotlin
// Provide observable metrics
interface Metrics {
    val requestCount: Long
    val errorCount: Long
    val averageLatency: Duration
}

interface Observable {
    fun getMetrics(): Metrics
}
```

#### Structured Logging
```kotlin
// Support structured context
interface Logger {
    fun info(message: String, context: Map<String, Any>)
}

// Example usage
logger.info("Request processed", mapOf(
    "requestId" to id,
    "duration" to duration.inWholeMilliseconds,
    "status" to status.name
))
```

### Observability Anti-Patterns

- **Hardcoded instrumentation**: Tightly coupling to specific OTel SDK versions
- **No opt-out**: Forcing instrumentation even when unwanted
- **Missing correlation IDs**: Not propagating request context through call chains
- **Opaque operations**: No visibility into what's happening internally
- **Excessive instrumentation**: Tracing every function kills performance

### Observability Checklist

- [ ] Trace context propagation through async boundaries
- [ ] Metrics for key operations (requests, errors, latency)
- [ ] Structured logging with correlation IDs
- [ ] Health check endpoints
- [ ] Readiness/liveness probes for containers
- [ ] Resource usage metrics (memory, CPU, connections)
- [ ] Ability to inject custom span processors
- [ ] Support for distributed tracing headers (W3C, B3)

## Stateless Architecture Patterns

### Shared-Nothing Design
Each request is independent; no shared mutable state between requests.

**Implications:**
- No static mutable state
- No caching without external store (Redis, Hazelcast)
- No in-memory session state
- Idempotent operations where possible

### Request Scoping
```kotlin
// Good: Request-scoped context
class RequestContext(
    val requestId: String,
    val userId: String,
    val timestamp: Instant
)

// Pass context explicitly or use context receivers
fun handleRequest(context: RequestContext, data: Data): Response
```

### External State Management
When state is needed:
- **Distributed cache**: Redis, Memcached for transient data
- **Database**: PostgreSQL, DynamoDB for persistent data
- **Message queues**: Kafka, SQS for event-driven state

### Stateless Anti-Patterns
- **Thread-local storage**: Breaks in async/coroutine contexts
- **Static caches**: Memory leaks and stale data
- **In-memory sessions**: Breaks horizontal scaling
- **Singleton services with mutable state**: Race conditions and data corruption

## Scalability Patterns

### Horizontal Scaling Compatibility

#### Stateless Operations
All operations must work correctly when load-balanced across multiple instances:
- No in-memory coordination
- No expectation of sticky sessions
- No local filesystem dependencies

#### Idempotency
Operations should be safely retryable:
```kotlin
// Use idempotency keys for mutations
data class CreateRequest(
    val idempotencyKey: String,
    val data: Data
)

// Store results keyed by idempotency key
interface IdempotentStore {
    fun getOrCreate(key: String, create: () -> Result): Result
}
```

#### Partitioning Strategies
- **Hash-based**: Consistent hashing for cache/database sharding
- **Range-based**: Time-based partitioning for logs/events
- **Geography-based**: Regional data isolation

### Bulkhead Pattern
Isolate resource pools to prevent cascading failures:
```kotlin
// Separate thread pools/coroutine dispatchers
val dbDispatcher = Dispatchers.IO.limitedParallelism(20)
val apiDispatcher = Dispatchers.IO.limitedParallelism(50)

suspend fun dbQuery() = withContext(dbDispatcher) { /* ... */ }
suspend fun apiCall() = withContext(apiDispatcher) { /* ... */ }
```

### Circuit Breaker
Fail fast when downstream services are unavailable:
```kotlin
interface CircuitBreaker {
    suspend fun <T> execute(operation: suspend () -> T): T
}

// Configurable thresholds
class CircuitBreakerConfig(
    val failureThreshold: Int = 5,
    val timeout: Duration = 30.seconds,
    val resetTimeout: Duration = 60.seconds
)
```

### Rate Limiting
Protect services from overload:
```kotlin
interface RateLimiter {
    suspend fun acquire(permits: Int = 1): Boolean
}

// Token bucket implementation
class TokenBucket(
    val capacity: Int,
    val refillRate: Int,
    val refillPeriod: Duration
) : RateLimiter
```

## Backwards Compatibility

### API Evolution Strategy

#### Additive Changes (Safe)
- New optional parameters with defaults
- New methods/functions
- New classes/interfaces
- Extension functions
- New enum values (with exhaustive when handled properly)

#### Breaking Changes (Unsafe)
- Removing public API
- Renaming public API
- Changing method signatures
- Reordering parameters
- Changing return types
- Removing enum values
- Changing class to interface or vice versa

#### Deprecation Workflow
```kotlin
@Deprecated(
    message = "Use newMethod instead",
    replaceWith = ReplaceWith("newMethod(param)"),
    level = DeprecationLevel.WARNING // then ERROR, then remove
)
fun oldMethod(param: String) = newMethod(param)

fun newMethod(param: String) { /* ... */ }
```

### Versioning Strategies

#### URI Versioning (REST APIs)
```
/v1/users
/v2/users
```

#### Header Versioning
```
Accept: application/vnd.company.v2+json
```

#### Content Negotiation
```kotlin
sealed interface Response {
    data class V1(val data: String) : Response
    data class V2(val data: DataV2) : Response
}

fun handleRequest(version: ApiVersion): Response
```

### Schema Evolution

#### Forward Compatibility
New code can read old data:
- Add optional fields with defaults
- Never remove required fields
- Validate unknown fields are preserved

#### Backward Compatibility  
Old code can read new data:
- New fields are optional
- Old consumers ignore unknown fields
- No semantic changes to existing fields

#### Data Migration
```kotlin
interface SchemaVersion {
    fun migrate(data: JsonNode): JsonNode
}

class SchemaV1ToV2 : SchemaVersion {
    override fun migrate(data: JsonNode): JsonNode {
        // Transform V1 -> V2
    }
}
```

## Multi-Tenancy Patterns

### Tenant Isolation
```kotlin
// Explicit tenant context
data class TenantContext(val tenantId: String)

context(TenantContext)
fun getData(): List<Data>

// Row-level security in queries
// SELECT * FROM data WHERE tenant_id = ?
```

### Configuration Isolation
- Tenant-specific feature flags
- Tenant-specific rate limits
- Tenant-specific SLAs

### Data Isolation Strategies
- **Shared database, shared schema**: Tenant ID in every table
- **Shared database, separate schemas**: Schema per tenant
- **Separate databases**: Database per tenant (highest isolation)

## Performance Patterns

### Lazy Initialization
```kotlin
val expensive: ExpensiveResource by lazy {
    createExpensiveResource()
}
```

### Object Pooling
For expensive-to-create objects in hot paths:
```kotlin
interface ObjectPool<T> {
    suspend fun borrow(): T
    suspend fun return(obj: T)
}
```

### Caching Strategies
```kotlin
interface Cache<K, V> {
    suspend fun get(key: K): V?
    suspend fun put(key: K, value: V)
    suspend fun getOrPut(key: K, compute: suspend () -> V): V
}

// TTL-based eviction
class TTLCache<K, V>(val ttl: Duration) : Cache<K, V>
```

### Batch Processing
```kotlin
interface BatchProcessor<T, R> {
    suspend fun process(items: List<T>): List<R>
}

// Accumulate requests, process in batches
class BatchingProcessor<T, R>(
    val batchSize: Int,
    val maxWait: Duration,
    val processor: BatchProcessor<T, R>
)
```

## Resilience Patterns

### Retry with Backoff
```kotlin
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelay: Duration = 100.milliseconds,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: RetryableException) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).coerceAtMost(30.seconds)
        }
    }
    return block() // Final attempt
}
```

### Timeout Handling
```kotlin
suspend fun <T> withTimeout(
    timeout: Duration,
    block: suspend () -> T
): T = kotlinx.coroutines.withTimeout(timeout.inWholeMilliseconds, block)
```

### Fallback Strategies
```kotlin
suspend fun <T> withFallback(
    primary: suspend () -> T,
    fallback: suspend () -> T
): T = try {
    primary()
} catch (e: Exception) {
    fallback()
}
```

## Security Patterns

### Input Validation
- Validate at boundaries
- Fail-fast on invalid input
- Sanitize before processing

### Secure Defaults
- Encryption enabled by default
- Minimal permissions
- Audit logging on by default

### Defense in Depth
- Multiple layers of validation
- Least privilege access
- Assume breach mentality

## Configuration Management

### Externalized Configuration
```kotlin
interface ConfigSource {
    fun getString(key: String): String?
    fun getInt(key: String): Int?
}

// Layered configuration: defaults < env < file < runtime
class LayeredConfig(
    val sources: List<ConfigSource>
) : ConfigSource
```

### Feature Flags
```kotlin
interface FeatureFlags {
    fun isEnabled(feature: String, context: Context): Boolean
}

// Support gradual rollouts
class GradualRollout(
    val percentage: Int,
    val userHasher: (String) -> Int
) : FeatureFlags
```

### Environment-Specific Config
- Development: Verbose logging, relaxed timeouts
- Staging: Production-like, safe for testing
- Production: Optimized, strict validation

## Deployment Considerations

### Health Checks
```kotlin
interface HealthCheck {
    suspend fun check(): HealthStatus
}

sealed interface HealthStatus {
    data object Healthy : HealthStatus
    data class Unhealthy(val reason: String) : HealthStatus
}
```

### Graceful Shutdown
```kotlin
interface Lifecycle {
    suspend fun start()
    suspend fun stop(timeout: Duration)
}

// Drain connections, finish in-flight requests
```

### Zero-Downtime Deployments
- Rolling updates
- Blue-green deployments
- Canary releases
