package uz.devsuhbat.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.devsuhbat.multi
import uz.devsuhbat.option
import uz.devsuhbat.single
import uz.devsuhbat.trueFalse

class ContentValidatorTest {

    private val topicId = "android.kotlin"
    private val fileName = "android_kotlin.json"

    private fun catalog(
        fields: List<Field> = listOf(Field("android", "Android", "mobile", listOf(topicId))),
        topics: List<Topic> = listOf(Topic(topicId, "Kotlin tili", fileName)),
    ) = Catalog(version = 1, fields = fields, topics = topics)

    private fun validate(vararg questions: Question): List<String> =
        ContentValidator.validate(catalog(), mapOf(fileName to TopicFile(topicId, questions.toList())))

    private fun assertSingleError(errors: List<String>, mentions: String) {
        assertEquals("expected exactly one error, got: $errors", 1, errors.size)
        assertTrue("'${errors[0]}' should start with '$mentions'", errors[0].startsWith(mentions))
    }

    private val good = single("android.kotlin.001")

    @Test
    fun validContentHasNoErrors() {
        val errors = validate(good, multi("android.kotlin.002"), trueFalse("android.kotlin.003"))

        assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun missingTopicFileIsReported() {
        val errors = ContentValidator.validate(catalog(), emptyMap())

        assertSingleError(errors, fileName)
    }

    @Test
    fun plannedTopicWithoutFileIsAllowed() {
        val planned = catalog(
            fields = listOf(Field("ios", "iOS", "mobile", listOf("ios.swift"))),
            topics = listOf(Topic("ios.swift", "Swift tili")),
        )

        assertEquals(emptyList<String>(), ContentValidator.validate(planned, emptyMap()))
    }

    @Test
    fun topicFileForOtherTopicIsReported() {
        val errors = ContentValidator.validate(catalog(), mapOf(fileName to TopicFile("android.compose", listOf(good))))

        assertSingleError(errors, fileName)
    }

    @Test
    fun fieldReferencingUnknownTopicIsReported() {
        val broken = catalog(fields = listOf(Field("android", "Android", "mobile", listOf(topicId, "android.ghost"))))

        val errors = ContentValidator.validate(broken, mapOf(fileName to TopicFile(topicId, listOf(good))))

        assertSingleError(errors, "android")
        assertTrue(errors[0].contains("android.ghost"))
    }

    @Test
    fun duplicateTopicIdIsReported() {
        val broken = catalog(topics = listOf(Topic(topicId, "Kotlin tili", fileName), Topic(topicId, "Kotlin")))

        val errors = ContentValidator.validate(broken, mapOf(fileName to TopicFile(topicId, listOf(good))))

        assertSingleError(errors, topicId)
    }

    @Test
    fun duplicateFieldIdIsReported() {
        val field = Field("android", "Android", "mobile", listOf(topicId))

        val errors = ContentValidator.validate(
            catalog(fields = listOf(field, field)),
            mapOf(fileName to TopicFile(topicId, listOf(good))),
        )

        assertSingleError(errors, "android")
    }

    @Test
    fun duplicateQuestionIdIsReported() {
        assertSingleError(validate(good, good.copy(prompt = "other")), "android.kotlin.001")
    }

    @Test
    fun questionIdMustMatchTopicPrefixAndThreeDigits() {
        assertSingleError(validate(good.copy(id = "android.kotlin.1")), "android.kotlin.1")
        assertSingleError(validate(good.copy(id = "core.dsa.001")), "core.dsa.001")
        assertSingleError(validate(good.copy(id = "android.kotlin.0001")), "android.kotlin.0001")
    }

    @Test
    fun singleNeedsExactlyOneCorrect() {
        val noneCorrect = good.copy(options = listOf(option("a"), option("b")))
        val twoCorrect = good.copy(options = listOf(option("a", true), option("b", true), option("c")))

        assertSingleError(validate(noneCorrect), good.id)
        assertSingleError(validate(twoCorrect), good.id)
    }

    @Test
    fun singleNeedsTwoToFiveOptions() {
        val one = good.copy(options = listOf(option("a", true)))
        val six = good.copy(options = listOf(option("a", true)) + listOf("b", "c", "d", "e", "f").map { option(it) })

        assertSingleError(validate(one), good.id)
        assertSingleError(validate(six), good.id)
    }

    @Test
    fun multiNeedsTwoCorrectAndOneWrong() {
        val base = multi("android.kotlin.001")
        val oneCorrect = base.copy(options = listOf(option("a", true), option("b"), option("c")))
        val noWrong = base.copy(options = listOf(option("a", true), option("b", true)))

        assertSingleError(validate(oneCorrect), base.id)
        assertSingleError(validate(noWrong), base.id)
    }

    @Test
    fun wrongOptionNeedsHint() {
        val nullHint = good.copy(options = listOf(option("a", true), Option("b", "text b")))
        val blankHint = good.copy(options = listOf(option("a", true), Option("b", "text b", hint = "  ")))

        assertSingleError(validate(nullHint), good.id)
        assertSingleError(validate(blankHint), good.id)
    }

    @Test
    fun correctOptionMustNotHaveHint() {
        val hinted = good.copy(options = listOf(Option("a", "text a", correct = true, hint = "h"), option("b")))

        assertSingleError(validate(hinted), good.id)
    }

    @Test
    fun hintMustNotQuoteCorrectOption() {
        val leaking = good.copy(
            options = listOf(
                Option("a", "Immutable reference", correct = true),
                Option("b", "Mutable reference", hint = "Bu yerda immutable reference kerak."),
            )
        )

        assertSingleError(validate(leaking), good.id)
    }

    @Test
    fun shortCorrectTextIsNotCheckedForLeak() {
        val shortAnswer = good.copy(
            options = listOf(
                Option("a", "Ha", correct = true),
                Option("b", "Yo'q", hint = "Ha deb o'ylash uchun asos bor."),
            )
        )

        assertEquals(emptyList<String>(), validate(shortAnswer))
    }

    @Test
    fun blankPromptOrExplanationIsReported() {
        assertSingleError(validate(good.copy(prompt = " ")), good.id)
        assertSingleError(validate(good.copy(explanation = "")), good.id)
    }

    @Test
    fun duplicateOptionIdOrTextIsReported() {
        val sameId = good.copy(options = listOf(option("a", true), option("b"), Option("b", "other", hint = "h")))
        val sameText = good.copy(options = listOf(option("a", true), option("b"), Option("c", "text b", hint = "h")))

        assertSingleError(validate(sameId), good.id)
        assertSingleError(validate(sameText), good.id)
    }

    @Test
    fun blankOptionTextIsReported() {
        val blank = good.copy(options = listOf(option("a", true), Option("b", " ", hint = "h")))

        assertSingleError(validate(blank), good.id)
    }

    @Test
    fun codeKindsNeedCode() {
        val output = good.copy(kind = QuestionKind.CODE_OUTPUT, code = null)
        val review = good.copy(kind = QuestionKind.CODE_REVIEW, code = "  ")

        assertSingleError(validate(output), good.id)
        assertSingleError(validate(review), good.id)
        assertEquals(emptyList<String>(), validate(good.copy(kind = QuestionKind.CODE_OUTPUT, code = "println(1)")))
    }

    @Test
    fun trueFalseNeedsSingleWithTwoOptions() {
        val fourOptions = good.copy(kind = QuestionKind.TRUE_FALSE)
        val asMulti = multi("android.kotlin.001").copy(kind = QuestionKind.TRUE_FALSE)

        assertSingleError(validate(fourOptions), good.id)
        assertSingleError(validate(asMulti), good.id)
    }
}
