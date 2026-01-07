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
                    link: { type: 'doc', id: 'runtime/index' },
                    items: [
                        'runtime/lifecycle',
                        'runtime/operations',
                    ],
                },
                {
                    type: 'category',
                    label: 'Serialization',
                    link: { type: 'doc', id: 'serialization/index' },
                    items: [
                        'serialization/persistence-format',
                        'serialization/reference',
                    ],
                },
                {
                    type: 'category',
                    label: 'Observability',
                    link: { type: 'doc', id: 'observability/index' },
                    items: [
                        'observability/shadow-evaluation',
                        'observability/reference',
                    ],
                },
                {
                    type: 'category',
                    label: 'OpenTelemetry',
                    link: { type: 'doc', id: 'opentelemetry/index' },
                    items: [
                        'opentelemetry/reference',
                    ],
                },
                {
                    type: 'category',
                    label: 'Config Metadata',
                    link: { type: 'doc', id: 'config-metadata/index' },
                    items: [
                        'config-metadata/reference',
                    ],
                },
                {
                    type: 'category',
                    label: 'Kontracts',
                    link: { type: 'doc', id: 'kontracts/index' },
                    items: [
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
                    label: 'Recipes',
                    link: { type: 'doc', id: 'advanced/recipes/index' },
                    items: [
                        'advanced/recipes/typed-variants',
                        'advanced/recipes/rampup',
                        'advanced/recipes/reset',
                        'advanced/recipes/axes',
                        'advanced/recipes/extension',
                        'advanced/recipes/structured',
                        'advanced/recipes/load',
                        'advanced/recipes/rollback',
                        'advanced/recipes/shadow',
                        'advanced/recipes/namespace',
                        'advanced/recipes/observability',
                    ],
                },
                'reference/migration-guide',
            ],
        },
    ],
};

export default sidebars;
