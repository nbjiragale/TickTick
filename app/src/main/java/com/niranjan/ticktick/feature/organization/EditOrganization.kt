package com.niranjan.ticktick.feature.organization

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niranjan.ticktick.domain.model.*
import com.niranjan.ticktick.domain.repository.*
import kotlinx.coroutines.launch

@Composable
fun EditOrganization(repository: TaskRepository, selection: TaskFilter, deleting: Boolean,
    onDismiss: () -> Unit, onSaved: (String?) -> Unit) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val list = (selection as? TaskFilter.ListFilter)?.let { selected -> snapshot.lists.find { it.id == selected.listId } }
    val tag = (selection as? TaskFilter.TagFilter)?.let { selected -> snapshot.knownTags.find { it.name.equals(selected.name, true) } }
    val filter = (selection as? TaskFilter.SavedFilter)?.let { selected -> snapshot.filters.find { it.id == selected.id } }
    if (deleting) {
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("Delete ${list?.name ?: tag?.name ?: filter?.name ?: "item"}?") },
            text = { Text(error ?: when (selection) {
                is TaskFilter.ListFilter -> "Tasks will move to Inbox. Saved filter conditions using this list will use Inbox."
                is TaskFilter.TagFilter -> "This tag will be removed from tasks and inline title tokens. Saved filters keep their condition, which will match no tasks until this tag is used again."
                else -> "The saved filter will be removed. Your tasks will remain."
            }) }, confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    taskWriteResult { when (selection) {
                        is TaskFilter.ListFilter -> repository.deleteList(selection.listId)
                        is TaskFilter.TagFilter -> repository.deleteTag(selection.name)
                        is TaskFilter.SavedFilter -> repository.deleteFilter(selection.id)
                        else -> error("Choose a list, tag or filter.")
                    } }.fold(onSuccess = { onSaved(null); onDismiss() }, onFailure = { error = it.message })
                    busy = false
                }
            }) { Text(if (busy) "Deleting…" else "Delete") } },
            dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } })
    } else when {
        list != null -> AddListOrTagScreen(false, onDismiss, list.name, list.color, list.view, editing = true) { name, color, view ->
            taskWriteResult { repository.editList(list.id, name, color, view); onSaved(null) }
        }
        tag != null -> AddListOrTagScreen(true, onDismiss, tag.name, tag.color, editing = true) { name, color, _ ->
            taskWriteResult { repository.editTag(tag.name, name, color); onSaved(name.trim().removePrefix("#")) }
        }
        filter != null -> AddFilterScreen(snapshot, onDismiss, filter) { name, rules, all ->
            taskWriteResult { repository.editFilter(filter.id, name, rules, all); onSaved(null) }
        }
        else -> LaunchedEffect(Unit) { onDismiss() }
    }
}
