.PHONY: help clean test build publish docs-serve docs-build docs-clean docs-install venv-create all docs-docusaurus-install docs-docusaurus-serve docs-docusaurus-build detekt detekt-baseline

# Default target
.DEFAULT_GOAL := help

# Variables
VENV_DIR := docs/venv
PYTHON := python3
VENV_BIN := $(VENV_DIR)/bin
PIP := $(VENV_BIN)/pip
MKDOCS := $(VENV_BIN)/mkdocs
GRADLEW := ./gradlew
REQUIREMENTS := requirements.txt

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
NC := \033[0m # No Color

##@ General

help: ## Display this help message
	@echo "$(BLUE)Konditional Makefile$(NC)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "Usage:\n  make $(GREEN)<target>$(NC)\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2 } /^##@/ { printf "\n$(YELLOW)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

all: clean build test docs-build ## Clean, build, test, and generate docs

##@ Gradle Tasks

clean: ## Clean build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	$(GRADLEW) clean

build: ## Build the project
	@echo "$(BLUE)Building project...$(NC)"
	$(GRADLEW) build

test: ## Run tests
	@echo "$(BLUE)Running tests...$(NC)"
	$(GRADLEW) test

publish: ## Publish to Maven Local
	@echo "$(BLUE)Publishing to Maven Local...$(NC)"
	$(GRADLEW) publishToMavenLocal

publish-sonatype: ## Publish to Sonatype (requires credentials)
	@echo "$(BLUE)Publishing to Sonatype...$(NC)"
	$(GRADLEW) publishToSonatype

compile: ## Compile Kotlin code
	@echo "$(BLUE)Compiling Kotlin code...$(NC)"
	$(GRADLEW) compileKotlin

compile-test: ## Compile test code
	@echo "$(BLUE)Compiling test code...$(NC)"
	$(GRADLEW) compileTestKotlin

detekt: ## Run Detekt static analysis
	@echo "$(BLUE)Running Detekt...$(NC)"
	$(GRADLEW) detekt

detekt-baseline: ## Generate Detekt baseline (suppress existing issues)
	@echo "$(BLUE)Generating Detekt baseline...$(NC)"
	$(GRADLEW) detektBaseline
	@echo "$(GREEN)Detekt baseline generated at detekt-baseline.xml$(NC)"

##@ Documentation

docs-install: ## Install Docusaurus dependencies (in ./docusaurus)
	@echo "$(BLUE)Installing Docusaurus dependencies...$(NC)"
	@cd docusaurus && npm install
	@echo "$(GREEN)Docusaurus dependencies installed$(NC)"

docs-build: docs-docusaurus-install ## Build the Docusaurus site
	@echo "$(BLUE)Building Docusaurus site...$(NC)"
	@cd docusaurus && npm run build
	@echo "$(GREEN)Docusaurus built successfully$(NC)"

docs-serve: docs-docusaurus-install ## Serve Docusaurus locally (http://localhost:3000/konditional/)
	@echo "$(BLUE)Starting Docusaurus server...$(NC)"
	@cd docusaurus && npm run start

docs-clean: ## Clean generated documentation
	@echo "$(BLUE)Cleaning documentation...$(NC)"
	@rm -rf site/
	@echo "$(GREEN)Documentation cleaned$(NC)"

##@ Combined Tasks

full-clean: clean docs-clean docs-venv-clean ## Clean everything (build + docs + venv)
	@echo "$(GREEN)Full clean completed$(NC)"

rebuild: clean build ## Clean and rebuild
	@echo "$(GREEN)Rebuild completed$(NC)"

check: test ## Alias for test
	@echo "$(GREEN)Check completed$(NC)"
