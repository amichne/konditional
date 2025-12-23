#!/bin/bash
#
# Back-compat entrypoint for context extraction.
# Prefer running `.llm-docs/scripts/extract-llm-context.sh`.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -x "$SCRIPT_DIR/scripts/extract-llm-context.sh" ]; then
  exec "$SCRIPT_DIR/scripts/extract-llm-context.sh" "$@"
fi

echo "Missing executable: $SCRIPT_DIR/scripts/extract-llm-context.sh" >&2
exit 1
