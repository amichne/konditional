#!/usr/bin/env python3
"""
sig_reachability.py — Dead-code surface scanner over .signatures/

Builds a reference graph from .sig files and reports types that are defined in
main sources but unreachable from other main sources. Three severity tiers:

  ORPHANED    — 0 refs anywhere in the project (candidates for deletion)
  TEST-ONLY   — referenced only from test sources (dead in production)
  LOW USAGE   — 1–2 main-source refs (worth a second look)

API surface modules (openfeature, kontracts, konditional-http-server,
konditional-otel) are reported separately; their public types are intended to
be consumed externally and will naturally have 0 internal refs.

Usage (standalone):
    python3 sig_reachability.py [--sig-dir .signatures] [--report-file -]

Usage (hook dispatch — reads stdin, exits immediately):
    python3 sig_reachability.py --hook-mode
"""

import argparse
import re
import subprocess
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path

PROJECT_PREFIX = "io.amichne"

# Modules whose public types are designed to be consumed externally.
# Orphaned public types here are reported as API surface, not dead code.
API_SURFACE_MODULES = {
    "openfeature",
    "kontracts",
    "konditional-http-server",
    "konditional-otel",
}


# ---------------------------------------------------------------------------
# Parsing
# ---------------------------------------------------------------------------


def _is_main(path: Path) -> bool:
    s = path.as_posix()
    return "/src/main/" in s


def _is_test(path: Path) -> bool:
    s = path.as_posix()
    return "/src/test/" in s


def _module_of(path: Path, sig_dir: Path) -> str:
    return path.relative_to(sig_dir).parts[0]


def _detect_visibility(decl: str) -> str:
    """Infer Kotlin visibility from the declaration string."""
    # Strip leading annotations/whitespace before checking modifier
    stripped = re.sub(r'@\w+\s*', '', decl).strip()
    if re.match(r'private\b', stripped):
        return "private"
    if re.match(r'internal\b', stripped):
        return "internal"
    return "public"


@dataclass
class TypeEntry:
    fqcn: str
    sig_file: Path
    module: str
    visibility: str   # "public" | "internal" | "private"
    kind: str
    is_api_surface: bool = False
    # All FQCNs that refer to this type (primary + nested-class aliases).
    # e.g. primary "io.amichne.x.Foo", alias "io.amichne.x.Context.Foo"
    aliases: list[str] = field(default_factory=list)
    # FQCNs of supertypes/interfaces this type directly implements or extends,
    # resolved by the generator using the import map + same-package fallback.
    supertypes: list[str] = field(default_factory=list)


@dataclass
class SigFile:
    path: Path
    module: str
    is_main: bool
    is_test: bool
    imports: list[str] = field(default_factory=list)
    defined_types: list[TypeEntry] = field(default_factory=list)
    raw_content: str = ""


