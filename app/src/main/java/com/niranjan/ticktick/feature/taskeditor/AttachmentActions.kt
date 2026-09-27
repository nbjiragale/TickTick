package com.niranjan.ticktick.feature.taskeditor

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.niranjan.ticktick.domain.model.TaskAttachment
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class AttachmentSource { TakePhoto, ChoosePhoto, Records, File, ScanDocuments, ExistingScan }

/** Activity results belong to the originating editor, even if another draft opens meanwhile. */
@Composable
internal fun rememberAttachmentActions(vm: TaskEditorViewModel): (AttachmentSource) -> Unit {
    val context = LocalContext.current.applicationContext
    var pendingEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    fun attach(uri: Uri, editorId: String?, persistAccess: Boolean) {
        vm.importAttachment(editorId) {
            withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    if (persistAccess) runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    var name = uri.lastPathSegment ?: "Attachment"
                    var size: Long? = null
                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameColumn >= 0 && !cursor.isNull(nameColumn)) name = cursor.getString(nameColumn)
                            if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn).takeIf { it >= 0 }
                        }
                    }
                    TaskAttachment(UUID.randomUUID().toString(), name, uri.toString(), resolver.getType(uri) ?: "application/octet-stream", size)
            }
        }
    }

    fun removeCancelledPhoto(path: String?) {
        if (path == null) return
        runCatching {
            val directory = File(context.filesDir, "attachments").canonicalFile
            val file = File(path).canonicalFile
            if (file.parentFile == directory && file.isFile) file.delete()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val editorId = pendingEditorId
        pendingEditorId = null
        if (uri != null) attach(uri, editorId, persistAccess = true)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = pendingPhotoPath
        val editorId = pendingEditorId
        pendingPhotoPath = null
        pendingEditorId = null
        if (success && path != null && File(path).length() > 0) {
            attach(FileProvider.getUriForFile(context, "${context.packageName}.attachments", File(path)), editorId, persistAccess = false)
        } else {
            removeCancelledPhoto(path)
            if (success && vm.uiState.value.active && vm.uiState.value.id == editorId) vm.reportError("The camera didn't return an image. Please try again.")
        }
    }

    return { source ->
        if (vm.uiState.value.attachmentLoading) {
            vm.reportError("Please wait for the attachment to finish loading.")
        } else if (source == AttachmentSource.ScanDocuments) {
            vm.panel(EditorPanel.ScanDocuments)
        } else {
            vm.panel(null)
            pendingEditorId = vm.uiState.value.id
            try {
                if (source == AttachmentSource.TakePhoto) {
                    val directory = File(context.filesDir, "attachments").apply { mkdirs() }
                    val photo = File.createTempFile("photo_", ".jpg", directory)
                    pendingPhotoPath = photo.absolutePath
                    camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.attachments", photo))
                } else {
                    picker.launch(when (source) {
                        AttachmentSource.ChoosePhoto -> arrayOf("image/*")
                        AttachmentSource.Records -> arrayOf("audio/*")
                        AttachmentSource.ExistingScan -> arrayOf("application/pdf", "image/*")
                        else -> arrayOf("*/*")
                    })
                }
            } catch (_: Exception) {
                removeCancelledPhoto(pendingPhotoPath)
                pendingPhotoPath = null
                pendingEditorId = null
                vm.reportError(if (source == AttachmentSource.TakePhoto) "No camera is available. Try Choose Photo instead." else "No file picker is available on this device.")
            }
        }
    }
}
