# Semantic Code Viewer: Implementation Specification

**Date**: 2026-01-25
**Status**: Implementation Ready
**Depends On**: plan.md (Kotlin analyzer), plan-frontend-integration.md (design)

---

## Phase 1: Core Primitives Extraction

**Goal**: Extract reusable rendering primitives from AnnotatedCode without changing behavior.

### Task 1.1: Create types module

```
docusaurus/src/types/code-rendering.ts
```

```typescript
// Shared position types used by all code rendering components

export type Position = {
  line: number;  // 1-based
  col: number;   // 1-based
};

export type Range = {
  from: Position;
  to: Position;
};

export type TokenPosition = {
  lineIndex: number;    // 0-based (for array indexing)
  tokenIndex: number;   // 0-based within line
  charStart: number;    // 1-based column where token starts
  charEnd: number;      // 1-based column where token ends (inclusive)
};

export type LineAnnotation = {
  id: string;
  label: string | number;
  range: Range;
  title?: string;
  body: React.ReactNode;
};

export type HighlightRegion = {
  range: Range;
  className?: string;
  style?: React.CSSProperties;
};
```

### Task 1.2: Create CodeRenderer primitive

```
docusaurus/src/components/code-primitives/CodeRenderer.tsx
```

```typescript
import React from "react";
import { Highlight, Token } from "prism-react-renderer";
import { usePrismTheme } from "@docusaurus/theme-common";
import type { Range, HighlightRegion } from "@site/src/types/code-rendering";

export type CodeRendererProps = {
  code: string;
  language: string;
  highlights?: HighlightRegion[];
  renderGutter?: (lineNumber: number) => React.ReactNode;
  renderToken?: (token: Token, defaultRender: React.ReactNode, position: TokenPosition) => React.ReactNode;
  className?: string;
};

export function CodeRenderer({
  code,
  language,
  highlights = [],
  renderGutter,
  renderToken,
  className,
}: CodeRendererProps) {
  const theme = usePrismTheme();

  return (
    <Highlight theme={theme} code={code} language={language as any}>
      {({ className: hlClassName, style, tokens, getLineProps, getTokenProps }) => (
        <pre
          className={`${hlClassName} ${className ?? ""}`}
          style={{ ...style, margin: 0, padding: 12, overflowX: "auto" }}
        >
          {tokens.map((line, lineIndex) => {
            const lineNumber = lineIndex + 1;
            let runningCol = 1;

            return (
              <div
                key={lineIndex}
                {...getLineProps({ line })}
                style={{
                  display: "grid",
                  gridTemplateColumns: renderGutter ? "48px 1fr" : "1fr",
                  gap: renderGutter ? 12 : 0,
                }}
              >
                {renderGutter && (
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "flex-end" }}>
                    {renderGutter(lineNumber)}
                  </div>
                )}
                <div style={{ minWidth: 0 }}>
                  {line.map((token, tokenIndex) => {
                    const charStart = runningCol;
                    const charEnd = runningCol + token.content.length - 1;
                    runningCol += token.content.length;

                    const tokenProps = getTokenProps({ token });
                    const position = { lineIndex, tokenIndex, charStart, charEnd };

                    // Check if token overlaps any highlight region
                    const overlappingHighlight = highlights.find((h) =>
                      rangeOverlapsToken(h.range, lineNumber, charStart, charEnd)
                    );

                    const baseStyle = overlappingHighlight
                      ? { ...tokenProps.style, ...overlappingHighlight.style }
                      : tokenProps.style;

                    const defaultRender = (
                      <span
                        key={tokenIndex}
                        {...tokenProps}
                        className={`${tokenProps.className ?? ""} ${overlappingHighlight?.className ?? ""}`}
                        style={baseStyle}
                      />
                    );

                    return renderToken
                      ? renderToken(token, defaultRender, position)
                      : defaultRender;
                  })}
                </div>
              </div>
            );
          })}
        </pre>
      )}
    </Highlight>
  );
}

function rangeOverlapsToken(
  range: Range,
  lineNumber: number,
  charStart: number,
  charEnd: number
): boolean {
  const { from, to } = range;
  if (lineNumber < from.line || lineNumber > to.line) return false;
  if (from.line === to.line) {
    return charEnd >= from.col && charStart <= to.col;
  }
  if (lineNumber === from.line) return charEnd >= from.col;
  if (lineNumber === to.line) return charStart <= to.col;
  return true;
}
```

### Task 1.3: Create InsightBadge primitive

```
docusaurus/src/components/code-primitives/InsightBadge.tsx
```

