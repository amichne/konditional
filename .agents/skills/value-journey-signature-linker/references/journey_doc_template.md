# Value journey doc template

Use this template for files in `docs/value-journeys/`.

```markdown
# <journey id>: <title>

## Value proposition

For <target user or team>, this journey delivers <outcome> by reducing
<pain/risk> and creating <gain>.

## Journey narrative

### Before

Describe the current friction, uncertainty, or operational cost.

### Turning point

Describe the key decision, capability, or process shift that changes behavior.

### After

Describe the new steady state and the measurable impact.

## Decision guidance

State when teams should adopt this journey and which tradeoff it resolves.

## Claim table

| claim_id | claim_statement | decision_type | status |
| --- | --- | --- | --- |
| <JV-001-C1> | <statement> | <adopt\|migrate\|operate> | <supported\|at_risk\|missing> |

## Technical evidence (signature links)

- kind: type
  signature: <fqcn>
  claim_supported: <claim_id>
  status: linked
- kind: method
  signature: <fqcn>#<method signature>
  claim_supported: <claim_id>
  status: linked

## Evidence status summary

- supported:
- at_risk:
- missing:

## Adoption signals

- Primary KPI:
- Secondary KPI:
- Early warning metric:

## Migration and shadowing impact

- Baseline behavior:
- Candidate behavior:
- Mismatch expectations:

## Open questions

- <question or unresolved claim evidence gap>
```

Keep claim IDs aligned with `docs/value-journeys/journey-claims.json` and keep
signature links aligned with `docs/value-journeys/journey-signature-links.json`.
