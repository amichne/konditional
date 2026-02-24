import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    {
      type: 'category',
      label: 'Getting Started',
      items: [
        'index',
        'overview/start-here',
        'overview/why-typed-flags',
        'overview/product-value-fit',
        'overview/first-success-map',
        'overview/adoption-roadmap',
        'overview/competitive-positioning',
        {
          type: 'category',
          label: 'Quickstart',
          items: [
            'quickstart/index',
            'quickstart/install',
            'quickstart/define-first-flag',
            'quickstart/evaluate-in-app-code',
            'quickstart/add-deterministic-ramp-up',
            'quickstart/load-first-snapshot-safely',
            'quickstart/verify-end-to-end',
          ],
        },
      ],
    },
    {
      type: 'category',
      label: 'Understand',
      items: [
        'concepts/namespaces',
        'concepts/features-and-types',
        'concepts/rules-and-precedence',
        'concepts/context-and-targeting',
        'concepts/evaluation-model',
        'concepts/parse-boundary',
        'concepts/configuration-lifecycle',
      ],
    },
    {
      type: 'category',
      label: 'Do',
      items: [
        'guides/remote-configuration',
        'guides/incremental-updates',
        'guides/custom-structured-values',
        'guides/custom-targeting-axes',
        'guides/namespace-per-team',
        'guides/testing-strategies',
        'guides/migration-from-legacy',
        'guides/enterprise-adoption',
      ],
    },
    {
      type: 'category',
      label: 'Look Up',
      items: [
        'reference/module-dependency-map',
        'reference/api-surface',
        'reference/snapshot-format',
        'reference/patch-format',
        'reference/snapshot-load-options',
        'reference/evaluation-diagnostics',
      ],
    },
    {
      type: 'category',
      label: 'Trust',
      items: [
        'theory/type-safety-boundaries',
        'theory/determinism-proofs',
        'theory/namespace-isolation',
        'theory/parse-dont-validate',
        'theory/atomicity-guarantees',
        'theory/migration-and-shadowing',
        'theory/verified-synthesis',
      ],
    },
    {
      type: 'category',
      label: 'Appendix',
      items: [
        'appendix/glossary',
        'appendix/faq',
        'appendix/changelog',
      ],
    },
  ],
};

export default sidebars;
