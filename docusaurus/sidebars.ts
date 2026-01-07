import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
    docsSidebar: [
        {type: 'doc', id: 'index', label: 'Home'},
        {
            type: 'category',
            label: 'Getting Started',
            items: [
                'getting-started/installation',
                'getting-started/your-first-flag',
            ],
        },
        {
            type: 'category',
            label: 'Core',
            items: [
                'core/index',
                'fundamentals/core-primitives',
                'fundamentals/evaluation-semantics',
                'core/rules',
                'core/reference',
                'core/types',
            ],
        },
        {
            type: 'category',
            label: 'Theory',
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
            label: 'Modules',
            items: [
                {
                    type: 'category',
                    label: 'Runtime',
                    items: [
                        'runtime/index',
                        'runtime/operations',
                        'runtime/lifecycle',
                    ],
                },
                {
                    type: 'category',
                    label: 'Serialization',
                    items: [
                        'serialization/index',
                        'serialization/reference',
                        'serialization/persistence-format',
                    ],
                },
                {
                    type: 'category',
                    label: 'Observability',
                    items: [
                        'observability/index',
                        'observability/reference',
                        'observability/shadow-evaluation',
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
            label: 'Advanced',
            items: [
                'advanced/recipes',
            ],
        },
    ],
};

export default sidebars;