```typescript
import React from "react";

export type InsightBadgeProps = {
  label: string | number;
  isActive: boolean;
  onClick: () => void;
  onMouseEnter: () => void;
  onMouseLeave: () => void;
  children?: React.ReactNode; // Tooltip content when active
};

export function InsightBadge({
  label,
  isActive,
  onClick,
  onMouseEnter,
  onMouseLeave,
  children,
}: InsightBadgeProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      onFocus={onMouseEnter}
      onBlur={onMouseLeave}
      aria-pressed={isActive}
      aria-haspopup={children != null}
      style={{
        width: 22,
        height: 22,
        borderRadius: 999,
        border: "1px solid var(--ifm-color-emphasis-400)",
        background: isActive ? "var(--ifm-color-emphasis-200)" : "transparent",
        fontSize: 12,
        fontWeight: 700,
        cursor: "pointer",
        position: "relative",
      }}
    >
      {label}
      {isActive && children && (
        <TooltipOverlay>{children}</TooltipOverlay>
      )}
    </button>
  );
}

function TooltipOverlay({ children }: { children: React.ReactNode }) {
  return (
    <span
      role="tooltip"
      style={{
        position: "absolute",
        left: "110%",
        top: "50%",
        transform: "translateY(-50%)",
        minWidth: 260,
        maxWidth: 420,
        whiteSpace: "normal",
        textAlign: "left",
        padding: 10,
        borderRadius: 8,
        border: "1px solid var(--ifm-color-emphasis-300)",
        background: "var(--ifm-background-surface-color)",
        color: "var(--ifm-font-color-base)",
        boxShadow: "0 6px 18px rgba(0,0,0,0.12)",
        zIndex: 10,
      }}
    >
      {children}
    </span>
  );
}
```

### Task 1.4: Create InlayHint primitive

```
docusaurus/src/components/code-primitives/InlayHint.tsx
```

```typescript
import React from "react";

export type InlayHintProps = {
  text: string;
  position: "before" | "after";
  onClick?: () => void;
  isInteractive?: boolean;
};

export function InlayHint({ text, position, onClick, isInteractive = false }: InlayHintProps) {
  const style: React.CSSProperties = {
    opacity: 0.55,
    fontSize: "0.9em",
    fontStyle: "italic",
    marginLeft: position === "after" ? 4 : 0,
    marginRight: position === "before" ? 4 : 0,
    cursor: isInteractive ? "pointer" : "default",
    userSelect: "none",
  };

  if (isInteractive && onClick) {
    return (
      <button
        type="button"
        onClick={onClick}
        style={{ ...style, background: "none", border: "none", padding: 0 }}
      >
        {text}
      </button>
    );
  }

  return <span style={style}>{text}</span>;
}
```

### Task 1.5: Refactor AnnotatedCode to use primitives

Modify existing `AnnotatedCode/index.tsx` to import and use `CodeRenderer`, `InsightBadge`.

**Key changes**:
- Replace inline Highlight usage with CodeRenderer
- Extract badge rendering to use InsightBadge
- Keep HoverMap loading logic unchanged

---

## Phase 2: SemanticProfile Types & Loading

**Goal**: Define TypeScript schema matching Kotlin output, implement data loading.

### Task 2.1: Create SemanticProfile types

```
docusaurus/src/types/semantic-profile.ts
```

