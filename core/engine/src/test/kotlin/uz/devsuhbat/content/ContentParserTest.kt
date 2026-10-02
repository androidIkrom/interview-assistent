package uz.devsuhbat.content

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentParserTest {

    @Test
    fun parsesTopicFile() {
        val file = ContentParser.parseTopicFile(
            """
            {"topic":"android.kotlin","questions":[{"id":"android.kotlin.001","level":"strong_middle","type":"single",
            "kind":"code_output","prompt":"p","code":"println(1)","options":[{"id":"a","text":"1","correct":true},
            {"id":"b","text":"2","hint":"h"}],"explanation":"e"}]}
            """.trimIndent()
        )

        assertEquals("android.kotlin", file.topic)
        val q = file.questions.single()
        assertEquals("android.kotlin.001", q.id)
        assertEquals(Level.STRONG_MIDDLE, q.level)
        assertEquals(QuestionType.SINGLE, q.type)
        assertEquals(QuestionKind.CODE_OUTPUT, q.kind)
        assertEquals("println(1)", q.code)
        assertTrue(q.options[0].correct)
        assertNull(q.options[0].hint)
        assertFalse(q.options[1].correct)
        assertEquals("h", q.options[1].hint)
        assertEquals("e", q.explanation)
        assertFalse(q.reviewed)
    }

    @Test
    fun kindDefaultsToConceptAndCodeToNull() {
        val file = ContentParser.parseTopicFile(
            """
            {"topic":"t","questions":[{"id":"t.001","level":"junior","type":"multi","prompt":"p",
            "options":[{"id":"a","text":"1","correct":true}],"explanation":"e"}]}
            """.trimIndent()
        )

        val q = file.questions.single()
        assertEquals(QuestionKind.CONCEPT, q.kind)
        assertEquals(QuestionType.MULTI, q.type)
        assertNull(q.code)
    }

    @Test
    fun parsesCatalogWithPlannedTopic() {
        val catalog = ContentParser.parseCatalog(
            """
            {"version":1,
             "fields":[{"id":"ios","title":"iOS (Swift)","group":"mobile","topics":["ios.swift","core.dsa"]}],
             "topics":[{"id":"ios.swift","title":"Swift tili"},
                       {"id":"core.dsa","title":"Algoritmlar","file":"core_dsa.json"}]}
            """.trimIndent()
        )

        assertEquals(1, catalog.version)
        assertEquals(listOf("ios.swift", "core.dsa"), catalog.fields.single().topics)
        assertEquals("mobile", catalog.fields.single().group)
        assertNull(catalog.topics[0].file)
        assertEquals("core_dsa.json", catalog.topics[1].file)
    }

    @Test
    fun levelsAreOrderedBySeniority() {
        assertTrue(Level.JUNIOR < Level.MIDDLE)
        assertTrue(Level.MIDDLE < Level.STRONG_MIDDLE)
        assertTrue(Level.STRONG_MIDDLE < Level.SENIOR)
    }

    @Test(expected = SerializationException::class)
    fun unknownLevelFails() {
        ContentParser.parseTopicFile(
            """
            {"topic":"t","questions":[{"id":"t.001","level":"lead","type":"single","prompt":"p",
            "options":[{"id":"a","text":"1","correct":true}],"explanation":"e"}]}
            """.trimIndent()
        )
    }

    @Test
    fun unknownKeysAreIgnored() {
        val catalog = ContentParser.parseCatalog("""{"version":2,"fields":[],"topics":[],"note":"x"}""")

        assertEquals(2, catalog.version)
    }
}
