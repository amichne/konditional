#!/usr/bin/env bash
set -euo pipefail

VERSION_CHOICE="patch"
RUN_PREFLIGHT=1
FULL_VALIDATION=0
DRY_RUN=0

usage() {
    cat <<'USAGE'
Usage: release_fastpath.sh [options]

Options:
  --version-choice <none|patch|minor|major>  Version action (default: patch)
  --no-preflight                              Skip publish preflight validation
  --full-validation                           Run full preflight (includes smoke checks)
  --dry-run                                   Print the publish command without executing it
  --help                                      Show this help

Notes:
  - Snapshot flows are intentionally unsupported in this fastpath.
  - Run from Konditional repository root.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version-choice)
            VERSION_CHOICE="${2:-}"
            shift 2
            ;;
        --no-preflight)
            RUN_PREFLIGHT=0
            shift
            ;;
        --full-validation)
            FULL_VALIDATION=1
            shift
            ;;
        --dry-run)
            DRY_RUN=1
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

case "$VERSION_CHOICE" in
    none|patch|minor|major)
        ;;
    *)
        echo "Invalid --version-choice '$VERSION_CHOICE'. Expected: none|patch|minor|major." >&2
        exit 1
        ;;
esac

if [[ ! -f "Makefile" || ! -f "scripts/publish-on-rails.sh" ]]; then
    echo "Run this script from the Konditional repository root." >&2
    exit 1
fi

if [[ "$FULL_VALIDATION" -eq 0 ]]; then
    export VALIDATE_PUBLISH_SKIP_SMOKE=1
fi

if [[ "$DRY_RUN" -eq 1 ]]; then
    if [[ "$RUN_PREFLIGHT" -eq 1 ]]; then
        echo "Dry run preflight: make publish-validate-release"
    fi
    echo "Dry run publish: make publish-plan PUBLISH_TARGET=release VERSION_CHOICE=$VERSION_CHOICE"
    exit 0
fi

if [[ "$RUN_PREFLIGHT" -eq 1 ]]; then
    echo "Running release preflight validation..."
    make publish-validate-release
fi

PUBLISH_CMD=(make publish-plan PUBLISH_TARGET=release VERSION_CHOICE="$VERSION_CHOICE")
echo "Executing: ${PUBLISH_CMD[*]}"

if [[ "$DRY_RUN" -eq 1 ]]; then
    exit 0
fi

"${PUBLISH_CMD[@]}"
echo "Release fastpath completed."
