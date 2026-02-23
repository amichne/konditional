---
name: konditional
description: >
  Use this skill for enterprise Konditional adoption: discover existing flag and
  config usage, map legacy systems to namespace-scoped typed APIs, migrate with
  dual-read and shadow evaluation, and ship with deterministic runtime and
  boundary-safe operations.
---

# Konditional skill (enterprise integration and adoption)

This skill is the canonical enterprise adoption guide for Konditional in this
repository. Use it when teams need to discover and adapt existing codebases to
the namespace-based API without behavior drift.

## Primary outcomes

- Inventory legacy flag usage and ownership boundaries in existing services.
- Convert string-keyed usage into namespace-scoped typed definitions.
- Preserve production behavior during migration with dual-read and shadowing.
- Keep guidance token-efficient by using signatures and symbol-scoped reads.

## Trigger when

- Engineers need a migration path from string keys, config maps, or SDK wrappers.
- Teams are defining namespaces and typed feature surfaces for enterprise domains.
- Integrations require runtime snapshot loading, rollback, and kill-switch safety.
- You need OpenFeature interoperability with typed context mapping.
- You are planning phased rollout with mismatch observability and rollback drills.

## Current contracts (signature-backed)

- **Namespace-first definitions**: define flags directly on
  `object : Namespace("id")` via delegated properties (`boolean`, `string`,
  `integer`, `double`, `enum`, `custom`). [CLM-NS-001]
- **Typed context model**: use `Context` plus mix-ins (`LocaleContext`,
  `PlatformContext`, `VersionContext`, `StableIdContext`) and custom subtype
  extensions for business fields. [CLM-CTX-001]
- **Deterministic rule DSL**: use `rule(value)`, `enable`, `disable`, `anyOf`,
  `axis`, `extension`, `versions`, `rampUp`, `allowlist`, and `ruleSet`.
  [CLM-DSL-001]
- **Namespace isolation by construction**: each namespace owns its registry and
  axis catalog; typed axis inference remains namespace-scoped. [CLM-ISO-001]
- **Runtime lifecycle in runtime module**: `Namespace.load`,
  `Namespace.rollback`, `history`, and `historyMetadata` live in
  `:konditional-runtime`. [CLM-RT-001]
- **Boundary-safe snapshot loading**: `NamespaceSnapshotLoader` decodes with
  compiled schema, returns typed parse failures, and keeps namespace context in
  boundary errors. [CLM-BND-002]
- **Typed parse failure model**: boundary failures are typed `ParseError`
  variants and introspectable through `parseErrorOrNull()`. [CLM-BND-001]
- **Shadow migration safety**: `evaluateWithShadow` returns baseline value and
  reports mismatch side-channel only. [CLM-SHD-001]
- **OpenFeature adapter safety**: provider context mapping uses typed success or
  typed mapping failure, and flag lookup is pre-indexed once. [CLM-OF-001]

See `/Users/amichne/code/konditional/skill/resources/evidence-map.md` for exact
signature/source/test links.

## Enterprise migration workflow

1. **Discovery and inventory**
   - Build a deterministic inventory of existing keys, call sites, defaults, and
     owners.
   - Prefer signatures first, then scoped text search in app repos.
   - Produce a migration table keyed by `legacy_key -> namespace.feature`.
2. **Namespace and ownership mapping**
   - Group flags by team/domain blast radius into namespace boundaries.
   - Keep one rollback/kill-switch control plane per namespace.
3. **Type conversion**
   - Replace string lookups with delegated typed features.
   - Move untyped context maps into typed `Context` subtypes and axes.
4. **Adapter phase (no behavior drift)**
   - Add a dual-read adapter that compares legacy value vs Konditional value.
   - Keep legacy value as baseline until mismatch rate is below rollout target.
5. **Runtime boundary hardening**
   - Load snapshots with `NamespaceSnapshotLoader` and typed error handling.
   - Preserve last-known-good snapshot on failures.
6. **Shadow and promotion**
   - Use `evaluateWithShadow` and emit structured mismatch telemetry.
   - Promote candidate only after deterministic gates pass.
7. **Integration finalization**
   - Add OpenFeature provider/mappers for external clients.
   - Run rollback and kill-switch drills before production cutover.

## Token-efficient execution workflow

1. Start with `/Users/amichne/code/konditional/signatures/INDEX.sig`.
2. Open only relevant `*.sig` files for target modules and symbols.
3. Use IntelliJ semantic symbol lookup for definitions/references before
   broad text search.
4. Read source only for symbols already selected from signatures.
5. Reuse existing docs samples and tests before introducing new patterns.
6. Keep all guidance scoped to one module boundary at a time:
   - `konditional-core`
   - `konditional-runtime`
   - `konditional-serialization`
   - `konditional-observability`
   - `openfeature`

## Response contract for this skill

- Produce adoption guidance in phases, not one-shot rewrites.
- Always include: boundary assumptions, rollback plan, and mismatch strategy.
- Prefer compile-ready snippets using current imports and APIs.
- Avoid DI frameworks, reflection registries, and stringly-typed core models.
- Keep observability side-channel only; never alter baseline semantics.

## Resources

- **Enterprise and API samples:**
  - `/Users/amichne/code/konditional/skill/resources/konditional_samples.kt`
- **Claim-to-signature/source/test map:**
  - `/Users/amichne/code/konditional/skill/resources/evidence-map.md`
- **OpenAI prompt/tooling best practices for skill authoring:**
  - `/Users/amichne/code/konditional/skill/resources/openai_prompting_best_practices.md`

## Quick usage template

Use this template when answering enterprise migration requests:

1. Identify target modules and invariants.
2. Build a legacy inventory and namespace mapping table.
3. Propose a dual-read and shadow rollout slice with explicit rollback gates.
4. Add boundary-safe loading with typed parse error handling for untrusted inputs.
5. Add tests for determinism, namespace isolation, boundary failures, and
   migration mismatch reporting.
