package com.manna.meeting.interfaces.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.manna.meeting.application.info.MeetingInfo
import com.manna.meeting.domain.entity.MeetingStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class ParticipantDto(
    val id: Long,
    val nickname: String,
)

data class MeetingResponse(
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
    val responseCount: Int,
    val participants: List<ParticipantDto>,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val isParticipant: Boolean? = null,
    val hasRevote: Boolean = false,
) {
    companion object {
        fun from(info: MeetingInfo) = MeetingResponse(
            id = info.id,
            hostId = info.hostId,
            title = info.title,
            description = info.description,
            dateRangeStart = info.dateRangeStart,
            dateRangeEnd = info.dateRangeEnd,
            confirmedDate = info.confirmedDate,
            status = info.status,
            createdAt = info.createdAt,
            participantCount = info.participantCount,
            responseCount = info.responseCount,
            participants = info.participants.map { ParticipantDto(it.id, it.nickname) },
            isParticipant = info.isParticipant,
            hasRevote = info.hasRevote,
        )
    }
}