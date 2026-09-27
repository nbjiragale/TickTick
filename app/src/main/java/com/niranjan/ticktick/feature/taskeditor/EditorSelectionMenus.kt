package com.niranjan.ticktick.feature.taskeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.ListSymbol
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.model.TaskPriority

/** Menus remain in the editor's IME session; the host handles Back without hiding it. */
@Composable
internal fun PriorityMenu(
    selected: TaskPriority,
    above: Boolean,
    maxHeight: Dp,
    onSelect: (TaskPriority) -> Unit,
    onDismiss: () -> Unit,
) {
    SelectionMenu("Priority", above, maxHeight, onDismiss = onDismiss) {
        items(TaskPriority.entries) { priority ->
            SelectionRow(priority == selected, 40.dp, { onSelect(priority) }) {
                AppIcon(AppSymbol.Flag, Modifier.size(20.dp), priority.color())
                Text(
                    if (priority == TaskPriority.None) "No Priority" else "${priority.name} Priority",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TickTickColors.Text,
                )
            }
        }
    }
}

@Composable
internal fun ListMenu(
    lists: List<TaskList>,
    selectedId: String,
    above: Boolean,
    maxHeight: Dp,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // In Quick Add the reference floats above the sheet, inset 64 dp from its start.
    SelectionMenu("Select list", above, maxHeight, startOffset = if (above) 64.dp else 0.dp, onDismiss = onDismiss) {
        items(lists, key = { it.id }) { list ->
            val selected = list.id == selectedId
            SelectionRow(selected, 42.dp, { onSelect(list.id) }) {
                if (list.color != null) AppIcon(AppSymbol.List, Modifier.size(20.dp), Color(list.color)) else EditorListIcon(list.symbol)
                Text(list.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) EditorBlue else TickTickColors.Text,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (selected) AppIcon(AppSymbol.Check, Modifier.size(14.dp), EditorBlue)
            }
        }
    }
}

@Composable
internal fun EditorListIcon(symbol: ListSymbol) {
    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        when (symbol) {
            ListSymbol.Inbox -> AppIcon(AppSymbol.InboxOutline, Modifier.size(20.dp), EditorBlue)
            ListSymbol.Work, ListSymbol.Personal, ListSymbol.Welcome -> Text(symbol.editorGlyph, fontSize = 17.sp, lineHeight = 20.sp)
            ListSymbol.Custom -> AppIcon(AppSymbol.List, Modifier.size(20.dp), EditorBlue)
        }
    }
}

@Composable
private fun SelectionRow(selected: Boolean, minHeight: Dp, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = minHeight)
        .selectable(selected, role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun SelectionMenu(
    title: String,
    above: Boolean,
    maxHeight: Dp,
    startOffset: Dp = 0.dp,
    onDismiss: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    val margin = with(density) { 8.dp.roundToPx() }
    val offset = with(density) { startOffset.roundToPx() }
    val width = (LocalConfiguration.current.screenWidthDp.dp - 16.dp).coerceIn(1.dp, 192.dp)
    Popup(
        popupPositionProvider = remember(above, margin, offset) { SelectionMenuPosition(above, margin, offset) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        val shape = RoundedCornerShape(16.dp)
        LazyColumn(Modifier.width(width).heightIn(max = maxHeight)
            .shadow(3.dp, shape).clip(shape).background(Color.White)
            .semantics { paneTitle = title }.selectableGroup(),
            contentPadding = PaddingValues(vertical = 8.dp), content = content)
    }
}

private class SelectionMenuPosition(private val above: Boolean, private val margin: Int, private val startOffset: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val start = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left + startOffset
            else anchorBounds.right - startOffset - popupContentSize.width
        val x = start.coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
        val aboveY = anchorBounds.top - popupContentSize.height
        val belowY = anchorBounds.bottom
        val y = if (above) aboveY else if (belowY + popupContentSize.height <= windowSize.height - margin) belowY else aboveY
        return IntOffset(x, y.coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)))
    }
}
