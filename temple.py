#!/usr/bin/env python3
# /// script
# requires-python = ">=3.11"
# dependencies = []
# ///
# Run: python temple.py --help
"""KltytonTemple command entrypoint."""
import sys

sys.dont_write_bytecode = True
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

from tools.temple.cli import main

if __name__ == "__main__":
    raise SystemExit(main())
