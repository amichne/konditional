#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Konditional Publishing Automation Script
# =============================================================================
# Publishes Konditional artifacts to Sonatype OSSRH (Maven Central) or GitHub Packages
#
# Usage:
#   ./scripts/publish.sh snapshot   - Publish snapshot to OSSRH snapshots
#   ./scripts/publish.sh release    - Publish release to OSSRH staging
#   ./scripts/publish.sh github     - Publish to GitHub Packages
#   ./scripts/publish.sh local      - Publish to local Maven (~/.m2/repository)
#
# Exit codes: 0 = success, 1 = failure

BOLD='\033[1m'
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# -----------------------------------------------------------------------------
# Parse arguments
# -----------------------------------------------------------------------------
PUBLISH_TYPE="${1:-}"

if [[ -z "$PUBLISH_TYPE" ]]; then
    echo -e "${RED}Error: Missing publish type${NC}"
    echo ""
    echo "Usage: $0 {snapshot|release|github|local}"
    echo ""
    echo "  snapshot - Publish to Sonatype snapshots (auto-published)"
    echo "  release  - Publish to Sonatype staging (requires manual release in UI)"
    echo "  github   - Publish to GitHub Packages"
    echo "  local    - Publish to local Maven repository (~/.m2/repository)"
    exit 1
fi

# -----------------------------------------------------------------------------
# Validation
# -----------------------------------------------------------------------------
echo -e "${BOLD}${BLUE}==============================================================================${NC}"
echo -e "${BOLD}${BLUE}Konditional Publishing - ${PUBLISH_TYPE^^}${NC}"
echo -e "${BOLD}${BLUE}==============================================================================${NC}"
echo ""

VERSION=$(grep "^VERSION=" gradle.properties | cut -d'=' -f2)
GROUP=$(grep "^GROUP=" gradle.properties | cut -d'=' -f2)

echo -e "${BOLD}Publishing:${NC}"
echo -e "  Group: ${BOLD}${GROUP}${NC}"
echo -e "  Version: ${BOLD}${VERSION}${NC}"
echo ""

# Validate version matches publish type
case "$PUBLISH_TYPE" in
    snapshot)
        if [[ "$VERSION" != *"-SNAPSHOT" ]]; then
            echo -e "${YELLOW}⚠ Warning: Version '${VERSION}' is not a SNAPSHOT${NC}"
            echo -e "${YELLOW}  Snapshot publishing typically uses versions like '0.1.0-SNAPSHOT'${NC}"
            read -p "Continue anyway? [y/N] " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                echo -e "${RED}Aborted${NC}"
                exit 1
            fi
        fi
        ;;
    release)
        if [[ "$VERSION" == *"-SNAPSHOT" ]]; then
            echo -e "${RED}Error: Cannot publish SNAPSHOT version as release${NC}"
            echo -e "${RED}Update VERSION in gradle.properties to a release version (e.g., 0.1.0)${NC}"
            exit 1
        fi
        ;;
    github)
        # No version restrictions for GitHub Packages
        ;;
    local)
        # No version restrictions for local publishing
        ;;
    *)
        echo -e "${RED}Error: Invalid publish type '${PUBLISH_TYPE}'${NC}"
        echo "Valid types: snapshot, release, github, local"
        exit 1
        ;;
esac

# -----------------------------------------------------------------------------
# Run validation (Sonatype only)
# -----------------------------------------------------------------------------
if [[ "$PUBLISH_TYPE" == "snapshot" || "$PUBLISH_TYPE" == "release" ]]; then
    echo -e "${BOLD}Running pre-publish validation...${NC}"
    echo ""
    if ! ./scripts/validate-publish.sh; then
        echo ""
        echo -e "${RED}Validation failed. Aborting publish.${NC}"
        exit 1
    fi
    echo ""
fi

