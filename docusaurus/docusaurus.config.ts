import {themes as prismThemes} from 'prism-react-renderer';
import type * as Preset from "@docusaurus/preset-classic";
import type {Config} from "@docusaurus/types";

const baseUrl = '/konditional'

const config: Config = {
    title: 'Konditional',
    tagline: 'Type-safe, deterministic feature flags for Kotlin',
    favicon: 'img/favicon.svg',

    url: 'https://amichne.github.io',
    baseUrl: baseUrl,
    trailingSlash: true,

    onBrokenMarkdownLinks: 'warn',
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
    },

    presets: [
        [
            "classic",
            {
                docs: {
                    sidebarPath: require.resolve("./sidebars.ts"),
                    editUrl: "https://github.com/amichne/konditional/tree/main/docusaurus/",
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
                        spec: 'openapi/openapi.json',
                        route: '/api/',
                    },
                ],
                theme: {
                    primaryColor: '#1890ff',
                },
            },
        ],
    ],

    themeConfig: {
        navbar: {
            title: 'Konditional',
            items: [
                {type: 'docSidebar', sidebarId: 'docs', position: 'left', label: 'Docs'},
                {to: '/api/', position: 'left', label: 'API'},
                {href: 'https://github.com/amichne/konditional', label: 'GitHub', position: 'right'},
            ],
        },
        footer: {
            style: 'dark',
            links: [
                {
                    title: 'Docs',
                    items: [{label: 'Konditional', to: '/docs/'}],
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
            additionalLanguages: ['kotlin'],
        },
    },
}

export default config
