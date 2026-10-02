package uz.devsuhbat.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentStoreTest {

    private val files = mutableMapOf<String, String>()
    private val reads = mutableListOf<String>()
    private val store = ContentStore { path ->
        reads += path
        files[path]
    }

    private val catalogJson = """
        {"version":1,
         "fields":[{"id":"android","title":"Android","group":"mobile",
                    "topics":["android.compose","android.kotlin","android.ghost","android.planned"]}],
         "topics":[{"id":"android.kotlin","title":"Kotlin tili","file":"android_kotlin.json"},
                   {"id":"android.compose","title":"Compose","file":"android_compose.json"},
                   {"id":"android.planned","title":"Keyinroq"}]}
    """.trimIndent()

    private fun topicJson(topic: String) = """
        {"topic":"$topic","questions":[{"id":"$topic.001","level":"junior","type":"single","prompt":"p",
        "options":[{"id":"a","text":"1","correct":true},{"id":"b","text":"2","hint":"h"}],"explanation":"e"}]}
    """.trimIndent()

    private fun fullContent() {
        files["content/catalog.json"] = catalogJson
        files["content/questions/android_kotlin.json"] = topicJson("android.kotlin")
        files["content/questions/android_compose.json"] = topicJson("android.compose")
    }

    @Test
    fun readsCatalogAndQuestions() {
        fullContent()

        assertEquals(1, store.catalog()!!.version)
        assertEquals("Android", store.field("android")!!.title)
        assertEquals(listOf("android.kotlin.001"), store.questions("android.kotlin").map { it.id })
    }

    @Test
    fun missingCatalogGivesNull() {
        assertNull(store.catalog())
        assertNull(store.field("android"))
        assertEquals(emptyList<Topic>(), store.topicsOf("android"))
        assertEquals(emptyList<Question>(), store.questions("android.kotlin"))
    }

    @Test
    fun brokenCatalogGivesNull() {
        files["content/catalog.json"] = "{not json"

        assertNull(store.catalog())
    }

    @Test
    fun unknownFieldGivesNull() {
        fullContent()

        assertNull(store.field("cobol"))
        assertEquals(emptyList<Topic>(), store.topicsOf("cobol"))
    }

    @Test
    fun plannedTopicHasNoQuestions() {
        fullContent()

        assertEquals(emptyList<Question>(), store.questions("android.planned"))
        assertEquals(listOf("content/catalog.json"), reads)
    }

    @Test
    fun unknownTopicHasNoQuestions() {
        fullContent()

        assertEquals(emptyList<Question>(), store.questions("android.ghost"))
    }

    @Test
    fun missingTopicFileGivesEmptyList() {
        fullContent()
        files.remove("content/questions/android_kotlin.json")

        assertEquals(emptyList<Question>(), store.questions("android.kotlin"))
    }

    @Test
    fun brokenTopicFileGivesEmptyListAndOthersStillLoad() {
        fullContent()
        files["content/questions/android_kotlin.json"] = """{"topic":"android.kotlin","questions":[{"id":1"""

        assertEquals(emptyList<Question>(), store.questions("android.kotlin"))
        assertEquals(1, store.questions("android.compose").size)
        assertNotNull(store.catalog())
    }

    @Test
    fun resultsAreCached() {
        fullContent()

        repeat(2) {
            store.catalog()
            store.questions("android.kotlin")
        }

        assertEquals(listOf("content/catalog.json", "content/questions/android_kotlin.json"), reads)
    }

    @Test
    fun topicsOfKeepsFieldOrderAndSkipsUnknown() {
        fullContent()

        assertEquals(
            listOf("android.compose", "android.kotlin", "android.planned"),
            store.topicsOf("android").map { it.id },
        )
    }
}
