#!/usr/bin/env python3
"""Build a deterministic public-surface bootstrap context from signatures."""

from __future__ import annotations

import argparse
import re
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

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


@dataclass(frozen=True)
class PublicType:
    fqcn: str
    kind: str
    decl: str
    package: str
    file: str


@dataclass(frozen=True)
class SignatureRecord:
    file: str
    package: str
    types: tuple[PublicType, ...]
    methods: tuple[str, ...]
    fields: tuple[str, ...]


def normalize_ws(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def language_mode(file_path: str) -> str:
    suffix = Path(file_path).suffix
    if suffix in {".kt", ".kts"}:
        return "kotlin"
    if suffix in {".java", ".scala"}:
        return "explicit_public"
    return "explicit_public"


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


def parse_type_line(raw: str) -> tuple[str, str, str]:
    parts: dict[str, str] = {}
    for token in raw.split("|"):
        if "=" not in token:
            continue
        key, value = token.split("=", 1)
        parts[key.strip()] = value.strip()
    return (
        parts.get("type", "<unknown>"),
        parts.get("kind", "<unknown>"),
        parts.get("decl", ""),
    )


def parse_signature(sig_path: Path) -> SignatureRecord:
    file_path = "<unknown>"
    package = "<unknown>"
    public_types: list[PublicType] = []
    public_methods: list[str] = []
    public_fields: list[str] = []
    section: str | None = None
    mode = "explicit_public"

    for raw_line in sig_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line:
            continue
        if line.startswith("file="):
            file_path = line.split("=", 1)[1].strip()
            mode = language_mode(file_path)
            section = None
            continue
        if line.startswith("package="):
            package = line.split("=", 1)[1].strip()
            section = None
            continue
        if line.startswith("type="):
            fqcn, kind, decl = parse_type_line(line)
            if is_public_signature(decl, mode):
                public_types.append(
                    PublicType(
                        fqcn=fqcn,
                        kind=kind,
                        decl=normalize_ws(decl),
                        package=package,
                        file=file_path,
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
        if line.startswith("- ") and section == "methods":
            signature = normalize_ws(line[2:])
            if is_public_signature(signature, mode):
                public_methods.append(signature)
            continue
        if line.startswith("- ") and section == "fields":
            signature = normalize_ws(line[2:])
            if is_public_signature(signature, mode):
                public_fields.append(signature)
            continue
        section = None

    return SignatureRecord(
        file=file_path,
        package=package,
        types=tuple(dict.fromkeys(public_types)),
        methods=tuple(dict.fromkeys(public_methods)),
        fields=tuple(dict.fromkeys(public_fields)),
    )


def parse_index(index_path: Path) -> list[str]:
    entries: list[str] = []
    for raw_line in index_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line:
            continue
        relative_path = line.split("|", 1)[0].strip()
        if relative_path and relative_path != "INDEX.sig":
            entries.append(relative_path)
    return sorted(dict.fromkeys(entries))


def detect_signatures_dir(repo_root: Path, requested: str | None) -> Path:
    if requested:
        candidate = (repo_root / requested).resolve()
        if (candidate / "INDEX.sig").exists():
            return candidate
        raise FileNotFoundError(
            f"INDEX.sig not found in requested signatures directory: {candidate}"
        )

    candidate = (repo_root / "signatures").resolve()
    if (candidate / "INDEX.sig").exists():
        return candidate
    raise FileNotFoundError(
        "Could not find signatures/INDEX.sig. Generate signatures first."
    )


def render_context(
    repo_root: Path, signatures_dir: Path, records: list[SignatureRecord], max_members: int
) -> str:
    all_types = sorted(
        [public_type for record in records for public_type in record.types],
        key=lambda item: (item.fqcn, item.file, item.kind, item.decl),
    )

    module_counts: dict[str, int] = defaultdict(int)
    for public_type in all_types:
        module = public_type.file.split("/", 1)[0] if "/" in public_type.file else "<root>"
        module_counts[module] += 1

    lines: list[str] = [
        "context=public-surface-init",
        "version=1",
        "scope=public-surface-only",
        f"repository={repo_root.name}",
        f"source_signatures_dir={signatures_dir.relative_to(repo_root).as_posix()}",
        f"public_type_count={len(all_types)}",
        "",
        "modules:",
    ]

    for module, count in sorted(module_counts.items()):
        lines.append(f"- module={module}|public_types={count}")

    lines.extend(["", "types:"])
    for public_type in all_types:
        lines.append(
            "- type="
            f"{public_type.fqcn}|kind={public_type.kind}|file={public_type.file}"
            f"|package={public_type.package}|decl={public_type.decl}"
        )

    lines.extend(["", "file_members:"])
    for record in sorted(records, key=lambda item: item.file):
        if not record.types:
            continue
        method_preview = record.methods[:max_members]
        field_preview = record.fields[:max_members]
        lines.append(
            f"- file={record.file}|public_methods={len(record.methods)}"
            f"|public_fields={len(record.fields)}"
        )
        for method in method_preview:
            lines.append(f"  method={method}")
        for field in field_preview:
            lines.append(f"  field={field}")
        if len(record.methods) > max_members:
            lines.append(f"  method_truncated={len(record.methods) - max_members}")
        if len(record.fields) > max_members:
            lines.append(f"  field_truncated={len(record.fields) - max_members}")

    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", default=".", help="Repository root.")
    parser.add_argument(
        "--signatures-dir",
        default=None,
        help="Path to signatures directory relative to repo root. Defaults to signatures/.",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="Output context file path relative to repo root. Defaults to <signatures-dir>/PUBLIC_SURFACE.ctx.",
    )
    parser.add_argument(
        "--max-members-per-file",
        type=int,
        default=6,
        help="Maximum methods and fields to keep per file in the preview section.",
    )
    parser.add_argument(
        "--include-non-primary-sources",
        action="store_true",
        help="Include files outside src/main-style source sets.",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    signatures_dir = detect_signatures_dir(repo_root, args.signatures_dir)
    index_path = signatures_dir / "INDEX.sig"
    if not index_path.exists():
        raise FileNotFoundError(f"Missing index file: {index_path}")

    signature_paths = parse_index(index_path)
    records: list[SignatureRecord] = []
    for relative_path in signature_paths:
        sig_path = signatures_dir / relative_path
        if not sig_path.exists():
            continue
        record = parse_signature(sig_path)
        if not args.include_non_primary_sources and not is_primary_source(record.file):
            continue
        if not is_public_location(record.package, record.file):
            continue
        if record.types:
            records.append(record)

    output_path = (
        (repo_root / args.output).resolve()
        if args.output
        else signatures_dir / "PUBLIC_SURFACE.ctx"
    )
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        render_context(
            repo_root=repo_root,
            signatures_dir=signatures_dir,
            records=records,
            max_members=max(0, args.max_members_per_file),
        ),
        encoding="utf-8",
    )
    print(f"Wrote public-surface context: {output_path}")
    print(f"Public types: {sum(len(record.types) for record in records)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
