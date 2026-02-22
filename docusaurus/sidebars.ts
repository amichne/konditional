import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    {
      type: 'category',
      label: 'Start here',
      items: [
        {type: 'doc', id: 'index', label: 'What is Konditional?'},
        'overview/start-here',
        'overview/product-value-fit',
        'overview/why-typed-flags',
        'overview/first-success-map',
      ],
    },
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
    {
      type: 'category',
      label: 'How-to guides',
      items: [
        'how-to-guides/rolling-out-gradually',
        'how-to-guides/safe-remote-config',
        'how-to-guides/local-http-server-container',
        'how-to-guides/testing-features',
        'how-to-guides/ab-testing',
        'how-to-guides/custom-business-logic',
        'how-to-guides/debugging-determinism',
        'how-to-guides/namespace-isolation',
        'how-to-guides/handling-failures',
        'how-to-guides/publishing',
      ],
    },
    {
      type: 'category',
      label: 'Operate in production',
      items: [
        'production-operations/thread-safety',
        'production-operations/failure-modes',
        'production-operations/refresh-patterns',
        'production-operations/debugging',
        'observability/index',
        'observability/shadow-evaluation',
        'opentelemetry/index',
      ],
    },
    {
      type: 'category',
      label: 'Migration and rollout',
      items: [
        'overview/adoption-roadmap',
        'reference/migration-guide',
      ],
    },
    {
      type: 'category',
      label: 'Reference',
      items: [
        'core/reference',
        'reference/glossary',
        {
          type: 'category',
          label: 'API',
          items: [
            'reference/api/namespace-operations',
            'reference/api/feature-evaluation',
            'reference/api/parse-result',
            'reference/api/snapshot-loader',
            'reference/api/ramp-up-bucketing',
          ],
        },
        'reference/claims-registry',
        'serialization/reference',
        'observability/reference',
        'opentelemetry/reference',
        'konditional-spec/reference',
        {
          type: 'category',
          label: 'Modules',
          items: [
            'learn/core-primitives',
            'learn/evaluation-model',
            'learn/type-safety',
            'learn/configuration-lifecycle',
            'core/index',
            'core/rules',
            'core/types',
            'runtime/index',
            'runtime/lifecycle',
            'runtime/operations',
            'serialization/index',
            'serialization/persistence-format',
            'konditional-spec/index',
            'kontracts/index',
            'kontracts/schema-dsl',
          ],
        },
      ],
    },
    {
      type: 'category',
      label: 'Theory and guarantees',
      items: [
        'theory/parse-dont-validate',
        'theory/type-safety-boundaries',
        'theory/determinism-proofs',
        'theory/atomicity-guarantees',
        'theory/namespace-isolation',
        'theory/migration-and-shadowing',
      ],
    },
    {
      type: 'category',
      label: 'Troubleshooting',
      items: [
        'troubleshooting/index',
        'troubleshooting/bucketing-issues',
        'troubleshooting/parsing-issues',
        'troubleshooting/integration-issues',
      ],
    },
    {
      type: 'category',
      label: 'Examples and recipes',
      items: [
        'examples/golden-path',
        'advanced/recipes',
      ],
    },
  ],
};

export default sidebars;
