# Konditional Documentation

This documentation is built with [MkDocs](https://www.mkdocs.org/) and [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/).

## Setup

### Install Dependencies

```bash
pip install -r requirements.txt
```

Or using a virtual environment:

```bash
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

## Development

### Serve Locally

```bash
mkdocs serve
```

Then open http://127.0.0.1:8000 in your browser.

### Build

```bash
mkdocs build
```

This generates the static site in the `site/` directory.

## Deploy

### GitHub Pages

```bash
mkdocs gh-deploy
```

This builds and pushes to the `gh-pages` branch.

## Project Structure

```
docs/
├── mkdocs.yml          # Configuration
├── requirements.txt    # Python dependencies
├── docs/              # Markdown source files
│   ├── index.md
│   ├── getting-started/
│   ├── serialization/
│   └── advanced/
└── site/              # Generated site (gitignored)
```
