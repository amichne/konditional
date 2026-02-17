#!/bin/bash

# Script to bump the version in gradle.properties
# Usage: ./scripts/bump-version.sh [major|minor|patch] [--snapshot]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
GRADLE_PROPS="$PROJECT_DIR/gradle.properties"

if [ ! -f "$GRADLE_PROPS" ]; then
    echo "ERROR: gradle.properties not found at $GRADLE_PROPS"
    exit 1
fi

# Parse arguments
BUMP_TYPE="${1:-patch}"
ADD_SNAPSHOT="${2}"

if [[ ! "$BUMP_TYPE" =~ ^(major|minor|patch)$ ]]; then
    echo "Usage: $0 [major|minor|patch] [--snapshot]"
    echo ""
    echo "Examples:"
    echo "  $0 patch              # 0.0.1 -> 0.0.2"
    echo "  $0 minor              # 0.0.1 -> 0.1.0"
    echo "  $0 major              # 0.0.1 -> 1.0.0"
    echo "  $0 patch --snapshot   # 0.0.1 -> 0.0.2-SNAPSHOT"
    exit 1
fi

# Read current version
CURRENT_VERSION=$(grep "^VERSION=" "$GRADLE_PROPS" | cut -d'=' -f2)

# Remove -SNAPSHOT suffix if present
CURRENT_VERSION="${CURRENT_VERSION%-SNAPSHOT}"

# Split version into parts
IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
MAJOR="${VERSION_PARTS[0]}"
MINOR="${VERSION_PARTS[1]}"
PATCH="${VERSION_PARTS[2]}"

# Bump version
case "$BUMP_TYPE" in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    patch)
        PATCH=$((PATCH + 1))
        ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"

# Add -SNAPSHOT suffix if requested
if [ "$ADD_SNAPSHOT" = "--snapshot" ]; then
    NEW_VERSION="$NEW_VERSION-SNAPSHOT"
fi

# Update gradle.properties
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s/^VERSION=.*/VERSION=$NEW_VERSION/" "$GRADLE_PROPS"
else
    # Linux
    sed -i "s/^VERSION=.*/VERSION=$NEW_VERSION/" "$GRADLE_PROPS"
fi

echo "✓ Version bumped: $CURRENT_VERSION -> $NEW_VERSION"
echo ""
echo "Next steps:"
if [ "$ADD_SNAPSHOT" != "--snapshot" ]; then
    echo "  1. Review changes: git diff gradle.properties"
    echo "  2. Commit: git add gradle.properties && git commit -m 'Bump version to $NEW_VERSION'"
    echo "  3. Tag: git tag -a v$NEW_VERSION -m 'Release v$NEW_VERSION'"
    echo "  4. Push: git push origin main --tags"
else
    echo "  1. Review changes: git diff gradle.properties"
    echo "  2. Commit: git add gradle.properties && git commit -m 'Prepare $NEW_VERSION'"
    echo "  3. Push: git push origin main"
    echo "  4. Trigger snapshot publish workflow in GitHub Actions"
fi
