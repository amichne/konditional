# Docusaurus Docs Migration Backlog

This backlog converts the approved IA into an executable migration plan for `docusaurus/docs`.

## Scope

- Fixed L0 scaffold:
  - Overview (Start Here)
  - Quickstart
  - Use Cases
  - Guides
  - Concepts
  - Architecture (How it works)
  - Reference
  - Trust (Security / Privacy / Reliability)
  - Troubleshooting & FAQ
  - Glossary
- Output format:
  - Page-by-page issue list
  - Source-to-target mappings
  - Ownership, dependencies, and acceptance checks
  - PR batching order

## Working Agreements

- Preserve existing technical correctness from:
  - `docusaurus/docs/theory/*.md`
  - `docusaurus/docs/reference/api/*.md`
  - Module references in `docusaurus/docs/{core,runtime,serialization,observability,opentelemetry,kontracts,konditional-spec}`
- Prefer merge/split over rewrite.
- Each migrated page must include:
  - What you will achieve/learn
  - Prerequisites
  - Main content
  - Next steps
- Each task guide must include:
  - Goal, Steps, Verify, Rollback (if applicable), Common issues

## Milestones

- MLD (minimum lovable docs):
  - Overview + Quickstart + 3 Use Cases + 4 Concepts + 4 Guides + Reference hubs + top troubleshooting paths + base glossary
- Expanded:
  - Full Architecture, full Trust, complete Reference, full Troubleshooting, split Glossary domains

## Issue List

Columns:
- `ID`: backlog item identifier
- `Target`: destination page path
- `Action`: New / Revise / Split / Merge / Remove
- `Source`: current pages to migrate from
- `Owner`: recommended role
- `Depends on`: prerequisite issue IDs
- `Batch`: PR batch

### 1) Overview (Start Here)

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-OV-01 | `docusaurus/docs/overview/start-here.md` | Merge | `docusaurus/docs/index.md`, `docusaurus/docs/quick-start/what-is-konditional.md`, `README.md` | DX writer + core maintainer | - | PR-01 |
| DOC-OV-02 | `docusaurus/docs/overview/product-value-fit.md` | Merge | `docusaurus/docs/index.md`, `README.md` | DX writer | DOC-OV-01 | PR-01 |
| DOC-OV-03 | `docusaurus/docs/overview/why-typed-flags.md` | Merge | `docusaurus/docs/theory/type-safety-boundaries.md`, `docusaurus/docs/learn/type-safety.md` | Core maintainer | DOC-OV-01 | PR-01 |
| DOC-OV-04 | `docusaurus/docs/overview/first-success-map.md` | New | n/a | DX writer | DOC-OV-01 | PR-01 |
| DOC-OV-05 | `docusaurus/docs/overview/adoption-roadmap.md` | New | `docusaurus/docs/reference/migration-guide.md`, `docusaurus/docs/how-to-guides/publishing.md` | Tech lead + maintainer | DOC-OV-01 | PR-01 |

### 2) Quickstart

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-QS-01 | `docusaurus/docs/quickstart/index.md` | Merge | `docusaurus/docs/getting-started/index.md` | DX writer | DOC-OV-04 | PR-01 |
| DOC-QS-02 | `docusaurus/docs/quickstart/install.md` | Merge | `docusaurus/docs/getting-started/installation.md`, `docusaurus/docs/guides/install-and-setup.md` | DX writer | DOC-QS-01 | PR-01 |
| DOC-QS-03 | `docusaurus/docs/quickstart/define-first-flag.md` | Revise | `docusaurus/docs/getting-started/your-first-flag.md` | Core maintainer | DOC-QS-01 | PR-01 |
| DOC-QS-04 | `docusaurus/docs/quickstart/evaluate-in-app-code.md` | Merge | `docusaurus/docs/reference/api/feature-evaluation.md`, `docusaurus/docs/getting-started/your-first-flag.md` | Core maintainer | DOC-QS-03 | PR-01 |
| DOC-QS-05 | `docusaurus/docs/quickstart/add-deterministic-ramp-up.md` | Merge | `docusaurus/docs/how-to-guides/rolling-out-gradually.md`, `docusaurus/docs/guides/roll-out-gradually.md` | Core maintainer | DOC-QS-04 | PR-01 |
| DOC-QS-06 | `docusaurus/docs/quickstart/load-first-snapshot-safely.md` | Merge | `docusaurus/docs/how-to-guides/safe-remote-config.md`, `docusaurus/docs/reference/api/snapshot-loader.md`, `docusaurus/docs/reference/api/parse-result.md` | Serialization maintainer | DOC-QS-04 | PR-01 |
| DOC-QS-07 | `docusaurus/docs/quickstart/verify-end-to-end.md` | Merge | `docusaurus/docs/examples/golden-path.md`, `docusaurus/docs/how-to-guides/testing-features.md`, `docusaurus/docs/guides/test-features.md` | DX writer + QA | DOC-QS-06 | PR-01 |

