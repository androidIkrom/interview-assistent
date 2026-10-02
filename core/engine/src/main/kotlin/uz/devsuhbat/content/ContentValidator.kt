package uz.devsuhbat.content

/** Checks the content rules of spec section 3.3. Every message starts with the offending id or file name. */
object ContentValidator {
    /** Shorter correct answers ("Ha", "1") appear in hints by accident, so they are not checked for leaks. */
    private const val LEAK_CHECK_MIN_LENGTH = 5
    private val singleOptionCount = 2..5

    /** [files] is keyed by [Topic.file]. An empty result means the content is valid. */
    fun validate(catalog: Catalog, files: Map<String, TopicFile>): List<String> {
        val errors = mutableListOf<String>()

        duplicates(catalog.fields.map { it.id }).forEach { errors += "$it: duplicate field id" }
        duplicates(catalog.topics.map { it.id }).forEach { errors += "$it: duplicate topic id" }

        val topicIds = catalog.topics.map { it.id }.toSet()
        for (field in catalog.fields.distinctBy { it.id }) {
            field.topics.filter { it !in topicIds }.forEach { errors += "${field.id}: references unknown topic $it" }
        }

        val seenQuestionIds = mutableSetOf<String>()
        for (topic in catalog.topics.distinctBy { it.id }) {
            val fileName = topic.file ?: continue
            val file = files[fileName]
            if (file == null) {
                errors += "$fileName: file is missing (topic ${topic.id})"
                continue
            }
            if (file.topic != topic.id) {
                errors += "$fileName: declares topic ${file.topic}, expected ${topic.id}"
            }
            val idPattern = Regex("^" + Regex.escape(topic.id) + "\\.\\d{3}$")
            for (question in file.questions) {
                if (!seenQuestionIds.add(question.id)) errors += "${question.id}: duplicate question id"
                if (!idPattern.matches(question.id)) errors += "${question.id}: id must be ${topic.id}.<3 digits>"
                validateQuestion(question, errors)
            }
        }
        return errors
    }

    private fun validateQuestion(question: Question, errors: MutableList<String>) {
        fun error(message: String) {
            errors += "${question.id}: $message"
        }

        val options = question.options
        val correct = options.filter { it.correct }
        val wrong = options.filterNot { it.correct }

        if (question.prompt.isBlank()) error("prompt is blank")
        if (question.explanation.isBlank()) error("explanation is blank")

        if (duplicates(options.map { it.id }).isNotEmpty()) error("duplicate option id")
        if (options.any { it.text.isBlank() }) error("blank option text")
        if (duplicates(options.map { it.text.trim() }).isNotEmpty()) error("duplicate option text")

        when (question.type) {
            QuestionType.SINGLE -> {
                if (correct.size != 1) error("single needs exactly 1 correct option, has ${correct.size}")
                if (options.size !in singleOptionCount) error("single needs 2 to 5 options, has ${options.size}")
            }
            QuestionType.MULTI -> {
                if (correct.size < 2) error("multi needs at least 2 correct options, has ${correct.size}")
                if (wrong.isEmpty()) error("multi needs at least 1 wrong option")
            }
        }

        if (wrong.any { it.hint.isNullOrBlank() }) error("every wrong option needs a hint")
        if (correct.any { it.hint != null }) error("a correct option must not have a hint")

        val leakable = correct.map { it.text.trim() }.filter { it.length >= LEAK_CHECK_MIN_LENGTH }
        val leaks = wrong.any { option -> leakable.any { option.hint.orEmpty().contains(it, ignoreCase = true) } }
        if (leaks) error("a hint quotes the correct option")

        val needsCode = question.kind == QuestionKind.CODE_OUTPUT || question.kind == QuestionKind.CODE_REVIEW
        if (needsCode && question.code.isNullOrBlank()) error("${question.kind} needs code")

        val validTrueFalse = question.type == QuestionType.SINGLE && options.size == 2
        if (question.kind == QuestionKind.TRUE_FALSE && !validTrueFalse) error("true_false must be single with 2 options")
    }

    private fun duplicates(values: List<String>): Set<String> =
        values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
}
