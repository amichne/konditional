# Semantic Code Viewer: Frontend Integration Plan

**Date**: 2026-01-25
**Status**: Draft
**Depends On**: Eager Semantic Analyzer (plan.md)

## Vision

Create an IDE-like documentation experience that communicates "what it feels like to write code using this library." The viewer should leverage developers' familiarity with IntelliJ/VS Code visual language to demonstrate Kotlin's type safety, smart casts, and API ergonomics without requiring them to clone and run code.

**Core Insight**: Developers evaluate libraries partly by imagining themselves using them. Static code samples don't convey the _dynamic feedback loop_ that makes well-designed Kotlin APIs satisfying. This component bridges that gap.

---

## Goals

1. **IDE-Familiar Visual Language** — Inlay hints, smart cast indicators, scope highlighting
2. **Configurable Expressiveness** — Filter by insight category, toggle between detail levels
3. **Multiple Interaction Modes** — Passive hints, hover tooltips, focus-lock, guided narrative
4. **Schema-Driven Rendering** — Frontend interprets structured `SemanticProfile`, not pre-formatted text
5. **Progressive Enhancement** — Works without JavaScript (falls back to syntax-highlighted code)

## Non-Goals

- Actual code execution or REPL functionality
- Full IDE feature set (refactoring, navigation, completion)
- Support for non-Kotlin languages (initially)

---

## Architecture Decision

### Options Considered

| Option | Description | Pros | Cons |
|--------|-------------|------|------|
| **A: Extend AnnotatedCode** | Add SemanticProfile support to existing component | Incremental, backwards compatible | Mixing concerns, growing complexity |
| **B: New SemanticViewer** | Purpose-built component for SemanticProfile | Clean design, optimized UX | Some duplication |
| **C: Composable Primitives** | Shared foundation used by both components | Maximum flexibility | Higher upfront complexity |

### Recommendation: Option C (Composable Primitives)

Build a layered architecture where:
- **Layer 1**: Core rendering primitives shared by all code viewers
- **Layer 2**: `AnnotatedCode` (simple hover annotations) and `SemanticViewer` (full insight system)
- **Layer 3**: MDX integration and markdown code fence routing

This allows AnnotatedCode to remain simple while SemanticViewer handles the rich IDE-like experience.

---

## Component Hierarchy

```
docusaurus/src/components/
├── code-primitives/
│   ├── CodeRenderer.tsx         # Prism + line numbers + grid layout
│   ├── TokenSpan.tsx            # Individual token with optional decorations
│   ├── InlayHint.tsx            # Faded inline type hints (like IntelliJ)
│   ├── InsightBadge.tsx         # Numbered annotation badge
│   ├── TooltipOverlay.tsx       # Positioned tooltip container
│   └── types.ts                 # Shared position types
│
├── AnnotatedCode/
│   └── index.tsx                # Simplified: uses CodeRenderer + TooltipOverlay
│
├── SemanticViewer/
│   ├── index.tsx                # Main component
│   ├── SemanticViewerContext.tsx
│   ├── InsightPanel.tsx         # Right-side insight details
│   ├── CategoryFilter.tsx       # Toggle insight categories
│   ├── ScopeIndicator.tsx       # Visual scope/receiver tracking
│   ├── SmartCastFlow.tsx        # Visual type narrowing indicators
│   ├── NarrativeMode.tsx        # Guided walkthrough
│   └── hooks/
│       ├── useSemanticProfile.ts
│       ├── useInsightFilters.ts
│       └── useActiveInsight.ts
│
└── CodeToggle/
    └── index.tsx                # Existing, unchanged
```

---

## Schema Mapping

### Backend → Frontend Type Alignment

The Kotlin `SemanticProfile` maps directly to TypeScript types:

