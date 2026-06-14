package com.mima.feltwords

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mima.feltwords.ui.root.RootScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 主题切换逻辑已移至 RootScaffold 内部（由天气状态驱动）
        setContent {
            RootScaffold()
        }
    }
}
