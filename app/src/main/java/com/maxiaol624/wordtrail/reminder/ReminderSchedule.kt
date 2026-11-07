package com.maxiaol624.wordtrail.reminder

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class ReminderSettings(
    val enabled: Boolean = false,
    val time: LocalTime = LocalTime.of(20, 0),
    val lastSentDate: LocalDate? = null,
    val permissionAsked: Boolean = false,
)

object ReminderSchedule {
    fun shouldCheck(now: ZonedDateTime, settings: ReminderSettings): Boolean {
        val today = now.toLocalDate()
        if (!settings.enabled || settings.lastSentDate?.let { it >= today } == true) return false
        val target = today.atTime(settings.time).atZone(now.zone).toInstant()
        return now.toInstant() >= target
    }

    fun nextCheck(now: ZonedDateTime, settings: ReminderSettings, checkedToday: Boolean): Instant {
        val today = now.toLocalDate()
        val firstDay = if (checkedToday) today.plusDays(1) else today
        val date = maxOf(firstDay, settings.lastSentDate?.plusDays(1) ?: firstDay)
        val target = date.atTime(settings.time).atZone(now.zone).toInstant()
        return maxOf(target, now.toInstant())
    }
}
