# Prompt Conventions

Cross-cutting rules that apply to all domain prompts in this directory.

## Prompt Structure

Every domain prompt should follow this structure:

```markdown
# Context: [Domain Name]

[One-sentence role assignment]

## Scope
[Explicit list of what's in scope]

## Out of Scope
[Explicit list of what's deferred to other domains, with references]

## Audience
[Who is reading the output and what they need]

## Key Concepts
[Domain-specific terminology and definitions]

## Constraints
[Style, format, and quality requirements]

## Examples (optional)
[Good/bad output examples]

## Context Injection Point
[Where to insert code snippets, type signatures, etc.]
```

## Cross-Domain References

When a topic spans multiple domains, use explicit handoffs:

```markdown
## Out of Scope (defer to other domains)
- Thread-safety mechanisms → See `04-reliability-guarantees.md`
- JSON serialization internals → See `05-configuration-integrity.md`
```

This prevents context bleeding while maintaining navigability.

## Voice and Tone

All prompts should produce output that is:

- **Precise**: Use exact terminology; avoid hedging when certainty exists
- **Grounded**: Reference actual code, types, and mechanisms
- **Bounded**: State what's guaranteed and what's not
- **Accessible**: Assume Kotlin familiarity but not library familiarity (for public docs)

## Code Examples

- Examples should be copy-paste runnable where possible
- Use the actual Konditional API, not pseudocode
- Show the minimal example that demonstrates the concept
- For complex scenarios, build incrementally

## Terminology Consistency

Use these terms consistently across all domains:

| Term | Definition |
|------|------------|
| **Feature** | A typed configuration value with optional targeting rules |
| **Namespace** | An isolation boundary for feature registries |
| **Context** | Runtime information used for rule evaluation (locale, platform, version, stableId) |
| **Rule** | A set of criteria that, when matched, returns a specific value |
| **Specificity** | The number of criteria in a rule; higher specificity wins |
| **Bucketing** | SHA-256-based assignment of users to percentage ranges |
| **Salt** | Per-feature string that affects bucket distribution |
| **Snapshot** | Serialized configuration state |

## Handling Uncertainty

When a prompt addresses topics where guarantees are qualified:

1. State the guarantee precisely
2. State the conditions under which it holds
3. State what can still go wrong
4. Avoid absolute claims that aren't defensible

Example:
> "Type safety is enforced at compile-time for statically-defined features. Dynamically-loaded configurations are validated at the deserialization boundary via `ParseResult`."

## Context Injection

Prompts may request specific context to be appended. Common injection points:

- `[INSERT: Core type signatures]` → Paste from `context/core-types.kt`
- `[INSERT: Relevant source file]` → Paste specific implementation
- `[INSERT: User's specific question]` → The actual task at hand

## Output Artifacts

When a prompt produces a document (brief, guide, reference), it should:

1. Be self-contained (readable without the prompt)
2. Include a clear title and date/version
3. State its scope at the top
4. Be saveable to `outputs/` for future reference

## Versioning

When the codebase changes significantly:

1. Review each domain prompt for accuracy
2. Update `context/` files via extraction script
3. Note breaking changes in prompt headers if needed
