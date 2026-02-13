# Novice Value-First Docs IA Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make a Kotlin backend novice see Konditional's value in under 10 minutes by restructuring docs around a value-first journey.

**Architecture:** Reorganize Docusaurus navigation from module-first to task-first, then rewrite entry and onboarding pages to emphasize safe feature shipping. Keep deep theory and module references intact but move them behind successful first-use flows. Add explicit page patterns (overview, task-oriented, conceptual), cross-links, and verification sections.

**Tech Stack:** Docusaurus docs markdown (`.md`), Docusaurus sidebar config (`sidebars.ts`), Node 20, `npm run build`.

### Task 1: Restructure global navigation for novice journey

**Files:**
- Modify: `docusaurus/sidebars.ts`

**Step 1: Write the failing structural check**

Document target order to enforce:
- Welcome
- Getting Started (10-minute path)
- Guides
- Operate in Production
- API Reference
- Theory and Guarantees
- Updates / migration / troubleshooting

**Step 2: Run current docs build to capture baseline**

Run: `npm run build`
Expected: PASS (baseline; no broken links)

**Step 3: Implement sidebar reorganization**

Update `docusaurus/sidebars.ts` with novice-first categories and move module/deep theory categories below onboarding and task guides.

**Step 4: Build docs to validate navigation changes**

Run: `npm run build`
Expected: PASS with no broken links.

### Task 2: Rewrite welcome page around first value moment

**Files:**
- Modify: `docusaurus/docs/index.md`

**Step 1: Define acceptance criteria**

Welcome page must include:
- 1-sentence value proposition for Kotlin backend engineers
- "In this section you will find" links (overview pattern)
- 10-minute outcome path and explicit safety guarantees
- Clear next actions: install -> first feature -> rollout

**Step 2: Implement concise value-first content**

Rewrite intro and section ordering to lead with "ship a safe toggle quickly" before deep theory.

**Step 3: Add section dividers and scannable structure**

Use `---` between major topic shifts and keep sections to short scan blocks.

**Step 4: Validate links locally**

Run: `npm run build`
Expected: PASS with no broken links from home page.

### Task 3: Create a 10-minute quickstart overview page

**Files:**
- Create: `docusaurus/docs/getting-started/index.md`
- Modify: `docusaurus/docs/getting-started/installation.md`
- Modify: `docusaurus/docs/getting-started/your-first-flag.md`

**Step 1: Write page skeleton using overview pattern**

Add overview page with:
- brief context
- "In this section you will find"
- linked sequence of child pages

**Step 2: Tighten installation page for quick win**

Keep dependency instructions, add a short "Verification" section and link to first feature.

**Step 3: Refactor first feature page to strict task-oriented pattern**

Order sections as:
- Brief context
- Prerequisites
- Numbered steps
- Verification
- Next steps

**Step 4: Build docs and confirm sequential flow**

Run: `npm run build`
Expected: PASS; pages render in intended order and links resolve.

### Task 4: Improve task-guide discoverability and progressive disclosure

**Files:**
- Modify: `docusaurus/docs/how-to-guides/rolling-out-gradually.md`
- Modify: `docusaurus/docs/how-to-guides/safe-remote-config.md`
- Modify: `docusaurus/docs/how-to-guides/testing-features.md`

**Step 1: Add lead outcomes at top of each guide**

Each guide starts with one key outcome sentence and short prerequisites.

**Step 2: Ensure section divider placement**

Add `---` before prerequisites and before "Next steps" / related links where missing.

**Step 3: Add first-mention cross-links**

Link first mention of core concepts to learn/theory/reference pages, once per section max.

**Step 4: Build docs and verify no link regressions**

Run: `npm run build`
Expected: PASS.

### Task 5: Add update-oriented entry points and migration path visibility

**Files:**
- Modify: `docusaurus/sidebars.ts`
- Modify: `docusaurus/docs/reference/migration-guide.md`
- Modify: `docusaurus/docs/troubleshooting/index.md`

**Step 1: Surface updates/migration in navigation**

Ensure updates/migration resources are discoverable from top-level navigation areas.

**Step 2: Add explicit "coming from string-keyed SDK" path**

Strengthen migration entry bullets for novice Kotlin teams transitioning from existing SDKs.

**Step 3: Improve troubleshooting triage path**

Ensure troubleshooting index quickly routes to parsing, integration, and bucketing issues.

**Step 4: Final verification build**

Run: `npm run build`
Expected: PASS with stable navigation and links.

### Task 6: Final quality pass and handoff notes

**Files:**
- Modify: `docs/plans/2026-02-13-novice-value-docs-ia.md` (append outcomes)

**Step 1: Run final verification command**

Run: `npm run build`
Expected: PASS.

**Step 2: Capture before/after IA summary**

Add concise bullets documenting the old vs new user journey and first value moment.

**Step 3: Commit implementation increment (if requested)**

Run:
```bash
git add docusaurus/sidebars.ts docusaurus/docs/index.md docusaurus/docs/getting-started/index.md docusaurus/docs/getting-started/installation.md docusaurus/docs/getting-started/your-first-flag.md docusaurus/docs/how-to-guides/rolling-out-gradually.md docusaurus/docs/how-to-guides/safe-remote-config.md docusaurus/docs/how-to-guides/testing-features.md docusaurus/docs/reference/migration-guide.md docusaurus/docs/troubleshooting/index.md docs/plans/2026-02-13-novice-value-docs-ia.md
git commit -m "docs: restructure docs IA around novice value-first journey"
```
