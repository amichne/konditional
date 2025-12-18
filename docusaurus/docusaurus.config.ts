import path from 'node:path'
import {fileURLToPath} from 'node:url'
import {themes as prismThemes} from 'prism-react-renderer'
import type {Config} from '@docusaurus/types'
import type {ScalarOptions} from '@scalar/docusaurus'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const baseUrl = '/konditional/'

const scalarPlugins: [string, ScalarOptions][] = [
    [
        '@scalar/docusaurus',
        {
            label: 'API',
            route: '/api',
            cdn: 'https://cdn.jsdelivr.net/npm/@scalar/api-reference@1.28.11',
            showNavLink: false,
            configuration: {
                url: `${baseUrl}openapi.json`,
            },
        },
    ],
]

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

    plugins: scalarPlugins,

    markdown: {
        mermaid: true,
    },

    themes: ['@docusaurus/theme-mermaid'],

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
    ],

    themeConfig: {
        navbar: {
            title: 'Konditional',
            items: [
                {type: 'docSidebar', sidebarId: 'docs', position: 'left', label: 'Docs'},
                {to: '/api', label: 'API', position: 'left'},
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
