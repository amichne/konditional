#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
GENERATOR_PY="${REPO_ROOT}/.agents/skills/llm-native-signature-spec/scripts/generate_signatures.py"

if [[ ! -f "${GENERATOR_PY}" ]]; then
    echo "Missing signature generator: ${GENERATOR_PY}" >&2
    exit 1
fi

if [[ "$#" -eq 0 ]]; then
    exec python3 "${GENERATOR_PY}" --repo-root "${REPO_ROOT}" --output-dir .signatures
fi

exec python3 "${GENERATOR_PY}" "$@"
