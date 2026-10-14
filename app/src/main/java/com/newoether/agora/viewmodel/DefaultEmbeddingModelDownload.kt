package com.newoether.agora.viewmodel

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Streams a EmbeddingGemma 2 GGUF from [URL] into [dest], verifying the pinned SHA-256 before
 * completion. Mirrors the sandbox rootfs fetch: `HttpURLConnection` with 30 s connect / 60 s read
 * timeouts, following the HuggingFace `resolve/main` 302 to its CDN, a 64 KiB buffer, a
 * cancellation check per chunk, streaming hashing, and byte-ratio progress ([Float]? null when the
 * length is unknown). Checksum mismatch deletes [dest] and throws so nothing is registered.
 */
internal suspend fun downloadEmbeddingGguf(
    url: URL,
    dest: File,
    expectedSha256: String,
    onProgress: (Float?) -> Unit,
) {
    val conn = url.openConnection() as HttpURLConnection
    try {
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Agora")
        conn.connect()
        if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode} fetching EmbeddingGemma 2")
        val total = conn.contentLengthLong
        val digest = MessageDigest.getInstance("SHA-256")
        var downloaded = 0L
        conn.inputStream.use { input ->
            dest.outputStream().use { output ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    digest.update(buf, 0, n)
                    downloaded += n
                    onProgress(if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else null)
                }
            }
        }
        val hex = digest.digest().joinToString("") { "%02x".format(it) }
        if (!hex.equals(expectedSha256, ignoreCase = true)) {
            dest.delete()
            error("EmbeddingGemma 2 checksum mismatch (expected $expectedSha256, got $hex)")
        }
    } finally {
        conn.disconnect()
    }
}

/**
 * Download seam for the default RAG embedding model so tests can fake the HTTP path while the
 * registration/activation flow stays real. See `downloadDefaultEmbeddingModel` in RagManager.
 */
internal typealias DefaultModelDownloader = suspend (
    url: URL,
    dest: File,
    expectedSha256: String,
    onProgress: (Float?) -> Unit,
) -> Unit