```typescript
// types/semantic-profile.ts

export type InsightCategory =
  | 'TYPE_INFERENCE' | 'NULLABILITY' | 'SMART_CASTS'
  | 'SCOPING' | 'EXTENSIONS' | 'LAMBDAS' | 'OVERLOADS';

export type InsightLevel = 'OFF' | 'HIGHLIGHTS' | 'ALL';

export type InsightKind =
  // TypeInference
  | 'INFERRED_TYPE' | 'EXPLICIT_TYPE' | 'GENERIC_ARGUMENT_INFERRED'
  // Nullability
  | 'NULLABLE_TYPE' | 'PLATFORM_TYPE' | 'NULL_SAFE_CALL' | 'ELVIS_OPERATOR' | 'NOT_NULL_ASSERTION'
  // SmartCasts
  | 'IS_CHECK_CAST' | 'WHEN_BRANCH_CAST' | 'NEGATED_CHECK_EXIT' | 'NULL_CHECK_CAST'
  // Scoping
  | 'RECEIVER_CHANGE' | 'IMPLICIT_THIS' | 'SCOPE_FUNCTION_ENTRY'
  // Extensions
  | 'EXTENSION_FUNCTION_CALL' | 'EXTENSION_PROPERTY_ACCESS' | 'MEMBER_VS_EXTENSION_RESOLUTION'
  // Lambdas
  | 'LAMBDA_PARAMETER_INFERRED' | 'LAMBDA_RETURN_INFERRED' | 'SAM_CONVERSION' | 'TRAILING_LAMBDA'
  // Overloads
  | 'OVERLOAD_RESOLVED' | 'DEFAULT_ARGUMENT_USED' | 'NAMED_ARGUMENT_REORDER';

export type Position = { line: number; col: number };
export type Range = { from: Position; to: Position };

export type ScopeKind =
  | 'FILE' | 'CLASS' | 'FUNCTION' | 'LAMBDA' | 'SCOPE_FUNCTION'
  | 'WHEN_BRANCH' | 'IF_BRANCH' | 'TRY_BLOCK' | 'CATCH_BLOCK';

export type ScopeRef = {
  scopeId: string;
  kind: ScopeKind;
  receiverType?: string;
  position: Range;
};

export type ScopeNode = {
  ref: ScopeRef;
  children: ScopeNode[];
  insights: string[]; // insight IDs
};

// Discriminated union for insight data
export type InsightData =
  | { type: 'TypeInference'; inferredType: string; declaredType?: string; typeArguments?: string[] }
  | { type: 'Nullability'; type: string; isNullable: boolean; isPlatformType: boolean; narrowedToNonNull: boolean }
  | { type: 'SmartCast'; originalType: string; narrowedType: string; evidencePosition: Range; evidenceKind: string }
  | { type: 'Scoping'; scopeFunction?: string; outerReceiver?: string; innerReceiver?: string; itParameterType?: string }
  | { type: 'Extension'; functionOrProperty: string; extensionReceiverType: string; dispatchReceiverType?: string; resolvedFrom: string; competingMember: boolean }
  | { type: 'Lambda'; parameterTypes: { name?: string; type: string }[]; returnType: string; inferredFromContext?: string; samInterface?: string }
  | { type: 'Overload'; selectedSignature: string; candidateCount: number; resolutionFactors: string[]; defaultArgumentsUsed?: string[] };

export type SemanticInsight = {
  id: string;
  position: Range;
  category: InsightCategory;
  level: InsightLevel;
  kind: InsightKind;
  scopeChain: ScopeRef[];
  data: InsightData;
  tokenText: string;
};

export type SemanticProfile = {
  snippetId: string;
  codeHash: string;
  code: string;
  insights: SemanticInsight[];
  rootScopes: ScopeNode[];
};
```

---

## Rendering Modes

### Mode 1: Passive Inlay Hints

Always-visible type information, similar to IntelliJ's inlay hints:

```
val items = listOf("a", "b")  : List<String>
    ⎿─────⏐                    └─ inlay hint (faded)
```

**When to show**: TYPE_INFERENCE insights with `level: HIGHLIGHTS`

### Mode 2: Hover Tooltips

Rich tooltip on hover, showing insight details:

```
┌─────────────────────────────────────────┐
│ Type Inference                          │
│ ─────────────────────────────────────── │
│ Inferred: List<String>                  │
│ No explicit type annotation needed      │
│                                         │
│ The compiler infers this from           │
│ listOf("a", "b") → vararg String → List │
└─────────────────────────────────────────┘
```