def parse_sig_file(path: Path, sig_dir: Path) -> SigFile:
    content = path.read_text(encoding="utf-8", errors="replace")
    module = _module_of(path, sig_dir)
    sf = SigFile(
        path=path,
        module=module,
        is_main=_is_main(path),
        is_test=_is_test(path),
        raw_content=content,
    )

    # Extract the file stem from the "file=" line so we can generate
    # nested-class aliases.  e.g. "Context.kt" → stem "Context".
    file_stem: str = ""
    for line in content.splitlines():
        if line.startswith("file="):
            raw_file = line[len("file="):].strip()
            file_stem = Path(raw_file).stem  # "Context.kt" → "Context"
            break

    for line in content.splitlines():
        if line.startswith("imports="):
            raw = line[len("imports="):].strip()
            sf.imports = [
                i.strip()
                for i in raw.split(",")
                if i.strip().startswith(PROJECT_PREFIX)
            ]
        elif line.startswith("type="):
            # Strip optional |supertypes=... suffix before applying the main
            # regex so that decl= captures only the declaration text.
            supertypes_raw = ""
            type_line = line
            st_match = re.search(r"\|supertypes=(.+)$", line)
            if st_match:
                supertypes_raw = st_match.group(1)
                type_line = line[: st_match.start()]

            # type=<FQCN>|kind=<kind>|decl=<decl>
            m = re.match(r"type=([^|]+)\|kind=([^|]+)\|decl=(.*)", type_line)
            if m:
                fqcn = m.group(1).strip()
                kind = m.group(2).strip()
                decl = m.group(3).strip()
                visibility = _detect_visibility(decl)

                # Resolved supertype FQCNs emitted by the generator.
                # We filter to project prefix here; non-project supertypes
                # (e.g. kotlin.Enum, dev.openfeature.*) are irrelevant.
                supertypes = [
                    s.strip()
                    for s in supertypes_raw.split(",")
                    if s.strip().startswith(PROJECT_PREFIX)
                ] if supertypes_raw else []

                # Compute nested-class alias: if this type is defined inside
                # a file whose stem differs from its own simple name, other
                # files will import it as "{package}.{FileStem}.{SimpleName}"
                # (e.g. Context.StableIdContext) rather than the bare FQCN.
                simple_name = fqcn.rsplit(".", 1)[-1]
                package = fqcn.rsplit(".", 1)[0] if "." in fqcn else ""
                aliases: list[str] = []
                if file_stem and file_stem != simple_name and package:
                    aliases.append(f"{package}.{file_stem}.{simple_name}")

                sf.defined_types.append(TypeEntry(
                    fqcn=fqcn,
                    sig_file=path,
                    module=module,
                    visibility=visibility,
                    kind=kind,
                    is_api_surface=module in API_SURFACE_MODULES,
                    aliases=aliases,
                    supertypes=supertypes,
                ))

    return sf


# ---------------------------------------------------------------------------
# Exclusions
# ---------------------------------------------------------------------------


def load_exclusions(exclusions_file: Path) -> tuple[set[str], list[str]]:
    """
    Parse the exclusions file and return (file_exclusions, package_prefixes).

    file_exclusions:  set of source-file paths relative to project root.
                      e.g. "konditional-core/src/main/kotlin/.../Foo.kt"
    package_prefixes: list of package prefix strings.
                      e.g. "io.amichne.konditional.context"

    Lines containing "/" are treated as file paths; dotted identifiers starting
    with a capital-free token are treated as package prefixes.
    Lines starting with "#" and blank lines are ignored.
    """
    if not exclusions_file.exists():
        return set(), []

    file_excl: set[str] = set()
    pkg_excl: list[str] = []
    for line in exclusions_file.read_text(encoding="utf-8").splitlines():
        entry = line.strip()
        if not entry or entry.startswith("#"):
            continue
        if "/" in entry or entry.endswith(".kt"):
            file_excl.add(entry)
        else:
            pkg_excl.append(entry)
    return file_excl, pkg_excl


def remove_file_exclusion(exclusions_file: Path, project_root: Path, edited_abs: str) -> None:
    """
    Remove any file-path exclusion entry that matches `edited_abs` (absolute
    path of the just-edited Kotlin file).  Called from hook_mode() before
    dispatching background analysis so that the next report includes the file.
    """
    if not exclusions_file.exists():
        return
    try:
        rel = str(Path(edited_abs).relative_to(project_root))
    except ValueError:
        return

    original = exclusions_file.read_text(encoding="utf-8")
    kept = []
    changed = False
    for line in original.splitlines(keepends=True):
        entry = line.strip()
        if not entry.startswith("#") and ("/" in entry or entry.endswith(".kt")) and entry == rel:
            changed = True
            continue  # drop this line
        kept.append(line)
    if changed:
        exclusions_file.write_text("".join(kept), encoding="utf-8")


# ---------------------------------------------------------------------------
# Reference extraction
# ---------------------------------------------------------------------------


def _refs_from_content(content: str, known_fqcns: set[str]) -> set[str]:
    """
    Scan raw sig content for known project FQCNs.

    We do a substring check (O(n*m) but fast enough for ~270 sig files and
    ~300 known types). This catches FQCNs embedded in decl strings, method
    signatures, and field declarations that may not appear in imports.
    """
    found = set()
    for fqcn in known_fqcns:
        if fqcn in content:
            found.add(fqcn)
    return found


