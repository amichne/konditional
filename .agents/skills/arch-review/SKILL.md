---
name: arch-review
description: Provide in-depth technical review of Kotlin codebases and OpenAPI specifications with focus on system architecture, API design, type safety, maintainability, scalability, and enterprise concerns. Use when the user requests architectural review, API surface analysis, code critique, design assessment, or asks to identify weaknesses, complexity reduction opportunities, or improvement areas. Trigger on requests like "review my architecture", "assess my API design", "find weaknesses in this code", "critique this implementation", or "analyze this for enterprise readiness". Particularly valuable for library/framework design, public API review, and production system architecture validation.
---

# Critical Architecture Review

Provide rigorous, critical analysis of Kotlin code and OpenAPI specifications to surface architectural weaknesses, design flaws, and improvement opportunities. Focus on enterprise-grade concerns: type safety, API design coherence, observability integration, scalability, and maintainability.

## Review Philosophy

**Be critical, not dismissive.** The goal is to identify genuine weaknesses and complexity that should be addressed, not to nitpick style preferences. Assume the engineer is skilled; look for subtle issues they might have missed.

**Prioritize impact over volume.** Focus on architectural decisions that affect maintainability, safety, and scalability. A single architectural flaw is more important than dozens of minor style issues.

**Suggest alternatives, not just problems.** When identifying weaknesses, provide concrete paths to improvement with code examples or specific patterns.

## Review Framework

### 1. Initial Assessment

Understand the context:
- What is being reviewed? (Library, service, API spec, architectural design)
- What are the stated goals? (Type safety, flexibility, simplicity, etc.)
- What constraints exist? (Backwards compatibility, team size, stateless architecture)

### 2. Review Dimensions

Evaluate across these critical dimensions (prioritize based on context):

#### Type Safety & Compile-Time Guarantees
- Are illegal states unrepresentable?
- Are errors internal or exposed to consumers?
- Can type system prevent misuse?
- Are there unsafe casts, nullable leaks, or `Any` abuse?

**Reference:** `references/type-safety-patterns.md` for patterns and anti-patterns.

#### API Surface Design
- Is the API minimal and coherent?
- Are abstractions at the right level?
- Is the naming consistent and intuitive?
- Does it follow progressive disclosure (simple things simple, complex things possible)?
- Are there temporal coupling or boolean traps?

**Reference:** `references/api-design-principles.md` for design principles and patterns.

#### Enterprise Concerns
- Are there observability hooks (OpenTelemetry integration points)?
- Is the design compatible with stateless architecture?
- Are backwards compatibility considerations addressed?
- Is there configuration flexibility without rigidity?
- Can it scale horizontally?

**Reference:** `references/enterprise-patterns.md` for observability, scalability, and compatibility patterns.

#### Maintainability & Complexity
- Is complexity justified by functionality?
- Are there simpler alternatives that achieve the same goals?
- Is the code testable?
- Are abstractions helping or hurting comprehension?
- Would this scale to 100 engineers?

#### Kotlin-Specific Issues
- Coroutine misuse or concurrency issues?
- Collection exposure or mutability leaks?
- Missing inline for performance-critical code?
- Reflection where compile-time solutions exist?

**Reference:** `references/kotlin-antipatterns.md` for common pitfalls.

#### OpenAPI Quality (if applicable)
- Type safety in schemas (avoid oneOf/anyOf where possible)?
- Validation constraints present?
- Error responses structured?
- Backwards compatibility considerations?
- Versioning strategy clear?

**Reference:** `references/openapi-guidelines.md` for spec best practices.

### 3. Severity Classification

Categorize findings by impact:

**CRITICAL**: Fundamental design flaws that will cause serious problems in production
- Type safety violations that could cause runtime errors
- Architectural decisions that prevent scaling
- Missing observability in production systems
- Concurrency issues in shared state

**HIGH**: Significant issues that increase maintenance burden or risk
- API design that couples consumers to internals
- Missing backwards compatibility paths
- Complex abstractions without clear benefit
- Testability problems

**MEDIUM**: Issues that affect code quality but have workarounds
- Inconsistent naming or patterns
- Suboptimal performance in non-critical paths
- Missing documentation for complex behavior
- Minor type safety improvements

**LOW**: Nice-to-haves and polish
- Style inconsistencies
- Additional convenience methods
- Documentation improvements

### 4. Review Output Format

Structure feedback as:

```markdown
## Executive Summary
[2-3 sentences on overall assessment and key themes]

## Critical Findings

### [Finding Name] - [SEVERITY]
**Issue:** [Specific problem identified]
**Impact:** [Why this matters for the codebase/team/users]
**Recommendation:** [Concrete suggestion with code example if relevant]

[Repeat for each finding]

## Complexity Reduction Opportunities

[Specific areas where simplification would maintain or increase functionality]

## Strengths

[Acknowledge what's done well - reinforces good patterns]

## Next Steps

[Prioritized recommendations for addressing findings]
```

### 5. Deep Dive When Needed

If analysis requires understanding specific patterns or anti-patterns in detail, load relevant reference files:
- `view references/type-safety-patterns.md` for type system deep dives
- `view references/api-design-principles.md` for API design evaluation
- `view references/enterprise-patterns.md` for observability/scalability assessment
- `view references/kotlin-antipatterns.md` for identifying code smells
- `view references/openapi-guidelines.md` for spec review

## Review Workflow

1. **Parse the request** - Understand what's being reviewed and stated constraints
2. **Load relevant references** - Based on review type, load appropriate reference files
3. **Conduct systematic review** - Evaluate across all relevant dimensions
4. **Identify patterns** - Look for repeated issues or architectural themes
5. **Prioritize findings** - Focus on high-impact issues
6. **Provide actionable recommendations** - Include concrete alternatives
7. **Suggest next steps** - Prioritized action items

## Context-Specific Guidance

### Library/Framework Review
Focus on:
- Public API surface (minimize, make coherent)
- Type safety guarantees for consumers
- Observability integration hooks
- Backwards compatibility strategy
- Documentation completeness

### Service Architecture Review
Focus on:
- Stateless design compatibility
- Horizontal scaling concerns
- Observability instrumentation
- Error handling and resilience
- Performance characteristics

### OpenAPI Spec Review
Focus on:
- Schema type safety
- Validation constraints
- Error response structure
- Versioning strategy
- Backwards compatibility

## Key Principles

- **Assume competence** - Look for subtle issues, not obvious mistakes
- **Focus on architecture** - System-level concerns over local code quality
- **Be specific** - Vague feedback isn't actionable
- **Provide alternatives** - Don't just identify problems
- **Respect constraints** - Work within stated requirements (type safety, backwards compatibility, etc.)
- **Prioritize ruthlessly** - Not all issues are equally important
