package com.niranjan.ticktick.feature.focus

import android.media.MediaPlayer
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.domain.model.Task
import java.util.Locale

val FocusBackground = Color(0xFFF3F3F9)
private val FocusBlue = Color(0xFF3975FF)
private val FocusMuted = Color(0xFFAAABB0)

@Composable
fun FocusScreen(state: FocusUiState, vm: FocusViewModel, tasks: List<Task>) {
    val session = state.session
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var soundUri by rememberSaveable { mutableStateOf<String?>(null) }
    var soundError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            soundUri = uri.toString(); soundError = null
        }.onFailure { soundError = "This audio file couldn't be opened. Choose another file."; panel = "sound" }
    }
    TimerDeviceEffects(session.keepScreenOn, soundUri, session.phase == FocusPhase.Running) {
        soundError = "This audio file couldn't be played. Choose another file."; soundUri = null; panel = "sound"
    }
    BackHandler(session.expanded && panel == null) { vm.expand(false) }
    val active = session.phase != FocusPhase.Ready
    val canConfigure = session.phase == FocusPhase.Ready || session.phase == FocusPhase.Finished
    val focusTitle = tasks.find { it.id == session.taskId }?.title ?: "Focus"
    BoxWithConstraints(Modifier.fillMaxSize().background(FocusBackground)) {
        val screenWidth = maxWidth
        // Reserve the normal navigation height even when expanded, preserving the reference's ring/control positions.
        val layoutHeight = (maxHeight - if (session.expanded) 64.dp else 0.dp).coerceAtLeast(630.dp)
        val totalHeight = layoutHeight + if (session.expanded) 64.dp else 0.dp
        val diameter = minOf(screenWidth * .74f, 340.dp, layoutHeight * .43f)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(totalHeight)) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (session.expanded) {
                        TimerIcon(AppSymbol.Chevron, "Minimize timer", { vm.expand(false) }, Modifier.rotate(90f))
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = vm::toggleScreenOn, modifier = Modifier.semantics {
                            contentDescription = "Keep screen on"; role = Role.Switch
                            toggleableState = androidx.compose.ui.state.ToggleableState(session.keepScreenOn)
                            stateDescription = if (session.keepScreenOn) "On" else "Off"
                        }) { AppIcon(AppSymbol.Sun, Modifier.size(25.dp), if (session.keepScreenOn) FocusBlue else Color(0xFF202124)) }
                    } else {
                        FocusMode.entries.forEach { mode ->
                            Column(Modifier.selectable(session.mode == mode, enabled = canConfigure, role = Role.Tab, onClick = { vm.mode(mode) })
                                .padding(end = if (mode == FocusMode.Pomo) 24.dp else 4.dp, top = 8.dp, bottom = 4.dp)) {
                                Text(mode.name, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                    color = if (session.mode == mode) Color(0xFF17181B) else Color(0xFF999A9E))
                                Box(Modifier.padding(top = 9.dp).width(if (mode == FocusMode.Pomo) 46.dp else 84.dp).height(3.dp)
                                    .clip(CircleShape).background(if (session.mode == mode) FocusBlue else Color.Transparent))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TimerIcon(AppSymbol.FocusStats, "Focus statistics", { panel = "stats" })
                        TimerIcon(AppSymbol.Plus, "Set focus duration", { panel = "duration" }, enabled = canConfigure && session.mode == FocusMode.Pomo)
                    }
                    Box {
                        TimerIcon(AppSymbol.More, "Timer options", { panel = "more" })
                        DropdownMenu(expanded = panel == "more", onDismissRequest = { panel = null }) {
                            DropdownMenuItem(text = { Text(if (session.keepScreenOn) "Disable screen always on" else "Enable screen always on") },
                                onClick = { vm.toggleScreenOn(); panel = null })
                            DropdownMenuItem(text = { Text("Focus duration") }, enabled = canConfigure && session.mode == FocusMode.Pomo, onClick = { panel = "duration" })
                            DropdownMenuItem(text = { Text("Focus sound") }, onClick = { panel = "sound" })
                            if (active) DropdownMenuItem(text = { Text(if (session.expanded) "Minimize timer" else "Expand timer") }, onClick = { vm.expand(!session.expanded); panel = null })
                        }
                    }
                }
                Row(Modifier.align(Alignment.TopCenter).offset(y = layoutHeight * .17f - 16.dp)
                    .widthIn(max = screenWidth - 48.dp).heightIn(min = 44.dp).clickable(role = Role.Button) { panel = "focus" },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(focusTitle, Modifier.widthIn(max = screenWidth - 92.dp), fontSize = 16.sp, color = Color(0xFF202124), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AppIcon(AppSymbol.Chevron, Modifier.size(18.dp), Color(0xFFD6D7DB))
                }
                Box(Modifier.align(Alignment.TopCenter).offset(y = layoutHeight * .45f - diameter / 2).size(diameter)
                    .clickable(enabled = active || session.mode == FocusMode.Pomo, role = Role.Button, onClickLabel = if (active) "Expand timer" else "Set duration") {
                        if (active) vm.expand(true) else if (session.mode == FocusMode.Pomo) panel = "duration"
                    }, contentAlignment = Alignment.Center) {
                    val progress by animateFloatAsState(state.progress, tween(200), label = "Focus progress")
                    Canvas(Modifier.fillMaxSize()) {
                        val width = 5.dp.toPx()
                        val inset = width / 2
                        drawCircle(Color(0xFFE7E8ED), radius = size.minDimension / 2 - inset, style = Stroke(width))
                        if (active) {
                            drawArc(FocusBlue, -90f, progress * 360f, useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                                size = androidx.compose.ui.geometry.Size(size.width - width, size.height - width), style = Stroke(width, cap = StrokeCap.Round))
                            val angle = Math.toRadians((-90 + progress * 360).toDouble())
                            val radius = size.minDimension / 2 - inset
                            drawCircle(FocusBlue, 3.dp.toPx(), center + androidx.compose.ui.geometry.Offset((kotlin.math.cos(angle) * radius).toFloat(), (kotlin.math.sin(angle) * radius).toFloat()))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val seconds = state.displaySeconds
                        Text(String.format(Locale.ENGLISH, "%02d : %02d", seconds / 60, seconds % 60),
                            fontSize = 44.sp, fontWeight = FontWeight.Normal, color = Color(0xFF101114),
                            modifier = Modifier.semantics { contentDescription = "$seconds seconds ${if (session.mode == FocusMode.Pomo) "remaining" else "elapsed"}" })
                        if (session.phase == FocusPhase.Finished) Text("Session complete", Modifier.padding(top = 10.dp), color = FocusMuted)
                        if (session.phase == FocusPhase.Paused) Text("Paused", Modifier.padding(top = 10.dp), color = FocusMuted)
                    }
                }
                Box(Modifier.align(Alignment.TopCenter).offset(y = layoutHeight * .8f - 32.dp).height(64.dp), contentAlignment = Alignment.Center) {
                    AnimatedContent(active, label = "Timer controls") { showControls ->
                        if (!showControls || session.phase == FocusPhase.Finished) {
                            Button(onClick = vm::start, modifier = Modifier.width(164.dp).height(48.dp), shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = FocusBlue)) { Text(if (session.phase == FocusPhase.Finished) "Start Again" else "Start", fontSize = 16.sp) }
                        } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            RoundControl(AppSymbol.Music, "Focus sound", 50, false, soundUri != null) { panel = "sound" }
                            RoundControl(if (session.phase == FocusPhase.Running) AppSymbol.Pause else AppSymbol.Play,
                                if (session.phase == FocusPhase.Running) "Pause timer" else "Resume timer", 66, true) {
                                if (session.phase == FocusPhase.Running) vm.pause() else vm.start()
                            }
                            RoundControl(AppSymbol.Stop, "Stop timer", 50, false) { panel = "stop" }
                        }
                    }
                }
                if (session.expanded) TextButton(onClick = { panel = "note" }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) {
                    Text(session.note.ifBlank { "Add Focus Note" }, Modifier.widthIn(max = screenWidth - 48.dp), color = FocusMuted,
                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    when (panel) {
        "stop" -> AlertDialog(onDismissRequest = { panel = null }, title = { Text("Stop focus?") },
            text = { Text(if (session.mode == FocusMode.Stopwatch) "Save this session and reset the stopwatch?" else "End this session and reset the timer?") },
            confirmButton = { TextButton(onClick = { vm.stop(); panel = null }) { Text("Stop") } },
            dismissButton = { TextButton(onClick = { panel = null }) { Text("Cancel") } })
        "duration" -> FocusInput("Focus duration", session.durationMinutes.toString(), true, { panel = null }) {
            val minutes = it.toIntOrNull()
            if (minutes == null || minutes !in 1..180) "Enter 1 to 180 minutes." else { vm.duration(minutes); panel = null; null }
        }
        "note" -> FocusInput("Focus note", session.note, false, { panel = null }) { vm.note(it.trim()); panel = null; null }
        "stats" -> AlertDialog(onDismissRequest = { panel = null }, title = { Text("Focus statistics") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${session.completedSessions} completed sessions")
                Text("${session.completedFocusMs / 60_000} minutes focused")
                Text("This app session", color = FocusMuted)
            } }, confirmButton = { TextButton(onClick = { panel = null }) { Text("Done") } })
        "focus" -> AlertDialog(onDismissRequest = { panel = null }, title = { Text("Focus on") }, text = {
            Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                TextButton(onClick = { vm.task(null); panel = null }) { Text("Focus · No task") }
                tasks.filter { it.isActive && !it.isNote }.forEach { task ->
                    TextButton(onClick = { vm.task(task.id); panel = null }) { Text(task.title) }
                }
            }
        }, confirmButton = { TextButton(onClick = { panel = null }) { Text("Cancel") } })
        "sound" -> AlertDialog(onDismissRequest = { panel = null }, title = { Text("Focus sound") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (soundUri == null) "Silent" else "Audio file selected")
                soundError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { soundUri = null; soundError = null; panel = null }) { Text("Silent") }
                TextButton(onClick = { panel = null; picker.launch(arrayOf("audio/*")) }) { Text("Choose audio file") }
            }
        }, confirmButton = { TextButton(onClick = { panel = null }) { Text("Done") } })
    }
}

