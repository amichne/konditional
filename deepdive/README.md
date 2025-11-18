# Konditional Deep Dive Series

## Overview

This deep dive series provides a comprehensive engineering exploration of Konditional, the type-safe feature flag framework for Kotlin. Unlike reference documentation that focuses on "what," this series emphasizes "how" and "why" - revealing the architecture, implementation details, and design decisions behind the framework.

## Who This Is For

- **Engineers evaluating Konditional** for their projects
- **Contributors** who want to understand the codebase
- **Architects** making integration and design decisions
- **Curious developers** interested in type-safe API design

## What You'll Learn

### The What
- Core components and their responsibilities
- Data structures and algorithms used
- API surface and usage patterns

### The How
- Evaluation engine implementation
- Bucketing algorithm mechanics
- Concurrency model details
- Serialization process

### The Why
- Design decisions and rationale
- Trade-offs and alternatives considered
- Performance characteristics
- Type safety enforcement mechanisms

## The Complete Series

### Part 1: Foundations

**[01. Introduction](01-introduction.md)**
- Overview of the deep dive series
- Core question: "What if runtime failures were impossible?"
- Learning path and prerequisites
- How to use this guide

**[02. Fundamentals](02-fundamentals.md)**
- The three core concepts: Features, Contexts, Namespaces
- How they relate and work together
- Complete flow from definition to evaluation
- Standard targeting dimensions

**[03. Type System](03-type-system.md)**
- The four type parameters (S, T, C, M) explained
- Property delegation deep dive
- Type constraints and bounds
- Variance and type safety
- Phantom types and their purpose

### Part 2: Implementation Details

**[04. Evaluation Engine](04-evaluation-engine.md)**
- FlagDefinition.evaluate() step-by-step
- Rule sorting by specificity
- Inactive flag short-circuiting
- ConditionalValue matching
- Complete evaluation flow

**[05. Rules & Specificity](05-rules-specificity.md)**
- BaseEvaluable implementation
- Specificity calculation algorithm
- Why more specific rules win
- Rule composition (base + extension)
- Extension evaluables and custom specificity

**[06. Bucketing Algorithm](06-bucketing-algorithm.md)**
- SHA-256 hashing implementation
- The stableBucket() function dissected
- Why SHA-256 over simpler hashing
- Bucket calculation (mod 10,000)
- Rollout threshold comparison
- Salt's role in redistribution

**[07. Concurrency Model](07-concurrency-model.md)**
- Immutable data structures throughout
- AtomicReference in NamespaceRegistry
- Lock-free reads (10-25x faster than locks)
- Atomic configuration updates
- Why no locks are needed
- Thread safety guarantees

**[08. Serialization](08-serialization.md)**
- SnapshotSerializer implementation
- ParseResult and parse-don't-validate pattern
- Moshi adapters for version ranges and flag values
- JSON format structure
- Patch operations for incremental updates
- Type-safe deserialization

### Part 3: Advanced Topics

**[09. Advanced Patterns](09-advanced-patterns.md)**
- Custom context patterns for business logic
- Reusable evaluable libraries
- Multi-namespace architectures
- Remote configuration patterns
- Testing strategies
- Migration from legacy systems
- Feature flag lifecycle management

**[10. Architecture Decisions](10-architecture-decisions.md)**
- Why property delegation over builder pattern
- Why sealed interfaces over open classes
- Why immutability is central to the design
- Why SHA-256 over simpler hashing
- Why atomic references over locks
- Why parse-don't-validate
- Trade-offs and alternatives considered

## Reading Paths

### Path 1: Complete Deep Dive (4-5 hours)

**For**: Contributors, architects making decisions

**Approach**: Read all chapters in order
1. Introduction → Fundamentals → Type System
2. Evaluation Engine → Rules & Specificity → Bucketing Algorithm
3. Concurrency Model → Serialization
4. Advanced Patterns → Architecture Decisions

**Outcome**: Complete understanding of implementation and design

### Path 2: Evaluation Focus (90 minutes)

**For**: Engineers evaluating Konditional for adoption

**Approach**: Read selected chapters
1. [Introduction](01-introduction.md) - Understand the goals
2. [Fundamentals](02-fundamentals.md) - Core concepts
3. [Type System](03-type-system.md) - Compile-time guarantees
4. [Evaluation Engine](04-evaluation-engine.md) - How flags work
5. [Architecture Decisions](10-architecture-decisions.md) - Design rationale

**Outcome**: Sufficient understanding to make adoption decision

### Path 3: Implementation Focus (2-3 hours)

**For**: Contributors working on the codebase

**Approach**: Skim foundations, deep dive implementation
1. [Fundamentals](02-fundamentals.md) - Quick refresher
2. [Type System](03-type-system.md) - Type parameter understanding
3. [Evaluation Engine](04-evaluation-engine.md) - Core algorithm
4. [Rules & Specificity](05-rules-specificity.md) - Rule ordering
5. [Bucketing Algorithm](06-bucketing-algorithm.md) - Rollout logic
6. [Concurrency Model](07-concurrency-model.md) - Thread safety
7. [Serialization](08-serialization.md) - JSON conversion

