# Personal Work Assistant

A small, focused AI assistant you run yourself. It does three things well —
**summarizes documents**, **researches questions on the web**, and **drafts
email replies in your voice** — and deliberately nothing else. Inspired by
["How (and Why) I Built an AI Assistant"](https://www.kdnuggets.com/how-and-why-i-built-an-ai-assistant):
the hardest part isn't the code, it's deciding what you actually want it to do.
A focused assistant beats a general one.

The whole thing is a few hundred lines of Python on top of the Anthropic SDK,
and runs anywhere Python runs.

## Why build your own?

- **Control.** It knows *your* context, uses *your* tone, and connects to
  *your* tools, instead of being built around someone else's assumptions.
- **Data ownership.** Your prompts and documents go to the model provider you
  choose, and the memory and drafts live on your own disk.
- **It's small enough to understand.** One agentic loop, four tools, a JSON
  memory file. You can read all of it.

## Architecture

```
assistant.py   chat() — the single entry point. Handles memory, the LLM call,
               tool execution, and the agentic loop. Also the CLI.
tools.py       The four tools (schemas + implementations) and the dispatcher.
memory.py      Persistent memory: a JSON file of facts to recall across sessions.
config.py      Model choice and the system prompt.
```

`chat(message, history)` builds the system prompt (folding in remembered
facts), calls the model with the tool definitions, and loops: while the model
asks for a tool, it runs the tool and feeds the result back; when the model is
done, it returns the reply.

## Setup

```bash
git clone <this-repo>
cd ai-assistant
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env        # then put your ANTHROPIC_API_KEY in .env
export $(grep -v '^#' .env | xargs)   # or use your own way of loading env vars
python assistant.py
```

## Using it

```
you > summarize ~/briefs/acme-proposal.pdf and save the summary
you > what's the latest on the EU AI Act? give me three bullet points with sources
you > draft a polite reply declining the meeting in ~/inbox/meeting-request.txt
you > remember that I prefer a warm but brief tone in emails
```

Drafts and summaries are written to `drafts/`. Facts you ask it to remember
are stored in `memory.json`. Both are gitignored.

## Choosing a model

Set `ASSISTANT_MODEL` in your environment:

| Model              | When to use it                                            |
| ------------------ | --------------------------------------------------------- |
| `claude-opus-4-8`  | Default. Excellent tool use, nothing special to set up.   |
| `claude-fable-5`   | Most capable. Requires 30-day data retention on your org. |
| `claude-haiku-4-5` | Cheapest and fastest; good for simple, high-volume use.   |

## Extending it

Adding a capability is three steps in `tools.py`: write the function, add its
schema to `TOOLS`, and add a branch to `run_tool`. Keep the set small — the
focus is the point.

Natural next steps: pull documents straight from Google Drive instead of local
paths; add a real email integration so drafts go to your Drafts folder; swap
the JSON memory for embeddings if simple recall stops being enough.
