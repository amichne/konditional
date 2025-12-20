import type * as Preset from "@docusaurus/preset-classic";
import type {Config} from "@docusaurus/types";
import type * as OpenApiPlugin from "docusaurus-plugin-openapi-docs";
import {themes as prismThemes} from 'prism-react-renderer';

const baseUrl = '/'

const config: Config = {

    title: 'Konditional',
    tagline: 'Type-safe, deterministic feature flags for Kotlin',
    favicon: 'img/favicon.svg',

    url: 'https://amichne.github.io',
    baseUrl: baseUrl,
    trailingSlash: true,

    onBrokenMarkdownLinks: 'warn',
    onBrokenLinks: 'throw',

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

    plugins: [
        [
            'docusaurus-plugin-openapi-docs',
            {
                id: "api", // plugin id
                docsPluginId: "classic", // configured for preset-classic
                config: {
                    snapshot: {
                        specPath: "openapi/openapi.json",
                        outputDir: "docs/openapi",
                        sidebarOptions: {
                            groupPathsBy: "tag",
                        },
                    } satisfies OpenApiPlugin.Options,
                }
            },
        ]
    ],
    themes: ["docusaurus-theme-openapi-docs"], // export theme components
    presets: [
    [
      "classic",
      {
        docs: {
          sidebarPath: "./sidebars.ts",
          docItemComponent: "@theme/ApiItem", // Derived from docusaurus-theme-openapi
          routeBasePath: '/',
        },
        theme: {
          customCss: "./src/css/custom.css",
        },
      } satisfies Preset.Options,
    ],
  ],

    themeConfig: {
        navbar: {
            title: 'Konditional',
            items: [
                {type: 'docSidebar', sidebarId: 'docs', position: 'left', label: 'Docs'},
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
