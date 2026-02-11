# Type Safety Patterns

## Compile-Time Guarantees

### Phantom Types
Encode state transitions in the type system to prevent invalid operations at compile time.

```kotlin
sealed interface State
sealed interface Disconnected : State
sealed interface Connected : State
sealed interface Authenticated : State

class Client<S : State> private constructor() {
    companion object {
        fun create(): Client<Disconnected> = Client()
    }
    
    fun connect(): Client<Connected> = Client()
}

fun Client<Connected>.authenticate(): Client<Authenticated> = Client()
fun Client<Authenticated>.sendRequest(req: Request): Response = TODO()

// Compile error: cannot send request on disconnected client
// val client = Client.create()
// client.sendRequest(request) // Won't compile!

// Must follow the state progression
val client = Client.create()
    .connect()
    .authenticate()
    .sendRequest(request) // OK!
```

### Inline Value Classes
Zero-cost type safety for primitive values:

```kotlin
@JvmInline
value class UserId(val value: String)

@JvmInline
value class OrderId(val value: String)

// Compile error: cannot pass UserId where OrderId expected
fun getOrder(id: OrderId): Order

val userId = UserId("user-123")
getOrder(userId) // Won't compile!
```

### Type-Level Computation
Use the type system to enforce invariants:

```kotlin
// Non-empty list guaranteed at compile time
sealed interface NonEmptyList<out T> {
    val head: T
    val tail: List<T>
    
    data class Single<T>(override val head: T) : NonEmptyList<T> {
        override val tail: List<T> = emptyList()
    }
    
    data class Cons<T>(
        override val head: T,
        override val tail: NonEmptyList<T>
    ) : NonEmptyList<T>
}

// Functions that require non-empty lists can guarantee it
fun <T> max(list: NonEmptyList<T>): T where T : Comparable<T> {
    // No need to check if list is empty!
    return list.tail.fold(list.head) { acc, item ->
        if (item > acc) item else acc
    }
}
```

## Error Handling Without Wrappers

### Sealed Class Outcomes
Represent success/failure as part of the domain, not as a wrapper type:

```kotlin
// Good: Domain-specific outcomes
sealed interface EvaluationOutcome<out T> {
    data class Enabled<T>(val value: T) : EvaluationOutcome<T>
    data object Disabled : EvaluationOutcome<Nothing>
    // No error case exposed - handled internally
}

// Bad: Generic wrapper exposes errors
sealed interface Result<out T, out E> {
    data class Success<T>(val value: T) : Result<T, Nothing>
    data class Failure<E>(val error: E) : Result<Nothing, E>
}
```

### Internal Error Containment
Errors should be handled internally and not leak to consumers:

```kotlin
class FeatureFlags internal constructor(
    private val config: Config,
    private val evaluator: Evaluator
) {
    // All errors caught and converted to Disabled
    fun evaluate(feature: String): EvaluationOutcome<Config> {
        return try {
            when (val result = evaluator.evaluate(feature, config)) {
                is InternalSuccess -> Enabled(result.config)
                is InternalFailure -> {
                    logger.error("Evaluation failed", result.error)
                    Disabled
                }
            }
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            Disabled
        }
    }
}

// Internal error types never exposed
private sealed interface InternalResult
private data class InternalSuccess(val config: Config) : InternalResult
private data class InternalFailure(val error: Throwable) : InternalResult
```

### Exhaustive When Expressions
Use sealed classes to ensure all cases are handled:

```kotlin
sealed interface Message {
    data class Text(val content: String) : Message
    data class Image(val url: String) : Message
    data class Video(val url: String) : Message
}

fun handle(message: Message): Unit = when (message) {
    is Message.Text -> handleText(message.content)
    is Message.Image -> handleImage(message.url)
    is Message.Video -> handleVideo(message.url)
    // Compiler enforces exhaustiveness - no else needed
}
```

## Nullability Elimination

### Non-Null by Construction
Design APIs that make null impossible:

```kotlin
// Bad: Nullable return requires null checks
fun findUser(id: String): User?

// Good: Explicit outcome type
sealed interface UserLookup {
    data class Found(val user: User) : UserLookup
    data object NotFound : UserLookup
}

fun findUser(id: String): UserLookup
```

### Late Initialization Alternatives
Avoid lateinit with safer patterns:

