package uz.devsuhbat.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Declared from least to most senior, so levels compare by seniority. */
@Serializable
enum class Level {
    @SerialName("junior") JUNIOR,
    @SerialName("middle") MIDDLE,
    @SerialName("strong_middle") STRONG_MIDDLE,
    @SerialName("senior") SENIOR,
}

@Serializable
enum class QuestionType {
    @SerialName("single") SINGLE,
    @SerialName("multi") MULTI,
}

/** How a question is presented; grading depends only on [QuestionType]. */
@Serializable
enum class QuestionKind {
    @SerialName("concept") CONCEPT,
    @SerialName("true_false") TRUE_FALSE,
    @SerialName("code_output") CODE_OUTPUT,
    @SerialName("code_review") CODE_REVIEW,
}

/** [hint] explains why a wrong option is wrong; correct options carry none. */
@Serializable
data class Option(
    val id: String,
    val text: String,
    val correct: Boolean = false,
    val hint: String? = null,
)

@Serializable
data class Question(
    val id: String,
    val level: Level,
    val type: QuestionType,
    val kind: QuestionKind = QuestionKind.CONCEPT,
    val prompt: String,
    val code: String? = null,
    val options: List<Option>,
    val explanation: String,
    val reviewed: Boolean = false,
)

@Serializable
data class TopicFile(val topic: String, val questions: List<Question>)

@Serializable
data class Field(val id: String, val title: String, val group: String, val topics: List<String>)

/** A topic without a [file] is planned: listed in the catalog, content not written yet. */
@Serializable
data class Topic(val id: String, val title: String, val file: String? = null)

@Serializable
data class Catalog(val version: Int, val fields: List<Field>, val topics: List<Topic>)
