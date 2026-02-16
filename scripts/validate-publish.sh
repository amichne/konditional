#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Konditional Pre-Publish Validation Script
# =============================================================================
# Validates that all prerequisites are met before publishing artifacts.
#
# Usage: ./scripts/validate-publish.sh
# Exit codes: 0 = success, 1 = validation failed

BOLD='\033[1m'
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

VALIDATION_FAILED=0
SKIP_CREDENTIAL_CHECKS="${VALIDATE_PUBLISH_SKIP_CREDENTIALS:-0}"

pass() { echo -e "  ${GREEN}✓${NC} $1"; }
warn() { echo -e "${YELLOW}⚠ $1${NC}"; }
fail() {
  echo -e "${RED}✗ $1${NC}"
  VALIDATION_FAILED=1
}

value_from_props() {
  local key="$1"
  local file="$2"
  grep -E "^${key}=" "$file" 2>/dev/null | head -n1 | cut -d'=' -f2-
}

echo -e "${BOLD}${BLUE}==============================================================================${NC}"
echo -e "${BOLD}${BLUE}Konditional Pre-Publish Validation${NC}"
echo -e "${BOLD}${BLUE}==============================================================================${NC}"
echo ""

# -----------------------------------------------------------------------------
# 1. Check gradle.properties
# -----------------------------------------------------------------------------
echo -e "${BOLD}[1/8] Checking gradle.properties...${NC}"

if [[ ! -f "gradle.properties" ]]; then
  fail "gradle.properties not found"
else
  VERSION=$(value_from_props "VERSION" "gradle.properties")
  GROUP=$(value_from_props "GROUP" "gradle.properties")

  [[ -n "${VERSION}" ]] && pass "Version: ${BOLD}${VERSION}${NC}" || fail "Missing VERSION in gradle.properties"
  [[ -n "${GROUP}" ]] && pass "Group: ${BOLD}${GROUP}${NC}" || fail "Missing GROUP in gradle.properties"

  if [[ "${VERSION:-}" == *"-SNAPSHOT" ]]; then
    warn "This is a SNAPSHOT version"
  else
    pass "Release version detected"
  fi
fi
echo ""

# -----------------------------------------------------------------------------
# 2. Check Git status
# -----------------------------------------------------------------------------
echo -e "${BOLD}[2/8] Checking Git status...${NC}"

if ! git rev-parse --is-inside-work-tree &>/dev/null; then
  fail "Not a Git repository"
else
  if [[ -n "$(git status --porcelain)" ]]; then
    warn "Uncommitted changes detected:"
    git status --short
    warn "Consider committing changes before publishing"
  else
    pass "Working directory clean"
  fi

  CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
  pass "Current branch: ${BOLD}${CURRENT_BRANCH}${NC}"
  if [[ "$CURRENT_BRANCH" != "main" ]]; then
    warn "Not on 'main' branch. Ensure this is intentional."
  fi
fi
echo ""

# -----------------------------------------------------------------------------
# 3. Check signing credentials
# -----------------------------------------------------------------------------
echo -e "${BOLD}[3/8] Checking signing credentials...${NC}"

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
USER_GRADLE_PROPS="$GRADLE_USER_HOME/gradle.properties"

if [[ "$SKIP_CREDENTIAL_CHECKS" == "1" ]]; then
  warn "Skipping signing credential checks (VALIDATE_PUBLISH_SKIP_CREDENTIALS=1)"
elif [[ ! -f "$USER_GRADLE_PROPS" ]]; then
  fail "~/.gradle/gradle.properties not found"
  echo -e "  ${RED}Please configure signing credentials${NC}"
