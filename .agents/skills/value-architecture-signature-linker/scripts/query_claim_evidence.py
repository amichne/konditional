#!/usr/bin/env python3
"""Build deterministic pseudo-RAG evidence from signatures and linked docs."""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from validate_claim_signature_links import (
    detect_signatures_dir,
    parse_index_entries,
    parse_links,
    parse_type_line,
    refresh_artifacts,
)


TOKEN_RE = re.compile(r"[a-z0-9_]+")
HEADING_RE = re.compile(r"^\s{0,3}#{1,6}\s+(.*)$")


@dataclass(frozen=True)
class EvidenceEntry:
    source_kind: str
    subject_kind: str
    identifier: str
    source_path: str
    snippet: str
    linked_hint: bool


@dataclass(frozen=True)
class RankedEvidence:
    score: int
    matched_terms: tuple[str, ...]
    entry: EvidenceEntry


def tokenize(text: str) -> set[str]:
    return set(TOKEN_RE.findall(text.lower()))


def _slugify(text: str) -> str:
    lowered = text.lower().strip()
    slug = re.sub(r"[^a-z0-9\s-]", "", lowered)
    slug = re.sub(r"\s+", "-", slug)
    return slug.strip("-") or "section"


def rel_path(from_root: Path, path: Path) -> str:
    return path.resolve().relative_to(from_root.resolve()).as_posix()


def _collect_link_hints(
    repo_root: Path,
    links_file: Path | None,
) -> tuple[set[str], set[str]]:
    if links_file is None or not links_file.exists():
        return set(), set()

    link_refs, _ = parse_links(links_file)

    linked_signatures = {link.signature for link in link_refs}
    linked_documents: set[str] = set()

    for link in link_refs:
        path_text = link.document_path.strip()
        if not path_text:
            continue
        maybe_path = (repo_root / path_text).resolve()
        if maybe_path.exists():
            linked_documents.add(rel_path(repo_root, maybe_path))

    return linked_signatures, linked_documents


def _collect_doc_paths(
    repo_root: Path,
    linked_docs: set[str],
    explicit_docs: list[str],
    doc_globs: list[str],
) -> list[Path]:
    paths: set[Path] = set()

    for path_text in sorted(linked_docs):
        candidate = (repo_root / path_text).resolve()
        if candidate.exists() and candidate.is_file():
            paths.add(candidate)

    for path_text in explicit_docs:
        candidate = (repo_root / path_text).resolve()
        if candidate.exists() and candidate.is_file():
            paths.add(candidate)

    for pattern in doc_globs:
        for matched in sorted(repo_root.glob(pattern)):
            if matched.is_file():
                paths.add(matched.resolve())

    return sorted(paths)


def _load_signature_entries(
    repo_root: Path,
    signatures_dir: Path,
    linked_signatures: set[str],
) -> list[EvidenceEntry]:
    entries: list[EvidenceEntry] = []

    for sig_path in parse_index_entries(signatures_dir):
        if not sig_path.exists():
            continue

        source_path = rel_path(repo_root, sig_path)
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
                current_type = parse_type_line(line)
                if current_type and current_type != "<unknown>":
                    entries.append(
                        EvidenceEntry(
                            source_kind="signature",
                            subject_kind="type",
                            identifier=current_type,
                            source_path=source_path,
                            snippet=f"type {current_type}",
                            linked_hint=current_type in linked_signatures,
                        )
                    )
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
                full_sig = f"{current_type}#{method_sig}"
                entries.append(
                    EvidenceEntry(
                        source_kind="signature",
                        subject_kind="method",
                        identifier=full_sig,
                        source_path=source_path,
                        snippet=f"method {full_sig}",
                        linked_hint=full_sig in linked_signatures,
                    )
                )
                continue

            if line.startswith("- ") and section == "fields" and current_type:
                field_sig = line[2:].strip()
                full_sig = f"{current_type}#{field_sig}"
                entries.append(
                    EvidenceEntry(
                        source_kind="signature",
                        subject_kind="field",
                        identifier=full_sig,
                        source_path=source_path,
                        snippet=f"field {full_sig}",
                        linked_hint=full_sig in linked_signatures,
                    )
                )
                continue

            section = None

    return entries


