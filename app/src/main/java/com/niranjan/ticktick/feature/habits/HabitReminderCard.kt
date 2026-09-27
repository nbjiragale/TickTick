package com.niranjan.ticktick.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.feature.taskeditor.schedule.ReferenceSwitch
import com.niranjan.ticktick.feature.taskeditor.schedule.ScheduleTextButton
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val habitReminderTimeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HabitReminderCard(
    reminders: List<LocalTime>,
    constantReminder: Boolean,
    onEdit: (LocalTime) -> Unit,
    onAdd: () -> Unit,
    onConstantChange: (Boolean) -> Unit,
) {
    HabitCard {
        Text("Reminder", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), fontSize = 16.sp)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 4,
        ) {
            reminders.forEach { time ->
                val label = time.format(habitReminderTimeFormat)
                Box(Modifier.widthIn(min = 72.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { onEdit(time) }
                    .semantics { contentDescription = "Edit reminder $label" }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.widthIn(min = 72.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F1F1))
                        .padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 16.sp, lineHeight = 20.sp)
                    }
                }
            }
            ScheduleTextButton("＋ Add", onAdd, Modifier.widthIn(min = 72.dp).semantics { contentDescription = "Add reminder" })
        }
        if (reminders.isNotEmpty()) {
            HorizontalDivider(color = Color(0xFFF1F1F1))
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("Constant Reminder", fontSize = 16.sp)
                    Box(Modifier.padding(start = 4.dp).size(16.dp).background(Color(0xFFFFC107), CircleShape), contentAlignment = Alignment.Center) {
                        AppIcon(AppSymbol.Crown, Modifier.size(12.dp), Color.White)
                    }
                }
                ReferenceSwitch(constantReminder, "Constant Reminder", onConstantChange)
            }
        }
    }
}
