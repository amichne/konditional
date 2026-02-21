#!/usr/bin/env python3
"""Backward-compatible wrapper for journey validation.

Use this script when your link map lives at:
`docs/value-journeys/journey-signature-links.json`.
"""

from __future__ import annotations

import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from validate_doc_signature_links import main as validate_main


if __name__ == "__main__":
    raise SystemExit(
        validate_main(
            default_links_file="docs/value-journeys/journey-signature-links.json"
        )
    )
