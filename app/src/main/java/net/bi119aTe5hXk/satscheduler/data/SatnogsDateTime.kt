package net.bi119aTe5hXk.satscheduler.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

private val satnogsDateTimeFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd HH:mm:ss")
    .optionalStart()
    .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
    .optionalEnd()
    .toFormatter()

private val satnogsScheduleFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm:ss")
    .withZone(ZoneOffset.UTC)

fun formatSatnogsScheduleDate(instant: Instant): String {
    return satnogsScheduleFormatter.format(instant)
}

fun parseSatnogsInstant(value: String): Instant {
    val text = value.trim()
    return runCatching { Instant.parse(text) }
        .recoverCatching { OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() }
        .recoverCatching { LocalDateTime.parse(text, satnogsDateTimeFormatter).toInstant(ZoneOffset.UTC) }
        .getOrThrow()
}

fun parseSatnogsInstantOrNull(value: String?): Instant? {
    return value?.let { runCatching { parseSatnogsInstant(it) }.getOrNull() }
}
