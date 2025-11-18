# Engineering Deep Dive: Concurrency Model

**Navigate**: [← Previous: Bucketing Algorithm](06-bucketing-algorithm.md) | [Next: Serialization →](08-serialization.md)

---

## Lock-Free Thread Safety

Concurrent access is inevitable in real applications:
- Multiple threads evaluating flags
- Background thread updating configuration
- UI thread rendering based on flag values

Traditional approaches use locks. Konditional uses immutability and atomic references for lock-free thread safety.

## The Concurrency Challenge

### Typical Scenarios

**Scenario 1: Concurrent Reads**
```kotlin
// Thread 1
val value1 = context.evaluate(FEATURE)

// Thread 2 (simultaneously)
val value2 = context.evaluate(FEATURE)
```

**Requirement**: Both reads must be safe and consistent.

**Scenario 2: Read During Write**
```kotlin
// Thread 1: Reading
val value = context.evaluate(FEATURE)

// Thread 2: Updating configuration
namespace.load(newConfiguration)
```

**Requirement**: Reader sees either old or new config (never partial/corrupted state).

**Scenario 3: Concurrent Writes**
```kotlin
// Thread 1: Updating config
namespace.load(configuration1)

// Thread 2: Updating config
namespace.load(configuration2)
```

**Requirement**: One update wins, other may retry or be lost (no corruption).

### Traditional Approach: Locks

```kotlin
class NamespaceRegistry {
    private val lock = ReentrantReadWriteLock()
    private var config: Configuration = ...

    fun evaluate(...): T {
        lock.readLock().lock()
        try {
            return config.evaluate(...)
        } finally {
            lock.readLock().unlock()
        }
    }

    fun load(newConfig: Configuration) {
        lock.writeLock().lock()
        try {
            config = newConfig
        } finally {
            lock.writeLock().unlock()
        }
    }
}
```

**Problems**:
1. **Overhead**: Lock acquisition/release on every read
2. **Contention**: Multiple readers block each other unnecessarily
3. **Deadlock risk**: Lock ordering issues
4. **Complexity**: try-finally boilerplate everywhere

### Konditional's Approach: Immutability + AtomicReference

**Key insight**: If data is immutable, concurrent reads are safe without locks.

---

## Immutable Data Structures

### FlagDefinition: Immutable by Design

```kotlin
data class FlagDefinition<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>(
    val defaultValue: T,
    val feature: Feature<S, T, C, M>,
    val values: List<ConditionalValue<S, T, C, M>>,
    val isActive: Boolean = true,
    val salt: String = "v1"
)
```

**All properties are `val`** (immutable).

**Effect**: Once created, cannot be modified.

**Benefit**: Safe to read from multiple threads without synchronization.

### Configuration (Konfig): Immutable Map

```kotlin
@ConsistentCopyVisibility
data class Configuration internal constructor(
    internal val flags: Map<Feature<*, *, *, *>, FlagDefinition<*, *, *, *>>
)
```

**Map is immutable**: Cannot add/remove entries after creation.

**Effect**: Reference to Configuration is a snapshot in time.

**Benefit**: Thread-safe reads without locks.

### ConditionalValue: Immutable Rule-Value Pair

```kotlin
data class ConditionalValue<S : EncodableValue<T>, T : Any, C : Context, M : Namespace>(
    val rule: Rule<C>,
    val value: T
)
```

**All properties `val`**.

**Effect**: Rule and value binding is immutable.

### Rule: Immutable Conditions

```kotlin
data class Rule<C : Context>(
    val rollout: Rollout = Rollout.default,
    val note: String? = null,
    internal val baseEvaluable: BaseEvaluable<C> = BaseEvaluable(),
    val extension: Evaluable<C> = Placeholder,
)
```

**All properties `val`**.

**Effect**: Rule conditions cannot change after creation.

---

## AtomicReference: The Update Mechanism

### InMemoryNamespaceRegistry Implementation

