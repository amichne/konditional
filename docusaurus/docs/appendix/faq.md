---
title: FAQ
sidebar_position: 2
---

# FAQ

## Does Konditional work with Java?

Konditional is Kotlin-first. Java interoperability is possible for core usage, but the ergonomics are optimized for Kotlin declarations and DSL patterns. If your org is Java-heavy, validate integration early and consider wrapper APIs for team ergonomics.

## Is there a built-in UI?

No. Konditional is a library/runtime model, not a hosted control plane. Teams typically pair it with internal config delivery tooling.

## How does Konditional compare to LaunchDarkly?

LaunchDarkly provides a managed UI and hosted platform. Konditional prioritizes in-code typed declarations, deterministic evaluation semantics, and explicit parse boundaries. The trade-off is more operational ownership in your platform team.

## How does Konditional compare to OpenFeature?

OpenFeature is a vendor-neutral API/spec. Konditional can complement OpenFeature as an implementation strategy where Kotlin typing and deterministic semantics are required.

## Can I migrate incrementally?

Yes. Use shadow evaluation and mismatch reporting to compare baseline and candidate behavior before cutover.

## Where should I start?

Start with [Quickstart](/quickstart/), then choose [Guides](/guides/enterprise-adoption) or [Theory](/theory/type-safety-boundaries) based on your adoption stage.
