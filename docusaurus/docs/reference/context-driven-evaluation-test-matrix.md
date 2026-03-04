# Context-Driven Evaluation — Test Matrix

Covers invariants introduced in deliverables D1–D7 of the Context-Driven Evaluation Refactor.

---

## Invariant legend

| Code | Invariant |
|---|---|
| **DET** | Determinism — repeated evaluation on identical inputs yields identical outputs |
| **ATOM** | Atomicity — readers see only complete old-or-new snapshots, never partial state |
| **ISO** | Namespace isolation — operations in one namespace cannot affect another |
| **BOUND** | Typed boundary errors — external input failures surface as `ParseError` values, not exceptions |
| **SHADOW** | Shadow non-invasiveness — the candidate path never changes the baseline result |

---

## D2 — Typed identifiers (`ContextKey`, `AxisKey`, `ExternalSnapshotRef`)

| Test | File | Invariant | Assertion |
|---|---|---|---|
| `ContextKey rejects blank id` | `CoreTypeModelTest` | **BOUND** | `require` throws `IllegalArgumentException` |
| `ContextKey equality by id` | `CoreTypeModelTest` | **DET** | Equal ids → equal keys |
| `AxisKey rejects blank id` | `CoreTypeModelTest` | **BOUND** | `require` throws `IllegalArgumentException` |
| `AxisKey equality by id` | `CoreTypeModelTest` | **DET** | Equal ids → equal keys |
| `Axis.key delegates to id` | `CoreTypeModelTest` | **DET** | `axis.key == AxisKey(axis.id)` |
| `ExternalSnapshotRef.versioned validates` | `CoreTypeModelTest` | **BOUND** | Blank id or version → `IllegalArgumentException` |
| `ExternalSnapshotRef equality` | `CoreTypeModelTest` | **DET** | Equal id+version → equal refs |

---

## D3 — Predicate registry

| Test | File | Invariant | Assertion |
|---|---|---|---|
| `BuiltIn rejects blank id` | `PredicateRegistryTest` | **BOUND** | `require` throws |
| `Registered rejects blank namespaceId` | `PredicateRegistryTest` | **BOUND** | `require` throws |
| `Registered rejects blank id` | `PredicateRegistryTest` | **BOUND** | `require` throws |
| `BuiltIn sorts before Registered` | `PredicateRegistryTest` | **DET** | `BuiltIn < Registered` |
| `BuiltIn refs sort lexicographically` | `PredicateRegistryTest` | **DET** | stable ordering by id |
| `Registered refs sort by namespaceId then id` | `PredicateRegistryTest` | **DET** | stable compound key |
| `resolved predicate is callable` | `PredicateRegistryTest` | **DET** | registered predicate evaluates correctly |
| `resolve unknown ref returns UnknownPredicate` | `PredicateRegistryTest` | **BOUND** | typed `ParseError.UnknownPredicate` |
| `resolve BuiltIn ref returns UnknownPredicate` | `PredicateRegistryTest` | **BOUND** | BuiltIn not resolvable via user registry |
| `registries in different namespaces are isolated` | `PredicateRegistryTest` | **ISO** | cross-namespace registration rejected |
| `registeredRefs returns insertion-ordered list` | `PredicateRegistryTest` | **DET** | insertion order preserved |

---

## D4 — Structured parse error variants

| Test | File | Invariant | Assertion |
|---|---|---|---|
| `UnknownField carries path in message` | `ParseErrorVariantsTest` | **BOUND** | message contains path |
| `MissingRequired carries path in message` | `ParseErrorVariantsTest` | **BOUND** | message contains path |
| `InvalidValue carries path and reason` | `ParseErrorVariantsTest` | **BOUND** | message contains path and reason |
| `ParseError sealed when is exhaustive` | `ParseErrorVariantsTest` | **BOUND** | compile-time exhaustiveness |
| `malformed JSON returns InvalidJson` | `ParseErrorFixtureTest` | **BOUND** | no exception thrown |
| `unknown feature key returns FeatureNotFound` | `ParseErrorFixtureTest` | **BOUND** | typed `ParseError.FeatureNotFound` |
| `null flags returns InvalidJson` | `ParseErrorFixtureTest` | **BOUND** | typed `ParseError.InvalidJson` |

