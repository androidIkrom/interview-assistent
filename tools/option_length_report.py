"""Report how often a question's correct answer can be spotted by option length.

A single question (kind other than true_false) leaks when its correct option is strictly longer than every
wrong option. A multi question leaks when every correct option is strictly longer than every wrong option.
ContentAssetsTest uses the same definition.

Usage:
    python tools/option_length_report.py                 # every topic plus the bank total
    python tools/option_length_report.py android.kotlin core.dsa
    python tools/option_length_report.py --leaks android.kotlin   # also list the leaking question ids
"""
import glob
import json
import os
import sys

QUESTIONS = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "content", "questions")


def leaks(question):
    options = question["options"]
    correct = [len(o["text"]) for o in options if o.get("correct")]
    wrong = [len(o["text"]) for o in options if not o.get("correct")]
    return min(correct) > max(wrong)


def considered(question):
    return question.get("kind") != "true_false"


def main(args):
    show_ids = "--leaks" in args
    wanted = [a for a in args if not a.startswith("--")]
    total = leaking = 0
    for path in sorted(glob.glob(os.path.join(QUESTIONS, "*.json"))):
        data = json.load(open(path, encoding="utf-8"))
        topic = data["topic"]
        if wanted and topic not in wanted:
            continue
        questions = [q for q in data["questions"] if considered(q)]
        bad = [q["id"] for q in questions if leaks(q)]
        total += len(questions)
        leaking += len(bad)
        share = len(bad) / len(questions) if questions else 0
        print(f"{topic:22} {len(bad):3}/{len(questions):3}  {share:5.0%}")
        if show_ids and bad:
            print("    " + " ".join(i.rsplit(".", 1)[1] for i in bad))
    if total:
        print(f"{'TOTAL':22} {leaking:3}/{total:3}  {leaking / total:5.0%}")


if __name__ == "__main__":
    main(sys.argv[1:])
