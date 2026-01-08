#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Konditional Pre-Publish Validation Script
# =============================================================================
# Validates that all prerequisites are met before publishing to Maven Central
#
# Usage: ./scripts/validate-publish.sh
# Exit codes: 0 = success, 1 = validation failed

BOLD='\033[1m'
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BOLD}${BLUE}==============================================================================${NC}"
echo -e "${BOLD}${BLUE}Konditional Pre-Publish Validation${NC}"
echo -e "${BOLD}${BLUE}==============================================================================${NC}"
echo ""

VALIDATION_FAILED=0

# -----------------------------------------------------------------------------
# 1. Check Gradle properties
# -----------------------------------------------------------------------------
echo -e "${BOLD}[1/7] Checking gradle.properties...${NC}"

if [[ ! -f "gradle.properties" ]]; then
    echo -e "${RED}✗ gradle.properties not found${NC}"
    VALIDATION_FAILED=1
else
    VERSION=$(grep "^VERSION=" gradle.properties | cut -d'=' -f2)
    GROUP=$(grep "^GROUP=" gradle.properties | cut -d'=' -f2)

    echo -e "  ${GREEN}✓${NC} Version: ${BOLD}${VERSION}${NC}"
    echo -e "  ${GREEN}✓${NC} Group: ${BOLD}${GROUP}${NC}"

    # Check if this is a snapshot version
    if [[ "$VERSION" == *"-SNAPSHOT" ]]; then
        echo -e "  ${YELLOW}⚠${NC} This is a SNAPSHOT version"
    else
        echo -e "  ${GREEN}✓${NC} Release version detected"
    fi
fi
echo ""

# -----------------------------------------------------------------------------
# 2. Check Git status
# -----------------------------------------------------------------------------
echo -e "${BOLD}[2/7] Checking Git status...${NC}"

if ! git rev-parse --is-inside-work-tree &>/dev/null; then
    echo -e "${RED}✗ Not a Git repository${NC}"
    VALIDATION_FAILED=1
else
    # Check for uncommitted changes
    if [[ -n $(git status --porcelain) ]]; then
        echo -e "${YELLOW}⚠ Uncommitted changes detected:${NC}"
        git status --short
        echo -e "${YELLOW}  Consider committing changes before publishing${NC}"
    else
        echo -e "  ${GREEN}✓${NC} Working directory clean"
    fi

    # Check current branch
    CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    echo -e "  ${GREEN}✓${NC} Current branch: ${BOLD}${CURRENT_BRANCH}${NC}"

    if [[ "$CURRENT_BRANCH" != "main" ]]; then
        echo -e "${YELLOW}⚠ Not on 'main' branch. Ensure this is intentional.${NC}"
    fi
fi
echo ""

# -----------------------------------------------------------------------------
# 3. Check signing credentials
# -----------------------------------------------------------------------------
echo -e "${BOLD}[3/7] Checking signing credentials...${NC}"

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
USER_GRADLE_PROPS="$GRADLE_USER_HOME/gradle.properties"

if [[ ! -f "$USER_GRADLE_PROPS" ]]; then
    echo -e "${RED}✗ ~/.gradle/gradle.properties not found${NC}"
    echo -e "  ${RED}Please configure signing credentials${NC}"
    VALIDATION_FAILED=1
else
    # Check for signing properties
    SIGNING_KEY_ID=$(grep "^signing.keyId=" "$USER_GRADLE_PROPS" 2>/dev/null | cut -d'=' -f2 || echo "")
    SIGNING_PASSWORD=$(grep "^signing.password=" "$USER_GRADLE_PROPS" 2>/dev/null | cut -d'=' -f2 || echo "")
    SIGNING_KEY_FILE=$(grep "^signing.secretKeyRingFile=" "$USER_GRADLE_PROPS" 2>/dev/null | cut -d'=' -f2 || echo "")

    if [[ -z "$SIGNING_KEY_ID" ]]; then
        echo -e "${RED}✗ signing.keyId not configured${NC}"
        VALIDATION_FAILED=1
    else
        echo -e "  ${GREEN}✓${NC} signing.keyId: ${BOLD}${SIGNING_KEY_ID}${NC}"
    fi

    if [[ -z "$SIGNING_PASSWORD" ]]; then
        echo -e "${RED}✗ signing.password not configured${NC}"
        VALIDATION_FAILED=1
    else
        echo -e "  ${GREEN}✓${NC} signing.password: ${BOLD}[REDACTED]${NC}"
    fi

    if [[ -z "$SIGNING_KEY_FILE" ]]; then
        echo -e "${RED}✗ signing.secretKeyRingFile not configured${NC}"
        VALIDATION_FAILED=1
    else
        # Expand tilde if present
        SIGNING_KEY_FILE="${SIGNING_KEY_FILE/#\~/$HOME}"

        if [[ ! -f "$SIGNING_KEY_FILE" ]]; then
            echo -e "${RED}✗ signing.secretKeyRingFile not found: ${SIGNING_KEY_FILE}${NC}"
            VALIDATION_FAILED=1
        else
            echo -e "  ${GREEN}✓${NC} signing.secretKeyRingFile: ${BOLD}${SIGNING_KEY_FILE}${NC}"
        fi
    fi
