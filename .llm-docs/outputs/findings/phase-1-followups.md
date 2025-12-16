Phase 1 Follow-ups (Deferred)

These are issues discovered while addressing the Phase 1 findings, but not treated as urgent blockers for the refactor/tests completed in this task.

0. SER-01 (Global Registry Lifecycle)
   - `FeatureRegistry` is process-global and currently required for snapshot deserialization (identifier → feature lookup).
   - In-process “restart simulation” requires `FeatureRegistry.clear()` to allow re-registering the same `(namespaceSeed, key)` feature IDs.
   - Potential future direction: introduce an explicit `FeatureResolver` parameter for snapshot deserialization (or per-namespace registries) to remove implicit global state and make lifecycle semantics explicit.

1. DOC-01 (CONTRIBUTING.md Legacy Sections)
   - `CONTRIBUTING.md` still contains large legacy sections referencing `FeatureModule`, `ModuleRegistry`, and enum-based feature definitions that do not exist in the current codebase.
   - This task updated the most directly impacted sections (removed `EncodableValue` references), but a full document pass is still needed.

2. API-01 (ValueType Drift)
   - `io.amichne.konditional.core.ValueType` currently includes values like `LONG`, `JSON`, `JSON_OBJECT`, `JSON_ARRAY` that are not represented in the current `FlagValue` serialization model.
   - Recommendation: either remove unused variants or extend `FlagValue` to cover them, then add round-trip tests.

3. PERF-01 (Bucketing Digest Allocation)
   - `FlagDefinition.stableBucket` creates a new `MessageDigest` per evaluation for thread-safety; this is correct but may be a hot-path allocation cost at scale.
   - Potential follow-up: `ThreadLocal<MessageDigest>` (still deterministic), and microbenchmarks to validate impact.

4. DOC-02 (Terminology Consistency)
   - Some KDoc and comments still use `contextFn` while the public API parameter name is `context`.
   - Recommendation: normalize terminology to reduce cognitive overhead.

5. WARN-01 (AxisValues Deprecation Warning)
   - `AxisValues` overrides `toArray(IntFunction<...>)`, which triggers deprecation warnings in recent Kotlin/JDK toolchains.
   - Recommendation: remove the override (or isolate behind an API that doesn’t require `toArray`), then re-run compilation to confirm warning elimination.
