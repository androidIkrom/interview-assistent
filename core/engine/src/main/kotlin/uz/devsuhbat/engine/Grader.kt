package uz.devsuhbat.engine

import uz.devsuhbat.content.Option
import uz.devsuhbat.content.Question

sealed interface Verdict {
    data object Correct : Verdict

    /**
     * [wrongPicked] are the selected wrong options, in question order; each carries its hint.
     * [correctPicked] and [correctMissing] are counts only, so a wrong verdict never tells which options are correct.
     */
    data class Wrong(val wrongPicked: List<Option>, val correctPicked: Int, val correctMissing: Int) : Verdict
}

object Grader {
    fun grade(question: Question, selected: Set<String>): Verdict {
        val correctIds = question.options.filter { it.correct }.map { it.id }.toSet()
        if (selected == correctIds) return Verdict.Correct

        val correctPicked = selected.count { it in correctIds }
        return Verdict.Wrong(
            wrongPicked = question.options.filter { !it.correct && it.id in selected },
            correctPicked = correctPicked,
            correctMissing = correctIds.size - correctPicked,
        )
    }
}
