# Konditional — Agent Instructions (AGENTS.md)

You are an agent working inside the **Konditional** repository. Your job is to produce **top-tier Kotlin** implementations: type-driven, deterministic, and enterprise-grade. Favor compile-time guarantees over runtime checks. Do not introduce dependency injection frameworks or reflection-heavy registries unless explicitly required.

## Repository map (expected)
Top-level modules/directories you should assume exist and prefer:
- `konditional-core/` — core algebra, types, evaluation semantics
- `konditional-runtime/` — runtime registry, lifecycle, snapshot/atomic updates
- `konditional-serialization/` — JSON boundary, codecs, parse results
- `konditional-observability/` — explainability, tracing, shadowing/mismatch reporting
- `konditional-spec/` — specifications, contracts, conformance fixtures
- `openapi/` — OpenAPI artifacts and generation inputs/outputs
- `openfeature/` — OpenFeature integration surface
- `kontracts/` — Kontract DSL / schema tooling (OpenAPI generation, etc.)
- `build-logic/` — Gradle plugins/conventions used by the build
- `detekt-rules/` — static analysis rules
- `llm-docs/` — design documents and invariants you must obey
- `docusaurus/` — documentation site

If a referenced file is missing, **search for it** (filename or closest equivalent) before proceeding.

---

## Codex 5.3 Runtime Profile (Current Default)

Use Codex with a model/runtime profile optimized for complex Kotlin and cross-module work.

### Default profile
- Default model: `gpt-5.3-codex`
- Default reasoning effort: `xhigh` when supported by the active model; otherwise `high`
- Default verbosity: `medium`
- Reduce reasoning effort only for trivial edits or when latency is more important than depth.

### Launch commands
```bash
# Preferred default for complex work
codex --model gpt-5.3-codex -c 'model_reasoning_effort="xhigh"' -c 'model_verbosity="medium"'

# Lower-latency variant for small edits
codex --model gpt-5.3-codex -c 'model_reasoning_effort="high"' -c 'model_verbosity="medium"'
```

### In-session controls
- Use `/model` to switch model or reasoning depth.
- Use `/status` to confirm model, approvals, writable roots, and token state.
- Use `/compact` after long threads to preserve headroom without losing task context.

---

## Instruction Chain Discipline (AGENTS Precedence)

Codex loads instructions in a strict chain:
1) Global (`~/.codex/AGENTS.override.md` or `~/.codex/AGENTS.md`)
2) Project path (`AGENTS.override.md` then `AGENTS.md`, from repo root down to current directory)
3) Deeper path instructions override broader path instructions.

Keep instruction files explicit and scoped. Split nested concerns into deeper directories rather than growing one monolithic file.

### Verification commands
```bash
codex --ask-for-approval never "Summarize the current instructions."
codex --cd <subdir> --ask-for-approval never "Show which instruction files are active."
```

### Audit path
- `~/.codex/log/codex-tui.log`

---

## Beads Is Persistent Memory (Required)

Treat Beads as the canonical working memory for planning, status, and handoff. Do not rely on chat transcript memory for project state.

### Session start
```bash
bd ready --limit 50
bd list --status in_progress
```

### During execution
```bash
# Capture new work or discoveries
bd create "<clear task title>"

# Claim/track active work
bd update <issue-id> --status in_progress

# Append implementation notes, decisions, and blockers
bd update <issue-id> --append-notes "<what changed and why>"
```

### Session close
```bash
# Close completed issues and sync to git-native JSONL
bd update <issue-id> --status closed --notes "<verification + outcome>"
bd sync
```

---

## IntelliJ-Native MCP Navigation (idea-first)

For symbol-aware operations, use IntelliJ MCP tools first. Plain text search (`rg`) is fallback only when symbol identity is irrelevant.

### MCP server checks
```bash
codex mcp list --json
codex mcp get idea --json
```

If `idea` is missing in a local setup:
```bash
codex mcp add idea --url http://127.0.0.1:64343/stream
```

### Preferred symbol operations (when exposed by the idea/intellij index MCP)
- Definition lookup
- Reference search
- Implementation search
- Type hierarchy
- Safe rename refactor
- IDE diagnostics after non-trivial edits

Fallback rule:
- Use `rg`/`rg --files` for plain-text discovery, logs, and non-symbol searches only.

---

## Hard invariants (do not violate)

### 1) Kotlin-first, type-safe by default
- Model domain constraints using Kotlin types: `sealed` ADTs, `value class` identifiers, delegating wrappers, variance, `inline` + `reified` generics where helpful.
- Prefer exhaustive `when` over open hierarchies.
- Avoid nullability; model absence explicitly.

### 2) Parse, don’t validate (boundary discipline)
- Treat all external inputs (JSON, OpenAPI payloads, HTTP bodies, files) as **untrusted**.
- Decode into a **trusted typed model** that cannot represent invalid state.
- No exceptions for control flow at boundaries: return typed error ADTs.

### 3) Determinism by construction
- Same inputs must yield same outputs.
- No ambient time, randomness, global state, or unstable iteration order in core evaluation.
- Any ordering must be explicitly stabilized with deterministic tie-breakers.

