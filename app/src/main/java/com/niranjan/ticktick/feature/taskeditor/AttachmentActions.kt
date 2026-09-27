package com.niranjan.ticktick.feature.taskeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.niranjan.ticktick.platform.attachments.ManagedAttachmentStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class AttachmentSource { TakePhoto, ChoosePhoto, Records, File, ScanDocuments, ExistingScan }

/** Activity results belong to the originating editor, even if another draft opens meanwhile. */
@Composable
internal fun rememberAttachmentActions(vm: TaskEditorViewModel): (AttachmentSource) -> Unit {
    val context = LocalContext.current.applicationContext
    val attachmentStore = androidx.compose.runtime.remember(context) { ManagedAttachmentStore(context) }
    var pendingEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    fun attach(uri: Uri, editorId: String?, capturedPath: String? = null) {
        vm.importAttachment(editorId) {
            withContext(Dispatchers.IO) {
                attachmentStore.import(uri).also { if (capturedPath != null) attachmentStore.discardCapture(capturedPath) }
            }
        }
    }

    fun removeCancelledPhoto(path: String?) {
        if (path == null) return
        runCatching { attachmentStore.discardCapture(path) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val editorId = pendingEditorId
        pendingEditorId = null
        if (uri != null) attach(uri, editorId)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = pendingPhotoPath
        val editorId = pendingEditorId
        pendingPhotoPath = null
        pendingEditorId = null
        if (success && path != null && File(path).length() > 0) {
            attach(FileProvider.getUriForFile(context, "${context.packageName}.attachments", File(path)), editorId, path)
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
                    val photo = attachmentStore.newCaptureFile()
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
