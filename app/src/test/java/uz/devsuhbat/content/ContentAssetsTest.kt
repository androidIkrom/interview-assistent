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
}
