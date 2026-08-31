package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppMode

@Composable
fun TopModeSwitcher(
    currentMode: AppMode,
    onModeSelected: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Glassy pill container matching Spatial_Photo_20260830_140317.jpg
    Box(
        modifier = modifier
            .padding(top = 16.dp)
            .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = Color.Black)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF8B94A0).copy(alpha = 0.65f)) // Translucent slate matching screenshot
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .testTag("top_mode_switcher")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModeItem(
                title = "MR",
                isSelected = currentMode == AppMode.MR,
                onClick = { onModeSelected(AppMode.MR) },
                testTag = "mode_mr_button"
            )
            ModeItem(
                title = "AR",
                isSelected = currentMode == AppMode.AR,
                onClick = { onModeSelected(AppMode.AR) },
                testTag = "mode_ar_button"
            )
            ModeItem(
                title = "Object",
                isSelected = currentMode == AppMode.OBJECT,
                onClick = { onModeSelected(AppMode.OBJECT) },
                testTag = "mode_object_button"
            )
        }
    }
}

@Composable
private fun ModeItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF6B7584) else Color.Transparent,
        animationSpec = tween(200),
        label = "mode_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF10141D) else Color(0xFF333A45),
        animationSpec = tween(200),
        label = "mode_text"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 9.dp)
            .testTag(testTag)
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}
