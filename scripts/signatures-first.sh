#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PIPELINE_PY="${SCRIPT_DIR}/signatures_first_pipeline.py"

if [[ ! -f "${PIPELINE_PY}" ]]; then
    echo "Missing pipeline script: ${PIPELINE_PY}" >&2
    exit 1
fi

MODE="all"
FAIL_ON_MISSING=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --mode)
            MODE="${2:-}"
            shift 2
            ;;
        --fail-on-missing)
            FAIL_ON_MISSING=true
            shift
            ;;
        *)
            echo "Unknown argument: $1" >&2
            echo "Usage: ./scripts/signatures-first.sh --mode {generate|verify|all} [--fail-on-missing]" >&2
            exit 1
            ;;
    esac
done

ARGS=("${PIPELINE_PY}" "--repo-root" "${REPO_ROOT}" "--mode" "${MODE}")
if [[ "${FAIL_ON_MISSING}" == "true" ]]; then
    ARGS+=("--fail-on-missing")
fi

exec python3 "${ARGS[@]}"
