// sidebars.ts — Generated from DOCS-ARCHITECTURE-PLAN.md
// Docusaurus v3 sidebar configuration for Konditional documentation

import type { SidebarsConfig } from "@docusaurus/plugin-content-docs";

const sidebars: SidebarsConfig = {
  docs: [
    "index",
    {
      type: "category",
      label: "Overview",
      collapsed: false,
      items: [
        "overview/start-here",
        "overview/why-typed-flags",
        "overview/product-value-fit",
        "overview/first-success-map",
        "overview/adoption-roadmap",
        "overview/competitive-positioning",
      ],
    },
    {
      type: "category",
      label: "Quickstart",
      collapsed: false,
      items: [
        "quickstart/index",
        "quickstart/install",
        "quickstart/define-first-flag",
        "quickstart/evaluate-in-app-code",
        "quickstart/add-deterministic-ramp-up",
        "quickstart/load-first-snapshot-safely",
        "quickstart/verify-end-to-end",
      ],
    },
    {
      type: "category",
      label: "Concepts",
      items: [
        "concepts/namespaces",
        "concepts/features-and-types",
        "concepts/rules-and-precedence",
        "concepts/context-and-targeting",
        "concepts/evaluation-model",
        "concepts/parse-boundary",
        "concepts/configuration-lifecycle",
      ],
    },
    {
      type: "category",
      label: "Guides",
      items: [
        "guides/remote-configuration",
        "guides/incremental-updates",
        "guides/custom-structured-values",
        "guides/custom-targeting-axes",
        "guides/namespace-per-team",
        "guides/testing-strategies",
        "guides/migration-from-legacy",
        "guides/enterprise-adoption",
      ],
    },
    {
      type: "category",
      label: "Reference",
      items: [
        "reference/api-surface",
        "reference/snapshot-format",
        "reference/patch-format",
        "reference/snapshot-load-options",
        "reference/evaluation-diagnostics",
        "reference/module-dependency-map",
      ],
    },
    {
      type: "category",
      label: "Theory",
      items: [
        "theory/type-safety-boundaries",
        "theory/determinism-proofs",
        "theory/namespace-isolation",
        "theory/parse-dont-validate",
        "theory/atomicity-guarantees",
        "theory/migration-and-shadowing",
      ],
    },
    {
      type: "category",
      label: "Appendix",
      items: [
        "appendix/glossary",
        "appendix/faq",
        "appendix/changelog",
      ],
    },
  ],
};

export default sidebars;
