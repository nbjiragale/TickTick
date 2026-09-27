package com.niranjan.ticktick.feature.taskeditor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.niranjan.ticktick.core.designsystem.AppSymbol

@Composable
internal fun EditorPanels(state: TaskEditorState, vm: TaskEditorViewModel, onAttachment: (AttachmentSource) -> Unit) {
    val panel = state.panel ?: return
    if (panel == EditorPanel.More || panel == EditorPanel.Date || panel == EditorPanel.Priority || panel == EditorPanel.Lists || panel == EditorPanel.Attachments) return
    AlertDialog(
        onDismissRequest = { vm.panel(null) },
        title = { Text(when (panel) {
            EditorPanel.Settings -> "Quick Add Settings"
            EditorPanel.Templates -> "Template"
            EditorPanel.Voice -> "Voice input"
            EditorPanel.Delete -> "Delete this task?"
            EditorPanel.ScanDocuments -> "Scan Documents"
            else -> ""
        }) },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (panel) {
                    EditorPanel.Settings -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recognize dates while typing", Modifier.weight(1f))
                        Switch(state.nlpEnabled, vm::setNlpEnabled)
                    }
                    EditorPanel.Templates -> {
                        TextButton(onClick = { vm.applyTemplate("Shopping list", listOf("Fruit", "Vegetables", "Milk")) }) { Text("Shopping list") }
                        TextButton(onClick = { vm.applyTemplate("Daily plan", listOf("Choose today's priorities", "Review upcoming tasks")) }) { Text("Daily plan") }
                    }
                    EditorPanel.Voice -> Text("Use the microphone on your keyboard to dictate a task.")
                    EditorPanel.Delete -> Text("This will delete the saved task and its checklist.")
                    EditorPanel.ScanDocuments -> {
                        Text("Take a document photo or choose an existing PDF or image.")
                        TextButton(onClick = { onAttachment(AttachmentSource.TakePhoto) }) { Text("Photograph document") }
                        TextButton(onClick = { onAttachment(AttachmentSource.ExistingScan) }) { Text("Choose existing scan") }
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (panel) {
                    EditorPanel.Delete -> vm.delete()
                    else -> vm.panel(null)
                }
            }) { Text(when (panel) { EditorPanel.Delete -> "Delete"; EditorPanel.ScanDocuments -> "Cancel"; else -> "Done" }) }
        },
        dismissButton = { if (panel == EditorPanel.Delete) TextButton(onClick = { vm.panel(null) }) { Text("Cancel") } },
    )
}

@Composable
internal fun EditorExtraContent(state: TaskEditorState, vm: TaskEditorViewModel, onEditTag: (String) -> Unit) {
    if (state.allTags.isNotEmpty()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.allTags.forEach { tag -> AssistChip(onClick = { onEditTag(tag) }, label = { Text("#$tag") }) }
        }
    }
    if (state.checklistMode) {
        state.checklist.forEachIndexed { index, item ->
            androidx.compose.runtime.key(item.id) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    EditorCheckbox(item.completed, "Complete checklist item", { vm.toggleChecklistItem(item.id) })
                    BasicTextField(item.text, { vm.editChecklistItem(item.id, it) }, Modifier.weight(1f).semantics { contentDescription = "Checklist item ${index + 1}" }, textStyle = MaterialTheme.typography.bodyLarge,
                        decorationBox = { inner -> if (item.text.isEmpty()) Text("List item", color = EditorPlaceholder); inner() })
                    if (index > 0) EditorIconButton(AppSymbol.Back, "Move item up", { vm.moveChecklistItem(item.id, -1) }, Modifier.width(28.dp).rotate(90f))
                    if (index < state.checklist.lastIndex) EditorIconButton(AppSymbol.Back, "Move item down", { vm.moveChecklistItem(item.id, 1) }, Modifier.width(28.dp).rotate(270f))
                    EditorIconButton(AppSymbol.Close, "Remove checklist item", { vm.removeChecklistItem(item.id) }, Modifier.width(28.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        TextButton(onClick = vm::addChecklistItem) { Text("Add item") }
    }
    AttachmentCards(state.attachments, vm::removeAttachment, vm::reportError, state.attachmentLoading)
}
