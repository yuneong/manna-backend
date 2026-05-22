package com.manna.meeting.application.command

import java.time.LocalDate

data class CreateRevoteCommand(
    val meetingId: Long,
    val userId: Long,
    val candidateDates: List<LocalDate>,
)
