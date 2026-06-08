package com.mima.feltwords.ui.capture

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
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
import com.mima.feltwords.domain.model.RecognitionResult
import com.mima.feltwords.ui.AppViewModel
import com.mima.feltwords.ui.theme.FeltTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * 识别结果页 —— 对齐 iOS WordResultView.swift。
 * 显示：识别结果（单词/中文/例句）、朗读按钮、收藏、生成绘本入口。
 */
@Composable
fun WordResultScreen(
    vm: CaptureViewModel,
    appViewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigateToStories: () -> Unit = {},
) {
    val uiState by vm.uiState.collectAsState()
    val isSpeaking by vm.tts.isSpeaking.collectAsState()
    val felt = FeltTheme.colors
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(felt.cream)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── 顶部工具栏 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(felt.surface, CircleShape)
                    .clip(CircleShape)
                    .clickable { onBack() }
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "返回", tint = felt.ink)
            }
        }

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val state = uiState) {
                is CaptureViewModel.UiState.Error -> {
                    ErrorCard(message = state.message, onRetry = onBack)
                }
                is CaptureViewModel.UiState.Recognizing -> {
                    RecognizingContent(state.capturedBitmap)
                }
                is CaptureViewModel.UiState.Success -> {
                    SuccessContent(
                        state = state,
                        isSpeaking = isSpeaking,
                        onSpeakWord = { vm.tts.speak(state.result.word) },
                        onSpeakSentence = {
                            vm.tts.speak("${state.result.word}. ${state.result.exampleSentence}")
                        },
                        onSaveToWordbook = {
                            appViewModel.saveWord(state.result, state.feltImageUrl)
                            vm.markSavedToWordbook()
                        },
                        onStartStory = {
                            vm.markStoryGenerating()
                            appViewModel.startStoryGeneration(
                                result = state.result,
                                reference = state.capturedBitmap,
                                coverUrl = state.feltImageUrl,
                            )
                            scope.launch {
                                delay(420)
                                onNavigateToStories()
                            }
                        },
                    )
                }
                else -> { /* Idle 不应出现在这里 */ }
            }
        }
    }
}