fi
echo ""

# -----------------------------------------------------------------------------
# 4. Check Sonatype credentials
# -----------------------------------------------------------------------------
echo -e "${BOLD}[4/7] Checking Sonatype credentials...${NC}"

OSSRH_USERNAME=$(grep "^ossrhUsername=" "$USER_GRADLE_PROPS" 2>/dev/null | cut -d'=' -f2 || echo "")
OSSRH_PASSWORD=$(grep "^ossrhPassword=" "$USER_GRADLE_PROPS" 2>/dev/null | cut -d'=' -f2 || echo "")

if [[ -z "$OSSRH_USERNAME" ]] && [[ -z "$OSSRH_PASSWORD" ]]; then
    # Check environment variables
    if [[ -n "${OSSRH_USERNAME:-}" ]] || [[ -n "${OSSRH_PASSWORD:-}" ]]; then
        echo -e "  ${GREEN}✓${NC} Credentials configured via environment variables"
    else
        echo -e "${RED}✗ OSSRH credentials not configured${NC}"
        echo -e "  ${RED}Set ossrhUsername and ossrhPassword in ~/.gradle/gradle.properties${NC}"
        VALIDATION_FAILED=1
    fi
else
    if [[ -z "$OSSRH_USERNAME" ]]; then
        echo -e "${RED}✗ ossrhUsername not configured${NC}"
        VALIDATION_FAILED=1
    else
        echo -e "  ${GREEN}✓${NC} ossrhUsername: ${BOLD}${OSSRH_USERNAME}${NC}"
    fi

    if [[ -z "$OSSRH_PASSWORD" ]]; then
        echo -e "${RED}✗ ossrhPassword not configured${NC}"
        VALIDATION_FAILED=1
    else
        echo -e "  ${GREEN}✓${NC} ossrhPassword: ${BOLD}[REDACTED]${NC}"
    fi
fi
echo ""

# -----------------------------------------------------------------------------
# 5. Verify GPG key is published
# -----------------------------------------------------------------------------
echo -e "${BOLD}[5/7] Verifying GPG key is published...${NC}"

if [[ -n "${SIGNING_KEY_ID:-}" ]]; then
    # Try to fetch from key server
    if gpg --keyserver keys.openpgp.org --recv-keys "$SIGNING_KEY_ID" &>/dev/null; then
        echo -e "  ${GREEN}✓${NC} GPG key ${BOLD}${SIGNING_KEY_ID}${NC} is published"
    else
        echo -e "${YELLOW}⚠ Could not verify GPG key on keys.openpgp.org${NC}"
        echo -e "  ${YELLOW}Ensure your key is published to key servers${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Skipping (no signing.keyId configured)${NC}"
fi
echo ""

# -----------------------------------------------------------------------------
# 6. Verify publishable modules
# -----------------------------------------------------------------------------
echo -e "${BOLD}[6/7] Verifying publishable modules...${NC}"

PUBLISHABLE_MODULES=(
    "konditional-core"
    "konditional-serialization"
    "konditional-runtime"
    "konditional-observability"
)

for module in "${PUBLISHABLE_MODULES[@]}"; do
    if [[ -d "$module" ]]; then
        if grep -q "maven-publish" "$module/build.gradle.kts" 2>/dev/null; then
            echo -e "  ${GREEN}✓${NC} ${module}"
        else
            echo -e "${RED}✗ ${module} missing maven-publish plugin${NC}"
            VALIDATION_FAILED=1
        fi
    else
        echo -e "${RED}✗ ${module} directory not found${NC}"
        VALIDATION_FAILED=1
    fi
done
echo ""

# -----------------------------------------------------------------------------
# 7. Run Gradle checks
# -----------------------------------------------------------------------------
echo -e "${BOLD}[7/7] Running Gradle validation tasks...${NC}"

if ! ./gradlew clean build -x test --quiet; then
    echo -e "${RED}✗ Build failed${NC}"
    VALIDATION_FAILED=1
else
    echo -e "  ${GREEN}✓${NC} Build successful"
fi
echo ""

# -----------------------------------------------------------------------------
# Summary
# -----------------------------------------------------------------------------
echo -e "${BOLD}${BLUE}==============================================================================${NC}"
if [[ $VALIDATION_FAILED -eq 0 ]]; then
    echo -e "${BOLD}${GREEN}✓ All validations passed!${NC}"
    echo -e ""
    echo -e "You can now publish with:"
    echo -e "  ${BOLD}make publish-snapshot${NC}  - Publish to Sonatype snapshots"
    echo -e "  ${BOLD}make publish-release${NC}   - Publish to Sonatype staging (requires manual release)"
    echo -e "${BOLD}${BLUE}==============================================================================${NC}"
    exit 0
else
    echo -e "${BOLD}${RED}✗ Validation failed!${NC}"
    echo -e ""
    echo -e "Please fix the issues above before publishing."
    echo -e "See ${BOLD}PUBLISHING_GUIDE.md${NC} for detailed setup instructions."
    echo -e "${BOLD}${BLUE}==============================================================================${NC}"
    exit 1
fi
