package com.manna.meeting.application.command

import java.time.LocalDate

data class UpdateMeetingCommand(
    val meetingId: Long,
    val userId: Long,
    val title: String,
    val description: String?,
    val dateRangeStart: LocalDate,
    val dateRangeEnd: LocalDate,
)