---
name: kotlin-architect
description: "Produce production-grade Kotlin designs and implementations with strict idiomatic standards, type-safe APIs, coroutine correctness, and full test coverage. Use when writing, reviewing, or refactoring Kotlin code where architecture quality, extensibility, and compile-time safety matter."
---

# Kotlin Architect

## Overview

Apply this skill to generate Kotlin solutions that prioritize long-term architectural quality, strong type-system guarantees, and complete verification through executable tests.

## Operating Rules

- Prefer expression bodies over statement-heavy control flow whenever readability is preserved.
- Avoid multiple return statements unless required for clarity or safety.
- Prefer immutable data, data classes, and functional-style transformations over imperative mutation.
- Avoid Java-isms when Kotlin-native constructs exist (object declarations, delegates, sealed interfaces, extension functions, value classes).
- Use scope functions (`let`, `run`, `apply`, `also`) only when they improve readability.

## Type-System-First Design

- Design APIs for consumers, not one-off usage.
- Enforce correctness at compile time via:
  - generic constraints
  - variance (`in`/`out`)
  - inline + reified type parameters when runtime type checks are required
  - value classes for domain primitives
  - sealed interfaces/classes for finite states
- Prevent misuse through explicit types and constrained constructors.
- Call out type-erasure limits and nullability boundaries explicitly.

## Coroutines and Concurrency Requirements

When coroutines are used:

- Define and propagate `CoroutineContext` intentionally.
- Preserve structured concurrency (`coroutineScope`/`supervisorScope`) instead of detached jobs.
- Define exception propagation behavior explicitly.
- Identify shared mutable state and eliminate race/deadlock risk via confinement, immutable snapshots, or synchronization primitives.

## Production-Readiness Requirements

- Include KDoc for non-obvious generic bounds, reflection, or advanced type behavior.
- Handle edge cases explicitly (nullability, empty inputs, invalid state transitions, type erasure interactions).
- Include a simplified DI wiring example when dependencies or replaceable implementations exist.
- Prefer breaking changes over preserving weak abstractions when long-term extensibility improves.

## Ambiguity Handling

Never assume business logic that is not specified.

When requirements are ambiguous:

1. State at least two viable interpretations.
2. Describe trade-offs (safety, extensibility, complexity, runtime cost).
3. Choose one approach and justify it.

## Completion Gate (Mandatory)

Do not declare completion until all are true:

1. Tests are added for all new functionality (without redundant coverage).
2. All claims about behavior are validated by executing code/tests.
3. Test and verification commands are reported with outcomes.
