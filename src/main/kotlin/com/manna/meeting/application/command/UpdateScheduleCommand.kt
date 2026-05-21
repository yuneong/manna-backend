package com.manna.meeting.application.command

import java.time.LocalDate

data class UpdateScheduleCommand(
    val meetingId: Long,
    val userId: Long,
    val scheduledDates: List<LocalDate>,
)
