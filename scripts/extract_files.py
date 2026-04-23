#!/usr/bin/env python3
"""
extract_files.py
-----------------
Reads a markdown output file produced by a Claude Code agent and extracts
every fenced code block that starts with a FILE comment into its actual path.

Supported comment formats (first line of each code block):
  JavaScript / JSX / TypeScript:  // FILE: path/to/file.js
  Python / Shell / Text:          # FILE: path/to/file.py
  CSS:                            /* FILE: path/to/file.css */
  JSON (no comment — label above): <!-- FILE: path/to/file.json -->

Usage:
  python3 scripts/extract_files.py frontend_output.md
  python3 scripts/extract_files.py backend_output.md

The script creates directories as needed and writes each file.
Existing files are overwritten — run this after each agent run to refresh.
"""

import sys
import os
import re

# ── Regex patterns for FILE comments ──────────────────────────────────────────
FILE_PATTERNS = [
    re.compile(r'^//\s*FILE:\s*(.+)$'),           # JavaScript / JSX
    re.compile(r'^#\s*FILE:\s*(.+)$'),             # Python / Shell / TOML
    re.compile(r'^/\*\s*FILE:\s*(.+?)\s*\*/$'),   # CSS
    re.compile(r'^<!--\s*FILE:\s*(.+?)\s*-->$'),  # HTML / Markdown
]

def extract_file_path(first_line: str):
    """Return the file path from a FILE comment, or None if not found."""
    for pattern in FILE_PATTERNS:
        m = pattern.match(first_line.strip())
        if m:
            return m.group(1).strip()
    return None

def extract_files(md_path: str):
    """Parse md_path and write out all labelled code blocks."""
    if not os.path.exists(md_path):
        print(f"ERROR: File not found: {md_path}")
        sys.exit(1)

    with open(md_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find all fenced code blocks (``` ... ```)
    # Use non-greedy match, allow optional language tag after backticks
    pattern = re.compile(r'```[^\n]*\n(.*?)```', re.DOTALL)
    blocks = pattern.findall(content)

    written = []
    skipped = 0

    for block in blocks:
        lines = block.split('\n')
        if not lines:
            continue

        file_path = extract_file_path(lines[0])
        if not file_path:
            skipped += 1
            continue

        # The actual file content starts on line 2 (after the FILE comment)
        file_content = '\n'.join(lines[1:]).rstrip('\n') + '\n'

        # Create parent directories if they don't exist
        parent = os.path.dirname(file_path)
        if parent:
            os.makedirs(parent, exist_ok=True)

        # Write the file
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(file_content)

        written.append(file_path)
        print(f"  wrote: {file_path}")

    print(f"\nDone. {len(written)} files written, {skipped} unlabelled blocks skipped.")
    if written:
        print("\nFiles created:")
        for p in written:
            print(f"  {p}")

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python3 scripts/extract_files.py <output_file.md>")
        print("Example: python3 scripts/extract_files.py frontend_output.md")
        sys.exit(1)

    extract_files(sys.argv[1])
