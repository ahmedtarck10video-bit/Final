package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.Renderer3D
import com.example.ui.UiState

@Composable
fun ObjectViewerScreen(
    state: UiState,
    onRotate: (Float, Float) -> Unit,
    onScale: (Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    onToggleAutoRotate: () -> Unit,
    onToggleWireframe: () -> Unit,
    onToggleGrid: () -> Unit,
    onOpenInspector: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF050811),
                        Color(0xFF000000)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) {
                        onScale(zoom)
                    }
                    if (pan.x != 0f || pan.y != 0f) {
                        if (zoom == 1f) {
                            onRotate(pan.x, pan.y)
                        } else {
                            onPan(pan.x, pan.y)
                        }
                    }
                }
            }
            .testTag("object_viewer_screen")
    ) {
        // 3D Canvas Viewport
        Canvas(modifier = Modifier.fillMaxSize()) {
            Renderer3D.renderModel(
                drawScope = this,
                model = state.selectedModel,
                rotationX = state.rotationX,
                rotationY = state.rotationY,
                scale = state.scale,
                translationX = state.translationX,
                translationY = state.translationY,
                translationZ = state.translationZ,
                lightDirection = state.lightDirection,
                ambientIntensity = state.ambientIntensity,
                isWireframe = state.isWireframe,
                showGridFloor = state.showGridFloor,
                animationTime = state.animationTime,
                activeAnimationIndex = state.activeAnimationTrackIndex
            )
        }

        // Side Quick Action Toolbar
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            // Auto Rotate Toggle
            QuickActionBubble(
                icon = Icons.Default.Sync,
                isActive = state.autoRotate,
                contentDescription = "Auto Rotate",
                onClick = onToggleAutoRotate,
                testTag = "auto_rotate_button"
            )

            // Grid Floor Toggle
            QuickActionBubble(
                icon = Icons.Default.GridOn,
                isActive = state.showGridFloor,
                contentDescription = "Spatial Grid",
                onClick = onToggleGrid,
                testTag = "grid_toggle_button"
            )

            // Wireframe Toggle
            QuickActionBubble(
                icon = Icons.Default.Layers,
                isActive = state.isWireframe,
                contentDescription = "Wireframe Mode",
                onClick = onToggleWireframe,
                testTag = "wireframe_button"
            )

            // PBR & Lighting Inspector
            QuickActionBubble(
                icon = Icons.Default.Tune,
                isActive = false,
                contentDescription = "Lighting & Materials Inspector",
                onClick = onOpenInspector,
                testTag = "inspector_button"
            )

            // Telemetry / Diagnostics
            QuickActionBubble(
                icon = Icons.Default.Speed,
                isActive = false,
                contentDescription = "System Telemetry",
                onClick = onOpenDiagnostics,
                testTag = "diagnostics_button"
            )
        }

        // Top Left Model Info Badge
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 80.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x990F172A))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = state.selectedModel.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.selectedModel.polyCount} Tris",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "•",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    text = state.selectedModel.fileSizeFormatted,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun QuickActionBubble(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isActive) Color(0xFF0288D1) else Color(0xBB1E293B))
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.White else Color(0xFFCBD5E1),
            modifier = Modifier.size(20.dp)
        )
    }
}