Here's the complete implementation from `InMemoryNamespaceRegistry.kt`:

```kotlin
internal class InMemoryNamespaceRegistry : NamespaceRegistry {
    private val current = AtomicReference(Configuration(emptyMap()))

    override fun load(config: Configuration) {
        current.set(config)
    }

    override val configuration: Configuration
        get() = current.get()

    internal fun updateDefinition(definition: FlagDefinition<*, *, *, *>) {
        current.updateAndGet { currentSnapshot ->
            val mutableFlags = currentSnapshot.flags.toMutableMap()
            mutableFlags[definition.feature] = definition
            Configuration(mutableFlags)
        }
    }
}
```

Let's break down each operation.

### AtomicReference Basics

```kotlin
private val current = AtomicReference(Configuration(emptyMap()))
```

**What**: `AtomicReference<T>` is a thread-safe reference to a value of type `T`.

**Operations**:
- `get()`: Read current value (atomic)
- `set(newValue)`: Write new value (atomic)
- `updateAndGet(transform)`: Read, transform, write (atomic compare-and-swap)

**Key property**: All operations are atomic (indivisible).

---

## Lock-Free Reads

### The get() Operation

```kotlin
override val configuration: Configuration
    get() = current.get()
```

**What happens**:
1. Read reference from `AtomicReference`
2. Return the `Configuration` object

**Thread safety**: `get()` is atomic and lock-free.

**Performance**: O(1), no lock acquisition, no blocking.

### Evaluation Flow

```kotlin
// Application code
val value = context.evaluate(FEATURE)

// Under the hood:
// 1. Lookup feature in namespace registry
val config = namespace.registry.configuration  // AtomicReference.get()
val definition = config.flags[FEATURE]

// 2. Evaluate definition
val result = definition.evaluate(context)
```

**Concurrent reads**: All readers get consistent snapshot without blocking each other.

**Why safe**: `Configuration` is immutable, so:
- Multiple threads can read same object
- No risk of seeing partial modifications
- No memory visibility issues (AtomicReference provides happens-before guarantee)

---

## Atomic Configuration Updates

### The set() Operation

```kotlin
override fun load(config: Configuration) {
    current.set(config)
}
```

**What happens**:
1. Atomically replace old `Configuration` with new one

**Thread safety**: `set()` is atomic. Readers see either old or new config, never partial state.

**Effect on concurrent readers**:
- Readers that started before `set()` see old config
- Readers that start after `set()` see new config
- No reader sees corrupted/partial state

### Example Timeline

```
Thread 1 (Reader)          Thread 2 (Writer)          Thread 3 (Reader)
     |                          |                          |
     |-- get() → config A       |                          |
     |                          |-- set(config B)          |
     |-- evaluate()             |                          |
     |                          |                          |-- get() → config B
     |                          |                          |-- evaluate()
     |-- result from A          |                          |-- result from B
```

**Thread 1**: Gets config A, evaluates with A (consistent)
**Thread 3**: Gets config B, evaluates with B (consistent)
**No thread**: Sees partial state between A and B

---

## Compare-And-Swap Updates

### The updateAndGet() Operation

```kotlin
internal fun updateDefinition(definition: FlagDefinition<*, *, *, *>) {
    current.updateAndGet { currentSnapshot ->
        val mutableFlags = currentSnapshot.flags.toMutableMap()
        mutableFlags[definition.feature] = definition
        Configuration(mutableFlags)
    }
}
```

**What happens**:
1. Read current `Configuration`
2. Transform it (create new with updated flag)
3. Atomically swap if current hasn't changed
4. If changed, retry from step 1

**This is compare-and-swap (CAS)**: Update succeeds only if value hasn't changed since read.

### CAS Deep Dive

**Pseudocode**:
```kotlin
fun updateAndGet(transform: (T) -> T): T {
    while (true) {
        val current = get()
        val updated = transform(current)
        if (compareAndSet(current, updated)) {
            return updated
        }
        // If CAS failed, retry
    }
}
```