```typescript
import type { Range, Position } from "./code-rendering";

// Re-export for convenience
export type { Range, Position };

// ─────────────────────────────────────────────────────────────
// Insight Categories & Levels
// ─────────────────────────────────────────────────────────────

export type InsightCategory =
  | "TYPE_INFERENCE"
  | "NULLABILITY"
  | "SMART_CASTS"
  | "SCOPING"
  | "EXTENSIONS"
  | "LAMBDAS"
  | "OVERLOADS";

export const INSIGHT_CATEGORIES: InsightCategory[] = [
  "TYPE_INFERENCE",
  "NULLABILITY",
  "SMART_CASTS",
  "SCOPING",
  "EXTENSIONS",
  "LAMBDAS",
  "OVERLOADS",
];

export type InsightLevel = "OFF" | "HIGHLIGHTS" | "ALL";

// ─────────────────────────────────────────────────────────────
// Insight Kinds (per category)
// ─────────────────────────────────────────────────────────────

export type InsightKind =
  // TypeInference
  | "INFERRED_TYPE"
  | "EXPLICIT_TYPE"
  | "GENERIC_ARGUMENT_INFERRED"
  // Nullability
  | "NULLABLE_TYPE"
  | "PLATFORM_TYPE"
  | "NULL_SAFE_CALL"
  | "ELVIS_OPERATOR"
  | "NOT_NULL_ASSERTION"
  // SmartCasts
  | "IS_CHECK_CAST"
  | "WHEN_BRANCH_CAST"
  | "NEGATED_CHECK_EXIT"
  | "NULL_CHECK_CAST"
  // Scoping
  | "RECEIVER_CHANGE"
  | "IMPLICIT_THIS"
  | "SCOPE_FUNCTION_ENTRY"
  // Extensions
  | "EXTENSION_FUNCTION_CALL"
  | "EXTENSION_PROPERTY_ACCESS"
  | "MEMBER_VS_EXTENSION_RESOLUTION"
  // Lambdas
  | "LAMBDA_PARAMETER_INFERRED"
  | "LAMBDA_RETURN_INFERRED"
  | "SAM_CONVERSION"
  | "TRAILING_LAMBDA"
  // Overloads
  | "OVERLOAD_RESOLVED"
  | "DEFAULT_ARGUMENT_USED"
  | "NAMED_ARGUMENT_REORDER";

// ─────────────────────────────────────────────────────────────
// Scope Model
// ─────────────────────────────────────────────────────────────

export type ScopeKind =
  | "FILE"
  | "CLASS"
  | "FUNCTION"
  | "LAMBDA"
  | "SCOPE_FUNCTION"
  | "WHEN_BRANCH"
  | "IF_BRANCH"
  | "TRY_BLOCK"
  | "CATCH_BLOCK";

export type ScopeRef = {
  scopeId: string;
  kind: ScopeKind;
  receiverType?: string;
  position: Range;
};

export type ScopeNode = {
  ref: ScopeRef;
  children: ScopeNode[];
  insights: string[]; // insight IDs contained in this scope
};

// ─────────────────────────────────────────────────────────────
// Insight Data (discriminated union)
// ─────────────────────────────────────────────────────────────

export type TypeInferenceData = {
  type: "TypeInference";
  inferredType: string;
  declaredType?: string;
  typeArguments?: string[];
};

export type NullabilityData = {
  type: "Nullability";
  nullableType: string;
  isNullable: boolean;
  isPlatformType: boolean;
  narrowedToNonNull: boolean;
};

export type SmartCastData = {
  type: "SmartCast";
  originalType: string;
  narrowedType: string;
  evidencePosition: Range;
  evidenceKind: string;
};

export type ScopingData = {
  type: "Scoping";
  scopeFunction?: string;
  outerReceiver?: string;
  innerReceiver?: string;
  itParameterType?: string;
};

export type ExtensionData = {
  type: "Extension";
  functionOrProperty: string;
  extensionReceiverType: string;
  dispatchReceiverType?: string;
  resolvedFrom: string;
  competingMember: boolean;
};

export type LambdaParam = {
  name?: string;
  type: string;
};

export type LambdaData = {
  type: "Lambda";
  parameterTypes: LambdaParam[];
  returnType: string;
  inferredFromContext?: string;
  samInterface?: string;
};

export type OverloadData = {
  type: "Overload";
  selectedSignature: string;
  candidateCount: number;
  resolutionFactors: string[];
  defaultArgumentsUsed?: string[];
};

export type InsightData =
  | TypeInferenceData
  | NullabilityData
  | SmartCastData
  | ScopingData
  | ExtensionData
  | LambdaData
  | OverloadData;

// ─────────────────────────────────────────────────────────────
// Semantic Insight
// ─────────────────────────────────────────────────────────────

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

// ─────────────────────────────────────────────────────────────
// Semantic Profile (top-level)
// ─────────────────────────────────────────────────────────────

export type SemanticProfile = {
  snippetId: string;
  codeHash: string;
  code: string;
  insights: SemanticInsight[];
  rootScopes: ScopeNode[];
};
```

### Task 2.2: Create useSemanticProfile hook

```
docusaurus/src/components/SemanticViewer/hooks/useSemanticProfile.ts
```

```typescript
import React from "react";
import useBaseUrl from "@docusaurus/useBaseUrl";
import type { SemanticProfile } from "@site/src/types/semantic-profile";

export type SemanticProfileLoadState = {
  profile: SemanticProfile | null;
  status: "idle" | "loading" | "ready" | "error";
  error: string | null;
};

const profileCache = new Map<string, SemanticProfile>();

export function useSemanticProfile(
  snippetId: string | undefined,
  inlineProfile?: SemanticProfile
): SemanticProfileLoadState {
  const baseUrl = useBaseUrl("semantic-profiles/");

  const resolvedUrl = React.useMemo(() => {
    if (inlineProfile != null || snippetId == null) return null;
    return `${baseUrl}${encodeURIComponent(snippetId)}.json`;
  }, [snippetId, inlineProfile, baseUrl]);

  const [state, setState] = React.useState<SemanticProfileLoadState>(() => ({
    profile: inlineProfile ?? null,
    status: inlineProfile != null ? "ready" : "idle",
    error: null,
  }));

  React.useEffect(() => {
    if (inlineProfile != null) {
      setState({ profile: inlineProfile, status: "ready", error: null });
      return;
    }

    if (resolvedUrl == null) {
      setState({ profile: null, status: "idle", error: null });
      return;
    }

    const cached = profileCache.get(resolvedUrl);
    if (cached != null) {
      setState({ profile: cached, status: "ready", error: null });
      return;
    }

    let cancelled = false;
    setState((prev) => ({ ...prev, status: "loading", error: null }));

    fetch(resolvedUrl)
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then((data: SemanticProfile) => {
        if (cancelled) return;
        profileCache.set(resolvedUrl, data);
        setState({ profile: data, status: "ready", error: null });
      })
      .catch((err) => {
        if (cancelled) return;
        setState({ profile: null, status: "error", error: String(err) });
      });

    return () => {
      cancelled = true;
    };
  }, [inlineProfile, resolvedUrl]);

  return state;
}
```

