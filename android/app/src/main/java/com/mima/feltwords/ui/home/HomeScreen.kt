package com.mima.feltwords.ui.home

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mima.feltwords.R
import com.mima.feltwords.domain.model.DailyTask
import com.mima.feltwords.ui.AppViewModel
import com.mima.feltwords.ui.MascotDailyTheme
import com.mima.feltwords.ui.theme.FeltTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(appViewModel: AppViewModel, onNavigateToTab: (Int) -> Unit) {
    val felt = FeltTheme.colors
    val context = LocalContext.current
    val temperature by appViewModel.weather.temperature.collectAsState()
    val city by appViewModel.weather.city.collectAsState()
    val avatar by appViewModel.avatarImage.collectAsState()
    val tasks by appViewModel.tasks.collectAsState()
    val stories by appViewModel.stories.collectAsState()
    val words by appViewModel.words.collectAsState()
    val history by appViewModel.history.collectAsState()
    var editingTasks by remember { mutableStateOf(false) }
    val reveal = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { loadBitmapFromUri(context, it) }?.let(appViewModel::setAvatar)
    }

    Box(Modifier.fillMaxSize().background(felt.cream)) {
        Column(Modifier.fillMaxSize()) {
            if (reveal.value > 1f) {
                MascotDailyStage(
                    theme = appViewModel.mascotDailyTheme,
                    recognized = appViewModel.todayHistoryCount,
                    wordCount = appViewModel.todayWordCount,
                    storyCount = appViewModel.todayStoryCount,
                    modifier = Modifier.height(reveal.value.dp),
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Header(temperature, appViewModel.weather.isDay) { appViewModel.weather.toggleLightDark() }
                Profile(
                    avatar = avatar,
                    city = city,
                    onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onDelete = appViewModel::deleteAvatar,
                )
                TasksCard(tasks, editingTasks, { editingTasks = !editingTasks }, appViewModel::updateTask)
                DiscoveryCards(history.size, words.size, stories.size, onNavigateToTab)
                Spacer(Modifier.height(4.dp))
            }
        }

        PullCord(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 92.dp)
                .offset { IntOffset(0, reveal.value.roundToInt()) }
                .alpha(1f - (reveal.value / 248f).coerceIn(0f, 1f))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, amount ->
                            scope.launch { reveal.snapTo((reveal.value + amount).coerceIn(0f, 248f)) }
                        },
                        onDragEnd = {
                            scope.launch { reveal.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 330f)) }
                        },
                    )
                },
        )
    }
}

@Composable
private fun PullCord(modifier: Modifier = Modifier) {
    val felt = FeltTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(3.dp).height(24.dp).background(felt.orange.copy(alpha = .75f), CircleShape))
        Box(
            Modifier.size(34.dp).background(felt.orange, CircleShape).shadow(5.dp, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Pets, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text("下拉看看毛毛", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = felt.secondary)
    }
}

@Composable
private fun MascotDailyStage(
    theme: MascotDailyTheme,
    recognized: Int,
    wordCount: Int,
    storyCount: Int,
    modifier: Modifier = Modifier,
) {
    val felt = FeltTheme.colors
    val image = drawableId(theme.assetName)
    Box(
        modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(felt.surface),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Image(painterResource(image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, felt.surface.copy(alpha = .96f)))))
        Column(Modifier.padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(theme.title, fontWeight = FontWeight.ExtraBold, color = felt.ink)
            Text(theme.subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = felt.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 7.dp)) {
                StatChip(recognized, "发现")
                StatChip(wordCount, "单词")
                StatChip(storyCount, "绘本")
            }
        }
    }
}

@Composable
private fun StatChip(value: Int, label: String) {
    val felt = FeltTheme.colors
    Column(
        Modifier.width(62.dp).background(felt.surface.copy(alpha = .82f), RoundedCornerShape(18.dp)).padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", fontWeight = FontWeight.ExtraBold, color = felt.ink)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = felt.secondary)
    }
}

@Composable
private fun Header(temperature: Int?, isDay: Boolean, onTheme: () -> Unit) {
    val felt = FeltTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("Hi, 小悠好～", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = felt.ink)
            Text("今天也去发现一个新单词", color = felt.secondary)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(62.dp).shadow(5.dp, CircleShape).background(felt.surface.copy(alpha = .78f), CircleShape).clickable(onClick = onTheme),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.LightMode, "切换主题", tint = if (isDay) felt.orange else felt.sky, modifier = Modifier.size(31.dp))
            }
            Text(temperature?.let { "$it°" } ?: "—", fontWeight = FontWeight.ExtraBold, color = felt.ink)
        }
    }
}