def _flush_paragraph(
    entries: list[EvidenceEntry],
    repo_root: Path,
    doc_path: Path,
    heading: str,
    paragraph_lines: list[str],
    paragraph_index: int,
    linked_docs: set[str],
) -> int:
    snippet = " ".join(line.strip() for line in paragraph_lines if line.strip())
    if not snippet:
        return paragraph_index

    relative = rel_path(repo_root, doc_path)
    slug = _slugify(heading)
    identifier = f"{relative}#{slug}:{paragraph_index}"

    entries.append(
        EvidenceEntry(
            source_kind="document",
            subject_kind="doc-paragraph",
            identifier=identifier,
            source_path=relative,
            snippet=snippet,
            linked_hint=relative in linked_docs,
        )
    )
    return paragraph_index + 1


def _load_document_entries(
    repo_root: Path,
    doc_paths: list[Path],
    linked_docs: set[str],
    max_per_file: int,
) -> list[EvidenceEntry]:
    entries: list[EvidenceEntry] = []

    for doc_path in doc_paths:
        text = doc_path.read_text(encoding="utf-8", errors="ignore")
        heading = "document"
        paragraph_lines: list[str] = []
        paragraph_index = 1
        in_fence = False
        created_for_file = 0

        for raw_line in text.splitlines():
            line = raw_line.rstrip()

            if line.strip().startswith("```"):
                in_fence = not in_fence
                continue
            if in_fence:
                continue

            heading_match = HEADING_RE.match(line)
            if heading_match:
                if paragraph_lines and created_for_file < max_per_file:
                    paragraph_index = _flush_paragraph(
                        entries,
                        repo_root,
                        doc_path,
                        heading,
                        paragraph_lines,
                        paragraph_index,
                        linked_docs,
                    )
                    created_for_file += 1
                paragraph_lines = []
                heading = heading_match.group(1).strip() or heading
                continue

            if not line.strip():
                if paragraph_lines and created_for_file < max_per_file:
                    paragraph_index = _flush_paragraph(
                        entries,
                        repo_root,
                        doc_path,
                        heading,
                        paragraph_lines,
                        paragraph_index,
                        linked_docs,
                    )
                    created_for_file += 1
                paragraph_lines = []
                continue

            paragraph_lines.append(line)

        if paragraph_lines and created_for_file < max_per_file:
            _flush_paragraph(
                entries,
                repo_root,
                doc_path,
                heading,
                paragraph_lines,
                paragraph_index,
                linked_docs,
            )

    return entries


def _score_entry(query: str, query_terms: set[str], entry: EvidenceEntry) -> RankedEvidence | None:
    searchable = f"{entry.identifier} {entry.snippet}".lower()
    entry_terms = tokenize(searchable)
    matched = tuple(sorted(query_terms.intersection(entry_terms)))

    phrase_hit = query.lower().strip() in searchable

    if not matched and not phrase_hit:
        return None

    score = 0
    score += len(matched) * 10
    if phrase_hit:
        score += 6
    if entry.source_kind == "signature":
        score += 2
    if entry.linked_hint:
        score += 3

    return RankedEvidence(score=score, matched_terms=matched, entry=entry)


def _rank(
    query: str,
    entries: list[EvidenceEntry],
    top_k: int,
) -> list[RankedEvidence]:
    query_terms = tokenize(query)
    if not query_terms and not query.strip():
        return []

    ranked: list[RankedEvidence] = []
    for entry in entries:
        scored = _score_entry(query, query_terms, entry)
        if scored is not None:
            ranked.append(scored)

    ranked.sort(
        key=lambda item: (
            -item.score,
            item.entry.source_kind,
            item.entry.subject_kind,
            item.entry.identifier,
        )
    )
    return ranked[:top_k]