### Task 2.3: Create useInsightFilters hook

```
docusaurus/src/components/SemanticViewer/hooks/useInsightFilters.ts
```

```typescript
import React from "react";
import type { InsightCategory, InsightLevel, SemanticInsight } from "@site/src/types/semantic-profile";
import { INSIGHT_CATEGORIES } from "@site/src/types/semantic-profile";

export type InsightFilters = {
  enabledCategories: Set<InsightCategory>;
  level: InsightLevel;
};

export type InsightFiltersState = {
  filters: InsightFilters;
  toggleCategory: (category: InsightCategory) => void;
  setLevel: (level: InsightLevel) => void;
  resetToDefaults: () => void;
  filterInsights: (insights: SemanticInsight[]) => SemanticInsight[];
};

const STORAGE_KEY = "semanticViewer:filters";

function defaultFilters(): InsightFilters {
  return {
    enabledCategories: new Set(INSIGHT_CATEGORIES),
    level: "HIGHLIGHTS",
  };
}

function loadFilters(): InsightFilters {
  if (typeof window === "undefined") return defaultFilters();
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return defaultFilters();
    const parsed = JSON.parse(raw);
    return {
      enabledCategories: new Set(parsed.enabledCategories ?? INSIGHT_CATEGORIES),
      level: parsed.level ?? "HIGHLIGHTS",
    };
  } catch {
    return defaultFilters();
  }
}

function saveFilters(filters: InsightFilters): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      enabledCategories: Array.from(filters.enabledCategories),
      level: filters.level,
    })
  );
}

export function useInsightFilters(): InsightFiltersState {
  const [filters, setFilters] = React.useState<InsightFilters>(defaultFilters);

  // Load from localStorage on mount
  React.useEffect(() => {
    setFilters(loadFilters());
  }, []);

  // Persist changes
  React.useEffect(() => {
    saveFilters(filters);
  }, [filters]);

  const toggleCategory = React.useCallback((category: InsightCategory) => {
    setFilters((prev) => {
      const next = new Set(prev.enabledCategories);
      if (next.has(category)) {
        next.delete(category);
      } else {
        next.add(category);
      }
      return { ...prev, enabledCategories: next };
    });
  }, []);

  const setLevel = React.useCallback((level: InsightLevel) => {
    setFilters((prev) => ({ ...prev, level }));
  }, []);

  const resetToDefaults = React.useCallback(() => {
    setFilters(defaultFilters());
  }, []);

  const filterInsights = React.useCallback(
    (insights: SemanticInsight[]): SemanticInsight[] => {
      return insights.filter((insight) => {
        if (!filters.enabledCategories.has(insight.category)) return false;
        if (filters.level === "OFF") return false;
        if (filters.level === "HIGHLIGHTS" && insight.level !== "HIGHLIGHTS") return false;
        return true;
      });
    },
    [filters]
  );

  return { filters, toggleCategory, setLevel, resetToDefaults, filterInsights };
}
```

---

## Phase 3: SemanticViewer Core Component

**Goal**: Build the main viewer with hover tooltip mode.

### Task 3.1: Create SemanticViewerContext

```
docusaurus/src/components/SemanticViewer/SemanticViewerContext.tsx
```

```typescript
import React from "react";
import type { SemanticInsight, SemanticProfile, InsightCategory, InsightLevel } from "@site/src/types/semantic-profile";

export type SemanticViewerContextValue = {
  profile: SemanticProfile | null;
  filteredInsights: SemanticInsight[];
  activeInsightId: string | null;
  setActiveInsightId: (id: string | null) => void;
  lockedInsightId: string | null;
  setLockedInsightId: (id: string | null) => void;
  enabledCategories: Set<InsightCategory>;
  toggleCategory: (category: InsightCategory) => void;
  level: InsightLevel;
  setLevel: (level: InsightLevel) => void;
};

export const SemanticViewerContext = React.createContext<SemanticViewerContextValue | null>(null);

export function useSemanticViewerContext(): SemanticViewerContextValue {
  const ctx = React.useContext(SemanticViewerContext);
  if (ctx == null) {
    throw new Error("useSemanticViewerContext must be used within SemanticViewerProvider");
  }
  return ctx;
}
```

