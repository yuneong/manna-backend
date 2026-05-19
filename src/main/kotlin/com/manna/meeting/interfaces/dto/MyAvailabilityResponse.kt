package com.manna.meeting.interfaces.dto

data class MyAvailabilityResponse(
    val meetingId: Long,
    val availableDates: List<String>,
)
