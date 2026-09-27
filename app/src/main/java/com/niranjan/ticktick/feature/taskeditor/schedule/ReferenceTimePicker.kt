package com.niranjan.ticktick.feature.taskeditor.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.AppSymbol
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun ReferenceTimePicker(initial: LocalTime, onCancel: () -> Unit, onApply: (LocalTime) -> Unit, title: String = "Time", onClear: (() -> Unit)? = null) {
    var hour by rememberSaveable { mutableIntStateOf(initial.hour) }
    var minute by rememberSaveable { mutableIntStateOf(initial.minute) }
    var minuteMode by rememberSaveable { mutableStateOf(false) }
    var keyboard by rememberSaveable { mutableStateOf(false) }
    var hourInput by rememberSaveable { mutableStateOf((initial.hour % 12).let { if (it == 0) "12" else it.toString() }) }
    var minuteInput by rememberSaveable { mutableStateOf(initial.minute.toString().padStart(2, '0')) }
    var error by rememberSaveable { mutableStateOf(false) }
    val am = hour < 12
    fun choose(value: Int) {
        if (minuteMode) minute = value else hour = value % 12 + if (am) 0 else 12
    }
    ScheduleDialog(title, onCancel) {
        Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (keyboard) {
                OutlinedTextField(hourInput, { hourInput = it.filter(Char::isDigit).take(2); error = false }, Modifier.width(70.dp), label = { Text("Hour") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = error)
                Text(":", Modifier.padding(horizontal = 6.dp), color = ScheduleBlue, fontSize = 30.sp)
                OutlinedTextField(minuteInput, { minuteInput = it.filter(Char::isDigit).take(2); error = false }, Modifier.width(80.dp), label = { Text("Minute") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = error)
            } else {
                Text((hour % 12).let { if (it == 0) "12" else it.toString().padStart(2, '0') }, Modifier.clickable { minuteMode = false }, color = if (!minuteMode) ScheduleBlue else ScheduleBlue.copy(alpha = .58f), fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold)
                Text(" : ", color = ScheduleBlue.copy(alpha = .58f), fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold)
                Text(minute.toString().padStart(2, '0'), Modifier.clickable { minuteMode = true }, color = if (minuteMode) ScheduleBlue else ScheduleBlue.copy(alpha = .58f), fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text("AM", Modifier.clickable { hour %= 12 }.padding(vertical = 3.dp, horizontal = 2.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (am) ScheduleLabelBlue else ScheduleLabelBlue.copy(alpha = .6f))
                Text("PM", Modifier.clickable { hour = hour % 12 + 12 }.padding(vertical = 3.dp, horizontal = 2.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (!am) ScheduleLabelBlue else ScheduleLabelBlue.copy(alpha = .6f))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!keyboard) ClockDial(if (minuteMode) minute else hour % 12, minuteMode, ::choose, { minuteMode = true }, Modifier.align(Alignment.CenterHorizontally))
        if (error) Text("Enter an hour from 1–12 and minutes from 00–59.", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = ScheduleRed, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        DialogActions(onCancel, {
            if (keyboard) {
                val h = hourInput.toIntOrNull()
                val m = minuteInput.toIntOrNull()
                if (h == null || h !in 1..12 || m == null || m !in 0..59) error = true
                else onApply(LocalTime.of(h % 12 + if (am) 0 else 12, m))
            } else onApply(LocalTime.of(hour, minute))
        }, leading = {
            ScheduleIcon(if (keyboard) AppSymbol.Clock else AppSymbol.Keyboard, if (keyboard) "Use clock dial" else "Enter time with keyboard", {
                if (!keyboard) {
                    hourInput = (hour % 12).let { if (it == 0) "12" else it.toString() }
                    minuteInput = minute.toString().padStart(2, '0')
                } else {
                    val h = hourInput.toIntOrNull(); val m = minuteInput.toIntOrNull()
                    if (h != null && h in 1..12 && m != null && m in 0..59) { hour = h % 12 + if (am) 0 else 12; minute = m }
                }
                keyboard = !keyboard; error = false
            }, tint = ScheduleMuted)
            if (onClear != null) ScheduleTextButton("Clear", onClear)
        })
    }
}

@Composable
private fun ClockDial(value: Int, minutes: Boolean, onValue: (Int) -> Unit, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val change by rememberUpdatedState(onValue)
    val finish by rememberUpdatedState(onFinish)
    fun valueAt(point: Offset, width: Int, height: Int): Int {
        val angle = atan2((point.y - height / 2f).toDouble(), (point.x - width / 2f).toDouble()) + PI / 2
        val count = if (minutes) 60 else 12
        return ((angle / (2 * PI) * count).roundToInt() % count + count) % count
    }
    BoxWithConstraints(modifier.size(250.dp).clip(CircleShape).background(Color(0xFFF8F8FD))
        .pointerInput(minutes) { detectTapGestures { change(valueAt(it, size.width, size.height)); if (!minutes) finish() } }
        .pointerInput(minutes) {
            detectDragGestures(onDragStart = { change(valueAt(it, size.width, size.height)) }, onDragEnd = { if (!minutes) finish() }) { event, _ ->
                event.consume(); change(valueAt(event.position, size.width, size.height))
            }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            val angle = value.toDouble() / (if (minutes) 60 else 12) * 2 * PI - PI / 2
            val endpoint = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (size.width * .405f)
            drawLine(ScheduleLabelBlue, center, endpoint, 1.dp.toPx())
            drawCircle(ScheduleBlue, 20.dp.toPx(), endpoint)
            drawCircle(ScheduleBlue, 3.dp.toPx(), center)
        }
        val radius = maxWidth.value * .405f
        repeat(12) { index ->
            val number = if (minutes) index * 5 else if (index == 0) 12 else index
            val angle = index / 12.0 * 2 * PI - PI / 2
            Box(Modifier.align(Alignment.Center).offset(x = (cos(angle) * radius).dp, y = (sin(angle) * radius).dp).size(36.dp), contentAlignment = Alignment.Center) {
                Text(if (minutes) number.toString().padStart(2, '0') else number.toString(), fontSize = 14.sp, color = if (value == (if (minutes) number else index)) Color.White else ScheduleInk)
            }
        }
        if (minutes && value % 5 != 0) {
            val angle = value / 60.0 * 2 * PI - PI / 2
            Box(Modifier.align(Alignment.Center).offset(x = (cos(angle) * radius).dp, y = (sin(angle) * radius).dp).size(36.dp), contentAlignment = Alignment.Center) {
                Text(value.toString().padStart(2, '0'), fontSize = 14.sp, color = Color.White)
            }
        }
    }
}