# ---------------------------------------------------------------------------
# Kt-file word index (fallback for sig-invisible references)
# ---------------------------------------------------------------------------


def _build_kt_import_index(
    project_root: Path, sig_dir: Path
) -> tuple[dict[str, set[Path]], dict[str, set[Path]]]:
    """
    Scan every .kt file under project_root and index their import statements.

    Returns:
        exact_imports : fqcn    → set of .kt Paths with `import <fqcn>`
        star_imports  : package → set of .kt Paths with `import <package>.*`

    Because detekt rejects unused imports, every entry here is a guaranteed
    live reference.  This gives us a zero-false-positive rescue pass for types
    that look orphaned in the sig graph — the sig scanner can miss references
    that appear in generated code, annotation processors, or import aliases.
    """
    _IMPORT = re.compile(r'^import\s+([\w.]+?)(\.\*)?[ \t]*$', re.MULTILINE)
    exact: dict[str, set[Path]] = defaultdict(set)
    star: dict[str, set[Path]] = defaultdict(set)

    for kt_file in project_root.rglob("*.kt"):
        try:
            kt_file.relative_to(sig_dir)
            continue  # inside sig_dir — skip
        except ValueError:
            pass
        try:
            content = kt_file.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        for m in _IMPORT.finditer(content):
            target = m.group(1)
            if m.group(2):   # ends with .*
                star[target].add(kt_file)
            else:
                exact[target].add(kt_file)

    return exact, star


# ---------------------------------------------------------------------------
# Analysis
# ---------------------------------------------------------------------------


def _package_of(content: str) -> str:
    """Extract the 'package=' value from raw sig content."""
    for line in content.splitlines():
        if line.startswith("package="):
            return line[len("package="):].strip()
    return ""


