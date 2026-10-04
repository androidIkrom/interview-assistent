package uz.devsuhbat.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import uz.devsuhbat.engine.QuestionPicker

/** Runs the shipped assets through the parser and the validator. Gradle runs unit tests from the module directory. */
class ContentAssetsTest {

    private val assets = File("src/main/assets")
    private val store = ContentStore { path -> File(assets, path).takeIf { it.isFile }?.readText() }

    private fun catalog(): Catalog =
        ContentParser.parseCatalog(File(assets, ContentStore.CATALOG_PATH).readText())

    private fun topicFiles(catalog: Catalog): Map<String, TopicFile> =
        catalog.topics.mapNotNull { it.file }.distinct()
            .map { it to File(assets, ContentStore.QUESTIONS_DIR + it) }
            .filter { (_, file) -> file.isFile }
            .associate { (name, file) -> name to ContentParser.parseTopicFile(file.readText()) }

    @Test
    fun catalogParses() {
        assertNotNull(store.catalog())
    }

    @Test
    fun allContentPassesValidator() {
        val catalog = catalog()

        val errors = ContentValidator.validate(catalog, topicFiles(catalog))

        assertTrue(errors.joinToString("\n", prefix = "content errors:\n"), errors.isEmpty())
    }

    @Test
    fun catalogHasThirteenFieldsAndEightyEightTopics() {
        val catalog = catalog()

        assertEquals(13, catalog.fields.size)
        assertEquals(88, catalog.topics.size)
        assertEquals(setOf("mobile", "frontend", "backend", "other"), catalog.fields.map { it.group }.toSet())
    }

    @Test
    fun everyQuestionFileIsReferencedByTheCatalog() {
        val referenced = catalog().topics.mapNotNull { it.file }.toSet()
        val onDisk = File(assets, ContentStore.QUESTIONS_DIR).listFiles().orEmpty().map { it.name }.toSet()

        assertEquals(referenced, onDisk)
    }

    @Test
    fun androidFieldHasEnoughJuniorQuestionsForASession() {
        val all = store.topicsOf("android").flatMap { store.questions(it.id) }

        assertTrue(QuestionPicker.eligible(all, Level.JUNIOR).size >= QuestionPicker.PRACTICE_SIZE)
    }

    @Test
    fun androidSampleCoversEveryQuestionShape() {
        val all = store.topicsOf("android").flatMap { store.questions(it.id) }

        assertTrue(all.count { it.type == QuestionType.MULTI } >= 4)
        assertTrue(all.count { it.kind == QuestionKind.CODE_OUTPUT } >= 6)
        assertTrue(all.count { it.kind == QuestionKind.CODE_REVIEW } >= 3)
        assertTrue(all.count { it.kind == QuestionKind.TRUE_FALSE } >= 2)
        assertEquals(Level.entries.toSet(), all.map { it.level }.toSet())
    }

