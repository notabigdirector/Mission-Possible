package com.mission.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mission.app.data.entity.TaskConstants

@Composable
fun PriorityColor(priority: String): Color = when (priority) {
    TaskConstants.PRIORITY_HIGH -> Color(0xFFD32F2F)
    TaskConstants.PRIORITY_MEDIUM -> Color(0xFFF57C00)
    else -> Color(0xFF757575)
}