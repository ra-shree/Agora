package com.newoether.agora.ui.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newoether.agora.R
import com.newoether.agora.viewmodel.RagManager

/** Fixed trailing action slot ≥76 dp × 48 dp, shared by every download-card state. */
private val downloadActionWidth = 76.dp
private val downloadActionHeight = 48.dp

/**
 * Pinned one-tap entry for the default RAG embedding model (EmbeddingGemma 2). The whole row is
 * inert; the trailing action slot owns the crossfade between the Download button and the
 * determinate/indeterminate progress states. Rendered only while no registered model carries the
 * pinned default-model id — once installed, the ordinary model row represents it.
 */
@Composable
fun EmbeddingGemmaDownloadCard(
    ragManager: RagManager,
    modifier: Modifier = Modifier,
) {
    val downloading = ragManager.isDownloadingDefaultModel.collectAsState()
    val progress = ragManager.defaultModelDownloadProgress.collectAsState()
    val error = ragManager.defaultModelError.collectAsState()
    SettingsItem(
        modifier = modifier,
        headlineContent = { Text(stringResource(R.string.embedding_gemma_name)) },
        supportingContent = {
            Crossfade(
                targetState = error.value.isNullOrBlank(),
                animationSpec = tween(250),
                label = "embeddingGemmaDownloadStatus",
            ) { healthy ->
                Text(
                    if (healthy) stringResource(R.string.embedding_gemma_desc)
                    else error.value.orEmpty(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = {
            Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Box(
                modifier = Modifier.size(width = downloadActionWidth, height = downloadActionHeight),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = downloading.value,
                    animationSpec = tween(250),
                    label = "embeddingGemmaDownloadAction",
                ) { downloadingNow ->
                    if (downloadingNow) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.heightIn(min = 24.dp), contentAlignment = Alignment.Center) {
                                val fraction = progress.value
                                if (fraction == null) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            // Percent only when the length is known; never invent a fraction.
                            val percent = progress.value?.let { "${(it * 100).toInt()}%" }
                            Text(
                                if (percent == null) stringResource(R.string.embedding_gemma_downloading)
                                else "${stringResource(R.string.embedding_gemma_downloading)} $percent",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    } else {
                        TextButton(
                            onClick = { ragManager.downloadDefaultEmbeddingModel() },
                        ) {
                            Text(
                                stringResource(R.string.embedding_gemma_download),
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        },
    )
}
