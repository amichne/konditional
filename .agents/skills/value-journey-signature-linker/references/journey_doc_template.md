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

## Journey stages

1. <stage name>: <goal and user intent>
2. <stage name>: <goal and user intent>
3. <stage name>: <goal and user intent>

## Technical evidence (signature links)

- kind: type
  signature: <fqcn>
  claim_supported: <which narrative claim this proves>
  status: linked
- kind: method
  signature: <fqcn>#<method signature>
  claim_supported: <which narrative claim this proves>
  status: linked

## Adoption signals

- Primary KPI:
- Secondary KPI:
- Early warning metric:

## Migration and shadowing impact

- Baseline behavior:
- Candidate behavior:
- Mismatch expectations:

## Open questions

- <question or unresolved signature gap>
```

Keep "Technical evidence (signature links)" synchronized with
`docs/value-journeys/journey-signature-links.json`.
