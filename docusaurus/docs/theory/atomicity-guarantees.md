# Atomicity Guarantees

Why readers never see partial configuration updates, and how `AtomicReference` provides lock-free safety.

---

## The Problem: Torn Reads

Without atomicity, readers can observe partial updates:

```kotlin
// ✗ Non-atomic update (simplified)
class Registry {
    var config: Configuration = initial  // Not thread-safe

    fun update(newConfig: Configuration) {
        config = newConfig  // Multiple threads see inconsistent state
    }

    fun read(): Configuration {
        return config  // Might see partially-written config
    }
}
```

**Issues:**
- Thread A updates `config` while Thread B reads
- Thread B might see old config, new config, or **garbage** (torn read)
- No happens-before relationship between write and read

---

## Konditional's Solution: AtomicReference

```kotlin
// Simplified: the default in-memory NamespaceRegistry implementation.
private val current: AtomicReference<Configuration> = AtomicReference(initialConfiguration)

override fun load(config: Configuration) {
    current.set(config)  // Single atomic write
}

override val configuration: Configuration
    get() = current.get()  // Atomic read
```

**Guarantees:**
1. **Atomic swap** — `set(...)` is a single write operation (no partial updates)
2. **Happens-before** — JVM memory model guarantees writes are visible to subsequent reads
3. **No torn reads** — Reference swap is atomic at the hardware level

---

## How AtomicReference Works

### JVM Memory Model Guarantees

From the Java Language Specification (JLS §17.4.5):

> "All actions in a thread happen-before any action in that thread that comes later in the program order."

> "A write to a volatile variable v happens-before all subsequent reads of v by any thread."

`AtomicReference` uses volatile semantics internally, providing:
- **Visibility** — Writes are immediately visible to other threads
- **Ordering** — No reordering of reads/writes across the volatile barrier

### Single Write Operation

```kotlin
current.set(newConfig)
```

This is **one atomic operation**:
- Old reference is replaced with new reference
- No intermediate state exists
- Readers see either old OR new (never partial)

---

## Proof: Readers See Consistent Snapshots

### Scenario: Concurrent Update and Evaluation

```kotlin
// Thread 1: Update configuration
AppFeatures.load(newConfig)

// Thread 2: Concurrent evaluation
val value = AppFeatures.darkMode(context)
```

**What happens:**

1. Thread 1 calls `current.set(newConfig)`
2. Thread 2 calls `current.get()` during the update
3. Thread 2 sees **either**:
   - Old config (read happened before write completed)
   - New config (read happened after write completed)
4. Thread 2 **never** sees:
   - Partial config (half old, half new)
   - Null reference
   - Corrupt data

**Why:** `AtomicReference.set(...)` is a single atomic write; there's no intermediate state.

---

## Lock-Free Reads

Evaluation reads the current snapshot without acquiring locks:

```kotlin
operator fun <T : Any, C : Context, M : Namespace> Feature<T, C, M>.invoke(
    context: C,
    registry: NamespaceRegistry,
): T {
    val config = registry.configuration  // Lock-free atomic read
    // ... evaluate using config ...
}
```

**Benefits:**
- **No contention** — Multiple threads can read concurrently
- **No blocking** — Writers don't block readers, readers don't block writers
- **Predictable latency** — No lock acquisition overhead

### Comparison: Lock-Based Approach

```kotlin
// ✗ Lock-based (slower, more complex)
class Registry {
    private val lock = ReentrantReadWriteLock()
    private var config: Configuration = initial

    fun update(newConfig: Configuration) {
        lock.writeLock().lock()
        try {
            config = newConfig
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun read(): Configuration {
        lock.readLock().lock()
        try {
            return config
        } finally {
            lock.readLock().unlock()
        }
    }
}
```

**Issues:**
- Lock contention (readers block writers, writers block readers)
- Overhead of lock acquisition/release
- Potential for deadlocks

---

## Linearizability

`AtomicReference` provides **linearizability**: operations appear to execute atomically at a single point in time.

### Concurrent Updates

```kotlin
// Thread 1
AppFeatures.load(config1)

// Thread 2
AppFeatures.load(config2)

// Thread 3
val value = AppFeatures.darkMode(context)
```

**Outcome:**
- Thread 3 sees **one** of: initial config, config1, or config2
- Thread 3 **never** sees a mix of config1 and config2
- Last write wins (config1 or config2, depending on scheduling)

**Guarantee:** All threads agree on the order of operations (linearizable history).

---

## What Can Still Go Wrong (and What Can't)

### ✓ Safe: Concurrent Reads During Update

```kotlin
// Thread 1
AppFeatures.load(newConfig)

// Threads 2-100
(2..100).forEach { i ->
    thread {
        val value = AppFeatures.darkMode(context)
    }
}
```

**Outcome:** All threads see consistent snapshots (old or new, never mixed).

### ✓ Safe: Multiple Concurrent Updates

```kotlin
thread { AppFeatures.load(config1) }
thread { AppFeatures.load(config2) }
thread { AppFeatures.load(config3) }
```

**Outcome:** Last write wins. Readers see one of the configs.

### ✗ Unsafe: Mutating Configuration After Load

```kotlin
// DON'T DO THIS
val config = AppFeatures.configuration
mutateSomehow(config)  // Breaks the "snapshot" mental model
```

**Issue:** `Configuration` is treated as immutable. Mutating it would break the snapshot guarantee (readers could
observe changes that did not come from `load(...)`).

**Mitigation:** Treat snapshots as immutable values. If you need a different configuration, deserialize a new snapshot
and call `load(...)`.

### ✗ Unsafe: Bypassing `load(...)`

```kotlin
// There is no supported public API for mutating a registry's internal state.
// Always update via `load(...)` (or `rollback(...)`), which swaps the full snapshot atomically.
```

**Issue:** Any hypothetical internal mutation would break the atomic swap guarantee.

---

## Formal Guarantee

**Invariant:** For any evaluation at time `t`, the returned value is computed using a configuration snapshot that was active at some time `t' ≤ t`.

**Corollary:** Readers never observe a configuration that was never active (no partial updates, no torn reads).

**Proof:**
1. `AtomicReference.set(...)` is a single atomic write
2. `AtomicReference.get(...)` returns the current reference atomically
3. No intermediate states exist between old and new reference
4. Therefore, readers see either old or new snapshot (both were active at some point)

---

## Next Steps

- [Fundamentals: Refresh Safety](/fundamentals/refresh-safety) — Practical implications
- [Fundamentals: Evaluation Semantics](/fundamentals/evaluation-semantics) — Atomic + deterministic
- [API Reference: Namespace Operations](/api-reference/namespace-operations) — `load(...)` API
