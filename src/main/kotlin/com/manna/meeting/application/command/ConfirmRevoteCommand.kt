package com.manna.meeting.application.command

import java.time.LocalDate

data class ConfirmRevoteCommand(
    val meetingId: Long,
    val userId: Long,
    val confirmedDate: LocalDate,
)