### 3) Use Cases

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-UC-01 | `docusaurus/docs/use-cases/index.md` | New | n/a | DX writer | DOC-QS-01 | PR-02 |
| DOC-UC-02 | `docusaurus/docs/use-cases/progressive-rollout-and-experimentation.md` | Merge | `docusaurus/docs/how-to-guides/ab-testing.md`, `docusaurus/docs/how-to-guides/rolling-out-gradually.md` | Core maintainer | DOC-UC-01 | PR-02 |
| DOC-UC-03 | `docusaurus/docs/use-cases/safe-remote-configuration.md` | Merge | `docusaurus/docs/how-to-guides/safe-remote-config.md`, `docusaurus/docs/serialization/index.md` | Serialization maintainer | DOC-UC-01 | PR-02 |
| DOC-UC-04 | `docusaurus/docs/use-cases/multi-team-namespace-isolation.md` | Merge | `docusaurus/docs/how-to-guides/namespace-isolation.md`, `docusaurus/docs/theory/namespace-isolation.md` | Core maintainer | DOC-UC-01 | PR-02 |
| DOC-UC-05 | `docusaurus/docs/use-cases/migration-with-shadow-evaluation.md` | Merge | `docusaurus/docs/advanced/shadow-evaluation.md`, `docusaurus/docs/observability/shadow-evaluation.md`, `docusaurus/docs/theory/migration-and-shadowing.md` | Observability maintainer | DOC-UC-01 | PR-02 |
| DOC-UC-06 | `docusaurus/docs/use-cases/openfeature-provider-adoption.md` | New | `signatures/.claude/worktrees/modest-mccarthy/openfeature/src/main/kotlin/io/amichne/konditional/openfeature/KonditionalOpenFeatureProvider.kt.sig` | Integrations maintainer | DOC-UC-01 | PR-02 |
| DOC-UC-07 | `docusaurus/docs/use-cases/local-control-plane-http-server.md` | Revise | `docusaurus/docs/how-to-guides/local-http-server-container.md`, `README.md` | Runtime maintainer | DOC-UC-01 | PR-02 |

### 4) Guides (JTBD)

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-GD-01 | `docusaurus/docs/guides/index.md` | New | n/a | DX writer | DOC-UC-01 | PR-02 |
| DOC-GD-02 | `docusaurus/docs/guides/author-flags-and-rules.md` | Merge | `docusaurus/docs/core/rules.md`, `docusaurus/docs/rules-and-targeting/rule-composition.md`, `docusaurus/docs/core/best-practices.md` | Core maintainer | DOC-GD-01 | PR-02 |
| DOC-GD-03 | `docusaurus/docs/guides/roll-out-gradually.md` | Revise | `docusaurus/docs/how-to-guides/rolling-out-gradually.md`, `docusaurus/docs/guides/roll-out-gradually.md` | Core maintainer | DOC-GD-01 | PR-02 |
| DOC-GD-04 | `docusaurus/docs/guides/load-apply-snapshots-and-patches.md` | Merge | `docusaurus/docs/guides/load-remote-config.md`, `docusaurus/docs/serialization/persistence-format.md`, `docusaurus/docs/reference/api/snapshot-loader.md` | Serialization maintainer | DOC-GD-01 | PR-02 |
| DOC-GD-05 | `docusaurus/docs/guides/runtime-operations.md` | Merge | `docusaurus/docs/runtime/operations.md`, `docusaurus/docs/reference/api/namespace-operations.md` | Runtime maintainer | DOC-GD-01 | PR-02 |
| DOC-GD-06 | `docusaurus/docs/guides/testing-and-ci-guardrails.md` | Merge | `docusaurus/docs/how-to-guides/testing-features.md`, `docusaurus/docs/guides/test-features.md`, `docusaurus/docs/how-to-guides/debugging-determinism.md` | QA + core maintainer | DOC-GD-01 | PR-02 |
| DOC-GD-07 | `docusaurus/docs/guides/observability-and-telemetry.md` | Merge | `docusaurus/docs/observability/index.md`, `docusaurus/docs/opentelemetry/index.md`, `docusaurus/docs/api-reference/observability.md` | Observability maintainer | DOC-GD-01 | PR-02 |
| DOC-GD-08 | `docusaurus/docs/guides/release-and-publishing.md` | Merge | `docusaurus/docs/how-to-guides/publishing.md` | Maintainer | DOC-GD-01 | PR-02 |

