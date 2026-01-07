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
                    sidebarPath: require.resolve("./sidebars.ts"),
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
                        {type: 'doc', docId: 'getting-started/your-first-flag', label: 'Your First Feature'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Core',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'core/index', label: 'Core Module'},
                        {type: 'doc', docId: 'fundamentals/core-primitives', label: 'Core Concepts'},
                        {type: 'doc', docId: 'fundamentals/evaluation-semantics', label: 'Evaluation Model'},
                        {type: 'doc', docId: 'core/rules', label: 'Rule DSL'},
                        {type: 'doc', docId: 'core/reference', label: 'Core API Reference'},
                        {type: 'doc', docId: 'core/types', label: 'Core Types'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Theory',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'theory/parse-dont-validate', label: 'Parse, Don\'t Validate'},
                        {type: 'doc', docId: 'theory/type-safety-boundaries', label: 'Type Safety Boundaries'},
                        {type: 'doc', docId: 'theory/determinism-proofs', label: 'Determinism Proofs'},
                        {type: 'doc', docId: 'theory/atomicity-guarantees', label: 'Atomicity Guarantees'},
                        {type: 'doc', docId: 'theory/namespace-isolation', label: 'Namespace Isolation'},
                        {type: 'doc', docId: 'theory/migration-and-shadowing', label: 'Migration and Shadowing'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Modules',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'runtime/index', label: 'Runtime'},
                        {type: 'doc', docId: 'serialization/index', label: 'Serialization'},
                        {type: 'doc', docId: 'observability/index', label: 'Observability'},
                        {type: 'doc', docId: 'opentelemetry/index', label: 'OpenTelemetry'},
                        {type: 'doc', docId: 'config-metadata/index', label: 'Config Metadata'},
                        {type: 'doc', docId: 'kontracts/index', label: 'Kontracts'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Advanced',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'advanced/recipes', label: 'Recipes'},
                    ],
                },
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