    /**
     * Topics whose content is finished, with the number of questions per level that docs/content/taxonomy.md
     * plans for them: junior, middle, strong middle, senior.
     */
    private val completedTopics = mapOf(
        "android.kotlin" to listOf(6, 5, 2, 1),
        "android.components" to listOf(5, 4, 2, 1),
        "android.compose" to listOf(4, 4, 3, 2),
        "android.async" to listOf(4, 4, 3, 2),
        "android.data" to listOf(3, 3, 2, 2),
        "android.arch" to listOf(2, 2, 2, 2),
        "core.mobile" to listOf(6, 8, 6, 4),
        "core.git" to listOf(14, 10, 6, 2),
        "core.http" to listOf(16, 14, 10, 6),
        "core.sql" to listOf(20, 18, 12, 8),
        "core.dsa" to listOf(20, 18, 12, 6),
        "fe.js" to listOf(6, 5, 2, 1),
        "fe.htmlcss" to listOf(5, 4, 2, 1),
        "fe.react" to listOf(4, 4, 3, 2),
        "fe.ts" to listOf(4, 4, 3, 2),
        "fe.browser" to listOf(3, 3, 2, 2),
        "fe.arch" to listOf(2, 2, 2, 2),
        "py.lang" to listOf(6, 5, 2, 1),
        "py.idioms" to listOf(5, 4, 2, 1),
        "py.web" to listOf(4, 4, 3, 2),
        "py.async" to listOf(4, 4, 3, 2),
        "py.db" to listOf(3, 3, 2, 2),
        "py.arch" to listOf(2, 2, 2, 2),
        "node.js" to listOf(6, 5, 2, 1),
        "node.runtime" to listOf(5, 4, 2, 1),
        "node.web" to listOf(4, 4, 3, 2),
        "node.async" to listOf(4, 4, 3, 2),
        "node.db" to listOf(3, 3, 2, 2),
        "node.arch" to listOf(2, 2, 2, 2),
        "core.oop" to listOf(16, 14, 10, 6),
        "core.security" to listOf(8, 10, 8, 6),
        "core.testing" to listOf(10, 10, 8, 4),
        "core.sysdesign" to listOf(0, 10, 16, 22),
        "core.aicode" to listOf(6, 8, 8, 6),
        "java.lang" to listOf(6, 5, 2, 1),
        "java.collections" to listOf(5, 4, 2, 1),
        "java.spring" to listOf(4, 4, 3, 2),
        "java.concurrency" to listOf(4, 4, 3, 2),
        "java.data" to listOf(3, 3, 2, 2),
        "java.arch" to listOf(2, 2, 2, 2),
        "flutter.dart" to listOf(6, 5, 2, 1),
        "flutter.widgets" to listOf(5, 4, 2, 1),
        "flutter.state" to listOf(4, 4, 3, 2),
        "flutter.async" to listOf(4, 4, 3, 2),
        "flutter.data" to listOf(3, 3, 2, 2),
        "flutter.arch" to listOf(2, 2, 2, 2),
        "go.lang" to listOf(6, 5, 2, 1),
        "go.types" to listOf(5, 4, 2, 1),
        "go.concurrency" to listOf(4, 4, 3, 2),
        "go.web" to listOf(4, 4, 3, 2),
        "go.data" to listOf(3, 3, 2, 2),
        "go.arch" to listOf(2, 2, 2, 2),
        "php.lang" to listOf(6, 5, 2, 1),
        "php.oop" to listOf(5, 4, 2, 1),
        "php.laravel" to listOf(4, 4, 3, 2),
        "php.eloquent" to listOf(4, 4, 3, 2),
        "php.async" to listOf(3, 3, 2, 2),
        "php.arch" to listOf(2, 2, 2, 2),
        "net.csharp" to listOf(6, 5, 2, 1),
        "net.types" to listOf(5, 4, 2, 1),
        "net.aspnet" to listOf(4, 4, 3, 2),
        "net.async" to listOf(4, 4, 3, 2),
        "net.data" to listOf(3, 3, 2, 2),
        "net.arch" to listOf(2, 2, 2, 2),
        "ios.swift" to listOf(6, 5, 2, 1),
        "ios.ui" to listOf(5, 4, 2, 1),
        "ios.swiftui" to listOf(4, 4, 3, 2),
        "ios.concurrency" to listOf(4, 4, 3, 2),
        "ios.data" to listOf(3, 3, 2, 2),
        "ios.arch" to listOf(2, 2, 2, 2),
        "qa.theory" to listOf(6, 5, 2, 1),
        "qa.docs" to listOf(5, 4, 2, 1),
        "qa.design" to listOf(4, 4, 3, 2),
        "qa.api" to listOf(4, 4, 3, 2),
        "qa.auto" to listOf(3, 3, 2, 2),
        "qa.process" to listOf(2, 2, 2, 2),
        "ops.linux" to listOf(6, 5, 2, 1),
        "ops.docker" to listOf(5, 4, 2, 1),
        "ops.cicd" to listOf(4, 4, 3, 2),
        "ops.k8s" to listOf(4, 4, 3, 2),
        "ops.iac" to listOf(3, 3, 2, 2),
        "ops.observability" to listOf(2, 2, 2, 2),
        "ml.python" to listOf(6, 5, 2, 1),
        "ml.stats" to listOf(5, 4, 2, 1),
        "ml.classic" to listOf(4, 4, 3, 2),
        "ml.eval" to listOf(4, 4, 3, 2),
        "ml.dl" to listOf(3, 3, 2, 2),
        "ml.ops" to listOf(2, 2, 2, 2),
    )