else
  SIGNING_GPG_KEY_NAME=$(value_from_props "signing.gnupg.keyName" "$USER_GRADLE_PROPS")
  SIGNING_GPG_EXECUTABLE=$(value_from_props "signing.gnupg.executable" "$USER_GRADLE_PROPS")
  SIGNING_GPG_PASSPHRASE=$(value_from_props "signing.gnupg.passphrase" "$USER_GRADLE_PROPS")

  SIGNING_KEY_ID=$(value_from_props "signing.keyId" "$USER_GRADLE_PROPS")
  SIGNING_PASSWORD=$(value_from_props "signing.password" "$USER_GRADLE_PROPS")
  SIGNING_KEY_FILE=$(value_from_props "signing.secretKeyRingFile" "$USER_GRADLE_PROPS")

  if [[ -n "$SIGNING_GPG_KEY_NAME" ]]; then
    pass "Using GPG agent signing (recommended)"
    pass "signing.gnupg.keyName: ${BOLD}${SIGNING_GPG_KEY_NAME}${NC}"
    [[ -n "$SIGNING_GPG_EXECUTABLE" ]] && pass "signing.gnupg.executable: ${BOLD}${SIGNING_GPG_EXECUTABLE}${NC}"
    [[ -n "$SIGNING_GPG_PASSPHRASE" ]] && pass "signing.gnupg.passphrase: ${BOLD}[REDACTED]${NC}"

    if ! command -v gpg &>/dev/null; then
      fail "gpg command not found in PATH"
    else
      pass "gpg command available"
      if gpg --list-secret-keys "$SIGNING_GPG_KEY_NAME" &>/dev/null; then
        pass "GPG key found in keyring"
      else
        fail "GPG key ${SIGNING_GPG_KEY_NAME} not found in keyring"
      fi
    fi
  elif [[ -n "$SIGNING_KEY_ID" ]]; then
    warn "Using legacy keyring signing (consider migrating to GPG agent)"

    [[ -n "$SIGNING_KEY_ID" ]] && pass "signing.keyId: ${BOLD}${SIGNING_KEY_ID}${NC}" || fail "signing.keyId not configured"
    [[ -n "$SIGNING_PASSWORD" ]] && pass "signing.password: ${BOLD}[REDACTED]${NC}" || fail "signing.password not configured"

    if [[ -z "$SIGNING_KEY_FILE" ]]; then
      fail "signing.secretKeyRingFile not configured"
    else
      SIGNING_KEY_FILE="${SIGNING_KEY_FILE/#\~/$HOME}"
      [[ -f "$SIGNING_KEY_FILE" ]] && pass "signing.secretKeyRingFile: ${BOLD}${SIGNING_KEY_FILE}${NC}" || fail "signing.secretKeyRingFile not found: ${SIGNING_KEY_FILE}"
    fi
  else
    fail "No signing credentials configured"
    echo -e "  ${RED}Configure either:${NC}"
    echo -e "  ${RED}  - GPG agent: signing.gnupg.keyName (recommended)${NC}"
    echo -e "  ${RED}  - Keyring: signing.keyId + signing.password + signing.secretKeyRingFile${NC}"
  fi
fi
echo ""

# -----------------------------------------------------------------------------
# 4. Check publishing repository credentials
# -----------------------------------------------------------------------------
echo -e "${BOLD}[4/8] Checking repository credentials...${NC}"

if [[ "$SKIP_CREDENTIAL_CHECKS" == "1" ]]; then
  warn "Skipping repository credential checks (VALIDATE_PUBLISH_SKIP_CREDENTIALS=1)"
else
# Maven Central credentials can be provided via either OSSRH or mavenCentral keys.
OSSRH_USERNAME=$(value_from_props "ossrhUsername" "$USER_GRADLE_PROPS")
OSSRH_PASSWORD=$(value_from_props "ossrhPassword" "$USER_GRADLE_PROPS")
MAVEN_CENTRAL_USERNAME=$(value_from_props "mavenCentralUsername" "$USER_GRADLE_PROPS")
MAVEN_CENTRAL_PASSWORD=$(value_from_props "mavenCentralPassword" "$USER_GRADLE_PROPS")

if [[ -n "${OSSRH_USERNAME:-}" && -n "${OSSRH_PASSWORD:-}" ]]; then
  pass "Maven Central credentials configured via ossrhUsername/ossrhPassword"
elif [[ -n "${MAVEN_CENTRAL_USERNAME:-}" && -n "${MAVEN_CENTRAL_PASSWORD:-}" ]]; then
  pass "Maven Central credentials configured via mavenCentralUsername/mavenCentralPassword"
else
  warn "Maven Central credentials not detected in ~/.gradle/gradle.properties"
  warn "Release publishing may fail unless credentials are available through environment/JReleaser"
fi

GPR_USER=$(value_from_props "gpr.user" "$USER_GRADLE_PROPS")
GPR_KEY=$(value_from_props "gpr.key" "$USER_GRADLE_PROPS")

if [[ -n "${GPR_USER:-}" && -n "${GPR_KEY:-}" ]]; then
  pass "GitHub Packages credentials configured via gpr.user/gpr.key"
