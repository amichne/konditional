---
title: First Success Map
sidebar_position: 4
---

# First Success Map

Pick the shortest path to your first measurable outcome.

<span id="claim-clm-pr01-04a"></span>
The first-success routes map to concrete runtime APIs for deterministic bucketing and namespace snapshot loading.

```mermaid
flowchart TD
  A[What do you need first?] --> B[I want to try it now]
  A --> C[I want to understand the model]
  A --> D[I need to convince my team]
  A --> E[I need formal guarantees]

  B --> B1[/quickstart/]
  C --> C1[/concepts/]
  D --> D1[/overview/why-typed-flags/]
  D --> D2[/overview/competitive-positioning/]
  E --> E1[/theory/]

  B1 --> X[RampUpBucketing + NamespaceSnapshotLoader]
  C1 --> X
```

## Route Guide

1. Try it quickly: [Quickstart](/quickstart/).
2. Build shared mental model: [Concepts](/concepts/namespaces).
3. Build adoption case: [Why Typed Flags](/overview/why-typed-flags) and [Competitive Positioning](/overview/competitive-positioning).
4. Validate guarantees: [Theory](/theory/type-safety-boundaries).

## Claim Coverage

| Claim ID | Statement |
| --- | --- |
| CLM-PR01-04A | The first-success routes correspond to concrete runtime APIs for ramp-up and snapshot loading. |
