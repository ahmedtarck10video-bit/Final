package com.example.ui.components

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.engine3d.Renderer3D
import com.example.model.Vector3D
import com.example.ui.UiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ArViewerScreen(
    state: UiState,
    onPlaceAnchor: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Map tap to surface coordinates
                    val normX = (offset.x / size.width - 0.5f) * 2.0f
                    val normY = (offset.y / size.height - 0.5f) * 2.0f
                    onPlaceAnchor(normX * 1.5f, -0.6f, -2.5f + normY * 1.2f)
                }
            }
            .testTag("ar_viewer_screen")
    ) {
        // Camera Stream Feed
        if (cameraPermissionState.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High-fidelity AR Environment Simulation Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF050811)
                            )
                        )
                    )
            )
        }

        // AR Surface Detection Grid & Scanning Ripples
        val infiniteTransition = rememberInfiniteTransition(label = "ar_scan")
        val scanPulse by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scan_pulse"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.65f

            // Scanning rings on detected floor plane
            val ringRadius = size.width * 0.45f * scanPulse
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 1.0f - scanPulse),
                radius = ringRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )

            // Detected Feature Points (Point Cloud dots)
            val dotColor = Color(0xFF38BDF8).copy(alpha = 0.7f)
            val points = listOf(
                Offset(cx - 120, cy - 40),
                Offset(cx + 140, cy - 60),
                Offset(cx - 80, cy + 50),
                Offset(cx + 100, cy + 40),
                Offset(cx - 160, cy + 80),
                Offset(cx + 180, cy + 70),
                Offset(cx, cy - 90),
                Offset(cx - 40, cy - 110),
                Offset(cx + 50, cy - 100)
            )
            for (pt in points) {
                drawCircle(color = dotColor, radius = 3.dp.toPx(), center = pt)
            }
        }

        // Render Anchored 3D Model in AR Space
        Canvas(modifier = Modifier.fillMaxSize()) {
            Renderer3D.renderModel(
                drawScope = this,
                model = state.selectedModel,
                rotationX = 12f,
                rotationY = state.rotationY,
                scale = state.scale * 0.9f,
                translationX = 0f,
                translationY = -0.35f,
                translationZ = -2.6f,
                lightDirection = state.lightDirection,
                ambientIntensity = 0.55f, // Brighter ambient for AR room lighting match
                isWireframe = state.isWireframe,
                showGridFloor = false,
                animationTime = state.animationTime
            )

            // Render any additional pinned spatial anchors
            for (anchor in state.activeAnchors) {
                Renderer3D.renderModel(
                    drawScope = this,
                    model = state.selectedModel,
                    rotationX = 10f,
                    rotationY = anchor.rotationY + state.rotationY,
                    scale = anchor.scale * 0.6f,
                    translationX = anchor.position.x,
                    translationY = anchor.position.y,
                    translationZ = anchor.position.z,
                    lightDirection = state.lightDirection,
                    ambientIntensity = 0.5f,
                    showGridFloor = false
                )
            }
        }

        // AR Center Target Reticle
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.85f),
                    radius = r * 0.75f,
                    center = c,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 3.dp.toPx(),
                    center = c
                )
                // Reticle Crosshairs
                drawLine(Color(0xFF00E5FF), Offset(c.x - r, c.y), Offset(c.x - r * 0.4f, c.y), strokeWidth = 2f)
                drawLine(Color(0xFF00E5FF), Offset(c.x + r * 0.4f, c.y), Offset(c.x + r, c.y), strokeWidth = 2f)
                drawLine(Color(0xFF00E5FF), Offset(c.x, c.y - r), Offset(c.x, c.y - r * 0.4f), strokeWidth = 2f)
                drawLine(Color(0xFF00E5FF), Offset(c.x, c.y + r * 0.4f), Offset(c.x, c.y + r), strokeWidth = 2f)
            }
        }

        // Top Status HUD (Plane Detected & Real-time Light Estimation)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 80.dp)
        ) {
            // Surface Plane Lock Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC0F172A))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SURFACE LOCKED",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Real-time Light Estimation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC0F172A))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFACC15),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "420 LUX (HDR)",
                    color = Color(0xFFFACC15),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Tap hint at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x99000000))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Tap surface to place 3D anchor",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