// ──────────────── 识别成功内容 ────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuccessContent(
    state: CaptureViewModel.UiState.Success,
    isSpeaking: Boolean,
    onSpeakWord: () -> Unit,
    onSpeakSentence: () -> Unit,
    onSaveToWordbook: () -> Unit,
    onStartStory: () -> Unit,
) {
    val felt = FeltTheme.colors
    val result = state.result

    // ── 图片展示区（原图或毛毡图） ──
    ImageCard(state = state)

    Spacer(modifier = Modifier.height(20.dp))

    // ── "找到啦，是" ──
    Text(
        text = "找到啦，是",
        style = MaterialTheme.typography.bodyLarge,
        color = felt.secondary,
    )

    Spacer(modifier = Modifier.height(8.dp))

    // ── 单词 + 朗读按钮 ──
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = result.word,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = felt.ink,
        )
        Spacer(modifier = Modifier.width(12.dp))
        // 朗读单词按钮
        val speakScale by animateFloatAsState(
            targetValue = if (isSpeaking) 1.1f else 1f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = 500f,
            ),
            label = "speakScale",
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(speakScale)
                .background(felt.yellow, CircleShape)
                .clip(CircleShape)
                .clickable { onSpeakWord() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "朗读",
                tint = felt.ink,
                modifier = Modifier.size(24.dp),
            )
        }
    }

    // ── 中文名 ──
    Text(
        text = result.displayNameZh,
        style = MaterialTheme.typography.titleMedium,
        color = felt.secondary,
    )

    Spacer(modifier = Modifier.height(12.dp))

    // ── 自动存入历史标签 ──
    Box(
        modifier = Modifier
            .background(felt.surface, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = "已自动存入历史记录",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = felt.secondary,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ── 例句 ──
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(felt.surface, RoundedCornerShape(22.dp))
            .clickable { onSpeakSentence() }
            .padding(22.dp),
    ) {
        Text(
            text = result.exampleSentence,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = felt.ink,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ── 候选词（alternatives） ──
    if (result.alternatives.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            result.alternatives.take(3).forEach { alt ->
                Text(
                    text = alt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = felt.ink,
                    modifier = Modifier
                        .background(felt.surface, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // ── 生成绘本按钮 ──
    FeltButton(
        text = if (state.generatingStory) "正在生成绘本…" else "生成小绘本",
        icon = Icons.Filled.AutoStories,
        color = felt.yellow,
        enabled = !state.generatingStory,
        isLoading = state.generatingStory,
        onClick = onStartStory,
    )

    Spacer(modifier = Modifier.height(12.dp))

    // ── 收藏到单词本按钮 ──
    FeltButton(
        text = if (state.savedToWordbook) "已加入单词本" else "加入单词本",
        icon = if (state.savedToWordbook) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
        color = if (state.savedToWordbook) felt.mint else felt.surface,
        enabled = !state.savedToWordbook,
        onClick = onSaveToWordbook,
    )
}

// ──────────────── 图片卡片 ────────────────

@Composable
private fun ImageCard(state: CaptureViewModel.UiState.Success) {
    val context = LocalContext.current
    val felt = FeltTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ComparisonTile("你的照片", Modifier.weight(1f)) {
            Image(
                bitmap = state.capturedBitmap.asImageBitmap(),
                contentDescription = "拍摄的照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text("→", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = felt.orange)
        ComparisonTile("毛毡绘本", Modifier.weight(1f)) {
            val feltUrl = state.feltImageUrl
            if (feltUrl != null) {
                val model = ImageRequest.Builder(context)
                    .data(if (feltUrl.startsWith("/")) File(feltUrl) else feltUrl)
                    .crossfade(true)
                    .build()
                AsyncImage(model, "毛毡插图", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Image(
                    bitmap = state.capturedBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(14.dp),
                )
            }
            if (state.generatingFeltImage) {
                LoadingOverlay("毛毡化中…")
            }
        }
    }
}

// ──────────────── 识别中 ────────────────

@Composable
private fun RecognizingContent(bitmap: Bitmap) {
    val felt = FeltTheme.colors
    Row(
        Modifier.fillMaxWidth().height(190.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ComparisonTile("你的照片", Modifier.weight(1f)) {
            Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Text("→", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = felt.orange)
        ComparisonTile("毛毡绘本", Modifier.weight(1f)) {
            Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize().blur(14.dp), contentScale = ContentScale.Crop)
            LoadingOverlay("找单词中…")
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "毛毛正在认真看…",
        style = MaterialTheme.typography.titleMedium,
        color = felt.secondary,
    )
}

@Composable
private fun ComparisonTile(label: String, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val felt = FeltTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
            content = content,
        )
        Text(label, fontSize = 11.sp, color = felt.secondary, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun LoadingOverlay(caption: String) {
    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .32f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(25.dp), strokeWidth = 2.5.dp)
            Text(caption, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

// ──────────────── 错误卡片 ────────────────

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    val felt = FeltTheme.colors

    Spacer(modifier = Modifier.height(40.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(felt.surface, RoundedCornerShape(24.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = null,
            tint = felt.orange,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = felt.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        FeltButton(
            text = "重新拍照",
            color = felt.yellow,
            onClick = onRetry,
        )
    }
}

// ──────────────── 毛毡风格按钮 ────────────────

@Composable
private fun FeltButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val felt = FeltTheme.colors
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 500f,
        ),
        label = "buttonScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .background(
                color = if (enabled) color else color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(22.dp),
            )
            .clip(RoundedCornerShape(22.dp))
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        onClick()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = felt.ink,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(9.dp))
            } else if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = felt.ink,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = felt.ink,
            )
        }
    }
}
