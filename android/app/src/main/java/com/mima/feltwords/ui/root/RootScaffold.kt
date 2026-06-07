package com.mima.feltwords.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mima.feltwords.domain.model.Storybook
import com.mima.feltwords.ui.AppViewModel
import com.mima.feltwords.ui.capture.CaptureFlow
import com.mima.feltwords.ui.components.feltPress
import com.mima.feltwords.ui.history.HistoryScreen
import com.mima.feltwords.ui.home.HomeScreen
import com.mima.feltwords.ui.story.StoryLibraryScreen
import com.mima.feltwords.ui.story.StoryReaderScreen
import com.mima.feltwords.ui.theme.FeltTheme
import com.mima.feltwords.ui.theme.FeltWordsTheme
import com.mima.feltwords.ui.word.WordbookScreen

enum class FeltTab(val title: String, val icon: ImageVector, val index: Int) {
    Home("首页", Icons.Filled.Home, 0),
    Camera("拍一拍", Icons.Filled.CameraAlt, 1),
    Stories("绘本", Icons.Filled.AutoStories, 2),
    Words("单词", Icons.Filled.Translate, 3),
    History("历史", Icons.Filled.History, 4),
}

@Composable
fun RootScaffold() {
    // 共享 ViewModel：在 RootScaffold 层创建，向下传递
    val appViewModel: AppViewModel = viewModel()

    // 天气驱动主题
    val themeMode by appViewModel.weather.themeMode.collectAsState()
    val weatherIsDay by appViewModel.weather.isDayFlow.collectAsState()
    val darkOverride: Boolean? = when (themeMode) {
        com.mima.feltwords.data.weather.ThemeMode.Automatic -> !weatherIsDay
        com.mima.feltwords.data.weather.ThemeMode.Light -> false
        com.mima.feltwords.data.weather.ThemeMode.Dark -> true
    }

    FeltWordsTheme(darkOverride = darkOverride) {
        RootContent(appViewModel = appViewModel)
    }
}

@Composable
private fun RootContent(appViewModel: AppViewModel) {
    var selected by remember { mutableStateOf(FeltTab.Home) }
    val felt = FeltTheme.colors

    // 绘本阅读器的简单导航状态（null 时显示列表页，非 null 时显示阅读器）
    var openedStory by remember { mutableStateOf<Storybook?>(null) }

    Scaffold(
        containerColor = if (selected == FeltTab.Camera) Color.Black else felt.yellow,
        bottomBar = {
            if (selected != FeltTab.Camera) Surface(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = felt.surface.copy(alpha = 0.96f),
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 12.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                ) {
                FeltTab.entries.forEach { tab ->
                        BottomTab(
                            tab = tab,
                            selected = selected == tab,
                            modifier = Modifier.weight(1f),
                            onClick = {
                            if (selected == tab && tab == FeltTab.Stories) {
                                openedStory = null
                            }
                            selected = tab
                        },
                    )
                }
            }
            }
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (selected == FeltTab.Camera) PaddingValues(0.dp) else inner)
                .background(if (selected == FeltTab.Camera) Color.Black else felt.yellow),
            contentAlignment = Alignment.Center,
        ) {
            when (selected) {
                FeltTab.Home -> HomeScreen(
                    appViewModel = appViewModel,
                    onNavigateToTab = { index ->
                        selected = FeltTab.entries.firstOrNull { it.index == index } ?: FeltTab.Home
                    },
                )

                FeltTab.Camera -> CaptureFlow(
                    onNavigateHome = { selected = FeltTab.Home },
                    appViewModel = appViewModel,
                    onNavigateToStories = { selected = FeltTab.Stories },
                )

                FeltTab.Stories -> {
                    if (openedStory != null) {
                        StoryReaderScreen(
                            story = openedStory ?: return@Box,
                            onBack = { openedStory = null },
                        )
                    } else {
                        StoryLibraryScreen(
                            appViewModel = appViewModel,
                            onOpenStory = { story -> openedStory = story },
                        )
                    }
                }

                FeltTab.Words -> WordbookScreen(
                    appViewModel = appViewModel,
                )

                FeltTab.History -> HistoryScreen(
                    appViewModel = appViewModel,
                    onNavigateToStories = { selected = FeltTab.Stories },
                )
            }
        }
    }
}

@Composable
private fun BottomTab(
    tab: FeltTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val felt = FeltTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) felt.yellow.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .feltPress(pressedScale = 0.9f, onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(27.dp)
                .background(if (selected) felt.yellow.copy(alpha = 0.32f) else androidx.compose.ui.graphics.Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                tab.icon,
                contentDescription = tab.title,
                tint = if (selected) felt.orange else felt.secondary,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            tab.title,
            color = if (selected) felt.orange else felt.secondary,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 10.sp,
        )
    }
}
