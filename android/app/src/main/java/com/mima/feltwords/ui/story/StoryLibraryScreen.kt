package com.mima.feltwords.ui.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mima.feltwords.domain.model.Storybook
import com.mima.feltwords.ui.AppViewModel
import com.mima.feltwords.ui.GeneratingStory
import com.mima.feltwords.ui.components.MascotEmptyState
import com.mima.feltwords.ui.theme.FeltTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * 绘本库页 —— 对齐 iOS StoryLibraryView。
 * 网格展示已完成绘本 + 顶部"生成中"占位卡片。
 * 支持抖动删除模式 + 撤销。
 */
@Composable
fun StoryLibraryScreen(
    appViewModel: AppViewModel,
    onOpenStory: (Storybook) -> Unit,
) {
    val stories by appViewModel.stories.collectAsState()
    val generatingStories by appViewModel.generatingStories.collectAsState()
    val felt = FeltTheme.colors

    var isDeleteMode by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }

    // 撤销机制
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // 存放最近一批被删除的绘本（index, story）
    var deletedBatch by remember { mutableStateOf<List<Pair<Int, Storybook>>>(emptyList()) }
    var undoJob by remember { mutableStateOf<Job?>(null) }

    fun deleteWithUndo(story: Storybook) {
        val index = stories.indexOf(story)
        deletedBatch = deletedBatch + (index to story)
        appViewModel.deleteStory(story.id)
        undoJob?.cancel()
        undoJob = scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "已删除 ${deletedBatch.size} 本绘本",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                appViewModel.restoreStories(deletedBatch)
            }
            deletedBatch = emptyList()
        }
    }

    val isEmpty = stories.isEmpty() && generatingStories.isEmpty()

    // 最近 4 本 + 更早的
    val recentStories = stories.take(4)
    val archivedStories = stories.drop(4)

    Box(modifier = Modifier.fillMaxSize().background(felt.cream)) {
        LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 标题行
                item(span = { GridItemSpan(2) }) {
                    HeaderRow(
                        storyCount = stories.size,
                        isDeleteMode = isDeleteMode,
                        onToggleDeleteMode = { isDeleteMode = !isDeleteMode },
                    )
                }

                // 生成中占位卡片
                items(generatingStories, key = { it.id }) { job ->
                    GeneratingCard(
                        job = job,
                        onRetry = { appViewModel.retryStoryGeneration(job.id) },
                        onDismiss = { appViewModel.dismissStoryJob(job.id) },
                    )
                }

                // 最近绘本
                items(recentStories, key = { it.id }) { story ->
                    StoryTile(
                        story = story,
                        isDeleteMode = isDeleteMode,
                        onClick = { if (!isDeleteMode) onOpenStory(story) },
                        onDelete = { deleteWithUndo(story) },
                    )
                }

                // 故事库展开/收起
                if (archivedStories.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        ArchiveHeader(
                            count = archivedStories.size,
                            expanded = showArchive,
                            onToggle = { showArchive = !showArchive },
                        )
                    }
                    if (showArchive) {
                        items(archivedStories, key = { it.id }) { story ->
                            StoryTile(
                                story = story,
                                isDeleteMode = isDeleteMode,
                                onClick = { if (!isDeleteMode) onOpenStory(story) },
                                onDelete = { deleteWithUndo(story) },
                            )
                        }
                    }
                }
        }

        if (isEmpty) {
            EmptyState()
        }

        // Snackbar 撤销提示
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
    storyCount: Int,
    isDeleteMode: Boolean,
    onToggleDeleteMode: () -> Unit,
) {
    val felt = FeltTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "我的绘本",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = felt.ink,
            )
            Text(
                text = "共 $storyCount 本小故事",
                style = MaterialTheme.typography.bodyMedium,
                color = felt.secondary,
            )
        }

        // 删除模式切换按钮
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
                contentDescription = if (isDeleteMode) "完成删除" else "管理绘本",
                tint = if (isDeleteMode) Color.Red else felt.orange,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ──────────────── 已完成绘本卡片 ────────────────

@Composable
private fun StoryTile(
    story: Storybook,
    isDeleteMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val felt = FeltTheme.colors

    // 抖动动画
    val infiniteTransition = rememberInfiniteTransition(label = "wobble_${story.id}")
    val wobbleAngle by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wobble",
    )
    val rotation = if (isDeleteMode) wobbleAngle else 0f

    Box {
        Column(
            modifier = Modifier
                .graphicsLayer { rotationZ = rotation }
                .shadow(6.dp, RoundedCornerShape(22.dp))
                .background(felt.surface, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .clickable(enabled = !isDeleteMode) { onClick() }
                .padding(10.dp),
        ) {
            // 封面图
            StoryCoverImage(
                imageUrl = story.pages.firstOrNull()?.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 标题
            Text(
                text = story.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = felt.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // focusWord 标签 + 页数
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = story.focusWord,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = felt.ink,
                    modifier = Modifier
                        .background(felt.yellow, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                // iOS：书页图标 + 页数（无「页」字）
                Icon(
                    Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = felt.secondary,
                    modifier = Modifier.size(11.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${story.pages.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = felt.secondary,
                )
            }
        }

        // 删除模式下的角标删除按钮
        if (isDeleteMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(30.dp)
                    .background(Color.Red.copy(alpha = 0.2f), CircleShape)
                    .clip(CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = Color.Red,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ──────────────── 生成中占位卡片 ────────────────

@Composable
private fun GeneratingCard(
    job: GeneratingStory,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val felt = FeltTheme.colors

    Column(
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .background(felt.surface, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .clickable { if (job.failed) onRetry() }
            .padding(10.dp),
    ) {
        // 封面区域 + 遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // 封面图（如有）
            StoryCoverImage(
                imageUrl = job.coverUrl,
                modifier = Modifier.fillMaxSize(),
            )
            // 暗色遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
            )
            // 状态指示
            if (job.failed) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "重试",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            } else {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 状态文案
        Text(
            text = if (job.failed) "生成失败" else "正在画绘本…",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = felt.ink,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = job.focusWord,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = felt.ink,
                modifier = Modifier
                    .background(felt.yellow, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(modifier = Modifier.weight(1f))

            if (job.failed) {
                // 失败时：轻点重试提示 + 关闭按钮
                Text(
                    text = "轻点重试",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = felt.secondary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = felt.secondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDismiss() },
                )
            } else {
                Text(
                    text = "生成中",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = felt.secondary,
                )
            }
        }
    }
}

// ──────────────── 故事库折叠区 ────────────────

@Composable
private fun ArchiveHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val felt = FeltTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(felt.surface, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.AutoStories,
            contentDescription = null,
            tint = felt.ink,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "故事库",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = felt.ink,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = felt.ink,
            modifier = Modifier
                .background(felt.mint, RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            tint = felt.ink,
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
        MascotEmptyState("还没有绘本", "拍一个物品，和毛毛一起\n生成第一本小故事")
    }
}

// ──────────────── 封面图通用组件 ────────────────

@Composable
fun StoryCoverImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val felt = FeltTheme.colors
    val context = LocalContext.current

    if (imageUrl != null) {
        val model = if (imageUrl.startsWith("/")) {
            ImageRequest.Builder(context)
                .data(File(imageUrl))
                .crossfade(true)
                .build()
        } else {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = model,
            contentDescription = "绘本封面",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        // 占位
        Box(
            modifier = modifier.background(felt.mint.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.AutoStories,
                contentDescription = null,
                tint = felt.secondary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
