import type {SidebarsConfig} from '@docusaurus/plugin-content-docs'

const sidebars: SidebarsConfig = {
    docs: [
        'index',
        {
            type: 'category',
            label: 'Getting Started',
            collapsed: false,
            items: [
                'getting-started/installation',
                'getting-started/your-first-flag',
                'getting-started/loading-from-json',
            ],
        },
        {
            type: 'category',
            label: 'Fundamentals',
            collapsed: false,
            items: [
                'fundamentals/core-primitives',
                'fundamentals/trust-boundaries',
                'fundamentals/definition-vs-initialization',
                'fundamentals/configuration-lifecycle',
                'fundamentals/evaluation-semantics',
                'fundamentals/refresh-safety',
                'fundamentals/failure-modes',
            ],
        },
        {
            type: 'category',
            label: 'Rules & Targeting',
            collapsed: true,
            items: [
                'rules-and-targeting/rule-composition',
                'rules-and-targeting/specificity-system',
                'rules-and-targeting/rollout-strategies',
                'rules-and-targeting/custom-extensions',
            ],
        },
        {
            type: 'category',
            label: 'API Reference',
            collapsed: true,
            items: [
                'api-reference/feature-operations',
                'api-reference/namespace-operations',
                'api-reference/serialization',
                'api-reference/observability',
                'api-reference/core-types',
            ],
        },
        {
            type: 'category',
            label: 'Theory',
            collapsed: true,
            items: [
                'theory/parse-dont-validate',
                'theory/atomicity-guarantees',
                'theory/determinism-proofs',
                'theory/type-safety-boundaries',
                'theory/migration-and-shadowing',
                'theory/namespace-isolation',
            ],
        },
        {
            type: 'category',
            label: 'Advanced',
            collapsed: true,
            items: [
                'advanced/multiple-namespaces',
                'advanced/shadow-evaluation',
                'advanced/custom-context-types',
                'advanced/kontracts-deep-dive',
                'advanced/testing-strategies',
            ],
        },
        'glossary',
        {
            type: 'category',
            label: 'Additional Resources',
            collapsed: true,
            items: [
                'why-konditional',
                'migration',
                'persistence-format',
            ],
        },
    ],
}

export default sidebars
