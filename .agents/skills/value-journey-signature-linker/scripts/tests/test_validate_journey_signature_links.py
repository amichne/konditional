from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path


SCRIPT_PATH = (
    Path(__file__).resolve().parents[1] / "validate_journey_signature_links.py"
)

SPEC = importlib.util.spec_from_file_location(
    "validate_journey_signature_links",
    SCRIPT_PATH,
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


def write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def make_signature_tree(repo_root: Path) -> None:
    write_file(
        repo_root / ".signatures/INDEX.sig",
        "runtime/registry.sig|module=konditional-runtime\n",
    )
    write_file(
        repo_root / ".signatures/runtime/registry.sig",
        textwrap.dedent(
            """
            type=io.example.runtime.Registry|kind=class|visibility=public
            methods:
            - override fun evaluate(flagKey: String): Boolean
            - override fun rollback(steps: Int): Boolean
            fields:
            - val revision: Long
            """
        ).strip()
        + "\n",
    )


def make_links_file(repo_root: Path) -> None:
    payload = {
        "journeys": [
            {
                "journey_id": "JV-001",
                "title": "Sample",
                "value_proposition": "Sample proposition",
                "links": [
                    {
                        "kind": "type",
                        "signature": "io.example.runtime.Registry",
                    }
                ],
            }
        ]
    }
    write_file(
        repo_root / "docs/value-journeys/journey-signature-links.json",
        json.dumps(payload, indent=2, sort_keys=True) + "\n",
    )


def make_doc_refs(repo_root: Path, claim_ids: list[str]) -> None:
    claims_text = "\n".join(f"- {claim_id}" for claim_id in claim_ids)
    content = f"# JV-001: Sample journey\n\n## Claim IDs\n{claims_text}\n"
    write_file(repo_root / "docs/value-journeys/jv-001-sample.md", content)
    write_file(
        repo_root / "docusaurus/docs/value-journeys/jv-001-sample.md",
        content,
    )


class JourneyClaimsValidatorTests(unittest.TestCase):
    def test_parse_claims_rejects_invalid_enum_missing_tests_and_duplicate_claim_id(
        self,
    ) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            claims_payload = {
                "claims": [
                    {
                        "journey_id": "JV-001",
                        "claim_id": "JV-001-C1",
                        "claim_statement": "A valid length claim statement.",
                        "decision_type": "invalid",
                        "signatures": [
                            {
                                "kind": "type",
                                "signature": "io.example.runtime.Registry",
                            }
                        ],
                        "tests": [],
                        "status": "supported",
                        "owner_modules": ["konditional-runtime"],
                    },
                    {
                        "journey_id": "JV-001",
                        "claim_id": "JV-001-C1",
                        "claim_statement": "Another valid length claim statement.",
                        "decision_type": "operate",
                        "signatures": [
                            {
                                "kind": "type",
                                "signature": "io.example.runtime.Registry",
                            }
                        ],
                        "tests": ["path/to/test.kt::test name"],
                        "status": "supported",
                        "owner_modules": ["konditional-runtime"],
                    },
                ]
            }
            claims_file = root / "claims.json"
            write_file(claims_file, json.dumps(claims_payload))

            _, _, errors = MODULE.parse_claims(claims_file=claims_file, require_tests=True)

            joined = "\n".join(errors)
            self.assertIn("decision_type must be one of", joined)
            self.assertIn("tests must be a non-empty array", joined)
            self.assertIn("duplicate claim_id 'JV-001-C1'", joined)

    def test_resolve_test_ref_supports_path_and_fqcn_selectors(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            missing_path_ref = "konditional-runtime/src/test/FooTest.kt::present selector"
            catalog = MODULE.SignatureCatalog(
                type_symbols=frozenset({"io.example.runtime.RegistryTest"}),
                method_symbols=frozenset(),
                field_symbols=frozenset(),
                methods_by_type={
                    "io.example.runtime.RegistryTest": frozenset(
                        {"override fun handlesRollback(): Unit"}
                    )
                },
                method_names_by_type={
                    "io.example.runtime.RegistryTest": frozenset({"handlesRollback"})
                },
            )

            self.assertFalse(MODULE.resolve_test_ref(missing_path_ref, root, catalog))

            test_file = root / "konditional-runtime/src/test/FooTest.kt"
            write_file(test_file, "class FooTest { fun example() {} }\n// present selector\n")

            self.assertTrue(MODULE.resolve_test_ref(missing_path_ref, root, catalog))
            self.assertFalse(
                MODULE.resolve_test_ref(
                    "konditional-runtime/src/test/FooTest.kt::absent selector",
                    root,
                    catalog,
                )
            )
            self.assertTrue(
                MODULE.resolve_test_ref(
                    "io.example.runtime.RegistryTest#handlesRollback",
                    root,
                    catalog,
                )
            )
            self.assertFalse(
                MODULE.resolve_test_ref(
                    "io.example.runtime.RegistryTest#missingMethod",
                    root,
                    catalog,
                )
            )

    def test_cli_warn_succeeds_and_strict_fails_for_unresolved_claim(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            make_signature_tree(root)
            make_links_file(root)
            make_doc_refs(root, ["JV-001-C1"])

            claims_payload = {
                "owner_routing": {"konditional-runtime": "/konditional-runtime/**"},
                "claims": [
                    {
                        "journey_id": "JV-001",
                        "claim_id": "JV-001-C1",
                        "claim_statement": "Claim has signature evidence but test path is missing.",
                        "decision_type": "operate",
                        "signatures": [
                            {
                                "kind": "type",
                                "signature": "io.example.runtime.Registry",
                            }
                        ],
                        "tests": ["konditional-runtime/src/test/MissingTest.kt::selector"],
                        "status": "at_risk",
                        "owner_modules": ["konditional-runtime"],
                    }
                ],
            }
            write_file(
                root / "docs/value-journeys/journey-claims.json",
                json.dumps(claims_payload, indent=2, sort_keys=True) + "\n",
            )

            warn = subprocess.run(
                [
                    "python3",
                    str(SCRIPT_PATH),
                    "--repo-root",
                    str(root),
                    "--links-file",
                    "docs/value-journeys/journey-signature-links.json",
                    "--claims-file",
                    "docs/value-journeys/journey-claims.json",
                    "--signatures-dir",
                    ".signatures",
                    "--ci-mode",
                    "warn",
                ],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(warn.returncode, 0)
            warn_report = json.loads(warn.stdout)
            self.assertEqual(warn_report["status"], "issues")
            self.assertEqual(warn_report["claims"]["at_risk"], 1)
            self.assertEqual(
                warn_report["claims"]["unresolved"][0]["owner_paths"],
                ["/konditional-runtime/**"],
            )

            strict = subprocess.run(
                [
                    "python3",
                    str(SCRIPT_PATH),
                    "--repo-root",
                    str(root),
                    "--links-file",
                    "docs/value-journeys/journey-signature-links.json",
                    "--claims-file",
                    "docs/value-journeys/journey-claims.json",
                    "--signatures-dir",
                    ".signatures",
                    "--ci-mode",
                    "strict",
                ],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(strict.returncode, 2)

    def test_report_output_is_deterministic_and_sorted_by_journey_then_claim(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            make_signature_tree(root)
            make_links_file(root)
            make_doc_refs(root, ["JV-001-C1", "JV-002-C1"])

            claims_payload = {
                "claims": [
                    {
                        "journey_id": "JV-002",
                        "claim_id": "JV-002-C1",
                        "claim_statement": "Missing both signature and test should be missing.",
                        "decision_type": "migrate",
                        "signatures": [
                            {
                                "kind": "type",
                                "signature": "io.example.runtime.DoesNotExist",
                            }
                        ],
                        "tests": ["konditional-runtime/src/test/Missing.kt::x"],
                        "status": "missing",
                        "owner_modules": ["konditional-runtime"],
                    },
                    {
                        "journey_id": "JV-001",
                        "claim_id": "JV-001-C1",
                        "claim_statement": "Missing both signature and test should be missing.",
                        "decision_type": "operate",
                        "signatures": [
                            {
                                "kind": "type",
                                "signature": "io.example.runtime.AlsoMissing",
                            }
                        ],
                        "tests": ["konditional-runtime/src/test/AlsoMissing.kt::x"],
                        "status": "missing",
                        "owner_modules": ["konditional-runtime"],
                    },
                ]
            }
            write_file(
                root / "docs/value-journeys/journey-claims.json",
                json.dumps(claims_payload, indent=2, sort_keys=True) + "\n",
            )

            report_rel = "docs/value-journeys/journey-claims-report.json"
            cmd = [
                "python3",
                str(SCRIPT_PATH),
                "--repo-root",
                str(root),
                "--links-file",
                "docs/value-journeys/journey-signature-links.json",
                "--claims-file",
                "docs/value-journeys/journey-claims.json",
                "--signatures-dir",
                ".signatures",
                "--ci-mode",
                "warn",
                "--report-out",
                report_rel,
            ]

            first = subprocess.run(cmd, capture_output=True, text=True, check=False)
            self.assertEqual(first.returncode, 0)
            first_content = (root / report_rel).read_text(encoding="utf-8")

            second = subprocess.run(cmd, capture_output=True, text=True, check=False)
            self.assertEqual(second.returncode, 0)
            second_content = (root / report_rel).read_text(encoding="utf-8")

            self.assertEqual(first_content, second_content)
            report = json.loads(first_content)
            unresolved_ids = [item["claim_id"] for item in report["claims"]["unresolved"]]
            self.assertEqual(unresolved_ids, ["JV-001-C1", "JV-002-C1"])


if __name__ == "__main__":
    unittest.main()
