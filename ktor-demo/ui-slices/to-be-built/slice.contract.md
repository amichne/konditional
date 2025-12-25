# To-Be-Built UI Slice (React → Kotlin/JS + HTMX)

## Overview
- Source entrypoint: `from/src/App.tsx`
- Purpose: Replace the React Router “component library” shell with a Kotlin-rendered catalog page that preserves *functional* behaviors while reducing client JS surface area via HTMX fragment swaps.

## Fragment Roots (stable ids)
- `#tbbRoot`: page shell (server-rendered)
- `#tbbContent`: the main content fragment swapped for navigation
- `#tbbToastHost`: toast host area (event-driven via `HX-Trigger`)

## Route / Screen Map (from `from/src/App.tsx`)
Navigation is modeled as “sections” (server fragments) rather than a SPA router:
- `/to-be-built` → `Index`
- `/to-be-built/primitives/colors` → “Colors”
- `/to-be-built/primitives/typography` → “Typography”
- `/to-be-built/components/inputs` → “Inputs & Forms”
- `/to-be-built/patterns/schema-forms` → “Schema Forms” (flag browser + JSON preview; editing can be incremental)
- `/to-be-built/*` → Placeholder / NotFound (mirrors the TS route set)

## Observable States
### Global
- Navigation swaps only `#tbbContent` (`hx-swap="outerHTML"`).
- Non-HTMX fallback: regular links may load full pages.

### Index
- Normal: hero + navigation cards + highlights + recent applications.

### Colors / Typography
- Normal: static token tables / examples.

### Inputs & Forms
- Normal: showcases buttons/inputs/selects/toggles/slider.
- Interactive: slider value updates (must update the displayed “API Timeout” label).

### Schema Forms
- Normal: list flags grouped by namespace.
- Selected: shows a flag detail panel (JSON view is sufficient for parity).

## Events → HTMX Mapping (recommended)
0) **Navigation click**
   - Trigger: sidebar links
   - Option: 0 (HTMX)
   - Endpoint: `GET /to-be-built/...`
   - Swap: `#tbbContent outerHTML`

1) **Inputs slider change**
   - Trigger: `input[type=range]#tbbRange`
   - Option: 0 (HTMX)
   - Endpoint: `POST /to-be-built/components/inputs/slider`
   - Swap: `#tbbInputsSliderCard outerHTML`
   - Success: returns updated fragment with computed label value
   - Error: 422 returns same fragment with inline error state

2) **Toast**
   - Trigger: server-side actions (e.g., slider update)
   - Option: 2 (Hybrid)
   - Transport: `HX-Trigger` header, consumed by minimal Kotlin/JS listener

## Dependencies
- Uses: `kotlinx-html` (server fragments), `kotlinx-html-js` (client-rendered shell), HTMX runtime (`htmx.min.js` served statically).
- Avoids: React, React Router, React Query.

## Acceptance Checks
- Sidebar navigation swaps `#tbbContent` without a full reload (HTMX).
- Inputs slider updates the visible value label via a fragment swap (HTMX).
- Schema Forms section can select a flag and render details (at least JSON + metadata).
