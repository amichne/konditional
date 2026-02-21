# Documentation update template

Use this template for documentation pages that need claim-level evidence links
and deterministic update tracking.

```markdown
# <document title>

## Document intent

This page helps <audience> achieve <outcome> by reducing <pain> and improving
<gain>.

## Why this matters now

Describe urgency, risk, or opportunity in concrete terms.

## Before and after

### Before

Describe the current friction, ambiguity, or operational cost.

### After

Describe the desired state and measurable impact.

## Guidance

1. <step or recommendation>
2. <step or recommendation>
3. <step or recommendation>

## Evidence links

- claim_id: <claim id>
  statement: <claim statement>
  links:
    - kind: type
      signature: <fqcn>
      status: linked
    - kind: method
      signature: <fqcn>#<normalized method signature>
      status: linked

## Adoption signals

- Primary KPI:
- Secondary KPI:
- Leading indicator:

## Open questions

- <unresolved signature gap or content decision>
```

Keep evidence links synchronized with the chosen link-map JSON file.
