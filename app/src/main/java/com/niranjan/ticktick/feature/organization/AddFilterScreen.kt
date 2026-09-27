package com.niranjan.ticktick.feature.organization

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.*

@Composable
fun AddFilterScreen(snapshot: TaskSnapshot, onDismiss: () -> Unit, onSave: suspend (String, List<FilterRule>, Boolean) -> Result<Unit>) {
    var name by rememberSaveable { mutableStateOf("") }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var matchAll by rememberSaveable { mutableStateOf(true) }
    var rules by rememberSaveable { mutableStateOf(arrayListOf<FilterRule>()) }
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    var normalField by rememberSaveable { mutableStateOf<FilterField?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    OrganizationFrame("Add Filter", name, { name = it; error = null }, AppSymbol.Filter,
        name.isNotBlank() || rules.isNotEmpty(), error, onDismiss, {
            if (!saving) {
                saving = true
                scope.launch {
                    try { onSave(name, rules, matchAll).fold(onSuccess = { onDismiss() }, onFailure = { error = it.message ?: "Couldn't save filter." }) }
                    finally { saving = false }
                }
            }
        }, saving = saving) {
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.Center) {
            listOf("Normal", "Advanced").forEachIndexed { index, label ->
                Column(Modifier.weight(1f).selectable(advanced == (index == 1), role = Role.Tab, onClick = {
                    if (index == 0 && (rules.distinctBy { it.field }.size != rules.size || !matchAll)) {
                        error = "Normal mode uses AND and one condition per field. Adjust those conditions before switching."
                    } else { advanced = index == 1; error = null }
                }).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = if (advanced == (index == 1)) TickTickColors.Text else TickTickColors.SecondaryText)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(72.dp).height(2.dp).background(if (advanced == (index == 1)) TickTickColors.Accent else Color.Transparent))
                }
            }
        }
        if (!advanced) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)) {
                FilterField.entries.forEach { field ->
                    val rule = rules.firstOrNull { it.field == field }
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) {
                        normalField = field; editing = rules.indexOfFirst { it.field == field }.takeIf { it >= 0 } ?: -1
                    }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(field.label, Modifier.weight(1f))
                        Text(rule?.label(snapshot) ?: "Any", Modifier.widthIn(max = 180.dp), color = TickTickColors.SecondaryText)
                        if (rule != null) IconButton(onClick = { rules = ArrayList(rules.filterNot { it.field == field }) }, modifier = Modifier.size(40.dp).semantics { contentDescription = "Clear ${field.label} condition" }) {
                            AppIcon(AppSymbol.Close, Modifier.size(14.dp))
                        } else AppIcon(AppSymbol.Chevron, Modifier.padding(start = 10.dp).size(14.dp), TickTickColors.SecondaryText)
                    }
                }
            }
        } else {
            Text("Match ${if (matchAll) "all" else "any"} conditions", color = TickTickColors.SecondaryText, style = MaterialTheme.typography.bodyMedium)
            rules.forEachIndexed { index, rule ->
                if (index > 0) TextButton(onClick = { matchAll = !matchAll }, modifier = Modifier.fillMaxWidth().background(Color(0xFFD7DEFF), RoundedCornerShape(9.dp))) {
                    Text(if (matchAll) "And" else "Or")
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White)
                    .clickable(role = Role.Button) { normalField = null; editing = index }.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.label(snapshot), Modifier.weight(1f).padding(vertical = 14.dp))
                    IconButton(onClick = { rules = ArrayList(rules.filterIndexed { i, _ -> i != index }) }, modifier = Modifier.semantics { contentDescription = "Remove ${rule.label(snapshot)}" }) {
                        AppIcon(AppSymbol.Close, Modifier.size(16.dp), TickTickColors.SecondaryText)
                    }
                }
            }
            TextButton(onClick = { normalField = null; editing = -1 }, modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(10.dp))) { Text("＋ Add") }
        }
        Text("Examples: This week AND High Priority; Today OR High Priority.", Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            color = TickTickColors.SecondaryText, style = MaterialTheme.typography.bodySmall)
        if (editing != null) FilterRuleDialog(snapshot, rules.getOrNull(editing!!), normalField, { editing = null }) { rule ->
            val updated = ArrayList(rules)
            if (editing!! in updated.indices) updated[editing!!] = rule else updated.add(rule)
            rules = updated; editing = null; error = null
        }
    }
}

@Composable
private fun FilterRuleDialog(snapshot: TaskSnapshot, initial: FilterRule?, lockedField: FilterField?, onDismiss: () -> Unit, onApply: (FilterRule) -> Unit) {
    var field by rememberSaveable { mutableStateOf(lockedField ?: initial?.field ?: FilterField.Date) }
    var value by rememberSaveable { mutableStateOf(initial?.value.orEmpty()) }
    val options = when (field) {
        FilterField.Date -> FilterDate.entries.map { it.name to it.label }
        FilterField.Priority -> TaskPriority.entries.map { it.name to if (it == TaskPriority.None) "No Priority" else "${it.name} Priority" }
        FilterField.List -> snapshot.lists.map { it.id to it.name }
        FilterField.Tag -> snapshot.knownTags.map { it.name to "#${it.name}" }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Filter condition") }, text = {
        Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            if (lockedField == null) Row(Modifier.fillMaxWidth().selectableGroup()) {
                FilterField.entries.forEach { option -> TextButton(onClick = { field = option; value = "" }, modifier = Modifier.weight(1f)) {
                    Text(option.label, color = if (field == option) TickTickColors.Accent else TickTickColors.SecondaryText)
                } }
            }
            if (options.isEmpty()) Text("Create a tag first to use a tag condition.")
            Column(Modifier.selectableGroup()) {
                options.forEach { (id, label) ->
                    Row(Modifier.fillMaxWidth().selectable(value == id, role = Role.RadioButton, onClick = { value = id }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = value == id, onClick = null)
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }, confirmButton = { TextButton(enabled = options.any { it.first == value }, onClick = { onApply(FilterRule(field, value)) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
