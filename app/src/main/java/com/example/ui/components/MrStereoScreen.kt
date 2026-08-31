package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine3d.Renderer3D
import com.example.ui.UiState

@Composable
fun MrStereoScreen(
    state: UiState,
    onIpdChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("mr_stereo_screen")
    ) {
        // Dual Eye Stereoscopic Split
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Eye Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                EyeViewport(
                    state = state,
                    eyeLabel = "L",
                    stereoOffset = -state.stereoIpdOffset
                )
            }

            // Center Separator Line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF334155))
            )

            // Right Eye Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                EyeViewport(
                    state = state,
                    eyeLabel = "R",
                    stereoOffset = state.stereoIpdOffset
                )
            }
        }

        // Top IPD & Sensor Tracking Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC0F172A))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "MR STEREO ACTIVE (6-DOF HEAD-TRACK)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // IPD Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x990F172A))
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "IPD:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = state.stereoIpdOffset,
                    onValueChange = onIpdChanged,
                    valueRange = 0.05f..0.25f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF0288D1),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = "${(state.stereoIpdOffset * 500).toInt()} mm",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EyeViewport(
    state: UiState,
    eyeLabel: String,
    stereoOffset: Float
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 3D Canvas per eye
        Canvas(modifier = Modifier.fillMaxSize()) {
            Renderer3D.renderModel(
                drawScope = this,
                model = state.selectedModel,
                rotationX = state.rotationX,
                rotationY = state.rotationY,
                scale = state.scale * 0.75f,
                translationX = state.translationX,
                translationY = state.translationY,
                translationZ = state.translationZ,
                lightDirection = state.lightDirection,
                ambientIntensity = state.ambientIntensity,
                isWireframe = state.isWireframe,
                showGridFloor = state.showGridFloor,
                animationTime = state.animationTime,
                stereoOffset = stereoOffset
            )
        }

        // Stereo Lens Reticle
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color(0x6600E5FF),
                    radius = 8.dp.toPx(),
                    center = c,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 2.dp.toPx(),
                    center = c
                )
            }
        }

        // Eye Indicator Pill
        Text(
            text = eyeLabel,
            color = Color(0x66FFFFFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        )
    }
}
