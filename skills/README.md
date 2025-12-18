# Konditional Skills for Claude

A collection of focused, single-purpose skills for working with the Konditional feature flag framework.

## Overview

These skills follow Claude's best practices: each skill is focused on a single task, 1-2 pages long, with clear structure and actionable guidance.

## Skills Included

### Meta-Skill

**[skill-creator.md](skill-creator.md)** - How to create domain-specific documentation skills
- Use when: Creating new skills or standardizing documentation
- Focus: Single-task skills with clear structure and validation

### Feature Development

**[konditional-feature-definition.md](konditional-feature-definition.md)** - Define Konditional features correctly
- Use when: Creating new flags, converting boolean flags to typed variants
- Focus: Type-safe feature definitions with compile-time guarantees

**[konditional-evaluation.md](konditional-evaluation.md)** - Explain evaluation semantics
- Use when: Debugging evaluation, explaining rule precedence, documenting bucketing
- Focus: Deterministic, non-null evaluation with specificity-based ordering

### Configuration & Operations

**[konditional-configuration.md](konditional-configuration.md)** - Handle configuration lifecycle
- Use when: Loading JSON config, applying patches, handling parse failures
- Focus: Runtime validation boundary with explicit error handling

### Documentation & Testing

**[konditional-documentation.md](konditional-documentation.md)** - Write precise documentation
- Use when: Creating guides, API docs, or technical explanations
- Focus: Precise, mechanically-grounded documentation with clear boundaries

**[konditional-testing.md](konditional-testing.md)** - Test features comprehensively
- Use when: Writing tests for rules, rollouts, distribution, or configuration
- Focus: Determinism, distribution, and lifecycle testing patterns

## Using These Skills

### For Feature Development

1. **Defining a new feature?** → Start with `konditional-feature-definition.md`
2. **Understanding evaluation?** → Read `konditional-evaluation.md`
3. **Writing tests?** → Use patterns from `konditional-testing.md`

### For Documentation

1. **Creating guides?** → Follow `konditional-documentation.md` conventions
2. **Explaining mechanisms?** → Use bounded claims and show mechanisms
3. **Providing examples?** → Make them runnable and realistic

### For Configuration

1. **Loading remote config?** → Use patterns from `konditional-configuration.md`
2. **Handling failures?** → Always use explicit `ParseResult` handling
3. **Updating config?** → Use atomic loads, handle last-known-good

## Key Principles Across All Skills

### 1. Precision Over Hedging

**Good**: "Type safety is enforced at compile-time for statically-defined features"
**Bad**: "Type safety is generally provided in most cases"

### 2. Explicit Boundaries

Always distinguish:
- **Compile-time guarantees** (property access, type propagation)
- **Runtime validation** (JSON parsing, configuration updates)
- **Not guaranteed** (semantic correctness of business rules)

### 3. Grounded in Mechanisms

Don't just state behavior—explain the mechanism:
- "Deterministic because SHA-256 hashing of `($salt:$flagKey:$id)`"
- "Specificity-based ordering: rules sorted by number of criteria"
- "Atomic updates via `Namespace.load()`"

### 4. Consistent Terminology

| Term | Use | Don't Use |
|------|-----|-----------|
| Feature | Typed configuration value | Flag, setting |
| FeatureContainer | Object holding features | Registry |
| Context | Runtime evaluation inputs | Environment |
| Rule | Criteria → value mapping | Condition |
| Specificity | Number of targeting criteria | Priority |
| Bucketing | SHA-256 user assignment | Hashing |

## Skill Structure Template

Each skill follows this structure:

```markdown
# Skill: [Task Name]

## Purpose
[One sentence: what this skill helps Claude do]

## When to Use This Skill
- [Specific trigger 1]
- [Specific trigger 2]

## Instructions
[Step-by-step process with examples]

## Examples
[Concrete, runnable examples]

## Validation Checklist
- [ ] [Check 1]
- [ ] [Check 2]
```

## Creating New Skills

Use `skill-creator.md` as your guide. Each skill should:

1. **Focus on one task** - If it does multiple things, split it
2. **Be actionable** - Provide clear steps, not abstract advice
3. **Include examples** - Show both success and common mistakes
4. **Add validation** - End with a checklist for quality

## Quick Reference

### Need to...

| Task | Skill | Key Sections |
|------|-------|--------------|
| Define a feature | konditional-feature-definition.md | Feature types, rule templates |
| Understand evaluation | konditional-evaluation.md | Evaluation flow, specificity |
| Load configuration | konditional-configuration.md | Parse boundary, lifecycle |
| Write documentation | konditional-documentation.md | Bounded claims, mechanisms |
| Write tests | konditional-testing.md | Determinism, distribution |
| Create a new skill | skill-creator.md | Structure template, validation |

### Common Patterns

**Feature definition**:
```kotlin
object Features : FeatureContainer<Namespace.Global>(Namespace.Global) {
    val feature by type<Context>(default = value) {
        rule(value) { criteria }
    }
}
```

**Evaluation**:
```kotlin
val result: Type = Features.feature.evaluate(context)
```

**Configuration loading**:
```kotlin
when (val result = SnapshotSerializer.fromJson(json)) {
    is ParseResult.Success -> namespace.load(result.value)
    is ParseResult.Failure -> handleError(result.error)
}
```

## Konditional Resources

- **Repository**: [Konditional documentation in this project](../)
- **Core concepts**: See `03-core-concepts.md`
- **Getting started**: See `01-getting-started.md`
- **Why Konditional**: See `09-why-konditional.md`

## Contributing to These Skills

When updating or adding skills:

1. Keep each skill focused on one task
2. Maintain 1-2 page length maximum
3. Include concrete, runnable examples
4. Add validation checklists
5. Use consistent terminology
6. Follow the standard structure template

## License

These skills are part of the Konditional project and follow the same license.
