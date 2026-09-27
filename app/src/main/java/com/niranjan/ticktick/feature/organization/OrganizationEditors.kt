package com.niranjan.ticktick.feature.organization

import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.TaskListView

internal val OrganizationBackground = Color(0xFFF3F3F9)

@Composable
fun AddListOrTagScreen(tag: Boolean, onDismiss: () -> Unit, onSave: suspend (String, Int?, TaskListView) -> Result<Unit>) {
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf<Int?>(null) }
    var view by rememberSaveable { mutableStateOf(TaskListView.List) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    OrganizationFrame(if (tag) "Add Tag" else "Add List", name, { name = it; error = null },
        if (tag) AppSymbol.Tag else AppSymbol.Menu, name.isNotBlank() || color != null || view != TaskListView.List,
        error, onDismiss, {
            if (!saving) {
                saving = true
                scope.launch {
                    try { onSave(name, color, view).fold(onSuccess = { onDismiss() }, onFailure = { error = it.message ?: "Couldn't save. Please try again." }) }
                    finally { saving = false }
                }
            }
        }, focusName = tag, saving = saving) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(16.dp)) {
            Text(if (tag) "Color" else "List Color", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            OrganizationColors(color) { color = it }
            if (!tag) {
                Text("View Type", Modifier.padding(top = 20.dp, bottom = 14.dp), style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("List", "Kanban", "Timeline").forEachIndexed { index, label ->
                        val supported = index < 2
                        val selected = supported && view.ordinal == index
                        Column(Modifier.weight(1f).selectable(selected, enabled = supported, role = Role.RadioButton,
                            onClick = { view = TaskListView.entries[index] })
                            .semantics { contentDescription = if (supported) "$label view" else "Timeline view, not available yet" }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(7.dp)).border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(7.dp))) {
                                ViewThumbnail(index)
                                if (selected) Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).background(TickTickColors.Accent, CircleShape), contentAlignment = Alignment.Center) {
                                    AppIcon(AppSymbol.Check, Modifier.size(14.dp), Color.White)
                                }
                            }
                            Text(label, Modifier.padding(top = 8.dp), fontSize = 13.sp, color = if (selected) TickTickColors.DueTime else TickTickColors.SecondaryText)
                            if (!supported) Text("Later", fontSize = 11.sp, color = TickTickColors.SecondaryText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OrganizationFrame(title: String, name: String, onName: (String) -> Unit, icon: AppSymbol, dirty: Boolean,
    error: String?, onDismiss: () -> Unit, onSave: () -> Unit, focusName: Boolean = false, saving: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val dismiss = { if (!saving) { if (dirty) confirmDiscard = true else onDismiss() } }
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.let {
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightNavigationBars = true
        } }
        val focus = remember { FocusRequester() }
        LaunchedEffect(focusName) { if (focusName) focus.requestFocus() }
        Column(Modifier.fillMaxSize().background(OrganizationBackground).systemBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = dismiss) { AppIcon(AppSymbol.Close, Modifier.size(24.dp)) }
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onSave, enabled = !saving, modifier = Modifier.semantics { contentDescription = "Save $title" }) { AppIcon(AppSymbol.Check, Modifier.size(26.dp)) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFECEDF2)).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(icon, Modifier.size(20.dp), Color(0xFFADB0B5))
                    BasicTextField(name, onName, Modifier.weight(1f).padding(start = 12.dp).focusRequester(focus).semantics { contentDescription = "Name" }, enabled = !saving,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TickTickColors.Text), singleLine = true,
                        cursorBrush = SolidColor(TickTickColors.Accent), decorationBox = { field ->
                            Box { if (name.isEmpty()) Text("Name", color = Color(0xFFB8BABF), style = MaterialTheme.typography.bodyLarge); field() }
                        })
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                content()
            }
        }
        if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Discard changes?") },
            text = { Text("Your new item hasn't been saved.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } })
    }
}

private val Palette = listOf(0xFFFF5768, 0xFFFFA000, 0xFFFFD900, 0xFFDFEA00, 0xFF00CB63, 0xFF159FEC).map { it.toInt() }

