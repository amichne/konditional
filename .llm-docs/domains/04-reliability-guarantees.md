# Context: Konditional Reliability Guarantees

You are documenting the reliability invariants that Konditional maintains. Your audience is engineers responsible for production systems who need to understand failure modes and concurrency behavior.

## Scope

Enumerate and justify:

- **Thread-safety model**: Lock-free reads, atomic writes, no torn reads
- **Determinism**: Same inputs → same outputs, no PRNG, no time-dependence
- **Non-null guarantees**: Default requirement ensures every evaluation produces a value
- **Namespace isolation**: Separate registries, no cross-contamination
- **Atomicity of updates**: Configuration replacement semantics

## For Each Guarantee, Document

1. **The precise invariant statement**: What exactly is guaranteed?
2. **The mechanism that enforces it**: How is this achieved in code?
3. **The failure mode it prevents**: What bad thing can't happen?
4. **Conditions under which it might not hold**: Edge cases, misuse, external factors

## Thread-Safety Model

Document the concurrency guarantees:

### Read Path
- Are reads lock-free?
- Can a read ever block on a write?
- What memory visibility guarantees apply?

### Write Path (Configuration Updates)
- How does `Namespace.load()` work atomically?
- Can readers see a partially-applied configuration?
- What happens to in-flight evaluations during an update?

### Data Structures
- What synchronization primitives are used? (`AtomicReference`, `volatile`, locks?)
- Is copy-on-write used for configuration state?

## Determinism Guarantees

Document what makes evaluation deterministic:

### Inputs That Affect Evaluation
- Context fields (locale, platform, version, stableId)
- Feature definition (rules, default, salt)
- Nothing else (no system time, no random numbers, no external state)

### SHA-256 Bucketing Determinism
- Same inputs → same hash → same bucket, always
- Platform-independent (JVM, Android, iOS, JS produce identical results)
- No use of `Random` or system entropy

### Rule Evaluation Determinism
- Specificity sorting is stable
- First-match-wins is deterministic given sorted rules
- No undefined behavior in edge cases

## Non-Null Guarantees

Document the structural prevention of null:

- Default values are required at definition time
- Return type of `feature { }` is `T`, not `T?`
- No code path can produce null from evaluation
- Exception: What happens if someone passes null via reflection? (Answer: outside threat model)

## Namespace Isolation

Document how namespaces prevent collisions:

- Each namespace has its own registry instance
- `FeatureContainer<N>` is type-bound to namespace `N`
- Features are registered to the container's namespace
- Two containers in different namespaces can have same-named properties safely

## Atomicity of Configuration Updates

Document the update semantics:

```kotlin
// What guarantees does this provide?
Namespace.Global.load(newConfiguration)
```

- Is the entire configuration replaced atomically?
- Can a reader see some features from old config and some from new?
- What happens if `load()` is called concurrently from two threads?

## Failure Modes Prevented

| Failure Mode | Prevention Mechanism |
|--------------|---------------------|
| Race condition on read | Lock-free reads via atomic reference |
| Torn read (partial config) | Atomic reference swap |
| Null pointer in evaluation | Required defaults, non-nullable return |
| Non-deterministic bucketing | SHA-256 with deterministic inputs |
| Cross-namespace collision | Type-bound containers, separate registries |

## Failure Modes NOT Prevented

Be explicit about what can still go wrong:

- Semantic errors in configuration (wrong rollout percentage)
- Business logic errors (targeting wrong users)
- JSON parse failures (handled via `ParseResult`, but can happen)
- Memory exhaustion (not a Konditional-specific concern)

## Out of Scope (defer to other domains)

- API syntax → See `01-public-api.md`
- Evaluation algorithm details → See `02-internal-semantics.md`
- Type safety proofs → See `03-type-safety-theory.md`
- JSON serialization lifecycle → See `05-configuration-integrity.md`

## Constraints

- Be precise about concurrency guarantees; don't overstate
- Reference JVM memory model where relevant
- Distinguish "can't happen" from "won't happen if used correctly"
- Include what happens under adversarial conditions (malformed input, concurrent abuse)

## Output Format

For reliability documentation, produce:
1. Invariant statement (precise, testable)
2. Mechanism (how it's enforced)
3. Failure mode prevented (what bad thing can't happen)
4. Boundary conditions (when this might not hold)
5. Verification approach (how to test this guarantee)

## Context Injection Point

When documenting concurrency mechanisms, inject source here:

```
[INSERT: Registry implementation, atomic reference usage, configuration loading code]
```
