package com.niranjan.ticktick.feature.habits

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.sin

/** Plays only after a saved incomplete → complete transition, never on opening old history. */
@Composable
internal fun HabitCelebration(identity: String, completed: Boolean) {
    val context = LocalContext.current
    var previous by remember(identity) { mutableStateOf(completed) }
    val progress = remember(identity) { Animatable(1f) }
    LaunchedEffect(identity, completed) {
        val celebrate = completed && !previous
        previous = completed
        if (celebrate && Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f) {
            progress.snapTo(0f); progress.animateTo(1f, tween(1400))
        } else if (!completed) progress.snapTo(1f)
    }
    if (progress.value < 1f) Canvas(Modifier.fillMaxSize().zIndex(1f).clearAndSetSemantics { }) {
        val colors = listOf(Color(0xFFFFBE3D), Color(0xFF4778FF), Color(0xFFFF6380), Color(0xFF40B991))
        repeat(42) { index ->
            val t = progress.value
            val x = size.width * ((index * 17 % 43) / 43f) + sin(t * 9f + index) * 22f
            val y = size.height * (-0.1f + t * (0.8f + (index % 5) * 0.1f))
            drawCircle(colors[index % colors.size].copy(alpha = 1f - t), 3.dp.toPx(), Offset(x, y))
        }
    }
}
