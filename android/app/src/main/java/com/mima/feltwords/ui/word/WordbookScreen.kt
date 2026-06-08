package com.mima.feltwords.ui.word

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mima.feltwords.data.ServiceLocator
import com.mima.feltwords.domain.model.LearnedWord
import com.mima.feltwords.speech.TtsManager
import com.mima.feltwords.ui.AppViewModel
import com.mima.feltwords.ui.components.MascotEmptyState
import com.mima.feltwords.ui.components.feltPress
import com.mima.feltwords.ui.theme.FeltTheme
import kotlinx.coroutines.Job
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
    val backfillingWordIDs by appViewModel.backfillingWordIDs.collectAsState()
    val tts = remember { ServiceLocator.ttsManager }

    var isDeleteMode by remember { mutableStateOf(false) }
    var deletedBatch by remember { mutableStateOf<List<Pair<Int, LearnedWord>>>(emptyList()) }
    var undoJob by remember { mutableStateOf<Job?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun deleteWithUndo(word: LearnedWord) {
        val index = words.indexOf(word)
        deletedBatch = deletedBatch + (index to word)
        appViewModel.deleteWord(word.id)
        undoJob?.cancel()
        undoJob = scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "已删除 ${deletedBatch.size} 个单词",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                appViewModel.restoreWords(deletedBatch)
            }
            deletedBatch = emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(felt.cream),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderRow(
                isDeleteMode = isDeleteMode,
                onToggleDeleteMode = { isDeleteMode = !isDeleteMode },
            )
            if (words.isNotEmpty()) {
                // iOS 为 List(.plain)：整宽白行 + 分隔线，连成一块白底
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    item { HorizontalDivider(color = felt.ink.copy(alpha = 0.08f)) }
                    items(words, key = { it.id }) { word ->
                        WordRow(
                            word = word,
                            isDeleteMode = isDeleteMode,
                            isBackfilling = word.id in backfillingWordIDs,
                            onBackfill = { appViewModel.backfillWordImage(word.id) },
                            onTap = {
                                if (!isDeleteMode) tts.speak(word.word)
                            },
                            onDelete = { deleteWithUndo(word) },
                        )
                        HorizontalDivider(color = felt.ink.copy(alpha = 0.08f))
                    }
                }
            }
        }
        if (words.isEmpty()) EmptyState()

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
    isDeleteMode: Boolean,
    onToggleDeleteMode: () -> Unit,
) {
    val felt = FeltTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // iOS WordbookView 只用 navigationTitle「单词本」，无副标题
        Text(
            text = "单词本",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = felt.ink,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier
                .size(42.dp)
                .shadow(7.dp, CircleShape, spotColor = felt.orange.copy(alpha = .18f))
                .background(
                    if (isDeleteMode) Color(0xFFFFECE8) else felt.surface.copy(alpha = 0.76f),
                    CircleShape,
                )
                .border(
                    1.dp,
                    if (isDeleteMode) Color(0xFFFFA99D).copy(alpha = .55f) else Color.White.copy(alpha = .72f),
                    CircleShape,
                )
                .clip(CircleShape)
                .clickable { onToggleDeleteMode() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isDeleteMode) Icons.Rounded.Check else Icons.Outlined.Delete,
                contentDescription = if (isDeleteMode) "完成删除" else "管理单词",
                tint = if (isDeleteMode) Color(0xFFE95B4A) else felt.orange,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ──────────────── 单词行 ────────────────

@Composable
private fun WordRow(
    word: LearnedWord,
    isDeleteMode: Boolean,
    isBackfilling: Boolean,
    onBackfill: () -> Unit,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val felt = FeltTheme.colors
    // iOS List(.plain) + listRowBackground(surface)：整宽白行，无圆角无内缩
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(felt.surface)
            .feltPress(pressedScale = 0.98f, onClick = onTap)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 缩略图
        WordThumbnail(word = word, isBackfilling = isBackfilling, onBackfill = onBackfill)

        Spacer(modifier = Modifier.width(16.dp))

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
                    .size(38.dp)
                    .shadow(5.dp, CircleShape, spotColor = Color.Red.copy(alpha = .18f))
                    .background(felt.surface.copy(alpha = .86f), CircleShape)
                    .border(1.dp, Color(0xFFFFA99D).copy(alpha = .62f), CircleShape)
                    .clip(CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "删除",
                    tint = Color(0xFFE95B4A),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        AnimatedVisibility(visible = !isDeleteMode, enter = fadeIn(), exit = fadeOut()) {
            // iOS 单词本喇叭为纯橙图标、无圆底
            Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "朗读",
                    tint = felt.orange,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ──────────────── 缩略图 ────────────────

@Composable
private fun WordThumbnail(word: LearnedWord, isBackfilling: Boolean, onBackfill: () -> Unit) {
    val context = LocalContext.current
    val imageUrl = word.imageUrl

    Box(
        modifier = Modifier
            .size(72.dp)
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
        } else if (isBackfilling) {
            Box(Modifier.fillMaxSize().background(FeltTheme.colors.cream), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FeltTheme.colors.orange, modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
            }
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
                Text(
                    "点我补图",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .background(FeltTheme.colors.ink.copy(alpha = .58f), RoundedCornerShape(20.dp))
                        .clickable(onClick = onBackfill)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
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
