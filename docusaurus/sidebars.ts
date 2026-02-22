import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
    docsSidebar: [
        {
            type: 'category',
            label: 'Welcome',
            items: [
                {type: 'doc', id: 'index', label: 'What is Konditional?'},
            ],
        },
        {
            type: 'category',
            label: 'Getting Started (10-Minute Path)',
            items: [
                'getting-started/index',
                'getting-started/installation',
                'getting-started/your-first-flag',
            ],
        },
        {
            type: 'category',
            label: 'Guides',
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
            label: 'Operate in Production',
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
            label: 'Value Journeys',
            items: [
                'value-journeys/index',
                'value-journeys/jv-001-confident-rollouts',
                'value-journeys/jv-002-safe-snapshot-ingestion',
            ],
        },
        {
            type: 'category',
            label: 'API Reference',
            items: [
                {
                    type: 'category',
                    label: 'Introduction',
                    items: [
                        'core/reference',
                        'reference/glossary',
                    ],
                },
                {
                    type: 'category',
                    label: 'Endpoints and Operations',
                    items: [
                        'reference/api/namespace-operations',
                        'reference/api/parse-result',
                        'reference/api/snapshot-loader',
                        'reference/api/ramp-up-bucketing',
                    ],
                },
                'serialization/reference',
                'observability/reference',
                'konditional-spec/reference',
            ],
        },
        {
            type: 'category',
            label: 'Theory and Guarantees',
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
            label: 'Updates and Troubleshooting',
            items: [
                'reference/migration-guide',
                'troubleshooting/index',
            ],
        },
        {
            type: 'category',
            label: 'Deep Dive Modules',
            items: [
                {
                    type: 'category',
                    label: 'Learn',
                    items: [
                        'learn/core-primitives',
                        'learn/evaluation-model',
                        'learn/type-safety',
                        'learn/configuration-lifecycle',
                    ],
                },
                {
                    type: 'category',
                    label: 'Core Library',
                    items: [
                        'core/index',
                        'core/rules',
                        'core/types',
                    ],
                },
                {
                    type: 'category',
                    label: 'Runtime',
                    items: [
                        'runtime/index',
                        'runtime/lifecycle',
                        'runtime/operations',
                    ],
                },
                {
                    type: 'category',
                    label: 'Serialization',
                    items: [
                        'serialization/index',
                        'serialization/persistence-format',
                    ],
                },
                {
                    type: 'category',
                    label: 'OpenTelemetry',
                    items: [
                        'opentelemetry/reference',
                    ],
                },
                {
                    type: 'category',
                    label: 'Konditional Spec',
                    items: [
                        'konditional-spec/index',
                    ],
                },
                {
                    type: 'category',
                    label: 'Kontracts',
                    items: [
                        'kontracts/index',
                        'kontracts/schema-dsl',
                    ],
                },
            ],
        },
        {
            type: 'category',
            label: 'Examples',
            items: [
                'examples/golden-path',
            ],
        },
        {
            type: 'category',
            label: 'Recipes',
            link: {type: 'doc', id: 'advanced/recipes'},
            items: [
                {
                    type: 'link',
                    label: 'Typed Variants Instead of Boolean Explosion',
                    href: '#recipe-1-typed-variants-instead-of-boolean-explosion'
                },
                {
                    type: 'link',
                    label: 'Deterministic Ramp-Up with Resettable Salt',
                    href: '#recipe-2-deterministic-ramp-up-with-resettable-salt'
                },
                {
                    type: 'link',
                    label: 'Runtime-Configurable Segments via Axes',
                    href: '#recipe-3-runtime-configurable-segments-via-axes'
                },
                {
                    type: 'link',
                    label: 'Business Logic Targeting with Custom Context + Extension',
                    href: '#recipe-4-business-logic-targeting-with-custom-context-extension'
                },
                {
                    type: 'link',
                    label: 'Structured Values with Schema Validation',
                    href: '#recipe-5-structured-values-with-schema-validation'
                },
                {
                    type: 'link',
                    label: 'Safe Remote Config Loading + Rollback',
                    href: '#recipe-6-safe-remote-config-loading-rollback'
                },
                {
                    type: 'link',
                    label: 'Controlled Migrations with Shadow Evaluation',
                    href: '#recipe-7-controlled-migrations-with-shadow-evaluation'
                },
                {
                    type: 'link',
                    label: 'Namespace Isolation + Kill-Switch',
                    href: '#recipe-8-namespace-isolation-kill-switch'
                },
                {
                    type: 'link',
                    label: 'Lightweight Observability Hooks',
                    href: '#recipe-9-lightweight-observability-hooks'
                },
            ],
        },
    ],
};

export default sidebars;