**compareAndSet(expected, new)**:
- If current value == expected, set to new and return true
- Otherwise, return false (value was changed by another thread)

### Example: Concurrent Updates

```
Thread 1                           Thread 2
    |                                  |
    |-- updateAndGet()                 |
    |   read config A                  |
    |   transform A → B                |
    |                                  |-- updateAndGet()
    |                                  |   read config A
    |                                  |   transform A → C
    |                                  |   CAS(A, C) → success
    |   CAS(A, B) → fail (current is C, not A)
    |   retry: read config C
    |   transform C → D
    |   CAS(C, D) → success
```

**Result**: Both updates applied (C then D), no lost update.

**Key insight**: CAS ensures updates are not lost, without locks.

---

## Why No Locks Are Needed

### Immutability Eliminates Most Lock Needs

**Problem locks solve**: Preventing concurrent modification of shared mutable state.

**Konditional's solution**: No shared mutable state.

```kotlin
// Traditional approach: Mutable state + locks
class MutableConfig {
    private val flags = mutableMapOf<String, Boolean>()
    private val lock = ReentrantReadWriteLock()

    fun get(key: String): Boolean {
        lock.readLock().lock()
        try {
            return flags[key] ?: false
        } finally {
            lock.readLock().unlock()
        }
    }

    fun set(key: String, value: Boolean) {
        lock.writeLock().lock()
        try {
            flags[key] = value
        } finally {
            lock.writeLock().unlock()
        }
    }
}
```

**Problem**: Every read and write needs lock.

```kotlin
// Konditional approach: Immutable state + atomic reference
class ImmutableConfig {
    private val current = AtomicReference(mapOf<String, Boolean>())

    fun get(key: String): Boolean =
        current.get()[key] ?: false  // No lock needed

    fun set(newMap: Map<String, Boolean>) {
        current.set(newMap)  // Atomic, no lock needed
    }
}
```

**Benefit**: Reads are lock-free and never block.

### AtomicReference Provides Necessary Guarantees

**Memory visibility**: Changes by one thread visible to others (happens-before relationship).

**Atomicity**: Reference updates are atomic (no partial states).

**Consistency**: Readers always see valid (immutable) Configuration objects.

---

## Thread Safety Guarantees

### Guarantee 1: Consistent Reads

**Property**: Every read sees a valid, complete Configuration.

**Mechanism**:
- Configuration is immutable
- AtomicReference ensures atomic reference updates

**Example**:
```kotlin
// This flag evaluation always sees consistent state
val result = context.evaluate(FEATURE)

// Never sees:
// - Partially updated Configuration
// - Corrupted FlagDefinition
// - Inconsistent rule list
```

### Guarantee 2: Atomic Updates

**Property**: Configuration updates are atomic (all-or-nothing).

**Mechanism**: `AtomicReference.set()` is atomic

**Example**:
```kotlin
namespace.load(newConfiguration)

// Either:
// - Update succeeds, all subsequent reads see new config
// - Update fails (rare), configuration unchanged
// Never:
// - Partial update where some flags are new, some are old
```

### Guarantee 3: Lock-Free Reads

**Property**: Readers never block each other or writers.

**Mechanism**:
- Immutable data can be read safely without synchronization
- AtomicReference.get() is lock-free

**Benefit**: High-performance concurrent reads.

### Guarantee 4: Progress Guarantee

**Property**: Updates eventually succeed even under contention.

**Mechanism**: CAS retry loop in `updateAndGet()`

**Note**: High contention may cause retries, but updates won't deadlock.

---

## Performance Characteristics

### Read Performance

**Operation**: `context.evaluate(FEATURE)`

**Cost**:
- `AtomicReference.get()`: ~1-2 nanoseconds
- Flag lookup: ~10-20 nanoseconds (map lookup)
- Evaluation: ~100-1000 nanoseconds (rule matching, bucketing)

