"""The assistant's tools: web search, document reading, draft saving, and
memory. Each tool is a small Python function plus a JSON schema the model sees.

To add a capability, write the function, add its schema to TOOLS, and add a
branch to run_tool. That's the whole extension story.
"""

from datetime import datetime
from pathlib import Path

import memory

DRAFTS_DIR = Path(__file__).parent / "drafts"


# --- Tool schemas the model sees -------------------------------------------

TOOLS = [
    {
        "name": "search_web",
        "description": (
            "Search the web for current information. Use this whenever the "
            "answer depends on recent events, current facts, or anything you "
            "are not certain about. Returns a list of result titles, URLs, and "
            "snippets."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "The search query."},
            },
            "required": ["query"],
        },
    },
    {
        "name": "read_document",
        "description": (
            "Read a local document so you can summarize or answer questions "
            "about it. Supports .txt, .md, and .pdf files. Use this instead of "
            "guessing a file's contents."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "Path to the file to read."},
            },
            "required": ["path"],
        },
    },
    {
        "name": "save_draft",
        "description": (
            "Save text to a file in the drafts/ folder so the user can review "
            "it. Use this for email drafts, summaries, and any deliverable the "
            "user will want to keep."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "filename": {
                    "type": "string",
                    "description": "A short, descriptive filename, e.g. 'reply-to-acme.md'.",
                },
                "content": {"type": "string", "description": "The full text to save."},
            },
            "required": ["filename", "content"],
        },
    },
    {
        "name": "remember",
        "description": (
            "Store a durable fact about the user (a preference, a recurring "
            "contact, their writing tone) so you recall it in future sessions. "
            "Use this when the user tells you something worth keeping."
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "fact": {"type": "string", "description": "The fact to remember."},
            },
            "required": ["fact"],
        },
    },
]


# --- Tool implementations ---------------------------------------------------

def _search_web(query: str) -> str:
    from ddgs import DDGS  # imported lazily so the import error is actionable

    with DDGS() as ddgs:
        results = list(ddgs.text(query, max_results=5))
    if not results:
        return "No results found."
    lines = []
    for r in results:
        lines.append(f"- {r.get('title', '')}\n  {r.get('href', '')}\n  {r.get('body', '')}")
    return "\n".join(lines)


def _read_document(path: str) -> str:
    p = Path(path).expanduser()
    if not p.exists():
        return f"Error: no file at {p}"
    if p.suffix.lower() == ".pdf":
        from pypdf import PdfReader

        reader = PdfReader(str(p))
        text = "\n".join(page.extract_text() or "" for page in reader.pages)
        return text or "(The PDF contained no extractable text.)"
    return p.read_text(errors="replace")


def _save_draft(filename: str, content: str) -> str:
    DRAFTS_DIR.mkdir(exist_ok=True)
    safe_name = Path(filename).name  # strip any path components
    target = DRAFTS_DIR / safe_name
    target.write_text(content)
    return f"Saved to {target}"


def _remember(fact: str) -> str:
    memory.add_fact(fact)
    return f"Remembered: {fact}"


def run_tool(name: str, tool_input: dict) -> tuple[str, bool]:
    """Dispatch a tool call. Returns (result_text, is_error)."""
    try:
        if name == "search_web":
            return _search_web(tool_input["query"]), False
        if name == "read_document":
            return _read_document(tool_input["path"]), False
        if name == "save_draft":
            return _save_draft(tool_input["filename"], tool_input["content"]), False
        if name == "remember":
            return _remember(tool_input["fact"]), False
        return f"Unknown tool: {name}", True
    except Exception as exc:  # surface the failure to the model so it can adapt
        return f"Tool '{name}' failed: {exc}", True
