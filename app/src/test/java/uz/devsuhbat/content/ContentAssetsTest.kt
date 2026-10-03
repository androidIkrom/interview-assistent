package uz.devsuhbat.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    )

    @Test
    fun completedTopicsHaveTheirTaxonomyCounts() {
        val actual = completedTopics.keys.associateWith { topicId ->
            val questions = store.questions(topicId)
            Level.entries.map { level -> questions.count { it.level == level } }
        }

        assertEquals(completedTopics, actual)
    }
}
