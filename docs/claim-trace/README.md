# Claim trace artifacts

This folder stores deterministic evidence artifacts that link documentation
claims to JVM signature evidence.

## Files

- `claim-signature-links.json`: claim-to-signature link map.
- `claims-report.json`: claim-link validation report.
- `claim-evidence-query.json`: deterministic pseudo-RAG retrieval report.

## Regenerate

```bash
python3 .agents/skills/value-architecture-signature-linker/scripts/validate_claim_signature_links.py \
  --repo-root . \
  --links-file docs/claim-trace/claim-signature-links.json \
  --signatures-dir docs/claim-trace/signatures \
  --report-out docs/claim-trace/claims-report.json \
  --auto-refresh \
  --strict

python3 .agents/skills/value-architecture-signature-linker/scripts/query_claim_evidence.py \
  --repo-root . \
  --links-file docs/claim-trace/claim-signature-links.json \
  --signatures-dir docs/claim-trace/signatures \
  --docs-glob 'docusaurus/docs/overview/**/*.md' \
  --docs-glob 'docusaurus/docs/quickstart/**/*.md' \
  --query 'typed feature namespace deterministic bucketing stableId snapshot loader parse error boundary rollback evidence' \
  --top-k 20 \
  --report-out docs/claim-trace/claim-evidence-query.json \
  --auto-refresh \
  --strict
```

## Notes

- `docs/claim-trace/signatures/` is generated material and is git-ignored.
- Link records are deterministic and sorted by stable IDs.