# -----------------------------------------------------------------------------
# GitHub Packages checks
# -----------------------------------------------------------------------------
if [[ "$PUBLISH_TYPE" == "github" ]]; then
    GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
    USER_GRADLE_PROPS="$GRADLE_USER_HOME/gradle.properties"

    GPR_USER=""
    GPR_KEY=""
    GPR_REPO=""
    if [[ -f "$USER_GRADLE_PROPS" ]]; then
        GPR_USER=$(grep "^gpr.user=" "$USER_GRADLE_PROPS" 2>/dev/null | cut -d'=' -f2 || true)
        GPR_KEY=$(grep "^gpr.key=" "$USER_GRADLE_PROPS" 2>/dev/null | cut -d'=' -f2 || true)
        GPR_REPO=$(grep "^gpr.repo=" "$USER_GRADLE_PROPS" 2>/dev/null | cut -d'=' -f2 || true)
    fi

    GITHUB_USER="${GPR_USER:-${GITHUB_ACTOR:-${GITHUB_USERNAME:-}}}"
    GITHUB_TOKEN_VALUE="${GPR_KEY:-${GITHUB_TOKEN:-${GITHUB_PACKAGES_TOKEN:-}}}"

    POM_SCM_URL=$(grep "^POM_SCM_URL=" gradle.properties | cut -d'=' -f2- || true)
    POM_URL=$(grep "^POM_URL=" gradle.properties | cut -d'=' -f2- || true)

    GITHUB_REPOSITORY_VALUE="${GPR_REPO:-${GITHUB_REPOSITORY:-}}"

    if [[ -z "$GITHUB_REPOSITORY_VALUE" ]]; then
        for URL in "$POM_SCM_URL" "$POM_URL"; do
            if [[ -z "$GITHUB_REPOSITORY_VALUE" && "$URL" =~ github.com[:/]+([^/]+/[^/]+)(\.git)?$ ]]; then
                GITHUB_REPOSITORY_VALUE="${BASH_REMATCH[1]}"
            fi
        done
    fi

    if [[ -z "$GITHUB_REPOSITORY_VALUE" ]]; then
        echo -e "${RED}Error: Unable to determine GitHub repository (owner/repo)${NC}"
        echo -e "${RED}Set GITHUB_REPOSITORY or gpr.repo in ~/.gradle/gradle.properties${NC}"
        exit 1
    fi

    if [[ -z "$GITHUB_USER" || -z "$GITHUB_TOKEN_VALUE" ]]; then
        echo -e "${RED}Error: GitHub Packages credentials not configured${NC}"
        echo -e "${RED}Set gpr.user and gpr.key in ~/.gradle/gradle.properties or GITHUB_ACTOR/GITHUB_TOKEN env vars${NC}"
        exit 1
    fi
fi

# -----------------------------------------------------------------------------
# Confirm publish
# -----------------------------------------------------------------------------
if [[ "$PUBLISH_TYPE" == "release" ]]; then
    echo -e "${BOLD}${YELLOW}==============================================================================${NC}"
    echo -e "${BOLD}${YELLOW}RELEASE PUBLISHING${NC}"
    echo -e "${BOLD}${YELLOW}==============================================================================${NC}"
    echo ""
    echo -e "You are about to publish ${BOLD}${GROUP}:*:${VERSION}${NC} to Sonatype staging."
    echo ""
    echo -e "${YELLOW}After this completes, you MUST:${NC}"
    echo -e "  1. Login to https://s01.oss.sonatype.org"
    echo -e "  2. Navigate to 'Staging Repositories'"
    echo -e "  3. Find repository '${GROUP}' (e.g., ioamichne-1001)"
    echo -e "  4. Click 'Close' and wait for validation"
    echo -e "  5. Click 'Release' to publish to Maven Central"
    echo ""
    read -p "Continue? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}Aborted${NC}"
        exit 0
    fi
fi

# -----------------------------------------------------------------------------
# Execute publish
# -----------------------------------------------------------------------------
echo ""
echo -e "${BOLD}Publishing artifacts...${NC}"
echo ""

