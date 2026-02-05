---
title: Next Steps / Deep Dives
---

# Next Steps / Deep Dives

This documentation set focuses on `:konditional-core`: the DSL + evaluation engine.

When you’re ready to operate Konditional in a real system, the next modules to understand are:

- `:konditional-runtime` — configuration lifecycle (loading, rollback, and operational surfaces)
- `:konditional-serialization` — boundary codecs (snapshots/patches) and their typed failure models
- `:konditional-observability` — higher‑level observability patterns (including shadow evaluation)
- `:opentelemetry` — vendor integration (standardized telemetry pipelines)

| Module                         | When to use it                                   |
|-------------------------------|--------------------------------------------------|
| `konditional-runtime`         | You load/refresh configuration in production     |
| `konditional-serialization`   | You store/transport configs as JSON snapshots    |
| `konditional-observability`   | You need shadow evaluation and mismatch tracing  |
| `opentelemetry`               | You want vendor‑neutral telemetry export         |

If you want copy/paste patterns before deep dives, start with [Recipes](/recipes).