### 5) Concepts

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-CP-01 | `docusaurus/docs/concepts/index.md` | New | n/a | DX writer | DOC-GD-01 | PR-03 |
| DOC-CP-02 | `docusaurus/docs/concepts/namespace-feature-flag-definition.md` | Merge | `docusaurus/docs/learn/core-primitives.md`, `docusaurus/docs/core/types.md` | Core maintainer | DOC-CP-01 | PR-03 |
| DOC-CP-03 | `docusaurus/docs/concepts/rule-specificity-and-precedence.md` | Merge | `docusaurus/docs/learn/evaluation-model.md`, `docusaurus/docs/core/rules.md` | Core maintainer | DOC-CP-01 | PR-03 |
| DOC-CP-04 | `docusaurus/docs/concepts/determinism-and-bucketing.md` | Merge | `docusaurus/docs/theory/determinism-proofs.md`, `docusaurus/docs/reference/api/ramp-up-bucketing.md` | Core maintainer | DOC-CP-01 | PR-03 |
| DOC-CP-05 | `docusaurus/docs/concepts/parse-boundary-and-typed-errors.md` | Merge | `docusaurus/docs/theory/parse-dont-validate.md`, `docusaurus/docs/reference/api/parse-result.md` | Serialization maintainer | DOC-CP-01 | PR-03 |
| DOC-CP-06 | `docusaurus/docs/concepts/atomic-snapshot-semantics.md` | Merge | `docusaurus/docs/theory/atomicity-guarantees.md`, `docusaurus/docs/production-operations/thread-safety.md` | Runtime maintainer | DOC-CP-01 | PR-03 |
| DOC-CP-07 | `docusaurus/docs/concepts/shadow-evaluation-semantics.md` | Merge | `docusaurus/docs/theory/migration-and-shadowing.md`, `docusaurus/docs/observability/shadow-evaluation.md` | Observability maintainer | DOC-CP-01 | PR-03 |
| DOC-CP-08 | `docusaurus/docs/concepts/axes-and-extension-predicates.md` | Merge | `docusaurus/docs/how-to-guides/custom-business-logic.md`, `docusaurus/docs/core/types.md` | Core maintainer | DOC-CP-01 | PR-03 |

### 6) Architecture (How it works)

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-AR-01 | `docusaurus/docs/architecture/index.md` | New | n/a | Architect + DX writer | DOC-CP-01 | PR-03 |
| DOC-AR-02 | `docusaurus/docs/architecture/system-overview.md` | Merge | `docusaurus/docs/core/index.md`, `docusaurus/docs/runtime/index.md`, `docusaurus/docs/serialization/index.md`, `docusaurus/docs/observability/index.md` | Architect | DOC-AR-01 | PR-03 |
| DOC-AR-03 | `docusaurus/docs/architecture/evaluation-pipeline.md` | New | `docusaurus/docs/learn/evaluation-model.md`, `signatures/.claude/worktrees/modest-mccarthy/konditional-core/src/main/kotlin/io/amichne/konditional/core/FlagDefinition.kt.sig` | Core maintainer | DOC-AR-01 | PR-03 |
| DOC-AR-04 | `docusaurus/docs/architecture/runtime-registry-design.md` | Merge | `docusaurus/docs/runtime/lifecycle.md`, `docusaurus/docs/production-operations/thread-safety.md`, `signatures/.claude/worktrees/modest-mccarthy/konditional-runtime/src/main/kotlin/io/amichne/konditional/core/registry/InMemoryNamespaceRegistry.kt.sig` | Runtime maintainer | DOC-AR-01 | PR-03 |
| DOC-AR-05 | `docusaurus/docs/architecture/serialization-materialization-pipeline.md` | Merge | `docusaurus/docs/serialization/persistence-format.md`, `docusaurus/docs/theory/parse-dont-validate.md` | Serialization maintainer | DOC-AR-01 | PR-03 |
| DOC-AR-06 | `docusaurus/docs/architecture/observability-plane.md` | Merge | `docusaurus/docs/observability/reference.md`, `docusaurus/docs/opentelemetry/reference.md`, `signatures/.claude/worktrees/modest-mccarthy/konditional-observability/src/main/kotlin/io/amichne/konditional/api/ShadowMismatch.kt.sig` | Observability maintainer | DOC-AR-01 | PR-03 |
| DOC-AR-07 | `docusaurus/docs/architecture/integration-adapters.md` | New | `docusaurus/docs/how-to-guides/local-http-server-container.md`, openfeature and otel signatures | Integrations maintainer | DOC-AR-01 | PR-03 |
| DOC-AR-08 | `docusaurus/docs/architecture/build-and-contracts-pipeline.md` | New | `docusaurus/docs/kontracts/index.md`, `docusaurus/docs/konditional-spec/index.md` | Maintainer | DOC-AR-01 | PR-03 |