---

## D5 — `NamespaceSnapshot` atomic exchange

| Test | File | Invariant | Assertion |
|---|---|---|---|
| `empty sentinel has null version` | `NamespaceSnapshotTest` | **DET** | `NamespaceSnapshot.empty.version == null` |
| `version delegates to configuration metadata` | `NamespaceSnapshotTest` | **DET** | version equals metadata.version |
| `data class equality` | `NamespaceSnapshotTest` | **DET** | equal configs → equal snapshots |
| `currentSnapshot before load is empty` | `NamespaceSnapshotTest` | **ATOM** | empty sentinel returned before first load |
| `currentSnapshot after load reflects config` | `NamespaceSnapshotTest` | **ATOM** | snapshot matches loaded config |
| `concurrent read/write never sees partial state` | `NamespaceSnapshotTest` | **ATOM** | version coherence under 8-thread concurrent load |
| `two registries are snapshot-isolated` | `NamespaceSnapshotTest` | **ISO** | load in one does not affect the other |
| Linearizability (full suite) | `NamespaceLinearizabilityTest` | **ATOM** | jcstress linearizability proofs |

---

## D6 — External snapshot backstop registry

| Test | File | Invariant | Assertion |
|---|---|---|---|
| `Versioned rejects blank id` | `ExternalBackstopTest` | **BOUND** | `require` throws |
| `Versioned rejects blank version` | `ExternalBackstopTest` | **BOUND** | `require` throws |
| `parse() returns typed error for blank id` | `ExternalBackstopTest` | **BOUND** | `ParseError.UnversionedExternalRef` |
| `parse() returns typed error for blank version` | `ExternalBackstopTest` | **BOUND** | `ParseError.UnversionedExternalRef` |
| `parse() succeeds for valid id and version` | `ExternalBackstopTest` | **BOUND** | `Result.success` |
| `UnversionedExternalRef message contains id and reason` | `ExternalBackstopTest` | **BOUND** | structured message |
| `registry accepts valid ref` | `ExternalBackstopTest` | **DET** | ref appears in `registeredRefs` |
| `registry rejects blank id` | `ExternalBackstopTest` | **BOUND** | typed error returned |
| `registry rejects blank version` | `ExternalBackstopTest` | **BOUND** | typed error returned |
| `registeredRefs preserves insertion order` | `ExternalBackstopTest` | **DET** | deterministic ordering |
| `two registries are independent` | `ExternalBackstopTest` | **ISO** | refs registered in one not visible in other |
| `ExternalSnapshotRef sealed when is exhaustive` | `ExternalBackstopTest` | **BOUND** | compile-time exhaustiveness |

---

## D7 — Namespace-scoped shadow evaluation

| Test | File | Invariant | Assertion |
|---|---|---|---|
| `evaluateWithShadow returns baseline and reports mismatched values` | `ShadowEvaluationTest` | **SHADOW** | baseline value returned; mismatch reported |
| `evaluateWithShadow skips candidate when baseline is disabled` | `ShadowEvaluationTest` | **SHADOW** | no candidate evaluation when kill-switch active |
| `evaluateWithShadow evaluates candidate when baseline is disabled if enabled by options` | `ShadowEvaluationTest` | **SHADOW** | opt-in override works correctly |
| `ShadowMismatch carries namespaceId and detectedAtEpochMillis` | `ShadowEvaluationTest` | **SHADOW** | fields populated from baseline context |
| `evaluateWithShadow skips candidate when namespace not in enabledForNamespaces` | `ShadowEvaluationTest` | **SHADOW** + **ISO** | excluded namespace → no candidate eval, baseline unchanged |
| `evaluateWithShadow runs candidate when namespace is in enabledForNamespaces` | `ShadowEvaluationTest` | **SHADOW** + **ISO** | included namespace → candidate evaluated, mismatch reported |

---

## Regression baseline

Run `make test` (or `./gradlew test`) to execute the full matrix. Expected result: **BUILD SUCCESSFUL**, zero test failures across all modules.

Key modules to validate:

```
./gradlew :konditional-core:test
./gradlew :konditional-serialization:test
./gradlew :konditional-runtime:test
./gradlew :konditional-observability:test
```