```kotlin
// Bad: lateinit can throw
class Service {
    private lateinit var dependency: Dependency
    fun initialize(dep: Dependency) { dependency = dep }
}

// Good: Require in constructor
class Service(private val dependency: Dependency)

// Good: Use lazy for computed values
class Service {
    private val dependency: Dependency by lazy { createDependency() }
}

// Good: Explicit initialization state
class Service {
    private var dependency: Dependency? = null
    val isInitialized get() = dependency != null
    
    fun initialize(dep: Dependency) {
        check(dependency == null) { "Already initialized" }
        dependency = dep
    }
}
```

### Optional vs Null
Use explicit Option/Maybe type when nullability has semantic meaning:

```kotlin
sealed interface Option<out T> {
    data class Some<T>(val value: T) : Option<T>
    data object None : Option<Nothing>
}

// Makes "no value" explicit in the API
fun getConfig(key: String): Option<String>
```

## Immutability Patterns

### Data Class Immutability
All fields should be val, not var:

```kotlin
// Good: Immutable data class
data class User(
    val id: UserId,
    val name: String,
    val email: String
)

// Use copy for modifications
val updated = user.copy(name = "New Name")

// Bad: Mutable data class
data class User(
    var id: UserId,
    var name: String,
    var email: String
)
```

### Immutable Collections
Expose read-only collection interfaces:

```kotlin
// Good: Return immutable interface
class Repository {
    private val items = mutableListOf<Item>()
    
    fun getItems(): List<Item> = items.toList() // Defensive copy
    // Or: return items.asUnmodifiable() if copying is expensive
}

// Bad: Expose mutable collection
class Repository {
    val items = mutableListOf<Item>() // Direct access!
}
```

### Builder Pattern for Complex Objects
Use builders for objects with many optional fields:

```kotlin
data class Config private constructor(
    val host: String,
    val port: Int,
    val timeout: Duration,
    val retries: Int,
    val useSsl: Boolean
) {
    class Builder {
        private var host: String? = null
        private var port: Int = 8080
        private var timeout: Duration = 30.seconds
        private var retries: Int = 3
        private var useSsl: Boolean = true
        
        fun host(value: String) = apply { host = value }
        fun port(value: Int) = apply { port = value }
        fun timeout(value: Duration) = apply { timeout = value }
        fun retries(value: Int) = apply { retries = value }
        fun useSsl(value: Boolean) = apply { useSsl = value }
        
        fun build(): Config {
            val host = checkNotNull(host) { "host required" }
            require(port > 0) { "port must be positive" }
            return Config(host, port, timeout, retries, useSsl)
        }
    }
    
    companion object {
        fun builder() = Builder()
    }
}
```

## Type-Safe DSLs

### Scope Control with Receivers
Use scoped functions to create type-safe configuration DSLs:

```kotlin
class ConfigBuilder {
    var timeout: Duration = 30.seconds
    var retries: Int = 3
    
    fun build() = Config(timeout, retries)
}

fun config(block: ConfigBuilder.() -> Unit): Config {
    return ConfigBuilder().apply(block).build()
}

// Usage
val cfg = config {
    timeout = 60.seconds
    retries = 5
}
```

### Marker Interfaces for DSL Scoping
Prevent invalid nesting:

```kotlin
@DslMarker
annotation class HtmlDsl

@HtmlDsl
interface Element {
    fun render(): String
}

@HtmlDsl
class Html : Element {
    private val children = mutableListOf<Element>()
    
    fun body(block: Body.() -> Unit) {
        children.add(Body().apply(block))
    }
    
    override fun render() = children.joinToString { it.render() }
}

@HtmlDsl
class Body : Element {
    private val children = mutableListOf<Element>()
    
    fun div(block: Div.() -> Unit) {
        children.add(Div().apply(block))
    }
    
    override fun render() = "<body>${children.joinToString { it.render() }}</body>"
}

// DSL marker prevents invalid nesting
html {
    body {
        div {
            // body { } // Compile error! Can't nest body in div
        }
    }
}
```

## Variance and Type Bounds

### Covariance for Producers
Use `out` for types that only produce values:

```kotlin
interface Producer<out T> {
    fun produce(): T
    // Cannot have methods that consume T
}

val stringProducer: Producer<String> = TODO()
val anyProducer: Producer<Any> = stringProducer // OK, covariant
```

