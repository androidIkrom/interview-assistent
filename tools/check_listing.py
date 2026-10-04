"""Checks docs/play/listing-uz.md against Google Play metadata limits and wording rules.

Prints the length of the title, short and full description; exits 1 on any violation.
"""
import re
import sys
from pathlib import Path

LISTING = Path(__file__).resolve().parent.parent / "docs" / "play" / "listing-uz.md"
LIMITS = {"title": 30, "short": 80, "full": 4000}
BANNED = ["eng yaxshi", "#1", "bepul", "kafolatlaymiz", "ishga joylashasiz", "ishga kirasiz"]
# Technical acronyms are not "shouting".
ALLOWED_CAPS = {"FASTAPI"}
EMOJI = re.compile("[\U0001F300-\U0001FAFF☀-➿\U0001F000-\U0001F2FF]")


def blocks(text: str) -> list[str]:
    return [b.strip("\n") for b in re.findall(r"```\n(.*?)```", text, flags=re.S)]


def main() -> int:
    title, short, full = blocks(LISTING.read_text(encoding="utf-8"))[:3]
    problems = []
    for name, value in (("title", title), ("short", short), ("full", full)):
        print(f"{name:5} {len(value):4} / {LIMITS[name]}")
        if len(value) > LIMITS[name]:
            problems.append(f"{name} is longer than {LIMITS[name]}")
        lowered = value.lower()
        problems += [f"{name} contains '{word}'" for word in BANNED if word in lowered]
        if EMOJI.search(value):
            problems.append(f"{name} contains an emoji")
        shouting = [w for w in re.findall(r"\b[A-Z]{5,}\b", value) if w not in ALLOWED_CAPS]
        if shouting:
            problems.append(f"{name} has all-caps words: {shouting}")
    for p in problems:
        print("FAIL:", p)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
