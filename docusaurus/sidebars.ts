import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
    docsSidebar: [
        {type: 'doc', id: 'index', label: 'Home'},
        {
            type: 'category',
            label: 'Quick Start',
            items: [
                'quick-start/what-is-konditional',
                'getting-started/installation',
                'getting-started/your-first-flag',
            ],
        },
        {
            type: 'category',
            label: 'Fundamentals',
            items: [
                'fundamentals/core-primitives',
                'fundamentals/evaluation-semantics',
                'fundamentals/type-safety',
                'fundamentals/configuration-lifecycle',
            ],
        },
        {
            type: 'category',
            label: 'How-To Guides',
            items: [
                'how-to-guides/rolling-out-gradually',
                'how-to-guides/ab-testing',
                'how-to-guides/safe-remote-config',
                'how-to-guides/handling-failures',
                'how-to-guides/debugging-determinism',
                'how-to-guides/custom-business-logic',
                'how-to-guides/namespace-isolation',
                'how-to-guides/testing-features',
                'how-to-guides/publishing',
            ],
        },
        {
            type: 'category',
            label: 'Core Library',
            items: [
                'core/index',
                'core/reference',
                'core/rules',
                'core/types',
            ],
        },
        {
            type: 'category',
            label: 'Production Operations',
            items: [
                'production-operations/thread-safety',
                'production-operations/failure-modes',
                'production-operations/refresh-patterns',
                'production-operations/debugging',
            ],
        },
        {
            type: 'category',
            label: 'Modules',
            items: [
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
                        'serialization/reference',
                    ],
                },
                {
                    type: 'category',
                    label: 'Observability',
                    items: [
                        'observability/index',
                        'observability/shadow-evaluation',
                        'observability/reference',
                    ],
                },
                {
                    type: 'category',
                    label: 'OpenTelemetry',
                    items: [
                        'opentelemetry/index',
                        'opentelemetry/reference',
                    ],
                },
                {
                    type: 'category',
                    label: 'Config Metadata',
                    items: [
                        'config-metadata/index',
                        'config-metadata/reference',
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
            label: 'Design & Safety',
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
            label: 'Reference',
            items: [
                'reference/glossary',
                {
                    type: 'category',
                    label: "Recipes",
                    link: {type: 'doc', id: 'advanced/recipes'},
                    items: [
                        // Link each recipe to the corresponding anchor in the aggregated recipes doc
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
                'reference/migration-guide',
            ],
        },
    ],
};

export default sidebars;