@Composable
private fun Profile(avatar: Bitmap?, city: String?, onPick: () -> Unit, onDelete: () -> Unit) {
    val felt = FeltTheme.colors
    val today = remember { Date() }
    val weekday = remember { SimpleDateFormat("EEEE", Locale.CHINA).format(today) }
    val date = remember { SimpleDateFormat("M月d日", Locale.CHINA).format(today) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Box {
            if (avatar == null) {
                Image(
                    painterResource(R.drawable.mascot_key_art),
                    "设置头像",
                    Modifier.size(150.dp).shadow(8.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).clickable(onClick = onPick),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Image(avatar.asImageBitmap(), "头像", Modifier.size(150.dp).shadow(8.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).clickable(onClick = onPick), contentScale = ContentScale.Crop)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).size(34.dp).background(if (avatar == null) felt.orange else Color(0xFFE95B4A), CircleShape).clickable(onClick = if (avatar == null) onPick else onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (avatar == null) Icons.Filled.Add else Icons.Filled.Delete, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
        }
        Column {
            Text(weekday, fontWeight = FontWeight.Bold, color = felt.secondary)
            Text(date, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = felt.ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = felt.secondary, modifier = Modifier.size(14.dp))
                Text(city ?: "定位中…", fontSize = 13.sp, color = felt.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TasksCard(tasks: List<DailyTask>, editing: Boolean, onEdit: () -> Unit, onUpdate: (Int, Int) -> Unit) {
    val felt = FeltTheme.colors
    Column(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)).background(felt.surface, RoundedCornerShape(24.dp)).padding(22.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("今日任务", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = felt.ink)
            Spacer(Modifier.weight(1f))
            Row(Modifier.clickable(onClick = onEdit).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Edit, null, tint = felt.orange, modifier = Modifier.size(16.dp))
                Text(if (editing) "完成" else "编辑", fontWeight = FontWeight.Bold, color = felt.orange)
            }
        }
        tasks.forEachIndexed { index, task ->
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(taskIcon(task.icon), null, tint = felt.ink, modifier = Modifier.size(22.dp))
                Text(task.prefix, Modifier.padding(start = 12.dp), color = felt.ink)
                Text("${task.count}", fontWeight = FontWeight.ExtraBold, color = felt.orange)
                Text(task.suffix, color = felt.ink)
                Spacer(Modifier.weight(1f))
                if (editing) {
                    Text("−", Modifier.clickable { onUpdate(index, task.count - 1) }.padding(6.dp), fontWeight = FontWeight.Bold)
                    Text("+", Modifier.clickable { onUpdate(index, task.count + 1) }.padding(6.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class Panel(val title: String, val hint: String, val summary: String, val color: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector, val image: Int, val tab: Int)

@Composable
private fun DiscoveryCards(history: Int, words: Int, stories: Int, onNavigate: (Int) -> Unit) {
    val felt = FeltTheme.colors
    val panels = listOf(
        Panel("开始拍照", "往左滑看我的绘本", "拍下身边的东西，认识一个新英文", felt.orange, Icons.Filled.CameraAlt, R.drawable.card_camera, 1),
        Panel("我的绘本", "左右滑查看更多", if (stories == 0) "还没有绘本，去生成第一本小故事" else "已经收藏 $stories 本小故事", felt.mint, Icons.Filled.AutoStories, R.drawable.card_stories, 2),
        Panel("单词本", "左右滑查看更多", if (words == 0) "把喜欢的英文收藏到这里" else "已经认识 $words 个英文", felt.pink, Icons.Filled.Translate, R.drawable.card_words, 3),
        Panel("历史记录", "往右滑回到开始拍照", if (history == 0) "识别完成后会自动保存，按时间排好" else "共 $history 条识别记录", felt.sky, Icons.Filled.History, R.drawable.card_history, 4),
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        panels.forEach { panel ->
            Box(
                Modifier.width(300.dp).height(176.dp).shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).clickable { onNavigate(panel.tab) },
            ) {
                Image(painterResource(panel.image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(panel.color.copy(alpha = .98f), panel.color.copy(alpha = .76f), panel.color.copy(alpha = .12f)))))
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Text(panel.hint, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = felt.ink.copy(alpha = .72f))
                    Spacer(Modifier.height(10.dp))
                    Text(panel.title, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = felt.ink)
                    Text(panel.summary, Modifier.width(210.dp).padding(top = 8.dp), fontSize = 13.sp, color = felt.ink.copy(alpha = .75f), maxLines = 2)
                    Spacer(Modifier.weight(1f))
                }
                Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).size(46.dp).background(felt.surface.copy(alpha = .65f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(panel.icon, null, tint = felt.ink, modifier = Modifier.size(25.dp))
                }
            }
        }
    }
}

private fun drawableId(name: String): Int = when (name) {
    "daily_eating" -> R.drawable.daily_eating
    "daily_learning" -> R.drawable.daily_learning
    "daily_playing" -> R.drawable.daily_playing
    "daily_tidying" -> R.drawable.daily_tidying
    "daily_bedtime" -> R.drawable.daily_bedtime
    else -> R.drawable.mascot_key_art
}

private fun taskIcon(name: String) = when (name) {
    "speaker.wave.2.fill" -> Icons.AutoMirrored.Filled.VolumeUp
    "book.fill" -> Icons.Filled.AutoStories
    else -> Icons.Filled.CameraAlt
}

private fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
} catch (_: Exception) {
    null
}
