# AI-Assisted Development Workflow Guide

**A Team Introduction to the Konditional Skills System**

---

## What This Document Is About

This guide explains how the Konditional repository uses **AI agents with specialized skills** to maintain code quality, documentation, and development workflows. If you're new to AI-assisted development, think of it as having domain experts available 24/7—each with deep knowledge of specific tasks, access to project context, and the ability to execute complex workflows automatically.

This is not about using AI for one-off code generation. This is about **building a living system of reusable knowledge** that makes your entire team more effective.

---

## The Core Idea: Skills as Executable Knowledge

### Traditional Approach
You have tribal knowledge scattered across:
- Wiki pages that get outdated
- Slack threads that disappear
- Senior developers' heads
- "Just look at how we did it last time"

### The New Way
**Skills are executable documentation**. Each skill:
1. **Encodes domain expertise** in a structured format ([see SKILL.md schema](file:///Users/amichn/code/personal/konditional/.agents/skills/kotlin-mastery/SKILL.md))
2. **Includes specific commands and workflows** the AI can execute
3. **Links to relevant files, theory docs, and external resources**
4. **Enforces quality gates and invariants** automatically
5. **Evolves with your codebase** as it changes

[Learn more about GitHub Copilot Skills](https://docs.github.com/en/copilot/using-github-copilot/using-extensions-to-integrate-external-tools-with-copilot-chat)

---

## The Knowledge Pipeline: How Context Flows

### Stage 1: Code → Signatures (Machine-Readable Documentation)

**Skill: [`llm-native-signature-spec`](file:///Users/amichn/code/personal/konditional/.agents/skills/llm-native-signature-spec/SKILL.md)**

Instead of having AI read entire source files every time, we extract **type signatures** into a compressed format:

```
# signatures/konditional-core/src/main/kotlin/io/amichne/konditional/api/Namespace.sig

package=io.amichne.konditional.api
file=Namespace.kt

type=class io.amichne.konditional.api.Namespace
  method=evaluate(context: Context): FeatureValue
  method=load(configuration: Configuration)
  property=id: NamespaceId
```

**Why this matters:**
- AI agents can scan 100+ signature files in seconds vs. reading full source
- Changes to public APIs are immediately visible
- Downstream skills know exactly what's available without guessing

**When to use:**
- After refactoring modules
- Before generating documentation
- When setting up new AI workflows

[Read the signature spec schema](file:///Users/amichn/code/personal/konditional/skills/llm-native-signature-spec/references/signature_spec.md)

---

### Stage 2: Signatures → Public Surface Context

**Skill: [`public-surface-init-context`](file:///Users/amichn/code/personal/konditional/.agents/skills/public-surface-init-context/SKILL.md)**

From the full signature tree, generate a **compact initialization context** containing only public APIs:

```
repository=Konditional
scope=public_surface_only
modules=9

[konditional-core]
public_types=15
- io.amichne.konditional.api.Namespace
- io.amichne.konditional.api.Feature
- io.amichne.konditional.context.Context
...
```

**Why this matters:**
- New AI sessions start with the **right mental model** instantly
- No accidental reliance on internal implementation details
- Documentation stays aligned with what users actually see

**When to use:**
- Starting a new coding session
- Onboarding new team members
- Before architectural reviews

[See the public context schema](file:///Users/amichn/code/personal/konditional/.agents/skills/public-surface-init-context/references/public_context_schema.md)

---

### Stage 3: Documentation That Stays True

**Skill: [`docs-authoring`](file:///Users/amichn/code/personal/konditional/.agents/skills/docs-authoring/SKILL.md)**

Documentation is written using:
- **Only public API signatures** (no accidental internal leaks)
- **Claim anchors** that link narrative to code (`<span id="claim-42">`)
- **Disclosure tiers** (L0 = quick start, L1 = how-to, L2 = deep theory)
- **Prerequisite tracking** (readers must complete X before Y)

Example constraint from the skill:
```markdown
### Do Not
- Reference classes from `internal/` packages.
- Describe Moshi adapters, builder internals, or serialization 
  wire details beyond documented schema.
- Use marketing language, superlatives, or unsupported claims.
```

**Why this matters:**
- Documentation can't lie about what the code does
- Refactors that break docs are caught immediately
- New developers get accurate mental models from day one

**When to use:**
- Writing quickstart guides
- Explaining new features
- Updating docs after API changes

[View the Docusaurus docs structure](file:///Users/amichn/code/personal/konditional/docusaurus/docs/)

---

### Stage 4: Linking Claims to Proofs

**Skill: [`value-architecture-signature-linker`](file:///Users/amichn/code/personal/konditional/.agents/skills/value-architecture-signature-linker/SKILL.md)**

This creates a **bidirectional map**:
- **Claim**: "Namespace updates are atomic" (in docs)
- **Proof**: Links to `AtomicReference<Configuration>` in source + test proving it

Generated artifacts:
- [`claims-registry.json`](file:///Users/amichn/code/personal/konditional/docs/claim-trace/claims-registry.json) — All claims
- [`claim-signature-links.json`](file:///Users/amichn/code/personal/konditional/docs/claim-trace/claim-signature-links.json) — Claims → code mappings

**Why this matters:**
- Marketing claims are backed by implementation reality
- Refactors that violate documented guarantees fail CI
- Users trust your docs because they're **verifiably true**

**When to use:**
- Before major releases
- When architectural decisions need justification
- During contract negotiations (yes, really—enterprise teams care)

[Read the claims registry theory doc](file:///Users/amichn/code/personal/konditional/docusaurus/docs/theory/claims-registry.md)

---

## Language-Specific Skills

### Kotlin Mastery

**Skills:**
- [`kotlin-mastery`](file:///Users/amichn/code/personal/konditional/.agents/skills/kotlin-mastery/SKILL.md)
- [`kotlin-architect`](file:///Users/amichn/.agents/skills/kotlin-architect/SKILL.md)

Enforce Konditional's strict quality bar:
- **Type-driven design**: Use `sealed interface`, `@JvmInline value class`, exhaustive `when`
- **Parse, don't validate**: External input → typed `ParseResult<T>` with structured errors
- **Determinism**: No ambient time, no unstable iteration order, no randomness in core logic
- **Atomicity**: Readers never see partial updates (via `AtomicReference<Snapshot>`)

Example rule from the skill:
```markdown
### Hard invariants (do not violate)
1. Model domain constraints using Kotlin types
2. No exceptions for control flow at boundaries
3. Same inputs must yield same outputs
4. Prefer immutable snapshots swapped atomically
```

**Why this matters:**
- Code reviews catch issues before humans waste time
- New contributors get instant feedback on idiomatic patterns
- The codebase maintains consistency even as the team grows

[Read the type safety boundaries theory](file:///Users/amichn/code/personal/konditional/docusaurus/docs/theory/type-safety-boundaries.md)

---

### Technical Review & Architecture Analysis

**Skill: [`technical-review`](file:///Users/amichn/code/personal/konditional/.agents/skills/technical-review/SKILL.md)**

On-demand architectural critique:
- Identify coupling, complexity, and blast radius issues
- Suggest simplifications and alternative designs
- Validate against enterprise patterns (atomicity, observability, migration)

**When to use:**
- Before merging large refactors
- When feature complexity feels too high
- During quarterly architecture reviews

[Example review checklist](file:///Users/amichn/code/personal/konditional/.agents/skills/technical-review/SKILL.md#L50-L100)

---

## Workflow Automation Skills

### GitHub CI Debugging

**Skill: [`gh-fix-ci`](file:///Users/amichn/code/personal/konditional/.agents/skills/gh-fix-ci/SKILL.md)**

Instead of manually:
1. Opening GitHub Actions
2. Scrolling through logs
3. Guessing what failed
4. Copy-pasting errors into Slack

The agent:
1. Fetches the PR check status via `gh api`
2. Downloads failing logs
3. Identifies root cause
4. Proposes a fix
5. Waits for approval, then applies it

[GitHub CLI documentation](https://cli.github.com/manual/)

---

### Maven Central Release

**Skill: [`konditional-maven-central-release-fastpath`](file:///Users/amichn/code/personal/konditional/.agents/skills/konditional-maven-central-release-fastpath/SKILL.md)**

Codifies the entire release workflow:
- Version bumping
- Signing artifacts
- Publishing to staging
- Promoting to release
- Creating GitHub releases

**Before**: 30-minute manual process with 6 places to make mistakes

**After**: One command, AI handles the sequence, fails fast if prerequisites are missing

[Konditional publish script](file:///Users/amichn/code/personal/konditional/scripts/publish.sh)

---

### LSP & Debugging Setup

**Skill: [`kotlin-jvm-lsp-gradle-debug`](file:///Users/amichn/code/personal/konditional/.agents/skills/kotlin-jvm-lsp-gradle-debug/SKILL.md)**

Standardizes:
- Language server setup (so any editor works, not just IntelliJ)
- Gradle wrapper conventions
- JDWP debugging configuration

**Why this matters:**
- New contributors don't need a 2-hour setup call
- CI environment matches local dev exactly
- Editor choice doesn't create tribal knowledge silos

[Kotlin Language Server docs](https://github.com/fwcd/kotlin-language-server)

---

## How to Use This System

### For New Team Members

1. **Start with public surface context:**
   ```bash
   make signatures  # Generates .signatures/ tree
   cat signatures/PUBLIC_SURFACE.ctx
   ```
   This is your "map" of the codebase.

2. **Read the theory docs** linked in [`AGENTS.md`](file:///Users/amichn/code/personal/konditional/AGENTS.md#L70-L100):
   - [Type Safety Boundaries](file:///Users/amichn/code/personal/konditional/docusaurus/docs/theory/type-safety-boundaries.md)
   - [Determinism Proofs](file:///Users/amichn/code/personal/konditional/docusaurus/docs/theory/determinism-proofs.md)
   - [Atomicity Guarantees](file:///Users/amichn/code/personal/konditional/docusaurus/docs/theory/atomicity-guarantees.md)

3. **Trigger skills by prefix:**
   - Ask: `/skill:kotlin-mastery How do I model this domain constraint?`
   - Ask: `/skill:docs-authoring Write a quickstart for custom contexts`

---

### For Maintainers

1. **Keep signatures fresh:**
   ```bash
   make signatures  # Run after refactors
   git add signatures/
   ```

2. **Update skills when patterns change:**
   Edit `.agents/skills/{skill-name}/SKILL.md` directly.

3. **Link new claims to code:**
   ```bash
   make claim-trace  # Regenerates claim-signature-links.json
   ```

---

### For Code Reviewers

When reviewing PRs, ask the AI:
- `/skill:technical-review Analyze this module for coupling and complexity`
- `/skill:kotlin-mastery Does this violate any hard invariants?`
- "Does this change require updating documentation?"

The agent will reference the skills automatically.

---

## What Makes This Different from "Just Using ChatGPT"

| Traditional AI Chat | Konditional Skills System |
|---------------------|---------------------------|
| Forgets context between sessions | Persistent, versioned knowledge |
| Generic advice | Domain-specific, project-aware |
| No code execution | Runs tests, checks CI, validates invariants |
| Can't enforce standards | Enforces quality gates automatically |
| Output quality depends on prompt | Output quality encoded in skill |
| Manual copy-paste workflow | Automated end-to-end workflows |

---

## Measuring Success

You'll know this is working when:

1. **Onboarding time drops**: New devs contribute meaningful PRs on day 2
2. **Docs stay accurate**: Claims are backed by code, drift is caught in CI
3. **Code reviews focus on design**: Style/convention issues caught by skills
4. **Releases are boring**: No more "did we remember to…" checklists
5. **Knowledge scales horizontally**: Junior dev + AI skill ≈ senior dev bandwidth

---

## Common Pitfalls

### ❌ Treating skills as static documentation
Skills must evolve with the codebase. Update them during refactors, not after.

### ❌ Over-specifying early
Start with 2-3 critical skills (e.g., coding standards + docs authoring). Add more as patterns emerge.

### ❌ Skipping signature generation
If signatures are stale, every downstream skill gets worse context. Run `make signatures` often.

### ❌ Not enforcing skill usage
If code reviews don't reference skills, they become ignored. Make `/skill:technical-review` part of your PR template.

---

## External Resources

### GitHub Copilot
- [Copilot Chat documentation](https://docs.github.com/en/copilot/using-github-copilot/asking-github-copilot-questions-in-your-ide)
- [Copilot Extensions](https://docs.github.com/en/copilot/using-github-copilot/using-extensions-to-integrate-external-tools-with-copilot-chat)
- [Agent SDK (for building custom skills)](https://github.com/github-copilot-resources/copilot-agents-sdk)

### OpenAI Developer Resources
- [Structured Outputs](https://platform.openai.com/docs/guides/structured-outputs) — how we enforce skill schemas
- [Function Calling](https://platform.openai.com/docs/guides/function-calling) — executing commands from skills
- [Prompt Engineering Guide](https://platform.openai.com/docs/guides/prompt-engineering) — designing effective skills

### Kotlin & JVM
- [Kotlin Language Server](https://github.com/fwcd/kotlin-language-server) — editor-agnostic tooling
- [Gradle Build Tool](https://docs.gradle.org/) — used by all automation scripts
- [Detekt](https://detekt.dev/) — static analysis (complementary to AI skills)

---

## Next Steps

1. **Explore the skills directory:**
   ```bash
   ls -R .agents/skills/
   ```

2. **Run your first skill:**
   Open a Copilot chat and type:
   ```
   /skill:public-surface-init-context Show me the public API surface
   ```

3. **Read a theory doc:**
   Start with [Parse, Don't Validate](file:///Users/amichn/code/personal/konditional/docusaurus/docs/theory/parse-dont-validate.md)

4. **Ask questions:**
   Skills are most powerful when you interrogate them. Ask:
   - "Why does this skill enforce X?"
   - "What would break if I violated constraint Y?"
   - "Show me an example of this pattern in the codebase"

---

## Questions?

This is a living document. If something isn't clear:

1. **Check the skill itself** — it contains the source of truth
2. **Read the linked theory docs** — they explain *why* not just *what*
3. **Ask the AI** — it has context on the entire system

The goal is not to replace human judgment, but to **amplify it** by encoding the team's collective knowledge into reusable, executable skills.

Welcome to the new way of working. 🚀
