# Konditional LLM-First Documentation Index

**Purpose**: This directory contains machine-optimized documentation for LLM agents working on the Konditional codebase. Optimized for information density and technical precision over human readability.

**Last Updated**: 2025-12-06
**Codebase Version**: Post type-parameter-simplification (2-param architecture)

## Documentation Structure

### Core Architecture
- `01-ARCHITECTURE-OVERVIEW.md` - High-level system design, components, data flow
- `02-TYPE-SYSTEM.md` - Complete type parameter architecture, relationships, constraints
- `03-API-SURFACE.md` - Public API contracts, signatures, usage patterns

### Implementation Details
- `04-FEATURE-SYSTEM.md` - Feature flags, definitions, evaluation, lifecycle
- `05-DSL-BUILDERS.md` - DSL implementation, delegation, scoping
- `06-REGISTRY-CONFIGURATION.md` - Configuration management, namespaces, registries

### Patterns & Idioms
- `07-DESIGN-PATTERNS.md` - Architectural patterns, Kotlin idioms, conventions
- `08-TYPE-SAFETY-GUARANTEES.md` - Compile-time guarantees, phantom types, evidence

### Evolution & Context
- `09-RECENT-CHANGES.md` - Recent architectural changes, migration paths
- `10-SERIALIZATION.md` - JSON serialization, internal encoding, schema

### Cross-Cutting Concerns
- `11-THREAD-SAFETY.md` - Concurrency model, atomic operations, lock-free reads
- `12-EVALUATION-RULES.md` - Rule matching, specificity, rollout algorithms

## Quick Reference

**Primary Entry Points**:
- `FeatureContainer` - Main user-facing API for defining flags
- `Feature<T, M>` - Core abstraction (2 type parameters)
- `FlagDefinition<T, C, M>` - Internal evaluation model (3 type parameters)

**Key Architectural Decisions**:
1. Parse-don't-validate: Use sealed types for errors
2. Type-bound namespaces: Compile-time isolation between teams
3. Property delegation: Ergonomic DSL with automatic registration
4. Phantom types: Namespace type parameter for isolation without runtime cost
5. Context polymorphism: Evaluation accepts any Context subtype

**Recent Major Changes**:
- 2025-12-06: Type parameter reduction from 4 to 2 (Feature<T, M>)
- EncodableValue moved to internal implementation detail
- Context type parameter made polymorphic at evaluation time

## Reading Guide for LLMs

**For Architecture Understanding**: Read 01, 02, 03 in order
**For Implementation**: Read 04, 05, 06 in order
**For Pattern Recognition**: Read 07, 08
**For Recent Context**: Read 09 first
**For Specific Concerns**: Use index to jump to relevant section

## Conventions

- Type parameters documented with constraints: `T : Any`
- File paths use absolute notation: `src/main/kotlin/...`
- Line numbers included for code references: `Feature.kt:31`
- Cross-references use format: `[see 02-TYPE-SYSTEM.md#feature-interface]`
- Breaking changes marked with ⚠️
- Internal APIs marked with 🔒
- Public APIs marked with 📖
