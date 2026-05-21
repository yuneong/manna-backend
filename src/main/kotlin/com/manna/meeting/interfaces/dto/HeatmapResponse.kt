package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.info.AvailabilityHeatmapInfo

data class DateAvailabilityEntry(
    val count: Int,
    val availableParticipantIds: List<Long>,
)

data class HeatmapResponse(
    val meetingId: Long,
    val heatmap: Map<String, DateAvailabilityEntry>,
) {
    companion object {
        fun from(info: AvailabilityHeatmapInfo) = HeatmapResponse(
            meetingId = info.meetingId,
            heatmap = info.heatmap.mapValues { (_, userIds) ->
                DateAvailabilityEntry(count = userIds.size, availableParticipantIds = userIds)
            },
        )
    }
}
