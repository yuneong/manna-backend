package com.manna.meeting.application.command

data class CancelConfirmCommand(
    val meetingId: Long,
    val userId: Long,
)
