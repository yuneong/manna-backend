package com.manna.meeting.application.info

import com.manna.meeting.domain.entity.RevoteStatus
import java.time.LocalDate

data class RevoteCandidateInfo(
    val date: LocalDate,
    val count: Int,
    val voters: List<String>,
)

data class RevoteInfo(
    val status: RevoteStatus,
    val candidates: List<RevoteCandidateInfo>,
    val votedCount: Int,
    val totalCount: Int,
    val myVotedDate: LocalDate?,
)
