package com.manna.meeting.application.command

data class JoinMeetingCommand(
    val meetingId: Long,
    val userId: Long,
)
