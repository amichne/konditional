# OpenAI prompting and tooling best practices (skill authoring)

Last verified: `2026-02-23` against official OpenAI docs.

This file captures practical guidance for writing efficient, reliable skills
that operate under strict token budgets and high integration correctness.

## 1) Instruction hierarchy and prompt layout

- Put durable policy in `instructions` (or developer messages), not user text.
- Keep prompts explicit, short, and conflict-free.
- Structure long prompts into: `Identity`, `Instructions`, `Examples`,
  `Context`.
- Use clear delimiters (Markdown headings or XML tags) between policy and data.
- Do not assume text is always `output[0].content[0].text`; parse full output
  items safely.

References:
- [Prompt engineering](https://platform.openai.com/docs/guides/prompt-engineering)
- [Reasoning best practices](https://platform.openai.com/docs/guides/reasoning-best-practices)

## 2) Prompting reasoning models vs GPT models

- Reasoning models: start simple and direct, zero-shot first.
- Avoid explicit chain-of-thought instructions like "think step by step."
- Add few-shot only when required for specific format or edge behavior.
- State concrete success criteria and constraints.

References:
- [Reasoning best practices](https://platform.openai.com/docs/guides/reasoning-best-practices)
- [Prompt engineering](https://platform.openai.com/docs/guides/prompt-engineering)

## 3) Tool and function schema discipline

- Use clear function names and complete field descriptions.
- Set `strict: true` for function schemas when possible.
- For strict schemas:
  - set `additionalProperties: false` for object parameters
  - mark expected fields as required
  - model optional fields as nullable types
- Keep the active toolset small; large toolsets reduce selection accuracy.
- Restrict tools per request (`allowed_tools`) when only a subset is needed.

References:
- [Function calling](https://platform.openai.com/docs/guides/function-calling)
- [Structured outputs](https://platform.openai.com/docs/guides/structured-outputs)

## 4) Structured outputs and refusal-safe parsing

- Use `json_schema` when shape guarantees matter.
- Treat refusal as a first-class output path.
- Keep response schemas and language types in sync (Pydantic/Zod helpers when
  available).
- Add CI checks to detect schema/type drift in production pipelines.

References:
- [Structured outputs](https://platform.openai.com/docs/guides/structured-outputs)

## 5) Cost, latency, and context controls

- Place reusable/static prefix content first; append dynamic user content last.
- Prompt caching requires exact prefix match; standardize shared prefixes.
- Use `prompt_cache_key` consistently for repeated workloads.
- Long-running sessions should use compaction to control context growth.
- Track `cached_tokens`, latency, and token usage in telemetry.

References:
- [Prompt caching](https://platform.openai.com/docs/guides/prompt-caching)
- [Compaction](https://platform.openai.com/docs/guides/compaction)
- [Production best practices](https://platform.openai.com/docs/guides/production-best-practices#managing-costs)
- [Prompt engineering](https://platform.openai.com/docs/guides/prompt-engineering#save-on-cost-and-latency-with-prompt-caching)

## 6) Enterprise delivery checklist for skills

- Define deterministic acceptance criteria before generation.
- Make output format machine-checkable when consumed by CI or automation.
- Include fallback behavior for missing context/tool failures.
- Add eval cases for high-risk flows (tool routing, schema adherence, refusals).
- Keep instructions compact and reusable for maximum cache hit rate.

## 7) Minimal instruction template

```text
Identity:
- You are a [role] optimizing for [primary objective].

Instructions:
- Always do [required behavior].
- Never do [forbidden behavior].
- If [edge case], then [fallback behavior].

Examples:
- Input: ...
  Output: ...

Context:
- Repository/module invariants...
- API/version assumptions...
- Verification/test expectations...
```
