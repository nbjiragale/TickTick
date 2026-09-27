package com.niranjan.ticktick.feature.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niranjan.ticktick.domain.model.AppSound
import com.niranjan.ticktick.domain.model.SoundChoice
import com.niranjan.ticktick.domain.repository.taskWriteResult
import com.niranjan.ticktick.platform.sounds.AppSounds
import kotlinx.coroutines.launch

@Composable
fun SoundSettings(sounds: AppSounds) {
    Column {
        Text("Sounds", style = MaterialTheme.typography.titleMedium)
        AppSound.entries.forEach { SoundSetting(sounds, it) }
        Text("Popup reminders use the popup tone once. Other reminders use the notification tone. Phone volume, silent mode and Do Not Disturb still apply.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SoundSetting(sounds: AppSounds, sound: AppSound) {
    val context = LocalContext.current
    val choices by sounds.choices.collectAsStateWithLifecycle()
    val choice = choices.getValue(sound)
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        // Some picker implementations return no data on cancel; only an explicit null URI means Silent.
        if (result.resultCode == Activity.RESULT_OK && data?.hasExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) == true) {
            val selected = if (Build.VERSION.SDK_INT >= 33) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                else { @Suppress("DEPRECATION") data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) }
            val isDefault = selected == RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val label = if (selected == null) "Silent" else if (isDefault) "System default" else try {
                RingtoneManager.getRingtone(context, selected)?.getTitle(context) ?: "Selected sound"
            } catch (_: Exception) { "Selected sound" }
            saving = true
            scope.launch {
                try {
                    taskWriteResult { sounds.save(sound, SoundChoice(if (isDefault) "" else selected?.toString(), label)) }
                        .onSuccess { error = null }.onFailure { error = "Couldn't save this sound. Tap to choose it again." }
                } finally { saving = false }
            }
        }
    }
    Column(Modifier.fillMaxWidth().clickable(enabled = !saving, role = Role.Button) {
        try {
            picker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, sound.label)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, sounds.uri(choice.uri)))
        } catch (_: Exception) { error = "Android's sound picker isn't available on this device." }
    }.padding(vertical = 12.dp)) {
        Text(sound.label, style = MaterialTheme.typography.bodyLarge)
        Text(if (saving) "Saving…" else choice.label, style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