@Composable
private fun OrganizationColors(selected: Int?, onSelect: (Int?) -> Unit) {
    var custom by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
        (listOf<Int?>(null) + Palette).forEach { color ->
            val label = if (color == null) "No color" else listOf("Red", "Orange", "Yellow", "Lime", "Green", "Blue")[Palette.indexOf(color)]
            Box(Modifier.size(40.dp).selectable(selected == color, role = Role.RadioButton, onClick = { onSelect(color) })
                .semantics { contentDescription = label }.padding(3.dp).border(if (selected == color) 1.5.dp else 0.dp,
                    if (selected == color) TickTickColors.DueTime else Color.Transparent, CircleShape).padding(2.dp)
                .background(color?.let { Color(it) } ?: Color.White, CircleShape), contentAlignment = Alignment.Center) {
                if (color == null) Canvas(Modifier.fillMaxSize().border(1.dp, Color(0xFFDADCE0), CircleShape).padding(4.dp)) {
                    drawLine(Color(0xFFFF8790), Offset(0f, size.height), Offset(size.width, 0f), 1.5.dp.toPx())
                }
                else if (selected == color) AppIcon(AppSymbol.Check, Modifier.size(14.dp), Color.White)
            }
        }
        Box(Modifier.size(40.dp).clickable(role = Role.Button) { custom = true }.semantics { contentDescription = "Custom color" }.padding(5.dp)
            .background(Brush.sweepGradient(listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)), CircleShape), contentAlignment = Alignment.Center) {
            if (selected != null && selected !in Palette) Box(Modifier.size(18.dp).background(Color(selected), CircleShape).border(2.dp, Color.White, CircleShape))
        }
    }
    if (custom) {
        var hex by rememberSaveable { mutableStateOf(selected?.let { "%06X".format(it and 0xFFFFFF) } ?: "FF0000") }
        val valid = Regex("[0-9a-fA-F]{6}").matches(hex)
        AlertDialog(onDismissRequest = { custom = false }, title = { Text("Custom color") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(hex, { hex = it.removePrefix("#").take(6) }, label = { Text("Hex color") }, prefix = { Text("#") }, singleLine = true, isError = !valid)
                if (valid) Box(Modifier.fillMaxWidth().height(32.dp).background(Color((0xFF000000L or hex.toLong(16)).toInt()), RoundedCornerShape(8.dp)))
            }
        }, confirmButton = { TextButton(enabled = valid, onClick = { onSelect((0xFF000000L or hex.toLong(16)).toInt()); custom = false }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { custom = false }) { Text("Cancel") } })
    }
}

@Composable
private fun ViewThumbnail(type: Int) {
    Canvas(Modifier.fillMaxSize().padding(9.dp)) {
        val gray = Color(0xFFF0F1F2)
        if (type == 2) {
            repeat(4) { i -> drawLine(gray, Offset(size.width * i / 3, 0f), Offset(size.width * i / 3, size.height), 1.dp.toPx()) }
            repeat(3) { i -> drawRect(if (i == 2) Color(0xFFD9E3FF) else Color(0xFFFFDDDF), Offset(size.width * (i % 2) / 3, size.height * (.1f + i * .3f)), Size(size.width * .6f, size.height * .14f)) }
        } else repeat(3) { i ->
            val y = size.height * i / 3
            if (type == 1) drawRoundRect(gray, Offset(0f, y), Size(size.width, size.height * .28f), CornerRadius(3f))
            drawRect(if (i == 0) Color(0xFFF5D9DE) else Color(0xFFDBE3F1), Offset(2f, y + 3f), Size(size.width * .06f, size.height * .09f))
            drawRect(if (type == 1) Color.White else gray, Offset(size.width * .14f, y + 3f), Size(size.width * .7f, size.height * .07f))
            drawRect(if (type == 1) Color.White else gray, Offset(size.width * .14f, y + size.height * .13f), Size(size.width * .35f, size.height * .05f))
        }
    }
}
