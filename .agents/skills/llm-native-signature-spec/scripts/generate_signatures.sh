#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GENERATOR_PY="${SCRIPT_DIR}/generate_signatures.py"

if [[ ! -f "${GENERATOR_PY}" ]]; then
    echo "Missing generator script: ${GENERATOR_PY}" >&2
    exit 1
fi

if [[ "$#" -eq 0 ]]; then
    exec python3 "${GENERATOR_PY}" --repo-root . --output-dir signatures
fi

exec python3 "${GENERATOR_PY}" "$@"
