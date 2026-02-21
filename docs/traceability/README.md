# Documentation traceability artifacts

This folder stores deterministic evidence artifacts that link documentation
claims to JVM signature evidence.

## Files

- `doc-signature-links.json`: claim-to-signature link map.
- `doc-signature-report.json`: link validation report.
- `doc-evidence-query.json`: deterministic pseudo-RAG retrieval report.

## Regenerate

```bash
python3 .agents/skills/value-journey-signature-linker/scripts/validate_doc_signature_links.py \
  --repo-root . \
  --links-file docs/traceability/doc-signature-links.json \
  --signatures-dir docs/traceability/signatures \
  --report-out docs/traceability/doc-signature-report.json \
  --auto-refresh \
  --strict

python3 .agents/skills/value-journey-signature-linker/scripts/query_doc_evidence.py \
  --repo-root . \
  --links-file docs/traceability/doc-signature-links.json \
  --signatures-dir docs/traceability/signatures \
  --docs-glob 'docusaurus/docs/overview/**/*.md' \
  --docs-glob 'docusaurus/docs/quickstart/**/*.md' \
  --query 'typed feature namespace deterministic bucketing stableId snapshot loader parse error boundary rollback evidence' \
  --top-k 20 \
  --report-out docs/traceability/doc-evidence-query.json \
  --auto-refresh \
  --strict
```

## Notes

- `docs/traceability/signatures/` is generated material and is git-ignored.
- Link records are intentionally deterministic and sorted by stable IDs.
