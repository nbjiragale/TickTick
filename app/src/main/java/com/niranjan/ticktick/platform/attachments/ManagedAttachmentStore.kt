package com.niranjan.ticktick.platform.attachments

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.niranjan.ticktick.domain.model.TaskAttachment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject

/** Owns copies of imported media. Task and draft records, not individual attachment IDs, own files. */
class ManagedAttachmentStore(private val context: Context) {
    private val root = File(context.filesDir, "attachments")
    private val owned = File(root, "owned")
    private val staging = File(root, "staging")
    private val capture = File(root, "capture")
    private val authority = "${context.packageName}.attachments"

    fun newCaptureFile(): File {
        if (!capture.isDirectory && !capture.mkdirs()) throw IOException("Can't prepare photo storage.")
        return File.createTempFile("photo_", ".jpg", capture)
    }

    fun discardCapture(path: String?) {
        if (path == null) return
        val file = File(path).canonicalFile
        // The parent root also covers destinations created by earlier app versions.
        if (file.parentFile == capture.canonicalFile || file.parentFile == root.canonicalFile) file.delete()
    }

    suspend fun import(uri: Uri): TaskAttachment {
        val resolver = context.contentResolver
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Attachment"
        var mime = resolver.getType(uri) ?: "application/octet-stream"
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (column >= 0 && !cursor.isNull(column)) name = cursor.getString(column)
                }
            }
        }
        if (name.isBlank()) name = "Attachment"
        if (mime.isBlank()) mime = "application/octet-stream"
        if (!staging.isDirectory && !staging.mkdirs()) throw IOException("Can't prepare attachment storage.")
        if (!owned.isDirectory && !owned.mkdirs()) throw IOException("Can't prepare attachment storage.")
        val temporary = File.createTempFile("import_", ".tmp", staging)
        try {
            val source = resolver.openInputStream(uri) ?: throw IOException("The selected file is unavailable.")
            source.use { input -> FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            } }
            currentCoroutineContext().ensureActive()
            val extension = name.substringAfterLast('.', "").takeIf { it.length in 1..8 && it.all(Char::isLetterOrDigit) }
            val file = File(owned, UUID.randomUUID().toString() + (extension?.let { ".$it" } ?: ""))
            if (!temporary.renameTo(file)) throw IOException("Couldn't finish the attachment import.")
            val storedUri = FileProvider.getUriForFile(context, authority, file)
            return TaskAttachment(UUID.randomUUID().toString(), name, storedUri.toString(), mime, file.length())
        } finally {
            temporary.delete()
        }
    }

    /** Run only after both task and draft stores have loaded, once per process launch. */
    fun cleanOldOrphans(referencedUris: Collection<String>, draftPayload: String?) {
        val references = referencedUris.toMutableSet()
        if (draftPayload != null) {
            // An unreadable draft may still own a file. Preserve everything until recovery.
            val attachments = runCatching { JSONObject(draftPayload).getJSONArray("attachments") }.getOrNull() ?: return
            for (index in 0 until attachments.length()) {
                val uri = runCatching { attachments.getJSONObject(index).getString("uri") }.getOrNull() ?: return
                references += uri
            }
        }
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        for (directory in listOf(owned, staging, capture)) {
            directory.listFiles()?.filter { it.isFile && it.lastModified() in 1L until cutoff }?.forEach { file ->
                if (directory != owned && directory != capture ||
                    FileProvider.getUriForFile(context, authority, file).toString() !in references) file.delete()
            }
        }
        // Earlier versions placed camera destinations directly in attachments/.
        root.listFiles()?.filter { it.isFile && it.lastModified() in 1L until cutoff }?.forEach { file ->
            if (FileProvider.getUriForFile(context, authority, file).toString() !in references) file.delete()
        }
    }
}
