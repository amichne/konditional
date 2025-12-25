# To-Be-Built UI Shell (React → Kotlin/JS + HTMX) — slice.contract.md

## Overview
- Source entrypoint: `from/src/App.tsx`
- Purpose: replace the React Router + provider wrapper with a Kotlin-first “to be built” UI shell, using HTMX for navigation/content swaps to minimize client JS.
- Kotlin target: `ktor-demo` (Ktor partial endpoints) + `ktor-demo:demo-client` (Kotlin/JS progressive enhancement for prefs).

## Fragment Roots (stable swap boundaries)
0) `main#tbbContent`: page content fragment swapped by navigation and internal links (`hx-target="#tbbContent"`, `hx-swap="outerHTML"`).
1) `div#tbbToastArea` (optional): notification area; present but currently unused (React `Toaster`/`Sonner` providers exist but pages don’t call `toast()`).

## Routes (functional parity with `from/src/App.tsx`)
All routes are namespaced under `/to-be-built` to avoid clashing with existing demo routes.

- `/to-be-built/` → Index (dashboard/landing)
- `/to-be-built/primitives` → Colors (alias)
- `/to-be-built/primitives/colors` → Colors
- `/to-be-built/primitives/typography` → Typography
- `/to-be-built/primitives/spacing` → Placeholder
- `/to-be-built/primitives/motion` → Placeholder
- `/to-be-built/components` → Inputs (alias)
- `/to-be-built/components/layout` → Inputs (alias)
- `/to-be-built/components/navigation` → Inputs (alias)
- `/to-be-built/components/overlays` → Inputs (alias)
- `/to-be-built/components/inputs` → Inputs
- `/to-be-built/components/data-display` → Inputs (alias)
- `/to-be-built/patterns` → Schema Forms (alias)
- `/to-be-built/patterns/schema-forms` → Schema Forms (links to existing `/config-state`)
- `/to-be-built/patterns/safe-publishing` → Placeholder
- `/to-be-built/patterns/large-datasets` → Placeholder
- `/to-be-built/playground` → Placeholder
- `/to-be-built/demo` → Links to existing demo pages (`/`, `/config-state`)
- `*` → Not Found

## States
- Normal: shell + rendered content for the active route.
- Not Found: `main#tbbContent` renders a 404 panel.

## Events (user → effect mapping) + HTMX strategy

### Navigation links (SideNav / internal links)
- Trigger: click `<a data-nav href="/to-be-built/...">`
- Option: 0 (HTMX roundtrip)
- Endpoint: `GET /to-be-built/...`
- Swap: `#tbbContent outerHTML`
- Success: 200 with updated `main#tbbContent`
- Not found: 404 with updated `main#tbbContent` in error state

### Theme / density / reduced motion preferences (TopBar)
- Trigger: change `select#tbbTheme`, `select#tbbDensity`, `input#tbbReducedMotion`
- Option: 1 (Kotlin/JS-only local update)
- Behavior: update `document.documentElement` classes and persist to `localStorage` (no server roundtrip).

### Sidebar collapsed preference
- Trigger: click `button#tbbSidebarToggle`
- Option: 1 (Kotlin/JS-only local update)
- Behavior: toggle `data-sidebar="collapsed|expanded"` on `body` and persist to `localStorage`.

## Dependencies
- HTMX runtime: `static/vendor/htmx.min.js` (served by `ktor-demo`).
- Kotlin/JS bundle: `demo-client.js` (already served from `/static` by the existing `ktor-demo` build).
- No React Query equivalent: server-render + HTMX replaces `QueryClientProvider`.
- Icons: omitted/simplified (no `lucide-react`).

## Acceptance Checks
- Route coverage matches `from/src/App.tsx` (aliases included).
- Clicking a nav link updates only `main#tbbContent` via HTMX swap and pushes URL.
- Deep-linking a route (full page load) renders the full shell + correct content.
- Theme/density toggles work without server roundtrips and persist across refresh.

