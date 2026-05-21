package com.manna.meeting.interfaces.dto

data class MyScheduleResponse(
    val meetingId: Long,
    val scheduledDates: List<String>,
)
