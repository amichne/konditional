---
name: kotlin-jvm-lsp-gradle-debug
description: Standardize editor-agnostic Kotlin/JVM development workflows using Language Server Protocol, Gradle wrapper commands, and JVM debugging. Use when setting up or troubleshooting Kotlin tooling in LSP-capable editors, validating Gradle build/test/run loops, or attaching debuggers to JVM app and test processes.
---

# Kotlin JVM LSP Gradle Debug

## Goal

Standardize Kotlin/JVM workflows across editors by using one repeatable loop for tooling checks, Gradle execution, and debug attach.
Prefer open, editor-agnostic integrations and avoid IDE lock-in unless the user explicitly requests it.

## Workflow

1. Detect project boundaries and toolchain.
- Confirm project root and prefer `./gradlew` over system Gradle.
- Verify Java and Gradle: `java -version` and `./gradlew -v`.
- Confirm module layout and runnable tasks: `./gradlew tasks --all`.

2. Stabilize the Gradle baseline before editor integration.
- Run a clean verification loop: `./gradlew clean test`.
- If the project has an app entrypoint, run it with `./gradlew run` (or project-specific task).
- Fix build errors before enabling LSP/debug workflows.

3. Select an LSP backend explicitly.
- Load [tooling-matrix.md](references/tooling-matrix.md) before choosing a backend.
- Prefer `Kotlin/kotlin-lsp` for current official direction.
- Use `fwcd/kotlin-language-server` when an open-source server is required and tradeoffs are acceptable.
- Record the selected backend and any known limitations in the final output.

4. Run debug workflows with deterministic attach points.
- Start JVM app debugging with `./gradlew run --debug-jvm`.
- Start JVM test debugging with `./gradlew test --debug-jvm`.
- Attach a DAP/JDWP client to `localhost:5005` unless the build defines a different port.
- For editor-agnostic debug support, use `fwcd/kotlin-debug-adapter` when available.

5. Triage failures with a fixed recovery sequence.
- Stop stale daemons: `./gradlew --stop`.
- Retry with dependency refresh: `./gradlew clean build --refresh-dependencies`.
- Re-check tool versions and classpath assumptions.
- If issues persist, capture exact failing command, stack trace, and minimal repro module.

## Output Contract

Always provide:
- Detected environment summary (JDK, Gradle wrapper, project type).
- Exact commands executed in order.
- LSP backend and debug adapter selection with rationale.
- Current status: working, partially working, or blocked.
- Next concrete command to run.

## References

- Use [tooling-matrix.md](references/tooling-matrix.md) when selecting LSP and debug tooling.
- Use [command-playbook.md](references/command-playbook.md) for reusable command snippets and attach templates.

## Constraints

- Do not use global Gradle or global Kotlin executables when `./gradlew` is present.
- Do not mark setup complete unless at least one build/test command and one debug attach path have been validated.
- Do not hide experimental or deprecated tooling status; state it explicitly.
