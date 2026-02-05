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
                    path: "./docs",
                    routeBasePath: "/",
                    editUrl: "https://github.com/amichne/konditional/tree/main/docs/",
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
                    type: "doc",
                    docId: "value-proposition",
                    label: "Value",
                },
                {
                    type: "doc",
                    docId: "quick-start",
                    label: "Quick Start",
                },
                {
                    type: "doc",
                    docId: "core-concepts",
                    label: "Core",
                },
                {
                    type: "doc",
                    docId: "recipes",
                    label: "Recipes",
                },
                {
                    type: "doc",
                    docId: "reference-index",
                    label: "Reference",
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
