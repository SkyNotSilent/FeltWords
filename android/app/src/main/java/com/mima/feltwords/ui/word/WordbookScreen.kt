package com.mima.feltwords.ui.word

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mima.feltwords.data.ServiceLocator
import com.mima.feltwords.domain.model.LearnedWord
import com.mima.feltwords.speech.TtsManager
import com.mima.feltwords.ui.AppViewModel
import com.mima.feltwords.ui.components.MascotEmptyState
import com.mima.feltwords.ui.theme.FeltTheme
import kotlinx.coroutines.launch
import java.io.File

/**
 * 单词本页 —— 对齐 iOS WordbookView。
 *
 * 功能：
 * - 单词列表（缩略图 + 单词 + 中文 + 例句）
 * - 点击朗读
 * - 删除模式 + 撤销
 * - 空态
 */
@Composable
fun WordbookScreen(
    appViewModel: AppViewModel,
) {
    val felt = FeltTheme.colors
    val words by appViewModel.words.collectAsState()
    val tts = remember { ServiceLocator.ttsManager }

    var isDeleteMode by remember { mutableStateOf(false) }
    var deletedBatch by remember { mutableStateOf<List<Pair<Int, LearnedWord>>>(emptyList()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(felt.cream),
    ) {
        if (words.isEmpty()) {
            // 空态
            EmptyState()
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题栏
                HeaderRow(
                    wordCount = words.size,
                    isDeleteMode = isDeleteMode,
                    onToggleDeleteMode = { isDeleteMode = !isDeleteMode },
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(words, key = { it.id }) { word ->
                        WordRow(
                            word = word,
                            isDeleteMode = isDeleteMode,
                            onTap = {
                                if (!isDeleteMode) tts.speak(word.word)
                            },
                            onDelete = {
                                val index = words.indexOf(word)
                                deletedBatch = deletedBatch + (index to word)
                                appViewModel.deleteWord(word.id)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "已删除 ${deletedBatch.size} 个单词",
                                        actionLabel = "撤销",
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        appViewModel.restoreWords(deletedBatch)
                                        deletedBatch = emptyList()
                                    } else {
                                        deletedBatch = emptyList()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = felt.surface,
                contentColor = felt.ink,
                actionColor = felt.orange,
                shape = RoundedCornerShape(22.dp),
            )
        }
    }
}

// ──────────────── 标题行 ────────────────

@Composable
private fun HeaderRow(
    wordCount: Int,
    isDeleteMode: Boolean,
    onToggleDeleteMode: () -> Unit,
) {
    val felt = FeltTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "单词本",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = felt.ink,
            )
            Text(
                text = "共 $wordCount 个单词",
                style = MaterialTheme.typography.bodyMedium,
                color = felt.secondary,
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isDeleteMode) Color.Red.copy(alpha = 0.12f) else felt.orange.copy(alpha = 0.12f),
                    CircleShape,
                )
                .clip(CircleShape)
                .clickable { onToggleDeleteMode() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isDeleteMode) Icons.Filled.Check else Icons.Filled.Delete,
                contentDescription = if (isDeleteMode) "完成删除" else "管理单词",
                tint = if (isDeleteMode) Color.Red else felt.orange,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ──────────────── 单词行 ────────────────

@Composable
private fun WordRow(
    word: LearnedWord,
    isDeleteMode: Boolean,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val felt = FeltTheme.colors
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "rowScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(22.dp))
            .background(felt.surface, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .clickable { onTap() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 缩略图
        WordThumbnail(word = word)

        Spacer(modifier = Modifier.width(14.dp))

        // 文字信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word.word,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = felt.ink,
            )
            Text(
                text = word.displayNameZh,
                style = MaterialTheme.typography.bodyMedium,
                color = felt.secondary,
            )
            Text(
                text = word.exampleSentence,
                style = MaterialTheme.typography.bodySmall,
                color = felt.secondary,
                maxLines = 1,
            )
        }

        // 右侧按钮
        AnimatedVisibility(visible = isDeleteMode, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Red.copy(alpha = 0.15f), CircleShape)
                    .clip(CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        AnimatedVisibility(visible = !isDeleteMode, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(felt.orange.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "朗读",
                    tint = felt.orange,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ──────────────── 缩略图 ────────────────

@Composable
private fun WordThumbnail(word: LearnedWord) {
    val context = LocalContext.current
    val imageUrl = word.imageUrl

    Box(
        modifier = Modifier
            .size(68.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            val model = if (imageUrl.startsWith("/")) {
                ImageRequest.Builder(context).data(File(imageUrl)).crossfade(true).build()
            } else {
                ImageRequest.Builder(context).data(imageUrl).crossfade(true).build()
            }
            AsyncImage(
                model = model,
                contentDescription = word.word,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // 分类占位
            val (emoji, bgColor) = placeholderStyle(word.category)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 28.sp)
            }
        }
    }
}

@Composable
private fun placeholderStyle(category: String): Pair<String, Color> {
    val felt = FeltTheme.colors
    val c = category.lowercase()
    return when {
        c.contains("food") || c.contains("fruit") || c.contains("食") -> "🍎" to felt.orange.copy(alpha = 0.2f)
        c.contains("animal") || c.contains("动物") -> "🐾" to felt.mint.copy(alpha = 0.3f)
        c.contains("plant") || c.contains("nature") || c.contains("自然") -> "🌿" to felt.mint.copy(alpha = 0.3f)
        c.contains("toy") || c.contains("玩") -> "🧸" to felt.pink.copy(alpha = 0.3f)
        c.contains("water") || c.contains("sky") || c.contains("水") -> "💧" to felt.sky.copy(alpha = 0.3f)
        else -> "✨" to felt.yellow.copy(alpha = 0.2f)
    }
}

// ──────────────── 空态 ────────────────

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        MascotEmptyState("还没有单词", "识别物品后，把喜欢的英文收藏在这里")
    }
}
