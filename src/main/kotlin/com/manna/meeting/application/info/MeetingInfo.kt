package com.manna.meeting.application.info

import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class ParticipantInfo(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String? = null,
)

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
    val participants: List<ParticipantInfo>,
    val responseCount: Int = 0,
    val isParticipant: Boolean? = null,
    val hasRevote: Boolean = false,
) {
    val participantCount: Int get() = participants.size

    companion object {
        fun from(
            meeting: Meeting,
            participants: List<ParticipantInfo>,
            responseCount: Int = 0,
            isParticipant: Boolean? = null,
            hasRevote: Boolean = false,
        ) = MeetingInfo(
            id = meeting.id,
            hostId = meeting.hostId,
            title = meeting.title,
            description = meeting.description,
            dateRangeStart = meeting.dateRangeStart,
            dateRangeEnd = meeting.dateRangeEnd,
            confirmedDate = meeting.confirmedDate,
            status = meeting.status,
            createdAt = meeting.createdAt,
            participants = participants,
            responseCount = responseCount,
            isParticipant = isParticipant,
            hasRevote = hasRevote,
        )
    }
}