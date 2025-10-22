# Konditional Documentation

This directory contains the complete documentation for the Konditional serialization system, built with [Astro](https://astro.build) and [Starlight](https://starlight.astro.build).

## 📚 Documentation Structure

The documentation is organized into three main formats:

### 1. **Step-by-Step Guide** (8 Steps)
Individual pages that can be followed sequentially:
- Step 1: Dependencies
- Step 2: Register Flags
- Step 3: Create Configuration
- Step 4: Serialize
- Step 5: Deserialize
- Step 6: Load into Runtime
- Step 7: Testing
- Step 8: Production Setup

### 2. **Integration Guide**
Conceptual overview of integration patterns, architecture decisions, and best practices.

### 3. **Full Runthrough**
Complete end-to-end integration in a single page with all code examples.

## 🚀 Quick Start

### Install Dependencies

```bash
cd docs
npm install
```

### Development Server

Start the development server with hot reload:

```bash
npm run dev
```

The documentation will be available at `http://localhost:4321`

### Build for Production

Build the static site:

```bash
npm run build
```

Output will be in `docs/dist/`

### Preview Production Build

Preview the production build locally:

```bash
npm run preview
```

## 📖 Content Structure

```
docs/
├── src/
│   └── content/
│       └── docs/
│           ├── serialization/
│           │   ├── overview.mdx           # Overview of serialization
│           │   ├── integration.mdx        # Integration guide
│           │   ├── runthrough.mdx         # Complete runthrough
│           │   ├── api.mdx                # API reference
│           │   └── steps/
│           │       ├── step-01-dependencies.mdx
│           │       ├── step-02-register.mdx
│           │       ├── step-03-configuration.mdx
│           │       ├── step-04-serialize.mdx
│           │       ├── step-05-deserialize.mdx
│           │       ├── step-06-load.mdx
│           │       ├── step-07-testing.mdx
│           │       └── step-08-production.mdx
│           └── getting-started/
│               ├── introduction.mdx
│               └── quick-start.mdx
├── astro.config.mjs                       # Astro configuration
├── package.json                           # Dependencies
└── tsconfig.json                          # TypeScript config
```

## ✨ Features

- **Search** - Full-text search across all documentation
- **Dark Mode** - Automatic dark/light mode support
- **Mobile Responsive** - Works on all device sizes
- **Code Highlighting** - Syntax highlighting for Kotlin, JSON, YAML, etc.
- **Navigation** - Sidebar navigation with collapsible sections
- **Previous/Next Links** - Easy navigation between steps

## 🎨 Customization

### Update Site Title

Edit `astro.config.mjs`:

```javascript
starlight({
  title: 'Your Project Name',
  // ...
})
```

### Add New Pages

Create a new `.mdx` file in `src/content/docs/`:

```mdx
---
title: My New Page
description: Description of the page
---

## Content here
```

Update `astro.config.mjs` to add to sidebar:

```javascript
sidebar: [
  {
    label: 'My Section',
    items: [
      { label: 'My New Page', link: '/path/to/page/' }
    ]
  }
]
```

## 📝 Writing Documentation

### MDX Format

Documentation files use MDX, which allows you to mix Markdown with JSX components.

### Starlight Components

Use built-in Starlight components:

```mdx
import { Aside, Card, Code, FileTree } from '@astrojs/starlight/components';

<Aside type="tip">
Helpful tip for readers
</Aside>

<Card title="Feature Name">
Description of the feature
</Card>
```

### Code Blocks

Use syntax highlighting:

````mdx
```kotlin
fun example() {
    println("Hello")
}
```
````

Add file names:

````mdx
```kotlin title="MyFile.kt"
class MyClass
```
````

Highlight lines:

````mdx
```kotlin {2-3}
fun example() {
    val highlighted = true
    val alsoHighlighted = true
}
```
````

## 🔧 Development Tips

### Hot Reload

The dev server automatically reloads when you edit files. Just save and see changes instantly.

### Check Build

Always test the production build before deploying:

```bash
npm run build
npm run preview
```

### Validate Links

Ensure all internal links work correctly by testing navigation in the preview.

## 📦 Deployment

### Deploy to Vercel

1. Push to GitHub
2. Import project in Vercel
3. Set build command: `npm run build`
4. Set output directory: `dist`

### Deploy to Netlify

1. Push to GitHub
2. Connect repository in Netlify
3. Set build command: `npm run build`
4. Set publish directory: `dist`

### Deploy to GitHub Pages

Add to `astro.config.mjs`:

```javascript
export default defineConfig({
  site: 'https://yourusername.github.io',
  base: '/konditional',
  // ...
});
```

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy to GitHub Pages

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - run: npm run build
      - uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./dist
```

## 🤝 Contributing

To contribute to the documentation:

1. Edit files in `src/content/docs/`
2. Test locally with `npm run dev`
3. Build and preview with `npm run build && npm run preview`
4. Submit a pull request

## 📄 License

Same as the main Konditional project.

## 🔗 Links

- [Astro Documentation](https://docs.astro.build)
- [Starlight Documentation](https://starlight.astro.build)
- [Konditional Repository](https://github.com/amichne/konditional)
