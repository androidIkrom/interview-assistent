package uz.devsuhbat.content

import kotlinx.serialization.json.Json

/** Both functions throw [kotlinx.serialization.SerializationException] on malformed input. */
object ContentParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseCatalog(json: String): Catalog = this.json.decodeFromString(json)

    fun parseTopicFile(json: String): TopicFile = this.json.decodeFromString(json)
}
