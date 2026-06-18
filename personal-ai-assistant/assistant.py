"""A focused personal work assistant in one small file.

The single entry point is chat(): you pass it a message and the running
conversation, and it handles memory, the LLM call, tool execution, and the
agentic loop, returning the assistant's reply. The block at the bottom turns
this script into a runnable CLI:

    python assistant.py
"""

import anthropic

import config
import memory
from tools import TOOLS, run_tool

client = anthropic.Anthropic()  # reads ANTHROPIC_API_KEY from the environment


def chat(user_message: str, history: list[dict]) -> str:
    """Send one user message and return the assistant's reply.

    `history` is the running list of messages; it is mutated in place so the
    caller keeps full conversation state across turns.
    """
    history.append({"role": "user", "content": user_message})
    system = config.build_system_prompt(memory.load_facts())

    # Agentic loop: keep going while the model wants to call tools.
    while True:
        response = client.messages.create(
            model=config.MODEL,
            max_tokens=config.MAX_TOKENS,
            system=system,
            tools=TOOLS,
            messages=history,
        )
        history.append({"role": "assistant", "content": response.content})

        if response.stop_reason == "refusal":
            return "[The assistant declined to respond to that request.]"

        if response.stop_reason == "tool_use":
            tool_results = []
            for block in response.content:
                if block.type == "tool_use":
                    result, is_error = run_tool(block.name, block.input)
                    tool_results.append(
                        {
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": result,
                            "is_error": is_error,
                        }
                    )
            history.append({"role": "user", "content": tool_results})
            continue  # let the model use the results

        # Normal completion: gather the text blocks and return.
        return "".join(b.text for b in response.content if b.type == "text")


def main() -> None:
    print("Personal assistant ready. Type your message, or 'exit' to quit.\n")
    history: list[dict] = []
    while True:
        try:
            user_message = input("you > ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if user_message.lower() in {"exit", "quit"}:
            break
        if not user_message:
            continue
        reply = chat(user_message, history)
        print(f"\nassistant > {reply}\n")


if __name__ == "__main__":
    main()
