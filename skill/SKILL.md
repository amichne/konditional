---
name: konditional
description: >
  Use this skill whenever work involves Konditional feature flags with the current
  Namespace-first Kotlin API: namespace design, typed flag definitions, deterministic
  rule DSL, runtime snapshot loading/rollback, shadow evaluation, OpenFeature mapping,
  and migration from string-keyed/boolean-matrix systems.
---

# Konditional Skill (Current API)

This skill is the canonical integration guide for Konditional in this repository.
It is aligned to the current code surface (Namespace-first, delegated typed flags),
not legacy `FeatureContainer` patterns.

## When to trigger

- Defining new flags or namespaces in Kotlin.
- Migrating from string-keyed flags, config maps, or boolean capability matrices.
- Implementing rule targeting (locale/platform/version/axis/extension/rampUp).
- Loading remote JSON snapshots into namespaces with typed boundary errors.
- Adding shadow/candidate evaluation without baseline behavior drift.
- Integrating OpenFeature while preserving typed boundary parsing.

## Current contracts (signature-backed)

- **Namespace-first definitions**: define flags directly on `object : Namespace("id")` via delegated properties (`boolean`, `string`, `integer`, `double`, `enum`, `custom`). [CLM-NS-001]
- **Typed context model**: `Context` plus mix-ins (`LocaleContext`, `PlatformContext`, `VersionContext`, `StableIdContext`) and optional custom fields via subtypes. [CLM-CTX-001]
- **Deterministic rule DSL**: use `rule(value) { ... }` or boolean sugar `enable { ... }` / `disable { ... }`; allow `anyOf`, `axis`, `extension`, `versions`, `rampUp`, `allowlist`, and optional `ruleSet` composition. [CLM-DSL-001]
- **Runtime lifecycle is module-scoped**: load/rollback/history APIs are in `:konditional-runtime` (`Namespace.load`, `Namespace.rollback`). [CLM-RT-001]
- **Boundary discipline**: snapshot decoding requires compiled schema and returns `Result` with typed `ParseError` payloads (`parseErrorOrNull`) instead of exception-first control flow. [CLM-BND-001]
- **Shadow evaluation safety**: `evaluateWithShadow` returns baseline value and reports mismatch side-channel only. [CLM-SHD-001]

See `/Users/amichne/code/konditional/skill/resources/evidence-map.md` for exact symbol/test linkage.

## Execution workflow (token-efficient)

1. Start in signatures:
   - `/Users/amichne/code/konditional/signatures/INDEX.sig`
   - Then only relevant `*.sig` under affected modules.
2. Use IntelliJ semantic tools for symbol work (definition/references/diagnostics) before text search.
3. Read source only for selected symbols after signature narrowing.
4. Reuse existing recipe/test patterns before inventing new DSL usage.
5. Keep edits scoped by module boundary:
   - `konditional-core` (types + DSL + evaluation)
   - `konditional-runtime` (load/rollback/state)
   - `konditional-serialization` (trusted decode/materialization)
   - `konditional-observability` (shadow/mismatch/explain hooks)
6. For boundaries, return typed failures and preserve last-known-good behavior.

## Authoring standards for responses

- Prefer concise, compile-oriented snippets using current imports and APIs.
- Model invariants with Kotlin types; avoid stringly typed identifiers in core logic.
- Make ordering deterministic and document tie-break behavior explicitly.
- Do not introduce DI frameworks or reflection-heavy registries unless requested.
- Keep observability hooks side-effect-only; do not alter evaluation semantics.

## Resources

- **Code samples (current API):**
  - `/Users/amichne/code/konditional/skill/resources/konditional_samples.kt`
- **Claim-to-signature/test map:**
  - `/Users/amichne/code/konditional/skill/resources/evidence-map.md`
- **OpenAI prompt/tooling best practices (official-doc derived):**
  - `/Users/amichne/code/konditional/skill/resources/openai_prompting_best_practices.md`

## Quick usage template

Use this template when answering implementation requests:

1. Identify affected module(s) and invariants.
2. Mirror existing Namespace + DSL patterns from repository samples.
3. Add/adjust boundary parsing with typed `ParseError` flow if inputs are untrusted.
4. Add tests for determinism, boundary failure shape, and namespace isolation where relevant.
5. Validate compilation/tests for touched modules.

