# Interactive Documentation Components - Implementation Guide

## Overview

I've created three React components to make Konditional's documentation more interactive and surface IDE benefits. These components help communicate:

1. **What types are available in different scopes**
2. **What IntelliJ shows (autocomplete, type hints)**
3. **How Konditional's type safety helps developers**

---

## Components Created

### 1. **IDEInsight** - Show What IntelliJ Sees

**Location:** `docusaurus/src/components/IDEInsight.tsx`

**Purpose:** Displays code with tabs showing:
- The code itself
- Type hints overlaid on the code
- Autocomplete suggestions IntelliJ would show
- Inferred types

**Use cases:**
- Demonstrating type inference
- Showing what autocomplete reveals
- Highlighting compile-time safety

**Example:**
```jsx
<IDEInsight
  title="Feature Definition"
  inferredTypes={{
    "darkMode": "Feature<Boolean, Context, Namespace>",
    "enabled": "Boolean"
  }}
  autocomplete={[
    { text: "boolean<Context>()", type: "method", description: "Creates boolean feature" },
    { text: "string<Context>()", type: "method", description: "Creates string feature" }
  ]}
>
```kotlin
val darkMode by boolean<Context>(default = false)
```
</IDEInsight>
```

**Key features:**
- Tabbed interface (Code / Type Hints / Autocomplete)
- Shows type inference visually
- Mobile responsive
- Light/dark mode support

---

### 2. **ScopeExplorer** - What's Available in This Scope

**Location:** `docusaurus/src/components/ScopeExplorer.tsx`

**Purpose:** Shows what methods/properties/DSL functions are available in a given scope.