### Contravariance for Consumers
Use `in` for types that only consume values:

```kotlin
interface Consumer<in T> {
    fun consume(value: T)
    // Cannot have methods that produce T
}

val anyConsumer: Consumer<Any> = TODO()
val stringConsumer: Consumer<String> = anyConsumer // OK, contravariant
```

### Type Bounds for Constraints
Constrain generic types to ensure required capabilities:

```kotlin
// Upper bound
fun <T : Comparable<T>> max(a: T, b: T): T = if (a > b) a else b

// Multiple bounds
interface Persistable {
    fun save()
}

fun <T> persist(item: T) where T : Serializable, T : Persistable {
    // T is both Serializable and Persistable
}
```

## Reified Generics

### Type-Safe Casting
Use reified generics to access type information at runtime:

```kotlin
inline fun <reified T> JsonNode.parse(): T {
    return when (T::class) {
        String::class -> this.asText() as T
        Int::class -> this.asInt() as T
        else -> objectMapper.treeToValue(this, T::class.java)
    }
}

// Usage
val value: String = jsonNode.parse() // Type-safe!
```

### Instance Checks
```kotlin
inline fun <reified T> Any.isInstanceOf(): Boolean = this is T

val obj: Any = "Hello"
obj.isInstanceOf<String>() // true
obj.isInstanceOf<Int>() // false
```

## Context Receivers (Experimental)

### Type-Safe Context Propagation
```kotlin
interface LoggingContext {
    fun log(message: String)
}

interface DatabaseContext {
    fun query(sql: String): ResultSet
}

// Function requires both contexts
context(LoggingContext, DatabaseContext)
fun fetchUser(id: String): User {
    log("Fetching user $id")
    val result = query("SELECT * FROM users WHERE id = ?")
    return parseUser(result)
}

// Must be called with both contexts
with(loggingCtx) {
    with(dbCtx) {
        fetchUser("123")
    }
}
```

## Type Aliases for Clarity

### Semantic Type Names
```kotlin
typealias UserId = String
typealias Timestamp = Long
typealias Json = String

// Makes intent clear
fun getUser(id: UserId): User
fun parseEvent(json: Json, timestamp: Timestamp): Event

// Better than
fun getUser(id: String): User
fun parseEvent(json: String, timestamp: Long): Event
```

### Complex Type Simplification
```kotlin
typealias UserCache = Map<UserId, User>
typealias EventHandler = (Event) -> Unit
typealias ValidationRule<T> = (T) -> Boolean

// Simpler signatures
fun cacheUsers(cache: UserCache, users: List<User>)
fun registerHandler(handler: EventHandler)
fun validate(value: String, rule: ValidationRule<String>): Boolean
```

## Anti-Patterns to Avoid

### Unchecked Casts
```kotlin
// Bad: Unsafe cast
val user = obj as User

// Good: Safe cast with handling
val user = obj as? User ?: return null
```

### Any Abuse
```kotlin
// Bad: Loss of type information
fun process(data: Any): Any

// Good: Generic with constraints
fun <T : Processable> process(data: T): T
```

### Reflection for Type Checking
```kotlin
// Bad: Runtime type checking
if (obj::class == User::class) { }

// Good: Use sealed classes or is checks
when (obj) {
    is User -> handleUser(obj)
    is Admin -> handleAdmin(obj)
}
```

### Nullable Types When Not Needed
```kotlin
// Bad: Null for "not found" semantics
fun find(id: String): User?

// Good: Explicit result type
sealed interface FindResult {
    data class Found(val user: User) : FindResult
    data object NotFound : FindResult
}
fun find(id: String): FindResult
```

## Testing Type Safety

### Compile-Time Test
```kotlin
// Test that certain operations don't compile
// Place in separate source set that's expected to fail compilation

fun testCannotSendOnDisconnected() {
    val client = Client.create()
    // client.sendRequest(request) // Should not compile
}

fun testCannotPassWrongId() {
    val userId = UserId("123")
    // getOrder(userId) // Should not compile
}
```

### Property-Based Testing
```kotlin
// Test type invariants hold for all inputs
@Test
fun `NonEmptyList always has at least one element`() = runTest {
    checkAll(Arb.nonEmptyList(Arb.int())) { list ->
        assertTrue(list.size >= 1)
    }
}
```
