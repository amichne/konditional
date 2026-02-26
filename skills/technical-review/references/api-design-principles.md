# API Design Principles

## Core Principles

### Principle of Least Surprise
API behavior should match user expectations based on naming and documentation. Avoid clever abstractions that require deep understanding of internals.

### Progressive Disclosure
Simple cases should be simple; complex cases should be possible. Start with sensible defaults, allow customization through optional parameters or builder patterns.

### Fail-Fast Principle
Errors should surface at compile-time when possible, at initialization time when not, and never silently at runtime.

## Type Safety Guarantees

### Compile-Time Verification
- **Phantom types**: Encode state in type system to prevent invalid operations.
- **Sealed hierarchies**: Make illegal states unrepresentable.
- **Inline value classes**: Zero-cost type safety for primitives.
- **Context receivers**: Type-safe dependency injection without boilerplate.

### Error Handling Without Wrappers
- **Return types encode success/failure**: Use sealed classes, not Result<T>.
- **Exceptions for truly exceptional cases**: API misuse should be compile error, not runtime exception.
- **Internal error types**: Consumers shouldn't handle implementation errors.

Example pattern:
```kotlin
sealed interface EvaluationOutcome<out T> {
    data class Enabled<T>(val value: T) : EvaluationOutcome<T>
    data object Disabled : EvaluationOutcome<Nothing>
}

// Consumer never sees errors - they're handled internally
fun evaluate(): EvaluationOutcome<Config>
```

## API Surface Design

### Minimalism
Every public member is a commitment. Default to internal visibility; make public only what must be.

**Review questions:**
- Could this be internal?
- Is there a simpler way to achieve the same result?
- Can multiple functions be unified?

### Consistency
- **Naming patterns**: Use consistent prefixes/suffixes (create/build, get/find, update/modify).
- **Parameter ordering**: Consistent across related functions (receiver, required, optional).
- **Return type patterns**: Don't mix nullable and Result types for similar operations.

### Orthogonality
Each API element should do one thing independently. Avoid interdependencies between unrelated features.

**Red flags:**
- Functions that require calling others in specific order
- Configuration options that conflict with each other
- State that affects unrelated operations

## Interface Design

### Segregation
Prefer multiple focused interfaces over one large interface. Clients should depend only on methods they use.

```kotlin
// Good: Focused interfaces
interface Observable {
    fun observe(observer: Observer)
}

interface Configurable {
    fun configure(config: Config)
}

// Bad: Kitchen sink interface
interface Component : Observable, Configurable, Lifecycle, Serializable
```

### Immutability by Default
- Expose read-only views (List not MutableList)
- Make data classes immutable (val not var)
- Use copy methods for modifications

### Extension Points
Provide hooks for customization without requiring inheritance:
- Function parameters for behavior injection
- Strategy interfaces
- Builder callbacks

## Lifecycle Management

### Resource Handling
- **Explicit lifecycle**: Closeable/AutoCloseable for resources
- **Idempotent cleanup**: Safe to call close() multiple times
- **Clear ownership**: Document who owns and when to release

### Initialization Patterns
- **Fail-fast construction**: Validate in constructor/factory
- **Two-phase initialization**: Avoid if possible; use factory functions instead
- **Lazy initialization**: Document thread-safety guarantees

## Evolution and Versioning

### Binary Compatibility
Adding is safe; changing/removing breaks compatibility:
- **Safe additions**: New overloads, default parameters, extension functions
- **Breaking changes**: Removing members, changing signatures, reordering parameters
- **Deprecation path**: Mark deprecated, provide alternative, remove in next major version

### API Stability Markers
```kotlin
@RequiresOptIn("Experimental API")
annotation class ExperimentalApi

@ExperimentalApi
fun experimentalFeature() { }
```

### Semantic Versioning
- MAJOR: Incompatible API changes
- MINOR: Backwards-compatible functionality
- PATCH: Backwards-compatible bug fixes

## Documentation Requirements

### KDoc Essentials
Every public API element needs:
- **Purpose**: What it does
- **Parameters**: Meaning and constraints
- **Return value**: What it represents
- **Exceptions**: What can be thrown and why
- **Thread-safety**: Synchronization requirements
- **Examples**: Code samples for non-trivial usage

### Contract Documentation
- **Preconditions**: What must be true before calling
- **Postconditions**: What will be true after calling
- **Invariants**: What's always true
- **Performance**: Time/space complexity for operations

## Testability

### Test-Friendly Design
- **Avoid final classes**: Use interfaces or open classes for mocking
- **Inject dependencies**: No hidden dependencies (static methods, singletons)
- **Deterministic behavior**: Provide test doubles for time, random, I/O

### Observable Behavior
APIs should make it easy to verify correctness:
- Expose enough state to observe outcomes
- Provide hooks for test instrumentation
- Allow dependency injection for external systems

## Common API Patterns

### Builder Pattern
For complex object construction with many optional parameters:
```kotlin
class Config private constructor(
    val timeout: Duration,
    val retries: Int,
    val endpoint: String
) {
    class Builder {
        private var timeout: Duration = 30.seconds
        private var retries: Int = 3
        private lateinit var endpoint: String
        
        fun timeout(duration: Duration) = apply { timeout = duration }
        fun retries(count: Int) = apply { retries = count }
        fun endpoint(url: String) = apply { endpoint = url }
        
        fun build(): Config {
            require(::endpoint.isInitialized) { "endpoint required" }
            return Config(timeout, retries, endpoint)
        }
    }
}
```

### Factory Pattern
For controlled object creation:
```kotlin
interface Connection
object ConnectionFactory {
    fun create(config: Config): Connection
}
```

### Strategy Pattern
For pluggable behavior:
```kotlin
interface RetryStrategy {
    fun shouldRetry(attempt: Int, error: Throwable): Boolean
}

class ExponentialBackoff : RetryStrategy {
    override fun shouldRetry(attempt: Int, error: Throwable) = 
        attempt < 5 && error is RetryableException
}
```

## Anti-Patterns to Avoid

### Boolean Trap
```kotlin
// Bad: What does true mean?
fun process(data: Data, flag: Boolean)

// Good: Self-documenting
enum class ProcessMode { SYNC, ASYNC }
fun process(data: Data, mode: ProcessMode)
```

### Temporal Coupling
```kotlin
// Bad: Must call in order
client.connect()
client.authenticate()
client.sendRequest()

// Good: Enforce order via types
interface Connected
interface Authenticated : Connected
fun connect(): Connected
fun Connected.authenticate(): Authenticated
fun Authenticated.sendRequest()
```

### Over-Engineering
Don't add flexibility "just in case":
- Start simple, add complexity when needed
- YAGNI (You Aren't Gonna Need It)
- Measure before optimizing

### Under-Specification
Don't leave behavior undefined:
- Document thread-safety explicitly
- Specify null handling
- Define error conditions
- Clarify ownership semantics