def _render_report(
    query: str,
    top_k: int,
    signatures_dir: Path | None,
    links_file: Path | None,
    total_entries: int,
    ranked: list[RankedEvidence],
    refresh_attempted: bool,
    refresh_succeeded: bool,
    refresh_details: str,
) -> dict[str, Any]:
    return {
        "query": query,
        "top_k": top_k,
        "signatures_dir": str(signatures_dir) if signatures_dir else "",
        "links_file": str(links_file) if links_file else "",
        "total_entries_indexed": total_entries,
        "results": [
            {
                "rank": index + 1,
                "score": item.score,
                "source_kind": item.entry.source_kind,
                "subject_kind": item.entry.subject_kind,
                "identifier": item.entry.identifier,
                "source_path": item.entry.source_path,
                "snippet": item.entry.snippet,
                "matched_terms": list(item.matched_terms),
                "linked_hint": item.entry.linked_hint,
            }
            for index, item in enumerate(ranked)
        ],
        "refresh_attempted": refresh_attempted,
        "refresh_succeeded": refresh_succeeded,
        "refresh_details": refresh_details,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", default=".", help="Repository root path.")
    parser.add_argument("--query", required=True, help="Free-text retrieval query.")
    parser.add_argument(
        "--signatures-dir",
        default=None,
        help="Signatures directory relative to repo root. Auto-detect if omitted.",
    )
    parser.add_argument(
        "--links-file",
        default="docs/claim-trace/claim-signature-links.json",
        help="Claim/signature links JSON for retrieval hints.",
    )
    parser.add_argument(
        "--doc-path",
        action="append",
        default=[],
        help="Optional doc path relative to repo root. Repeatable.",
    )
    parser.add_argument(
        "--docs-glob",
        action="append",
        default=[],
        help="Optional glob pattern for extra docs. Repeatable.",
    )
    parser.add_argument(
        "--signatures-only",
        action="store_true",
        help="Use only signatures as retrieval corpus.",
    )
    parser.add_argument("--top-k", type=int, default=12, help="Result limit.")
    parser.add_argument(
        "--max-doc-entries-per-file",
        type=int,
        default=120,
        help="Cap indexed markdown paragraph entries per file.",
    )
    parser.add_argument(
        "--report-out",
        default=None,
        help="Optional output path for machine-readable report JSON.",
    )
    parser.add_argument(
        "--auto-refresh",
        action="store_true",
        help="Regenerate signatures and public-surface context if missing.",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Return non-zero when no results are found.",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    links_file = (repo_root / args.links_file).resolve() if args.links_file else None

    signatures_dir = detect_signatures_dir(repo_root, args.signatures_dir)
    refresh_attempted = False
    refresh_succeeded = False
    refresh_details = ""

    if signatures_dir is None and args.auto_refresh:
        refresh_attempted = True
        signatures_rel = args.signatures_dir or "signatures"
        refresh_succeeded, refresh_details = refresh_artifacts(repo_root, signatures_rel)
        if refresh_succeeded:
            signatures_dir = detect_signatures_dir(repo_root, signatures_rel)

    if signatures_dir is None:
        print("signatures directory missing; use --auto-refresh", file=sys.stderr)
        return 1

    linked_signatures, linked_docs = _collect_link_hints(repo_root, links_file)

    signature_entries = _load_signature_entries(
        repo_root,
        signatures_dir,
        linked_signatures,
    )

    document_entries: list[EvidenceEntry] = []
    if not args.signatures_only:
        doc_paths = _collect_doc_paths(
            repo_root,
            linked_docs,
            args.doc_path,
            args.docs_glob,
        )
        document_entries = _load_document_entries(
            repo_root,
            doc_paths,
            linked_docs,
            max_per_file=max(1, args.max_doc_entries_per_file),
        )

    corpus = signature_entries + document_entries
    ranked = _rank(args.query, corpus, top_k=max(1, args.top_k))

    report = _render_report(
        query=args.query,
        top_k=max(1, args.top_k),
        signatures_dir=signatures_dir,
        links_file=links_file,
        total_entries=len(corpus),
        ranked=ranked,
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

    if args.strict and not ranked:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
