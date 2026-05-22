package com.manna.meeting.application.command

import java.time.LocalDate

data class VoteRevoteCommand(
    val meetingId: Long,
    val userId: Long,
    val votedDate: LocalDate,
)