**Use cases:**
- Documenting DSL scopes (what's inside `{ }` blocks)
- Showing available targeting methods
- Filtering by type (DSL / properties / methods)

**Example:**
```jsx
<ScopeExplorer
  title="Available DSL Methods"
  context="Feature<Boolean, Context> { ... }"
  items={[
    {
      name: "enable",
      type: "dsl",
      signature: "enable(block: RuleBuilder<Context>.() -> Unit)",
      description: "Sugar for rule(true)"
    },
    {
      name: "platforms",
      type: "method",
      signature: "platforms(vararg platforms: Platform)",
      description: "Matches platform criteria"
    }
  ]}
/>
```

**Key features:**
- Filterable by item type
- Shows signatures and descriptions
- Marks inherited items
- Scrollable list for many items

---

### 3. **TypeAnnotation** - Inline Type Tooltips

**Location:** `docusaurus/src/components/TypeAnnotation.tsx`

**Purpose:** Wraps inline code tokens with hover tooltips showing type information.

**Use cases:**
- Highlighting specific inferred types in prose
- Showing what IntelliJ knows about a variable
- Emphasizing non-null guarantees

**Example:**
```jsx
The <TypeAnnotation type="Boolean" description="Never null">enabled</TypeAnnotation>
variable is guaranteed non-null.
```

**Key features:**
- Dotted underline indicates hoverability
- Tooltip with type and optional description
- Minimal visual footprint
- Positioned above text

---

## Implementation Strategy

### Phase 1: High-Impact Pages (Recommended Start)

Add interactive components to these pages first for maximum impact:

1. **Getting Started / Your First Flag**
   - Use `IDEInsight` to show type inference in the first example
   - Use `ScopeExplorer` to show what's available in feature blocks

2. **Core / Rule DSL**
   - Use `ScopeExplorer` extensively to document each DSL scope
   - Show what's available inside `rule { }`, `rampUp { }`, `versions { }`, etc.

3. **Core Concepts**
   - Use `TypeAnnotation` to highlight key type information inline
   - Use `IDEInsight` to show autocomplete for feature types

4. **Advanced Recipes**
   - Use all three components to show complex patterns
   - Demonstrate how type safety prevents errors

### Phase 2: Systematic Enhancement

Roll out to remaining pages:
- Runtime module docs
- Serialization docs
- Kontracts schema DSL
- OpenTelemetry integration

### Phase 3: Specialized Use Cases

Create custom variations for:
- Side-by-side comparisons (with/without Konditional)
- Error prevention examples (what won't compile)
- Migration guides (before/after)

---

## Usage Patterns

### Pattern 1: Feature Introduction

**Goal:** Show what developers get when defining a feature

```jsx
<IDEInsight
  title="Type-Safe Feature Definition"
  inferredTypes={{
    "darkMode": "Feature<Boolean, Context, Namespace>"
  }}
>
  {/* code block */}
</IDEInsight>

<ScopeExplorer
  title="What IntelliJ Suggests"
  context="Feature<Boolean, Context> { ... }"
  items={[/* DSL methods */]}
/>
```

### Pattern 2: Scoped DSL Documentation

**Goal:** Document what's available in each nested scope

```jsx
## Inside rampUp { } Block

<ScopeExplorer
  title="RampUp DSL"
  context="rampUp { ... }"
  items={[
    { name: "percentage", type: "property", signature: "var percentage: Double" },
    { name: "salt", type: "property", signature: "var salt: String?" }
  ]}
/>
```

### Pattern 3: Inline Type Emphasis

**Goal:** Highlight type safety in prose

```jsx
The <TypeAnnotation type="Boolean">evaluate()</TypeAnnotation> method
returns a non-null Boolean, guaranteed by the type system.
```

### Pattern 4: Complex Type Interactions

**Goal:** Show how generic types propagate

```jsx
<IDEInsight
  title="Generic Type Propagation"
  typeHints={[
    { line: 1, text: "theme", type: "Feature<Theme, Context, Namespace>" },
    { line: 5, text: "result", type: "Theme" }
  ]}
  inferredTypes={{
    "theme": "Feature<Theme, Context, Namespace>",
    "result": "Theme (not Theme?)"
  }}
>
  {/* code showing enum feature and evaluation */}
</IDEInsight>
```

---

## Best Practices

### Do:
- ✅ Use `ScopeExplorer` for DSL documentation (shows "what's available here")
- ✅ Use `IDEInsight` for complex examples demonstrating type safety
- ✅ Use `TypeAnnotation` sparingly for key concepts in prose
- ✅ Include realistic descriptions that explain *why* something matters
- ✅ Show autocomplete for discoverability
- ✅ Highlight non-null guarantees explicitly

### Don't:
- ❌ Overuse on every code block (makes docs cluttered)
- ❌ Use `TypeAnnotation` for every variable (too noisy)
- ❌ Create huge `ScopeExplorer` lists (split into multiple if >15 items)
- ❌ Forget to test mobile responsiveness
- ❌ Use without explaining what the component is showing

---

## Technical Details

### Dependencies
All components use:
- React (already in Docusaurus)
- CSS Modules (built into Docusaurus)
- No external dependencies

### Styling
- Uses CSS custom properties for theming
- Matches existing Docusaurus GitHub-style theme
- Supports light/dark mode automatically
- Mobile-responsive breakpoints at 768px

### Performance
- Components are lightweight (no heavy libraries)
- Lazy-loaded by Docusaurus automatically
- No runtime type checking or validation
- Static data passed as props

### Accessibility
- Semantic HTML
- Keyboard navigation for tabs
- ARIA labels where appropriate
- Sufficient color contrast
- Focus indicators

---

## Examples in Codebase

I've created two example files demonstrating usage:

1. **`INTERACTIVE_CODE_BLOCKS_DEMO.mdx`**
   - Overview of all three components
   - Basic usage examples
   - Props documentation

2. **`RULES_DSL_ENHANCED_EXAMPLE.mdx`**
   - Real-world usage in documentation
   - Shows how to enhance existing docs
   - Multiple patterns combined

---

## Customization Options

### Extending IDEInsight

Add more tabs:
```typescript
interface IDEInsightProps {
  // ... existing props
  errorPrevention?: ErrorExample[];  // Show what won't compile
  comparison?: ComparisonView;        // Before/after Konditional
}
```

### Extending ScopeExplorer

Add categories:
```typescript
interface ScopeItem {
  // ... existing fields
  category?: 'targeting' | 'rollout' | 'metadata';
  since?: string;  // API version
}
```

### Adding New Components

Ideas for future components:
- **`CompilationGuard`** - Show what doesn't compile (errors prevented)
- **`TypeFlowDiagram`** - Visualize type propagation through code
- **`SideByeSide`** - Compare Konditional vs alternatives
- **`InteractiveREPL`** - Live code editing (advanced)

---

## Migration Path

### Immediate (No Code Changes)

Already done:
- Created components ✅
- Styling complete ✅
- Dark mode support ✅
- Mobile responsive ✅

### Quick Wins (1-2 hours)

1. Add to Getting Started guide
2. Enhance Rule DSL reference
3. Update Core Concepts page

### Full Rollout (4-6 hours)

1. Systematic review of all docs
2. Add to 10-15 key pages
3. Create component usage guide
4. Get feedback and iterate

---

## Maintenance

### Adding New DSL Methods

When adding new DSL methods to Konditional:

1. Update `ScopeExplorer` items in relevant docs
2. Add to autocomplete lists in `IDEInsight` examples
3. Update type signatures if context changes

### Updating for API Changes

If generic types or signatures change:

1. Update `inferredTypes` props
2. Review `typeHints` accuracy
3. Update scope documentation

---

## Questions to Consider

1. **How much interactivity do you want?**
   - Current components are read-only
   - Could add actual code execution (more complex)

2. **Should we standardize which pages get which components?**
   - Create templates for different doc types
   - Reference vs Tutorial vs Conceptual

3. **Do we want "Try It" buttons?**
   - Link to IDE with pre-filled code
   - Or embed actual Kotlin playground

4. **Should type information be extracted from actual code?**
   - Currently manual (props)
   - Could use Kotlin compiler plugin for accuracy

5. **Do we want visual flow diagrams?**
   - Show how types flow through evaluation
   - Might be helpful for complex scenarios

---

## Recommendations

### Start With:

1. **Add `ScopeExplorer` to Rule DSL page**
   - Immediate value
   - Documents existing functionality
   - Low maintenance

2. **Add `IDEInsight` to Getting Started**
   - Great first impression
   - Shows type safety immediately
   - Sticky learning

3. **Use `TypeAnnotation` in Core Concepts**
   - Emphasize key guarantees
   - Lightweight addition
   - Easy to add incrementally

### Measure Success:

- User feedback on clarity
- Time to first successful feature definition
- Questions in issues/discussions about scope
- Adoption metrics (if available)

---

## Next Steps

1. **Review components with team**
2. **Pick 2-3 pages for initial rollout**
3. **Get user feedback**
4. **Iterate on design/content**
5. **Expand systematically**

The components are ready to use - just import and add to any `.mdx` file!