**Outcome**: Ready to contribute to codebase

### Path 4: Usage Focus (60 minutes)

**For**: Application developers using Konditional

**Approach**: Practical usage patterns
1. [Fundamentals](02-fundamentals.md) - How to define features
2. [Type System](03-type-system.md) (skim) - Understand types
3. [Advanced Patterns](09-advanced-patterns.md) - Real-world usage
   - Custom contexts
   - Reusable evaluables
   - Testing strategies
   - Remote configuration

**Outcome**: Effective usage in applications

## Prerequisites

### Required Knowledge

- **Kotlin basics**: Syntax, classes, functions, data classes
- **Generics**: Type parameters, constraints, variance
- **Feature flags**: Basic understanding of what they are and why they're used

### Helpful But Not Required

- **Kotlin property delegation**: How `by` keyword works
- **Cryptographic hashing**: SHA-256 basics
- **Concurrency**: Thread safety, atomic operations
- **JSON serialization**: How JSON maps to objects
- **Functional programming**: Immutability, parse-don't-validate

## Key Concepts Summary

### Core Abstractions

```
Feature       - Type-safe identifier for a flag
Context       - Runtime state (locale, platform, version, user ID)
Namespace     - Isolation boundary for features
Rule          - Targeting conditions (who sees what)
FlagDefinition - Complete flag configuration
```

### Type Parameters

```kotlin
Feature<S, T, C, M>
  S: EncodableValue<T>  - Serialization wrapper
  T: Any                - Actual value type (Boolean, String, Int, Double)
  C: Context            - Evaluation context type
  M: Namespace          - Feature namespace
```

### Key Algorithms

**Evaluation**:
1. Check if active
2. Iterate rules by specificity (descending)
3. Check if rule matches context
4. Check if user in rollout bucket (SHA-256 based)
5. Return matching value or default

**Specificity**:
```
specificity = count of non-empty constraints
            = (locales ? 1 : 0) + (platforms ? 1 : 0) + (versions ? 1 : 0)
              + extension.specificity()
```

**Bucketing**:
```
bucket = SHA256("salt:flagKey:userId")
           .take(4 bytes)
           .asUInt32()
           .mod(10_000)

inRollout = bucket < (rolloutPercentage * 100)
```

## Code Examples

Each chapter includes:

- **Real implementation code** from the Konditional source
- **Simplified examples** to illustrate concepts
- **Anti-patterns** showing what not to do
- **Complete working examples** you can adapt

## Performance Characteristics

| Operation | Time Complexity | Typical Duration |
|-----------|----------------|------------------|
| Flag evaluation (no rollout) | O(1) to O(n) rules | ~100-1000 ns |
| Flag evaluation (with rollout) | O(1) to O(n) rules | ~1-2 μs |
| Configuration update | O(n) flags | ~1 ms |
| SHA-256 hash | O(m) input bytes | ~1 μs |
| Concurrent read | Lock-free | ~1-2 ns |
| JSON serialization | O(n) flags | ~1-10 ms |
| JSON deserialization | O(n) flags | ~5-50 ms |

## Architecture Principles

Konditional embodies these principles:

1. **Type Safety**: Leverage Kotlin's type system maximally
2. **Immutability**: Functional programming principles throughout
3. **Explicitness**: Prefer explicit over implicit (errors as values)
4. **Performance**: Lock-free concurrency, efficient algorithms
5. **Ergonomics**: Concise API, sensible defaults

**Core philosophy**: "If it compiles, it works."

## Reference Documentation

This deep dive complements the reference documentation:

- **[Main README](../README.md)** - Quick start and overview
- **[Features.md](../docs/Features.md)** - Feature definition API
- **[Context.md](../docs/Context.md)** - Context creation and usage
- **[Evaluation.md](../docs/Evaluation.md)** - Evaluation API
- **[Rules.md](../docs/Rules.md)** - Rule syntax and semantics
- **[Serialization.md](../docs/Serialization.md)** - JSON format and API

**Difference**: Reference docs explain **what** you can do; deep dive explains **how** and **why** it works.

## Contributing to This Guide

Found something unclear? See an error? Want to add an example?

This guide is maintained alongside the codebase. Contributions welcome:

1. **Issues**: Report errors or unclear sections
2. **Pull requests**: Fix typos, improve explanations, add examples
3. **Questions**: Ask in discussions (might become new sections)

Repository: [github.com/amichne/konditional](https://github.com/amichne/konditional)

## Questions and Feedback

After reading, you should be able to answer:

- How does Konditional enforce type safety?
- Why can't flag evaluation return null?
- How does rollout bucketing work?
- Why are reads so fast (lock-free)?
- How is platform independence achieved?
- What makes rules automatically prioritize?

If you can't answer these after reading relevant chapters, please let us know - the guide needs improvement.

## Next Steps

Ready to dive in? Start with:

**[Chapter 01: Introduction →](01-introduction.md)**

Or jump to a specific topic using the table of contents above.

---

*Happy learning, and may your feature flags be forever type-safe!*
