package com.niranjan.ticktick.feature.taskeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.TaskPriority
import com.niranjan.ticktick.domain.nlp.TextSpan
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val EditorMuted = Color(0xFFB7B9BA)
internal val EditorPlaceholder = Color(0xFFCECECE)
internal val EditorBlue = Color(0xFF4D7DBF)

internal fun TaskPriority.color(): Color = when (this) {
    TaskPriority.High -> Color(0xFFD84E57)
    TaskPriority.Medium -> Color(0xFFE8B323)
    TaskPriority.Low -> EditorBlue
    TaskPriority.None -> EditorMuted
}

internal fun TaskEditorState.dateLabel(today: LocalDate, full: Boolean = false): String {
    val date = dueDate ?: return "Date & Time"
    val day = when (date) {
        today -> if (full) "Today, ${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))}" else "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
    }
    val time = dueTime?.format(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH))
    duration?.let { range ->
        val endDay = if (range.endDate == date) "" else range.endDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)) + ", "
        if (range.allDay) return if (range.startDate == range.endDate) "$day, All day" else "$day – ${range.endDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))}, All day"
        return "$day, $time – $endDay${range.endTime.format(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH))}"
    }
    return day + if (time != null) ", $time" else ""
}

private class HighlightTransformation(private val spans: List<TextSpan>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText = TransformedText(
        buildAnnotatedString {
            append(text)
            spans.forEach { span ->
                if (span.start >= 0 && span.end <= text.length && span.start < span.end) {
                    addStyle(SpanStyle(color = EditorBlue, background = Color(0xFFF0EFFA)), span.start, span.end)
                }
            }
        },
        OffsetMapping.Identity,
    )
}

@Composable
internal fun EditorTextField(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    placeholder: String,
    label: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    highlights: List<TextSpan> = emptyList(),
    focusRequester: FocusRequester? = null,
) {
    val transformation = remember(highlights) { HighlightTransformation(highlights) }
    BasicTextField(
        value = value, onValueChange = onChange,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        textStyle = style.copy(color = TickTickColors.Text),
        cursorBrush = SolidColor(TickTickColors.Accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        visualTransformation = transformation,
        decorationBox = { inner ->
            Box {
                if (value.text.isEmpty()) Text(placeholder, color = EditorPlaceholder, style = style)
                inner()
            }
        },
    )
}

@Composable
internal fun EditorIconButton(symbol: AppSymbol, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = EditorMuted) {
    Box(modifier.size(38.dp, 48.dp).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        AppIcon(symbol, Modifier.size(22.dp), tint)
    }
}

@Composable
internal fun EditorCheckbox(checked: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp, 40.dp).semantics { contentDescription = label }
        .toggleable(checked, role = Role.Checkbox, onValueChange = { onClick() }), contentAlignment = Alignment.Center) {
        Box(Modifier.size(17.dp).clip(RoundedCornerShape(4.dp))
            .background(if (checked) TickTickColors.Accent else Color.Transparent)
            .border(1.4.dp, if (checked) TickTickColors.Accent else TickTickColors.Checkbox, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            if (checked) AppIcon(AppSymbol.Check, Modifier.size(13.dp), Color.White)
        }
    }
}

internal class MenuPosition(private val above: Boolean, private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val x = anchorBounds.left.coerceIn(margin, maxX)
        val proposedY = if (above) anchorBounds.top - popupContentSize.height else anchorBounds.bottom
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        return IntOffset(x, proposedY.coerceIn(margin, maxY))
    }
}

@Composable
internal fun EditorMoreMenu(state: TaskEditorState, vm: TaskEditorViewModel, onImage: () -> Unit) {
    if (state.panel != EditorPanel.More) return
    val margin = with(LocalDensity.current) { 28.dp.roundToPx() }
    Popup(
        popupPositionProvider = remember(state.fullScreen, margin) { MenuPosition(!state.fullScreen, margin) },
        onDismissRequest = { vm.panel(null) },
        properties = PopupProperties(focusable = false),
    ) {
        Column(Modifier.width(196.dp).shadow(3.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Color.White).padding(vertical = 8.dp)) {
            MenuRow(AppSymbol.Image, "Image") { vm.panel(null); onImage() }
            MenuRow(AppSymbol.Note, if (state.isNote) "Convert to Task" else "Convert to Note", vm::toggleNote)
            if (!state.fullScreen) MenuRow(AppSymbol.Expand, "Full-Screen", vm::showFullScreen)
            else if (state.existing) MenuRow(AppSymbol.Close, "Delete") { vm.panel(EditorPanel.Delete) }
        }
    }
}

@Composable
internal fun MenuRow(symbol: AppSymbol, title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppIcon(symbol, Modifier.size(20.dp), TickTickColors.Text)
        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
    }
}
