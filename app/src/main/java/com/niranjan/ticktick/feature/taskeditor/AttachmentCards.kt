package com.niranjan.ticktick.feature.taskeditor

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.readLocalImage
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.TaskAttachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AttachmentCards(attachments: List<TaskAttachment>, onDelete: (String) -> Unit, onError: (String) -> Unit, isLoading: Boolean = false) {
    if (attachments.isEmpty() && !isLoading) return
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isLoading) Text("Adding attachment…", style = MaterialTheme.typography.bodyMedium, color = EditorBlue)
        attachments.forEach { attachment ->
            key(attachment.id) {
                val isImage = attachment.mimeType.startsWith("image/")
                val shape = RoundedCornerShape(12.dp)
                Column(Modifier.fillMaxWidth().clip(shape).background(Color.White)
                    .border(1.dp, Color(0xFFE9EAEE), shape)) {
                    fun open() {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW)
                                .setDataAndType(Uri.parse(attachment.uri), attachment.mimeType)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                        }.onFailure { onError("This attachment can't be opened. Choose it again or install an app that supports it.") }
                    }
                    if (isImage) {
                        Box(Modifier.fillMaxWidth().height(156.dp).background(TickTickColors.Background)
                            .clickable(role = Role.Button, onClickLabel = "Open ${attachment.name}", onClick = ::open),
                            contentAlignment = Alignment.Center) {
                            AttachmentThumbnail(attachment)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val symbol = when {
                            isImage -> AppSymbol.Image
                            attachment.mimeType.startsWith("audio/") -> AppSymbol.Microphone
                            else -> AppSymbol.Note
                        }
                        AppIcon(symbol, Modifier.size(22.dp), EditorBlue)
                        Column(Modifier.weight(1f).clickable(role = Role.Button, onClick = ::open).padding(10.dp)) {
                            Text(attachment.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            val kind = when {
                                isImage -> "Image"
                                attachment.mimeType == "application/pdf" -> "PDF"
                                attachment.mimeType.startsWith("audio/") -> "Audio"
                                else -> "File"
                            }
                            val size = attachment.sizeBytes?.let { " · ${Formatter.formatShortFileSize(context, it)}" }.orEmpty()
                            Text(kind + size, style = MaterialTheme.typography.bodySmall, color = TickTickColors.SecondaryText)
                        }
                        EditorIconButton(AppSymbol.Trash, "Delete attachment ${attachment.name}", { onDelete(attachment.id) }, tint = EditorMuted)
                    }
                }
            }
        }
    }
}

private sealed interface Thumbnail {
    data object Loading : Thumbnail
    data object Unavailable : Thumbnail
    data class Ready(val bitmap: Bitmap) : Thumbnail
}

@Composable
private fun AttachmentThumbnail(attachment: TaskAttachment) {
    val context = LocalContext.current
    val thumbnail by produceState<Thumbnail>(Thumbnail.Loading, attachment.uri) {
        value = Thumbnail.Loading
        value = try {
            withContext(Dispatchers.IO) { Thumbnail.Ready(readLocalImage(context, Uri.parse(attachment.uri))) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Thumbnail.Unavailable
        }
    }
    when (val result = thumbnail) {
        is Thumbnail.Ready -> Image(result.bitmap.asImageBitmap(), attachment.name, Modifier.fillMaxWidth().height(156.dp), contentScale = ContentScale.Fit)
        else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(AppSymbol.Image, Modifier.size(30.dp), EditorMuted)
            Text(if (result == Thumbnail.Loading) "Loading image…" else "Preview unavailable",
                style = MaterialTheme.typography.bodySmall, color = TickTickColors.SecondaryText)
        }
    }
}