**Total**: ~100-1000 nanoseconds (dominated by evaluation logic)

**Scalability**: Linear with number of threads (no lock contention).

### Write Performance

**Operation**: `namespace.load(newConfiguration)`

**Cost**:
- `AtomicReference.set()`: ~1-2 nanoseconds

**Total**: ~1-2 nanoseconds

**Note**: Creating new Configuration may take longer (allocations), but that's outside the atomic operation.

### CAS Performance

**Operation**: `updateDefinition(definition)`

**Cost**:
- No contention: ~10-20 nanoseconds (one CAS)
- With contention: ~50-100 nanoseconds per retry

**Retries**: Rare in practice (updates are infrequent relative to reads).

---

## Comparison: Locks vs. Lock-Free

### Locks

```kotlin
class LockedRegistry {
    private val lock = ReentrantReadWriteLock()
    private var config: Configuration = ...

    fun evaluate(...): T {
        lock.readLock().lock()
        try {
            return config.evaluate(...)
        } finally {
            lock.readLock().unlock()
        }
    }
}
```

**Read cost**: ~25-50 nanoseconds (lock acquisition/release)
**Contention**: Readers may block on lock
**Scalability**: Degrades under high read concurrency

### AtomicReference (Konditional)

```kotlin
class LockFreeRegistry {
    private val current = AtomicReference(Configuration(...))

    fun evaluate(...): T =
        current.get().evaluate(...)
}
```

**Read cost**: ~1-2 nanoseconds (atomic load)
**Contention**: None (lock-free)
**Scalability**: Linear (no blocking)

**Speedup**: ~10-25x faster reads, infinite scalability improvement.

---

## When Immutability Isn't Enough

### Example: Mutable Collection

```kotlin
// ✗ Wrong: Mutable collection in AtomicReference
private val current = AtomicReference(mutableMapOf<String, Boolean>())

fun update(key: String, value: Boolean) {
    current.get()[key] = value  // Modifies shared mutable map!
}
```

**Problem**: AtomicReference protects the reference, not the object.

**Effect**: Multiple threads modifying the map concurrently → data corruption.

### Solution: Immutable Collection

```kotlin
// ✓ Correct: Immutable collection
private val current = AtomicReference(mapOf<String, Boolean>())

fun update(key: String, value: Boolean) {
    current.updateAndGet { map ->
        map + (key to value)  // Creates new immutable map
    }
}
```

**Benefit**: Each update creates new immutable map, old map untouched.

---

## Real-World Example: Configuration Hot-Reload

```kotlin
// Application startup
val registry = InMemoryNamespaceRegistry()

// Background thread: Poll for configuration updates
launch(Dispatchers.IO) {
    while (true) {
        delay(60_000)  // Check every minute

        val newConfig = fetchConfigurationFromServer()
        when (val result = SnapshotSerializer.fromJson(newConfig)) {
            is ParseResult.Success -> {
                registry.load(result.value)  // Atomic update
                logger.info("Configuration updated")
            }
            is ParseResult.Failure -> {
                logger.error("Failed to parse config: ${result.error}")
            }
        }
    }
}

// UI thread: Evaluate flags
fun renderUI(context: Context) {
    val showNewUI = context.evaluate(UI_FEATURES.NEW_DESIGN)  // Lock-free read
    if (showNewUI) {
        renderNewDesign()
    } else {
        renderOldDesign()
    }
}
```

**Concurrency**:
- Background thread periodically updates configuration
- UI thread continuously evaluates flags
- No locks, no blocking, no race conditions

**Guarantees**:
- UI thread sees either old or new config (never partial)
- Background thread update is atomic
- Both threads run at full speed

---

## Edge Cases

### Edge Case 1: Lost Update (With CAS)

```kotlin
// Thread 1
registry.updateDefinition(featureA_v1)

// Thread 2 (concurrent)
registry.updateDefinition(featureB_v1)
```

