package com.manna.meeting.application.info

data class ScheduleHeatmapInfo(
    val meetingId: Long,
    val heatmap: Map<String, List<Long>>,
)
