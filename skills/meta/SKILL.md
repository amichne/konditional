---
name: skill-creator
description: Create focused, single-purpose skills for technical projects with strong type systems and compile-time guarantees
---

# Skill Creator

## Instructions

### Identify Single Task
Each skill should do **one thing well**:
- What specific problem does this solve?
- Can this be broken into smaller tasks?
- Is this about writing, analyzing, or generating code?

**Good scope**: "Explain how rule precedence works in feature flags"
**Too broad**: "Document the entire feature flag system"

### Standard Structure
```markdown
---
name: your-skill-name
description: Brief description of what this Skill does and when to use it
---

# Your Skill Name

## Instructions
[Step-by-step process with subsections as needed]

## Examples
### Example 1: [Scenario]
[Code or output example]

### Example 2: [Common Mistake]
**Wrong**: [Incorrect approach]
**Right**: [Correct approach]
```

### Make It Actionable
Provide clear steps, not abstract advice:

**Bad**: "Consider the type system when documenting"
**Good**: "List each type's name, purpose, and relationships in a table"

### Include Concrete Examples
Show both:
- **Successful output** with explanation
- **Common mistakes** with fixes

## Examples

### Well-Scoped Skill
```markdown
---
name: rule-precedence
description: Explain how feature flag systems determine which rule applies when multiple rules could match
---

# Feature Flag Rule Precedence

## Instructions
1. State the precedence mechanism (e.g., "specificity-based sorting")
2. Define specificity calculation with code example
3. Show evaluation flow as numbered steps
4. Provide 2-3 examples showing different precedence scenarios
5. Document common mistakes about precedence

## Examples
[Concrete examples here]
```

**Why this works**: Single task (precedence), clear steps, concrete examples.

### Too Broad (Split Required)
```markdown
---
name: feature-flags
description: Document everything about feature flags
---

# Feature Flag System

## Instructions
- Document all aspects of feature flags
```

**Why this fails**: Too many tasks. Split into:
- `feature-definition`: How to define features
- `evaluation`: How features are evaluated
- `configuration`: How to load configuration
- `testing`: How to test features
