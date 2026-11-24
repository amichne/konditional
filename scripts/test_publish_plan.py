import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


class PublishPlanTest(unittest.TestCase):
    def test_preview_and_explicit_execution(self) -> None:
        root = Path(__file__).resolve().parents[1]
        make = shutil.which("make")
        if make is None:
            self.fail("The publication entry point needs make")

        with tempfile.TemporaryDirectory() as directory:
            fixture = Path(directory)
            shutil.copy(root / "Makefile", fixture)
            scripts = fixture / "scripts"
            scripts.mkdir()
            for script in ("publish-on-rails.sh", "bump-version.sh"):
                shutil.copy(root / "scripts" / script, scripts)
            properties = fixture / "gradle.properties"
            initial = b"version=1.2.3-SNAPSHOT\nVERSION=1.2.3-SNAPSHOT\n"
            properties.write_bytes(initial)
            calls = fixture / "calls"
            recorder = fixture / "make"
            recorder.write_text('#!/bin/sh\nprintf "%s\\n" "$@" >> "$RECORD"\n')
            recorder.chmod(0o700)

            def invoke(
                command: str, target: str, version: str
            ) -> subprocess.CompletedProcess[str]:
                return subprocess.run(
                    [
                        make,
                        command,
                        f"PUBLISH_TARGET={target}",
                        f"VERSION_CHOICE={version}",
                    ],
                    cwd=fixture,
                    check=False,
                    env={
                        **os.environ,
                        "PATH": f"{fixture}:{os.environ['PATH']}",
                        "RECORD": str(calls),
                    },
                    stdin=subprocess.DEVNULL,
                    capture_output=True,
                    text=True,
                    timeout=10,
                )

            for target, choice, destination, version in (
                ("release", "patch", "Maven Central releases", "1.2.4"),
                ("github", "minor-snapshot", "GitHub Packages", "1.3.0-SNAPSHOT"),
                ("snapshot", "snapshot", "Maven Central snapshots", "1.2.3-SNAPSHOT"),
                ("local", "none", "Local Maven repository", "1.2.3"),
            ):
                with self.subTest(target=target):
                    result = invoke("publish-plan", target, choice)
                    self.assertEqual(result.returncode, 0, result.stderr)
                    self.assertIn(
                        f"  destination: {destination}", result.stdout.splitlines()
                    )
                    self.assertIn(f"  version: {version}", result.stdout.splitlines())

            for target, choice in (("invalid", "patch"), ("release", "invalid")):
                self.assertNotEqual(
                    invoke("publish-plan", target, choice).returncode, 0
                )
            self.assertEqual(properties.read_bytes(), initial)
            self.assertFalse(calls.exists())

            result = invoke("publish", "release", "patch")
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(
                calls.read_text().splitlines(),
                ["publish-version-patch", "publish-run-release"],
            )