### 7) Reference

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-RF-01 | `docusaurus/docs/reference/index.md` | New | n/a | DX writer | DOC-AR-01 | PR-04 |
| DOC-RF-02 | `docusaurus/docs/reference/api-index.md` | New | `docusaurus/docs/reference/api/*.md`, module references | API owner | DOC-RF-01 | PR-04 |
| DOC-RF-03 | `docusaurus/docs/reference/api/evaluation-apis.md` | Merge | `docusaurus/docs/reference/api/feature-evaluation.md` | Core maintainer | DOC-RF-02 | PR-04 |
| DOC-RF-04 | `docusaurus/docs/reference/api/runtime-operations-apis.md` | Merge | `docusaurus/docs/reference/api/namespace-operations.md`, `docusaurus/docs/runtime/operations.md` | Runtime maintainer | DOC-RF-02 | PR-04 |
| DOC-RF-05 | `docusaurus/docs/reference/api/integration-apis.md` | New | `docusaurus/docs/api-reference/observability.md`, openfeature/otel/http signatures | Integrations maintainer | DOC-RF-02 | PR-04 |
| DOC-RF-06 | `docusaurus/docs/reference/data-model-index.md` | New | `docusaurus/docs/serialization/persistence-format.md` | Serialization maintainer | DOC-RF-01 | PR-04 |
| DOC-RF-07 | `docusaurus/docs/reference/data/snapshot-json-shape.md` | Revise | `docusaurus/docs/serialization/persistence-format.md` | Serialization maintainer | DOC-RF-06 | PR-04 |
| DOC-RF-08 | `docusaurus/docs/reference/data/patch-json-shape.md` | New | `docusaurus/docs/serialization/persistence-format.md`, `signatures/.claude/worktrees/modest-mccarthy/konditional-serialization/src/main/kotlin/io/amichne/konditional/serialization/instance/ConfigurationPatch.kt.sig` | Serialization maintainer | DOC-RF-06 | PR-04 |
| DOC-RF-09 | `docusaurus/docs/reference/data/fixture-gallery.md` | New | existing docs snippets and tests fixtures | QA + serialization maintainer | DOC-RF-07 | PR-04 |
| DOC-RF-10 | `docusaurus/docs/reference/error-catalog.md` | New | `docusaurus/docs/reference/api/parse-result.md`, troubleshooting pages | Serialization maintainer | DOC-RF-01 | PR-04 |
| DOC-RF-11 | `docusaurus/docs/reference/compatibility-and-versioning.md` | Merge | `docusaurus/docs/reference/migration-guide.md`, `docusaurus/docs/core/reference.md` | Maintainer | DOC-RF-01 | PR-04 |
| DOC-RF-12 | `docusaurus/docs/reference/module-index.md` | New | module index pages under `core`, `runtime`, `serialization`, `observability`, `opentelemetry`, `konditional-spec`, `kontracts` | Architect | DOC-RF-01 | PR-04 |
| DOC-RF-13 | `docusaurus/docs/reference/cli-make-scripts.md` | New | `Makefile`, release script docs | Maintainer | DOC-RF-01 | PR-04 |

