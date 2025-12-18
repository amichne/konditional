import path from 'node:path'
import {fileURLToPath} from 'node:url'
import createJiti from 'jiti'
import type {SidebarsConfig} from '@docusaurus/plugin-content-docs'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const loadOpenApiSidebar = (): string[] => {
  const sidebarPath = path.join(__dirname, 'docs', 'api', 'sidebar.ts')
  try {
    const load = createJiti(__filename, {
      cache: true,
      requireCache: false,
      interopDefault: true,
    })
    const sidebar = load(sidebarPath)
    if (Array.isArray(sidebar)) {
      return sidebar
    }
    if (Array.isArray(sidebar?.default)) {
      return sidebar.default
    }
    return []
  } catch (error) {
    return []
  }
}

const openApiSidebar = loadOpenApiSidebar()

const sidebars: SidebarsConfig = {
  docs: [
    'index',
    'getting-started',
    'migration',
    'core-concepts',
    'targeting-ramp-ups',
    'evaluation',
    'remote-config',
    'theory',
    'persistence-format',
    'why-konditional',
    'api-reference',
    {
      type: 'category',
      label: 'API Reference',
      link: { type: 'doc', id: 'api-reference' },
      items: openApiSidebar,
    },
  ],
}

export default sidebars
