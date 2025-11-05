#!/bin/bash

# Script to prepare a release
# This script will:
# 1. Validate the current state
# 2. Run tests
# 3. Create a changelog entry template
# 4. Guide you through the release process

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CHANGELOG_DIR="$PROJECT_DIR/ai/changelog"

cd "$PROJECT_DIR"

echo "═══════════════════════════════════════════════════════"
echo "  Konditional Release Preparation"
echo "═══════════════════════════════════════════════════════"
echo ""

# Check if on main branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "⚠️  WARNING: You are not on the main branch (current: $CURRENT_BRANCH)"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    echo "❌ ERROR: You have uncommitted changes"
    echo ""
    git status --short
    echo ""
    echo "Please commit or stash your changes before preparing a release."
    exit 1
fi

# Read current version
CURRENT_VERSION=$(./gradlew properties -q | grep "^version:" | awk '{print $2}')

echo "Current version: $CURRENT_VERSION"
echo ""

# Check if version is a snapshot
if [[ "$CURRENT_VERSION" == *-SNAPSHOT ]]; then
    echo "❌ ERROR: Cannot release a SNAPSHOT version"
    echo ""
    echo "Run: ./scripts/bump-version.sh patch"
    echo "Then run this script again."
    exit 1
fi

# Run tests
echo "Running tests..."
./gradlew test --no-daemon --console=plain

echo ""
echo "✅ Tests passed!"
echo ""

# Check for changelog entries
if [ -d "$CHANGELOG_DIR" ]; then
    CHANGELOG_COUNT=$(find "$CHANGELOG_DIR" -type f -name "*.md" 2>/dev/null | wc -l | tr -d ' ')
    if [ "$CHANGELOG_COUNT" -eq 0 ]; then
        echo "⚠️  WARNING: No changelog entries found in $CHANGELOG_DIR"
        echo ""
        read -p "Continue without changelog entries? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo "Found $CHANGELOG_COUNT changelog entries:"
        find "$CHANGELOG_DIR" -type f -name "*.md" | sort | while read -r file; do
            echo "  - $(basename "$file")"
        done
        echo ""
    fi
fi

# Generate release checklist
cat << EOF

═══════════════════════════════════════════════════════════════
  Release Checklist for v$CURRENT_VERSION
═══════════════════════════════════════════════════════════════

✅ Tests passing
✅ No uncommitted changes
✅ Version set to: $CURRENT_VERSION

Next steps:

1. Review the code one final time

2. Create and push the release tag:
   git tag -a v$CURRENT_VERSION -m "Release v$CURRENT_VERSION"
   git push origin v$CURRENT_VERSION

3. The GitHub Actions workflow will automatically:
   - Run tests across all platforms
   - Publish to Maven Central
   - Publish to GitHub Packages
   - Create a GitHub Release with artifacts
   - Generate changelog

4. After release, bump to next development version:
   ./scripts/bump-version.sh patch --snapshot

5. Monitor the release:
   - GitHub Actions: https://github.com/amichne/konditional/actions
   - Maven Central: https://central.sonatype.com/artifact/io.amichne/konditional
   - GitHub Releases: https://github.com/amichne/konditional/releases

═══════════════════════════════════════════════════════════════

EOF

read -p "Ready to create the release tag? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    git tag -a "v$CURRENT_VERSION" -m "Release v$CURRENT_VERSION"
    echo ""
    echo "✅ Tag created: v$CURRENT_VERSION"
    echo ""
    read -p "Push tag to origin? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git push origin "v$CURRENT_VERSION"
        echo ""
        echo "✅ Tag pushed! Release workflow started."
        echo ""
        echo "Monitor at: https://github.com/amichne/konditional/actions"
    fi
fi
