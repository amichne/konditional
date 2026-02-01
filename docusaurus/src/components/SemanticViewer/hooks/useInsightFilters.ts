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