### Task 3.2: Create insight renderers

```
docusaurus/src/components/SemanticViewer/InsightTooltip.tsx
```

```typescript
import React from "react";
import type { SemanticInsight, InsightData } from "@site/src/types/semantic-profile";

export function InsightTooltip({ insight }: { insight: SemanticInsight }) {
  return (
    <div style={{ minWidth: 240 }}>
      <div style={{ fontWeight: 700, marginBottom: 6, fontSize: 11, textTransform: "uppercase", opacity: 0.7 }}>
        {formatCategory(insight.category)}
      </div>
      <InsightDataRenderer data={insight.data} />
      <InsightNarrative insight={insight} />
    </div>
  );
}

function InsightDataRenderer({ data }: { data: InsightData }) {
  switch (data.type) {
    case "TypeInference":
      return (
        <div>
          <div>
            <strong>Inferred:</strong> <code>{data.inferredType}</code>
          </div>
          {data.declaredType && (
            <div style={{ marginTop: 4, opacity: 0.8 }}>
              Declared: <code>{data.declaredType}</code>
            </div>
          )}
          {data.typeArguments && data.typeArguments.length > 0 && (
            <div style={{ marginTop: 4 }}>
              Type args: {data.typeArguments.map((t, i) => <code key={i}>{t}</code>).reduce((a, b) => <>{a}, {b}</>)}
            </div>
          )}
        </div>
      );

    case "SmartCast":
      return (
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <code>{data.originalType}</code>
            <span style={{ opacity: 0.5 }}>→</span>
            <code style={{ color: "var(--ifm-color-success)" }}>{data.narrowedType}</code>
          </div>
          <div style={{ marginTop: 4, fontSize: 12, opacity: 0.8 }}>
            Evidence: {data.evidenceKind} at line {data.evidencePosition.from.line}
          </div>
        </div>
      );

    case "Scoping":
      return (
        <div>
          {data.scopeFunction && <div>Scope function: <code>{data.scopeFunction}</code></div>}
          {data.innerReceiver && (
            <div style={{ marginTop: 4 }}>
              <code>this</code> is now <code>{data.innerReceiver}</code>
            </div>
          )}
          {data.itParameterType && (
            <div style={{ marginTop: 4 }}>
              <code>it</code> has type <code>{data.itParameterType}</code>
            </div>
          )}
        </div>
      );

    case "Extension":
      return (
        <div>
          <div>
            <code>{data.functionOrProperty}</code> on <code>{data.extensionReceiverType}</code>
          </div>
          <div style={{ marginTop: 4, fontSize: 12, opacity: 0.8 }}>
            From: {data.resolvedFrom}
          </div>
          {data.competingMember && (
            <div style={{ marginTop: 4, fontSize: 12, color: "var(--ifm-color-warning)" }}>
              Shadowed member function exists
            </div>
          )}
        </div>
      );

    case "Lambda":
      return (
        <div>
          <div>
            ({data.parameterTypes.map((p, i) => (
              <span key={i}>
                {p.name ? `${p.name}: ` : ""}<code>{p.type}</code>
                {i < data.parameterTypes.length - 1 && ", "}
              </span>
            ))}) → <code>{data.returnType}</code>
          </div>
          {data.samInterface && (
            <div style={{ marginTop: 4 }}>SAM: <code>{data.samInterface}</code></div>
          )}
        </div>
      );

    case "Overload":
      return (
        <div>
          <div><code>{data.selectedSignature}</code></div>
          <div style={{ marginTop: 4, fontSize: 12, opacity: 0.8 }}>
            Selected from {data.candidateCount} candidates
          </div>
          {data.resolutionFactors.length > 0 && (
            <div style={{ marginTop: 4, fontSize: 12 }}>
              Factors: {data.resolutionFactors.join(", ")}
            </div>
          )}
        </div>
      );

    case "Nullability":
      return (
        <div>
          <div><code>{data.nullableType}</code></div>
          {data.isPlatformType && (
            <div style={{ marginTop: 4, color: "var(--ifm-color-warning)" }}>
              Platform type (nullability unknown)
            </div>
          )}
          {data.narrowedToNonNull && (
            <div style={{ marginTop: 4, color: "var(--ifm-color-success)" }}>
              Narrowed to non-null in this scope
            </div>
          )}
        </div>
      );

    default:
      return <div>Unknown insight type</div>;
  }
}

function InsightNarrative({ insight }: { insight: SemanticInsight }) {
  const narrative = getNarrative(insight);
  if (!narrative) return null;

  return (
    <div style={{ marginTop: 8, paddingTop: 8, borderTop: "1px solid var(--ifm-color-emphasis-200)", fontSize: 13, opacity: 0.9 }}>
      {narrative}
    </div>
  );
}

function getNarrative(insight: SemanticInsight): string | null {
  switch (insight.data.type) {
    case "TypeInference":
      return "The compiler infers this type from the expression — no annotation needed.";
    case "SmartCast":
      return `After the ${insight.data.evidenceKind}, the compiler knows this is ${insight.data.narrowedType}.`;
    case "Scoping":
      return insight.data.scopeFunction
        ? `Inside ${insight.data.scopeFunction}, the receiver changes for cleaner syntax.`
        : null;
    case "Extension":
      return insight.data.competingMember
        ? "This extension is called because it's more specific than the member."
        : "This extension function feels native but comes from a library.";
    default:
      return null;
  }
}

function formatCategory(category: string): string {
  return category.replace(/_/g, " ");
}
```

