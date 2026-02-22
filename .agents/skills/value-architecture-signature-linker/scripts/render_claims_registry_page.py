#!/usr/bin/env python3
"""Render a human-readable claims registry page from claims-registry.json."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


def _slug(text: str) -> str:
    result = "".join(ch.lower() if ch.isalnum() else "-" for ch in text)
    while "--" in result:
        result = result.replace("--", "-")
    return result.strip("-")


def _as_links(values: list[str]) -> str:
    if not values:
        return "-"
    return ", ".join(f"`{value}`" for value in values)


def _as_signature_links(rows: list[dict[str, Any]]) -> str:
    if not rows:
        return "-"
    normalized = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        kind = str(row.get("kind", "")).strip()
        signature = str(row.get("signature", "")).strip()
        if signature:
            normalized.append(f"{kind}:{signature}" if kind else signature)
    if not normalized:
        return "-"
    return ", ".join(f"`{item}`" for item in sorted(normalized))


def _as_test_links(rows: list[dict[str, Any]]) -> str:
    if not rows:
        return "-"
    normalized = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        module = str(row.get("module", "")).strip()
        path = str(row.get("path", "")).strip()
        symbol = str(row.get("symbol", "")).strip()
        test_id = str(row.get("test_id", "")).strip()
        kind = str(row.get("kind", "")).strip()
        identifier = symbol or test_id
        if module or path or identifier:
            normalized.append(f"{kind}:{module}:{path}:{identifier}".strip(":"))
    if not normalized:
        return "-"
    return ", ".join(f"`{item}`" for item in sorted(normalized))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", default=".", help="Repository root path")
    parser.add_argument(
        "--registry-file",
        default="docs/claim-trace/claims-registry.json",
        help="Registry JSON input path relative to repo root",
    )
    parser.add_argument(
        "--output",
        default="docusaurus/docs/reference/claims-registry.md",
        help="Output markdown path relative to repo root",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    registry_file = (repo_root / args.registry_file).resolve()
    output = (repo_root / args.output).resolve()

    raw: Any = json.loads(registry_file.read_text(encoding="utf-8"))
    claims_raw = raw.get("claims", []) if isinstance(raw, dict) else []

    claims: list[dict[str, Any]] = [
        row for row in claims_raw if isinstance(row, dict) and row.get("claim_id")
    ]
    claims.sort(key=lambda row: str(row.get("claim_id", "")))

    lines: list[str] = []
    lines.append("# Claims registry")
    lines.append("")
    lines.append(
        "This page is generated from `docs/claim-trace/claims-registry.json` and "
        "is the human-readable index for claim IDs used across documentation."
    )
    lines.append("")
    lines.append(
        "| Claim ID | Claim text | Evidence status | Risk category | Source pages |"
    )
    lines.append("|---|---|---|---|---|")

    for claim in claims:
        claim_id = str(claim.get("claim_id", "")).strip()
        claim_text = str(claim.get("claim_text", "")).replace("|", "\\|")
        evidence_status = str(claim.get("evidence_status", "")).strip() or "-"
        risk_category = str(claim.get("risk_category", "")).strip() or "-"
        source_pages = claim.get("source_pages", [])
        page_links: list[str] = []
        if isinstance(source_pages, list):
            for source in source_pages:
                if not isinstance(source, dict):
                    continue
                path = str(source.get("path", "")).strip()
                anchor = str(source.get("anchor", "")).strip()
                label = path + (f"#{anchor}" if anchor else "")
                if label:
                    page_links.append(label)

        page_links_text = ", ".join(f"`{p}`" for p in sorted(page_links)) or "-"
        lines.append(
            f"| [{claim_id}](#{_slug(claim_id)}) | {claim_text} | {evidence_status} | {risk_category} | {page_links_text} |"
        )

    for claim in claims:
        claim_id = str(claim.get("claim_id", "")).strip()
        claim_text = str(claim.get("claim_text", "")).strip()
        topics = claim.get("topics", [])
        evidence_status = str(claim.get("evidence_status", "")).strip() or "-"
        risk_category = str(claim.get("risk_category", "")).strip() or "-"
        signature_links = claim.get("signature_links", [])
        test_links = claim.get("test_links", [])
        source_pages = claim.get("source_pages", [])
        related_claims = claim.get("related_claims", [])

        source_page_text: list[str] = []
        if isinstance(source_pages, list):
            for source in source_pages:
                if not isinstance(source, dict):
                    continue
                path = str(source.get("path", "")).strip()
                anchor = str(source.get("anchor", "")).strip()
                label = path + (f"#{anchor}" if anchor else "")
                if label:
                    source_page_text.append(label)

        lines.append("")
        lines.append(f"## {claim_id}")
        lines.append("")
        lines.append(claim_text)
        lines.append("")
        lines.append(f"- **Evidence status:** `{evidence_status}`")
        lines.append(f"- **Risk category:** `{risk_category}`")
        lines.append(f"- **Topics:** {_as_links(sorted(topics) if isinstance(topics, list) else [])}")
        lines.append(f"- **Source pages:** {_as_links(sorted(source_page_text))}")
        lines.append(f"- **Signature links:** {_as_signature_links(signature_links if isinstance(signature_links, list) else [])}")
        lines.append(f"- **Test links:** {_as_test_links(test_links if isinstance(test_links, list) else [])}")
        lines.append(
            f"- **Related claims:** {_as_links(sorted(related_claims) if isinstance(related_claims, list) else [])}"
        )

    rendered = "\n".join(lines).rstrip() + "\n"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(rendered, encoding="utf-8")
    print(f"rendered claims registry page: {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
