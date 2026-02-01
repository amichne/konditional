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
