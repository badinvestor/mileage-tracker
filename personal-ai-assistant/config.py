"""Configuration: model choice and the system prompt.

The model is read from the ASSISTANT_MODEL environment variable so you can swap
it without touching code:

  - claude-opus-4-8  (default) - excellent tool use, no special setup
  - claude-fable-5             - most capable; requires 30-day data retention
  - claude-haiku-4-5           - cheapest and fastest, good for simple tasks
"""

import os
from datetime import date

MODEL = os.environ.get("ASSISTANT_MODEL", "claude-opus-4-8")
MAX_TOKENS = 4096

# The whole personality and operating contract lives here. The single most
# important line is the explicit instruction to USE TOOLS rather than guess --
# the most common failure mode for a tool-using assistant is answering from
# memory when it should have searched or read a file.
SYSTEM_TEMPLATE = """\
You are a focused personal work assistant. You do three things well, and you
stay in your lane:

  1. Summarize documents the user points you to.
  2. Research questions on the web.
  3. Draft email replies and other short messages in the user's voice.

Today's date is {today}.

Operating rules:
- USE YOUR TOOLS. When a question depends on current information, search the
  web instead of answering from memory. When the user refers to a file or
  document, read it with the document tool instead of guessing its contents.
  Never fabricate facts, quotes, links, or file contents.
- When you draft an email or save a summary, use the save_draft tool so the
  user has the text on disk.
- Be concise and direct. Lead with the answer, then supporting detail.
- If you genuinely cannot do something with your tools, say so plainly.

{memory}"""


def build_system_prompt(memory_facts: list[str]) -> str:
    """Assemble the system prompt, folding in anything the user asked us to
    remember across sessions."""
    if memory_facts:
        memory = "What you know about this user (remembered across sessions):\n" + "\n".join(
            f"- {fact}" for fact in memory_facts
        )
    else:
        memory = "You don't have any remembered facts about this user yet."
    return SYSTEM_TEMPLATE.format(today=date.today().isoformat(), memory=memory)
