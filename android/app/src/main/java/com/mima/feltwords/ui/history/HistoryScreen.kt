package com.mima.feltwords.ui.history

import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mima.feltwords.data.ServiceLocator
import com.mima.feltwords.domain.model.RecognitionHistoryItem
import com.mima.feltwords.ui.components.AnimatedSpeakerIcon
import com.mima.feltwords.ui.components.FeltCard
import com.mima.feltwords.ui.AppViewModel
import com.mima.feltwords.ui.components.MascotEmptyState
import com.mima.feltwords.ui.theme.FeltTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史记录页 —— 对齐 iOS HistoryView。
 *
 * 功能：
 * - 历史卡片列表（图片 + 单词 + 中文 + 时间 + 朗读）
 * - 存入单词本按钮
 * - 生成绘本按钮
 * - 空态
 */
@Composable
fun HistoryScreen(
    appViewModel: AppViewModel,
    onNavigateToStories: () -> Unit = {},
    onOpenHistoryDetail: (RecognitionHistoryItem) -> Unit = {},
) {
    val felt = FeltTheme.colors
    val history by appViewModel.history.collectAsState()
    val words by appViewModel.words.collectAsState()
    val generatingIDs by appViewModel.generatingHistoryIDs.collectAsState()
    val tts = remember { ServiceLocator.ttsManager }
    val isSpeaking by tts.isSpeaking.collectAsState()
    var speakingWord by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(felt.cream),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "历史记录",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = felt.ink,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
            )
            if (history.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(history, key = { it.id }) { item ->
                        HistoryCard(
                            item = item,
                            isGenerating = item.id in generatingIDs,
                            isSavedToWordbook = words.any {
                                it.word.equals(item.result.word, ignoreCase = true)
                            },
                            isSpeaking = isSpeaking && speakingWord == item.result.word,
                            onSpeak = {
                                speakingWord = item.result.word
                                tts.speak(item.result.word)
                            },
                            onItemClick = { onOpenHistoryDetail(item) },
                            onSaveToWordbook = {
                                appViewModel.saveWord(item.result, item.imageUrl)
                            },
                            onGenerateStory = {
                                val reference = (item.imageUrl ?: item.capturedImagePath)
                                    ?.takeIf { it.startsWith("/") }
                                    ?.let(BitmapFactory::decodeFile)
                                appViewModel.startStoryGeneration(
                                    result = item.result,
                                    reference = reference,
                                    coverUrl = item.imageUrl,
                                )
                                onNavigateToStories()
                            },
                        )
                    }
                }
            }
        }
        if (history.isEmpty()) EmptyState()
    }
}

// ──────────────── 历史卡片 ────────────────

@Composable
private fun HistoryCard(
    item: RecognitionHistoryItem,
    isGenerating: Boolean,
    isSavedToWordbook: Boolean,
    isSpeaking: Boolean = false,
    onSpeak: () -> Unit,
    onItemClick: () -> Unit,
    onSaveToWordbook: () -> Unit,
    onGenerateStory: () -> Unit,
) {
    val felt = FeltTheme.colors
    val timeFormat = remember { SimpleDateFormat("M月d日 HH:mm", Locale.CHINA) }

    FeltCard(
        modifier = Modifier.fillMaxWidth().clickable { onItemClick() },
        cornerRadius = 24.dp,
        elevation = 6.dp,
        padding = 16.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
        // 上半部：图片 + 信息 + 朗读
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 图片：毛毡图优先 → 生成中 → 拍照原图回退 → 占位
            HistoryImage(
                imageUrl = item.imageUrl,
                capturedImagePath = item.capturedImagePath,
                isGenerating = isGenerating,
                modifier = Modifier
                    .size(80.dp)
                    .shadow(4.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp)),
            )

            Spacer(modifier = Modifier.width(14.dp))

            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.result.word,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = felt.ink,
                )
                Text(
                    text = item.result.displayNameZh,
                    style = MaterialTheme.typography.bodyMedium,
                    color = felt.secondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = felt.secondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeFormat.format(Date(item.recognizedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = felt.secondary,
                    )
                }
            }

            // 朗读按钮
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(felt.yellow, CircleShape)
                    .clip(CircleShape)
                    .clickable { onSpeak() },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedSpeakerIcon(
                    isSpeaking = isSpeaking,
                    tint = felt.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 下半部：操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 存入单词本
            ActionButton(
                text = if (isSavedToWordbook) "已在单词本" else "存入单词本",
                icon = if (isSavedToWordbook) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
                color = if (isSavedToWordbook) felt.mint.copy(alpha = 0.5f) else felt.mint,
                enabled = !isSavedToWordbook,
                onClick = onSaveToWordbook,
                modifier = Modifier.weight(1f),
            )

            // 生成绘本
            ActionButton(
                text = "生成绘本",
                icon = Icons.Filled.AutoStories,
                color = felt.yellow,
                enabled = true,
                onClick = onGenerateStory,
                modifier = Modifier.weight(1f),
            )
        }
        }
    }
}

// ──────────────── 操作按钮 ────────────────

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val felt = FeltTheme.colors

    Row(
        modifier = modifier
            .height(48.dp)
            .background(
                color = if (enabled) color else color.copy(alpha = 0.4f),
                shape = RoundedCornerShape(17.dp),
            )
            .clip(RoundedCornerShape(17.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = felt.ink,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = felt.ink,
        )
    }
}

// ──────────────── 历史图片 ────────────────

@Composable
private fun HistoryImage(
    imageUrl: String?,
    capturedImagePath: String?,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
) {
    val felt = FeltTheme.colors
    val context = LocalContext.current

    when {
        imageUrl != null -> {
            val model = if (imageUrl.startsWith("/")) {
                ImageRequest.Builder(context).data(File(imageUrl)).crossfade(true).build()
            } else {
                ImageRequest.Builder(context).data(imageUrl).crossfade(true).build()
            }
            AsyncImage(
                model = model,
                contentDescription = "毛毡封面",
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
        }
        isGenerating -> {
            Box(
                modifier = modifier.background(felt.cream),
                contentAlignment = Alignment.Center,
            ) {
                GeneratingContent(felt.orange)
            }
        }
        capturedImagePath != null -> {
            val model = ImageRequest.Builder(context).data(File(capturedImagePath)).crossfade(true).build()
            AsyncImage(
                model = model,
                contentDescription = "识别原图",
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
        }
        else -> {
            Box(
                modifier = modifier.background(felt.sky.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = felt.secondary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun GeneratingContent(tint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CircularProgressIndicator(
            color = tint,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "正在生成",
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

// ──────────────── 空态 ────────────────

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        MascotEmptyState("还没有识别记录", "拍照识别后，毛毛会帮你把发现排好")
    }
}
