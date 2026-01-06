# Task

Split konditional-core public API into smaller modules and tighten visibility

# Instructions

Final minimal public API (what to keep public)
- Namespace (open class) — id only; implements minimal NamespaceRegistry interface but without lifecycle ops on core artifact.
- Feature<T, C : Context, M : Namespace> (interface) — key, namespace, id.
- Delegated factory functions (top-level): boolean<>, string<>, integer<>, double<>, enum<>, custom<> — stable DSL entrypoints.
- Rule DSL receiver types used by delegates (RuleDsl, VersionDsl, etc.) — small, focused surfaces only.
- Context (interface) + Context.Core (data class/factory) — locale, platform, appVersion, stableId, axisValues accessor.
- StableId factory helpers (StableId.of, StableId.fromHex).
- Feature evaluation extensions: evaluate(context, registry = namespace) and explain(context, registry = namespace).
- EvaluationResult<T> (small explainable result) and Decision sealed kinds (RegistryDisabled, Inactive, Rule, Default).
- FeatureId (value class) with create/parse helpers.
- Version type with parse/of/default (basic helpers + Comparable).
- Version/Platform/Locale tag interfaces and built-in enums (AppLocale, Platform) as read-only enums.
- Axis surface only as opaque AxisValues accessor; expose Axis/AxisValue only if third-party axes required but prefer opaque map.
- Minimal KonditionalLogger interface (NoOp default).

Everything else (serialization, lifecycle ops, metrics, detailed DTOs, internal bucketing helpers, advanced admin APIs) should be moved out of the tiny core.

2) Concrete list of symbols to hide / move and recommended action (apply in short phased steps)

Make these internal (or move to konditional-runtime / konditional-serialization / konditional-observability modules)

- Configuration (data class)
    - Action: change constructor to internal; expose read-only accessors or a lightweight view type if needed.
    - Rationale: produced/consumed only at JSON boundary and by admin ops.

- FlagDefinition<T, C, M>
    - Action: make constructor/internal and move to io.amichne.konditional.internal or runtime module; provide a FlagDefinitionView for introspection.
    - Rationale: implementation DTO, not needed for day-to-day evaluation.

- ConfigurationMetadata
    - Action: internal constructor; expose ConfigurationMetadata.of(...) factory if a public metadata view is necessary.
    - Rationale: operational metadata only.

- ConfigurationSnapshotCodec (object)
    - Action: move to konditional-serialization module (opt-in). Keep thin public loader façade in runtime module if needed.
    - Rationale: parsing/encoding is operational and should be opt-in.

- NamespaceSnapshotLoader / applyPatchJson / SnapshotLoadOptions / UnknownFeatureKeyStrategy / SnapshotWarning / ParseResult / ParseError
    - Action: move to konditional-serialization. For ParseResult/ParseError keep narrow boundary types but mark detailed error cases internal or in serialization artifact.
    - Rationale: keep decode/patch logic out of core.

- Namespace.load / Namespace.rollback / Namespace.historyMetadata / Namespace.configuration / NamespaceSnapshotLoader usage
    - Action: move lifecycle admin methods into konditional-runtime (NamespaceRegistry facade). In core, keep only name/id and registration semantics; deprecate old methods and add Runtime API in new artifact.
    - Rationale: Prevent accidental admin use by app developers.

- RegistryHooks / setHooks / Registry setHooks usage
    - Action: move to konditional-observability module; keep kavminimal KonditionalLogger in core (NoOp default). Make RegistryHooks an opt-in type in observability artifact.
    - Rationale: logging/metrics are opt-in and hot-path.

- MetricsCollector + Metrics payloads (Evaluation, ConfigLoadMetric, ConfigRollbackMetric)
    - Action: move to konditional-observability module; keep only tiny Metrics interface in runtime if absolutely necessary; otherwise provide adapter hooks.
    - Rationale: avoid polluting core with metric payloads.

- RampUp, RampUpBucketing, BucketInfo, RampUp utilities
    - Action: keep deterministic bucketing algorithm in core but hide detailed helpers (BucketInfo) behind explain() result; make RampUp value class internal constructor if possible or keep small public wrapper.
    - Rationale: expose minimal deterministic guarantees; hide implementation details.

- Shadow evaluation APIs (evaluateWithShadow, evaluateShadow, ShadowOptions, ShadowMismatch)
    - Action: move to konditional-observability or runtime module as opt-in advanced feature.
    - Rationale: advanced testing/ops functionality.

