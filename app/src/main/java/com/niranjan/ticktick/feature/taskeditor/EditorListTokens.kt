package com.niranjan.ticktick.feature.taskeditor

import com.niranjan.ticktick.domain.model.ListSymbol
import com.niranjan.ticktick.domain.model.TaskList
import com.niranjan.ticktick.domain.nlp.TextSpan

internal val ListSymbol.editorGlyph: String get() = when (this) {
    ListSymbol.Inbox -> "📥"
    ListSymbol.Work -> "💼"
    ListSymbol.Personal -> "🏠"
    ListSymbol.Welcome -> "👋"
    ListSymbol.Custom -> "☰"
}

internal val TaskList.editorToken get() = "~${symbol.editorGlyph}$name"

internal fun TaskList.tokenSpans(text: String): List<TextSpan> =
    Regex("(?<!\\S)${Regex.escape(editorToken)}(?!\\S)").findAll(text)
        .map { TextSpan(it.range.first, it.range.last + 1) }.toList()

// Keep source offsets intact and prevent list names such as "Tomorrow" becoming dates/tags.
internal fun titleWithoutListTokens(text: String, list: TaskList?): String {
    val chars = text.toCharArray()
    list?.tokenSpans(text)?.forEach { span ->
        for (index in span.start until span.end) chars[index] = ' '
    }
    return chars.concatToString()
}

internal fun TaskEditorState.titleHighlights(list: TaskList?): List<TextSpan> =
    activePrediction?.spans.orEmpty() + parsedTags.map { it.span } + list?.tokenSpans(title.text).orEmpty()