### 4) Atomic snapshot state
- Readers must never observe partial updates.
- Prefer immutable snapshots swapped atomically (e.g., `AtomicReference<Snapshot>`).
- Updates must be linearizable: either old snapshot or new snapshot.

### 5) Namespace isolation and blast radius control
- Keep operations scoped: namespace → feature → flag/rule (as applicable).
- Avoid global registries that mix concerns or allow accidental cross-namespace mutation.

### 6) Migration/shadowing without behavior drift
- Support dual-run/shadow evaluation and mismatch reporting without changing baseline results.
- Observability must not alter evaluation semantics.

---

## Required reading (repo-relative)
Treat these as source-of-truth for constraints and terminology:
- [`docusaurus/docs/theory/type-safety-boundaries.md`](docusaurus/docs/theory/type-safety-boundaries.md)
- [`docusaurus/docs/theory/namespace-isolation.md`](docusaurus/docs/theory/namespace-isolation.md)
- [`docusaurus/docs/theory/determinism-proofs.md`](docusaurus/docs/theory/determinism-proofs.md)
- [`docusaurus/docs/theory/parse-dont-validate.md`](docusaurus/docs/theory/parse-dont-validate.md)
- [`docusaurus/docs/theory/atomicity-guarantees.md`](docusaurus/docs/theory/atomicity-guarantees.md)
- [`docusaurus/docs/theory/migration-and-shadowing.md`](docusaurus/docs/theory/migration-and-shadowing.md)
- [`.signatures/INDEX.sig`](.signatures/INDEX.sig)

Schema/contract inputs (often needed for boundary work):
- `openapi/` (OpenAPI specs/artifacts)
- `openapi.json` or `openapi/*.json` (if present)
- `konditional-serialization/` (codecs)
- `kontracts/` (OpenAPI/spec generation DSL)

---

## What “world-class Kotlin” means here (quality bar)

### Public API discipline
- Small, opinionated, stable surface. Hide internals aggressively (`internal`, sealed boundaries, package scoping).
- Every public type/function must have KDoc describing:
    - invariants
    - determinism assumptions
    - boundary expectations
    - error semantics

### Total, explicit error handling
- Prefer `sealed interface Error` + `sealed interface Result` patterns.
- No `null`, no `Throwable` propagation across module boundaries unless explicitly defined as part of the API contract.

### Testing requirements (non-negotiable)
For any meaningful change, add tests that prove invariants:
- **Determinism tests**: same inputs → same output; ordering stabilized.
- **Boundary tests**: decoding failures produce typed errors with precise paths/fields.
- **Atomicity tests**: concurrent read/write never yields partial state; readers observe whole snapshots.
- **Namespace isolation tests**: operations cannot leak across namespaces.
- **Golden fixtures**: JSON fixtures for serialization/openapi shapes when relevant.

Prefer property-based tests when they increase confidence (ordering, bucketing, reducers).

---

## Agent workflow (how you should operate)

### Step 0 — Locate invariants and the right module
Before writing code:
1) Identify which modules are affected (`konditional-core`, `runtime`, `serialization`, `observability`, `spec`).
2) Read the relevant `llm-docs/*` files for invariants involved.
3) Search for existing patterns/types; reuse them rather than inventing parallel structures.

### Step 1 — Write an “Assertability Plan” in your head
Do not output it unless asked, but you must follow it:
- List invariants to preserve.
- List tests to add proving each invariant.
- Identify boundary points and ensure parse/don’t-validate is respected.

### Step 2 — Implement in vertical slices
1) Core types / ADTs (pure)
2) Pure evaluation semantics (deterministic)
3) Runtime snapshot/registry (atomic)
4) Serialization boundary (typed parsing + errors)
5) Observability/shadow/migration hooks

### Step 3 — Prove it
- Add tests before declaring completion.
- Keep hot paths allocation- and complexity-aware (but don’t micro-optimize prematurely).

---

## Kotlin style constraints (strong opinions)
- Prefer:
    - `sealed interface` for domain sums
    - `@JvmInline value class` for identifiers and stable keys
    - `data class` for immutable product types
    - `object` singletons only for stateless values (no mutable caches)
- Avoid:
    - reflection-based type registries
    - global mutable singletons
    - DI frameworks
    - `Map<String, Any>` style payload models in core logic

---

## Completion checklist (must be true before you stop)
- Code compiles.
- Tests added and pass.
- Public API documented (KDoc).
- Determinism, atomicity, isolation, and boundary constraints are preserved.
- No new “stringly typed” identifiers or ad-hoc parsing in core modules.
- Any new JSON shape is backed by fixtures and (where applicable) OpenAPI/schema alignment.

```xml
<!-- Optional structural prompt markers (kept literal via fenced block) -->
<kotlin_mastery_rules enabled="true" />
```

## Landing the Plane (Session Completion)

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   git pull --rebase
   bd sync
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds

Always use the OpenAI developer documentation MCP server (`openaiDeveloperDocs`) for OpenAI API, Codex, Apps SDK, Agents SDK, and model/tooling questions. Use web fallback only when MCP yields no meaningful results.
