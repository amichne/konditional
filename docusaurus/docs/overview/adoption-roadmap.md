# Adoption roadmap

Use this roadmap to move from a local pilot to production operation with clear
gates.

## What you will achieve

You will:

- define phased adoption work with measurable gates;
- assign responsibilities across development and operations roles;
- reduce migration risk with explicit verification at each phase.

## Prerequisites

You need ownership for application code, runtime operations, and observability.

## Main content

Treat adoption as four phases with hard verification gates.

## Phase 1: Pilot namespace

Scope:

- one namespace;
- one boolean feature;
- one enum or integer feature.

Required checks:

- deterministic evaluation for fixed context;
- compile-time type propagation at call sites;
- operational fallback to defaults.

## Phase 2: Controlled rollout

Scope:

- add ramp-up targeting to one production-adjacent path.

Required checks:

- stable IDs are sourced from durable identity;
- rollout metrics are collected per feature and platform;
- rollback procedure is documented and tested.

## Phase 3: Remote configuration boundary

Scope:

- ingest one remote snapshot through `NamespaceSnapshotLoader`.

Required checks:

- parse failures are logged and alerted;
- failed loads do not mutate active runtime state;
- last-known-good behavior is validated in tests.

## Phase 4: Operational hardening

Scope:

- add troubleshooting runbooks and trust documentation.

Required checks:

- incident diagnosis paths are documented by symptom;
- compatibility and versioning references are up to date;
- release process includes docs and link validation gates.

## Success metrics

Track these metrics through adoption:

- time to first successful flag (target: under 15 minutes);
- remote config load success/failure rates;
- incident count from configuration errors;
- percentage of guides with verify and rollback sections.

## Next steps

- [Quickstart](/quickstart/)
- [How-to guides](/how-to-guides/rolling-out-gradually)
- [Troubleshooting](/troubleshooting/)
