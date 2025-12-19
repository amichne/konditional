// import path from 'node:path'
// docusaurus.config.ts
import type {Config} from '@docusaurus/types';
import type * as Redocusaurus from 'redocusaurus';
import {themes as prismThemes} from 'prism-react-renderer';
import path from "node:path";

const baseUrl = '/konditional/'

const config: Config = {

    title: 'Konditional',
    tagline: 'Type-safe, deterministic feature flags for Kotlin',
    favicon: 'img/favicon.svg',

    url: 'https://amichne.github.io',
    baseUrl,

    onBrokenMarkdownLinks: 'warn',
    onBrokenLinks: 'throw',

    organizationName: 'amichne',
    projectName: 'konditional',

    i18n: {
        defaultLocale: 'en',
        locales: ['en'],
    },

    plugins: [],

    markdown: {
        mermaid: true,
    },

    themes: ['@docusaurus/theme-mermaid', "docusaurus-theme-openapi-docs"],
    presets: [
        [
            'classic',
            {
                docs: {
                    routeBasePath: '/',
                    sidebarPath: path.resolve(__dirname, 'sidebars.ts'),
                    editUrl: 'https://github.com/amichne/konditional/edit/main/docusaurus/',
                },
                blog: false,
                theme: {
                    customCss: path.resolve(__dirname, 'src/css/custom.css'),
                },
            },
        ],

        [
            'redocusaurus',
            {
                id: "api",
                openapi: {
                    // Folder to scan for *.openapi.yaml files
                    path: 'openapi',
                    routeBasePath: '/api',
                },
                specs: [
                    // Optionally provide individual files/urls to load
                    {
                        // Pass it a path to a local OpenAPI YAML file
                        spec: 'docusaurus/openapi/openapi.json',
                        id: 'from-manual-file',
                        route: '/api/from-manual-file',
                    },
                    // You can also pass it an OpenAPI spec URL
                    {
                        spec: 'https://redocly.github.io/redoc/openapi.yaml',
                        id: 'from-remote-file',
                        route: '/api/from-remote-file',
                    },
                ],
                // Theme Options for modifying how redoc renders them
                theme: {
                    // Change with your site colors
                    primaryColor: '#1890ff',
                },
            },
        ] satisfies Redocusaurus.PresetEntry,
    ],

    themeConfig: {
        navbar: {
            title: 'Konditional',
            items: [
                {type: 'docSidebar', sidebarId: 'docs', position: 'left', label: 'Docs'},
                {to: '/api/from-manual-file', label: 'API', position: 'left'},
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
            additionalLanguages: ['kotlin'],
        },
    },
}

export default config
