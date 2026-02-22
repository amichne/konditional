#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${REPO_ROOT}"
bash ./scripts/generate-signatures.sh

if ! git diff --quiet -- .signatures; then
    echo "Signature artifacts are out of date. Run bash ./scripts/generate-signatures.sh and commit .signatures/." >&2
    git --no-pager diff -- .signatures
    exit 1
fi

UNTRACKED_FILES="$(git ls-files --others --exclude-standard -- .signatures)"
if [[ -n "${UNTRACKED_FILES}" ]]; then
    echo "Untracked signature artifacts detected. Commit all .signatures changes." >&2
    echo "${UNTRACKED_FILES}" >&2
    exit 1
fi

echo "Signature artifacts are up to date."
