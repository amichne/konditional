import React from "react";
import {CodeRenderer} from "@site/src/components/code-primitives/CodeRenderer";
import {InsightBadge} from "@site/src/components/code-primitives/InsightBadge";
import {InlayHint, InlayHintDialogProvider} from "@site/src/components/code-primitives/InlayHint";
import {CategoryFilter} from "./CategoryFilter";
import {InsightTooltip} from "./InsightTooltip";
import {useSemanticProfile} from "./hooks/useSemanticProfile";
import {useInsightFilters} from "./hooks/useInsightFilters";
import type {SemanticInsight, SemanticProfile} from "@site/src/types/semantic-profile";
import type {HighlightRegion} from "@site/src/types/code-rendering";

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
    if (!showInlayHints) return new Map<string, SemanticInsight>();
    const map = new Map<string, SemanticInsight>();
    for (const insight of filteredInsights) {
      if (insight.data.type === "TypeInference" && !insight.data.declaredType) {
        const key = `${insight.position.to.line}:${insight.position.to.col}`;
        map.set(key, insight);
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
    <InlayHintDialogProvider>
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
            const insight = inlayHints.get(key);
            if (insight && insight.data.type === "TypeInference") {
              const hintText = `: ${insight.data.inferredType}`;
              const dialogContent = (
                <div>
                  <h3 style={{ marginTop: 0 }}>Type Inference</h3>
                  <p>
                    <strong>Inferred Type:</strong> <code>{insight.data.inferredType}</code>
                  </p>
                  {insight.message && <p>{insight.message}</p>}
                  <div style={{ marginTop: "1rem", fontSize: "0.9em", opacity: 0.7 }}>
                    <div><strong>Category:</strong> {insight.category}</div>
                    <div><strong>Level:</strong> {insight.level}</div>
                  </div>
                </div>
              );
              return (
                <React.Fragment key={position.tokenIndex}>
                  {defaultRender}
                  <InlayHint
                    id={insight.id}
                    text={hintText}
                    position="after"
                    dialogContent={dialogContent}
                  />
                </React.Fragment>
              );
            }
            return defaultRender;
          }}
        />
      </div>
    </InlayHintDialogProvider>
  );
}
