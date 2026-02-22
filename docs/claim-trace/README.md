# Claim trace artifacts

This folder stores deterministic evidence artifacts that link documentation
claims to JVM signature evidence.

## Files

- `claim-signature-links.json`: claim-to-signature link map.
- `claims-registry.json`: canonical claim registry with signature and test links.
- `claims-registry.schema.json`: schema contract for claim registry validation.
- `claims-report.json`: claim-link validation report.
- `claim-evidence-query.json`: deterministic pseudo-RAG retrieval report.

## Regenerate

```bash
python3 .agents/skills/value-architecture-signature-linker/scripts/validate_claim_signature_links.py \
  --repo-root . \
  --links-file docs/claim-trace/claim-signature-links.json \
  --registry-file docs/claim-trace/claims-registry.json \
  --report-out docs/claim-trace/claims-report.json \
  --auto-refresh \
  --strict

python3 .agents/skills/value-architecture-signature-linker/scripts/query_claim_evidence.py \
  --repo-root . \
  --links-file docs/claim-trace/claim-signature-links.json \
  --docs-glob 'docusaurus/docs/**/*.md' \
  --query 'deterministic evaluation claim linkage' \
  --top-k 20 \
  --report-out docs/claim-trace/claim-evidence-query.json \
  --strict

python3 .agents/skills/value-architecture-signature-linker/scripts/render_claims_registry_page.py \
  --repo-root . \
  --registry-file docs/claim-trace/claims-registry.json \
  --output docusaurus/docs/reference/claims-registry.md
```

## Notes

- `.signatures/` is generated material and is git-ignored.
- Link records are deterministic and sorted by stable IDs.
