package uz.devsuhbat.engine

import kotlin.random.Random
import uz.devsuhbat.content.Question
import uz.devsuhbat.content.QuestionKind
import uz.devsuhbat.content.QuestionType
import uz.devsuhbat.content.topicId

data class MockAnswer(val questionId: String, val topicId: String, val correct: Boolean)

data class TopicScore(val topicId: String, val total: Int, val correct: Int)

data class MockResult(val answers: List<MockAnswer>) {
    val total: Int get() = answers.size
    val correct: Int get() = answers.count { it.correct }

    /** Scores per topic, in the order the topics first appear in the mock. */
    val topics: List<TopicScore>
        get() = answers.groupBy { it.topicId }.map { (topicId, ofTopic) ->
            TopicScore(topicId, total = ofTopic.size, correct = ofTopic.count { it.correct })
        }
}

/**
 * A mock interview: every question takes one answer, which can be changed until [finish].
 * There is no feedback; an unanswered question counts as wrong.
 */
class MockSession(questions: List<Question>, random: Random) {
    /** The questions in the given order, each with its options shuffled once (true/false keeps file order). */
    val questions: List<Question> = questions.map { question ->
        if (question.kind == QuestionKind.TRUE_FALSE) question
        else question.copy(options = question.options.shuffled(random))
    }

    private val selections = MutableList(questions.size) { emptySet<String>() }

    val size: Int get() = questions.size

    val answeredCount: Int get() = selections.count { it.isNotEmpty() }

    fun selection(index: Int): Set<String> = selections[index]

    /** Replaces the answer of question [index]; an empty set clears it. */
    fun answer(index: Int, selected: Set<String>) {
        val question = questions[index]
        val optionIds = question.options.map { it.id }.toSet()
        require(selected.all { it in optionIds }) { "unknown option in $selected for ${question.id}" }
        require(question.type == QuestionType.MULTI || selected.size <= 1) { "${question.id} takes one option" }
        selections[index] = selected
    }

    fun finish(): MockResult = MockResult(
        questions.mapIndexed { index, question ->
            val correct = selections[index].isNotEmpty() && Grader.grade(question, selections[index]) == Verdict.Correct
            MockAnswer(question.id, question.topicId, correct)
        }
    )
}
