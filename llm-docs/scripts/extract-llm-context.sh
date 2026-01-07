#!/bin/bash
# llm-docs/scripts/extract-llm-context.sh
#
# Extracts key type signatures and API surface for LLM context injection.
# Run this after significant API changes to keep llm-docs/context/ current.

set -euo pipefail

PROJECT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
CONTEXT_DIR="$PROJECT_ROOT/llm-docs/context"

mkdir -p "$CONTEXT_DIR"

echo "Extracting LLM context..."

CORE_TYPES_FILE="$CONTEXT_DIR/core-types.kt"
PUBLIC_API_FILE="$CONTEXT_DIR/public-api-surface.md"

# Extract core type signatures (classes, interfaces, objects, functions)
echo "# Core Type Signatures" > "$CORE_TYPES_FILE"
echo "# Extracted: $(date -Iseconds)" >> "$CORE_TYPES_FILE"
echo "# Source: modules/*/src/main/kotlin" >> "$CORE_TYPES_FILE"
echo "" >> "$CORE_TYPES_FILE"

TYPE_DECLARATION_REGEX="^(public[[:space:]]+)?(sealed[[:space:]]+|data[[:space:]]+|abstract[[:space:]]+|open[[:space:]]+|value[[:space:]]+|enum[[:space:]]+|annotation[[:space:]]+|inline[[:space:]]+)?(class|interface|object|fun)[[:space:]]"
MAX_LINES_PER_MODULE=300
MODULES=(konditional-core konditional-runtime konditional-serialization konditional-observability config-metadata kontracts openapi opentelemetry)

for module in "${MODULES[@]}"; do
    module_source="$PROJECT_ROOT/$module/src/main/kotlin"
    if [ -d "$module_source" ]; then
        echo "# Module: $module" >> "$CORE_TYPES_FILE"
        if command -v rg >/dev/null 2>&1; then
            rg -n --no-heading --glob "*.kt" "$TYPE_DECLARATION_REGEX" "$module_source" \
                | head -n "$MAX_LINES_PER_MODULE" \
                | sed "s|$PROJECT_ROOT/||" >> "$CORE_TYPES_FILE" || true
        else
            grep -Ern "$TYPE_DECLARATION_REGEX" "$module_source" \
                --include="*.kt" \
                2>/dev/null | head -n "$MAX_LINES_PER_MODULE" \
                | sed "s|$PROJECT_ROOT/||" >> "$CORE_TYPES_FILE" || true
        fi
        echo "" >> "$CORE_TYPES_FILE"
    fi
done

echo "# --- End of extraction ---" >> "$CORE_TYPES_FILE"

# Create a summary of public API surface from existing docs
echo "# Public API Surface Summary" > "$PUBLIC_API_FILE"
echo "# Extracted: $(date -Iseconds)" >> "$PUBLIC_API_FILE"
echo "" >> "$PUBLIC_API_FILE"

DOCS_DIR="$PROJECT_ROOT/docusaurus/docs"
DOC_SOURCES=(
    "$DOCS_DIR/index.md"
    "$DOCS_DIR/getting-started/installation.md"
    "$DOCS_DIR/getting-started/your-first-flag.md"
    "$DOCS_DIR/fundamentals/core-primitives.md"
    "$DOCS_DIR/fundamentals/evaluation-semantics.md"
    "$DOCS_DIR/api-reference/core-types.md"
    "$DOCS_DIR/api-reference/feature-operations.md"
    "$DOCS_DIR/api-reference/namespace-operations.md"
    "$DOCS_DIR/api-reference/serialization.md"
    "$DOCS_DIR/api-reference/observability.md"
)

for doc in "${DOC_SOURCES[@]}"; do
    if [ -f "$doc" ]; then
        relative_doc="${doc#"$PROJECT_ROOT/"}"
        echo "## From $relative_doc" >> "$PUBLIC_API_FILE"
        echo "" >> "$PUBLIC_API_FILE"
        cat "$doc" >> "$PUBLIC_API_FILE"
        echo "" >> "$PUBLIC_API_FILE"
    fi
done

# Record last update timestamp
echo "$(date -Iseconds)" > "$CONTEXT_DIR/.last-updated"

echo "Context extraction complete."
echo "  - $CORE_TYPES_FILE"
echo "  - $PUBLIC_API_FILE"
echo "  - Last updated: $(cat "$CONTEXT_DIR/.last-updated")"
