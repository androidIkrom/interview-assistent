package uz.devsuhbat.engine

import uz.devsuhbat.content.Question

data class Progress(val mastered: Int, val total: Int) {
    /** Whole percent, rounded down; 0 when there are no questions. */
    val percent: Int get() = if (total == 0) 0 else mastered * 100 / total
}

object Readiness {
    /** Share of [questions] that are mastered. States of questions outside the list are ignored. */
    fun of(questions: List<Question>, states: Map<String, QuestionState>): Progress =
        Progress(mastered = questions.count { states[it.id]?.mastered == true }, total = questions.size)
}
