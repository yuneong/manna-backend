package com.manna.meeting.application.command

import java.time.LocalDate

data class UpdateAvailabilityCommand(
    val meetingId: Long,
    val userId: Long,
    val availableDates: List<LocalDate>,
)
