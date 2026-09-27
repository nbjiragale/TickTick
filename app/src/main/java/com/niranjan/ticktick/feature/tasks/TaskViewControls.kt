package com.niranjan.ticktick.feature.tasks

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors

@Composable
fun TaskOverflowMenu(state: TasksUiState, onDismiss: () -> Unit, onChange: ((TaskPresentation) -> TaskPresentation) -> Unit,
    onCompleted: () -> Unit, onBackground: () -> Unit, onGroupSort: () -> Unit, onSelect: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val width = (LocalConfiguration.current.screenWidthDp.dp - 24.dp).coerceAtMost(244.dp)
    Popup(alignment = Alignment.TopEnd, offset = IntOffset(0, 0), onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = !expanded)) {
        BackHandler(expanded) { expanded = false }
        Column(Modifier.width(width).heightIn(max = (LocalConfiguration.current.screenHeightDp.dp - 100.dp).coerceAtLeast(100.dp))
            .shadow(16.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Color.White)
            .verticalScroll(rememberScrollState()).padding(vertical = 10.dp).semantics { paneTitle = "Task list options" }) {
            OptionRow("View", if (state.presentation.layout == TaskLayout.List) AppSymbol.List else AppSymbol.Kanban,
                { expanded = !expanded }) {
                AppIcon(AppSymbol.Chevron, Modifier.size(16.dp).rotate(if (expanded) 90f else 0f))
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 10.dp), color = Color(0xFFF1F1F1))
                Column(Modifier.selectableGroup()) {
                    TaskLayout.entries.forEach { layout ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).selectable(state.presentation.layout == layout,
                            role = Role.RadioButton, onClick = { onChange { it.copy(layout = layout) }; expanded = false })
                            .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(20.dp)) { if (state.presentation.layout == layout) AppIcon(AppSymbol.Check, Modifier.size(16.dp)) }
                            Text(layout.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                OptionRow("Background", AppSymbol.Background, { onDismiss(); onBackground() })
                OptionRow(if (state.presentation.details) "Hide Details" else "Show Details", AppSymbol.Note,
                    { onChange { it.copy(details = !it.details) }; onDismiss() })
                OptionRow(if (state.showCompleted) "Hide Completed" else "Show Completed", AppSymbol.PlanDone,
                    { onCompleted(); onDismiss() })
                HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 10.dp), color = Color(0xFFF1F1F1))
                OptionRow("Group & Sort", AppSymbol.Sort, { onDismiss(); onGroupSort() })
                OptionRow("Select", AppSymbol.PlanDone, { onDismiss(); onSelect() })
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, icon: AppSymbol, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppIcon(icon, Modifier.size(20.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskGroupSortSheet(options: TaskPresentation, onChange: ((TaskPresentation) -> TaskPresentation) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF7F7F7), dragHandle = null, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Group by", color = TickTickColors.SecondaryText, style = MaterialTheme.typography.titleMedium)
            ChoiceGrid(TaskGrouping.entries, { it.label }, options.grouping) { value -> onChange { it.copy(grouping = value) } }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Sort by", Modifier.weight(1f), color = TickTickColors.SecondaryText, style = MaterialTheme.typography.titleMedium)
                val orderLabel = when (options.sorting) {
                    TaskSorting.Title, TaskSorting.Tag -> if (options.descending) "Z–A ↓" else "A–Z ↑"
                    TaskSorting.Priority -> if (options.descending) "Low First ↓" else "High First ↑"
                    else -> if (options.descending) "Newest First ↓" else "Oldest First ↑"
                }
                TextButton(onClick = { onChange { it.copy(descending = !it.descending) } }) { Text(orderLabel, color = TickTickColors.SecondaryText) }
            }
            ChoiceGrid(TaskSorting.entries, { it.label }, options.sorting) { value -> onChange { it.copy(sorting = value) } }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun <T> ChoiceGrid(values: List<T>, label: (T) -> String, selected: T, onSelect: (T) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        values.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { value ->
                    Box(Modifier.weight(1f).heightIn(min = 36.dp).clip(RoundedCornerShape(24.dp))
                        .background(if (selected == value) Color(0xFF4C70FF) else Color(0xFFEBEBEB))
                        .selectable(selected == value, role = Role.RadioButton, onClick = { onSelect(value) })
                        .padding(horizontal = 4.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(label(value), style = MaterialTheme.typography.bodyMedium, color = if (selected == value) Color.White else Color(0xFF676767))
                    }
                }
            }
        }
    }
}

internal val backdropColors = listOf(0xFFC9D9FF, 0xFFAFEAF0, 0xFFBAE8D5, 0xFFD3E7C2, 0xFFFFE0AA, 0xFFF9C4D9, 0xFFE0C6EF).map { Color(it) }
internal val backdropTops = listOf(0xFFEDE7FF, 0xFFE4FAFF, 0xFFE2F8EC, 0xFFF0F6E3, 0xFFFFF3D6, 0xFFFFE8EF, 0xFFF8E4FF).map { Color(it) }
internal val backdropSwatchBottoms = listOf(0xFF819AFF, 0xFF53D2E4, 0xFF40CCAE, 0xFFA9C78E, 0xFFFFCB86, 0xFFF492BA, 0xFFB0AFF4).map { Color(it) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBackgroundSheet(options: TaskPresentation, onChange: ((TaskPresentation) -> TaskPresentation) -> Unit, onDismiss: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(options.backdrop) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onChange { it.copy(backdrop = TaskBackdrop.Image, imageUri = uri.toString()) }
                error = null
            } catch (_: Exception) { error = "This image can't be accessed. Choose another image." }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White, scrimColor = Color.Transparent, dragHandle = null,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4F4F4)).padding(3.dp).selectableGroup()) {
                TaskBackdrop.entries.forEach { type ->
                    Box(Modifier.weight(1f).heightIn(min = 36.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (tab == type) Color.White else Color.Transparent)
                        .selectable(tab == type, role = Role.Tab, onClick = {
                            tab = type
                            if (type != TaskBackdrop.Image || options.imageUri != null) onChange { it.copy(backdrop = type) }
                        }).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(type.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            when (tab) {
                TaskBackdrop.None -> Text("Default background", color = TickTickColors.SecondaryText, modifier = Modifier.padding(vertical = 12.dp))
                TaskBackdrop.Color, TaskBackdrop.Gradient -> Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    backdropColors.indices.forEach { index ->
                        val selected = options.backdrop == tab && options.swatch == index
                        val brush = if (tab == TaskBackdrop.Gradient) Brush.verticalGradient(listOf(backdropTops[index], backdropSwatchBottoms[index]))
                            else Brush.verticalGradient(listOf(backdropColors[index], backdropColors[index]))
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(brush)
                            .selectable(selected, role = Role.RadioButton, onClick = { onChange { it.copy(backdrop = tab, swatch = index) } })
                            .semantics { contentDescription = "${listOf("Blue", "Cyan", "Mint", "Green", "Amber", "Pink", "Purple")[index]} ${tab.label}" }) {
                            if (selected) AppIcon(AppSymbol.Check, Modifier.align(Alignment.BottomEnd).size(16.dp)
                                .background(TickTickColors.Accent, RoundedCornerShape(8.dp)).padding(2.dp), Color.White)
                        }
                    }
                }
                TaskBackdrop.Image -> Column {
                    TextButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text(if (options.imageUri == null) "Choose image" else "Change image") }
                    if (options.imageUri != null) TextButton(onClick = { onChange { it.copy(imageUri = null, backdrop = TaskBackdrop.None) }; tab = TaskBackdrop.None }) { Text("Remove image") }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
