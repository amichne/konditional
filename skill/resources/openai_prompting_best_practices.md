# OpenAI Prompting and Tooling Best Practices (Skill Authoring)

Last verified: `2026-02-23` via OpenAI Developer Docs MCP.

This file captures practical guidance to keep skill instructions clear, reliable, and token-efficient when using OpenAI models/tools.

## 1) Prompt structure and instruction hierarchy

- Put high-priority behavior in `instructions` / developer messages.
- Keep prompts direct and explicit; avoid contradictory requirements.
- Structure long prompts with clear sections (`Identity`, `Instructions`, `Examples`, `Context`).
- Use Markdown/XML delimiters to separate instruction blocks and context payloads.

References:
- [Prompt engineering](https://platform.openai.com/docs/guides/prompt-engineering)
- [Reasoning best practices](https://platform.openai.com/docs/guides/reasoning-best-practices)

## 2) Reasoning-model specific guidance

- For reasoning models, prefer simple/direct prompts over chain-of-thought requests.
- Start zero-shot; add few-shot only when required for reliability.
- Be explicit about success criteria and constraints.

References:
- [Reasoning best practices](https://platform.openai.com/docs/guides/reasoning-best-practices)

## 3) Tool/function schema quality

- Use clear tool names and detailed parameter descriptions.
- Define schemas so invalid states are hard to represent (enums, constrained objects).
- Keep active tool set small when possible; evaluate accuracy as tool count grows.
- Prefer strict schema enforcement for function calls.

References:
- [Function calling](https://platform.openai.com/docs/guides/function-calling)

## 4) Structured outputs and refusal-safe handling

- Use Structured Outputs (`json_schema`) when response shape must be guaranteed.
- Handle refusals explicitly (refusal may not match your response schema).
- Keep schema and language types synchronized (SDK helpers like Pydantic/Zod where possible).

References:
- [Structured model outputs](https://platform.openai.com/docs/guides/structured-outputs)

## 5) Token, latency, and cost controls

- Keep reusable/static prompt prefixes at the beginning of prompts.
- Move user-specific dynamic content toward the end.
- Prefer shorter prompts and right-sized models for the task.
- Use prompt caching intentionally for repeated prefixes.
- In long workflows, use compaction to keep context bounded.

References:
- [Prompt caching](https://platform.openai.com/docs/guides/prompt-caching)
- [Compaction](https://platform.openai.com/docs/guides/compaction)
- [Production best practices: Managing costs](https://platform.openai.com/docs/guides/production-best-practices#managing-costs)
- [Prompt engineering: prompt caching tip](https://platform.openai.com/docs/guides/prompt-engineering#save-on-cost-and-latency-with-prompt-caching)

## 6) Minimal template for robust skill instructions

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
- Repo/module constraints...
- API/version assumptions...
```

Use this template as a base, then tighten with repository-specific invariants and test expectations.
