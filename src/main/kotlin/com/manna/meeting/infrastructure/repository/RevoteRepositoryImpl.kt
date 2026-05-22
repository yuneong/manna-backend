package com.manna.meeting.infrastructure.repository

import com.manna.meeting.domain.entity.Revote
import com.manna.meeting.domain.entity.RevoteCandidate
import com.manna.meeting.domain.entity.RevoteStatus
import com.manna.meeting.domain.entity.RevoteVote
import com.manna.meeting.domain.repository.RevoteRepository
import com.manna.meeting.infrastructure.jpa.RevoteCandidateJpaRepository
import com.manna.meeting.infrastructure.jpa.RevoteJpaRepository
import com.manna.meeting.infrastructure.jpa.RevoteVoteJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class RevoteRepositoryImpl(
    private val revoteJpaRepository: RevoteJpaRepository,
    private val candidateJpaRepository: RevoteCandidateJpaRepository,
    private val voteJpaRepository: RevoteVoteJpaRepository,
) : RevoteRepository {

    override fun save(revote: Revote): Revote = revoteJpaRepository.save(revote)

    override fun findOpenByMeetingId(meetingId: Long): Revote? =
        revoteJpaRepository.findByMeetingIdAndStatus(meetingId, RevoteStatus.OPEN)

    override fun findLatestByMeetingId(meetingId: Long): Revote? =
        revoteJpaRepository.findTopByMeetingIdOrderByCreatedAtDesc(meetingId)

    override fun saveCandidate(candidate: RevoteCandidate): RevoteCandidate =
        candidateJpaRepository.save(candidate)

    override fun findCandidatesByRevoteId(revoteId: Long): List<RevoteCandidate> =
        candidateJpaRepository.findByRevoteId(revoteId)

    override fun existsCandidateByRevoteIdAndDate(revoteId: Long, date: LocalDate): Boolean =
        candidateJpaRepository.existsByRevoteIdAndCandidateDate(revoteId, date)

    override fun saveVote(vote: RevoteVote): RevoteVote = voteJpaRepository.save(vote)

    override fun findVotesByRevoteId(revoteId: Long): List<RevoteVote> =
        voteJpaRepository.findByRevoteId(revoteId)

    override fun findVoteByRevoteIdAndUserId(revoteId: Long, userId: Long): RevoteVote? =
        voteJpaRepository.findByRevoteIdAndUserId(revoteId, userId)

    override fun countVotesByRevoteId(revoteId: Long): Int =
        voteJpaRepository.countByRevoteId(revoteId)
}
