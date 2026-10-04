package uz.devsuhbat.engine

import uz.devsuhbat.content.Question

/** How many questions are untouched, still being learned, or mastered. */
data class Breakdown(val fresh: Int, val learning: Int, val mastered: Int) {
    val total: Int get() = fresh + learning + mastered
}

object LeitnerBreakdown {
    /** States of questions outside [questions] are ignored. */
    fun of(questions: List<Question>, states: Map<String, QuestionState>): Breakdown {
        var fresh = 0
        var learning = 0
        var mastered = 0
        questions.forEach { question ->
            val state = states[question.id]
            when {
                state == null -> fresh++
                state.mastered -> mastered++
                else -> learning++
            }
        }
        return Breakdown(fresh, learning, mastered)
    }
}
