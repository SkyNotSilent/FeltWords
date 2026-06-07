package com.mima.feltwords.ui.story

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mima.feltwords.data.ServiceLocator
import com.mima.feltwords.domain.model.Storybook
import com.mima.feltwords.speech.TtsManager
import com.mima.feltwords.ui.theme.FeltTheme
import kotlinx.coroutines.launch

/**
 * 翻页阅读器 —— 对齐 iOS StoryReaderView。
 * 横向翻页，每页显示插画 + 句子。
 * 支持自动播放：朗读当前页，onFinish 后自动翻到下一页；末页结束停止。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StoryReaderScreen(
    story: Storybook,
    onBack: () -> Unit,
) {
    val felt = FeltTheme.colors
    val tts: TtsManager = remember { ServiceLocator.ttsManager }
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { story.pages.size })
    var isAutoPlaying by remember { mutableStateOf(false) }

    // 进入时朗读第一页
    LaunchedEffect(Unit) {
        if (story.pages.isNotEmpty()) {
            tts.speak(story.pages[0].sentence)
        }
    }

    // 离开时停止朗读
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
        }
    }

    // 手动滑页时朗读当前页（非自动播放模式下）
    LaunchedEffect(pagerState.currentPage) {
        if (!isAutoPlaying && story.pages.isNotEmpty()) {
            tts.speak(story.pages[pagerState.currentPage].sentence)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(felt.sky, felt.cream))
            ),
    ) {
        // 顶部导航栏
        TopBar(
            title = story.title,
            onBack = onBack,
        )

        // 翻页内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { pageIndex ->
            val page = story.pages[pageIndex]
            PageContent(
                imageUrl = page.imageUrl,
                sentence = page.sentence,
            )
        }

        // 页码指示器
        PageDots(
            pageCount = story.pages.size,
            currentPage = pagerState.currentPage,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 控制栏
        ControlBar(
            isAutoPlaying = isAutoPlaying,
            canGoPrevious = pagerState.currentPage > 0,
            canGoNext = pagerState.currentPage < story.pages.size - 1,
            onPrevious = {
                isAutoPlaying = false
                tts.stop()
                scope.launch {
                    pagerState.animateScrollToPage(
                        (pagerState.currentPage - 1).coerceAtLeast(0)
                    )
                }
            },
            onNext = {
                isAutoPlaying = false
                tts.stop()
                scope.launch {
                    pagerState.animateScrollToPage(
                        (pagerState.currentPage + 1).coerceAtMost(story.pages.size - 1)
                    )
                }
            },
            onTogglePlay = {
                if (isAutoPlaying) {
                    // 暂停
                    isAutoPlaying = false
                    tts.stop()
                } else {
                    // 开始自动播放
                    isAutoPlaying = true
                    // 如果在末页，回到第一页
                    if (pagerState.currentPage == story.pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                            playCurrentThenAdvance(
                                story = story,
                                pagerState = pagerState,
                                tts = tts,
                                isAutoPlaying = { isAutoPlaying },
                                setAutoPlaying = { isAutoPlaying = it },
                                scope = scope,
                            )
                        }
                    } else {
                        playCurrentThenAdvance(
                            story = story,
                            pagerState = pagerState,
                            tts = tts,
                            isAutoPlaying = { isAutoPlaying },
                            setAutoPlaying = { isAutoPlaying = it },
                            scope = scope,
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(18.dp))
    }
}

/**
 * 自动播放核心逻辑：朗读当前页，自然结束后自动翻到下一页并继续朗读。
 * 对齐 iOS StoryReaderView.playCurrentThenAdvance。
 *
 * TtsManager.speak 的 onFinish 只在自然结束时触发（被 stop/新 speak 打断时不触发），
 * 因此手动暂停/离开不会误翻页。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun playCurrentThenAdvance(
    story: Storybook,
    pagerState: androidx.compose.foundation.pager.PagerState,
    tts: TtsManager,
    isAutoPlaying: () -> Boolean,
    setAutoPlaying: (Boolean) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val currentPage = pagerState.currentPage
    if (currentPage >= story.pages.size) return

    tts.speak(story.pages[currentPage].sentence) {
        // onFinish：自然读完后
        if (!isAutoPlaying()) return@speak
        if (currentPage < story.pages.size - 1) {
            // 翻到下一页
            scope.launch {
                pagerState.animateScrollToPage(currentPage + 1)
                playCurrentThenAdvance(
                    story = story,
                    pagerState = pagerState,
                    tts = tts,
                    isAutoPlaying = isAutoPlaying,
                    setAutoPlaying = setAutoPlaying,
                    scope = scope,
                )
            }
        } else {
            // 末页读完，停止自动播放
            setAutoPlaying(false)
        }
    }
}

// ──────────────── 顶部导航栏 ────────────────

@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit,
) {
    val felt = FeltTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(felt.surface.copy(alpha = 0.7f), CircleShape)
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = felt.ink,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = felt.ink,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

// ──────────────── 单页内容 ────────────────

@Composable
private fun PageContent(
    imageUrl: String?,
    sentence: String,
) {
    val felt = FeltTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 插画
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .shadow(12.dp, RoundedCornerShape(28.dp)),
        ) {
            StoryCoverImage(
                imageUrl = imageUrl,
                modifier = Modifier.fillMaxSize(),
            )
            // 白色边框效果
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.0f),
                        RoundedCornerShape(28.dp),
                    ),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 句子文本
        Text(
            text = sentence,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = felt.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    felt.surface.copy(alpha = 0.85f),
                    RoundedCornerShape(22.dp),
                )
                .padding(horizontal = 16.dp, vertical = 18.dp),
        )
    }
}

// ──────────────── 页码指示器 ────────────────

@Composable
private fun PageDots(
    pageCount: Int,
    currentPage: Int,
) {
    val felt = FeltTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val width by animateDpAsState(
                targetValue = if (index == currentPage) 22.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = 500f,
                ),
                label = "dotWidth",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(width)
                    .height(8.dp)
                    .background(
                        if (index == currentPage) felt.orange
                        else felt.ink.copy(alpha = 0.18f),
                        RoundedCornerShape(50),
                    ),
            )
        }
    }
}

// ──────────────── 控制栏 ────────────────

@Composable
private fun ControlBar(
    isAutoPlaying: Boolean,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    val felt = FeltTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 上一页
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    felt.surface.copy(alpha = if (canGoPrevious) 0.7f else 0.3f),
                    CircleShape,
                )
                .clip(CircleShape)
                .clickable(enabled = canGoPrevious) { onPrevious() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = "上一页",
                tint = felt.ink.copy(alpha = if (canGoPrevious) 0.7f else 0.3f),
                modifier = Modifier.size(28.dp),
            )
        }

        // 播放/暂停按钮
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(8.dp, CircleShape)
                .background(felt.yellow, CircleShape)
                .clip(CircleShape)
                .clickable { onTogglePlay() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isAutoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isAutoPlaying) "暂停" else "播放",
                tint = felt.ink,
                modifier = Modifier.size(32.dp),
            )
        }

        // 下一页
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    felt.surface.copy(alpha = if (canGoNext) 0.7f else 0.3f),
                    CircleShape,
                )
                .clip(CircleShape)
                .clickable(enabled = canGoNext) { onNext() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "下一页",
                tint = felt.ink.copy(alpha = if (canGoNext) 0.7f else 0.3f),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
