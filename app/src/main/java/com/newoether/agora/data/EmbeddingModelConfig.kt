package com.newoether.agora.data

import kotlinx.serialization.Serializable
import java.util.UUID

enum class EmbeddingModelType { REMOTE, LOCAL }

@Serializable
data class EmbeddingModelConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: EmbeddingModelType,
    val remoteModelName: String = "",
    val remoteBaseUrl: String = "",
    val remoteApiKey: String = "",
    val localFilePath: String = "",
    val batchSize: Int = 8,
    val promptFormat: String = PROMPT_FORMAT_NONE
) {
    /**
     * Formats raw message text into the input expected by this model. Models that do not
     * declare a prompt format index raw text unchanged.
     */
    fun documentEmbeddingInput(text: String): String =
        if (promptFormat == PROMPT_FORMAT_EMBEDDING_GEMMA) "title: none | text: $text" else text

    /** Formats a search query into the input expected by this model. */
    fun queryEmbeddingInput(text: String): String =
        if (promptFormat == PROMPT_FORMAT_EMBEDDING_GEMMA) "task: search result | query: $text" else text

    companion object {
        const val PROMPT_FORMAT_NONE = ""
        const val PROMPT_FORMAT_EMBEDDING_GEMMA = "embedding_gemma"
    }
}