- Axis / AxisValue / Axis registration internals
    - Action: make Axis auto-registration internal; expose AxisValues as an opaque map/receiver; only keep public API for reading axes when necessary.
    - Rationale: axes are advanced extension points.

- Version.parse error types and low-level ParseError cases
    - Action: keep parse result but narrow public error surface; move fine-grained errors into serialization artifact.

- BucketInfo and RampUpBucketing.explain
    - Action: keep explain result minimal and internal; expose only fields needed by EvaluationResult.

- Any data classes currently public that are only needed by serialization or admin flows (search for data class ... internal constructor).
    - Action: change constructors to internal or move classes into .internal packages / runtime module.

3) Suggested file/path patterns to change (where to look)
- core API candidates:
    - src/main/kotlin/io/amichne/konditional/core/*.kt
    - src/main/kotlin/io/amichne/konditional/core/dsl/*.kt
    - src/main/kotlin/io/amichne/konditional/api/*.kt
    - src/main/kotlin/io/amichne/konditional/context/*.kt
    - src/main/kotlin/io/amichne/konditional/id/*.kt
- serialization & snapshot:
    - src/main/kotlin/io/amichne/konditional/serialization/**
    - src/main/kotlin/io/amichne/konditional/snapshot/**
- observability & runtime:
    - src/main/kotlin/io/amichne/konditional/observability/**
    - src/main/kotlin/io/amichne/konditional/runtime/** or namespace/**

(If you want, I can scan the repository and produce exact file paths for each symbol. Say “scan repo” and I’ll run the search and produce a file-by-file change list.)

4) Draft module split and PR plan (step-by-step)

High level split
- konditional-core (minimal): core DSL + evaluate/explain + Context, Feature, FeatureId, Version, StableId, Delegates, Rule DSL APIs, small KonditionalLogger.
- konditional-serialization (opt-in): Configuration, ConfigurationSnapshotCodec, NamespaceSnapshotLoader, ParseResult/ParseError, SnapshotLoadOptions, patch APIs.
- konditional-runtime (opt-in): NamespaceRegistry, lifecycle operations (load/rollback/enable/disable/history), ConfigurationMetadata view, introspection API (FlagDefinitionView).
- konditional-observability (opt-in): RegistryHooks, MetricsCollector, Metrics payloads, Shadow evaluation APIs.

PR plan (small iterative commits)
1. Prepare: add public-api policy file and CI check (kotlin-public-api or binary-compatibility-validator). Commit.
2. Create new modules in settings.gradle.kts: konditional-core, konditional-serialization, konditional-runtime, konditional-observability. Add Gradle module skeleton with dependencies (core -> none; runtime -> core + serialization optional; observability -> runtime).
3. Move files:
- Commit A: move serialization files into konditional-serialization module (package names unchanged). Add forwarding facades only if necessary.
- Commit B: move lifecycle/admin APIs into konditional-runtime; leave lightweight runtime facade in core that throws instructive exception if not present, and mark deprecated methods in core as @Deprecated(..., level = WARNING) pointing to runtime API.
4. Visibility tightening:
- Commit C: change constructors of Configuration, FlagDefinition, ConfigurationMetadata, BucketInfo to internal; add FlagDefinitionView & ConfigurationView types in runtime/serialization modules to expose read-only fields.
- Commit D: move RegistryHooks/MetricsCollector to konditional-observability; add small KonditionalLogger shim in core.
5. Deprecation & shims:
- Commit E: annotate old public members in core as @Deprecated("moved to konditional-runtime", level = DeprecationLevel.WARNING) with migration info.
- Commit F: add docs updates and migration guide in docs/ (docusaurus): quick start keeps core-only code; admin docs show runtime/obs usage.
6. CI & validation:
- Commit G: enable public-api enforcement and detekt rule to prevent new public symbols in implementation packages.
7. Final cleanup:
- After 1–2 releases deprecations -> HIDDEN/internal and remove shims.

PR metadata
- PR title: "Split public surface: core + serialization + runtime + observability; tighten visibility"
- Description: list of moved symbols, migration guidance, API compatibility notes, timeline for hidden removal.
- Include CHANGELOG entry and short migration guide.

Output:
- Run a repo scan and produce an exact file -> symbol -> recommended change list (I can apply edits).
- Create the Gradle module skeleton files for the split and open commits.
- Produce the deprecation patches (annotate public members with @Deprecated WARNING) for review.
