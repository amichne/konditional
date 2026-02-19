#!/usr/bin/env python3
"""Generate dense LLM-native signature docs in a mirrored signatures/ tree."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from dataclasses import dataclass
from pathlib import Path

EXCLUDED_DIRS = {
    ".beads",
    ".claude",
    ".codex",
    ".git",
    ".idea",
    ".gradle",
    ".mvn",
    ".signatures",
    ".worktrees",
    "__pycache__",
    "build",
    "out",
    "target",
    "node_modules",
    "dist",
    "signatures",
    "venv",
    ".venv",
}

JAVA_EXTENSIONS = {".java", ".kt", ".kts", ".scala"}


TYPE_PATTERN = re.compile(
    r"""
    ^
    [ \t]*
    (?:(?:public|protected|private|internal|sealed|final|abstract|open|data|value|inner|static|expect|actual|fun|annotation)[ \t]+)*
    (?P<kind>enum\s+class|annotation\s+class|class|interface|object|record|@interface)
    [ \t]+
    (?P<name>[A-Za-z_][A-Za-z0-9_]*)
    """,
    re.MULTILINE | re.VERBOSE,
)

KOTLIN_METHOD_START = re.compile(
    r"^(?:(?:public|protected|private|internal|open|final|abstract|override|suspend|inline|tailrec|operator|infix|external|actual|expect)\s+)*fun\b"
)

JAVA_METHOD_START = re.compile(
    r"^(?:(?:public|protected|private|static|final|abstract|synchronized|native|default|strictfp)\s+)+[A-Za-z_][A-Za-z0-9_<>,\[\]\.?\s@]*\s+[A-Za-z_][A-Za-z0-9_]*\s*\("
)

KOTLIN_FIELD_START = re.compile(
    r"^(?:(?:public|protected|private|internal|lateinit|const|override|open|final|abstract|volatile|transient)\s+)*(?:val|var)\b"
)

TYPE_START = re.compile(
    r"^(?:(?:public|protected|private|internal|sealed|final|abstract|open|data|value|inner|static|expect|actual|fun|annotation)\s+)*(?:enum\s+class|annotation\s+class|class|interface|object|record|@interface)\b"
)

JAVA_FIELD_START = re.compile(
    r"^(?:(?:public|protected|private|static|final|transient|volatile)\s+)+[A-Za-z_][A-Za-z0-9_<>,\[\]\.?\s@]*\s+[A-Za-z_][A-Za-z0-9_]*\s*(?:=|;)"
)

CONTROL_KEYWORDS = ("if ", "for ", "while ", "when ", "switch ", "catch ", "return ", "else ", "do ")


@dataclass(frozen=True)
class TypeBlock:
    kind: str
    name: str
    header: str
    open_brace: int | None
    close_brace: int | None


@dataclass(frozen=True)
class SignatureMetadata:
    sig_path: str
    source_file: str
    package: str
    type_count: int
    symbol_count: int
    sig_sha256: str


def normalize_ws(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def normalize_kind(kind: str) -> str:
    normalized = normalize_ws(kind)
    if normalized == "enum class":
        return "enum"
    if normalized in {"annotation class", "@interface"}:
        return "annotation"
    return normalized


def unique_stable(items: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for item in items:
        if item in seen:
            continue
        seen.add(item)
        result.append(item)
    return result


def count_balance_outside_strings(line: str, open_char: str, close_char: str) -> int:
    balance = 0
    in_single = False
    in_double = False
    escaped = False
    for ch in line:
        if escaped:
            escaped = False
            continue
        if ch == "\\" and (in_single or in_double):
            escaped = True
            continue
        if ch == "'" and not in_double:
            in_single = not in_single
            continue
        if ch == '"' and not in_single:
            in_double = not in_double
            continue
        if in_single or in_double:
            continue
        if ch == open_char:
            balance += 1
        elif ch == close_char:
            balance -= 1
    return balance


def strip_comments(line: str, in_block_comment: bool) -> tuple[str, bool]:
    out: list[str] = []
    i = 0
    while i < len(line):
        if in_block_comment:
            end = line.find("*/", i)
            if end == -1:
                return "", True
            i = end + 2
            in_block_comment = False
            continue
        if line.startswith("/*", i):
            in_block_comment = True
            i += 2
            continue
        if line.startswith("//", i):
            break
        out.append(line[i])
        i += 1
    return "".join(out), in_block_comment


def find_type_body_open(text: str, start: int, stop: int) -> int | None:
    paren_depth = 0
    angle_depth = 0
    in_single = False
    in_double = False
    escaped = False
    for idx in range(start, stop):
        ch = text[idx]
        if escaped:
            escaped = False
            continue
        if ch == "\\" and (in_single or in_double):
            escaped = True
            continue
        if ch == "'" and not in_double:
            in_single = not in_single
            continue
        if ch == '"' and not in_single:
            in_double = not in_double
            continue
        if in_single or in_double:
            continue
        if ch == "(":
            paren_depth += 1
            continue
        if ch == ")" and paren_depth > 0:
            paren_depth -= 1
            continue
        if ch == "<":
            angle_depth += 1
            continue
        if ch == ">" and angle_depth > 0:
            angle_depth -= 1
            continue
        if ch == "{" and paren_depth == 0 and angle_depth == 0:
            return idx
        if ch == "\n" and paren_depth == 0 and angle_depth == 0:
            lookahead = idx + 1
            while lookahead < stop and text[lookahead] in " \t":
                lookahead += 1
            if lookahead >= stop:
                return None
            if text.startswith("{", lookahead):
                return lookahead
            if text.startswith(":", lookahead) or text.startswith("where ", lookahead):
                continue
            return None
    return None


def find_matching_brace(text: str, open_brace: int) -> int | None:
    depth = 0
    in_single = False
    in_double = False
    escaped = False
    for idx in range(open_brace, len(text)):
        ch = text[idx]
        if escaped:
            escaped = False
            continue
        if ch == "\\" and (in_single or in_double):
            escaped = True
            continue
        if ch == "'" and not in_double:
            in_single = not in_single
            continue
        if ch == '"' and not in_single:
            in_double = not in_double
            continue
        if in_single or in_double:
            continue
        if ch == "{":
            depth += 1
            continue
        if ch == "}":
            depth -= 1
            if depth == 0:
                return idx
    return None


def find_type_declaration_end(text: str, start: int, stop: int) -> int:
    paren_depth = 0
    angle_depth = 0
    in_single = False
    in_double = False
    escaped = False
    for idx in range(start, stop):
        ch = text[idx]
        if escaped:
            escaped = False
            continue
        if ch == "\\" and (in_single or in_double):
            escaped = True
            continue
        if ch == "'" and not in_double:
            in_single = not in_single
            continue
        if ch == '"' and not in_single:
            in_double = not in_double
            continue
        if in_single or in_double:
            continue
        if ch == "(":
            paren_depth += 1
            continue
        if ch == ")" and paren_depth > 0:
            paren_depth -= 1
            continue
        if ch == "<":
            angle_depth += 1
            continue
        if ch == ">" and angle_depth > 0:
            angle_depth -= 1
            continue
        if ch == "\n" and paren_depth == 0 and angle_depth == 0:
            lookahead = idx + 1
            while lookahead < stop and text[lookahead] in " \t":
                lookahead += 1
            if lookahead >= stop:
                return idx
            if text.startswith(":", lookahead) or text.startswith("where ", lookahead):
                continue
            return idx
    return stop


def parse_types(text: str) -> list[TypeBlock]:
    matches = list(TYPE_PATTERN.finditer(text))
    types: list[TypeBlock] = []
    for idx, match in enumerate(matches):
        next_start = matches[idx + 1].start() if idx + 1 < len(matches) else len(text)
        open_brace = find_type_body_open(text, match.end(), next_start)
        close_brace = find_matching_brace(text, open_brace) if open_brace is not None else None
        header_end = open_brace if open_brace is not None else find_type_declaration_end(text, match.end(), next_start)
        header = normalize_ws(text[match.start() : header_end])
        types.append(
            TypeBlock(
                kind=normalize_kind(match.group("kind")),
                name=match.group("name"),
                header=header,
                open_brace=open_brace,
                close_brace=close_brace,
            )
        )
    return types


def starts_control_flow(line: str) -> bool:
    return any(line.startswith(prefix) for prefix in CONTROL_KEYWORDS)


def looks_like_method_start(line: str) -> bool:
    if starts_control_flow(line):
        return False
    return bool(KOTLIN_METHOD_START.match(line) or JAVA_METHOD_START.match(line))


def looks_like_field_start(line: str) -> bool:
    if starts_control_flow(line):
        return False
    if KOTLIN_FIELD_START.match(line):
        return True
    if JAVA_FIELD_START.match(line):
        return "(" not in line.split("=", 1)[0]
    return False


def declaration_complete(kind: str, line: str, paren_depth: int) -> bool:
    if kind == "method":
        if paren_depth > 0:
            return False
        return "{" in line or "=" in line or ";" in line
    if ";" in line:
        return True
    if "=" in line and paren_depth <= 0:
        return True
    return paren_depth <= 0 and not line.endswith(",")


def truncate_top_level_equals(signature: str) -> str:
    paren_depth = 0
    angle_depth = 0
    in_single = False
    in_double = False
    escaped = False
    for idx, ch in enumerate(signature):
        if escaped:
            escaped = False
            continue
        if ch == "\\" and (in_single or in_double):
            escaped = True
            continue
        if ch == "'" and not in_double:
            in_single = not in_single
            continue
        if ch == '"' and not in_single:
            in_double = not in_double
            continue
        if in_single or in_double:
            continue
        if ch == "(":
            paren_depth += 1
            continue
        if ch == ")" and paren_depth > 0:
            paren_depth -= 1
            continue
        if ch == "<":
            angle_depth += 1
            continue
        if ch == ">" and angle_depth > 0:
            angle_depth -= 1
            continue
        if ch == "=" and paren_depth == 0 and angle_depth == 0:
            return signature[:idx]
    return signature


def clean_member_signature(signature: str) -> str:
    normalized = normalize_ws(truncate_top_level_equals(signature))
    normalized = re.sub(r"\s*(?:\{|=|;)\s*$", "", normalized)
    normalized = normalized.rstrip(",")
    return normalized


def extract_members_from_body(body: str) -> tuple[list[str], list[str]]:
    fields: list[str] = []
    methods: list[str] = []
    brace_depth = 0
    pending_kind: str | None = None
    pending_parts: list[str] = []
    pending_paren_depth = 0
    skipping_type_header = False
    type_header_paren_depth = 0
    in_block_comment = False

    for raw_line in body.splitlines():
        line, in_block_comment = strip_comments(raw_line, in_block_comment)
        stripped = line.strip()

        if skipping_type_header:
            if stripped:
                type_header_paren_depth += count_balance_outside_strings(stripped, "(", ")")
                if "{" in stripped and type_header_paren_depth <= 0:
                    skipping_type_header = False
                elif (
                    type_header_paren_depth <= 0
                    and not stripped.endswith(",")
                    and not stripped.startswith(":")
                    and not stripped.startswith("where ")
                ):
                    skipping_type_header = False
        elif pending_kind:
            if stripped:
                pending_parts.append(stripped)
                pending_paren_depth += count_balance_outside_strings(stripped, "(", ")")
            if declaration_complete(pending_kind, stripped, pending_paren_depth):
                cleaned = clean_member_signature(" ".join(pending_parts))
                if cleaned:
                    if pending_kind == "field":
                        fields.append(cleaned)
                    else:
                        methods.append(cleaned)
                pending_kind = None
                pending_parts = []
                pending_paren_depth = 0
        elif brace_depth == 0 and stripped and not stripped.startswith("@"):
            if TYPE_START.match(stripped):
                skipping_type_header = True
                type_header_paren_depth = count_balance_outside_strings(stripped, "(", ")")
                if "{" in stripped and type_header_paren_depth <= 0:
                    skipping_type_header = False
            elif looks_like_method_start(stripped):
                pending_kind = "method"
                pending_parts = [stripped]
                pending_paren_depth = count_balance_outside_strings(stripped, "(", ")")
                if declaration_complete("method", stripped, pending_paren_depth):
                    cleaned = clean_member_signature(" ".join(pending_parts))
                    if cleaned:
                        methods.append(cleaned)
                    pending_kind = None
                    pending_parts = []
                    pending_paren_depth = 0
            elif looks_like_field_start(stripped):
                pending_kind = "field"
                pending_parts = [stripped]
                pending_paren_depth = count_balance_outside_strings(stripped, "(", ")")
                if declaration_complete("field", stripped, pending_paren_depth):
                    cleaned = clean_member_signature(" ".join(pending_parts))
                    if cleaned:
                        fields.append(cleaned)
                    pending_kind = None
                    pending_parts = []
                    pending_paren_depth = 0

        brace_depth += count_balance_outside_strings(line, "{", "}")

    return unique_stable(fields), unique_stable(methods)


def parse_types_and_members(text: str) -> tuple[list[TypeBlock], list[str], list[str]]:
    types = parse_types(text)
    fields: list[str] = []
    methods: list[str] = []
    for type_block in types:
        if type_block.open_brace is None or type_block.close_brace is None:
            continue
        body = text[type_block.open_brace + 1 : type_block.close_brace]
        type_fields, type_methods = extract_members_from_body(body)
        fields.extend(type_fields)
        methods.extend(type_methods)
    return types, methods, fields


def parse_file(path: Path, repo_root: Path) -> str:
    text = path.read_text(encoding="utf-8", errors="ignore")

    package_match = re.search(r"(?m)^\s*package\s+([A-Za-z0-9_\.]+)", text)
    package_name = package_match.group(1) if package_match else ""

    imports = re.findall(r"(?m)^\s*import\s+([A-Za-z0-9_\.\*]+)", text)
    types, methods, fields = parse_types_and_members(text)

    lines: list[str] = []
    lines.append(f"file={path.relative_to(repo_root).as_posix()}")
    lines.append(f"package={package_name or '<default>'}")
    if imports:
        lines.append("imports=" + ",".join(sorted(set(imports))))

    if not types:
        lines.append("types=<none>")
        return "\n".join(lines) + "\n"

    for t in types:
        fqcn = f"{package_name}.{t.name}" if package_name else t.name
        lines.append(f"type={fqcn}|kind={t.kind}|decl={t.header}")

    if fields:
        lines.append("fields:")
        for field in fields:
            lines.append(f"- {field}")

    if methods:
        lines.append("methods:")
        for method in methods:
            lines.append(f"- {method}")

    return "\n".join(lines) + "\n"


def candidate_files(root: Path, output_root: Path) -> list[Path]:
    files: list[Path] = []
    for path in root.rglob("*"):
        if not path.is_file() or path.suffix not in JAVA_EXTENSIONS:
            continue
        relative_parts = path.relative_to(root).parts
        if any(part in EXCLUDED_DIRS for part in relative_parts):
            continue
        if output_root in path.parents:
            continue
        files.append(path)
    return sorted(files)


def write_signature_file(repo_root: Path, src: Path, output_root: Path) -> None:
    relative = src.relative_to(repo_root)
    out_path = output_root / relative.parent / f"{relative.name}.sig"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(parse_file(src, repo_root), encoding="utf-8")


def clear_previous_signatures(output_root: Path) -> None:
    for path in output_root.rglob("*.sig"):
        path.unlink()
    meta_index = output_root / "INDEX.meta.json"
    if meta_index.exists():
        meta_index.unlink()


def signature_metadata(sig_path: Path, output_root: Path) -> SignatureMetadata:
    source_file = "<unknown>"
    package = "<unknown>"
    type_count = 0
    field_count = 0
    method_count = 0
    section: str | None = None

    contents = sig_path.read_text(encoding="utf-8", errors="ignore")
    for raw_line in contents.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        if line.startswith("file="):
            source_file = line.split("=", 1)[1].strip()
            section = None
            continue
        if line.startswith("package="):
            package = line.split("=", 1)[1].strip()
            section = None
            continue
        if line.startswith("type="):
            type_count += 1
            section = None
            continue
        if line == "fields:":
            section = "fields"
            continue
        if line == "methods:":
            section = "methods"
            continue
        if line.startswith("- ") and section == "fields":
            field_count += 1
            continue
        if line.startswith("- ") and section == "methods":
            method_count += 1
            continue
        section = None

    return SignatureMetadata(
        sig_path=sig_path.relative_to(output_root).as_posix(),
        source_file=source_file,
        package=package,
        type_count=type_count,
        symbol_count=type_count + field_count + method_count,
        sig_sha256=hashlib.sha256(contents.encode("utf-8")).hexdigest(),
    )


def build_indexes(output_root: Path) -> None:
    entries: list[str] = []
    metadata: list[SignatureMetadata] = []
    for path in sorted(output_root.rglob("*.sig")):
        if path.name == "INDEX.sig":
            continue
        rel = path.relative_to(output_root)
        entries.append(rel.as_posix())
        metadata.append(signature_metadata(path, output_root))

    index = output_root / "INDEX.sig"
    index.write_text("\n".join(entries) + ("\n" if entries else ""), encoding="utf-8")
    meta_index = output_root / "INDEX.meta.json"
    meta_index.write_text(
        json.dumps(
            {
                "version": 1,
                "entries": [
                    {
                        "sig_path": entry.sig_path,
                        "source_file": entry.source_file,
                        "package": entry.package,
                        "type_count": entry.type_count,
                        "symbol_count": entry.symbol_count,
                        "sig_sha256": entry.sig_sha256,
                    }
                    for entry in metadata
                ],
            },
            indent=2,
            sort_keys=True,
        )
        + "\n",
        encoding="utf-8",
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", default=".", help="Repository root to scan.")
    parser.add_argument(
        "--output-dir",
        default="signatures",
        help="Output directory for generated signature files (mirrors source layout).",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    output_root = (repo_root / args.output_dir).resolve()
    output_root.mkdir(parents=True, exist_ok=True)
    clear_previous_signatures(output_root)

    files = candidate_files(repo_root, output_root)
    for src in files:
        write_signature_file(repo_root, src, output_root)

    build_indexes(output_root)
    print(f"Generated {len(files)} signature files in {output_root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