def analyse(sig_dir: Path, exclusions_file: Path | None = None) -> str:
    all_sigs = list(sig_dir.rglob("*.sig"))
    if not all_sigs:
        return f"No .sig files found under {sig_dir}"

    project_root = sig_dir.parent
    file_excl, pkg_excl = load_exclusions(exclusions_file) if exclusions_file else (set(), [])

    def _is_excluded(entry: TypeEntry) -> bool:
        rel_sig = entry.sig_file.relative_to(sig_dir)
        src_rel = str(rel_sig.with_suffix(""))   # strip .sig → Foo.kt
        if src_rel in file_excl:
            return True
        pkg = _package_of(entry.sig_file.read_text(encoding="utf-8", errors="replace"))
        return any(pkg == p or pkg.startswith(p + ".") for p in pkg_excl)

    parsed: list[SigFile] = [parse_sig_file(p, sig_dir) for p in all_sigs]

    main_sigs = [sf for sf in parsed if sf.is_main]
    test_sigs = [sf for sf in parsed if sf.is_test]

    # Collect all non-private types from main sources.
    # Register both the primary FQCN and any nested-class aliases so that
    # references like "Context.StableIdContext" resolve to the same TypeEntry.
    all_main_types: dict[str, TypeEntry] = {}   # fqcn (or alias) → TypeEntry
    canonical_fqcns: set[str] = set()           # only primary FQCNs
    for sf in main_sigs:
        for t in sf.defined_types:
            if t.visibility != "private":
                all_main_types[t.fqcn] = t
                canonical_fqcns.add(t.fqcn)
                for alias in t.aliases:
                    all_main_types[alias] = t   # alias → same TypeEntry

    known_fqcns = set(all_main_types)

    # Index: (module, package) → list of SigFile, for same-package implicit ref resolution
    module_package_sigs: dict[tuple[str, str], list[SigFile]] = defaultdict(list)
    for sf in main_sigs:
        pkg = _package_of(sf.raw_content)
        module_package_sigs[(sf.module, pkg)].append(sf)

    # Build inbound ref counts
    # main_refs[fqcn] = set of sig file paths (in main sources) that reference fqcn
    main_refs: dict[str, set[str]] = defaultdict(set)
    test_refs: dict[str, set[str]] = defaultdict(set)

    for sf in main_sigs:
        refs = (set(sf.imports) & known_fqcns) | _refs_from_content(sf.raw_content, known_fqcns)
        for ref in refs:
            t = all_main_types.get(ref)
            if t is None or t.sig_file == sf.path:
                continue  # skip missing / self-reference
            # Internal types only count refs from within the same module
            if t.visibility == "internal" and t.module != sf.module:
                continue
            # Always accumulate under the canonical FQCN, not the alias
            main_refs[t.fqcn].add(sf.path.as_posix())

    for sf in test_sigs:
        refs = (set(sf.imports) & known_fqcns) | _refs_from_content(sf.raw_content, known_fqcns)
        for ref in refs:
            t = all_main_types.get(ref)
            if t is not None:
                test_refs[t.fqcn].add(sf.path.as_posix())

    # Supertype-based inbound refs: if type A implements/extends type B,
    # count it as B having an inbound reference from A.
    #
    # Unlike the import/content scan, we do NOT skip same-file references here.
    # If LocaleContext and Core are both in Context.kt, Core implementing
    # LocaleContext is a legitimate use of LocaleContext.
    for sf in main_sigs:
        for te in sf.defined_types:
            if te.visibility == "private":
                continue
            for st_fqcn in te.supertypes:
                # Supertypes may also appear under an alias key
                st_entry = all_main_types.get(st_fqcn)
                if st_entry is None:
                    continue
                if st_entry.visibility == "internal" and st_entry.module != sf.module:
                    continue
                # Key includes the implementing FQCN so one file with multiple
                # implementors still counts each separately.
                main_refs[st_entry.fqcn].add(f"supertype:{te.fqcn}")

    # Build same-package index: (module, package) → set of simple names mentioned
    # in sibling files' raw content.  Used to rescue internal types that are
    # referenced without an import (same-package references in Kotlin need no import).
    same_package_simple_names: dict[tuple[str, str], set[str]] = {}

    # Classify — iterate over canonical FQCNs only (skip aliases)
    orphaned_internal: list[tuple] = []  # dead, no API surface excuse
    orphaned_api_surface: list[tuple] = []  # may be external entry points
    test_only: list[tuple] = []
    low_usage: list[tuple] = []

    for fqcn, entry in sorted(
        ((k, v) for k, v in all_main_types.items() if k in canonical_fqcns),
        key=lambda kv: kv[0],
    ):
        if _is_excluded(entry):
            continue  # assume fully-used until the file is next edited

        m_count = len(main_refs.get(fqcn, set()))

        # For internal types with 0 explicit refs: check if the simple name
        # appears in any sibling file within the same (module, package).
        # Same-package usage in Kotlin never generates an import statement,
        # so explicit ref counts will be 0 even when the type is actively used.
        if m_count == 0 and entry.visibility == "internal":
            pkg = _package_of(entry.sig_file.read_text(encoding="utf-8", errors="replace"))
            key = (entry.module, pkg)
            if key not in same_package_simple_names:
                combined = ""
                for sibling in module_package_sigs.get(key, []):
                    if sibling.path != entry.sig_file:
                        combined += sibling.raw_content
                same_package_simple_names[key] = set()
                # We want whole-word matches to avoid "Int" matching "IntFeature"
                for word in re.findall(r'\b[A-Z][A-Za-z0-9_]*\b', combined):
                    same_package_simple_names[key].add(word)
            simple_name = fqcn.rsplit(".", 1)[-1]
            if simple_name in same_package_simple_names.get(key, set()):
                m_count = 1  # treat as implicitly referenced; promote out of ORPHANED

        # An internal type that directly implements a public project interface
        # is an implementation detail of that interface — not dead code.
        # e.g. internal class BooleanFeatureImpl : BooleanFeature<...>
        if m_count == 0 and entry.visibility == "internal":
            for st_fqcn in entry.supertypes:
                st = all_main_types.get(st_fqcn)
                if st is not None and st.fqcn in canonical_fqcns and st.visibility == "public":
                    m_count = 1
                    break

        t_count = len(test_refs.get(fqcn, set()))

        if m_count == 0 and t_count == 0:
            if entry.is_api_surface and entry.visibility == "public":
                orphaned_api_surface.append((fqcn, entry, m_count, t_count))
            else:
                orphaned_internal.append((fqcn, entry, m_count, t_count))
        elif m_count == 0 and t_count > 0:
            test_only.append((fqcn, entry, m_count, t_count))
        elif 1 <= m_count <= 2:
            low_usage.append((fqcn, entry, m_count, t_count))

    # -----------------------------------------------------------------------
    # Kt-file rescue pass
    # For each ORPHANED candidate, do a word-boundary regex search across all
    # real .kt files in the project.  If the simple name appears in any file
    # other than the type's own source file, the sig scanner simply missed the
    # reference — promote to "KT-REF" (sig-invisible) rather than ORPHANED.
    # -----------------------------------------------------------------------

    def _source_path(entry: TypeEntry) -> Path:
        rel = entry.sig_file.relative_to(sig_dir)
        return sig_dir.parent / rel.with_suffix("")  # Foo.kt.sig → Foo.kt

    kt_referenced: list[tuple] = []
    true_orphaned: list[tuple] = []

    if orphaned_internal:
        exact_imports, star_imports = _build_kt_import_index(project_root, sig_dir)
        for item in orphaned_internal:
            fqcn, entry, m, t = item
            own_src = _source_path(entry)
            package = fqcn.rsplit(".", 1)[0] if "." in fqcn else ""

            # Gather every kt file that provably references this type:
            #   - exact import of the canonical FQCN or any nested-class alias
            #   - star import of the type's package
            ref_files: set[Path] = set()
            for key in [fqcn] + entry.aliases:
                ref_files |= exact_imports.get(key, set())
            if package:
                ref_files |= star_imports.get(package, set())
            ref_files.discard(own_src)

            if ref_files:
                kt_referenced.append(item)
            else:
                true_orphaned.append(item)
        orphaned_internal = true_orphaned

    # -----------------------------------------------------------------------
    # Format report — simple styled HTML, grouped by module
    # -----------------------------------------------------------------------

    TIER_ORDER = {"ORPHANED": 0, "KT-REF": 1, "TEST-ONLY": 2, "LOW-USAGE": 3, "API-SURFACE": 4}
    TIER_STYLE = {
        "ORPHANED":    ("Orphaned",       "#b91c1c", "#fef2f2"),
        "KT-REF":      ("Sig-invisible",  "#374151", "#f3f4f6"),
        "TEST-ONLY":   ("Test-only",      "#92400e", "#fffbeb"),
        "LOW-USAGE":   ("Low usage",      "#7c3aed", "#f5f3ff"),
        "API-SURFACE": ("API surface",    "#1d4ed8", "#eff6ff"),
    }

    def source_path(entry: TypeEntry) -> Path:
        """Strip .sig to get the actual Kotlin source file path."""
        return _source_path(entry)

    def badge(tier: str) -> str:
        label, color, _ = TIER_STYLE[tier]
        return (
            f'<span style="background:{color};color:#fff;padding:2px 8px;'
            f'border-radius:12px;font-size:11px;font-weight:600;white-space:nowrap">'
            f'{label}</span>'
        )

    def file_link(path: Path, display: str) -> str:
        return f'<a href="file://{path}" style="color:#0550ae;text-decoration:none;font-family:monospace">{display}</a>'

    # Collect all findings into (tier, fqcn, entry, m, t) tuples
    all_findings: list[tuple[str, str, TypeEntry, int, int]] = [
        ("ORPHANED",    *item) for item in orphaned_internal
    ] + [
        ("KT-REF",      *item) for item in kt_referenced
    ] + [
        ("TEST-ONLY",   *item) for item in test_only
    ] + [
        ("LOW-USAGE",   *item) for item in low_usage
    ] + [
        ("API-SURFACE", *item) for item in orphaned_api_surface
    ]

    # Group by module
    by_module: dict[str, list[tuple[str, str, TypeEntry, int, int]]] = defaultdict(list)
    for finding in all_findings:
        by_module[finding[2].module].append(finding)

    healthy_count = len(canonical_fqcns) - len(all_findings)

    from datetime import datetime, timezone
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    html: list[str] = []
    html.append("<!DOCTYPE html>")
    html.append('<html lang="en">')
    html.append("<head>")
    html.append('<meta charset="UTF-8">')
    html.append('<meta name="viewport" content="width=device-width, initial-scale=1">')
    html.append("<title>sig-reachability — dead-code surface report</title>")
    html.append("<style>")
    html.append("""
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
         font-size: 14px; color: #1f2328; background: #ffffff; margin: 0; padding: 24px; }
  h1   { font-size: 20px; font-weight: 600; margin: 0 0 4px; }
  .meta { color: #656d76; font-size: 12px; margin-bottom: 20px; }
  h2   { font-size: 15px; font-weight: 600; margin: 28px 0 8px;
         padding-bottom: 6px; border-bottom: 1px solid #d0d7de; }
  table { border-collapse: collapse; width: 100%; margin-bottom: 8px; }
  th   { background: #f6f8fa; text-align: left; padding: 6px 12px;
         font-size: 12px; font-weight: 600; color: #656d76;
         border: 1px solid #d0d7de; white-space: nowrap; }
  td   { padding: 6px 12px; border: 1px solid #d0d7de; vertical-align: middle; }
  tr:nth-child(even) td { background: #f6f8fa; }
  td.num { text-align: right; font-variant-numeric: tabular-nums; }
  .summary-table { width: auto; margin-bottom: 24px; }
  .summary-table td { padding: 4px 16px 4px 0; border: none; background: none; }
  .summary-table tr:nth-child(even) td { background: none; }
  code { font-family: 'SFMono-Regular', Consolas, monospace; font-size: 12px; }
""")
    html.append("</style>")
    html.append("</head>")
    html.append("<body>")
    html.append("<h1>sig-reachability — dead-code surface report</h1>")
    html.append(f'<div class="meta">Generated {generated_at} &nbsp;·&nbsp; '
                f'{len(main_sigs)} main sig files &nbsp;·&nbsp; {len(test_sigs)} test sig files</div>')

    # Summary stats
    html.append('<table class="summary-table">')
    html.append("<tbody>")

    def stat_row(label: str, value: str) -> str:
        return f"<tr><td>{label}</td><td><strong>{value}</strong></td></tr>"

    html.append(stat_row("Non-private types", str(len(canonical_fqcns))))
    html.append(stat_row('<span style="color:#b91c1c">&#9679;</span> Orphaned', str(len(orphaned_internal))))
    html.append(stat_row('<span style="color:#374151">&#9679;</span> Sig-invisible', str(len(kt_referenced))))
    html.append(stat_row('<span style="color:#92400e">&#9679;</span> Test-only', str(len(test_only))))
    html.append(stat_row('<span style="color:#7c3aed">&#9679;</span> Low usage', str(len(low_usage))))
    html.append(stat_row('<span style="color:#1d4ed8">&#9679;</span> API surface', str(len(orphaned_api_surface))))
    html.append(stat_row('<span style="color:#15803d">&#9679;</span> Healthy', str(healthy_count)))
    html.append("</tbody></table>")

    if not all_findings:
        html.append("<p>No findings — all types are healthy.</p>")
    else:
        for module in sorted(by_module):
            findings = by_module[module]
            findings.sort(key=lambda f: (TIER_ORDER[f[0]], f[1]))

            html.append(f"<h2>{module}</h2>")
            html.append("<table>")
            html.append("<thead><tr>")
            for col in ("Tier", "Type", "Kind", "Visibility", "File", "main", "test"):
                html.append(f"<th>{col}</th>")
            html.append("</tr></thead>")
            html.append("<tbody>")

            for tier, fqcn, entry, m, t in findings:
                simple = fqcn.rsplit(".", 1)[-1]
                src = source_path(entry)
                html.append("<tr>")
                html.append(f"<td>{badge(tier)}</td>")
                html.append(f"<td>{file_link(src, simple)}</td>")
                html.append(f"<td><code>{entry.kind}</code></td>")
                html.append(f"<td>{entry.visibility}</td>")
                html.append(f"<td>{file_link(src, src.name)}</td>")
                html.append(f'<td class="num">{m}</td>')
                html.append(f'<td class="num">{t}</td>')
                html.append("</tr>")

            html.append("</tbody></table>")

    html.append("</body></html>")
    return "\n".join(html)