    @Test
    fun completedTopicsHaveTheirTaxonomyCounts() {
        val actual = completedTopics.keys.associateWith { topicId ->
            val questions = store.questions(topicId)
            Level.entries.map { level -> questions.count { it.level == level } }
        }

        assertEquals(completedTopics, actual)
    }

    // --- answers must not be guessable by option length (stage 5g) ---

    /** Topics whose option lengths are already balanced; grows batch by batch until it holds all 88. */
    private val lengthFixedTopics = setOf(
        "android.kotlin", "android.components", "android.compose", "android.async", "android.data", "android.arch",
        "fe.js", "fe.htmlcss", "fe.react", "fe.ts", "fe.browser", "fe.arch",
        "py.lang", "py.idioms", "py.web", "py.async", "py.db", "py.arch",
        "node.arch", "node.async", "node.db", "node.js", "node.runtime", "node.web",
        "java.arch", "java.collections", "java.concurrency", "java.data", "java.lang", "java.spring",
        "flutter.arch", "flutter.async", "flutter.dart", "flutter.data", "flutter.state", "flutter.widgets",
        "go.arch", "go.concurrency", "go.data", "go.lang", "go.types", "go.web",
        "php.arch", "php.async", "php.eloquent", "php.lang", "php.laravel", "php.oop",
        "net.arch", "net.aspnet", "net.async", "net.csharp", "net.data", "net.types",
        "ios.arch", "ios.concurrency", "ios.data", "ios.swift", "ios.swiftui", "ios.ui",
    )

    /**
     * A single question (not true/false) leaks when its correct option is longer than every wrong one; a multi
     * question leaks when every correct option is longer than every wrong one. tools/option_length_report.py
     * uses the same rule.
     */
    private fun leaksByLength(question: Question): Boolean {
        val correct = question.options.filter { it.correct }.map { it.text.length }
        val wrong = question.options.filterNot { it.correct }.map { it.text.length }
        return correct.min() > wrong.max()
    }

    private fun leakShare(questions: List<Question>): Double {
        val considered = questions.filter { it.kind != QuestionKind.TRUE_FALSE }
        return if (considered.isEmpty()) 0.0 else considered.count(::leaksByLength).toDouble() / considered.size
    }

    @Test
    fun fixedTopicsDoNotRevealAnswersByLength() {
        val tooLong = lengthFixedTopics.associateWith { leakShare(store.questions(it)) }
            .filterValues { it > MAX_LONGEST_SHARE_TOPIC }

        assertTrue("answer is the longest option too often: $tooLong", tooLong.isEmpty())
    }

    @Test
    fun bankDoesNotRevealAnswersByLength() {
        val catalog = catalog()
        assumeTrue(lengthFixedTopics.size == catalog.topics.size)

        val share = leakShare(catalog.topics.flatMap { store.questions(it.id) })

        assertTrue("answer is the longest option in ${"%.0f".format(share * 100)}% of the bank", share <= MAX_LONGEST_SHARE_BANK)
    }

    private companion object {
        const val MAX_LONGEST_SHARE_TOPIC = 0.5
        const val MAX_LONGEST_SHARE_BANK = 0.35
    }
}
