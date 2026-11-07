package com.maxiaol624.wordtrail.reminder

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderScheduleTest {
    private val settings = ReminderSettings(enabled = true)
    private val target = ZonedDateTime.parse("2026-11-07T20:00:00+08:00[Asia/Shanghai]")

    @Test fun disabledOrEarlyWorkDoesNotCheckWords() {
        val early = target.minusMinutes(1)
        assertFalse(ReminderSchedule.shouldCheck(early, settings))
        assertFalse(ReminderSchedule.shouldCheck(target.plusMinutes(5), settings.copy(enabled = false)))
        assertEquals(target.toInstant(), ReminderSchedule.nextCheck(early, settings, checkedToday = false))
    }

    @Test fun workCanCheckAtTheBoundaryOrWhenTheSystemRunsLate() {
        assertTrue(ReminderSchedule.shouldCheck(target, settings))
        val late = target.plusHours(2)
        assertTrue(ReminderSchedule.shouldCheck(late, settings))
        assertEquals(late.toInstant(), ReminderSchedule.nextCheck(late, settings, checkedToday = false))
    }

    @Test fun aCompletedCheckKeepsTomorrowAtTheConfiguredLocalTime() {
        val late = target.plusHours(2)
        assertEquals(target.plusDays(1).toInstant(), ReminderSchedule.nextCheck(late, settings, checkedToday = true))
    }

    @Test fun changingTimeOrReEnablingDoesNotRepeatAnAlreadySentReminder() {
        val sent = settings.copy(lastSentDate = target.toLocalDate(), time = LocalTime.of(21, 30))
        assertFalse(ReminderSchedule.shouldCheck(target.plusHours(2), sent))
        assertFalse(ReminderSchedule.shouldCheck(target.minusDays(1).plusHours(2), sent))
        val tomorrow = target.plusDays(1).withHour(21).withMinute(30)
        assertEquals(tomorrow.toInstant(), ReminderSchedule.nextCheck(target, sent, checkedToday = false))
        assertTrue(ReminderSchedule.shouldCheck(tomorrow, sent))
    }

    @Test fun dayWithDaylightSavingChangeDoesNotUseFixed24Hours() {
        val before = ZonedDateTime.parse("2026-03-07T20:00:00-05:00[America/New_York]")
        val next = ReminderSchedule.nextCheck(before, settings, checkedToday = true)
        assertEquals(before.plusDays(1).toInstant(), next)
        assertEquals(23L, Duration.between(before.toInstant(), next).toHours())
    }

    @Test fun nonexistentLocalTimeIsMovedThroughDaylightSavingGap() {
        val gapSettings = settings.copy(time = LocalTime.of(2, 30))
        val early = ZonedDateTime.parse("2026-03-08T03:00:00-04:00[America/New_York]")
        assertFalse(ReminderSchedule.shouldCheck(early, gapSettings))
        assertEquals(early.plusMinutes(30).toInstant(), ReminderSchedule.nextCheck(early, gapSettings, checkedToday = false))
        assertTrue(ReminderSchedule.shouldCheck(early.plusMinutes(30), gapSettings))
    }
}
