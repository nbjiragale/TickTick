package com.niranjan.ticktick.feature.tasks

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.core.designsystem.readLocalImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TaskBackground(options: TaskPresentation) {
    val index = options.swatch.coerceIn(backdropColors.indices)
    val brush = when (options.backdrop) {
        TaskBackdrop.Gradient -> Brush.verticalGradient(listOf(backdropTops[index], backdropColors[index]))
        TaskBackdrop.Color -> Brush.verticalGradient(listOf(backdropColors[index], backdropColors[index]))
        else -> Brush.verticalGradient(listOf(TickTickColors.Background, TickTickColors.Background))
    }
    Box(Modifier.fillMaxSize().background(brush)) {
        if (options.backdrop == TaskBackdrop.Image && options.imageUri != null) {
            val context = LocalContext.current
            var error by remember(options.imageUri) { mutableStateOf(false) }
            val bitmap by produceState<Bitmap?>(null, options.imageUri) {
                value = null
                try { value = withContext(Dispatchers.IO) { readLocalImage(context, Uri.parse(options.imageUri)) } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = true }
            }
            bitmap?.let {
                Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .3f)))
            }
            if (error) Text("Background unavailable · choose another image", Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 64.dp).background(Color.White).padding(8.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}
