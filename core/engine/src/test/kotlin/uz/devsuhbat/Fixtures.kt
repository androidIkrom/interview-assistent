package uz.devsuhbat

import uz.devsuhbat.content.Level
import uz.devsuhbat.content.Option
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionKind
import uz.devsuhbat.content.QuestionType

fun option(id: String, correct: Boolean = false): Option =
    Option(id = id, text = "text $id", correct = correct, hint = if (correct) null else "hint $id")

/** Single-choice question with options a (correct), b, c, d. */
fun single(
    id: String,
    level: Level = Level.JUNIOR,
    kind: QuestionKind = QuestionKind.CONCEPT,
    code: String? = null,
): Question = Question(
    id = id,
    level = level,
    type = QuestionType.SINGLE,
    kind = kind,
    prompt = "prompt $id",
    code = code,
    options = listOf(option("a", correct = true), option("b"), option("c"), option("d")),
    explanation = "explanation $id",
)

/** Multi-choice question with options a, b (correct) and c, d (wrong). */
fun multi(id: String, level: Level = Level.JUNIOR): Question = Question(
    id = id,
    level = level,
    type = QuestionType.MULTI,
    prompt = "prompt $id",
    options = listOf(option("a", correct = true), option("b", correct = true), option("c"), option("d")),
    explanation = "explanation $id",
)

/** True/false question with options a (correct), b. */
fun trueFalse(id: String): Question = Question(
    id = id,
    level = Level.JUNIOR,
    type = QuestionType.SINGLE,
    kind = QuestionKind.TRUE_FALSE,
    prompt = "prompt $id",
    options = listOf(option("a", correct = true), option("b")),
    explanation = "explanation $id",
)
