package com.niranjan.ticktick.domain.nlp

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class TextSpan(val start: Int, val end: Int)
data class ParsedSchedule(val date: LocalDate, val time: LocalTime?, val spans: List<TextSpan>, val fingerprint: String)
data class ParsedTag(val name: String, val span: TextSpan)

/** Local, deterministic English parsing. It never edits the source text. */
class TaskDateParser(private val clock: Clock) {
    private val dateRegex = Regex("(?<![\\w#])(today|tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", RegexOption.IGNORE_CASE)
    private val timeRegex = Regex("(?<![\\w:#])([1-9]|1[0-2])(?::([0-5][0-9]))?\\s*(am|pm)\\b", RegexOption.IGNORE_CASE)
    private val tagRegex = Regex("(?<![\\w#])#([\\p{L}\\p{N}_-]+)")

    fun parse(text: String): ParsedSchedule? {
        val dateMatch = dateRegex.find(text)
        val timeMatch = timeRegex.find(text)
        if (dateMatch == null && timeMatch == null) return null
        val today = LocalDate.now(clock)
        val time = timeMatch?.let {
            val hour = it.groupValues[1].toInt() % 12 + if (it.groupValues[3].equals("pm", true)) 12 else 0
            LocalTime.of(hour, it.groupValues[2].ifEmpty { "0" }.toInt())
        }
        val dayWord = dateMatch?.value?.lowercase(Locale.ENGLISH)
        var date = when (dayWord) {
            null, "today" -> today
            "tomorrow" -> today.plusDays(1)
            else -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.valueOf(dayWord.uppercase(Locale.ENGLISH))))
        }
        // A time without a date means its next occurrence. Explicit "today" stays today.
        if (dayWord == null && time != null && !time.isAfter(LocalTime.now(clock))) date = date.plusDays(1)
        val spans = listOfNotNull(dateMatch, timeMatch).map { TextSpan(it.range.first, it.range.last + 1) }.sortedBy { it.start }
        val merged = if (spans.size == 2 && Regex("\\s+(?:at\\s+)?", RegexOption.IGNORE_CASE).matches(text.substring(spans[0].end, spans[1].start))) {
            listOf(TextSpan(spans[0].start, spans[1].end))
        } else spans
        val fingerprint = listOfNotNull(dateMatch?.value, timeMatch?.value).joinToString("|").lowercase(Locale.ENGLISH).replace(Regex("\\s+"), "")
        return ParsedSchedule(date, time, merged, fingerprint)
    }

    fun tags(text: String): List<ParsedTag> = tagRegex.findAll(text).map {
        ParsedTag(it.groupValues[1], TextSpan(it.range.first, it.range.last + 1))
    }.toList()
}
