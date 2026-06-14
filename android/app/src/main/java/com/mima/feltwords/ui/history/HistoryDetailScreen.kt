package com.mima.feltwords.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mima.feltwords.data.ServiceLocator
import com.mima.feltwords.domain.model.RecognitionHistoryItem
import com.mima.feltwords.ui.AppViewModel
import com.mima.feltwords.ui.components.AnimatedSpeakerIcon
import com.mima.feltwords.ui.theme.FeltTheme
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryDetailScreen(
    item: RecognitionHistoryItem,
    appViewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigateToStories: () -> Unit = {},
) {
    val felt = FeltTheme.colors
    val context = LocalContext.current
    val tts = remember { ServiceLocator.ttsManager }
    val isSpeaking by tts.isSpeaking.collectAsState()
    val words by appViewModel.words.collectAsState()
    val result = item.result
    val isSaved = words.any { it.word.equals(result.word, ignoreCase = true) }

    DisposableEffect(Unit) {
        onDispose { tts.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(felt.cream)
            .verticalScroll(rememberScrollState()),
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(felt.surface.copy(alpha = 0.7f), CircleShape)
                    .clip(CircleShape)
                    .clickable { onBack() }
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = felt.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Image
            val displayUrl = item.imageUrl ?: item.capturedImagePath
            if (displayUrl != null) {
                val model = ImageRequest.Builder(context)
                    .data(if (displayUrl.startsWith("/")) File(displayUrl) else displayUrl)
                    .crossfade(true)
                    .build()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp)),
                ) {
                    AsyncImage(
                        model = model,
                        contentDescription = "封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Word + speaker
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(felt.yellow, CircleShape)
                        .clip(CircleShape)
                        .clickable { tts.speak(result.word) },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedSpeakerIcon(
                        isSpeaking = isSpeaking,
                        tint = felt.ink,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Chinese name
            Text(
                text = result.displayNameZh,
                style = MaterialTheme.typography.titleMedium,
                color = felt.secondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Example sentence
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(felt.surface, RoundedCornerShape(22.dp))
                    .clickable { tts.speak("${result.word}. ${result.exampleSentence}") }
                    .padding(22.dp),
            ) {
                Text(
                    text = result.exampleSentence,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = felt.ink,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (result.exampleSentenceZh.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.exampleSentenceZh,
                        style = MaterialTheme.typography.bodyMedium,
                        color = felt.secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alternatives
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

            // Generate story button
            DetailButton(
                text = "生成小绘本",
                icon = Icons.Filled.AutoStories,
                color = felt.yellow,
                onClick = {
                    val reference = (item.imageUrl ?: item.capturedImagePath)
                        ?.takeIf { it.startsWith("/") }
                        ?.let { android.graphics.BitmapFactory.decodeFile(it) }
                    appViewModel.startStoryGeneration(
                        result = result,
                        reference = reference,
                        coverUrl = item.imageUrl,
                    )
                    onNavigateToStories()
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Save to wordbook button
            DetailButton(
                text = if (isSaved) "已加入单词本" else "加入单词本",
                icon = if (isSaved) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
                color = if (isSaved) felt.mint else felt.surface,
                enabled = !isSaved,
                onClick = { appViewModel.saveWord(result, item.imageUrl) },
            )
        }
    }
}

@Composable
private fun DetailButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val felt = FeltTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = if (enabled) color else color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(22.dp),
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = felt.ink, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = felt.ink)
        }
    }
}
