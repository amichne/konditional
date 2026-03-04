# Skills Quick Reference

**One-page cheat sheet for the Konditional AI Skills system**

---

## Quick Start

```bash
# Generate code signatures
.agents/skills/llm-native-signature-spec/scripts/generate_signatures.sh --repo-root . --output-dir .signatures

# Build public API surface context
python3 .agents/skills/public-surface-init-context/scripts/build_public_surface_context.py --repo-root . --signatures-dir .signatures --output .signatures/PUBLIC_SURFACE.ctx

# View public API surface
cat .signatures/PUBLIC_SURFACE.ctx

# Use a skill in Copilot Chat
/skill:kotlin-mastery <your question>
```

---

## Available Skills

### 📚 Knowledge Pipeline

| Skill                                                                                                | When to Use                   | Command                              |
|------------------------------------------------------------------------------------------------------|-------------------------------|--------------------------------------|
| [`llm-native-signature-spec`](.agents/skills/llm-native-signature-spec/SKILL.md)                     | After refactors, before docs  | `generate_signatures.sh`             |
| [`public-surface-init-context`](.agents/skills/public-surface-init-context/SKILL.md)                 | Starting sessions, onboarding | `/skill:public-surface-init-context` |
| [`value-architecture-signature-linker`](.agents/skills/value-architecture-signature-linker/SKILL.md) | Linking claims to code        | `validate_claim_signature_links.py`  |

### ✍️ Documentation

| Skill                                                      | When to Use           | Command                 |
|------------------------------------------------------------|-----------------------|-------------------------|
| [`docs-authoring`](.agents/skills/docs-authoring/SKILL.md) | Writing/updating docs | `/skill:docs-authoring` |

### 🏗️ Code Quality

| Skill                                                          | When to Use           | Command                   |
|----------------------------------------------------------------|-----------------------|---------------------------|
| [`kotlin-mastery`](.agents/skills/kotlin-mastery/SKILL.md)     | Writing Kotlin code   | `/skill:kotlin-mastery`   |
| [`technical-review`](.agents/skills/technical-review/SKILL.md) | Architectural reviews | `/skill:technical-review` |

### 🔧 Workflow Automation

| Skill                                                                                                              | When to Use           | Command                              |
|--------------------------------------------------------------------------------------------------------------------|-----------------------|--------------------------------------|
| [`gh-fix-ci`](.agents/skills/gh-fix-ci/SKILL.md)                                                                   | Debugging CI failures | `/skill:gh-fix-ci`                   |
| [`konditional-maven-central-release-fastpath`](.agents/skills/konditional-maven-central-release-fastpath/SKILL.md) | Publishing releases   | `make publish`                       |
| [`kotlin-jvm-lsp-gradle-debug`](.agents/skills/kotlin-jvm-lsp-gradle-debug/SKILL.md)                               | Editor setup          | `/skill:kotlin-jvm-lsp-gradle-debug` |

### 🚀 Integration & Migration

| Skill                                                            | When to Use                          | Command                    |
|------------------------------------------------------------------|--------------------------------------|----------------------------|
| [`codebase-uplift`](.agents/skills/codebase-uplift/SKILL.md)     | Migrating legacy flag systems        | `/skill:codebase-uplift`   |

---

## Common Workflows

### New Feature Development

```bash
1. /skill:public-surface-init-context  # Understand current API
2. /skill:kotlin-mastery               # Write implementation
3. /skill:technical-review             # Review design
4. /skill:docs-authoring               # Document feature
5. python3 .agents/skills/value-architecture-signature-linker/scripts/validate_claim_signature_links.py --repo-root . --links-file docs/claim-trace/claim-signature-links.json --report-out docs/claim-trace/claims-report.json --auto-refresh
```

### Migrating Legacy Flag Systems

```bash
1. /skill:codebase-uplift              # Start migration guidance
2. # Create inventory using template
3. # Design namespaces and contexts
4. /skill:kotlin-mastery               # Implement migration adapter
5. # Deploy dual-read with mismatch telemetry
6. # Analyze and fix mismatches
7. # Gradual promotion to Konditional
8. # Remove legacy system
```

### Fixing Failing CI

```bash
1. /skill:gh-fix-ci analyze PR #123
2. # AI proposes fix
3. # Review and approve
4. # AI applies fix
```

### Documentation Update

```bash
1. .agents/skills/llm-native-signature-spec/scripts/generate_signatures.sh --repo-root . --output-dir .signatures
2. /skill:docs-authoring              # Update page
3. python3 .agents/skills/value-architecture-signature-linker/scripts/validate_claim_signature_links.py --repo-root . --links-file docs/claim-trace/claim-signature-links.json --report-out docs/claim-trace/claims-report.json --auto-refresh
```

---

## Key Files

| Path                                                 | Purpose                                    |
|------------------------------------------------------|--------------------------------------------|
| [`AGENTS.md`](AGENTS.md)                             | Master instructions for all AI agents      |
| [`AI_WORKFLOW_GUIDE.md`](AI_WORKFLOW_GUIDE.md)       | Full introduction (this guide's companion) |
| [`.agents/skills/`](.agents/skills/)                 | Skill definitions                          |
| [`.signatures/`](.signatures/)                       | Generated code signatures                  |
| [`docs/claim-trace/`](docs/claim-trace/)             | Claims registry                            |
| [`docusaurus/docs/theory/`](docusaurus/docs/theory/) | Theory and invariants                      |

---

## Essential Make Targets

```bash
make build          # Compile all modules
make test           # Run test suite
make detekt         # Static analysis
make check          # All quality gates
make docs-build     # Build documentation site
make publish        # Interactive release flow
```

---

## Theory Docs (Required Reading)

1. [Type Safety Boundaries](docusaurus/docs/theory/type-safety-boundaries.md)
2. [Parse Don't Validate](docusaurus/docs/theory/parse-dont-validate.md)
3. [Determinism Proofs](docusaurus/docs/theory/determinism-proofs.md)
4. [Atomicity Guarantees](docusaurus/docs/theory/atomicity-guarantees.md)
5. [Namespace Isolation](docusaurus/docs/theory/namespace-isolation.md)

---

## External Resources

- [GitHub Copilot Chat](https://docs.github.com/en/copilot/using-github-copilot/asking-github-copilot-questions-in-your-ide)
- [OpenAI Structured Outputs](https://platform.openai.com/docs/guides/structured-outputs)
- [Kotlin Language Server](https://github.com/fwcd/kotlin-language-server)

---

## Questions?

1. Read the full guide: [`AI_WORKFLOW_GUIDE.md`](AI_WORKFLOW_GUIDE.md)
2. Check the specific skill's `SKILL.md` file
3. Ask the AI: it has full context on the system

---

**Remember:** Skills are living documentation. Update them as patterns evolve.