elif [[ -n "${GITHUB_ACTOR:-}" && -n "${GITHUB_TOKEN:-}" ]]; then
  pass "GitHub Packages credentials configured via GITHUB_ACTOR/GITHUB_TOKEN"
else
  warn "GitHub Packages credentials not detected (gpr.user/gpr.key or GITHUB_ACTOR/GITHUB_TOKEN)"
fi
fi
echo ""

# -----------------------------------------------------------------------------
# 5. Verify GPG key is published
# -----------------------------------------------------------------------------
echo -e "${BOLD}[5/8] Verifying GPG key is published...${NC}"

if [[ "$SKIP_CREDENTIAL_CHECKS" == "1" ]]; then
  warn "Skipping public key server verification (VALIDATE_PUBLISH_SKIP_CREDENTIALS=1)"
else
KEY_TO_CHECK=""
if [[ -n "${SIGNING_GPG_KEY_NAME:-}" ]]; then
  KEY_TO_CHECK="$SIGNING_GPG_KEY_NAME"
elif [[ -n "${SIGNING_KEY_ID:-}" ]]; then
  KEY_TO_CHECK="$SIGNING_KEY_ID"
fi

if [[ -n "$KEY_TO_CHECK" ]]; then
  if gpg --keyserver keys.openpgp.org --recv-keys "$KEY_TO_CHECK" &>/dev/null; then
    pass "GPG key ${BOLD}${KEY_TO_CHECK}${NC} is published"
  else
    warn "Could not verify GPG key on keys.openpgp.org"
    warn "Ensure your key is published: gpg --keyserver keys.openpgp.org --send-keys ${KEY_TO_CHECK}"
  fi
else
  warn "Skipping (no signing key configured)"
fi
fi
echo ""

# -----------------------------------------------------------------------------
# 6. Verify publishable modules
# -----------------------------------------------------------------------------
echo -e "${BOLD}[6/8] Verifying publishable modules...${NC}"

PUBLISHABLE_MODULES=(
  "konditional-core"
  "konditional-serialization"
  "konditional-runtime"
  "konditional-observability"
  "server/rest-spec"
  "kontracts"
  "openapi"
  "openfeature"
  "opentelemetry"
)

for module in "${PUBLISHABLE_MODULES[@]}"; do
  if [[ ! -d "$module" ]]; then
    fail "${module} directory not found"
    continue
  fi

  BUILD_FILE="$module/build.gradle.kts"
  if [[ ! -f "$BUILD_FILE" ]]; then
    fail "${module} missing build.gradle.kts"
    continue
  fi

  if rg -q 'id\("konditional\.publishing"\)' "$BUILD_FILE"; then
    pass "$module"
  else
    fail "${module} missing konditional.publishing plugin"
  fi
done
echo ""

# -----------------------------------------------------------------------------
# 7. Verify publishing tasks exist
# -----------------------------------------------------------------------------
echo -e "${BOLD}[7/8] Verifying publishing task graph...${NC}"

if ./gradlew tasks --all --no-daemon | rg -q "publishMavenPublicationToGitHubPackagesRepository"; then
  pass "GitHub Packages publication tasks available"
else
  fail "publishMavenPublicationToGitHubPackagesRepository task not found"
fi

if ./gradlew tasks --all --no-daemon | rg -q "publishToMavenLocal"; then
  pass "Local publication tasks available"
else
  fail "publishToMavenLocal task not found"
fi
echo ""

# -----------------------------------------------------------------------------
# 8. Run Gradle publishing validation
# -----------------------------------------------------------------------------
echo -e "${BOLD}[8/8] Running Gradle publishing validation...${NC}"

if ./gradlew publishToMavenLocal --no-daemon --stacktrace; then
  pass "publishToMavenLocal successful"
else
  fail "publishToMavenLocal failed"
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
  echo -e "  ${BOLD}make publish-local${NC}     - Publish to local Maven repository"
  echo -e "  ${BOLD}make publish-github${NC}   - Publish to GitHub Packages"
  echo -e "  ${BOLD}make publish-release${NC}  - Publish release artifacts"
  echo -e "${BOLD}${BLUE}==============================================================================${NC}"
  exit 0
else
  echo -e "${BOLD}${RED}✗ Validation failed!${NC}"
  echo -e ""
  echo -e "Please fix the issues above before publishing."
  echo -e "See ${BOLD}docusaurus/docs/how-to-guides/publishing.md${NC} for setup instructions."
  echo -e "${BOLD}${BLUE}==============================================================================${NC}"
  exit 1
fi
