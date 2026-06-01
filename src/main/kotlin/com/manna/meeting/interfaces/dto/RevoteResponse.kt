package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.info.RevoteInfo
import com.manna.meeting.domain.entity.RevoteStatus
import java.time.LocalDate

data class VoterDto(
    val id: Long,
    val nickname: String,
)

data class RevoteCandidateDto(
    val date: LocalDate,
    val count: Int,
    val voters: List<VoterDto>,
)

data class RevoteResponse(
    val status: RevoteStatus,
    val candidates: List<RevoteCandidateDto>,
    val votedCount: Int,
    val totalCount: Int,
    val myVotedDate: LocalDate?,
) {
    companion object {
        fun from(info: RevoteInfo) = RevoteResponse(
            status = info.status,
            candidates = info.candidates.map { c ->
                RevoteCandidateDto(c.date, c.count, c.voters.map { VoterDto(it.id, it.nickname) })
            },
            votedCount = info.votedCount,
            totalCount = info.totalCount,
            myVotedDate = info.myVotedDate,
        )
    }
}