### Task 3.3: Create CategoryFilter component

```
docusaurus/src/components/SemanticViewer/CategoryFilter.tsx
```

```typescript
import React from "react";
import type { InsightCategory, InsightLevel } from "@site/src/types/semantic-profile";
import { INSIGHT_CATEGORIES } from "@site/src/types/semantic-profile";

type CategoryFilterProps = {
  enabledCategories: Set<InsightCategory>;
  level: InsightLevel;
  onToggleCategory: (category: InsightCategory) => void;
  onSetLevel: (level: InsightLevel) => void;
};

const CATEGORY_LABELS: Record<InsightCategory, string> = {
  TYPE_INFERENCE: "Types",
  NULLABILITY: "Null",
  SMART_CASTS: "Casts",
  SCOPING: "Scope",
  EXTENSIONS: "Ext",
  LAMBDAS: "λ",
  OVERLOADS: "Overload",
};

export function CategoryFilter({
  enabledCategories,
  level,
  onToggleCategory,
  onSetLevel,
}: CategoryFilterProps) {
  return (
    <div
      style={{
        display: "flex",
        flexWrap: "wrap",
        gap: 6,
        padding: "8px 12px",
        borderBottom: "1px solid var(--ifm-color-emphasis-200)",
        background: "var(--ifm-color-emphasis-100)",
        fontSize: 12,
      }}
    >
      {INSIGHT_CATEGORIES.map((cat) => (
        <button
          key={cat}
          type="button"
          onClick={() => onToggleCategory(cat)}
          aria-pressed={enabledCategories.has(cat)}
          style={{
            padding: "3px 8px",
            borderRadius: 4,
            border: "1px solid var(--ifm-color-emphasis-300)",
            background: enabledCategories.has(cat) ? "var(--ifm-color-primary-lightest)" : "transparent",
            cursor: "pointer",
            fontWeight: enabledCategories.has(cat) ? 600 : 400,
          }}
        >
          {CATEGORY_LABELS[cat]}
        </button>
      ))}

      <div style={{ marginLeft: "auto", display: "flex", gap: 4 }}>
        <button
          type="button"
          onClick={() => onSetLevel("HIGHLIGHTS")}
          aria-pressed={level === "HIGHLIGHTS"}
          style={{
            padding: "3px 8px",
            borderRadius: 4,
            border: "1px solid var(--ifm-color-emphasis-300)",
            background: level === "HIGHLIGHTS" ? "var(--ifm-color-emphasis-200)" : "transparent",
            cursor: "pointer",
          }}
        >
          Key
        </button>
        <button
          type="button"
          onClick={() => onSetLevel("ALL")}
          aria-pressed={level === "ALL"}
          style={{
            padding: "3px 8px",
            borderRadius: 4,
            border: "1px solid var(--ifm-color-emphasis-300)",
            background: level === "ALL" ? "var(--ifm-color-emphasis-200)" : "transparent",
            cursor: "pointer",
          }}
        >
          All
        </button>
      </div>
    </div>
  );
}
```

### Task 3.4: Create main SemanticViewer component

```
docusaurus/src/components/SemanticViewer/index.tsx
```

