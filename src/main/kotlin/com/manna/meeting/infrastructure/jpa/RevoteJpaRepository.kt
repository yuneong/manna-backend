package com.manna.meeting.infrastructure.jpa

import com.manna.meeting.domain.entity.Revote
import com.manna.meeting.domain.entity.RevoteCandidate
import com.manna.meeting.domain.entity.RevoteStatus
import com.manna.meeting.domain.entity.RevoteVote
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface RevoteJpaRepository : JpaRepository<Revote, Long> {
    fun findByMeetingIdAndStatus(meetingId: Long, status: RevoteStatus): Revote?
    fun findTopByMeetingIdOrderByCreatedAtDesc(meetingId: Long): Revote?
}

interface RevoteCandidateJpaRepository : JpaRepository<RevoteCandidate, Long> {
    fun findByRevoteId(revoteId: Long): List<RevoteCandidate>
    fun existsByRevoteIdAndCandidateDate(revoteId: Long, candidateDate: LocalDate): Boolean
}

interface RevoteVoteJpaRepository : JpaRepository<RevoteVote, Long> {
    fun findByRevoteId(revoteId: Long): List<RevoteVote>
    fun findByRevoteIdAndUserId(revoteId: Long, userId: Long): RevoteVote?
    fun countByRevoteId(revoteId: Long): Int
}
