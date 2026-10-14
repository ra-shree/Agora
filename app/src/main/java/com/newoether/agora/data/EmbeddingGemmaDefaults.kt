package com.newoether.agora.data

/** Pinned identity and download source for the default RAG embedding model (EmbeddingGemma 2). */
object EmbeddingGemmaDefaults {
    const val MODEL_ID = "local-embeddinggemma-2"          // stable id → re-download replaces, never duplicates
    const val DISPLAY_NAME = "EmbeddingGemma 2"
    const val GGUF_FILE_NAME = "embeddinggemma-2-Q8_0.gguf" // stored in context.filesDir
    const val URL = "https://huggingface.co/ggml-org/embeddinggemma-2-GGUF/resolve/main/embeddinggemma-2-Q8_0.gguf"
    const val SHA256 = "2188ac1deca4b77dffefd603c2776a9d76d9d74ec01841392982ebb840b09135" // from HF tree lfs.oid
    const val SIZE_BYTES = 309_855_456L
    const val BATCH_SIZE = 8
    const val PROMPT_FORMAT = EmbeddingModelConfig.PROMPT_FORMAT_EMBEDDING_GEMMA
}
