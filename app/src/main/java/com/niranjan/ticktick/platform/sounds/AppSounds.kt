package com.niranjan.ticktick.platform.sounds

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.niranjan.ticktick.domain.model.AppSound
import com.niranjan.ticktick.domain.model.SoundChoice
import com.niranjan.ticktick.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

class AppSounds(private val context: Context, private val preferences: UiStateRepository,
    private val habits: HabitRepository, private val scope: CoroutineScope) {
    val choices = combine(preferences.snapshot, habits.snapshot) { prefs, habitSnapshot ->
        AppSound.entries.associateWith { sound -> read(prefs, sound, habitSnapshot.settings.ringtoneUri, habitSnapshot.settings.ringtone) }
    }.stateIn(scope, SharingStarted.Eagerly, AppSound.entries.associateWith { current(it) })
    private var playback: Job? = null // Accessed only on the main thread.

    private fun read(snapshot: UiSnapshot, sound: AppSound, legacyUri: String?, legacyLabel: String): SoundChoice {
        val fallback = when (sound) {
            AppSound.Notification -> SoundChoice(legacyUri, if (legacyUri == "") "System default" else if (legacyUri == null) "Silent" else legacyLabel)
            AppSound.Popup -> SoundChoice("", "System default")
            else -> SoundChoice(null, "Silent")
        }
        val payload = snapshot.preferences[sound.key] ?: return fallback
        return try {
            val json = JSONObject(payload)
            require(json.getInt("version") == 1)
            SoundChoice(if (json.isNull("uri")) null else json.getString("uri"), json.getString("label"))
        } catch (_: Exception) { fallback }
    }

    fun current(sound: AppSound): SoundChoice = habits.snapshot.value.settings.let {
        read(preferences.snapshot.value, sound, it.ringtoneUri, it.ringtone)
    }

    suspend fun awaitReady() {
        check(preferences.storeState.first { it != TaskStoreState.Loading } == TaskStoreState.Ready)
    }

    suspend fun save(sound: AppSound, choice: SoundChoice) {
        preferences.updatePreference(sound.key) {
            JSONObject().put("version", 1).put("uri", choice.uri ?: JSONObject.NULL).put("label", choice.label).toString()
        }
    }

    fun uri(value: String?): Uri? = when (value) {
        null -> null
        "" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        else -> Uri.parse(value)
    }

    /** Best-effort feedback after commit; playback can never fail an already saved action. */
    fun play(sound: AppSound) {
        scope.launch(Dispatchers.Main.immediate) {
            playback?.cancel()
            playback = launch tonePlayback@{
                var tone: android.media.Ringtone? = null
                try {
                    awaitReady()
                    val audio = context.getSystemService(AudioManager::class.java)
                    val notifications = context.getSystemService(NotificationManager::class.java)
                    if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
                        notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return@tonePlayback
                    val selected = uri(current(sound).uri) ?: return@tonePlayback
                    tone = RingtoneManager.getRingtone(context, selected) ?: return@tonePlayback
                    tone.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                    if (Build.VERSION.SDK_INT >= 28) tone.isLooping = false
                    tone.play()
                    delay(5_000) // Never leave a long ringtone playing indefinitely.
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* A removed/unavailable tone must not break task or habit actions. */ }
                finally { try { tone?.stop() } catch (_: Exception) { } }
            }
        }
    }
}
