#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
README_PATH="${ROOT_DIR}/README.md"
DOCS_PATH="${ROOT_DIR}/docusaurus/docs"

declare -a CHECK_LABELS=(
  "StableId constructor form"
  "RampUpBucketing.calculateBucket symbol"
  "NamespaceSnapshotLoader.validate symbol"
)

declare -a CHECK_PATTERNS=(
  "(^|[^[:alnum:]_])StableId\\("
  "RampUpBucketing\\.calculateBucket"
  "NamespaceSnapshotLoader[^[:cntrl:]]*\\.validate\\("
)

echo "Checking docs API conformance in README + docusaurus/docs..."

failures=0
for i in "${!CHECK_LABELS[@]}"; do
  label="${CHECK_LABELS[$i]}"
  pattern="${CHECK_PATTERNS[$i]}"
  matches="$(
    grep -R -nE \
      --include='*.md' \
      --include='*.mdx' \
      "${pattern}" \
      "${README_PATH}" \
      "${DOCS_PATH}" \
      || true
  )"

  if [[ -n "${matches}" ]]; then
    failures=1
    echo
    echo "[FAIL] ${label}"
    echo "${matches}"
  else
    echo "[PASS] ${label}"
  fi
done

if [[ "${failures}" -ne 0 ]]; then
  echo
  echo "Docs API conformance check failed."
  exit 1
fi

echo
echo "Docs API conformance check passed."
