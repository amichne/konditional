---
title: Module Dependency Map
sidebar_position: 1
---

# Module Dependency Map

Lookup table for selecting the minimal Konditional modules by responsibility.

<span id="claim-clm-pr01-07a"></span>
Installation targets the core namespace model and runtime in-memory registry implementation.

| Module | Gradle coordinate | Provides | Depend on it when | Linked docs |
| --- | --- | --- | --- | --- |
| `konditional-core` | `io.github.amichne:konditional-core:VERSION` | Namespace model, typed feature declarations, evaluation API, deterministic bucketing primitives | You define features and call `evaluate(...)` in app code | [/quickstart/define-first-flag](/quickstart/define-first-flag), [/concepts/features-and-types](/concepts/features-and-types) |
| `konditional-runtime` | `io.github.amichne:konditional-runtime:VERSION` | `InMemoryNamespaceRegistry`, runtime lifecycle (`load`, `rollback`, `disableAll`), namespace snapshot loader wiring | You need mutable runtime config or rollback/kill-switch operations | [/quickstart/load-first-snapshot-safely](/quickstart/load-first-snapshot-safely), [/concepts/configuration-lifecycle](/concepts/configuration-lifecycle) |
| `konditional-serialization` | `io.github.amichne:konditional-serialization:VERSION` | Snapshot codec, patch application, load options and warning strategies | You ingest JSON snapshots/patches across trust boundaries | [/reference/snapshot-format](/reference/snapshot-format), [/reference/patch-format](/reference/patch-format) |
| `konditional-observability` | `io.github.amichne:konditional-observability:VERSION` | Shadow evaluation APIs and mismatch reporting | You run migration dual-evaluation or mismatch telemetry | [/guides/migration-from-legacy](/guides/migration-from-legacy), [/theory/migration-and-shadowing](/theory/migration-and-shadowing) |
| `konditional-otel` | `io.github.amichne:konditional-otel:VERSION` | OpenTelemetry metrics/traces for feature evaluation lifecycle | You emit standardized telemetry for operational monitoring | [/guides/enterprise-adoption](/guides/enterprise-adoption) |
| `konditional-http-server` | `io.github.amichne:konditional-http-server:VERSION` | HTTP delivery surface for configuration/runtime endpoints | You expose internal config-delivery endpoints | [/guides/remote-configuration](/guides/remote-configuration) |
| `openfeature` | `io.github.amichne:openfeature:VERSION` | OpenFeature provider integration and context mapping | You bridge Konditional into OpenFeature-compatible clients | [/guides/enterprise-adoption](/guides/enterprise-adoption) |
| `kontracts` | `io.github.amichne:kontracts:VERSION` | Schema DSL and constrained value contracts | You define structured custom values with schema-backed boundaries | [/guides/custom-structured-values](/guides/custom-structured-values) |
| `openapi` | `io.github.amichne:openapi:VERSION` | OpenAPI schema generation helpers for contract surfaces | You publish machine-readable API/schema contracts | [/guides/enterprise-adoption](/guides/enterprise-adoption) |
| `server/*` modules | `io.github.amichne:server-...:VERSION` | Deployment-specific integration surfaces | You need hosted/runtime integration instead of in-process loading | [/guides/remote-configuration](/guides/remote-configuration) |

## Recommended Baselines

| Use case | Minimal modules |
| --- | --- |
| Typed local evaluation only | `konditional-core` |
| Evaluation + remote/runtime loading | `konditional-core`, `konditional-runtime`, `konditional-serialization` |
| Migration and observability rollout | Baseline + `konditional-observability` |
| Enterprise delivery pipeline | Baseline + `konditional-otel` + selected `server/*` integration |

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-07A | Installation targets the core namespace model and runtime in-memory registry implementation. |

## Next Steps

- [API Surface](/reference/api-surface) - Inspect claim-linked public API signatures.
- [Quickstart Install](/quickstart/install) - Apply the baseline dependency block.
