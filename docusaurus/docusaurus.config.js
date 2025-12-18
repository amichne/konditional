// @ts-check
// Note: We keep this JS-first for a smaller initial surface area.

const path = require('path')

const themeClassicRoot = path.dirname(require.resolve('@docusaurus/theme-classic/package.json'))
const prism = require(require.resolve('prism-react-renderer', { paths: [themeClassicRoot] }))
const lightCodeTheme = prism.themes.github
const darkCodeTheme = prism.themes.dracula

const config = {
  title: 'Konditional',
  tagline: 'Type-safe, deterministic feature flags for Kotlin',
  favicon: 'img/favicon.svg',

  url: 'https://amichne.github.io',
  baseUrl: '/konditional/',

  organizationName: 'amichne',
  projectName: 'konditional',

  onBrokenLinks: 'throw',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  markdown: {
    mermaid: true,
    hooks: {
      onBrokenMarkdownLinks: 'throw',
    },
  },
  themes: ['@docusaurus/theme-mermaid'],

  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: '/',
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: 'https://github.com/amichne/konditional/edit/main/docusaurus/',
        },
        blog: false,
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      },
    ],
  ],

  themeConfig: {
    navbar: {
      title: 'Konditional',
      items: [
        { type: 'docSidebar', sidebarId: 'docs', position: 'left', label: 'Docs' },
        { href: 'https://github.com/amichne/konditional', label: 'GitHub', position: 'right' },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [{ label: 'Konditional', to: '/' }],
        },
        {
          title: 'More',
          items: [{ label: 'GitHub', href: 'https://github.com/amichne/konditional' }],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} amichne.`,
    },
    prism: {
      theme: lightCodeTheme,
      darkTheme: darkCodeTheme,
      additionalLanguages: ['kotlin'],
    },
  },
}

module.exports = config