# ---------------------------------------------------------------------------
# Hook mode — reads stdin (required by Claude Code hook protocol), then
# dispatches the full analysis as a detached subprocess so the hook returns
# immediately without blocking Claude.
# ---------------------------------------------------------------------------


def hook_mode() -> None:
    import json
    import os

    data = json.load(sys.stdin)
    tool_input = data.get("tool_input", {})
    file_path = tool_input.get("file_path", "")

    # Only trigger on main-source Kotlin file edits, not tests or build files
    if "/src/main/" not in file_path or not file_path.endswith(".kt"):
        return

    proj = os.environ.get("CLAUDE_PROJECT_DIR", ".")
    proj_path = Path(proj)
    sig_dir = str(proj_path / ".signatures")
    exclusions_file = proj_path / ".claude" / "sig-reachability-exclusions.txt"
    report_file = str(proj_path / ".claude" / "sig-reachability-report.html")
    script = str(Path(__file__).resolve())

    # If this file was excluded "until next edit", remove the exclusion now
    # so the fresh report will include it again.
    remove_file_exclusion(exclusions_file, proj_path, file_path)

    subprocess.Popen(
        [
            sys.executable, script,
            "--sig-dir", sig_dir,
            "--exclusions-file", str(exclusions_file),
            "--report-file", report_file,
        ],
        close_fds=True,
        start_new_session=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------


def main() -> None:
    parser = argparse.ArgumentParser(description="Sig reachability / dead-code scanner")
    parser.add_argument(
        "--sig-dir",
        default=".signatures",
        help="Path to .signatures directory (default: .signatures)",
    )
    parser.add_argument(
        "--report-file",
        default="-",
        help="Output path for report, or - for stdout (default: -)",
    )
    parser.add_argument(
        "--exclusions-file",
        default=None,
        help=(
            "Path to exclusions file (default: <sig-dir>/../.claude/sig-reachability-exclusions.txt). "
            "Lines with '/' are treated as source-file paths relative to project root; "
            "dotted identifiers are treated as package prefixes."
        ),
    )
    parser.add_argument(
        "--hook-mode",
        action="store_true",
        help="Hook protocol: read stdin, dispatch analysis in background, exit immediately",
    )
    args = parser.parse_args()

    if args.hook_mode:
        hook_mode()
        return

    sig_dir = Path(args.sig_dir).resolve()
    if not sig_dir.is_dir():
        print(f"Error: sig dir not found: {sig_dir}", file=sys.stderr)
        sys.exit(1)

    if args.exclusions_file is not None:
        exclusions_file: Path | None = Path(args.exclusions_file).resolve()
    else:
        # Default: <project-root>/.claude/sig-reachability-exclusions.txt
        exclusions_file = (sig_dir.parent / ".claude" / "sig-reachability-exclusions.txt").resolve()

    report = analyse(sig_dir, exclusions_file=exclusions_file)

    if args.report_file == "-":
        print(report)
    else:
        out = Path(args.report_file)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(report, encoding="utf-8")
        print(f"Report written to {args.report_file}", file=sys.stderr)


if __name__ == "__main__":
    main()
