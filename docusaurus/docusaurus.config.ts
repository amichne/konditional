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
        [
            require.resolve('@docusaurus/plugin-client-redirects'),
            {
                redirects: [
                    {from: '/getting-started', to: '/quickstart'},
                    {from: '/getting-started/installation', to: '/quickstart/install'},
                    {from: '/getting-started/your-first-flag', to: '/quickstart/define-first-flag'},
                    {from: '/quick-start/what-is-konditional', to: '/overview/start-here'},
                    {from: '/guides/install-and-setup', to: '/quickstart/install'},
                    {from: '/guides/roll-out-gradually', to: '/quickstart/add-deterministic-ramp-up'},
                    {from: '/guides/load-remote-config', to: '/guides/remote-configuration'},
                    {from: '/guides/debug-evaluation', to: '/reference/evaluation-diagnostics'},
                    {from: '/guides/test-features', to: '/guides/testing-strategies'},
                    {from: '/design-theory/determinism-proofs', to: '/theory/determinism-proofs'},
                    {from: '/design-theory/parse-dont-validate', to: '/theory/parse-dont-validate'},
                    {from: '/advanced/shadow-evaluation', to: '/theory/migration-and-shadowing'},
                    {from: '/api-reference/observability', to: '/reference/evaluation-diagnostics'},
                ],
            },
        ],
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
                    label: 'Start here',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'overview/start-here', label: 'Start here'},
                        {type: 'doc', docId: 'overview/product-value-fit', label: 'Product value and fit'},
                        {type: 'doc', docId: 'overview/first-success-map', label: 'First success map'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Quickstart',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'quickstart/index', label: 'Quickstart'},
                        {type: 'doc', docId: 'quickstart/verify-end-to-end', label: 'Verify end-to-end'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Understand',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'concepts/namespaces', label: 'Namespaces'},
                        {type: 'doc', docId: 'concepts/features-and-types', label: 'Features and types'},
                        {type: 'doc', docId: 'concepts/rules-and-precedence', label: 'Rules and precedence'},
                        {type: 'doc', docId: 'concepts/context-and-targeting', label: 'Context and targeting'},
                        {type: 'doc', docId: 'concepts/evaluation-model', label: 'Evaluation model'},
                        {type: 'doc', docId: 'concepts/configuration-lifecycle', label: 'Configuration lifecycle'},
                    ],
                },
                {
                    type: 'dropdown',
                    label: 'Do',
                    position: 'left',
                    items: [
                        {type: 'doc', docId: 'guides/remote-configuration', label: 'Remote configuration'},
                        {type: 'doc', docId: 'guides/incremental-updates', label: 'Incremental updates'},
                        {type: 'doc', docId: 'guides/custom-structured-values', label: 'Custom structured values'},
                        {type: 'doc', docId: 'guides/custom-targeting-axes', label: 'Custom targeting axes'},
                        {type: 'doc', docId: 'guides/testing-strategies', label: 'Testing strategies'},
                        {type: 'doc', docId: 'guides/migration-from-legacy', label: 'Migration from legacy'},
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
                    to: '/api/',
                    label: 'OpenAPI Spec',
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
