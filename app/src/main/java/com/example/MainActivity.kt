package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppMode
import com.example.ui.MRViewModel
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MRViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                MixedRealityApp(
                    viewModel = viewModel,
                    onShareSnapshot = { bitmap ->
                        shareCapturedImage(bitmap)
                    }
                )
            }
        }
    }

    private fun shareCapturedImage(bitmap: Bitmap) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Captured in Mixed Reality Spatial Studio 🕶️✨")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Mixed Reality Snapshot")
            startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Sharing initialized", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun MixedRealityApp(
    viewModel: MRViewModel,
    onShareSnapshot: (Bitmap) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(data.visuals.message, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        containerColor = Color.Black,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Primary Viewport based on selected Mode
            AnimatedContent(
                targetState = uiState.currentMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "mode_viewport"
            ) { mode ->
                when (mode) {
                    AppMode.OBJECT -> {
                        ObjectViewerScreen(
                            state = uiState,
                            onRotate = { dx, dy -> viewModel.onRotate(dx, dy) },
                            onScale = { s -> viewModel.onScale(s) },
                            onPan = { dx, dy -> viewModel.onPan(dx, dy) },
                            onToggleAutoRotate = { viewModel.toggleAutoRotate() },
                            onToggleWireframe = { viewModel.toggleWireframe() },
                            onToggleGrid = { viewModel.toggleGridFloor() },
                            onOpenInspector = { viewModel.openInspector(true) },
                            onOpenDiagnostics = { viewModel.openDiagnostics(true) }
                        )
                    }
                    AppMode.AR -> {
                        ArViewerScreen(
                            state = uiState,
                            onPlaceAnchor = { x, y, z -> viewModel.placeAnchor(x, y, z) }
                        )
                    }
                    AppMode.MR -> {
                        MrStereoScreen(
                            state = uiState,
                            onIpdChanged = { ipd -> viewModel.setStereoIpd(ipd) }
                        )
                    }
                }
            }

            // 2. Camera Shutter Flash Overlay
            if (uiState.flashActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }

            // 3. Top Mode Switcher Pill [ MR | AR | Object ]
            TopModeSwitcher(
                currentMode = uiState.currentMode,
                onModeSelected = { newMode ->
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.setMode(newMode)
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 4. Bottom Control Bar [ PHOTO | (● REC) | Open | Clear ]
            BottomControlBar(
                isRecording = uiState.isRecording,
                recordingDuration = uiState.recordingDuration,
                onPhotoClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    // Create capture bitmap representation
                    val dummyBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(dummyBitmap)
                    canvas.drawColor(android.graphics.Color.parseColor("#0F172A"))
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#00E5FF")
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 6f
                    }
                    canvas.drawCircle(200f, 200f, 120f, paint)
                    viewModel.triggerPhotoCapture(dummyBitmap)
                },
                onRecClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    viewModel.toggleRecording()
                },
                onOpenClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.openModelPicker(true)
                },
                onClearClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.resetView()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // 5. Model Picker Modal Bottom Sheet
            ModelPickerSheet(
                isOpen = uiState.isModelPickerOpen,
                onDismiss = { viewModel.openModelPicker(false) },
                models = uiState.availableModels,
                selectedModel = uiState.selectedModel,
                onSelectModel = { model ->
                    viewModel.selectModel(model)
                },
                onImportGlb = { uri ->
                    viewModel.loadCustomGlb(uri, context)
                }
            )

            // 6. Scene & PBR Lighting Inspector Sheet
            InspectorSheet(
                isOpen = uiState.isInspectorOpen,
                onDismiss = { viewModel.openInspector(false) },
                state = uiState,
                onAmbientIntensityChange = { viewModel.setAmbientIntensity(it) },
                onLightAngleChange = { viewModel.setLightAngle(it) },
                onToggleWireframe = { viewModel.toggleWireframe() },
                onToggleGrid = { viewModel.toggleGridFloor() },
                onToggleAnimation = { viewModel.toggleAnimationPlayback() },
                onSpeedChange = { viewModel.setAnimationSpeed(it) },
                onResetView = { viewModel.resetView() }
            )

            // 7. Captured Photo Preview Dialog
            PhotoPreviewDialog(
                isOpen = uiState.isPhotoPreviewOpen,
                bitmap = uiState.capturedPhoto,
                onDismiss = { viewModel.closePhotoPreview() },
                onShare = {
                    uiState.capturedPhoto?.let { bmp ->
                        onShareSnapshot(bmp)
                    }
                }
            )

            // 8. Live Diagnostics / Telemetry Overlay
            DiagnosticsOverlay(
                isOpen = uiState.isDiagnosticsOpen,
                onDismiss = { viewModel.openDiagnostics(false) },
                state = uiState
            )
        }
    }
}