### 8) Trust (Security / Privacy / Reliability)

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-TR-01 | `docusaurus/docs/trust/index.md` | New | n/a | Security reviewer + DX writer | DOC-CP-01 | PR-05 |
| DOC-TR-02 | `docusaurus/docs/trust/security-model.md` | New | `docusaurus/docs/theory/type-safety-boundaries.md`, `docusaurus/docs/theory/parse-dont-validate.md` | Security reviewer | DOC-TR-01 | PR-05 |
| DOC-TR-03 | `docusaurus/docs/trust/privacy-and-data-handling.md` | New | telemetry + stableId docs | Security/privacy reviewer | DOC-TR-01 | PR-05 |
| DOC-TR-04 | `docusaurus/docs/trust/reliability-and-failure-modes.md` | Merge | `docusaurus/docs/production-operations/failure-modes.md`, `docusaurus/docs/production-operations/refresh-patterns.md` | SRE + runtime maintainer | DOC-TR-01 | PR-05 |
| DOC-TR-05 | `docusaurus/docs/trust/threat-model-and-hardening.md` | New | theory + ops docs | Security reviewer | DOC-TR-02 | PR-05 |
| DOC-TR-06 | `docusaurus/docs/trust/supply-chain-and-release-integrity.md` | New | publishing docs and signatures process docs | Maintainer | DOC-TR-01 | PR-05 |
| DOC-TR-07 | `docusaurus/docs/trust/invariants-proof-map.md` | Merge | `docusaurus/docs/theory/verified-synthesis.md`, theory pages | Architect + QA | DOC-TR-01 | PR-05 |

### 9) Troubleshooting & FAQ

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-TS-01 | `docusaurus/docs/troubleshooting/index.md` | Revise | `docusaurus/docs/troubleshooting/index.md` | Support writer | DOC-QS-06 | PR-05 |
| DOC-TS-02 | `docusaurus/docs/troubleshooting/symptom-index.md` | New | `docusaurus/docs/troubleshooting/index.md` | Support writer | DOC-TS-01 | PR-05 |
| DOC-TS-03 | `docusaurus/docs/troubleshooting/parsing-load-failures.md` | Merge | `docusaurus/docs/troubleshooting/parsing-issues.md`, `docusaurus/docs/reference/api/parse-result.md` | Serialization maintainer | DOC-TS-01 | PR-05 |
| DOC-TS-04 | `docusaurus/docs/troubleshooting/evaluation-mismatches.md` | New | `docusaurus/docs/troubleshooting/index.md`, `docusaurus/docs/guides/debug-evaluation.md` | Core maintainer | DOC-TS-01 | PR-05 |
| DOC-TS-05 | `docusaurus/docs/troubleshooting/bucketing-ramp-up-anomalies.md` | Merge | `docusaurus/docs/troubleshooting/bucketing-issues.md`, rollout guides | Core maintainer | DOC-TS-01 | PR-05 |
| DOC-TS-06 | `docusaurus/docs/troubleshooting/runtime-staleness-and-rollback.md` | New | `docusaurus/docs/troubleshooting/integration-issues.md`, runtime docs | Runtime maintainer | DOC-TS-01 | PR-05 |
| DOC-TS-07 | `docusaurus/docs/troubleshooting/integration-failures.md` | Revise | `docusaurus/docs/troubleshooting/integration-issues.md` | Integrations maintainer | DOC-TS-01 | PR-05 |
| DOC-TS-08 | `docusaurus/docs/troubleshooting/faq.md` | New | support tickets and repeated docs questions | Support writer | DOC-TS-01 | PR-05 |

### 10) Glossary

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-GL-01 | `docusaurus/docs/glossary/index.md` | New | `docusaurus/docs/reference/glossary.md` | Docs editor | DOC-CP-01 | PR-05 |
| DOC-GL-02 | `docusaurus/docs/glossary/core-domain-terms.md` | Split | `docusaurus/docs/reference/glossary.md` | Docs editor + core maintainer | DOC-GL-01 | PR-05 |
| DOC-GL-03 | `docusaurus/docs/glossary/runtime-operations-terms.md` | Split | `docusaurus/docs/reference/glossary.md` | Docs editor + runtime maintainer | DOC-GL-01 | PR-05 |
| DOC-GL-04 | `docusaurus/docs/glossary/serialization-schema-terms.md` | Split | `docusaurus/docs/reference/glossary.md` | Docs editor + serialization maintainer | DOC-GL-01 | PR-05 |
| DOC-GL-05 | `docusaurus/docs/glossary/observability-migration-terms.md` | Split | `docusaurus/docs/reference/glossary.md` | Docs editor + observability maintainer | DOC-GL-01 | PR-05 |

### 11) Cleanup, Redirects, and Legacy Paths

