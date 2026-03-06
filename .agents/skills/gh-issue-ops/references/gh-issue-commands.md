# GitHub Issue Commands

Use these commands as stable templates for issue-only work.
Add `-R <owner>/<repo>` when operating outside the current checkout.

## Environment checks

```bash
command -v gh
gh auth status
git remote -v
```

## Read and discover

```bash
gh issue view 123 --json number,title,body,state,stateReason,assignees,labels,milestone,projectItems,url,updatedAt
gh issue view https://github.com/OWNER/REPO/issues/123 --comments
gh issue list --state open --limit 50 --json number,title,state,labels,assignees,url
gh issue list --search 'label:"bug" sort:updated-desc' --state all --json number,title,state,updatedAt,url
```

Prefer `--json` output for machine-readable inspection. Add `comments` only when the request requires comment history.

## Create

```bash
gh issue create --title "Title" --body "Body"
gh issue create --title "Title" --body "Body" --label bug --assignee "@me"
gh issue create --title "Title" --body "Body" --milestone "v1.2.0" --project "Roadmap"
```

## Update

```bash
gh issue edit 123 --title "New title"
gh issue edit 123 --body-file /tmp/issue-body.md
gh issue edit 123 --add-label bug --remove-label triage
gh issue edit 123 --add-assignee "@me" --remove-assignee octocat
gh issue edit 123 --milestone "v1.2.0"
gh issue edit 123 --remove-milestone
gh issue edit 123 --add-project "Roadmap" --remove-project "Inbox"
```

Use additive edits unless the user explicitly asks to replace or remove metadata.

## Change state

```bash
gh issue close 123 --reason completed
gh issue close 123 --reason not planned --comment "Closing because this is outside the current roadmap."
gh issue reopen 123 --comment "Reopening after reproducing the issue again."
```

## Comment and soft-link issues

```bash
gh issue comment 123 --body "Related: #456"
gh issue comment 123 --body "Related: owner/repo#456"
gh issue edit 123 --body-file /tmp/issue-body.md
```

Use comments or body references to associate issues when the user wants discoverable linkage but there is no direct `gh issue` flag for the relation.

## GraphQL fallback

Use `gh api graphql` only for issue-native relations that are not exposed through `gh issue` subcommands and only after confirming the repo supports the target feature.
Keep the mutation minimal, then re-read the affected issue with `gh issue view --json ...` and report the resulting state instead of assuming success.
