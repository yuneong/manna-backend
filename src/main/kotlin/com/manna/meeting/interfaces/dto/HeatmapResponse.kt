package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.info.ScheduleHeatmapInfo

data class DateScheduleEntry(
    val count: Int,
    val availableParticipantIds: List<Long>,
)

data class HeatmapResponse(
    val meetingId: Long,
    val heatmap: Map<String, DateScheduleEntry>,
) {
    companion object {
        fun from(info: ScheduleHeatmapInfo) = HeatmapResponse(
            meetingId = info.meetingId,
            heatmap = info.heatmap.mapValues { (_, userIds) ->
                DateScheduleEntry(count = userIds.size, availableParticipantIds = userIds)
            },
        )
    }
}