@Composable
private fun TimerIcon(symbol: AppSymbol, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp).semantics { contentDescription = label }) {
        AppIcon(symbol, modifier.size(24.dp), if (enabled) Color(0xFF202124) else FocusMuted)
    }
}

@Composable
private fun RoundControl(symbol: AppSymbol, label: String, size: Int, primary: Boolean, selected: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(if (primary) FocusBlue else Color.Transparent)
        .then(if (primary) Modifier else Modifier.border(1.dp, if (selected) FocusBlue else FocusMuted, CircleShape))
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        AppIcon(symbol, Modifier.size(if (primary) 32.dp else 24.dp), if (primary) Color.White else if (selected) FocusBlue else FocusMuted)
    }
}

@Composable
private fun FocusInput(title: String, initial: String, numeric: Boolean, onDismiss: () -> Unit, onSave: (String) -> String?) {
    var value by rememberSaveable { mutableStateOf(initial) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        OutlinedTextField(value, { value = if (numeric) it.filter(Char::isDigit).take(3) else it; error = null },
            label = { Text(if (numeric) "Minutes" else "Note") }, keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
            singleLine = numeric, maxLines = if (numeric) 1 else 6, isError = error != null, supportingText = { error?.let { Text(it) } })
    }, confirmButton = { TextButton(onClick = { error = onSave(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun TimerDeviceEffects(keepOn: Boolean, soundUri: String?, running: Boolean, onAudioError: () -> Unit) {
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    DisposableEffect(view, keepOn, resumed) {
        val previous = view.keepScreenOn
        view.keepScreenOn = keepOn && resumed
        onDispose { view.keepScreenOn = previous }
    }
    val audioError by rememberUpdatedState(onAudioError)
    DisposableEffect(soundUri, running, resumed) {
        val player = if (soundUri != null && running && resumed) MediaPlayer() else null
        if (player != null) runCatching {
            player.setDataSource(context, Uri.parse(soundUri))
            player.isLooping = true
            player.setOnPreparedListener { it.start() }
            player.setOnErrorListener { _, _, _ -> audioError(); true }
            player.prepareAsync()
        }.onFailure { audioError() }
        onDispose { player?.setOnPreparedListener(null); player?.setOnErrorListener(null); player?.release() }
    }
}
