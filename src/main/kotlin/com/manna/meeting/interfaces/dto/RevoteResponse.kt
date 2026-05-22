package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.info.RevoteInfo
import com.manna.meeting.domain.entity.RevoteStatus
import java.time.LocalDate

data class RevoteCandidateDto(
    val date: LocalDate,
    val count: Int,
    val voters: List<String>,
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
            candidates = info.candidates.map { RevoteCandidateDto(it.date, it.count, it.voters) },
            votedCount = info.votedCount,
            totalCount = info.totalCount,
            myVotedDate = info.myVotedDate,
        )
    }
}
