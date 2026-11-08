package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.text.wholeRegex

private const val LAST_MONTH = 12
private const val LAST_HOUR = 23
private const val LAST_MINUTE = 59

/** A leap second, as RFC 3339, which TOML's dates are, allows. */
private const val LAST_SECOND = 60
private const val FEBRUARY = 2
private const val LEAP_DAY = 29
private const val LEAP_EVERY = 4
private const val NO_LEAP_EVERY = 100
private const val LEAP_AGAIN_EVERY = 400

// The groups of DateTimePattern.
private const val YEAR = 1
private const val MONTH = 2
private const val DAY = 3
private const val HOUR = 4
private const val MINUTE = 5
private const val SECOND = 6
private const val ZULU = 7
private const val SIGN = 8
private const val OFFSET_HOUR = 9
private const val OFFSET_MINUTE = 10

/** How many days each month has in a year that is not a leap year. */
private val DaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

/** A date, and a time after a `T`, a `t` or a space, with an offset or none: its year, month, day, hour, … offset. */
private val DateTimePattern = wholeRegex(
    "^([0-9]{4})-([0-9]{2})-([0-9]{2})" +
        "(?:[Tt ]([0-9]{2}):([0-9]{2}):([0-9]{2})(?:\\.[0-9]+)?(?:([Zz])|([+-])([0-9]{2}):([0-9]{2}))?)?$"
)

/** A time alone: its hour, minute and second, and a fraction of the second or none. */
private val TimePattern = wholeRegex("^([0-9]{2}):([0-9]{2}):([0-9]{2})(?:\\.[0-9]+)?$")

/**
 * Which of TOML's dates and times [raw] is, or null where it is none of them: a day its month does not have,
 * `2026-02-30`, an hour past 23, a time with no seconds, `07:32`, which TOML 1.0 does not allow.
 */
internal fun tomlDateTimeKind(raw: String): TomlDateTimeKind? {
    val time = TimePattern.find(raw)
    if (time != null) {
        val (hour, minute, second) = time.destructured
        return if (isTime(hour, minute, second)) TomlDateTimeKind.LocalTime else null
    }
    val groups = DateTimePattern.find(raw)?.groupValues ?: return null
    val hour = groups[HOUR]
    val valid = isDate(groups[YEAR].toInt(), groups[MONTH].toInt(), groups[DAY].toInt()) &&
        (hour.isEmpty() || isTime(hour, groups[MINUTE], groups[SECOND])) &&
        (groups[OFFSET_HOUR].isEmpty() || isOffset(groups[OFFSET_HOUR], groups[OFFSET_MINUTE]))
    return when {
        !valid -> null
        hour.isEmpty() -> TomlDateTimeKind.LocalDate
        groups[ZULU].isEmpty() && groups[SIGN].isEmpty() -> TomlDateTimeKind.LocalDateTime
        else -> TomlDateTimeKind.OffsetDateTime
    }
}

/** Whether [month] is one of the year's and [day] one of its days, February's 29th in a leap year. */
private fun isDate(year: Int, month: Int, day: Int): Boolean {
    if (month !in 1..LAST_MONTH) return false
    val leap = year % LEAP_EVERY == 0 && (year % NO_LEAP_EVERY != 0 || year % LEAP_AGAIN_EVERY == 0)
    val days = if (month == FEBRUARY && leap) LEAP_DAY else DaysInMonth[month - 1]
    return day in 1..days
}

/** Whether [hour], [minute] and [second] are a time of day, a leap second among them. */
private fun isTime(hour: String, minute: String, second: String): Boolean =
    hour.toInt() <= LAST_HOUR && minute.toInt() <= LAST_MINUTE && second.toInt() <= LAST_SECOND

/** Whether [hour] and [minute] are an offset from UTC. */
private fun isOffset(hour: String, minute: String): Boolean = hour.toInt() <= LAST_HOUR && minute.toInt() <= LAST_MINUTE
