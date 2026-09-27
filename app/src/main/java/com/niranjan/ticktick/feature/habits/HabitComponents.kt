package com.niranjan.ticktick.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors

internal val HabitBlue = Color(0xFF3975FF)
internal val HabitMuted = Color(0xFFAAAAAA)
internal val HabitField = Color(0xFFF7F7F7)
internal val habitIcons = listOf(
    "😊", "🥤", "🍳", "🍚", "🍌", "🥕", "🍦", "🌷",
    "💤", "🚶", "🏃", "🧘", "🏋️", "🚴", "🏊", "🛁",
    "📘", "✏️", "📒", "💵", "📋", "☎️", "👍", "🎼",
    "🌅", "👔", "📷", "🧿", "🦷", "🌧️", "🧹", "🍓",
    "⭐", "▶️", "🎉", "😌", "👟", "🗑️", "🚭", "🌱",
)
private val iconColors = listOf(0xFF7CD77E, 0xFF5BE1CD, 0xFFFF86AB, 0xFF75CDF1, 0xFF92D77C, 0xFFFFD960, 0xFFEF80DF, 0xFFBCE85E)

@Composable
internal fun HabitAvatar(icon: String, modifier: Modifier = Modifier, selected: Boolean = false) {
    val index = habitIcons.indexOf(icon)
    val color = if (index < 0) Color(0xFFCA8392) else Color(iconColors[index % iconColors.size])
    Box(modifier.size(38.dp).background(if (selected) color.copy(alpha = .2f) else Color.Transparent, CircleShape).padding(if (selected) 4.dp else 1.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().background(color, CircleShape), contentAlignment = Alignment.Center) {
            Text(icon, fontSize = if (index < 0) 25.sp else 24.sp, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
internal fun HabitCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White), content = content)
}

@Composable
internal fun HabitInput(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier = Modifier, numeric: Boolean = false) {
    BasicTextField(value, onValue, modifier.heightIn(min = 46.dp).clip(RoundedCornerShape(9.dp)).background(HabitField)
        .semantics { contentDescription = hint }.padding(horizontal = 16.dp, vertical = 12.dp),
        singleLine = true, textStyle = TextStyle(color = TickTickColors.Text, fontSize = 16.sp), cursorBrush = SolidColor(HabitBlue),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        decorationBox = { field -> Box(contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(hint, color = Color(0xFFCBCBCB), fontSize = 16.sp)
            field()
        } })
}

@Composable
internal fun HabitSetting(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 51.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 16.sp)
        Text(value, Modifier.widthIn(max = 175.dp), fontSize = 16.sp, color = HabitMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        AppIcon(AppSymbol.Chevron, Modifier.padding(start = 10.dp).size(13.dp), HabitMuted)
    }
}

@Composable
internal fun HabitChoice(label: String, selected: Boolean, onClick: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 47.dp).selectable(selected, role = Role.RadioButton, onClick = onClick).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = null, modifier = Modifier.size(24.dp), colors = RadioButtonDefaults.colors(selectedColor = HabitBlue, unselectedColor = HabitMuted))
        Text(label, Modifier.padding(start = 7.dp).weight(1f), fontSize = 16.sp)
        trailing()
    }
}

@Composable
internal fun HabitMenu(value: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F1F1))
            .clickable(role = Role.Button) { open = true }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, Modifier.weight(1f), fontSize = 16.sp)
            Text("⌄", color = HabitMuted)
        }
        DropdownMenu(open, { open = false }, containerColor = Color.White) {
            options.forEach { label -> DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(label); open = false }) }
        }
    }
}
