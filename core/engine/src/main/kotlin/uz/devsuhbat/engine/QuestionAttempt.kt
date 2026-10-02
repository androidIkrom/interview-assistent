package uz.devsuhbat.engine

import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionType

/** The submissions made on one presentation of a question. Immutable: [submit] returns the next state. */
data class QuestionAttempt(
    val question: Question,
    /** Wrong options already picked; they cannot be picked again. */
    val disabled: Set<String> = emptySet(),
    val wrongSubmissions: Int = 0,
    val solved: Boolean = false,
) {
    val firstTryCorrect: Boolean get() = solved && wrongSubmissions == 0

    fun canSubmit(selected: Set<String>): Boolean {
        if (solved || selected.isEmpty()) return false
        if (question.type == QuestionType.SINGLE && selected.size != 1) return false
        val optionIds = question.options.map { it.id }.toSet()
        return selected.all { it in optionIds && it !in disabled }
    }

    fun submit(selected: Set<String>): Pair<QuestionAttempt, Verdict> {
        require(canSubmit(selected)) { "selection $selected cannot be submitted for ${question.id}" }
        return when (val verdict = Grader.grade(question, selected)) {
            Verdict.Correct -> copy(solved = true) to verdict
            is Verdict.Wrong -> copy(
                disabled = disabled + verdict.wrongPicked.map { it.id },
                wrongSubmissions = wrongSubmissions + 1,
            ) to verdict
        }
    }
}
