package com.manna.meeting.application.info

import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class MeetingInfo(
    val id: Long,
    val hostId: Long,
    val title: String,
    val description: String?,
    val dateRangeStart: LocalDate,
    val dateRangeEnd: LocalDate,
    val confirmedDate: LocalDate?,
    val status: MeetingStatus,
    val createdAt: LocalDateTime,
    val participantCount: Int,
    val isParticipant: Boolean? = null,
) {
    companion object {
        fun from(meeting: Meeting, participantCount: Int, isParticipant: Boolean? = null) = MeetingInfo(
            id = meeting.id,
            hostId = meeting.hostId,
            title = meeting.title,
            description = meeting.description,
            dateRangeStart = meeting.dateRangeStart,
            dateRangeEnd = meeting.dateRangeEnd,
            confirmedDate = meeting.confirmedDate,
            status = meeting.status,
            createdAt = meeting.createdAt,
            participantCount = participantCount,
            isParticipant = isParticipant,
        )
    }
}
