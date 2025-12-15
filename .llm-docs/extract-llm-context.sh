#!/bin/bash
# scripts/extract-llm-context.sh
#
# Extracts key type signatures and API surface for LLM context injection.
# Run this after significant API changes to keep .llm-docs/context/ current.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONTEXT_DIR="$PROJECT_ROOT/.llm-docs/context"

mkdir -p "$CONTEXT_DIR"

echo "Extracting LLM context..."

# Extract core type signatures (classes, interfaces, objects, functions)
echo "# Core Type Signatures" > "$CONTEXT_DIR/core-types.kt"
echo "# Extracted: $(date -Iseconds)" >> "$CONTEXT_DIR/core-types.kt"
echo "# Source: src/main/kotlin/io/amichne/konditional/" >> "$CONTEXT_DIR/core-types.kt"
echo "" >> "$CONTEXT_DIR/core-types.kt"

# Find key type definitions
if [ -d "$PROJECT_ROOT/src/main/kotlin" ]; then
    # Extract class/interface/object declarations with their signatures
    grep -rn "^\(public \)\?\(sealed \|data \|abstract \|open \)\?\(class\|interface\|object\|fun \)" \
        "$PROJECT_ROOT/src/main/kotlin/" \
        --include="*.kt" \
        2>/dev/null | head -300 >> "$CONTEXT_DIR/core-types.kt" || true
    
    echo "" >> "$CONTEXT_DIR/core-types.kt"
    echo "# --- End of extraction ---" >> "$CONTEXT_DIR/core-types.kt"
fi

# Extract from kontracts submodule if it exists
if [ -d "$PROJECT_ROOT/kontracts/src" ]; then
    echo "" >> "$CONTEXT_DIR/core-types.kt"
    echo "# Kontracts Submodule Types" >> "$CONTEXT_DIR/core-types.kt"
    grep -rn "^\(public \)\?\(sealed \|data \|abstract \|open \)\?\(class\|interface\|object\|fun \)" \
        "$PROJECT_ROOT/kontracts/src/" \
        --include="*.kt" \
        2>/dev/null | head -100 >> "$CONTEXT_DIR/core-types.kt" || true
fi

# Create a summary of public API surface from existing docs
echo "# Public API Surface Summary" > "$CONTEXT_DIR/public-api-surface.md"
echo "# Extracted: $(date -Iseconds)" >> "$CONTEXT_DIR/public-api-surface.md"
echo "" >> "$CONTEXT_DIR/public-api-surface.md"

if [ -d "$PROJECT_ROOT/docs" ]; then
    # Concatenate key documentation files
    for doc in "01-getting-started.md" "03-core-concepts.md" "05-evaluation.md"; do
        if [ -f "$PROJECT_ROOT/docs/$doc" ]; then
            echo "## From $doc" >> "$CONTEXT_DIR/public-api-surface.md"
            echo "" >> "$CONTEXT_DIR/public-api-surface.md"
            cat "$PROJECT_ROOT/docs/$doc" >> "$CONTEXT_DIR/public-api-surface.md"
            echo "" >> "$CONTEXT_DIR/public-api-surface.md"
        fi
    done
fi

# Record last update timestamp
echo "$(date -Iseconds)" > "$CONTEXT_DIR/.last-updated"

echo "Context extraction complete."
echo "  - $CONTEXT_DIR/core-types.kt"
echo "  - $CONTEXT_DIR/public-api-surface.md"
echo "  - Last updated: $(cat "$CONTEXT_DIR/.last-updated")"
