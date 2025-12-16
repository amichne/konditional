.PHONY: help clean test build publish docs-serve docs-build docs-clean docs-install venv-create all

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

##@ Documentation

venv-create: ## Create Python virtual environment for docs
	@if [ ! -d "$(VENV_DIR)" ]; then \
		echo "$(BLUE)Creating virtual environment...$(NC)"; \
		$(PYTHON) -m venv $(VENV_DIR); \
		echo "$(GREEN)Virtual environment created$(NC)"; \
	else \
		echo "$(YELLOW)Virtual environment already exists$(NC)"; \
	fi

docs-install: venv-create ## Install documentation dependencies
	@echo "$(BLUE)Installing documentation dependencies...$(NC)"
	$(PIP) install --upgrade pip
	$(PIP) install -r $(REQUIREMENTS)
	@echo "$(GREEN)Dependencies installed$(NC)"

docs-build: docs-install ## Build documentation site
	@echo "$(BLUE)Building documentation...$(NC)"
	$(MKDOCS) build
	@echo "$(GREEN)Documentation built successfully$(NC)"

docs-serve: docs-install ## Serve documentation locally (http://127.0.0.1:8000)
	@echo "$(BLUE)Starting documentation server...$(NC)"
	$(MKDOCS) serve

docs-clean: ## Clean generated documentation
	@echo "$(BLUE)Cleaning documentation...$(NC)"
	@rm -rf site/
	@echo "$(GREEN)Documentation cleaned$(NC)"

docs-venv-clean: ## Remove documentation virtual environment
	@echo "$(BLUE)Removing virtual environment...$(NC)"
	@rm -rf $(VENV_DIR)
	@echo "$(GREEN)Virtual environment removed$(NC)

##@ Combined Tasks

full-clean: clean docs-clean docs-venv-clean ## Clean everything (build + docs + venv)
	@echo "$(GREEN)Full clean completed$(NC)"

rebuild: clean build ## Clean and rebuild
	@echo "$(GREEN)Rebuild completed$(NC)"

check: test ## Alias for test
	@echo "$(GREEN)Check completed$(NC)"