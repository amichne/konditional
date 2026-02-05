import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
    docsSidebar: [
       {type: 'doc', id: 'index', label: 'Home'},
        {
            type: 'category',
            label: 'Welcome',
            items: [
                'value-proposition',
            ],
        },
        {
            type: 'category',
            label: 'Quick Start',
            items: [
                'quick-start',
            ],
        },
        {
            type: 'category',
            label: 'Konditional Core',
            items: [
                'core-concepts',
                'evaluation-flow',
                'rules',
                'context-and-axes',
                'rollouts-and-bucketing',
                'registry-and-configuration',
                'dsl-authoring',
                'structured-values',
                'parsing-and-errors',
            ],
        },
        {
            type: 'category',
            label: 'Operations',
            items: [
                'observability-and-debugging',
            ],
        },
        {
            type: 'category',
            label: 'Recipes',
            items: [
                'recipes',
            ],
        },
        {
            type: 'category',
            label: 'Reference',
            items: [
                'api-reference',
                'glossary',
                'reference-index',
                'faq',
                'next-steps',
            ],
        },
    ],
};

export default sidebars;