```typescript
import React from "react";
import { CodeRenderer } from "@site/src/components/code-primitives/CodeRenderer";
import { InsightBadge } from "@site/src/components/code-primitives/InsightBadge";
import { InlayHint } from "@site/src/components/code-primitives/InlayHint";
import { CategoryFilter } from "./CategoryFilter";
import { InsightTooltip } from "./InsightTooltip";
import { useSemanticProfile } from "./hooks/useSemanticProfile";
import { useInsightFilters } from "./hooks/useInsightFilters";
import type { SemanticProfile, SemanticInsight } from "@site/src/types/semantic-profile";
import type { HighlightRegion } from "@site/src/types/code-rendering";

export type SemanticViewerProps = {
  snippetId?: string;
  profile?: SemanticProfile;
  showFilters?: boolean;
  showInlayHints?: boolean;
  initialCategories?: string[];
};

export function SemanticViewer({
  snippetId,
  profile: inlineProfile,
  showFilters = true,
  showInlayHints = true,
}: SemanticViewerProps) {
  const { profile, status, error } = useSemanticProfile(snippetId, inlineProfile);
  const { filters, toggleCategory, setLevel, filterInsights } = useInsightFilters();

  const [activeId, setActiveId] = React.useState<string | null>(null);
  const [lockedId, setLockedId] = React.useState<string | null>(null);

  const effectiveActiveId = lockedId ?? activeId;

  const filteredInsights = React.useMemo(
    () => (profile ? filterInsights(profile.insights) : []),
    [profile, filterInsights]
  );

  const insightsByLine = React.useMemo(() => {
    const map = new Map<number, SemanticInsight[]>();
    for (const insight of filteredInsights) {
      const line = insight.position.from.line;
      map.set(line, [...(map.get(line) ?? []), insight]);
    }
    return map;
  }, [filteredInsights]);

  const activeInsight = effectiveActiveId
    ? filteredInsights.find((i) => i.id === effectiveActiveId)
    : undefined;

  const highlights: HighlightRegion[] = React.useMemo(() => {
    if (!activeInsight) return [];
    return [
      {
        range: activeInsight.position,
        style: { background: "rgba(250, 204, 21, 0.35)", borderRadius: 2 },
      },
    ];
  }, [activeInsight]);

  // Inlay hints for type inference
  const inlayHints = React.useMemo(() => {
    if (!showInlayHints) return new Map<string, string>();
    const map = new Map<string, string>();
    for (const insight of filteredInsights) {
      if (insight.data.type === "TypeInference" && !insight.data.declaredType) {
        const key = `${insight.position.to.line}:${insight.position.to.col}`;
        map.set(key, `: ${insight.data.inferredType}`);
      }
    }
    return map;
  }, [filteredInsights, showInlayHints]);

  if (status === "loading") {
    return (
      <div style={{ padding: 12, background: "var(--ifm-color-emphasis-100)", borderRadius: 8 }}>
        Loading semantic profile...
      </div>
    );
  }

  if (status === "error" || !profile) {
    return (
      <div style={{ padding: 12, background: "var(--ifm-color-emphasis-100)", borderRadius: 8 }}>
        {error ?? "No semantic profile available"}
      </div>
    );
  }

  return (
    <div style={{ border: "1px solid var(--ifm-color-emphasis-300)", borderRadius: 8, overflow: "hidden" }}>
      {showFilters && (
        <CategoryFilter
          enabledCategories={filters.enabledCategories}
          level={filters.level}
          onToggleCategory={toggleCategory}
          onSetLevel={setLevel}
        />
      )}

      <CodeRenderer
        code={profile.code}
        language="kotlin"
        highlights={highlights}
        renderGutter={(lineNumber) => {
          const lineInsights = insightsByLine.get(lineNumber) ?? [];
          return (
            <>
              {lineInsights.map((insight, idx) => (
                <InsightBadge
                  key={insight.id}
                  label={idx + 1}
                  isActive={insight.id === effectiveActiveId}
                  onClick={() => setLockedId((prev) => (prev === insight.id ? null : insight.id))}
                  onMouseEnter={() => setActiveId(insight.id)}
                  onMouseLeave={() => setActiveId(null)}
                >
                  <InsightTooltip insight={insight} />
                </InsightBadge>
              ))}
              <span style={{ opacity: 0.35, fontVariantNumeric: "tabular-nums", fontSize: 12 }}>
                {String(lineNumber).padStart(2, "0")}
              </span>
            </>
          );
        }}
        renderToken={(token, defaultRender, position) => {
          const key = `${position.lineIndex + 1}:${position.charEnd}`;
          const hint = inlayHints.get(key);
          if (hint) {
            return (
              <React.Fragment key={position.tokenIndex}>
                {defaultRender}
                <InlayHint text={hint} position="after" />
              </React.Fragment>
            );
          }
          return defaultRender;
        }}
      />
    </div>
  );
}
```

---

## Phase 4: Markdown Integration

**Goal**: Route code fences to SemanticViewer.

### Task 4.1: Update CodeBlock wrapper

```
docusaurus/src/theme/CodeBlock/index.tsx
```

