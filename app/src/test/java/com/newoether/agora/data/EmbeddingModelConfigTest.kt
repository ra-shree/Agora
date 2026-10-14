package com.newoether.agora.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class EmbeddingModelConfigTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun modelsWithoutAPromptFormatIndexRawText() {
        val config = EmbeddingModelConfig(name = "Remote", type = EmbeddingModelType.REMOTE)
        assertEquals("same text", config.documentEmbeddingInput("same text"))
        assertEquals("same query", config.queryEmbeddingInput("same query"))
    }

    @Test
    fun emptyPromptFormatIndexesRawText() {
        val config = EmbeddingModelConfig(
            name = "Local",
            type = EmbeddingModelType.LOCAL,
            promptFormat = EmbeddingModelConfig.PROMPT_FORMAT_NONE,
        )
        assertEquals("body", config.documentEmbeddingInput("body"))
        assertEquals("query", config.queryEmbeddingInput("query"))
    }

    @Test
    fun gemmaFormatAppliesTheOfficialTaskPrefixes() {
        val config = EmbeddingModelConfig(
            name = "EmbeddingGemma 2",
            type = EmbeddingModelType.LOCAL,
            promptFormat = EmbeddingModelConfig.PROMPT_FORMAT_EMBEDDING_GEMMA,
        )
        assertEquals(
            "title: none | text: hello",
            config.documentEmbeddingInput("hello"),
        )
        assertEquals(
            "task: search result | query: hello",
            config.queryEmbeddingInput("hello"),
        )
    }

    @Test
    fun storedJsonWithoutPromptFormatDeserializesToRawText() {
        // Settings stored before "promptFormat" existed carry none of the key.
        val config = json.decodeFromString(
            EmbeddingModelConfig.serializer(),
            """{"id":"m","name":"N","type":"LOCAL","remoteModelName":"","remoteBaseUrl":""" +
                """"","remoteApiKey":"","localFilePath":"/sd/ext.gguf","batchSize":8}""",
        )
        assertEquals(EmbeddingModelConfig.PROMPT_FORMAT_NONE, config.promptFormat)
    }
}
