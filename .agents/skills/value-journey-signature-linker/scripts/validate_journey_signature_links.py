#!/usr/bin/env python3
"""Validate journey links and claims evidence with optional artifact refresh."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

CLAIM_ID_PATTERN = re.compile(r"\b[A-Z]{2,}-[0-9]{3}-C[0-9]+\b")
JOURNEY_ID_PATTERN = re.compile(r"^[A-Z]{2,}-[0-9]{3}$")
DECISION_TYPES = {"adopt", "migrate", "operate"}
CLAIM_STATUSES = {"supported", "at_risk", "missing"}
REF_KINDS = {"type", "method", "field"}
DOC_SCOPES = {"value_journey", "theory", "learn"}
CLAIM_KINDS = {"guarantee", "mechanism", "boundary", "failure_mode", "performance"}


@dataclass(frozen=True)
class LinkRef:
    journey_id: str
    title: str
    value_proposition: str
    kind: str
    signature: str


@dataclass(frozen=True)
class SignatureRef:
    kind: str
    signature: str


@dataclass(frozen=True)
class ClaimRef:
    journey_id: str
    claim_id: str
    claim_statement: str
    decision_type: str
    doc_scope: str
    doc_page: str
    claim_kind: str
    non_trivial: bool
    status: str
    signatures: tuple[SignatureRef, ...]
    tests: tuple[str, ...]
    owner_modules: tuple[str, ...]


@dataclass(frozen=True)
class ClaimEvaluation:
    journey_id: str
    claim_id: str
    doc_scope: str
    doc_page: str
    claim_kind: str
    non_trivial: bool
    declared_status: str
    computed_status: str
    missing_signatures: tuple[str, ...]
    missing_tests: tuple[str, ...]
    owner_modules: tuple[str, ...]
    owner_paths: tuple[str, ...]


@dataclass(frozen=True)
class SignatureCatalog:
    type_symbols: frozenset[str]
    method_symbols: frozenset[str]
    field_symbols: frozenset[str]
    methods_by_type: dict[str, frozenset[str]]
    method_names_by_type: dict[str, frozenset[str]]


def detect_signatures_dir(repo_root: Path, requested: str | None) -> Path | None:
    if requested:
        candidate = (repo_root / requested).resolve()
        if (candidate / "INDEX.sig").exists():
            return candidate
        return None

    for name in ("signatures", ".signatures"):
        candidate = (repo_root / name).resolve()
        if (candidate / "INDEX.sig").exists():
            return candidate
    return None


def parse_links(links_file: Path) -> tuple[list[LinkRef], str]:
    raw: Any = json.loads(links_file.read_text(encoding="utf-8"))
    if not isinstance(raw, dict):
        return [], "unknown"

    if isinstance(raw.get("journeys"), list):
        rows = raw["journeys"]
        schema_mode = "journeys"
    elif isinstance(raw.get("stories"), list):
        rows = raw["stories"]
        schema_mode = "stories-legacy"
    else:
        rows = []
        schema_mode = "unknown"

    result: list[LinkRef] = []
    for row in rows:
        if not isinstance(row, dict):
            continue

        journey_id = str(row.get("journey_id") or row.get("story_id") or "<unknown>")
        title = str(row.get("title", ""))
        value_proposition = str(row.get("value_proposition") or row.get("outcome") or "")

        links = row.get("links", [])
        if not isinstance(links, list):
            continue

        for link in links:
            if not isinstance(link, dict):
                continue
            signature = str(link.get("signature", "")).strip()
            if not signature:
                continue
            kind = str(link.get("kind", "")).strip() or infer_kind(signature)

            result.append(
                LinkRef(
                    journey_id=journey_id,
                    title=title,
                    value_proposition=value_proposition,
                    kind=kind,
                    signature=signature,
                )
            )

    return result, schema_mode


def parse_claims(
    claims_file: Path,
    require_tests: bool,
) -> tuple[list[ClaimRef], dict[str, str], list[str]]:
    raw: Any = json.loads(claims_file.read_text(encoding="utf-8"))
    errors: list[str] = []

    if not isinstance(raw, dict):
        return [], {}, ["claims file root must be a JSON object"]

    owner_routing_raw = raw.get("owner_routing", {})
    owner_routing: dict[str, str] = {}
    if isinstance(owner_routing_raw, dict):
        for key, value in owner_routing_raw.items():
            if isinstance(key, str) and isinstance(value, str) and key.strip() and value.strip():
                owner_routing[key.strip()] = value.strip()

    claims_raw = raw.get("claims")
    if not isinstance(claims_raw, list):
        return [], owner_routing, ["claims file must contain an array at key 'claims'"]

    claims: list[ClaimRef] = []
    claim_ids_seen: set[str] = set()

    for idx, claim in enumerate(claims_raw):
        base = f"claims[{idx}]"
        if not isinstance(claim, dict):
            errors.append(f"{base}: claim must be an object")
            continue

        journey_id = str(claim.get("journey_id", "")).strip()
        claim_id = str(claim.get("claim_id", "")).strip()
        claim_statement = str(claim.get("claim_statement", "")).strip()
        decision_type = str(claim.get("decision_type", "")).strip()
        doc_scope = str(claim.get("doc_scope", "value_journey")).strip()
        doc_page = str(claim.get("doc_page", "")).strip()
        claim_kind = str(claim.get("claim_kind", "guarantee")).strip()
        non_trivial_raw = claim.get("non_trivial", True)
        non_trivial = bool(non_trivial_raw)
        status = str(claim.get("status", "")).strip()

        if not JOURNEY_ID_PATTERN.fullmatch(journey_id):
            errors.append(f"{base}: invalid journey_id '{journey_id}'")
        if not CLAIM_ID_PATTERN.fullmatch(claim_id):
            errors.append(f"{base}: invalid claim_id '{claim_id}'")
        if len(claim_statement) < 10:
            errors.append(f"{base}: claim_statement must be at least 10 chars")
        if decision_type not in DECISION_TYPES:
            errors.append(f"{base}: decision_type must be one of {sorted(DECISION_TYPES)}")
        if doc_scope not in DOC_SCOPES:
            errors.append(f"{base}: doc_scope must be one of {sorted(DOC_SCOPES)}")
        if not doc_page:
            errors.append(f"{base}: doc_page must be non-empty")
        if claim_kind not in CLAIM_KINDS:
            errors.append(f"{base}: claim_kind must be one of {sorted(CLAIM_KINDS)}")
        if not isinstance(non_trivial_raw, bool):
            errors.append(f"{base}: non_trivial must be a boolean")
        if status not in CLAIM_STATUSES:
            errors.append(f"{base}: status must be one of {sorted(CLAIM_STATUSES)}")
        if claim_id in claim_ids_seen:
            errors.append(f"{base}: duplicate claim_id '{claim_id}'")
        if claim_id:
            claim_ids_seen.add(claim_id)

        signatures_raw = claim.get("signatures")
        signatures: list[SignatureRef] = []
        if not isinstance(signatures_raw, list) or not signatures_raw:
            errors.append(f"{base}: signatures must be a non-empty array")
        else:
            for sig_idx, entry in enumerate(signatures_raw):
                sig_base = f"{base}.signatures[{sig_idx}]"
                if not isinstance(entry, dict):
                    errors.append(f"{sig_base}: signature entry must be an object")
                    continue
                kind = str(entry.get("kind", "")).strip().lower()
                signature = str(entry.get("signature", "")).strip()
                if kind not in REF_KINDS:
                    errors.append(f"{sig_base}: kind must be one of {sorted(REF_KINDS)}")
                if not signature:
                    errors.append(f"{sig_base}: signature must be non-empty")
                if kind in REF_KINDS and signature:
                    signatures.append(SignatureRef(kind=kind, signature=signature))

        tests_raw = claim.get("tests")
        tests: list[str] = []
        if require_tests:
            if not isinstance(tests_raw, list) or not tests_raw:
                errors.append(f"{base}: tests must be a non-empty array when --require-tests")
            else:
                for test_idx, entry in enumerate(tests_raw):
                    test_ref = str(entry).strip()
                    if not test_ref:
                        errors.append(f"{base}.tests[{test_idx}]: test reference must be non-empty")
                    else:
                        tests.append(test_ref)
        else:
            if isinstance(tests_raw, list):
                tests.extend(str(entry).strip() for entry in tests_raw if str(entry).strip())

        owner_modules_raw = claim.get("owner_modules")
        owner_modules: list[str] = []
        if not isinstance(owner_modules_raw, list) or not owner_modules_raw:
            errors.append(f"{base}: owner_modules must be a non-empty array")
        else:
            for owner_idx, entry in enumerate(owner_modules_raw):
                module = str(entry).strip()
                if not module:
                    errors.append(f"{base}.owner_modules[{owner_idx}]: module must be non-empty")
                else:
                    owner_modules.append(module)

        claims.append(
            ClaimRef(
                journey_id=journey_id,
                claim_id=claim_id,
                claim_statement=claim_statement,
                decision_type=decision_type,
                doc_scope=doc_scope,
                doc_page=doc_page,
                claim_kind=claim_kind,
                non_trivial=non_trivial,
                status=status,
                signatures=tuple(signatures),
                tests=tuple(tests),
                owner_modules=tuple(owner_modules),
            )
        )

    return claims, owner_routing, sorted(dict.fromkeys(errors))


def infer_kind(signature: str) -> str:
    return "method" if "#" in signature else "type"


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
        if rel:
            entries.append(signatures_dir / rel)
    return sorted(dict.fromkeys(entries))


def extract_method_name(signature: str) -> str | None:
    match = re.search(r"fun\s+`([^`]+)`", signature)
    if match:
        return match.group(1)
    match = re.search(r"fun\s+([A-Za-z_][A-Za-z0-9_]*)", signature)
    if match:
        return match.group(1)
    match = re.search(r"([A-Za-z_][A-Za-z0-9_]*)\s*\(", signature)
    if match:
        return match.group(1)
    return None


def build_catalog(signatures_dir: Path) -> SignatureCatalog:
    type_symbols: set[str] = set()
    method_symbols: set[str] = set()
    field_symbols: set[str] = set()
    methods_by_type: dict[str, set[str]] = {}
    method_names_by_type: dict[str, set[str]] = {}

    for sig_path in parse_index_entries(signatures_dir):
        if not sig_path.exists():
            continue

        current_types: list[str] = []
        section: str | None = None

        for raw_line in sig_path.read_text(encoding="utf-8", errors="ignore").splitlines():
            line = raw_line.strip()
            if not line:
                continue

            if line.startswith("type="):
                current_type = parse_type_line(line)
                if current_type and current_type != "<unknown>":
                    type_symbols.add(current_type)
                    current_types.append(current_type)
                    methods_by_type.setdefault(current_type, set())
                    method_names_by_type.setdefault(current_type, set())
                section = None
                continue

            if line == "methods:":
                section = "methods"
                continue
            if line == "fields:":
                section = "fields"
                continue

            if line.startswith("- ") and section == "methods":
                method_sig = line[2:].strip()
                name = extract_method_name(method_sig)
                for current_type in current_types:
                    method_symbols.add(f"{current_type}#{method_sig}")
                    methods_by_type.setdefault(current_type, set()).add(method_sig)
                    if name:
                        method_names_by_type.setdefault(current_type, set()).add(name)
                continue

            if line.startswith("- ") and section == "fields":
                field_sig = line[2:].strip()
                for current_type in current_types:
                    field_symbols.add(f"{current_type}#{field_sig}")
                continue

            section = None

    return SignatureCatalog(
        type_symbols=frozenset(type_symbols),
        method_symbols=frozenset(method_symbols),
        field_symbols=frozenset(field_symbols),
        methods_by_type={
            key: frozenset(values)
            for key, values in methods_by_type.items()
        },
        method_names_by_type={
            key: frozenset(values)
            for key, values in method_names_by_type.items()
        },
    )


def resolve_links(
    links: list[LinkRef],
    catalog: SignatureCatalog,
) -> tuple[list[LinkRef], list[LinkRef]]:
    resolved: list[LinkRef] = []
    missing: list[LinkRef] = []

    for link in links:
        kind = link.kind.lower()
        if kind == "type":
            exists = link.signature in catalog.type_symbols
        elif kind == "method":
            exists = link.signature in catalog.method_symbols
        elif kind == "field":
            exists = link.signature in catalog.field_symbols
        else:
            exists = False

        if exists:
            resolved.append(link)
        else:
            missing.append(link)

    missing_sorted = sorted(
        missing,
        key=lambda item: (item.journey_id, item.kind, item.signature),
    )
    return resolved, missing_sorted


def resolve_signature_ref(ref: SignatureRef, catalog: SignatureCatalog) -> bool:
    if ref.kind == "type":
        return ref.signature in catalog.type_symbols
    if ref.kind == "method":
        return ref.signature in catalog.method_symbols
    if ref.kind == "field":
        return ref.signature in catalog.field_symbols
    return False


def resolve_test_ref(test_ref: str, repo_root: Path, catalog: SignatureCatalog) -> bool:
    if "/" in test_ref:
        path_part, _, selector = test_ref.partition("::")
        test_file = (repo_root / path_part).resolve()
        if not test_file.exists() or not test_file.is_file():
            return False
        if not selector:
            return True
        content = test_file.read_text(encoding="utf-8", errors="ignore")
        return selector in content

    if "#" in test_ref:
        fqcn, _, selector = test_ref.partition("#")
        fqcn = fqcn.strip()
        selector = selector.strip()
        if fqcn not in catalog.type_symbols:
            return False
        if not selector:
            return True

        method_signatures = catalog.methods_by_type.get(fqcn, frozenset())
        method_names = catalog.method_names_by_type.get(fqcn, frozenset())
        if selector in method_signatures:
            return True
        if selector in method_names:
            return True
        return any(selector in method_signature for method_signature in method_signatures)

    return test_ref in catalog.type_symbols


def canonical_doc_page(doc_page: str) -> str:
    normalized = doc_page.strip()
    normalized = normalized.removeprefix("/")
    if normalized.endswith(".md"):
        normalized = normalized[:-3]
    return normalized


def doc_page_key(doc_scope: str, doc_page: str) -> str:
    return f"{doc_scope}:{canonical_doc_page(doc_page)}"


def resolve_doc_path(repo_root: Path, doc_page: str) -> Path:
    normalized = canonical_doc_page(doc_page)
    return (repo_root / "docusaurus" / "docs" / f"{normalized}.md").resolve()


def extract_claim_ids_from_claim_ledger(
    *,
    content: str,
    require_claim_ledger: bool,
    file_label: str,
) -> tuple[set[str], list[str]]:
    errors: list[str] = []
    section = content

    ledger_heading = re.search(r"^##\s+Claim ledger\s*$", content, re.IGNORECASE | re.MULTILINE)
    if ledger_heading:
        start = ledger_heading.end()
        next_heading = re.search(r"^##\s+", content[start:], re.MULTILINE)
        end = start + next_heading.start() if next_heading else len(content)
        section = content[start:end]
    elif require_claim_ledger:
        errors.append(f"{file_label}: missing required '## Claim ledger' section")
        section = ""

    claim_ids = set(CLAIM_ID_PATTERN.findall(section))
    if not claim_ids:
        errors.append(f"{file_label}: must reference at least one claim_id")

    return claim_ids, errors


def parse_doc_claim_refs(
    *,
    repo_root: Path,
    claims: list[ClaimRef],
) -> tuple[dict[str, set[str]], list[str]]:
    grouped_pages: dict[str, ClaimRef] = {}
    for claim in claims:
        key = doc_page_key(claim.doc_scope, claim.doc_page)
        if key not in grouped_pages:
            grouped_pages[key] = claim

    refs: dict[str, set[str]] = {}
    errors: list[str] = []

    for key, claim in sorted(grouped_pages.items(), key=lambda item: item[0]):
        path = resolve_doc_path(repo_root, claim.doc_page)
        path_label = path.relative_to(repo_root).as_posix() if path.is_relative_to(repo_root) else str(path)
        if not path.exists():
            errors.append(f"{key}: doc page not found at {path_label}")
            continue

        content = path.read_text(encoding="utf-8", errors="ignore")
        require_claim_ledger = claim.doc_scope in {"theory", "learn"}
        claim_ids, extraction_errors = extract_claim_ids_from_claim_ledger(
            content=content,
            require_claim_ledger=require_claim_ledger,
            file_label=path_label,
        )
        refs[key] = claim_ids
        errors.extend(extraction_errors)

    return refs, sorted(dict.fromkeys(errors))


def owner_paths_for_modules(owner_modules: tuple[str, ...], owner_routing: dict[str, str]) -> tuple[str, ...]:
    paths: list[str] = []
    for module in owner_modules:
        if module in owner_routing:
            paths.append(owner_routing[module])
        else:
            paths.append(f"/{module}/**")
    return tuple(sorted(dict.fromkeys(paths)))


def evaluate_claims(
    claims: list[ClaimRef],
    repo_root: Path,
    catalog: SignatureCatalog,
    require_tests: bool,
    owner_routing: dict[str, str],
) -> tuple[list[ClaimEvaluation], list[str]]:
    evaluations: list[ClaimEvaluation] = []
    errors: list[str] = []

    for claim in sorted(claims, key=lambda item: (item.journey_id, item.claim_id)):
        missing_signatures = tuple(
            sorted(
                ref.signature
                for ref in claim.signatures
                if not resolve_signature_ref(ref, catalog)
            )
        )

        missing_tests = tuple(
            sorted(
                test_ref
                for test_ref in claim.tests
                if not resolve_test_ref(test_ref, repo_root, catalog)
            )
        )

        signatures_ok = len(missing_signatures) == 0 and len(claim.signatures) > 0
        tests_ok = len(missing_tests) == 0 and (len(claim.tests) > 0 or not require_tests)

        if signatures_ok and tests_ok:
            computed_status = "supported"
        elif signatures_ok or tests_ok:
            computed_status = "at_risk"
        else:
            computed_status = "missing"

        if claim.status != computed_status:
            errors.append(
                f"{claim.claim_id}: declared status '{claim.status}' does not match computed '{computed_status}'"
            )

        evaluations.append(
            ClaimEvaluation(
                journey_id=claim.journey_id,
                claim_id=claim.claim_id,
                doc_scope=claim.doc_scope,
                doc_page=canonical_doc_page(claim.doc_page),
                claim_kind=claim.claim_kind,
                non_trivial=claim.non_trivial,
                declared_status=claim.status,
                computed_status=computed_status,
                missing_signatures=missing_signatures,
                missing_tests=missing_tests,
                owner_modules=claim.owner_modules,
                owner_paths=owner_paths_for_modules(claim.owner_modules, owner_routing),
            )
        )

    return evaluations, sorted(dict.fromkeys(errors))


def validate_doc_claim_references(
    claims: list[ClaimRef],
    doc_claim_refs: dict[str, set[str]],
) -> list[str]:
    errors: list[str] = []

    claim_ids_by_doc: dict[str, set[str]] = {}
    for claim in claims:
        key = doc_page_key(claim.doc_scope, claim.doc_page)
        claim_ids_by_doc.setdefault(key, set()).add(claim.claim_id)

    for key, doc_refs in sorted(doc_claim_refs.items()):
        valid = claim_ids_by_doc.get(key, set())
        unknown = sorted(ref for ref in doc_refs if ref not in valid)
        for claim_id in unknown:
            errors.append(
                f"{key}: unknown claim_id referenced in docs: {claim_id}"
            )

    for key, claim_ids in sorted(claim_ids_by_doc.items()):
        refs = doc_claim_refs.get(key, set())
        missing = sorted(claim_id for claim_id in claim_ids if claim_id not in refs)
        for claim_id in missing:
            errors.append(
                f"{key}: claim_id not referenced in docs: {claim_id}"
            )

    return sorted(dict.fromkeys(errors))


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


def rel_path(from_root: Path, path: Path) -> str:
    return path.resolve().relative_to(from_root.resolve()).as_posix()


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


def build_report(
    *,
    repo_root: Path,
    links_file: Path,
    signatures_dir: Path | None,
    schema_mode: str,
    links: list[LinkRef],
    resolved_links: list[LinkRef],
    missing_links: list[LinkRef],
    refresh_attempted: bool,
    refresh_succeeded: bool,
    refresh_details: str,
    claims_file: Path | None,
    claims: list[ClaimRef],
    claim_errors: list[str],
    claim_doc_errors: list[str],
    claim_evaluations: list[ClaimEvaluation],
    require_tests: bool,
    ci_mode: str,
) -> dict[str, Any]:
    supported = sum(1 for item in claim_evaluations if item.computed_status == "supported")
    at_risk = sum(1 for item in claim_evaluations if item.computed_status == "at_risk")
    missing = sum(1 for item in claim_evaluations if item.computed_status == "missing")
    claims_by_scope: dict[str, dict[str, int]] = {}
    unresolved_by_scope: dict[str, int] = {}

    for claim in claims:
        scope = claim.doc_scope
        claims_by_scope.setdefault(scope, {"total": 0, "supported": 0, "at_risk": 0, "missing": 0})
        claims_by_scope[scope]["total"] += 1

    for item in claim_evaluations:
        scope_summary = claims_by_scope.setdefault(
            item.doc_scope,
            {"total": 0, "supported": 0, "at_risk": 0, "missing": 0},
        )
        if item.computed_status == "supported":
            scope_summary["supported"] += 1
        elif item.computed_status == "at_risk":
            scope_summary["at_risk"] += 1
        else:
            scope_summary["missing"] += 1

    unresolved_claims = [
        {
            "journey_id": item.journey_id,
            "claim_id": item.claim_id,
            "doc_scope": item.doc_scope,
            "doc_page": item.doc_page,
            "claim_kind": item.claim_kind,
            "non_trivial": item.non_trivial,
            "declared_status": item.declared_status,
            "computed_status": item.computed_status,
            "missing_signatures": list(item.missing_signatures),
            "missing_tests": list(item.missing_tests),
            "owner_modules": list(item.owner_modules),
            "owner_paths": list(item.owner_paths),
        }
        for item in claim_evaluations
        if item.computed_status != "supported" and item.non_trivial
    ]

    for item in unresolved_claims:
        unresolved_by_scope[item["doc_scope"]] = unresolved_by_scope.get(item["doc_scope"], 0) + 1

    unresolved_claims_sorted = sorted(
        unresolved_claims,
        key=lambda item: (item["doc_scope"], item["journey_id"], item["claim_id"]),
    )

    has_issues = bool(missing_links or claim_errors or claim_doc_errors or unresolved_claims_sorted)

    return {
        "status": "ok" if not has_issues else "issues",
        "repo_root": str(repo_root),
        "links_file": str(links_file),
        "signatures_dir": str(signatures_dir) if signatures_dir else "",
        "schema_mode": schema_mode,
        "refresh_attempted": refresh_attempted,
        "refresh_succeeded": refresh_succeeded,
        "refresh_details": refresh_details,
        "ci_mode": ci_mode,
        "links": {
            "total": len(links),
            "resolved": len(resolved_links),
            "missing": len(missing_links),
            "missing_entries": [
                {
                    "journey_id": item.journey_id,
                    "title": item.title,
                    "kind": item.kind,
                    "signature": item.signature,
                }
                for item in missing_links
            ],
        },
        "claims": {
            "enabled": claims_file is not None,
            "claims_file": str(claims_file) if claims_file else "",
            "require_tests": require_tests,
            "total": len(claims),
            "supported": supported,
            "at_risk": at_risk,
            "missing": missing,
            "by_scope": claims_by_scope,
            "unresolved_by_scope": unresolved_by_scope,
            "schema_errors": claim_errors,
            "doc_reference_errors": claim_doc_errors,
            "unresolved": unresolved_claims_sorted,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", default=".", help="Repository root path.")
    parser.add_argument(
        "--links-file",
        default="docs/value-journeys/journey-signature-links.json",
        help="Journey/signature links JSON file.",
    )
    parser.add_argument(
        "--claims-file",
        default=None,
        help="Optional journey claims JSON file.",
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
        "--require-tests",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Require each claim to include and resolve test evidence.",
    )
    parser.add_argument(
        "--ci-mode",
        choices=["warn", "strict"],
        default="warn",
        help="warn keeps non-zero issues informational; strict fails when issues exist.",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Backward-compatible alias for --ci-mode strict.",
    )
    args = parser.parse_args()

    ci_mode = "strict" if args.strict else args.ci_mode

    repo_root = Path(args.repo_root).resolve()
    links_file = (repo_root / args.links_file).resolve()
    claims_file = (repo_root / args.claims_file).resolve() if args.claims_file else None

    if not links_file.exists():
        print(f"links file not found: {links_file}", file=sys.stderr)
        return 1

    links, schema_mode = parse_links(links_file)

    claims: list[ClaimRef] = []
    owner_routing: dict[str, str] = {}
    claim_errors: list[str] = []
    if claims_file is not None:
        if not claims_file.exists():
            claim_errors.append(f"claims file not found: {claims_file}")
        else:
            claims, owner_routing, claim_errors = parse_claims(
                claims_file=claims_file,
                require_tests=args.require_tests,
            )

    signatures_dir = detect_signatures_dir(repo_root, args.signatures_dir)
    refresh_attempted = False
    refresh_succeeded = False
    refresh_details = ""

    if signatures_dir is None and args.auto_refresh:
        refresh_attempted = True
        requested_rel = args.signatures_dir or "signatures"
        refresh_succeeded, refresh_details = refresh_artifacts(repo_root, requested_rel)
        if refresh_succeeded:
            signatures_dir = detect_signatures_dir(repo_root, requested_rel)
    elif signatures_dir is None:
        refresh_details = "signatures directory missing; run with --auto-refresh"

    if signatures_dir is not None:
        catalog = build_catalog(signatures_dir)
    else:
        catalog = SignatureCatalog(
            type_symbols=frozenset(),
            method_symbols=frozenset(),
            field_symbols=frozenset(),
            methods_by_type={},
            method_names_by_type={},
        )

    resolved_links, missing_links = resolve_links(links, catalog)

    claim_evaluations: list[ClaimEvaluation] = []
    claim_doc_errors: list[str] = []
    if claims and signatures_dir is not None:
        claim_evaluations, claim_eval_errors = evaluate_claims(
            claims=claims,
            repo_root=repo_root,
            catalog=catalog,
            require_tests=args.require_tests,
            owner_routing=owner_routing,
        )
        claim_errors = sorted(dict.fromkeys(claim_errors + claim_eval_errors))

        doc_claim_refs, doc_parse_errors = parse_doc_claim_refs(
            repo_root=repo_root,
            claims=claims,
        )
        claim_doc_errors = sorted(
            dict.fromkeys(
                doc_parse_errors + validate_doc_claim_references(claims, doc_claim_refs)
            )
        )

    needs_refresh = bool(missing_links)
    if claims and claim_evaluations:
        needs_refresh = needs_refresh or any(
            eval_item.computed_status != "supported"
            for eval_item in claim_evaluations
        )

    if needs_refresh and args.auto_refresh and not refresh_attempted:
        refresh_attempted = True
        signatures_rel = rel_path(repo_root, signatures_dir) if signatures_dir else "signatures"
        refresh_succeeded, refresh_details = refresh_artifacts(repo_root, signatures_rel)
        if refresh_succeeded:
            signatures_dir = detect_signatures_dir(repo_root, signatures_rel)
            if signatures_dir is not None:
                catalog = build_catalog(signatures_dir)
                resolved_links, missing_links = resolve_links(links, catalog)
                if claims:
                    claim_evaluations, claim_eval_errors = evaluate_claims(
                        claims=claims,
                        repo_root=repo_root,
                        catalog=catalog,
                        require_tests=args.require_tests,
                        owner_routing=owner_routing,
                    )
                    claim_errors = sorted(dict.fromkeys(claim_errors + claim_eval_errors))

                    doc_claim_refs, doc_parse_errors = parse_doc_claim_refs(
                        repo_root=repo_root,
                        claims=claims,
                    )
                    claim_doc_errors = sorted(
                        dict.fromkeys(
                            doc_parse_errors + validate_doc_claim_references(claims, doc_claim_refs)
                        )
                    )

    report = build_report(
        repo_root=repo_root,
        links_file=links_file,
        signatures_dir=signatures_dir,
        schema_mode=schema_mode,
        links=links,
        resolved_links=resolved_links,
        missing_links=missing_links,
        refresh_attempted=refresh_attempted,
        refresh_succeeded=refresh_succeeded,
        refresh_details=refresh_details,
        claims_file=claims_file,
        claims=claims,
        claim_errors=claim_errors,
        claim_doc_errors=claim_doc_errors,
        claim_evaluations=claim_evaluations,
        require_tests=args.require_tests,
        ci_mode=ci_mode,
    )

    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.report_out:
        report_out = (repo_root / args.report_out).resolve()
        report_out.parent.mkdir(parents=True, exist_ok=True)
        report_out.write_text(rendered, encoding="utf-8")
    print(rendered, end="")

    has_issues = report["status"] != "ok"
    if ci_mode == "strict" and has_issues:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