case "$PUBLISH_TYPE" in
    snapshot)
        ./gradlew clean build publishAllPublicationsToSonatypeRepository --no-daemon --stacktrace
        ;;
    release)
        ./gradlew clean build publishToSonatype closeAndReleaseSonatypeStagingRepository --no-daemon --stacktrace
        ;;
    github)
        ./gradlew clean build publishAllPublicationsToGitHubPackagesRepository --no-daemon --stacktrace
        ;;
    local)
        ./gradlew clean build publishToMavenLocal --no-daemon --stacktrace
        ;;
esac

PUBLISH_EXIT_CODE=$?

# -----------------------------------------------------------------------------
# Summary
# -----------------------------------------------------------------------------
echo ""
echo -e "${BOLD}${BLUE}==============================================================================${NC}"

if [[ $PUBLISH_EXIT_CODE -eq 0 ]]; then
    echo -e "${BOLD}${GREEN}✓ Publishing successful!${NC}"
    echo ""

    case "$PUBLISH_TYPE" in
        snapshot)
            echo -e "Snapshot artifacts published to:"
            echo -e "  ${BOLD}https://s01.oss.sonatype.org/content/repositories/snapshots/${NC}"
            echo ""
            echo -e "Consumers can use:"
            echo -e "  ${BOLD}implementation(\"${GROUP}:konditional-core:${VERSION}\")${NC}"
            echo ""
            echo -e "With repository:"
            echo -e "  ${BOLD}maven { url = uri(\"https://s01.oss.sonatype.org/content/repositories/snapshots/\") }${NC}"
            ;;
        release)
            echo -e "Release artifacts published to Sonatype staging."
            echo ""
            echo -e "${BOLD}${YELLOW}NEXT STEPS (MANUAL):${NC}"
            echo -e "  1. Login: ${BOLD}https://s01.oss.sonatype.org${NC}"
            echo -e "  2. Go to: ${BOLD}Staging Repositories${NC}"
            echo -e "  3. Find: ${BOLD}${GROUP}${NC} repository (e.g., ioamichne-1001)"
            echo -e "  4. Click: ${BOLD}Close${NC} (runs validations, wait 2-5 minutes)"
            echo -e "  5. Click: ${BOLD}Release${NC} (publishes to Maven Central)"
            echo ""
            echo -e "Maven Central propagation: ${BOLD}10-30 minutes${NC}"
            echo ""
            echo -e "Verify at:"
            echo -e "  ${BOLD}https://search.maven.org/search?q=g:${GROUP}${NC}"
            ;;
        github)
            echo -e "Artifacts published to GitHub Packages:"
            echo -e "  ${BOLD}https://maven.pkg.github.com/${GITHUB_REPOSITORY_VALUE}${NC}"
            echo ""
            echo -e "Consumers can use:"
            echo -e "  ${BOLD}implementation(\"${GROUP}:konditional-core:${VERSION}\")${NC}"
            echo ""
            echo -e "With repository:"
            echo -e "  ${BOLD}maven { url = uri(\"https://maven.pkg.github.com/${GITHUB_REPOSITORY_VALUE}\") }${NC}"
            ;;
        local)
            echo -e "Artifacts published to local Maven repository:"
            echo -e "  ${BOLD}~/.m2/repository/${GROUP//.//}/${NC}"
            echo ""
            echo -e "Consumers can use:"
            echo -e "  ${BOLD}implementation(\"${GROUP}:konditional-core:${VERSION}\")${NC}"
            echo ""
            echo -e "With repository:"
            echo -e "  ${BOLD}mavenLocal()${NC}"
            ;;
    esac
else
    echo -e "${BOLD}${RED}✗ Publishing failed!${NC}"
    echo ""
    echo -e "Check the error output above for details."
    echo -e "Common issues:"
    echo -e "  - Invalid credentials (check ~/.gradle/gradle.properties)"
    echo -e "  - GPG signing failed (check signing.* properties)"
    echo -e "  - Network connectivity issues"
    echo -e "  - POM validation errors"
fi

echo -e "${BOLD}${BLUE}==============================================================================${NC}"
exit $PUBLISH_EXIT_CODE
