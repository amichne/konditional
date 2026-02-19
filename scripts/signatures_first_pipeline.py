#!/usr/bin/env python3
"""Signatures-first generation and verification pipeline."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any

NON_PUBLIC = re.compile(r"\b(private|protected|internal)\b")
EXPLICIT_PUBLIC = re.compile(r"\bpublic\b")
SOURCE_INCLUDE_MARKERS = (
    "/src/main/",
    "/src/commonMain/",
    "/src/jvmMain/",
    "/src/jsMain/",
    "/src/nativeMain/",
)
SOURCE_EXCLUDE_MARKERS = (
    "/src/test/",
    "/src/testFixtures/",
    "/src/integrationTest/",
    "/src/androidTest/",
    "/src/jmh/",
)
SOURCE_VERIFICATION_REASONS = {
    "symbol-disambiguation",
    "body-behavior",
    "contradiction-check",
}
RANKING_SIGNALS = [
    "path_match",
    "package_module_proximity",
    "import_connectivity",
    "sig_path",
]


@dataclass(frozen=True)
class TypeEntry:
    fqcn: str
    kind: str
    decl: str


@dataclass(frozen=True)
class SignatureRecord:
    sig_path: str
    source_file: str
    package: str
    imports: tuple[str, ...]
    types: tuple[TypeEntry, ...]
    fields: tuple[str, ...]
    methods: tuple[str, ...]

    @property
    def module(self) -> str:
        if "/" in self.source_file:
            return self.source_file.split("/", 1)[0]
        return "<root>"

    @property
    def language_mode(self) -> str:
        suffix = Path(self.source_file).suffix
        if suffix in {".kt", ".kts"}:
            return "kotlin"
        return "explicit_public"

    @property
    def import_packages(self) -> tuple[str, ...]:
        packages: list[str] = []
        for imp in self.imports:
            if imp.endswith(".*"):
                packages.append(imp[:-2])
                continue
            if "." not in imp:
                continue
            packages.append(imp.rsplit(".", 1)[0])
        return tuple(sorted(dict.fromkeys(packages)))


@dataclass(frozen=True)
class SymbolRecord:
    symbol_id: str
    kind: str
    sig_path: str
    source_file: str
    package: str
    declaration: str


@dataclass(frozen=True)
class TargetRecord:
    target_id: str
    kind: str
    package: str
    scope_sig_paths: tuple[str, ...]
    symbol_ids: tuple[str, ...]
    target_hash: str
    ranking_trace_ref: str


def normalize_ws(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def short_hash(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:16]


def stable_hash(payload: Any) -> str:
    canonical = json.dumps(payload, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def is_public_signature(signature: str, mode: str) -> bool:
    normalized = normalize_ws(signature)
    if NON_PUBLIC.search(normalized):
        return False
    if mode == "kotlin":
        return True
    return bool(EXPLICIT_PUBLIC.search(normalized))


def is_primary_source(file_path: str) -> bool:
    normalized = "/" + file_path.strip("/")
    if any(marker in normalized for marker in SOURCE_EXCLUDE_MARKERS):
        return False
    if any(marker in normalized for marker in SOURCE_INCLUDE_MARKERS):
        return True
    return False


def is_public_location(package: str, file_path: str) -> bool:
    normalized_path = "/" + file_path.strip("/") + "/"
    if "/internal/" in normalized_path:
        return False
    if package == "<default>":
        return True
    return ".internal." not in package and not package.endswith(".internal")


def parse_type_line(raw: str) -> TypeEntry:
    parts: dict[str, str] = {}
    for token in raw.split("|"):
        if "=" not in token:
            continue
        key, value = token.split("=", 1)
        parts[key.strip()] = value.strip()
    return TypeEntry(
        fqcn=parts.get("type", "<unknown>"),
        kind=parts.get("kind", "<unknown>"),
        decl=normalize_ws(parts.get("decl", "")),
    )


def read_index(signatures_dir: Path) -> list[str]:
    index_path = signatures_dir / "INDEX.sig"
    if not index_path.exists():
        raise FileNotFoundError(f"Missing index file: {index_path}")

    entries = [line.strip() for line in index_path.read_text(encoding="utf-8").splitlines() if line.strip()]
    if any("|" in entry for entry in entries):
        raise ValueError("INDEX.sig must be path-only; metadata suffixes are not allowed")
    if entries != sorted(entries):
        raise ValueError("INDEX.sig must be lexicographically sorted")
    if len(entries) != len(set(entries)):
        raise ValueError("INDEX.sig must not contain duplicate paths")
    if any(entry == "INDEX.sig" for entry in entries):
        raise ValueError("INDEX.sig must not include itself")
    return entries


def parse_signature(signatures_dir: Path, relative_path: str) -> SignatureRecord:
    sig_path = signatures_dir / relative_path
    if not sig_path.exists():
        raise FileNotFoundError(f"Missing signature file referenced by index: {relative_path}")

    lines = [line.rstrip() for line in sig_path.read_text(encoding="utf-8").splitlines() if line.strip()]
    if len(lines) < 2:
        raise ValueError(f"Invalid signature schema in {relative_path}: expected at least file/package headers")
    if not lines[0].startswith("file="):
        raise ValueError(f"Invalid signature schema in {relative_path}: first line must be file=")
    if not lines[1].startswith("package="):
        raise ValueError(f"Invalid signature schema in {relative_path}: second line must be package=")

    source_file = lines[0].split("=", 1)[1].strip()
    package = lines[1].split("=", 1)[1].strip()

    imports: list[str] = []
    types: list[TypeEntry] = []
    fields: list[str] = []
    methods: list[str] = []

    i = 2
    if i < len(lines) and lines[i].startswith("imports="):
        raw_imports = lines[i].split("=", 1)[1].strip()
        imports = [part for part in raw_imports.split(",") if part]
        i += 1

    if i >= len(lines):
        raise ValueError(f"Invalid signature schema in {relative_path}: missing type= or types=<none>")

    if lines[i] == "types=<none>":
        i += 1
        if i != len(lines):
            raise ValueError(
                f"Invalid signature schema in {relative_path}: types=<none> must be the final section"
            )
        return SignatureRecord(
            sig_path=relative_path,
            source_file=source_file,
            package=package,
            imports=tuple(sorted(dict.fromkeys(imports))),
            types=tuple(),
            fields=tuple(),
            methods=tuple(),
        )

    while i < len(lines) and lines[i].startswith("type="):
        types.append(parse_type_line(lines[i]))
        i += 1

    if not types:
        raise ValueError(f"Invalid signature schema in {relative_path}: missing type= or types=<none>")

    if i < len(lines) and lines[i] == "fields:":
        i += 1
        while i < len(lines) and lines[i].startswith("- "):
            fields.append(normalize_ws(lines[i][2:]))
            i += 1

    if i < len(lines) and lines[i] == "methods:":
        i += 1
        while i < len(lines) and lines[i].startswith("- "):
            methods.append(normalize_ws(lines[i][2:]))
            i += 1

    if i != len(lines):
        raise ValueError(f"Invalid signature schema in {relative_path}: unexpected section ordering")

    return SignatureRecord(
        sig_path=relative_path,
        source_file=source_file,
        package=package,
        imports=tuple(sorted(dict.fromkeys(imports))),
        types=tuple(types),
        fields=tuple(dict.fromkeys(fields)),
        methods=tuple(dict.fromkeys(methods)),
    )


def load_signatures(signatures_dir: Path) -> list[SignatureRecord]:
    records: list[SignatureRecord] = []
    for entry in read_index(signatures_dir):
        records.append(parse_signature(signatures_dir, entry))
    return records


def select_documentation_records(records: list[SignatureRecord]) -> list[SignatureRecord]:
    selected: list[SignatureRecord] = []
    for record in records:
        if not is_primary_source(record.source_file):
            continue
        if not is_public_location(record.package, record.source_file):
            continue
        selected.append(record)
    return selected


def build_symbols(records: list[SignatureRecord]) -> dict[str, SymbolRecord]:
    symbols: dict[str, SymbolRecord] = {}
    for record in records:
        mode = record.language_mode

        for type_entry in record.types:
            if not is_public_signature(type_entry.decl, mode):
                continue
            symbol_id = f"type:{type_entry.fqcn}"
            symbols[symbol_id] = SymbolRecord(
                symbol_id=symbol_id,
                kind="type",
                sig_path=record.sig_path,
                source_file=record.source_file,
                package=record.package,
                declaration=type_entry.decl,
            )

        for method in record.methods:
            if not is_public_signature(method, mode):
                continue
            symbol_id = f"method:{short_hash(record.sig_path + '|' + method)}"
            symbols[symbol_id] = SymbolRecord(
                symbol_id=symbol_id,
                kind="method",
                sig_path=record.sig_path,
                source_file=record.source_file,
                package=record.package,
                declaration=method,
            )

    return symbols


def ranking_score(
    candidate: SignatureRecord,
    anchor: SignatureRecord,
    scope_sig_paths: set[str],
    target_package: str,
) -> tuple[int, int, int, str]:
    path_match = 1 if candidate.sig_path in scope_sig_paths else 0
    package_module_proximity = 0
    if candidate.package == target_package:
        package_module_proximity += 2
    if candidate.module == anchor.module:
        package_module_proximity += 1

    import_connectivity = 0
    if candidate.package in anchor.import_packages or anchor.package in candidate.import_packages:
        import_connectivity = 1

    return path_match, package_module_proximity, import_connectivity, candidate.sig_path


def sort_candidates(
    candidates: list[SignatureRecord],
    anchor: SignatureRecord,
    scope_sig_paths: tuple[str, ...],
    target_package: str,
) -> list[dict[str, Any]]:
    scored: list[dict[str, Any]] = []
    scope_set = set(scope_sig_paths)
    for candidate in candidates:
        score = ranking_score(candidate, anchor, scope_set, target_package)
        scored.append(
            {
                "sig_path": candidate.sig_path,
                "path_match": score[0],
                "package_module_proximity": score[1],
                "import_connectivity": score[2],
            }
        )

    scored.sort(
        key=lambda item: (
            -item["path_match"],
            -item["package_module_proximity"],
            -item["import_connectivity"],
            item["sig_path"],
        )
    )
    return scored


def target_hash(target: TargetRecord, symbols: dict[str, SymbolRecord]) -> str:
    symbol_entries = []
    for symbol_id in target.symbol_ids:
        symbol = symbols[symbol_id]
        symbol_entries.append(
            {
                "symbol_id": symbol.symbol_id,
                "kind": symbol.kind,
                "sig_path": symbol.sig_path,
                "source_file": symbol.source_file,
                "package": symbol.package,
                "declaration": symbol.declaration,
            }
        )

    payload = {
        "target_id": target.target_id,
        "kind": target.kind,
        "package": target.package,
        "scope_sig_paths": list(target.scope_sig_paths),
        "symbols": symbol_entries,
    }
    return stable_hash(payload)


def build_targets_and_trace(records: list[SignatureRecord], symbols: dict[str, SymbolRecord]) -> tuple[list[TargetRecord], list[dict[str, Any]]]:
    records_by_sig = {record.sig_path: record for record in records}

    package_to_sigs: dict[str, list[str]] = defaultdict(list)
    package_to_symbols: dict[str, set[str]] = defaultdict(set)
    file_to_method_symbols: dict[str, set[str]] = defaultdict(set)
    file_to_sig_path: dict[str, str] = {}
    type_symbols: list[tuple[str, str, str]] = []

    for symbol in symbols.values():
        package_to_symbols[symbol.package].add(symbol.symbol_id)
        package_to_sigs[symbol.package].append(symbol.sig_path)
        if symbol.kind == "method":
            file_to_method_symbols[symbol.source_file].add(symbol.symbol_id)
            file_to_sig_path[symbol.source_file] = symbol.sig_path
        if symbol.kind == "type":
            type_symbols.append((symbol.symbol_id, symbol.sig_path, symbol.package))

    for package, sigs in package_to_sigs.items():
        package_to_sigs[package] = sorted(dict.fromkeys(sigs))

    targets: list[TargetRecord] = []
    traces: list[dict[str, Any]] = [
        {
            "event": "consumed_signatures",
            "sig_paths": sorted(record.sig_path for record in records),
        }
    ]

    for package in sorted(package_to_sigs):
        scope_sig_paths = tuple(package_to_sigs[package])
        symbol_ids = tuple(sorted(package_to_symbols[package]))
        target = TargetRecord(
            target_id=f"package:{package}",
            kind="package",
            package=package,
            scope_sig_paths=scope_sig_paths,
            symbol_ids=symbol_ids,
            target_hash="",
            ranking_trace_ref=f"target-ranking:package:{package}",
        )
        anchor = records_by_sig[scope_sig_paths[0]]
        candidates = sort_candidates(records, anchor, scope_sig_paths, package)
        traces.append(
            {
                "event": "target_ranking",
                "target_id": target.target_id,
                "ranking_signals": RANKING_SIGNALS,
                "anchor_sig_path": anchor.sig_path,
                "selected_scope_sig_paths": list(scope_sig_paths),
                "candidate_count": len(candidates),
                "top_candidates": candidates[:12],
            }
        )
        targets.append(target)

    for symbol_id, sig_path, package in sorted(type_symbols, key=lambda item: item[0]):
        scope_sig_paths = (sig_path,)
        target = TargetRecord(
            target_id=f"type:{symbol_id.removeprefix('type:')}",
            kind="type",
            package=package,
            scope_sig_paths=scope_sig_paths,
            symbol_ids=(symbol_id,),
            target_hash="",
            ranking_trace_ref=f"target-ranking:type:{symbol_id.removeprefix('type:')}",
        )
        anchor = records_by_sig[sig_path]
        candidates = sort_candidates(records, anchor, scope_sig_paths, package)
        traces.append(
            {
                "event": "target_ranking",
                "target_id": target.target_id,
                "ranking_signals": RANKING_SIGNALS,
                "anchor_sig_path": anchor.sig_path,
                "selected_scope_sig_paths": [sig_path],
                "candidate_count": len(candidates),
                "top_candidates": candidates[:12],
            }
        )
        targets.append(target)

    for source_file in sorted(file_to_method_symbols):
        sig_path = file_to_sig_path[source_file]
        package = records_by_sig[sig_path].package
        symbol_ids = tuple(sorted(file_to_method_symbols[source_file]))
        target = TargetRecord(
            target_id=f"entrypoint:{source_file}",
            kind="entrypoint",
            package=package,
            scope_sig_paths=(sig_path,),
            symbol_ids=symbol_ids,
            target_hash="",
            ranking_trace_ref=f"target-ranking:entrypoint:{source_file}",
        )
        anchor = records_by_sig[sig_path]
        candidates = sort_candidates(records, anchor, (sig_path,), package)
        traces.append(
            {
                "event": "target_ranking",
                "target_id": target.target_id,
                "ranking_signals": RANKING_SIGNALS,
                "anchor_sig_path": anchor.sig_path,
                "selected_scope_sig_paths": [sig_path],
                "candidate_count": len(candidates),
                "top_candidates": candidates[:12],
            }
        )
        targets.append(target)

    finalized_targets: list[TargetRecord] = []
    for target in sorted(targets, key=lambda item: (item.kind, item.target_id)):
        provisional = TargetRecord(
            target_id=target.target_id,
            kind=target.kind,
            package=target.package,
            scope_sig_paths=target.scope_sig_paths,
            symbol_ids=target.symbol_ids,
            target_hash="",
            ranking_trace_ref=target.ranking_trace_ref,
        )
        finalized_targets.append(
            TargetRecord(
                target_id=target.target_id,
                kind=target.kind,
                package=target.package,
                scope_sig_paths=target.scope_sig_paths,
                symbol_ids=target.symbol_ids,
                target_hash=target_hash(provisional, symbols),
                ranking_trace_ref=target.ranking_trace_ref,
            )
        )

    return finalized_targets, traces


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def ensure_claims_ledger(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        path.write_text("", encoding="utf-8")


def build_schemas() -> dict[str, Any]:
    return {
        "version": 1,
        "target_types": {
            "package": {
                "required_sections": ["overview", "public-types", "entrypoints", "guarantees"],
                "required_fields": ["target_id", "scope_sig_paths", "symbol_ids", "claims"],
            },
            "type": {
                "required_sections": ["purpose", "invariants", "api-surface", "failure-modes"],
                "required_fields": ["target_id", "scope_sig_paths", "symbol_ids", "claims"],
            },
            "entrypoint": {
                "required_sections": ["inputs", "outputs", "determinism", "operational-notes"],
                "required_fields": ["target_id", "scope_sig_paths", "symbol_ids", "claims"],
            },
        },
        "claim_ledger": {
            "format": "jsonl",
            "required_fields": ["claim_id", "target_id", "target_hash", "doc_ref", "claim", "evidence"],
            "evidence_kinds": {
                "signature_symbol": {
                    "required_fields": ["kind", "symbol_id"],
                },
                "source_verification": {
                    "required_fields": ["kind", "symbol_id", "reason", "source_ref"],
                    "allowed_reasons": sorted(SOURCE_VERIFICATION_REASONS),
                },
            },
        },
    }


def write_determinism_trace(path: Path, traces: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        for trace in traces:
            handle.write(json.dumps(trace, sort_keys=True) + "\n")


def build_surface_hash(
    signatures_dir: Path,
    index_paths: list[str],
    targets: list[TargetRecord],
) -> dict[str, Any]:
    sig_hashes = []
    for relative in index_paths:
        contents = (signatures_dir / relative).read_text(encoding="utf-8")
        sig_hashes.append(
            {
                "sig_path": relative,
                "sig_sha256": hashlib.sha256(contents.encode("utf-8")).hexdigest(),
            }
        )

    target_hashes = [
        {
            "target_id": target.target_id,
            "target_hash": target.target_hash,
        }
        for target in targets
    ]

    global_payload = {
        "sig_hashes": sig_hashes,
        "target_hashes": target_hashes,
    }

    return {
        "version": 1,
        "sig_hashes": sig_hashes,
        "target_hashes": target_hashes,
        "global_hash": stable_hash(global_payload),
    }


def load_claims(claims_path: Path) -> list[dict[str, Any]]:
    claims: list[dict[str, Any]] = []
    if not claims_path.exists():
        raise FileNotFoundError(f"Missing claims ledger: {claims_path}")

    for line_no, raw_line in enumerate(claims_path.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw_line.strip()
        if not line:
            continue
        try:
            payload = json.loads(line)
        except json.JSONDecodeError as error:
            claims.append(
                {
                    "_invalid": True,
                    "_line": line_no,
                    "_error": f"Invalid JSON: {error}",
                }
            )
            continue
        if not isinstance(payload, dict):
            claims.append(
                {
                    "_invalid": True,
                    "_line": line_no,
                    "_error": "Claim entry must be a JSON object",
                }
            )
            continue
        payload["_line"] = line_no
        claims.append(payload)
    return claims


def validate_claims(
    repo_root: Path,
    claims: list[dict[str, Any]],
    targets: dict[str, TargetRecord],
    symbols: dict[str, SymbolRecord],
) -> tuple[list[dict[str, Any]], dict[str, list[str]], dict[str, list[str]]]:
    invalid_by_target: dict[str, list[str]] = defaultdict(list)
    stale_by_target: dict[str, list[str]] = defaultdict(list)
    valid_claims: list[dict[str, Any]] = []
    seen_claim_ids: set[str] = set()

    for claim in claims:
        line_no = claim.get("_line", 0)
        if claim.get("_invalid"):
            invalid_by_target["<unscoped>"].append(f"line {line_no}: {claim.get('_error', 'invalid claim')}")
            continue

        required_fields = ["claim_id", "target_id", "target_hash", "doc_ref", "claim", "evidence"]
        missing = [field for field in required_fields if field not in claim]
        if missing:
            invalid_by_target["<unscoped>"].append(
                f"line {line_no}: missing required fields {', '.join(missing)}"
            )
            continue

        claim_id = claim["claim_id"]
        target_id = claim["target_id"]
        target = targets.get(target_id)
        if not isinstance(claim_id, str) or not claim_id:
            invalid_by_target[target_id].append(f"line {line_no}: claim_id must be a non-empty string")
            continue
        if claim_id in seen_claim_ids:
            invalid_by_target[target_id].append(f"line {line_no}: duplicate claim_id={claim_id}")
            continue
        seen_claim_ids.add(claim_id)

        if target is None:
            invalid_by_target["<unscoped>"].append(f"line {line_no}: unknown target_id={target_id}")
            continue

        doc_ref = claim["doc_ref"]
        if not isinstance(doc_ref, str) or not doc_ref:
            invalid_by_target[target_id].append(f"line {line_no}: doc_ref must be a non-empty string")
            continue
        if not (repo_root / doc_ref).exists():
            invalid_by_target[target_id].append(f"line {line_no}: doc_ref path does not exist: {doc_ref}")
            continue

        evidence = claim["evidence"]
        if not isinstance(evidence, list) or not evidence:
            invalid_by_target[target_id].append(f"line {line_no}: evidence must be a non-empty list")
            continue

        target_symbol_set = set(target.symbol_ids)
        evidence_valid = True
        for entry in evidence:
            if not isinstance(entry, dict):
                invalid_by_target[target_id].append(f"line {line_no}: evidence item must be an object")
                evidence_valid = False
                break
            kind = entry.get("kind")
            if kind == "signature_symbol":
                symbol_id = entry.get("symbol_id")
                if not isinstance(symbol_id, str) or symbol_id not in target_symbol_set:
                    invalid_by_target[target_id].append(
                        f"line {line_no}: signature_symbol must reference a target-scoped symbol_id"
                    )
                    evidence_valid = False
                    break
                if symbol_id not in symbols:
                    invalid_by_target[target_id].append(f"line {line_no}: unknown symbol_id={symbol_id}")
                    evidence_valid = False
                    break
            elif kind == "source_verification":
                symbol_id = entry.get("symbol_id")
                reason = entry.get("reason")
                source_ref = entry.get("source_ref")
                if not isinstance(symbol_id, str) or symbol_id not in target_symbol_set:
                    invalid_by_target[target_id].append(
                        f"line {line_no}: source_verification must reference a target-scoped symbol_id"
                    )
                    evidence_valid = False
                    break
                if reason not in SOURCE_VERIFICATION_REASONS:
                    invalid_by_target[target_id].append(
                        f"line {line_no}: source_verification reason must be one of {sorted(SOURCE_VERIFICATION_REASONS)}"
                    )
                    evidence_valid = False
                    break
                if not isinstance(source_ref, str) or not source_ref:
                    invalid_by_target[target_id].append(
                        f"line {line_no}: source_verification source_ref must be a non-empty string"
                    )
                    evidence_valid = False
                    break
            else:
                invalid_by_target[target_id].append(
                    f"line {line_no}: unsupported evidence kind={kind}. Allowed: signature_symbol, source_verification"
                )
                evidence_valid = False
                break

        if not evidence_valid:
            continue

        if claim["target_hash"] != target.target_hash:
            stale_by_target[target_id].append(
                f"line {line_no}: target_hash mismatch (expected {target.target_hash})"
            )
            continue

        valid_claims.append(claim)

    return valid_claims, invalid_by_target, stale_by_target


def build_coverage_report(
    targets: list[TargetRecord],
    valid_claims: list[dict[str, Any]],
    invalid_by_target: dict[str, list[str]],
    stale_by_target: dict[str, list[str]],
) -> dict[str, Any]:
    target_ids = {target.target_id for target in targets}
    valid_by_target: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for claim in valid_claims:
        valid_by_target[claim["target_id"]].append(claim)

    entries: list[dict[str, Any]] = []
    complete = 0
    stale = 0
    invalid = 0
    missing = 0

    for target in targets:
        target_invalid = invalid_by_target.get(target.target_id, [])
        target_stale = stale_by_target.get(target.target_id, [])
        target_valid = valid_by_target.get(target.target_id, [])

        if target_invalid:
            status = "invalid"
            invalid += 1
        elif target_stale:
            status = "stale"
            stale += 1
        elif target_valid:
            status = "complete"
            complete += 1
        else:
            status = "missing"
            missing += 1

        entries.append(
            {
                "target_id": target.target_id,
                "kind": target.kind,
                "status": status,
                "scope_sig_paths": list(target.scope_sig_paths),
                "target_hash": target.target_hash,
                "valid_claim_count": len(target_valid),
                "doc_refs": sorted({claim["doc_ref"] for claim in target_valid}),
                "issues": sorted(target_invalid + target_stale),
            }
        )

    unscoped_issues: list[str] = []
    for target_id, issues in invalid_by_target.items():
        if target_id == "<unscoped>" or target_id not in target_ids:
            unscoped_issues.extend(issues)
    unscoped_issues = sorted(unscoped_issues)

    return {
        "version": 1,
        "totals": {
            "targets": len(targets),
            "complete": complete,
            "stale": stale,
            "invalid": invalid,
            "missing": missing,
            "unscoped_invalid_claims": len(unscoped_issues),
        },
        "targets": entries,
        "unscoped_issues": unscoped_issues,
    }


def fail_for_coverage(report: dict[str, Any], fail_on_missing: bool) -> None:
    totals = report["totals"]
    if totals["stale"] > 0 or totals["invalid"] > 0 or totals["unscoped_invalid_claims"] > 0:
        raise SystemExit(
            "Verification failed: stale/invalid documentation claims detected. "
            f"stale={totals['stale']} invalid={totals['invalid']} "
            f"unscoped_invalid_claims={totals['unscoped_invalid_claims']}"
        )
    if fail_on_missing and totals["missing"] > 0:
        raise SystemExit(
            f"Verification failed: missing documentation targets detected (missing={totals['missing']})"
        )


def ensure_hard_cutover(repo_root: Path) -> None:
    legacy = repo_root / ".signatures"
    if legacy.exists():
        raise SystemExit("Hard cutover violation: .signatures/ is unsupported. Use signatures/ only.")


def run_generate(repo_root: Path, signatures_dir: Path) -> None:
    generator = repo_root / "scripts" / "generate-signatures.sh"
    if not generator.exists():
        raise FileNotFoundError(f"Missing generator wrapper: {generator}")
    subprocess.run([str(generator)], check=True, cwd=repo_root)

    public_surface = (
        repo_root
        / ".agents"
        / "skills"
        / "public-surface-init-context"
        / "scripts"
        / "build_public_surface_context.py"
    )
    if not public_surface.exists():
        raise FileNotFoundError(f"Missing public-surface generator: {public_surface}")

    subprocess.run(
        [
            "python3",
            str(public_surface),
            "--repo-root",
            str(repo_root),
            "--signatures-dir",
            signatures_dir.relative_to(repo_root).as_posix(),
            "--output",
            (signatures_dir / "PUBLIC_SURFACE.ctx").relative_to(repo_root).as_posix(),
        ],
        check=True,
        cwd=repo_root,
    )


def build_targets_payload(targets: list[TargetRecord], symbols: dict[str, SymbolRecord]) -> dict[str, Any]:
    return {
        "version": 1,
        "ranking": {
            "signals": RANKING_SIGNALS,
            "sort": "desc(path_match,package_module_proximity,import_connectivity),asc(sig_path)",
        },
        "targets": [
            {
                "id": target.target_id,
                "kind": target.kind,
                "package": target.package,
                "symbol_ids": list(target.symbol_ids),
                "scope_sig_paths": list(target.scope_sig_paths),
                "target_hash": target.target_hash,
                "ranking_trace_ref": target.ranking_trace_ref,
            }
            for target in targets
        ],
        "symbols": [
            {
                "id": symbol.symbol_id,
                "kind": symbol.kind,
                "sig_path": symbol.sig_path,
                "source_file": symbol.source_file,
                "package": symbol.package,
                "declaration": symbol.declaration,
            }
            for symbol in sorted(symbols.values(), key=lambda item: item.symbol_id)
        ],
    }


def run_pipeline(repo_root: Path, mode: str, fail_on_missing: bool) -> None:
    ensure_hard_cutover(repo_root)

    signatures_dir = repo_root / "signatures"
    docs_dir = signatures_dir / "docs"
    targets_path = docs_dir / "targets.json"
    schemas_path = docs_dir / "schemas.json"
    claims_path = docs_dir / "claims-ledger.jsonl"
    coverage_path = docs_dir / "coverage-report.json"
    trace_path = docs_dir / "determinism-trace.jsonl"
    surface_hash_path = docs_dir / "surface-hash.json"

    if mode in {"generate", "all"}:
        run_generate(repo_root, signatures_dir)

    index_paths = read_index(signatures_dir)
    records = load_signatures(signatures_dir)
    scoped_records = select_documentation_records(records)
    symbols = build_symbols(scoped_records)
    targets, traces = build_targets_and_trace(scoped_records, symbols)

    write_json(targets_path, build_targets_payload(targets, symbols))
    write_json(schemas_path, build_schemas())
    write_determinism_trace(trace_path, traces)
    write_json(surface_hash_path, build_surface_hash(signatures_dir, index_paths, targets))
    ensure_claims_ledger(claims_path)

    claims = load_claims(claims_path)
    targets_by_id = {target.target_id: target for target in targets}
    valid_claims, invalid_by_target, stale_by_target = validate_claims(
        repo_root=repo_root,
        claims=claims,
        targets=targets_by_id,
        symbols=symbols,
    )
    coverage = build_coverage_report(
        targets=targets,
        valid_claims=valid_claims,
        invalid_by_target=invalid_by_target,
        stale_by_target=stale_by_target,
    )
    write_json(coverage_path, coverage)

    if mode in {"verify", "all"}:
        fail_for_coverage(coverage, fail_on_missing=fail_on_missing)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", default=".", help="Repository root.")
    parser.add_argument(
        "--mode",
        choices=("generate", "verify", "all"),
        default="all",
        help="Pipeline mode: generate artifacts, verify artifacts, or both.",
    )
    parser.add_argument(
        "--fail-on-missing",
        action="store_true",
        help="Fail verification when coverage status includes missing targets.",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    run_pipeline(repo_root=repo_root, mode=args.mode, fail_on_missing=args.fail_on_missing)
    print(
        "Signatures-first pipeline completed "
        f"(mode={args.mode}, fail_on_missing={args.fail_on_missing})."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
