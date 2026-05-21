package com.manna.meeting.application.info

data class AvailabilityHeatmapInfo(
    val meetingId: Long,
    val heatmap: Map<String, List<Long>>,
)