### Mode 3: Focus Lock

Click an insight badge to lock focus. Enables:
- Persistent tooltip display
- Scope highlighting (show which scope you're in)
- Related insights highlighting (e.g., the smart cast source)

### Mode 4: Narrative Walkthrough

Guided tour through insights in logical order:

```
┌──────────────────────────────────────────────────────┐
│ Insight 3 of 12                         [< Prev] [Next >] │
│ ──────────────────────────────────────────────────── │
│ SMART CAST                                           │
│                                                      │
│ After the null check on line 5, the compiler knows   │
│ that `user` cannot be null. The type narrows from    │
│ `User?` to `User`.                                   │
│                                                      │
│ This is what makes Kotlin's null-safety ergonomic — │
│ you check once, then write clean code.              │
└──────────────────────────────────────────────────────┘
```

---

## Category Filter UI

Horizontal filter bar above code:

```
┌─────────────────────────────────────────────────────────────┐
│ [◉ Types] [◉ Nullability] [○ Smart Casts] [◉ Scopes] ...   │
│                                           [All] [Highlights] │
└─────────────────────────────────────────────────────────────┘
```

- Toggle categories on/off
- Global level switch (ALL vs HIGHLIGHTS)
- Persist preference to localStorage

---

## Visual Language Reference

| Insight Type | Visual Treatment |
|--------------|------------------|
| **Inferred type** | Faded `: Type` after variable (IntelliJ style) |
| **Smart cast** | Subtle background tint on narrowed region |
| **Scope function entry** | Left-border indicator + receiver label |
| **Extension call** | Small icon/badge before function name |
| **Platform type** | Warning-colored `!` suffix |
| **Null-safe call** | Highlight on `?.` operator |
| **Overload resolution** | Badge showing candidate count |

---

## Markdown Integration

### Option A: Attribute-based routing

````markdown
```kotlin id="example-1" semantic
val x = listOf(1, 2, 3)
```

````

The `semantic` attribute triggers SemanticViewer instead of AnnotatedCode.

### Option B: Separate fence language

````markdown
```kotlin-semantic id="example-1"
val x = listOf(1, 2, 3)
```
````

### Option C: Component-only (no fence integration)
````
```markdown
<SemanticViewer snippetId="example-1" />
```
````

**Recommendation**: Option A for seamless MDX authoring, with Option C available for complex cases.

---

## Data Loading Strategy

### Static Generation (Recommended)

1. Kotlin analyzer runs at build time
2. Produces `semantic-profiles/{snippetId}.json` files
3. Frontend loads on demand with caching

### File Location

```
docusaurus/static/
├── hovermaps/           # Legacy HoverMap format (keep for compatibility)
│   └── *.json
└── semantic-profiles/   # New SemanticProfile format
    └── *.json
```

### Hook Design

```typescript
function useSemanticProfile(snippetId: string): {
  profile: SemanticProfile | null;
  status: 'idle' | 'loading' | 'ready' | 'error';
  error: string | null;
}
```

---

## Insight Rendering Templates

Each `InsightData` type needs a rendering template:

### TypeInferenceData

```tsx
<InsightTooltip>
  <InsightHeader>Type Inference</InsightHeader>
  <InsightBody>
    <TypeDisplay type={data.inferredType} />
    {data.declaredType && (
      <Note>Explicit annotation: {data.declaredType}</Note>
    )}
    {data.typeArguments && (
      <TypeArgs args={data.typeArguments} />
    )}
  </InsightBody>
  <InsightNarrative>
    The compiler infers this type from the expression on the right.
    No explicit annotation needed.
  </InsightNarrative>
</InsightTooltip>
```

### SmartCastData

```tsx
<InsightTooltip>
  <InsightHeader>Smart Cast</InsightHeader>
  <InsightBody>
    <TypeNarrowing from={data.originalType} to={data.narrowedType} />
    <Evidence position={data.evidencePosition} kind={data.evidenceKind} />
  </InsightBody>
  <InsightNarrative>
    After the {data.evidenceKind} on line {data.evidencePosition.from.line},
    the compiler knows this value is of type {data.narrowedType}.
  </InsightNarrative>
</InsightTooltip>
```

---

## Narrative Generation

For "sales tool" documentation, we want compelling narratives per insight category:

| Category | Narrative Theme |
|----------|-----------------|
| TYPE_INFERENCE | "No boilerplate — the compiler knows" |
| NULLABILITY | "Safety without ceremony" |
| SMART_CASTS | "Check once, code cleanly" |
| SCOPING | "Contextual APIs that read naturally" |
| EXTENSIONS | "Library APIs that feel built-in" |
| LAMBDAS | "Expressive without explicit types" |
| OVERLOADS | "One API, many entry points" |

These narratives should be configurable per-snippet via metadata:

```json
{
  "snippetId": "example-1",
  "narrativeOverrides": {
    "ins_001": "This is where the magic happens — no type annotation needed."
  }
}
```

---

## Implementation Phases

### Phase 1: Core Primitives

1. Extract `CodeRenderer` from current AnnotatedCode
2. Create `TokenSpan`, `InlayHint`, `InsightBadge` primitives
3. Refactor AnnotatedCode to use primitives
4. **Milestone**: Existing behavior preserved, internals modernized

### Phase 2: SemanticViewer MVP

1. Create TypeScript types matching SemanticProfile schema
2. Implement `useSemanticProfile` hook
3. Build SemanticViewer with hover tooltips
4. Add markdown fence routing (`semantic` attribute)
5. **Milestone**: Basic semantic viewing works for one snippet

### Phase 3: Visual Polish

1. Implement InlayHint rendering (passive mode)
2. Add smart cast background highlighting
3. Implement scope indicators
4. Add focus-lock interaction
5. **Milestone**: IDE-like visual experience

### Phase 4: Configurability

1. Implement CategoryFilter component
2. Add insight level toggle
3. Persist preferences to localStorage
4. **Milestone**: Users can customize their view

### Phase 5: Narrative Mode

1. Implement NarrativeMode component
2. Add prev/next navigation
3. Support narrative overrides from metadata
4. **Milestone**: Guided walkthrough experience

### Phase 6: Polish & Integration

1. Add keyboard navigation
2. Improve mobile/responsive behavior
3. Add print styles (expand all tooltips)
4. Performance optimization (virtualization for large snippets)
5. **Milestone**: Production-ready

---

## Open Questions

1. **Mobile experience**: How should insights display on touch devices?
2. **Accessibility**: How do we make insights accessible to screen readers?
3. **Dark/light mode**: Do insight colors need theme-aware variants?
4. **Performance**: For snippets with 50+ insights, should we virtualize?
5. **Analytics**: Should we track which insights users interact with?

---

## Success Criteria

1. **Developer reaction**: "This feels like my IDE"
2. **Documentation value**: Readers understand API benefits without running code
3. **Authoring experience**: MDX authors can add semantic viewing with one attribute
4. **Performance**: No perceptible lag on code blocks under 200 lines
5. **Accessibility**: Insights navigable via keyboard, readable by screen readers

---

## File Changes Summary

### New Files

```
docusaurus/src/components/code-primitives/
├── CodeRenderer.tsx
├── TokenSpan.tsx
├── InlayHint.tsx
├── InsightBadge.tsx
├── TooltipOverlay.tsx
└── types.ts

docusaurus/src/components/SemanticViewer/
├── index.tsx
├── SemanticViewerContext.tsx
├── InsightPanel.tsx
├── CategoryFilter.tsx
├── ScopeIndicator.tsx
├── SmartCastFlow.tsx
├── NarrativeMode.tsx
└── hooks/
    ├── useSemanticProfile.ts
    ├── useInsightFilters.ts
    └── useActiveInsight.ts

docusaurus/src/types/
└── semantic-profile.ts
```

### Modified Files

```
docusaurus/src/components/AnnotatedCode/index.tsx  # Refactor to use primitives
docusaurus/src/theme/CodeBlock/index.tsx           # Add semantic routing
docusaurus/src/theme/MDXComponents.tsx             # Export SemanticViewer
```

### New Static Assets

```
docusaurus/static/semantic-profiles/
└── *.json  # Generated by Kotlin analyzer
```
