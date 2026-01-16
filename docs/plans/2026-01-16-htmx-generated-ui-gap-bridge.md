# HTMX ↔ Generated UI Gap Bridge Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Bring the HTMX feature-flag editor to parity with the generated UI’s core workflows (flag list, full editor, JSON/preview, targeting) and make the generated UI runnable against the same snapshot API.

**Architecture:** Fix HTMX shell/asset loading first, then add missing mutation + JSON routes, then expand editor parity (value types + targeting). In parallel, expose a JSON snapshot API and host the generated UI assets under `/ui` so React can run against the same state.

**Tech Stack:** Kotlin/Ktor, kotlinx.html, Moshi, Gradle, Tailwind CSS (ui-ktor), React/Vite (konditional-generated-ui)

---

### Task 1: HTMX shell + indicator fixes

**Files:**
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/FlagListRenderer.kt`
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/UiRouting.kt`
- Create: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/HtmlLayout.kt`
- Test: `ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiRoutingFlagListTest.kt`
- Test: `ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiRoutingFlagEditorTest.kt`

**Step 1: Write the failing tests**

```kotlin
// UiRoutingFlagListTest.kt
assertTrue(response.bodyAsText().contains("id=\"loading-feature--ui--dark_mode\""))

// UiRoutingFlagEditorTest.kt
assertTrue(response.bodyAsText().contains("htmx.org@1.9.12"))
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiRoutingFlagListTest'`
Expected: FAIL with missing sanitized id

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiRoutingFlagEditorTest'`
Expected: FAIL with missing htmx script

**Step 3: Write minimal implementation**

```kotlin
// HtmlLayout.kt
internal fun sanitizeDomId(value: String): String =
    value.replace("::", "--").replace(":", "-")

internal fun HTML.renderFlagShell(titleText: String, bodyBlock: BODY.() -> Unit) {
    head {
        meta(charset = "utf-8")
        title(titleText)
        link(rel = "stylesheet", href = "/static/styles.css")
        script { defer = true; src = "https://unpkg.com/htmx.org@1.9.12" }
    }
    body {
        id = "main-content"
        bodyBlock()
    }
}
```

```kotlin
// FlagListRenderer.kt
val indicatorId = "loading-${sanitizeDomId(flag.key.toString())}"
attributes["hx-indicator"] = "#${indicatorId}"
...
id = indicatorId
```

```kotlin
// UiRouting.kt (flag editor route)
call.respondHtml {
    renderFlagShell("Feature Flags") {
        renderFlagEditor(flag, paths.page)
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiRoutingFlagListTest'`
Expected: PASS

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiRoutingFlagEditorTest'`
Expected: PASS

**Step 5: Commit**

```bash
git add ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/HtmlLayout.kt \
        ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/FlagListRenderer.kt \
        ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/UiRouting.kt \
        ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiRoutingFlagListTest.kt \
        ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiRoutingFlagEditorTest.kt
git commit -m "fix(ui-ktor): load htmx shell + sanitize indicator ids"
```

---

### Task 2: Default + rule value updates, JSON tab

**Files:**
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/UiRouting.kt`
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/state/FlagStateService.kt`
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/FlagEditorRenderer.kt`
- Create: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/FlagJsonRenderer.kt`
- Create: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/FlagValueParsers.kt`
- Test: `ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiRoutingFlagMutationTest.kt`

**Step 1: Write the failing tests**

```kotlin
// UiRoutingFlagMutationTest.kt
val defaultResponse = client.post("/config/flag/${snapshot.flags.first().key}/default") {
    contentType(ContentType.Application.FormUrlEncoded)
    setBody("value=true")
}
assertEquals(HttpStatusCode.OK, defaultResponse.status)
assertEquals(true, service.getSnapshot().flags.first().defaultValue.value)

val ruleValueResponse = client.post("/config/flag/${snapshot.flags.first().key}/rule/0/value") {
    contentType(ContentType.Application.FormUrlEncoded)
    setBody("value=false")
}
assertEquals(HttpStatusCode.OK, ruleValueResponse.status)
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiRoutingFlagMutationTest'`
Expected: FAIL with 404 for new routes

**Step 3: Write minimal implementation**

```kotlin
// FlagEditorRenderer.kt (ensure name="value" so HTMX sends payload)
input(type = InputType.checkBox) { name = "value"; ... }
input(type = InputType.text) { name = "value"; ... }
input(type = InputType.number) { name = "value"; ... }
```

```kotlin
// FlagValueParsers.kt
internal fun parseValue(raw: String?, current: FlagValue<*>): FlagValue<*> =
    when (current) {
        is FlagValue.BooleanValue -> FlagValue.BooleanValue(raw == "true" || raw == "on")
        is FlagValue.StringValue -> FlagValue.StringValue(raw.orEmpty())
        is FlagValue.IntValue -> FlagValue.IntValue(raw?.toIntOrNull() ?: current.value)
        is FlagValue.DoubleValue -> FlagValue.DoubleValue(raw?.toDoubleOrNull() ?: current.value)
        else -> current
    }
```

```kotlin
// UiRouting.kt (new routes)
get("${paths.page}/flag/{key}/json") { /* renderFlagJson(flag) */ }
post("${paths.page}/flag/{key}/default") { /* update defaultValue */ }
post("${paths.page}/flag/{key}/rule/{index}/value") { /* update rule.value */ }
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiRoutingFlagMutationTest'`
Expected: PASS

**Step 5: Commit**

```bash
git add ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/UiRouting.kt \
        ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/state/FlagStateService.kt \
        ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/FlagEditorRenderer.kt \
        ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/FlagJsonRenderer.kt \
        ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/FlagValueParsers.kt \
        ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiRoutingFlagMutationTest.kt
git commit -m "feat(ui-ktor): default/rule value updates + json tab"
```

