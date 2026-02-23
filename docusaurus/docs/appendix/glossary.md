---
title: Glossary
sidebar_position: 1
---

# Glossary

Canonical terms used across Konditional docs.

| Term | Definition |
| --- | --- |
| Namespace | A scoped container (`Namespace`) that owns feature declarations and runtime state for one domain boundary. |
| Feature | A typed declaration (`Feature<T, C, M>`) that binds value type, context type, and namespace type together. |
| FlagDefinition | The declared default plus ordered conditional values used to evaluate a feature. |
| Context | Runtime input (`Context`) used for rule matching; commonly includes locale, platform, app version, and stable ID. |
| Rule | A targeting clause that yields a value when its criteria match the incoming context. |
| RuleSet | A reusable collection of rules that can be attached to a feature definition. |
| Snapshot | A full JSON configuration payload materialized into trusted configuration before runtime load. |
| Patch | An incremental JSON payload that updates or removes existing snapshot entries. |
| ParseError | A sealed error taxonomy that identifies boundary decode failures (invalid JSON, unknown feature key, invalid snapshot, and related cases). |
| KonditionalBoundaryFailure | Structured failure wrapper that carries `ParseError` in failed `Result` values. |
| StableId | Deterministic identity handle used for rollout bucketing and allowlist targeting. |
| HexId | Canonical hex form used by `StableId` for deterministic bucketing inputs. |
| Bucketing | Deterministic assignment of a `(stableId, featureKey, salt)` tuple into a fixed bucket space. |
| RampUp | Percentage gate (`RampUp`) that controls how many matching contexts receive a rule value. |
| ConfigurationView | Read-only interface for currently active runtime configuration and metadata. |
| MaterializedConfiguration | Trusted snapshot wrapper produced by schema-aware decode and used for runtime loads. |
| SnapshotLoadOptions | Boundary policy object controlling unknown keys, missing declared flags, and warning behavior. |

## Next Steps

- [Module Dependency Map](/reference/module-dependency-map) - Pick the minimal modules for your use case.
- [Quickstart](/quickstart/) - Move from terms to runnable usage.
