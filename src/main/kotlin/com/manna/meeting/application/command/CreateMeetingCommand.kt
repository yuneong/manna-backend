package com.manna.meeting.application.command

import java.time.LocalDate

data class CreateMeetingCommand(
    val hostId: Long,
    val title: String,
    val description: String?,
    val dateRangeStart: LocalDate,
    val dateRangeEnd: LocalDate,
)
