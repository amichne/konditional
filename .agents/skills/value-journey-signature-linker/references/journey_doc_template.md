# Value journey doc template

Use this template for files in `docs/value-journeys/`.

This template is modular and portfolio-first. Build persuasive pages that can
span multiple docs, where each page has a clear audience and role.

## Required coverage across the document set

1. Clear value proposition and decision framing.
2. Comparison with baseline alternatives.
3. Citation-backed claims in narrative prose.
4. Adoption path and measurable success signals.

## Citation style

Embed compact claim citations in prose:

- `Konditional keeps rollout assignment deterministic per identity tuple [TH-003-C1].`
- `Boundary failures remain typed and diagnosable instead of exception-first [TH-001-C2].`

Citations must resolve to `docs/value-journeys/journey-claims.json`.

## Recommended page modules

- Value proposition and audience
- Why now
- Before/after journey
- Alternatives comparison
- Decision guidance
- Integration path
- Risks and tradeoffs
- Adoption signals
- Optional appendix with detailed evidence tables

```markdown
# <journey id>: <title>

## Value proposition

For <target user or team>, this journey delivers <outcome> by reducing
<pain/risk> and creating <gain>.

## Why now

Describe the urgency and why delay is expensive.

## Journey narrative

### Before

Describe friction and current risk profile.

### Turning point

Describe the decision or capability shift.

### After

Describe the new operating posture and measurable impact.

## Comparison with alternatives

| decision axis | baseline or traditional approach | konditional approach | practical value |
| --- | --- | --- | --- |
| <safety under change> | <best-effort visibility> | <coherent snapshot visibility> | <fewer rollback incidents> |
| <boundary behavior> | <generic failure behavior> | <typed boundary outcomes> | <faster triage> |

## Decision guidance

State when to adopt, when to defer, and what tradeoff this resolves.

## Citation-embedded proof points

- <Narrative sentence with citation token(s)> [<TH-002-C1>]
- <Narrative sentence with citation token(s)> [<TH-003-C1>]
- <Narrative sentence with citation token(s)> [<TH-004-C1>]

## Integration path

1. <First adoption step>.
2. <Second adoption step>.
3. <Verification step with citation> [<LRN-002-C2>].

## Adoption signals

- Primary KPI:
- Secondary KPI:
- Early warning metric:

## Risks and tradeoffs

- <What remains hard>
- <Boundary or migration caveat>

## Optional appendix: claim ledger and evidence index

| claim_id | claim statement | decision_type | status | code/test anchors |
| --- | --- | --- | --- | --- |
| <JV-001-C1> | <statement> | <adopt|migrate|operate> | <supported|at_risk|missing> | <links> |

### Optional detailed claim section

### <claim id>: <short claim title>

#### Code example

```kotlin
// Show only the smallest useful integration snippet.
```

#### Signature evidence

- kind: type
  signature: <fqcn>
- kind: method
  signature: <fqcn>#<method signature>

#### Test evidence

- <module>/src/test/.../<test file>::<test name>

#### Justification

Explain why this evidence is sufficient and what its limits are.
```

Keep citation tokens and claim IDs aligned with
`docs/value-journeys/journey-claims.json`.
Keep signature links aligned with
`docs/value-journeys/journey-signature-links.json`.
