# Documentation update template (claim-citation mode)

Use this template for markdown pages that require deterministic claim citations.

```markdown 
<details>

<summary>Claims</summary> 

| Claim ID | Explicit Claim | Local Evidence Linkage | Registry Link |
|---|---|---|---|
| CLM-TH-003 | <explicit statement> | `#section-anchor` | `/reference/claims-registry#clm-th-003` |

</details>


```

Keep claim citations synchronized with:

- `docs/claim-trace/claim-signature-links.json`
- `docs/claim-trace/claims-registry.json`
