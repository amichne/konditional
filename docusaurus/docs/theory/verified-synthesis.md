# Verified Design Synthesis

This page synthesizes the current Konditional docs and implementation behavior into one code-verified model.

Scope used for verification:

- `docusaurus/docs/theory/parse-dont-validate.md`
- `docusaurus/docs/theory/determinism-proofs.md`
- `docusaurus/docs/theory/atomicity-guarantees.md`
- `docusaurus/docs/theory/migration-and-shadowing.md`
- `konditional-core`, `konditional-runtime`, `konditional-serialization`, `konditional-observability`

---

## [Page 1]: Parse Boundary (`parse-dont-validate`)

1. **Novel Mechanism**: `NamespaceSnapshotLoader` decodes and loads in one typed operation (`ParseResult<Configuration>`), and uses namespace-scoped feature indexing when available.
2. **Constraint/Gotcha**: direct decode with an empty feature index can fall back to deprecated global `FeatureRegistry` lookup semantics.
3. **Composition Point**: `ConfigurationSnapshotCodec` provides pure decode; runtime mutation is applied separately through `Namespace.load(...)`.
4. **Performance Implication**: strict unknown-key handling fails fast by default; skip mode avoids hard-fail at the cost of warning-path handling.
5. **Misuse Prevention**: parse APIs force success/failure branching through a sealed result (`ParseResult` + `ParseError`).

-----

## [Page 2]: Deterministic Evaluation (`determinism-proofs`)

1. **Novel Mechanism**: ramp-up bucketing is `SHA-256(salt:featureKey:stableIdHex)` modulo `10_000`.
2. **Constraint/Gotcha**: contexts without `StableIdContext` use fallback bucket `9999`, which excludes them from partial rollout unless allowlisted or at 100% ramp-up.
3. **Composition Point**: `FlagDefinition.evaluateTrace` computes bucket once and reuses it while scanning rule candidates.
4. **Performance Implication**: a thread-local digest avoids repeated `MessageDigest` construction on hot paths.
5. **Misuse Prevention**: `RampUp` is a value class with constructor range checks (`0.0..100.0`).

-----

## [Page 3]: Atomic Snapshot State (`atomicity-guarantees`)

1. **Novel Mechanism**: the default runtime registry keeps `current` and `history` in atomic references while serializing writes with a private lock.
2. **Constraint/Gotcha**: `updateDefinition(...)` mutates current snapshot atomically but does not append rollback history.
3. **Composition Point**: runtime lifecycle extensions (`load`, `rollback`, `history`) route to `NamespaceRegistryRuntime`.
4. **Performance Implication**: reads are lock-free (`AtomicReference.get`), while writes pay synchronization cost for linearizable history updates.
5. **Misuse Prevention**: runtime mutation is split behind runtime contracts, while `core` remains on read-only registry surface.

-----

## [Page 4]: Shadow Migration (`migration-and-shadowing`)

1. **Novel Mechanism**: `evaluateWithShadow` computes baseline and candidate `EvaluationResult`s and returns baseline value only.
2. **Constraint/Gotcha**: candidate evaluation is skipped by default when baseline kill-switch is enabled (`evaluateCandidateWhenBaselineDisabled = false`).
3. **Composition Point**: mismatch reporting combines callback delivery (`onMismatch`) with built-in warning logs through registry hooks.
4. **Performance Implication**: shadow mode is effectively two evaluations plus mismatch object creation on the request path.
5. **Misuse Prevention**: default `ShadowOptions` reports value mismatches only and keeps candidate evaluation conservative.

-----

## Synthesis

### 1. Core Abstraction: Typed Snapshot Boundary

Konditional separates untrusted JSON from trusted evaluation state via `ParseResult<Configuration>` and namespace-scoped loading. Core evaluation does not consume raw payloads; it consumes already-typed snapshots. This keeps boundary failure explicit while preserving compile-time safety once data is inside the model.

**Advocate**: Use this when production config is remote and failure isolation is mandatory; typed parse boundaries and explicit failure ADTs prevent silent drift.
**Oppose**: For static-only configs with no runtime updates, this can be unnecessary ceremony versus compile-time constants.

### 2. Critical Integration Pattern: Deterministic Evaluate + Atomic Swap

Evaluation determinism depends on stable bucketing and fixed rule precedence, while runtime consistency depends on atomic snapshot reads/writes. The two parts compose as: deterministic pure evaluation over whichever snapshot was atomically visible at read time. If either piece is bypassed (non-deterministic bucketing or partial mutation), correctness guarantees degrade immediately.

**Advocate**: This pattern is superior for multi-threaded production services where reproducibility and rollback confidence matter.
**Oppose**: In single-threaded, low-risk environments, full snapshot machinery may exceed practical complexity needs.

### 3. Production Readiness Gap: Boundary and Hook Cost Discipline

Docs emphasize correctness guarantees but under-specify operational costs of callback/hook behavior on hot paths and fallback registry behavior during decode. `onMismatch`, logging hooks, and warning hooks execute inline; heavy implementations directly increase latency. The safe posture is lightweight hooks plus explicit sampling/control policies at call sites.

**Advocate**: Treating hook cost and decode mode as first-class operational policy is non-negotiable at scale.
**Oppose**: For internal tooling with low QPS, aggressive optimization and strict policy controls may be unnecessary overhead.

---

## Related

- [Parse Don't Validate](/theory/parse-dont-validate)
- [Determinism Proofs](/theory/determinism-proofs)
- [Atomicity Guarantees](/theory/atomicity-guarantees)
- [Migration and Shadowing](/theory/migration-and-shadowing)