---

### Task 3: Targeting editor parity (platforms/locales/axes/version/ramp allowlist)

**Files:**
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/RuleEditorRenderer.kt`
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/UiRouting.kt`
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/state/FlagStateService.kt`
- Test: `ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiRoutingFlagMutationTest.kt`

**Step 1: Write the failing tests**

```kotlin
val platformsResponse = client.post("/config/flag/${snapshot.flags.first().key}/rule/0/platforms") {
    contentType(ContentType.Application.FormUrlEncoded)
    setBody("platforms=IOS,ANDROID")
}
assertEquals(HttpStatusCode.OK, platformsResponse.status)
assertEquals(setOf("IOS", "ANDROID"), service.getSnapshot().flags.first().rules.first().platforms)
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiRoutingFlagMutationTest'`
Expected: FAIL with 404

**Step 3: Write minimal implementation**

```kotlin
// RuleEditorRenderer.kt (simple comma-separated editors)
input(type = InputType.text) {
    name = "platforms"
    value = rule.platforms.joinToString(",")
    attributes["hx-post"] = "$basePath/flag/$flagKey/rule/$ruleIndex/platforms"
    attributes["hx-trigger"] = "change"
    attributes["hx-target"] = "closest details"
}
```

```kotlin
// UiRouting.kt
post("${paths.page}/flag/{key}/rule/{index}/platforms") { /* split + update */ }
post("${paths.page}/flag/{key}/rule/{index}/locales") { /* split + update */ }
post("${paths.page}/flag/{key}/rule/{index}/axes") { /* parse key=value1|value2 */ }
post("${paths.page}/flag/{key}/rule/{index}/allowlist") { /* split + update */ }
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiRoutingFlagMutationTest'`
Expected: PASS

**Step 5: Commit**

```bash
git add ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/RuleEditorRenderer.kt \
        ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/UiRouting.kt \
        ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/state/FlagStateService.kt \
        ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiRoutingFlagMutationTest.kt
git commit -m "feat(ui-ktor): editable rule targeting fields"
```

---

### Task 4: Enum + data class editors

**Files:**
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/FlagEditorRenderer.kt`
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/FlagValueParsers.kt`
- Test: `ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiRoutingFlagMutationTest.kt`

**Step 1: Write the failing tests**

```kotlin
val enumSnapshot = snapshotWithEnumFlag()
val enumService = InMemoryFlagStateService(enumSnapshot)
...
val response = client.post("/config/flag/${enumSnapshot.flags.first().key}/default") {
    contentType(ContentType.Application.FormUrlEncoded)
    setBody("value=ADYEN")
}
assertEquals("ADYEN", enumService.getSnapshot().flags.first().defaultValue.value)
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiRoutingFlagMutationTest'`
Expected: FAIL with enum unsupported

**Step 3: Write minimal implementation**

```kotlin
// FlagEditorRenderer.kt
is FlagValue.EnumValue -> select { name = "value"; ... }
is FlagValue.DataClassValue -> textArea { name = "value"; ... }
```

```kotlin
// FlagValueParsers.kt
is FlagValue.EnumValue -> FlagValue.EnumValue(raw.orEmpty(), current.enumClassName)
is FlagValue.DataClassValue -> parseJsonToMap(raw) ?: current
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiRoutingFlagMutationTest'`
Expected: PASS

**Step 5: Commit**

```bash
git add ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/html/FlagEditorRenderer.kt \
        ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/FlagValueParsers.kt \
        ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiRoutingFlagMutationTest.kt
git commit -m "feat(ui-ktor): enum + data class editors"
```

---

### Task 5: Generated UI hosting + snapshot API

**Files:**
- Modify: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/demo/DemoKonditionalUi.kt`
- Create: `ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/UiApiRouting.kt`
- Modify: `konditional-generated-ui/src/components/command/CommandPalette.tsx`
- Create: `scripts/build-generated-ui.sh`
- Test: `ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiApiRoutingTest.kt`

**Step 1: Write the failing tests**

```kotlin
// UiApiRoutingTest.kt
val response = client.get("/ui/api/snapshot")
assertEquals(HttpStatusCode.OK, response.status)
assertTrue(response.bodyAsText().contains("\"flags\""))
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiApiRoutingTest'`
Expected: FAIL with 404

**Step 3: Write minimal implementation**

```kotlin
// UiApiRouting.kt
fun Route.installUiApiRoutes(stateService: FlagStateService) {
    get("/ui/api/snapshot") { call.respond(stateService.getSnapshot()) }
    patch("/ui/api/patch") { /* receive SerializablePatch, merge into snapshot */ }
}
```

```bash
# scripts/build-generated-ui.sh
cd konditional-generated-ui
npm run build
rm -rf ../ui-ktor/src/main/resources/ui
cp -R dist ../ui-ktor/src/main/resources/ui
```

```kotlin
// DemoKonditionalUi.kt
staticResources("/ui", "ui")
installUiApiRoutes(stateService)
```

```tsx
// CommandPalette.tsx
import { fetchSnapshot } from '@/lib/konditionalApi';
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :ui-ktor:test --tests 'io.amichne.konditional.uiktor.UiApiRoutingTest'`
Expected: PASS

**Step 5: Commit**

```bash
git add ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/UiApiRouting.kt \
        ui-ktor/src/main/kotlin/io/amichne/konditional/uiktor/demo/DemoKonditionalUi.kt \
        konditional-generated-ui/src/components/command/CommandPalette.tsx \
        scripts/build-generated-ui.sh \
        ui-ktor/src/test/kotlin/io/amichne/konditional/uiktor/UiApiRoutingTest.kt
git commit -m "feat(ui): serve generated UI + snapshot api"
```
