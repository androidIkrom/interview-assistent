package uz.devsuhbat.content

/**
 * Cached access to the content files. [read] returns the text of a file or null when it does not exist.
 * A missing or malformed file never throws: the catalog becomes null, a topic becomes an empty list.
 */
class ContentStore(private val read: (path: String) -> String?) {
    private val cachedCatalog: Catalog? by lazy { load(CATALOG_PATH, ContentParser::parseCatalog) }
    private val questionsByTopic = HashMap<String, List<Question>>()

    fun catalog(): Catalog? = cachedCatalog

    fun field(fieldId: String): Field? = cachedCatalog?.fields?.firstOrNull { it.id == fieldId }

    /** The field's topics in the field's own order; ids missing from the catalog are skipped. */
    fun topicsOf(fieldId: String): List<Topic> {
        val topicsById = cachedCatalog?.topics.orEmpty().associateBy { it.id }
        return field(fieldId)?.topics.orEmpty().mapNotNull { topicsById[it] }
    }

    fun questions(topicId: String): List<Question> = synchronized(questionsByTopic) {
        questionsByTopic.getOrPut(topicId) {
            val file = cachedCatalog?.topics?.firstOrNull { it.id == topicId }?.file
            file?.let { load(QUESTIONS_DIR + it, ContentParser::parseTopicFile) }?.questions.orEmpty()
        }
    }

    private fun <T> load(path: String, parse: (String) -> T): T? =
        try {
            read(path)?.let(parse)
        } catch (e: IllegalArgumentException) { // SerializationException is an IllegalArgumentException
            null
        }

    companion object {
        const val CATALOG_PATH = "content/catalog.json"
        const val QUESTIONS_DIR = "content/questions/"
    }
}
