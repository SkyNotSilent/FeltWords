package com.mima.feltwords.ui.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mima.feltwords.ui.AppViewModel
import com.mima.feltwords.ui.theme.FeltTheme
import java.io.InputStream

/**
 * 拍照页 —— 对齐 iOS CameraScreen.swift。
 *
 * 流程：CameraX 实时预览 + 拍照/相册选图 → recognize → 跳转结果页。
 * 无相机权限或无相机硬件时自动引导到相册（对齐 iOS 模拟器回退）。
 *
 * 导航：使用 Compose 内部状态（capturedBitmap != null 时显示结果页），
 * 保持简单，不额外引入 Navigation-Compose 子图。
 */
@Composable
fun CaptureFlow(
    onNavigateHome: () -> Unit,
    appViewModel: AppViewModel,
    onNavigateToStories: () -> Unit = {},
    vm: CaptureViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()

    // 当 uiState 为 Success/Recognizing/Error 时，显示结果页
    when (uiState) {
        is CaptureViewModel.UiState.Idle -> {
            CameraScreen(
                onPhotoCaptured = { bitmap -> vm.recognize(bitmap) },
                onNavigateHome = onNavigateHome,
            )
        }
        is CaptureViewModel.UiState.Recognizing -> {
            WordResultScreen(
                vm = vm,
                appViewModel = appViewModel,
                onBack = { vm.resetToIdle() },
                onNavigateToStories = onNavigateToStories,
            )
        }
        is CaptureViewModel.UiState.Success -> {
            WordResultScreen(
                vm = vm,
                appViewModel = appViewModel,
                onBack = { vm.resetToIdle() },
                onNavigateToStories = onNavigateToStories,
            )
        }
        is CaptureViewModel.UiState.Error -> {
            WordResultScreen(
                vm = vm,
                appViewModel = appViewModel,
                onBack = { vm.resetToIdle() },
                onNavigateToStories = onNavigateToStories,
            )
        }
    }
}

@Composable
private fun CameraScreen(
    onPhotoCaptured: (Bitmap) -> Unit,
    onNavigateHome: () -> Unit,
) {
    val context = LocalContext.current

    // ── 相机权限 ──
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
    }

    // ── 相机硬件检测 ──
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }

    // ── 相册选择 ──
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                onPhotoCaptured(bitmap)
            }
        }
    }

    // 启动时请求权限
    LaunchedEffect(Unit) {
        if (hasCamera) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 相机预览层 —— 仅在有权限且有相机时显示
        if (permissionGranted && hasCamera) {
            CameraPreviewLayer(
                onPhotoCaptured = onPhotoCaptured,
                onNavigateHome = onNavigateHome,
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )
        } else {
            // 无相机/无权限回退层 —— 对齐 iOS 的 permissionDenied / cameraUnavailable 覆盖
            NoCameraOverlay(
                permissionDenied = permissionDenied,
                onNavigateHome = onNavigateHome,
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onOpenSettings = {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
            )
        }
    }
}

/**
 * CameraX 预览 + 拍照控制按钮
 */
@Composable
private fun CameraPreviewLayer(
    onPhotoCaptured: (Bitmap) -> Unit,
    onNavigateHome: () -> Unit,
    onPickPhoto: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    // 绑定 CameraX
    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            } catch (_: Exception) {
                // 相机绑定失败时静默处理
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (_: Exception) { /* ignore */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 对焦框
            FocusCorners()

            // 提示文字
            Text(
                text = "把物品放进小框里",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 34.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            // 底部控制栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 关闭按钮
                CameraControlButton(onClick = onNavigateHome) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
                }

                // 快门按钮
                ShutterButton(onClick = {
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                image.close()
                                onPhotoCaptured(bitmap)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                // 拍照失败静默处理
                            }
                        }
                    )
                })

                // 相册按钮
                CameraControlButton(onClick = onPickPhoto) {
                    Icon(Icons.Filled.Photo, contentDescription = "相册", tint = Color.White)
                }
            }
        }
    }
}

/**
 * 无相机/无权限覆盖层 —— 对齐 iOS 的 permissionDenied/cameraUnavailable 卡片
 */
@Composable
private fun NoCameraOverlay(
    permissionDenied: Boolean,
    onNavigateHome: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val felt = FeltTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectTapGestures { onNavigateHome() }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(28.dp)
                .pointerInput(Unit) {
                    // 吸收卡片内点击，防止穿透到背景
                    detectTapGestures { }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 右上角关闭
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = felt.ink.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onNavigateHome() },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = felt.ink,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "让毛毛看看你发现的物品",
                style = MaterialTheme.typography.titleMedium,
                color = felt.ink,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (permissionDenied) {
                    "请在系统设置中允许相机权限，或者从相册选择一张照片。"
                } else {
                    "这台设备暂时用不了相机，先从相册选择一张照片试试吧。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = felt.secondary,
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (permissionDenied) {
                FeltActionButton(
                    text = "打开设置",
                    color = felt.yellow,
                    onClick = onOpenSettings,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            FeltActionButton(
                text = "从相册选择",
                color = felt.cream,
                onClick = onPickPhoto,
            )
        }
    }
}

// ──────────────── 子组件 ────────────────

/** 快门按钮 —— 对齐 iOS ShutterButton */
@Composable
private fun ShutterButton(onClick: () -> Unit) {
    val felt = FeltTheme.colors
    Box(
        modifier = Modifier
            .size(84.dp)
            .shadow(8.dp, CircleShape)
            .background(Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .border(6.dp, felt.orange, CircleShape),
        )
    }
}

/** 圆形控制按钮 */
@Composable
private fun CameraControlButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(Color.White.copy(alpha = 0.24f), CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 对焦框四角 */
@Composable
private fun FocusCorners() {
    Box(
        modifier = Modifier
            .size(270.dp)
            .border(3.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(22.dp)),
    )
}

/** 毛毡风格操作按钮 */
@Composable
private fun FeltActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    val felt = FeltTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(color, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = felt.ink,
        )
    }
}

/** 从 Uri 加载 Bitmap */
private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val stream: InputStream? = context.contentResolver.openInputStream(uri)
        stream?.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
        null
    }
}