```typescript
import React from "react";
import CodeBlock from "@theme-original/CodeBlock";
import { AnnotatedCode } from "@site/src/components/AnnotatedCode";
import { SemanticViewer } from "@site/src/components/SemanticViewer";

type CodeBlockProps = React.ComponentProps<typeof CodeBlock>;

function extractLanguage(className?: string): string | null {
  if (!className) return null;
  const match = className.match(/language-([^\s]+)/);
  return match ? match[1] : null;
}

function extractMeta(metastring?: string): { id?: string; semantic?: boolean } {
  if (!metastring) return {};
  const idMatch = metastring.match(/\bid=(?:"([^"]+)"|'([^']+)'|([^\s]+))/);
  const id = idMatch ? (idMatch[1] ?? idMatch[2] ?? idMatch[3]) : undefined;
  const semantic = /\bsemantic\b/.test(metastring);
  return { id, semantic };
}

function extractCode(children: CodeBlockProps["children"]): string {
  return React.Children.toArray(children)
    .map((child) => (typeof child === "string" ? child : ""))
    .join("");
}

export default function CodeBlockWrapper(props: CodeBlockProps) {
  const language = extractLanguage(props.className);
  const { id, semantic } = extractMeta(props.metastring);

  if (language === "kotlin" && id != null) {
    if (semantic) {
      return <SemanticViewer snippetId={id} />;
    }
    return (
      <AnnotatedCode
        snippetId={id}
        language={language}
        code={extractCode(props.children)}
      />
    );
  }

  return <CodeBlock {...props} />;
}
```

### Task 4.2: Update MDXComponents

```
docusaurus/src/theme/MDXComponents.tsx
```

```typescript
import MDXComponents from "@theme-original/MDXComponents";
import { CodeToggle } from "@site/src/components/CodeToggle";
import { AnnotatedCode } from "@site/src/components/AnnotatedCode";
import { SemanticViewer } from "@site/src/components/SemanticViewer";

export default {
  ...MDXComponents,
  CodeToggle,
  AnnotatedCode,
  SemanticViewer,
};
```

---

## Phase 5: Narrative Mode (Future)

### Task 5.1: Create NarrativeMode component

```
docusaurus/src/components/SemanticViewer/NarrativeMode.tsx
```

This adds a guided walkthrough with prev/next navigation, ordered by insight position or custom sequence.

---

## File Creation Order

1. `src/types/code-rendering.ts`
2. `src/types/semantic-profile.ts`
3. `src/components/code-primitives/CodeRenderer.tsx`
4. `src/components/code-primitives/InsightBadge.tsx`
5. `src/components/code-primitives/InlayHint.tsx`
6. `src/components/SemanticViewer/hooks/useSemanticProfile.ts`
7. `src/components/SemanticViewer/hooks/useInsightFilters.ts`
8. `src/components/SemanticViewer/InsightTooltip.tsx`
9. `src/components/SemanticViewer/CategoryFilter.tsx`
10. `src/components/SemanticViewer/SemanticViewerContext.tsx`
11. `src/components/SemanticViewer/index.tsx`
12. Update `src/theme/CodeBlock/index.tsx`
13. Update `src/theme/MDXComponents.tsx`
14. Refactor `src/components/AnnotatedCode/index.tsx` to use primitives

---

## Example Payloads

### Simple Example: `simple-types.json`

Minimal 2-line snippet demonstrating type inference and extension calls:

```kotlin
val numbers = listOf(1, 2, 3)
val doubled = numbers.map { it * 2 }
```

**Insights:** 4 total
- 2 type inference (numbers, doubled)
- 1 lambda parameter inference (it)
- 1 extension function call (map)

### Comprehensive Example: `example-demo.json`

Realistic snippet with nested scopes and smart casts:

```kotlin
data class User(val name: String, val email: String?)

fun processUser(user: User?) {
    user?.let { u ->
        val greeting = "Hello, ${u.name}"
        println(greeting)

        u.email?.let { email ->
            sendEmail(email)
        }
    }
}

fun sendEmail(address: String) {
    println("Sending to: $address")
}
```

**Insights:** 9 total across 6 categories
- TYPE_INFERENCE: `greeting` inferred as `String`
- NULLABILITY: `user?.let`, `u.email?.let`
- SMART_CASTS: `u` narrowed to `User`, `email` narrowed to `String`
- SCOPING: outer `let` (User receiver), inner `let` (String receiver)
- LAMBDAS: parameter type inference
- OVERLOADS: `sendEmail` resolution

### Demo Page

See `docs/examples/semantic-viewer-demo.mdx` for interactive demonstration.

---

## Validation Checklist

- [x] Existing AnnotatedCode behavior unchanged
- [x] SemanticViewer loads profiles from `/semantic-profiles/`
- [x] Category filters persist to localStorage
- [x] Inlay hints render for inferred types
- [x] Hover tooltips show insight details
- [x] Click-to-lock works for insights
- [x] Token highlighting matches active insight range
- [x] `semantic` attribute in code fence triggers SemanticViewer
- [ ] Mobile: tooltips readable on touch (needs testing)
- [ ] Dark mode: colors adapt to theme (needs testing)
