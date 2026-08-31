package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomControlBar(
    isRecording: Boolean,
    recordingDuration: Int,
    onPhotoClick: () -> Unit,
    onRecClick: () -> Unit,
    onOpenClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(bottom = 24.dp)
    ) {
        // Active Recording Badge
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val minutes = recordingDuration / 60
            val seconds = recordingDuration % 60
            val formattedTime = String.format("%02d:%02d", minutes, seconds)

            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "rec_alpha"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xDD1E293B))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = alpha))
                )
                Text(
                    text = "RECORDING $formattedTime",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        }

        // Bottom Pill Control Bar matching Spatial_Photo_20260830_140317.jpg
        Box(
            modifier = Modifier
                .shadow(16.dp, RoundedCornerShape(38.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(38.dp))
                .background(Color(0xFF9BA4B0).copy(alpha = 0.85f)) // Slate grey pill
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("bottom_control_bar")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // PHOTO Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPhotoClick
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .testTag("photo_button")
                ) {
                    Text(
                        text = "PHOTO",
                        color = Color(0xFF1E242E),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // (● REC) Button - Red Circle
                val recScale by animateFloatAsState(
                    targetValue = if (isRecording) 1.08f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.6f),
                    label = "rec_scale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(62.dp)
                        .scale(recScale)
                        .shadow(8.dp, CircleShape, spotColor = Color(0xFFE53935))
                        .clip(CircleShape)
                        .background(Color(0xFFEA4335)) // Vibrant red
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRecClick
                        )
                        .testTag("rec_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // White center dot
                        Box(
                            modifier = Modifier
                                .size(if (isRecording) 10.dp else 12.dp)
                                .clip(if (isRecording) RoundedCornerShape(2.dp) else CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isRecording) "STOP" else "REC",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                // Open Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenClick
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .testTag("open_button")
                ) {
                    Text(
                        text = "Open",
                        color = Color(0xFF1E242E),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }

                // Clear Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClearClick
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .testTag("clear_button")
                ) {
                    Text(
                        text = "Clear",
                        color = Color(0xFF1E242E),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}
