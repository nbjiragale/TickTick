package com.niranjan.ticktick.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.platform.backup.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun BackupSettings(manager: BackupManager) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                message = if (e is IllegalArgumentException) e.message ?: "The backup is invalid or exceeds a supported limit."
                    else "Backup operation failed. Check the file and available storage, then retry."
            }
            finally { busy = false }
        }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) run { manager.export(uri); message = "Backup exported. Media files are not included." }
    }
    val recovery = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) run { manager.export(uri, recovery = true); message = "Recovery backup exported." }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) run { preview = manager.preview(uri) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Backup & restore", style = MaterialTheme.typography.titleMedium)
        Text("Save an offline JSON copy of tasks, habits, history, drafts and settings. Attachment metadata is included; media files and access grants are not.")
        TextButton(enabled = !busy, onClick = { export.launch("todo-backup-${LocalDate.now()}.json") }) { Text("Export backup") }
        TextButton(enabled = !busy, onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Restore backup") }
        if (manager.recoveryAvailable) TextButton(enabled = !busy, onClick = { recovery.launch("todo-recovery-${LocalDate.now()}.json") }) { Text("Export pre-restore recovery copy") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    preview?.let { candidate ->
        AlertDialog(onDismissRequest = { if (!busy) preview = null }, title = { Text("Replace all saved data?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${candidate.tasks} tasks, ${candidate.habits} habits, ${candidate.focusSessions} Focus records.")
                Text("Exported ${candidate.createdAt}")
                Text("${candidate.unavailableMedia} attachment files cannot be opened on this device.")
                Text("Current data will be replaced. A recovery copy will be saved first. Running Focus timers will be restored paused.")
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            } },
            confirmButton = { TextButton(enabled = !busy, onClick = { run { manager.restore(candidate); preview = null } }) { Text("Replace and restore") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { preview = null }) { Text("Cancel") } })
    }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }) }
}
