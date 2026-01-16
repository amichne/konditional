# HTML/HTMX UI React Parity Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Migrate the HTML/HTMX UI from basic forms to match the React implementation's design quality and UX patterns

**Architecture:** Server-side rendered HTML with Tailwind CSS, HTMX for progressive disclosure (flag list → editor drill-down), native `<details>` for collapsible sections, CSS-driven tabs. Zero client state - server is source of truth.

**Tech Stack:** Kotlin, Ktor, kotlinx.html, HTMX 1.9.12, Tailwind CSS 3.4, Node.js (for Tailwind build)

---

## Task 8: Final Integration & Testing

**Files:**
- Modify: `demo/src/main/kotlin/demo/DemoServer.kt`

**Step 1: Switch demo to use HTML UI**

Modify `demo/src/main/kotlin/demo/DemoServer.kt`:

```kotlin
fun Application.demoModule() {
    routing {
        installDemoKonditionalUi() // Use HTML version
//        installDemoKonditionalReactUi() // Comment out React version
    }
}
```

**Step 2: Full end-to-end test**

```bash
./gradlew demo:run
```

Test checklist:
1. Visit http://localhost:8080/config → See flag list
2. Flags grouped by namespace ✓
3. Click flag → Navigate to editor ✓
4. Back button → Return to list ✓
5. Toggle active switch → State updates ✓
6. Edit default value → State updates ✓
7. Add rule → Rule appears ✓
8. Expand rule → See editor ✓
9. Adjust ramp slider → Badge updates ✓
10. Delete rule → Rule removed ✓

Expected: All interactions work smoothly with HTMX

**Step 3: Run `make check`**

```bash
make check
```

Expected: All checks pass

**Step 4: Final commit**

```bash
git add demo/src/main/kotlin/demo/DemoServer.kt
git commit -m "feat(ui): switch demo to HTML/HTMX UI

- Enable HTML UI by default in demo
- Comment out React UI
- Full feature parity achieved:
  - Namespace-grouped flag list
  - Progressive disclosure navigation
  - Collapsible rule editors
  - In-memory state persistence
  - Loading animations
  - Tailwind-styled components matching React design"
```

---

## Summary

**Completed:**
- ✅ Tailwind CSS build pipeline with design tokens
- ✅ Type-safe Kotlin DSL for component classes
- ✅ Flag list with namespace grouping
- ✅ Flag editor with progressive disclosure
- ✅ Collapsible rule editors
- ✅ In-memory state service
- ✅ HTMX-powered navigation & mutations
- ✅ Loading states & animations

**Architecture highlights:**
- Zero client JavaScript (besides HTMX)
- Server-side rendering with kotlinx.html
- Atomic state updates with `AtomicReference`
- Progressive enhancement (works without JS)
- 1:1 UX parity with React implementation

**Next steps (optional enhancements):**
- Persist to file/database instead of in-memory
- Add JSON tab content
- Implement platform/locale multi-select
- Add search/filter to flag list
- Dark mode toggle
