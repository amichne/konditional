import {themes as prismThemes} from 'prism-react-renderer';
import type * as Preset from "@docusaurus/preset-classic";
import type {Config} from "@docusaurus/types";

const baseUrl = '/konditional/'

const config: Config = {
    title: 'Konditional',
    tagline: 'Type-safe, deterministic feature flags for Kotlin',
    favicon: 'img/favicon.svg',

    url: 'https://amichne.github.io',
    baseUrl: baseUrl,
    trailingSlash: true,

    onBrokenLinks: 'warn',

    organizationName: 'amichne',
    projectName: 'konditional',
    deploymentBranch: 'gh-pages',

    i18n: {
        defaultLocale: 'en',
        locales: ['en'],
    },

    markdown: {
        mermaid: true,
        hooks: {
            onBrokenMarkdownLinks: 'warn',
        },
    },

    themes: ['@docusaurus/theme-mermaid'],
    plugins: [
        require.resolve('docusaurus-lunr-search'),
    ],
    presets: [
        [
            "classic",
            {
                docs: {
                    routeBasePath: "/",
                    editUrl: "https://github.com/amichne/konditional/tree/main/docusaurus/",
                    sidebarPath: false
                },

                theme: {
                    customCss: "./src/css/custom.css",
                },
            } satisfies Preset.Options,
        ],
        [
            'redocusaurus',
            {
                specs: [
                    {
                        id: 'konditional-api',
                        spec: 'openapi/openapi.yaml',
                        route: '/api/',
                    },
                ],
            },
        ],
    ],

    themeConfig: {

        navbar: {
            title: 'Konditional',
            items: [
                {
                    type: "doc",
                    docId: "index",
                    label: "Home",
                },
                {
                    type: 'dropdown',
                    label: 'Getting Started',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'getting-started/installation', label: 'Installation'},
                        {type: 'doc', docId: 'getting-started/your-first-flag', label: 'Your First Flag'},
                        {type: 'doc', docId: 'getting-started/loading-from-json', label: 'Loading From JSON'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Fundamentals',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'fundamentals/core-primitives', label: 'Core Primitives'},
                        {type: 'doc', docId: 'fundamentals/trust-boundaries', label: 'Trust Boundaries'},
                        {
                            type: 'doc',
                            docId: 'fundamentals/definition-vs-initialization',
                            label: 'Definition vs Initialization'
                        },
                        {type: 'doc', docId: 'fundamentals/configuration-lifecycle', label: 'Configuration Lifecycle'},
                        {type: 'doc', docId: 'fundamentals/evaluation-semantics', label: 'Evaluation Semantics'},
                        {type: 'doc', docId: 'fundamentals/refresh-safety', label: 'Refresh Safety'},
                        {type: 'doc', docId: 'fundamentals/failure-modes', label: 'Failure Modes'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Rules & Targeting',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'rules-and-targeting/rule-composition', label: 'Rule Composition'},
                        {type: 'doc', docId: 'rules-and-targeting/specificity-system', label: 'Specificity System'},
                        {type: 'doc', docId: 'rules-and-targeting/rollout-strategies', label: 'Rollout Strategies'},
                        {type: 'doc', docId: 'rules-and-targeting/custom-extensions', label: 'Custom Extensions'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'API Reference',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'api-reference/feature-operations', label: 'Feature Operations'},
                        {type: 'doc', docId: 'api-reference/namespace-operations', label: 'Namespace Operations'},
                        {type: 'doc', docId: 'api-reference/serialization', label: 'Serialization'},
                        {type: 'doc', docId: 'api-reference/observability', label: 'Observability'},
                        {type: 'doc', docId: 'api-reference/core-types', label: 'Core Types'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Theory',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'theory/parse-dont-validate', label: "Parse, Don't Validate"},
                        {type: 'doc', docId: 'theory/atomicity-guarantees', label: 'Atomicity Guarantees'},
                        {type: 'doc', docId: 'theory/determinism-proofs', label: 'Determinism Proofs'},
                        {type: 'doc', docId: 'theory/type-safety-boundaries', label: 'Type-safety Boundaries'},
                        {type: 'doc', docId: 'theory/migration-and-shadowing', label: 'Migration and Shadowing'},
                        {type: 'doc', docId: 'theory/namespace-isolation', label: 'Namespace Isolation'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Advanced',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'advanced/multiple-namespaces', label: 'Multiple Namespaces'},
                        {type: 'doc', docId: 'advanced/shadow-evaluation', label: 'Shadow Evaluation'},
                        {type: 'doc', docId: 'advanced/custom-context-types', label: 'Custom Context Types'},
                        {type: 'doc', docId: 'advanced/kontracts-deep-dive', label: 'Kontracts Deep Dive'},
                        {type: 'doc', docId: 'advanced/testing-strategies', label: 'Testing Strategies'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Resources',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'why-konditional', label: 'Why Konditional'},
                        {type: 'doc', docId: 'migration', label: 'Migration'},
                        {type: 'doc', docId: 'persistence-format', label: 'Persistence Format'},
                        {type: 'doc', docId: 'glossary', label: 'Glossary'},
                    ],
                },
                {to: '/api/', position: 'left', label: 'API Schema'},
                {href: 'https://github.com/amichne/konditional', label: 'GitHub', position: 'right'},
            ],

        },
        footer: {
            style: 'dark',
            links: [
                {
                    title: 'Docs',
                    items: [{label: 'Konditional', to: '/'}],
                },
                {
                    title: 'More',
                    items: [{label: 'GitHub', href: 'https://github.com/amichne/konditional'}],
                },
            ],
            copyright: `Copyright © ${new Date().getFullYear()} amichne.`,
        },
        prism: {
            theme: prismThemes.github,
            darkTheme: prismThemes.dracula,
            magicComments: [
                // Remember to extend the default highlight class name as well!
                {
                    className: 'theme-code-block-highlighted-line',
                    line: 'highlight-next-line',
                    block: {start: 'highlight-start', end: 'highlight-end'},
                },
                {
                    className: 'code-block-error-line',
                    line: 'This will error',
                },
            ],

            additionalLanguages: ['kotlin'],
        },
    },
}

export default config