| ID | Target | Action | Source | Owner | Depends on | Batch |
|---|---|---|---|---|---|---|
| DOC-CL-01 | `docusaurus/sidebars.ts` | Revise | existing sidebar | DX writer + maintainer | DOC-OV-01,DOC-QS-01,DOC-UC-01,DOC-GD-01,DOC-CP-01,DOC-AR-01,DOC-RF-01,DOC-TR-01,DOC-TS-01,DOC-GL-01 | PR-06 |
| DOC-CL-02 | slug frontmatter redirects | New | all moved pages | Maintainer | DOC-CL-01 | PR-06 |
| DOC-CL-03 | remove duplicate paths in `guides` vs `how-to-guides` | Remove | `docusaurus/docs/guides/*.md`, `docusaurus/docs/how-to-guides/*.md` | Maintainer | DOC-GD-08 | PR-06 |
| DOC-CL-04 | archive/retire `design-theory/*` | Remove | `docusaurus/docs/design-theory/*.md` | Maintainer | DOC-CP-04,DOC-CP-05 | PR-06 |
| DOC-CL-05 | archive or merge `quick-start/what-is-konditional.md` | Remove | `docusaurus/docs/quick-start/what-is-konditional.md` | DX writer | DOC-OV-01 | PR-06 |
| DOC-CL-06 | reconcile `learn/*` and `core/*` overlap into Concepts/Architecture | Remove | `docusaurus/docs/learn/*.md`, `docusaurus/docs/core/*.md` | Core maintainer | DOC-CP-08,DOC-AR-02 | PR-06 |
| DOC-CL-07 | link validation and docs build hard gate | Revise | docs CI config and local workflow | Maintainer | DOC-CL-01 | PR-06 |

## PR Batching Plan

### PR-01: Entry Funnel (Overview + Quickstart)

- Includes: DOC-OV-01..05, DOC-QS-01..07
- Goal: first-success path is complete and coherent.
- Gate:
  - `make docs-build` passes
  - no broken local links in Overview/Quickstart
  - one maintainer and one docs reviewer approval

### PR-02: Outcome and Task Paths (Use Cases + Guides)

- Includes: DOC-UC-01..07, DOC-GD-01..08
- Goal: all top JTBD paths documented with verify/rollback.
- Gate:
  - each guide has Goal/Steps/Verify/Rollback/Common issues
  - no duplicate pages for same job

### PR-03: Mental Model Layer (Concepts + Architecture)

- Includes: DOC-CP-01..08, DOC-AR-01..08
- Goal: deep technical model aligns with invariants and signatures.
- Gate:
  - deterministic and boundary claims cross-linked to proof pages
  - architecture diagrams reviewed by module maintainers

### PR-04: Contract Lookup Layer (Reference)

- Includes: DOC-RF-01..13
- Goal: readers can find exact API/data contracts fast.
- Gate:
  - all reference pages include version/compatibility notes
  - parse and error semantics consistent with current APIs

### PR-05: Operational Confidence Layer (Trust + Troubleshooting + Glossary)

- Includes: DOC-TR-01..07, DOC-TS-01..08, DOC-GL-01..05
- Goal: clear reliability/security posture and incident diagnosis flow.
- Gate:
  - symptom-index routes cover top incident classes
  - glossary terms linked from concept and guide pages

### PR-06: Cleanup and Navigation Hardening

- Includes: DOC-CL-01..07
- Goal: remove duplication, finalize IA nav, preserve link continuity.
- Gate:
  - redirects in place for moved top pages
  - no orphan pages
  - `make docs-build` and link checks pass

## Acceptance Criteria by Issue

Apply this checklist to every migrated issue:

1. Page has required structural sections.
2. Claims are measurable or verifiable (code, config, diagram, or fixture).
3. Page contains explicit next-step links.
4. Terminology links to glossary term on first mention.
5. If task page: includes Verify and Rollback.
6. If reference page: includes versioning/compatibility notes.
7. If trust/concept page: explicitly states assumptions and failure modes.

## Execution Order (Strict)

1. PR-01
2. PR-02
3. PR-03
4. PR-04
5. PR-05
6. PR-06

## Notes for Implementation

- While migrating, keep old pages with temporary redirects until PR-06.
- Prefer preserving existing slugs for high-traffic pages when possible.
- Use short stubs only when needed to unblock dependent pages; replace stubs in the same batch.
- Keep branch and commit naming aligned with repository conventions.
