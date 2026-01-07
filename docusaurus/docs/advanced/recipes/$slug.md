# $title

Attach logging and metrics without depending on a specific vendor SDK.

{{recipe-9-observability}}

- **Guarantee**: Hooks receive evaluation and lifecycle signals with consistent payloads.
- **Mechanism**: `RegistryHooks` are invoked inside the runtime's evaluation and load paths.
- **Boundary**: Hooks run on the hot path; keep them non-blocking.

---
