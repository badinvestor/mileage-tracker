"""Dead-simple persistent memory: a JSON file of facts the assistant has been
asked to remember. Start simple; swap in a vector store later only if plain
recall proves insufficient.
"""

import json
from pathlib import Path

MEMORY_FILE = Path(__file__).parent / "memory.json"


def load_facts() -> list[str]:
    if not MEMORY_FILE.exists():
        return []
    try:
        return json.loads(MEMORY_FILE.read_text())
    except (json.JSONDecodeError, OSError):
        return []


def add_fact(fact: str) -> None:
    facts = load_facts()
    if fact not in facts:
        facts.append(fact)
        MEMORY_FILE.write_text(json.dumps(facts, indent=2))
