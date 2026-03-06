---
name: gh-issue-ops
description: "Use when a user needs GitHub issue-only operations in a repository: create issues, read issue state or history, update titles, bodies, or metadata, close or reopen issues, or associate issues with assignees, labels, milestones, projects, or other issues. Prefer `gh` when it is available and authenticated. Do not use this skill for pull requests, branches, releases, workflows, or discussions."
---

# GitHub Issue Ops

## Overview

Use `gh` to perform issue-only GitHub work from the current repository or an explicit `OWNER/REPO`.
Read the current issue state before mutating it, apply the smallest change that satisfies the request, then re-read the issue to confirm the final result.

## Preconditions

1. Verify that `gh` is installed: `command -v gh`.
2. Verify authentication: `gh auth status`.
3. Resolve the target repository.
   - Prefer the current git remote when operating inside a checkout.
   - Use `-R <owner>/<repo>` when the user names a different repository.
4. Stop and report the blocker if issue access is unavailable.

If `gh` is unavailable or unauthenticated, fall back to another GitHub tool only if one is already available in the environment. Otherwise state exactly what is blocked and do not invent issue state.

## Workflow

1. Read first.
   - Use `gh issue view` for a known issue.
   - Use `gh issue list` when the user needs discovery, filtering, or search.
   - Prefer `--json` output for deterministic inspection.
2. Confirm the mutation shape from the current issue state.
   - Avoid overwriting labels, assignees, milestones, or projects blindly.
   - Preserve issue content unless the user asked to replace it.
3. Execute the requested mutation.
   - Create with `gh issue create`.
   - Update with `gh issue edit`.
   - Change state with `gh issue close` or `gh issue reopen`.
   - Add explanatory comments with `gh issue comment` when the request implies an audit trail.
4. Re-read the issue and report the resulting number, title, state, URL, and changed associations.

## Supported Operations

### Create

Use `gh issue create` for new issues.
- Set `--title` and `--body` explicitly when the request provides enough detail.
- Add `--assignee`, `--label`, `--milestone`, or `--project` only when requested or clearly implied.
- Prefer non-interactive flags over editor or browser flows.

### Read

Use `gh issue view <number> --json ...` for a specific issue.
Use `gh issue list --json ...` with filters or `--search` for discovery.
Load comments only when needed because they are noisier and slower than the core issue record.

### Update

Use `gh issue edit` to change title, body, assignees, labels, milestones, and projects.
- Use additive flags such as `--add-label` and `--add-assignee` when preserving existing metadata matters.
- Use removal flags only for associations the user explicitly wants removed.
- For body edits, preserve existing context unless the user asked for a rewrite.

### Change State

Use `gh issue close` to close an issue and `gh issue reopen` to reopen it.
- Include a comment when the user asks for rationale or when the state transition needs context.
- Use `--reason completed` or `--reason not planned` when closing and the distinction matters.

### Associate

Handle issue associations in this order:
1. Use first-class `gh` flags for assignees, labels, milestones, and projects.
2. Use canonical issue references such as `#123` or `owner/repo#123` in the body or a comment when the user wants one issue connected to another and a direct CLI mutation is not available.
3. Use `gh api graphql` only when the user explicitly needs a GitHub-native issue-to-issue relation that `gh issue` cannot express, and only after verifying the repository capabilities and token scope. Re-read the issue after the mutation and report the exact relation created or explain that the host does not expose it.

## Guardrails

- Stay scoped to issues. Do not branch into PR review, code changes, releases, or CI workflows.
- Prefer exact issue numbers or URLs over fuzzy matching when mutating state.
- Re-read before destructive changes such as closing, removing metadata, or replacing the body.
- Avoid browser-driven `--web` flows unless the user explicitly asks for them.
- Prefer `gh` over ad hoc REST calls when the CLI already supports the operation cleanly.

## Reference

Use [gh-issue-commands.md](/Users/amichne/code/konditional/.agents/skills/gh-issue-ops/references/gh-issue-commands.md) for concrete command patterns and JSON field suggestions.
