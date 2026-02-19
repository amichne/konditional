#!/usr/bin/env python3
"""Validate journey/signature links with optional artifact refresh."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class LinkRef:
    journey_id: str
    title: str
    value_proposition: str
    kind: str
    signature: str


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

        journey_id = str(
            row.get("journey_id")
            or row.get("story_id")
            or "<unknown>"
        )
        title = str(row.get("title", ""))
        value_proposition = str(
            row.get("value_proposition")
            or row.get("outcome")
            or ""
        )

        links = row.get("links", [])
        if not isinstance(links, list):
            continue

        for link in links:
            if not isinstance(link, dict):
                continue
            signature = str(link.get("signature", "")).strip()
            if not signature:
                continue
            kind = str(link.get("kind", "")).strip()
            if not kind:
                kind = infer_kind(signature)

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


def infer_kind(signature: str) -> str:
    if "#" in signature:
        return "method"
    return "type"


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
        current_types: list[str] = []
        section: str | None = None
        for raw_line in sig_path.read_text(
            encoding="utf-8", errors="ignore"
        ).splitlines():
            line = raw_line.strip()
            if not line:
                continue
            if line.startswith("type="):
                current_type = parse_type_line(line)
                if current_type and current_type != "<unknown>":
                    type_symbols.add(current_type)
                    current_types.append(current_type)
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
                for current_type in current_types:
                    method_symbols.add(f"{current_type}#{method_sig}")
                continue
            if line.startswith("- ") and section == "fields":
                field_sig = line[2:].strip()
                for current_type in current_types:
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
        key=lambda item: (item.journey_id, item.kind, item.signature),
    )
    return resolved, missing_sorted


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


def report_dict(
    repo_root: Path,
    links_file: Path,
    signatures_dir: Path | None,
    schema_mode: str,
    links: list[LinkRef],
    resolved: list[LinkRef],
    missing: list[LinkRef],
    refresh_attempted: bool,
    refresh_succeeded: bool,
    refresh_details: str,
) -> dict[str, Any]:
    return {
        "status": "ok" if not missing else "missing-links",
        "repo_root": str(repo_root),
        "links_file": str(links_file),
        "signatures_dir": str(signatures_dir) if signatures_dir else "",
        "schema_mode": schema_mode,
        "total_links": len(links),
        "resolved_links": len(resolved),
        "missing_links": len(missing),
        "refresh_attempted": refresh_attempted,
        "refresh_succeeded": refresh_succeeded,
        "refresh_details": refresh_details,
        "missing": [
            {
                "journey_id": item.journey_id,
                "title": item.title,
                "value_proposition": item.value_proposition,
                "kind": item.kind,
                "signature": item.signature,
            }
            for item in missing
        ],
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
        help="Return non-zero when missing links remain after refresh attempts.",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    links_file = (repo_root / args.links_file).resolve()
    if not links_file.exists():
        print(f"links file not found: {links_file}", file=sys.stderr)
        return 1

    links, schema_mode = parse_links(links_file)
    signatures_dir = detect_signatures_dir(repo_root, args.signatures_dir)

    refresh_attempted = False
    refresh_succeeded = False
    refresh_details = ""

    if signatures_dir is None and args.auto_refresh:
        refresh_attempted = True
        requested_rel = args.signatures_dir or "signatures"
        refresh_succeeded, refresh_details = refresh_artifacts(
            repo_root, requested_rel
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
        links,
        type_symbols,
        method_symbols,
        field_symbols,
    )

    if missing and args.auto_refresh and not refresh_attempted:
        refresh_attempted = True
        if signatures_dir is not None:
            signatures_rel = rel_path(repo_root, signatures_dir)
        else:
            signatures_rel = "signatures"
        refresh_succeeded, refresh_details = refresh_artifacts(
            repo_root, signatures_rel
        )
        if refresh_succeeded:
            signatures_dir = detect_signatures_dir(repo_root, signatures_rel)
            if signatures_dir is not None:
                type_symbols, method_symbols, field_symbols = build_catalog(
                    signatures_dir
                )
                resolved, missing = resolve_links(
                    links,
                    type_symbols,
                    method_symbols,
                    field_symbols,
                )

    report = report_dict(
        repo_root=repo_root,
        links_file=links_file,
        signatures_dir=signatures_dir,
        schema_mode=schema_mode,
        links=links,
        resolved=resolved,
        missing=missing,
        refresh_attempted=refresh_attempted,
        refresh_succeeded=refresh_succeeded,
        refresh_details=refresh_details,
    )

    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.report_out:
        report_out = (repo_root / args.report_out).resolve()
        report_out.parent.mkdir(parents=True, exist_ok=True)
        report_out.write_text(rendered, encoding="utf-8")
    print(rendered, end="")

    if args.strict and missing:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
