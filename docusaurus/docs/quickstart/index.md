# Quickstart

Use this path to ship one typed feature with deterministic behavior and a safe
runtime boundary.

## What you will achieve

By the end of this quickstart, you will:

- define and evaluate a typed feature;
- add deterministic rollout behavior;
- load a remote snapshot with typed parse failure handling;
- verify end-to-end behavior.

## Prerequisites

You need:

- Kotlin and Gradle basics;
- a local project where you can run one compile-and-test cycle.

## Main content

Complete these pages in order:

1. [Install](/quickstart/install)
2. [Define first flag](/quickstart/define-first-flag)
3. [Evaluate in app code](/quickstart/evaluate-in-app-code)
4. [Add deterministic ramp-up](/quickstart/add-deterministic-ramp-up)
5. [Load first snapshot safely](/quickstart/load-first-snapshot-safely)
6. [Verify end-to-end](/quickstart/verify-end-to-end)

This sequence gives you one complete vertical slice from declaration to runtime
operations.

## Expected output

You should finish with:

- one `Namespace` and at least one typed feature;
- one context evaluation path in application code;
- one valid and one invalid remote snapshot test case;
- one short verification checklist committed with your code.

## Next steps

- [How-to guides](/how-to-guides/rolling-out-gradually)
- [Runtime operations](/runtime/operations)
- [Troubleshooting](/troubleshooting/)
