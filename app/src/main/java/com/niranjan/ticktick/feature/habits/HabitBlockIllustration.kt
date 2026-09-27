package com.niranjan.ticktick.feature.habits

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale

/** Local geometric rendition of the supplied check-in illustration; no raster assets or network. */
@Composable
internal fun HabitBlockIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 384f, size.height / 180f, pivot = Offset.Zero) {
            fun face(color: Long, vararg points: Pair<Float, Float>) {
                drawPath(Path().apply {
                    points.forEachIndexed { index, (x, y) -> if (index == 0) moveTo(x, y) else lineTo(x, y) }
                    close()
                }, Color(color))
            }
            fun stud(x: Float, y: Float, top: Long, side: Long) {
                drawRect(Color(side), Offset(x - 14, y), Size(28f, 9f))
                drawOval(Color(side), Offset(x - 14, y + 2), Size(28f, 14f))
                drawOval(Color(top), Offset(x - 14, y - 7), Size(28f, 14f))
            }
            face(0xFF476FCF, 0f to 18f, 155f to 18f, 226f to 80f, 157f to 161f, 0f to 161f)
            // Rear green brick.
            face(0xFF00EF30, 226f to 12f, 296f to 47f, 226f to 83f, 156f to 48f)
            face(0xFF00D82E, 156f to 48f, 226f to 83f, 226f to 133f, 156f to 98f)
            face(0xFF00E529, 226f to 83f, 296f to 47f, 296f to 98f, 226f to 133f)
            stud(226f, 25f, 0xFF00F532, 0xFF00DB2B)
            stud(258f, 45f, 0xFF00F131, 0xFF00DC2B)
            stud(226f, 62f, 0xFF00EB2C, 0xFF00D629)
            // Front red base, with a yellow brick stacked above it.
            face(0xFFE84950, 88f to 80f, 157f to 115f, 157f to 161f, 88f to 126f)
            face(0xFFFF4B53, 157f to 115f, 226f to 80f, 226f to 126f, 157f to 161f)
            face(0xFFFFD127, 88f to 34f, 157f to 0f, 226f to 34f, 157f to 69f)
            face(0xFFF4AB00, 88f to 34f, 157f to 69f, 157f to 115f, 88f to 80f)
            face(0xFFFFC000, 157f to 69f, 226f to 34f, 226f to 80f, 157f to 115f)
            stud(157f, 16f, 0xFFFFD52F, 0xFFF5B500)
            stud(125f, 30f, 0xFFFFD82B, 0xFFF3B000)
            stud(189f, 30f, 0xFFFFD62A, 0xFFF4B000)
            stud(157f, 47f, 0xFFFFD42A, 0xFFF5B300)
        }
    }
}
