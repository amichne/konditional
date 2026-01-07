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
                'advanced/recipes',
                'reference/migration-guide',
            ],
        },
    ],
};

export default sidebars;
