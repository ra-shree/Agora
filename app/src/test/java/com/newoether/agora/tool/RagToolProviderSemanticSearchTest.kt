package com.newoether.agora.tool

import com.newoether.agora.api.EmbeddingClient
import com.newoether.agora.data.EmbeddingModelConfig
import com.newoether.agora.data.EmbeddingModelType
import com.newoether.agora.data.local.EmbeddingSearchRow
import com.newoether.agora.data.local.MessageEntity
import com.newoether.agora.data.repository.ConversationRepository
import java.io.File
import com.newoether.agora.model.MessageStatus
import com.newoether.agora.model.Participant
import com.newoether.agora.viewmodel.GenerationContext
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.every
import io.mockk.mockkObject
import com.newoether.agora.util.DebugLog
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Query-side prompt wiring: a gemma-formatted model must receive its official task prefix. */
class RagToolProviderSemanticSearchTest {
    private val conversations = mockk<ConversationRepository>()

    @After
    fun tearDown() = runTest { unmockkAll() }

    @Test
    fun gemmaQueryPrefixReachesTheEmbeddingInput() = runTest {
        // The native engine is device-only, so the same shared query-wrap is exercised through
        // the remote path that is mockable on the JVM; the local branch wraps identically.
        mockkObject(EmbeddingClient)
        mockkObject(DebugLog)
        every { DebugLog.w(any(), any()) } returns Unit
        every { DebugLog.d(any(), any()) } returns Unit
        var capturedQuery = ""
        coEvery { EmbeddingClient.computeEmbedding(any<String>(), any<String>(), any<String>(), any<String>()) } coAnswers {
            capturedQuery = firstArg<String>()
            floatArrayOf(1f, 0f, 0f)
        }
        coEvery {
            conversations.getEmbeddingSearchPage(any<String>(), any<Long>(), any<Int>(), any<Int>())
        } returns emptyList<EmbeddingSearchRow>()
        coEvery {
            conversations.getSearchableMessagesByIds(any<List<String>>())
        } returns emptyList<MessageEntity>()

        val config = EmbeddingModelConfig(
            id = "embedding",
            name = "EmbeddingGemma 2",
            type = EmbeddingModelType.REMOTE,
            remoteModelName = "text-embedding-3-small",
            promptFormat = EmbeddingModelConfig.PROMPT_FORMAT_EMBEDDING_GEMMA,
        )
        val provider = RagToolProvider(conversations)
        val results = provider.semanticSearch(
            query = "training dates",
            limit = 5,
            ctx = GenerationContext(
                activeEmbeddingConfig = config,
                ragThreshold = 0.5f,
                embeddingApiKey = "api-key",
            ),
        )

        assertEquals(emptyList<Pair<MessageEntity, Float>>(), results)
        assertEquals("task: search result | query: training dates", capturedQuery)
    }

    @Test
    fun localBranchFeedsTheEngineTheSameWrappedQueryAndBatchDocText() {
        // Behavior contract for the LOCAL branch; the native engine is device-only, so the
        // wiring is pinned at source level per the established Llama source-contract pattern.
        val toolProvider = locateMainJava()
            .resolve("com/newoether/agora/tool/RagToolProvider.kt")
            .readText()
        val search = toolProvider.substringAfter("suspend fun semanticSearch")
            .substringBefore("\n    fun ")
        val wrapped = search.indexOf("config.queryEmbeddingInput(query)")
        val llamaCall = search.indexOf("LlamaEngine.computeEmbedding(")
        val remoteCall = search.indexOf("EmbeddingClient.computeEmbedding(")
        assertTrue("wrapped=$wrapped llama=$llamaCall remote=$remoteCall",
            llamaCall in 0 until wrapped && wrapped < remoteCall)
    }

    @Test
    fun cacheWorkerAppliesTheDocumentPrefixToBothBranches() {
        val worker = locateMainJava()
            .resolve("com/newoether/agora/service/EmbeddingCacheWorker.kt")
            .readText()
        val section = worker.substringAfter("private suspend fun embedCandidates")
            .substringBefore("\n    private suspend fun ")
        val documentPrefix = section.indexOf("model.documentEmbeddingInput(")
        val branch = section.indexOf("model.type == EmbeddingModelType.LOCAL")
        assertTrue(documentPrefix in 0 until branch)
    }

    private fun locateMainJava(): File {
        var directory = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        repeat(8) {
            listOf(
                File(directory, "app/src/main/java"),
                File(directory, "src/main/java"),
            ).firstOrNull(File::isDirectory)?.let { return it }
            directory = directory.parentFile ?: error("Reached filesystem root")
        }
        error("Unable to locate the main java source directory")
    }
}