**With CAS**: Both updates succeed (one retries).

**Without CAS**: One update might be lost.

### Edge Case 2: Read During Update

```kotlin
// Thread 1: Reading
val config = registry.configuration
val value = config.flags[FEATURE]?.evaluate(context)

// Thread 2: Updating (concurrent with above)
registry.load(newConfiguration)
```

**Outcome**: Thread 1 gets either old or new config (but not both).

**Correct**: Yes, both old and new configs are valid.

### Edge Case 3: Multiple Rapid Updates

```kotlin
// Thread 1
repeat(1000) {
    registry.load(createConfig(it))
}
```

**Outcome**: All updates succeed, last one wins.

**Performance**: ~1-2 microseconds total (1000 × 1-2 nanoseconds).

**Note**: No lock contention, updates are very fast.

---

## Best Practices

### 1. Keep Updates Coarse-Grained

```kotlin
// ✓ Good: Replace entire configuration
registry.load(newConfiguration)

// ✗ Avoid: Many small updates
repeat(100) {
    registry.updateDefinition(flagDefinitions[it])
}
```

**Why**: Each update creates new Configuration object. Batching is more efficient.

### 2. Use Immutable Collections

```kotlin
// ✓ Good: Immutable map
val flags = mapOf(feature1 to definition1, feature2 to definition2)

// ✗ Bad: Mutable map
val flags = mutableMapOf(feature1 to definition1)
flags[feature2] = definition2  // Mutation
```

**Why**: Mutable collections break immutability guarantee.

### 3. Don't Hold References Across Updates

```kotlin
// ✗ Bad: Stale reference
val config = registry.configuration
delay(1000)
val value = config.flags[FEATURE]  // May be stale

// ✓ Good: Fresh reference each time
delay(1000)
val value = registry.configuration.flags[FEATURE]
```

**Why**: Old reference sees old configuration, may not be desired.

### 4. Prefer load() Over Multiple updateDefinition()

```kotlin
// ✓ Good: Single atomic update
val newFlags = buildMap {
    put(feature1, definition1)
    put(feature2, definition2)
    put(feature3, definition3)
}
registry.load(Configuration(newFlags))

// ✗ Less efficient: Multiple updates
registry.updateDefinition(definition1)
registry.updateDefinition(definition2)
registry.updateDefinition(definition3)
```

**Why**: Single update is faster and more atomic.

---

## Review: Concurrency Model

### Foundation: Immutability

All core data structures are immutable:
- `FlagDefinition`
- `Configuration`
- `ConditionalValue`
- `Rule`

**Benefit**: Safe to read concurrently without synchronization.

### Update Mechanism: AtomicReference

```kotlin
private val current = AtomicReference(Configuration(...))
```

**Operations**:
- `get()`: Lock-free atomic read
- `set()`: Lock-free atomic write
- `updateAndGet()`: Lock-free compare-and-swap

**Benefit**: Thread-safe updates without locks.

### Thread Safety Guarantees

1. **Consistent reads**: Always see valid configuration
2. **Atomic updates**: All-or-nothing
3. **Lock-free**: No blocking, high concurrency
4. **Progress**: Updates always eventually succeed

### Performance

- Reads: ~1-2 nanoseconds (atomic load)
- Writes: ~1-2 nanoseconds (atomic store)
- ~10-25x faster than lock-based approaches

---

## Next Steps

Now that you understand how concurrency is handled, we can explore how configurations are serialized and deserialized.

**Next chapter**: [Serialization](08-serialization.md)
- SnapshotSerializer implementation
- ParseResult (parse-don't-validate pattern)
- Moshi adapters for version ranges and flag values
- JSON format structure
- Type-safe deserialization

Serialization is how configurations move between systems. Let's see how type safety is maintained across the boundary.

---

**Navigate**: [← Previous: Bucketing Algorithm](06-bucketing-algorithm.md) | [Next: Serialization →](08-serialization.md)
