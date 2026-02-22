#!/usr/bin/env python3
"""Validate claim/signature links and registry coverage with optional artifact refresh."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ALLOWED_LINK_KINDS = frozenset({"type", "method", "field"})
ALLOWED_TEST_KINDS = frozenset(
    {"unit", "integration", "property", "smoke", "compatibility", "e2e"}
)


@dataclass(frozen=True)
class ClaimRef:
    entity_kind: str
    entity_id: str
    title: str
    summary: str
    document_path: str
    claim_id: str
    statement: str


@dataclass(frozen=True)
class LinkRef:
    entity_kind: str
    entity_id: str
    title: str
    summary: str
    document_path: str
    claim_id: str
    claim_statement: str
    kind: str
    signature: str


@dataclass(frozen=True)
class TestRef:
    entity_id: str
    document_path: str
    claim_id: str
    module: str
    path: str
    symbol: str
    test_id: str
    kind: str


@dataclass(frozen=True)
class ValidationIssue:
    code: str
    message: str
    entity_id: str
    document_path: str
    claim_id: str


@dataclass(frozen=True)
class ParsedLinks:
    links: list[LinkRef]
    claims: list[ClaimRef]
    test_refs: list[TestRef]
    schema_modes: list[str]
    issues: list[ValidationIssue]


def as_repo_relative(repo_root: Path, path: Path | None) -> str:
    if path is None:
        return ""
    try:
        return path.resolve().relative_to(repo_root.resolve()).as_posix()
    except ValueError:
        return str(path)


def detect_signatures_dir(repo_root: Path, requested: str | None) -> Path | None:
    if requested:
        candidate = (repo_root / requested).resolve()
        if (candidate / "INDEX.sig").exists():
            return candidate
        return None

    for name in (".signatures", "signatures"):
        candidate = (repo_root / name).resolve()
        if (candidate / "INDEX.sig").exists():
            return candidate
    return None


def _pick_first_text(row: dict[str, Any], keys: tuple[str, ...]) -> str:
    for key in keys:
        value = row.get(key)
        if value not in (None, ""):
            return str(value)
    return ""


def infer_kind(signature: str) -> str:
    return "method" if "#" in signature else "type"


def _build_issue(
    code: str,
    message: str,
    entity_id: str,
    document_path: str,
    claim_id: str = "",
) -> ValidationIssue:
    return ValidationIssue(
        code=code,
        message=message,
        entity_id=entity_id,
        document_path=document_path,
        claim_id=claim_id,
    )


def parse_links(links_file: Path, repo_root: Path) -> ParsedLinks:
    raw: Any = json.loads(links_file.read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        return ParsedLinks([], [], [], ["unknown"], [])

    container_specs: tuple[tuple[str, str], ...] = (
        ("documents", "document"),
        ("records", "record"),
    )

    issues: list[ValidationIssue] = []
    claims: list[ClaimRef] = []
    links: list[LinkRef] = []
    test_refs: list[TestRef] = []
    seen_modes: list[str] = []
    claim_id_owners: dict[str, str] = {}

    for container_key, entity_kind in container_specs:
        rows = raw.get(container_key)
        if not isinstance(rows, list):
            continue

        seen_modes.append(container_key)

        for row in rows:
            if not isinstance(row, dict):
                continue

            entity_id = _pick_first_text(
                row,
                ("document_id", "record_id", "doc_id", "id"),
            ) or "<unknown>"
            summary = _pick_first_text(row, ("summary", "purpose"))
            title = _pick_first_text(row, ("title",))
            document_path = _pick_first_text(
                row,
                ("path", "doc_path", "document_path", "file"),
            )

            if not document_path:
                issues.append(
                    _build_issue(
                        code="missing-document-path",
                        message="document path is missing",
                        entity_id=entity_id,
                        document_path="",
                    )
                )
            else:
                candidate = (repo_root / document_path).resolve()
                if not candidate.exists() or not candidate.is_file():
                    issues.append(
                        _build_issue(
                            code="document-path-not-found",
                            message=f"document path not found: {document_path}",
                            entity_id=entity_id,
                            document_path=document_path,
                        )
                    )

            claim_rows = row.get("claims", [])
            if claim_rows is None:
                claim_rows = []
            if not isinstance(claim_rows, list):
                issues.append(
                    _build_issue(
                        code="invalid-claims-shape",
                        message="claims must be a list",
                        entity_id=entity_id,
                        document_path=document_path,
                    )
                )
                claim_rows = []

            for claim_row in claim_rows:
                if not isinstance(claim_row, dict):
                    issues.append(
                        _build_issue(
                            code="invalid-claim-record",
                            message="claim entry must be an object",
                            entity_id=entity_id,
                            document_path=document_path,
                        )
                    )
                    continue

                claim_id = str(claim_row.get("claim_id", "")).strip()
                statement = str(claim_row.get("statement", "")).strip()

                if not claim_id:
                    issues.append(
                        _build_issue(
                            code="missing-claim-id",
                            message="claim_id is required",
                            entity_id=entity_id,
                            document_path=document_path,
                        )
                    )
                    claim_id = "<missing-claim-id>"
                elif claim_id in claim_id_owners:
                    issues.append(
                        _build_issue(
                            code="duplicate-claim-id",
                            message=f"claim_id is reused: {claim_id}",
                            entity_id=entity_id,
                            document_path=document_path,
                            claim_id=claim_id,
                        )
                    )
                else:
                    claim_id_owners[claim_id] = entity_id

                claims.append(
                    ClaimRef(
                        entity_kind=entity_kind,
                        entity_id=entity_id,
                        title=title,
                        summary=summary,
                        document_path=document_path,
                        claim_id=claim_id,
                        statement=statement,
                    )
                )

                claim_links = claim_row.get("links", [])
                if not isinstance(claim_links, list):
                    issues.append(
                        _build_issue(
                            code="invalid-claim-links-shape",
                            message="claim links must be a list",
                            entity_id=entity_id,
                            document_path=document_path,
                            claim_id=claim_id,
                        )
                    )
                    claim_links = []

                for link in claim_links:
                    if not isinstance(link, dict):
                        issues.append(
                            _build_issue(
                                code="invalid-link-record",
                                message="link entry must be an object",
                                entity_id=entity_id,
                                document_path=document_path,
                                claim_id=claim_id,
                            )
                        )
                        continue

                    signature = str(link.get("signature", "")).strip()
                    if not signature:
                        issues.append(
                            _build_issue(
                                code="missing-signature",
                                message="link signature is required",
                                entity_id=entity_id,
                                document_path=document_path,
                                claim_id=claim_id,
                            )
                        )
                        continue

                    kind = str(link.get("kind", "")).strip().lower() or infer_kind(
                        signature
                    )
                    if kind not in ALLOWED_LINK_KINDS:
                        issues.append(
                            _build_issue(
                                code="invalid-link-kind",
                                message=f"unsupported link kind: {kind}",
                                entity_id=entity_id,
                                document_path=document_path,
                                claim_id=claim_id,
                            )
                        )

                    links.append(
                        LinkRef(
                            entity_kind=entity_kind,
                            entity_id=entity_id,
                            title=title,
                            summary=summary,
                            document_path=document_path,
                            claim_id=claim_id,
                            claim_statement=statement,
                            kind=kind,
                            signature=signature,
                        )
                    )

                test_links = claim_row.get("test_links", [])
                if test_links is None:
                    test_links = []
                if not isinstance(test_links, list):
                    issues.append(
                        _build_issue(
                            code="invalid-test-links-shape",
                            message="test_links must be a list",
                            entity_id=entity_id,
                            document_path=document_path,
                            claim_id=claim_id,
                        )
                    )
                    test_links = []

                for test_link in test_links:
                    if not isinstance(test_link, dict):
                        issues.append(
                            _build_issue(
                                code="invalid-test-link-record",
                                message="test_link entry must be an object",
                                entity_id=entity_id,
                                document_path=document_path,
                                claim_id=claim_id,
                            )
                        )
                        continue

                    module = str(test_link.get("module", "")).strip()
                    path = str(test_link.get("path", "")).strip()
                    symbol = str(test_link.get("symbol", "")).strip()
                    test_id = str(test_link.get("test_id", "")).strip()
                    kind = str(test_link.get("kind", "")).strip().lower()

                    if not module:
                        issues.append(
                            _build_issue(
                                code="missing-test-module",
                                message="test_links[].module is required",
                                entity_id=entity_id,
                                document_path=document_path,
                                claim_id=claim_id,
                            )
                        )
                    if not path:
                        issues.append(
                            _build_issue(
                                code="missing-test-path",
                                message="test_links[].path is required",
                                entity_id=entity_id,
                                document_path=document_path,
                                claim_id=claim_id,
                            )
                        )
                    if not (symbol or test_id):
                        issues.append(
                            _build_issue(
                                code="missing-test-symbol-or-id",
                                message="test_links[] requires symbol or test_id",
                                entity_id=entity_id,
                                document_path=document_path,
                                claim_id=claim_id,
                            )
                        )
                    if kind and kind not in ALLOWED_TEST_KINDS:
                        issues.append(
                            _build_issue(
                                code="invalid-test-kind",
                                message=f"unsupported test kind: {kind}",
                                entity_id=entity_id,
                                document_path=document_path,
                                claim_id=claim_id,
                            )
                        )

                    if path:
                        test_path = (repo_root / path).resolve()
                        if not test_path.exists() or not test_path.is_file():
                            issues.append(
                                _build_issue(
                                    code="test-path-not-found",
                                    message=f"test path not found: {path}",
                                    entity_id=entity_id,
                                    document_path=document_path,
                                    claim_id=claim_id,
                                )
                            )

                    test_refs.append(
                        TestRef(
                            entity_id=entity_id,
                            document_path=document_path,
                            claim_id=claim_id,
                            module=module,
                            path=path,
                            symbol=symbol,
                            test_id=test_id,
                            kind=kind,
                        )
                    )

    if not seen_modes:
        seen_modes.append("unknown")

    links_sorted = sorted(
        links,
        key=lambda item: (
            item.entity_id,
            item.claim_id,
            item.kind,
            item.signature,
        ),
    )
    claims_sorted = sorted(
        claims,
        key=lambda item: (item.entity_id, item.claim_id),
    )
    test_refs_sorted = sorted(
        test_refs,
        key=lambda item: (
            item.entity_id,
            item.claim_id,
            item.module,
            item.path,
            item.symbol,
            item.test_id,
        ),
    )
    issues_sorted = sorted(
        issues,
        key=lambda issue: (
            issue.code,
            issue.document_path,
            issue.claim_id,
            issue.entity_id,
            issue.message,
        ),
    )

    return ParsedLinks(
        links=links_sorted,
        claims=claims_sorted,
        test_refs=test_refs_sorted,
        schema_modes=seen_modes,
        issues=issues_sorted,
    )


def parse_registry(
    registry_file: Path,
    repo_root: Path,
) -> tuple[set[str], list[ValidationIssue]]:
    if not registry_file.exists():
        return set(), [
            _build_issue(
                code="registry-file-not-found",
                message=f"registry file not found: {as_repo_relative(repo_root, registry_file)}",
                entity_id="registry",
                document_path=as_repo_relative(repo_root, registry_file),
            )
        ]

    try:
        raw: Any = json.loads(registry_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return set(), [
            _build_issue(
                code="registry-json-invalid",
                message=f"registry json decode failed: {exc}",
                entity_id="registry",
                document_path=as_repo_relative(repo_root, registry_file),
            )
        ]

    if not isinstance(raw, dict) or not isinstance(raw.get("claims"), list):
        return set(), [
            _build_issue(
                code="registry-shape-invalid",
                message="registry must be an object with claims[]",
                entity_id="registry",
                document_path=as_repo_relative(repo_root, registry_file),
            )
        ]

    claim_ids: set[str] = set()
    issues: list[ValidationIssue] = []

    required_fields = (
        "claim_id",
        "claim_text",
        "topics",
        "source_pages",
        "signature_links",
        "test_links",
        "evidence_status",
        "risk_category",
    )

    for row in raw.get("claims", []):
        if not isinstance(row, dict):
            issues.append(
                _build_issue(
                    code="registry-claim-record-invalid",
                    message="registry claim entry must be an object",
                    entity_id="registry",
                    document_path=as_repo_relative(repo_root, registry_file),
                )
            )
            continue

        claim_id = str(row.get("claim_id", "")).strip()
        if not claim_id:
            issues.append(
                _build_issue(
                    code="registry-claim-id-missing",
                    message="registry claim_id is required",
                    entity_id="registry",
                    document_path=as_repo_relative(repo_root, registry_file),
                )
            )
            continue

        if claim_id in claim_ids:
            issues.append(
                _build_issue(
                    code="registry-claim-id-duplicate",
                    message=f"duplicate claim_id in registry: {claim_id}",
                    entity_id="registry",
                    document_path=as_repo_relative(repo_root, registry_file),
                    claim_id=claim_id,
                )
            )
        claim_ids.add(claim_id)

        for field_name in required_fields:
            if field_name not in row:
                issues.append(
                    _build_issue(
                        code="registry-required-field-missing",
                        message=f"missing required registry field: {field_name}",
                        entity_id="registry",
                        document_path=as_repo_relative(repo_root, registry_file),
                        claim_id=claim_id,
                    )
                )

    issues_sorted = sorted(
        issues,
        key=lambda issue: (
            issue.code,
            issue.claim_id,
            issue.message,
        ),
    )
    return claim_ids, issues_sorted


def parse_type_line(raw: str) -> str:
    for token in raw.split("|"):
        if token.startswith("type="):
            return token.split("=", 1)[1].strip()
    return "<unknown>"


def parse_index_entries(signatures_dir: Path) -> list[Path]:
    index_path = signatures_dir / "INDEX.sig"
    entries: list[Path] = []
    for raw_line in index_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line:
            continue
        rel = line.split("|", 1)[0].strip()
        if not rel:
            continue
        entries.append(signatures_dir / rel)
    return sorted(dict.fromkeys(entries))


def build_catalog(signatures_dir: Path) -> tuple[set[str], set[str], set[str]]:
    type_symbols: set[str] = set()
    method_symbols: set[str] = set()
    field_symbols: set[str] = set()

    for sig_path in parse_index_entries(signatures_dir):
        if not sig_path.exists():
            continue

        current_type: str | None = None
        section: str | None = None

        for raw_line in sig_path.read_text(
            encoding="utf-8",
            errors="ignore",
        ).splitlines():
            line = raw_line.strip()
            if not line:
                continue

            if line.startswith("type="):
                parsed_type = parse_type_line(line)
                if parsed_type and parsed_type != "<unknown>":
                    current_type = parsed_type
                    type_symbols.add(parsed_type)
                else:
                    current_type = None
                section = None
                continue

            if line == "methods:":
                section = "methods"
                continue

            if line == "fields:":
                section = "fields"
                continue

            if line.startswith("- ") and section == "methods" and current_type:
                method_sig = line[2:].strip()
                method_symbols.add(f"{current_type}#{method_sig}")
                continue

            if line.startswith("- ") and section == "fields" and current_type:
                field_sig = line[2:].strip()
                field_symbols.add(f"{current_type}#{field_sig}")
                continue

            section = None

    return type_symbols, method_symbols, field_symbols


def resolve_links(
    links: list[LinkRef],
    type_symbols: set[str],
    method_symbols: set[str],
    field_symbols: set[str],
) -> tuple[list[LinkRef], list[LinkRef]]:
    resolved: list[LinkRef] = []
    missing: list[LinkRef] = []

    for link in links:
        kind = link.kind.lower()
        if kind == "type":
            exists = link.signature in type_symbols
        elif kind == "method":
            exists = link.signature in method_symbols
        elif kind == "field":
            exists = link.signature in field_symbols
        else:
            exists = False

        if exists:
            resolved.append(link)
        else:
            missing.append(link)

    missing_sorted = sorted(
        missing,
        key=lambda item: (
            item.entity_id,
            item.claim_id,
            item.kind,
            item.signature,
        ),
    )
    resolved_sorted = sorted(
        resolved,
        key=lambda item: (
            item.entity_id,
            item.claim_id,
            item.kind,
            item.signature,
        ),
    )
    return resolved_sorted, missing_sorted


def run_command(cmd: list[str], cwd: Path) -> tuple[int, str, str]:
    try:
        completed = subprocess.run(
            cmd,
            cwd=cwd,
            text=True,
            capture_output=True,
            check=False,
        )
    except OSError as exc:
        return 127, "", str(exc)
    return completed.returncode, completed.stdout, completed.stderr


def refresh_artifacts(repo_root: Path, signatures_rel: str) -> tuple[bool, str]:
    signature_cmd = [
        ".agents/skills/llm-native-signature-spec/scripts/generate_signatures.sh",
        "--repo-root",
        ".",
        "--output-dir",
        signatures_rel,
    ]
    build_context_cmd = [
        "python3",
        ".agents/skills/public-surface-init-context/scripts/build_public_surface_context.py",
        "--repo-root",
        ".",
        "--signatures-dir",
        signatures_rel,
        "--output",
        f"{signatures_rel}/PUBLIC_SURFACE.ctx",
    ]

    rc1, out1, err1 = run_command(signature_cmd, cwd=repo_root)
    if rc1 != 0:
        details = f"signature refresh failed rc={rc1}\n{out1}\n{err1}".strip()
        return False, details

    rc2, out2, err2 = run_command(build_context_cmd, cwd=repo_root)
    if rc2 != 0:
        details = f"public context build failed rc={rc2}\n{out2}\n{err2}".strip()
        return False, details

    details = "\n".join(
        [
            f"refresh_ok signatures={signatures_rel}",
            out1.strip(),
            out2.strip(),
        ]
    ).strip()
    return True, details


def _claim_coverage(
    claims: list[ClaimRef],
    links: list[LinkRef],
    missing_links: list[LinkRef],
    registry_ids: set[str],
    issues: list[ValidationIssue],
) -> tuple[int, int, int, int]:
    claim_ids = sorted({claim.claim_id for claim in claims if claim.claim_id})
    pages_with_claims = len({claim.document_path for claim in claims if claim.document_path})
    claims_total = len(claim_ids)

    links_by_claim: dict[str, list[LinkRef]] = {}
    for link in links:
        links_by_claim.setdefault(link.claim_id, []).append(link)

    missing_by_claim = {link.claim_id for link in missing_links}
    issue_codes_by_claim: dict[str, set[str]] = {}
    for issue in issues:
        if issue.claim_id:
            issue_codes_by_claim.setdefault(issue.claim_id, set()).add(issue.code)

    claims_linked = 0
    for claim_id in claim_ids:
        claim_links = links_by_claim.get(claim_id, [])
        has_links = len(claim_links) > 0
        has_missing_link = claim_id in missing_by_claim
        has_issues = claim_id in issue_codes_by_claim
        in_registry = claim_id in registry_ids
        if has_links and not has_missing_link and not has_issues and in_registry:
            claims_linked += 1

    claims_missing = claims_total - claims_linked
    return pages_with_claims, claims_total, claims_linked, claims_missing


def report_dict(
    repo_root: Path,
    links_file: Path,
    registry_file: Path,
    signatures_dir: Path | None,
    schema_modes: list[str],
    links: list[LinkRef],
    claims: list[ClaimRef],
    resolved: list[LinkRef],
    missing: list[LinkRef],
    issues: list[ValidationIssue],
    refresh_attempted: bool,
    refresh_succeeded: bool,
    refresh_details: str,
    pages_with_claims: int,
    claims_total: int,
    claims_linked: int,
    claims_missing: int,
) -> dict[str, Any]:
    status = "ok"
    if missing or issues or claims_missing > 0:
        status = "issues-detected"

    return {
        "status": status,
        "repo_root": ".",
        "links_file": as_repo_relative(repo_root, links_file),
        "registry_file": as_repo_relative(repo_root, registry_file),
        "signatures_dir": as_repo_relative(repo_root, signatures_dir),
        "schema_modes": schema_modes,
        "total_links": len(links),
        "resolved_links": len(resolved),
        "missing_links": len(missing),
        "pages_with_claims": pages_with_claims,
        "claims_total": claims_total,
        "claims_linked": claims_linked,
        "claims_missing": claims_missing,
        "issue_count": len(issues),
        "refresh_attempted": refresh_attempted,
        "refresh_succeeded": refresh_succeeded,
        "refresh_details": refresh_details,
        "missing": [
            {
                "entity_kind": item.entity_kind,
                "entity_id": item.entity_id,
                "document_path": item.document_path,
                "claim_id": item.claim_id,
                "kind": item.kind,
                "signature": item.signature,
            }
            for item in missing
        ],
        "issues": [
            {
                "code": issue.code,
                "message": issue.message,
                "entity_id": issue.entity_id,
                "document_path": issue.document_path,
                "claim_id": issue.claim_id,
            }
            for issue in issues
        ],
    }


def main(default_links_file: str = "docs/claim-trace/claim-signature-links.json") -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", default=".", help="Repository root path.")
    parser.add_argument(
        "--links-file",
        default=default_links_file,
        help="Claim/signature links JSON file.",
    )
    parser.add_argument(
        "--registry-file",
        default="docs/claim-trace/claims-registry.json",
        help="Claims registry JSON file.",
    )
    parser.add_argument(
        "--signatures-dir",
        default=None,
        help="Signatures directory relative to repo root. Auto-detect if omitted.",
    )
    parser.add_argument(
        "--report-out",
        default=None,
        help="Optional output path for machine-readable report JSON.",
    )
    parser.add_argument(
        "--auto-refresh",
        action="store_true",
        help="Regenerate signatures and public-surface context if links are missing.",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Return non-zero when unresolved issues remain.",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    links_file = (repo_root / args.links_file).resolve()
    registry_file = (repo_root / args.registry_file).resolve()

    if not links_file.exists():
        print(f"links file not found: {links_file}", file=sys.stderr)
        return 1

    parsed = parse_links(links_file, repo_root)
    signatures_dir = detect_signatures_dir(repo_root, args.signatures_dir)

    refresh_attempted = False
    refresh_succeeded = False
    refresh_details = ""

    if signatures_dir is None and args.auto_refresh:
        refresh_attempted = True
        requested_rel = args.signatures_dir or ".signatures"
        refresh_succeeded, refresh_details = refresh_artifacts(
            repo_root,
            requested_rel,
        )
        if refresh_succeeded:
            signatures_dir = detect_signatures_dir(repo_root, requested_rel)
    elif signatures_dir is None:
        refresh_details = "signatures directory missing; run with --auto-refresh"

    if signatures_dir is not None:
        type_symbols, method_symbols, field_symbols = build_catalog(signatures_dir)
    else:
        type_symbols, method_symbols, field_symbols = set(), set(), set()

    resolved, missing = resolve_links(
        parsed.links,
        type_symbols,
        method_symbols,
        field_symbols,
    )

    if missing and args.auto_refresh and not refresh_attempted:
        refresh_attempted = True
        signatures_rel = as_repo_relative(repo_root, signatures_dir) if signatures_dir else ".signatures"
        refresh_succeeded, refresh_details = refresh_artifacts(
            repo_root,
            signatures_rel,
        )
        if refresh_succeeded:
            signatures_dir = detect_signatures_dir(repo_root, signatures_rel)
            if signatures_dir is not None:
                type_symbols, method_symbols, field_symbols = build_catalog(
                    signatures_dir
                )
                resolved, missing = resolve_links(
                    parsed.links,
                    type_symbols,
                    method_symbols,
                    field_symbols,
                )

    registry_ids, registry_issues = parse_registry(registry_file, repo_root)

    issue_list: list[ValidationIssue] = []
    issue_list.extend(parsed.issues)
    issue_list.extend(registry_issues)

    for claim in parsed.claims:
        if claim.claim_id and claim.claim_id not in registry_ids:
            issue_list.append(
                _build_issue(
                    code="missing-registry-claim",
                    message=f"claim id missing from registry: {claim.claim_id}",
                    entity_id=claim.entity_id,
                    document_path=claim.document_path,
                    claim_id=claim.claim_id,
                )
            )

    issues_sorted = sorted(
        issue_list,
        key=lambda issue: (
            issue.code,
            issue.document_path,
            issue.claim_id,
            issue.entity_id,
            issue.message,
        ),
    )

    pages_with_claims, claims_total, claims_linked, claims_missing = _claim_coverage(
        claims=parsed.claims,
        links=parsed.links,
        missing_links=missing,
        registry_ids=registry_ids,
        issues=issues_sorted,
    )

    report = report_dict(
        repo_root=repo_root,
        links_file=links_file,
        registry_file=registry_file,
        signatures_dir=signatures_dir,
        schema_modes=parsed.schema_modes,
        links=parsed.links,
        claims=parsed.claims,
        resolved=resolved,
        missing=missing,
        issues=issues_sorted,
        refresh_attempted=refresh_attempted,
        refresh_succeeded=refresh_succeeded,
        refresh_details=refresh_details,
        pages_with_claims=pages_with_claims,
        claims_total=claims_total,
        claims_linked=claims_linked,
        claims_missing=claims_missing,
    )

    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.report_out:
        report_out = (repo_root / args.report_out).resolve()
        report_out.parent.mkdir(parents=True, exist_ok=True)
        report_out.write_text(rendered, encoding="utf-8")

    print(rendered, end="")

    has_failures = bool(missing or issues_sorted or claims_missing > 0)
    if args.strict and has_failures:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
