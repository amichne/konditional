#!/usr/bin/env bash
# check-docs-claim-coverage.sh
# Validates that all claims in claims-registry.json have corresponding
# anchors in the documentation pages.
#
# Usage: ./check-docs-claim-coverage.sh <claims-registry.json> <docs-root>
# Example: ./check-docs-claim-coverage.sh claims-registry.json docusaurus/docs/

set -euo pipefail

REGISTRY="${1:?Usage: $0 <claims-registry.json> <docs-root>}"
DOCS_ROOT="${2:?Usage: $0 <claims-registry.json> <docs-root>}"

if ! command -v jq &>/dev/null; then
  echo "ERROR: jq is required" >&2
  exit 1
fi

# Extract all claim IDs from registry
CLAIM_IDS=$(jq -r '.claims[].claim_id' "$REGISTRY" | sort)
TOTAL=$(echo "$CLAIM_IDS" | wc -l | tr -d ' ')
FOUND=0
MISSING=()

echo "Checking $TOTAL claims against docs in $DOCS_ROOT ..."
echo ""

for CLAIM_ID in $CLAIM_IDS; do
  # Normalize to lowercase-hyphenated anchor format
  ANCHOR=$(echo "$CLAIM_ID" | tr '[:upper:]' '[:lower:]' | tr '_' '-')

  # Search for the anchor in docs
  if grep -rq "id=\"claim-${ANCHOR}\"" "$DOCS_ROOT" 2>/dev/null; then
    FOUND=$((FOUND + 1))
  else
    MISSING+=("$CLAIM_ID")
  fi
done

echo "Coverage: $FOUND / $TOTAL claims have documentation anchors"
echo ""

if [ ${#MISSING[@]} -gt 0 ]; then
  echo "MISSING anchors (${#MISSING[@]}):"
  for M in "${MISSING[@]}"; do
    # Look up the claim text for context
    TEXT=$(jq -r --arg id "$M" '.claims[] | select(.claim_id == $id) | .claim_text' "$REGISTRY")
    echo "  - $M: $TEXT"
  done
  echo ""
  echo "RESULT: INCOMPLETE"
  exit 1
else
  echo "RESULT: ALL CLAIMS COVERED"
  exit 0
fi
