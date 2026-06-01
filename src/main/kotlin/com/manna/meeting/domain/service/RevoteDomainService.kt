package com.manna.meeting.domain.service

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import com.manna.meeting.application.command.ConfirmRevoteCommand
import com.manna.meeting.application.command.CreateRevoteCommand
import com.manna.meeting.application.command.VoteRevoteCommand
import com.manna.meeting.domain.entity.MeetingStatus
import com.manna.meeting.domain.entity.Revote
import com.manna.meeting.domain.entity.RevoteCandidate
import com.manna.meeting.domain.entity.RevoteStatus
import com.manna.meeting.domain.entity.RevoteVote
import com.manna.meeting.domain.repository.MeetingRepository
import com.manna.meeting.domain.repository.RevoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

data class RevoteStatusData(
    val revote: Revote,
    val candidates: List<RevoteCandidate>,
    val votes: List<RevoteVote>,
    val myVotedDate: LocalDate?,
    val totalCount: Int,
)

@Service
@Transactional(readOnly = true)
class RevoteDomainService(
    private val revoteRepository: RevoteRepository,
    private val meetingRepository: MeetingRepository,
) {

    @Transactional
    fun createRevote(command: CreateRevoteCommand): Revote {
        val meeting = meetingRepository.findById(command.meetingId)
            ?: throw MannaException(ErrorCode.MEETING_NOT_FOUND)
        if (!meeting.isHost(command.userId)) throw MannaException(ErrorCode.NOT_MEETING_HOST)
        if (meeting.status == MeetingStatus.CANCELLED) throw MannaException(ErrorCode.MEETING_NOT_OPEN)
        if (revoteRepository.findOpenByMeetingId(command.meetingId) != null) {
            throw MannaException(ErrorCode.REVOTE_ALREADY_EXISTS)
        }
        if (command.candidateDates.size < 2) throw MannaException(ErrorCode.REVOTE_INVALID_CANDIDATE_DATE)

        val heatmapDates = meetingRepository.findSchedulesByMeetingId(command.meetingId)
            .map { it.scheduledDate }.toSet()
        command.candidateDates.forEach { date ->
            if (!heatmapDates.contains(date)) throw MannaException(ErrorCode.REVOTE_INVALID_CANDIDATE_DATE)
        }

        val revote = revoteRepository.save(Revote(meetingId = command.meetingId))
        command.candidateDates.forEach { date ->
            revoteRepository.saveCandidate(RevoteCandidate(revoteId = revote.id, candidateDate = date))
        }
        return revote
    }

    @Transactional
    fun vote(command: VoteRevoteCommand) {
        meetingRepository.findParticipantByMeetingIdAndUserId(command.meetingId, command.userId)
            ?: throw MannaException(ErrorCode.NOT_MEETING_PARTICIPANT)

        val revote = revoteRepository.findOpenByMeetingId(command.meetingId)
            ?: throw MannaException(ErrorCode.REVOTE_NOT_FOUND)

        if (!revoteRepository.existsCandidateByRevoteIdAndDate(revote.id, command.votedDate)) {
            throw MannaException(ErrorCode.REVOTE_INVALID_CANDIDATE_DATE)
        }
        val existingVote = revoteRepository.findVoteByRevoteIdAndUserId(revote.id, command.userId)
        if (existingVote != null) {
            existingVote.votedDate = command.votedDate
            revoteRepository.saveVote(existingVote)
        } else {
            revoteRepository.saveVote(RevoteVote(revoteId = revote.id, userId = command.userId, votedDate = command.votedDate))
        }

        val totalParticipants = meetingRepository.findParticipantsByMeetingId(command.meetingId).size
        val totalVotes = revoteRepository.countVotesByRevoteId(revote.id)
        if (totalVotes == totalParticipants) {
            autoResolve(revote, command.meetingId)
        }
    }

    private fun autoResolve(revote: Revote, meetingId: Long) {
        val votes = revoteRepository.findVotesByRevoteId(revote.id)
        val voteCounts = votes.groupBy { it.votedDate }.mapValues { it.value.size }
        val maxVotes = voteCounts.values.maxOrNull() ?: return
        val winners = voteCounts.filter { it.value == maxVotes }.keys

        if (winners.size == 1) {
            val meeting = meetingRepository.findById(meetingId) ?: return
            meeting.confirmedDate = winners.first()
            meeting.status = MeetingStatus.CONFIRMED
            meetingRepository.save(meeting)
            // revote는 OPEN 유지 — 방장이 /revote/confirm 호출 시 CLOSED로 변경
        }
    }

    fun getRevoteStatusData(meetingId: Long, userId: Long): RevoteStatusData {
        val revote = revoteRepository.findLatestByMeetingId(meetingId)
            ?: throw MannaException(ErrorCode.REVOTE_NOT_FOUND)
        val candidates = revoteRepository.findCandidatesByRevoteId(revote.id)
        val votes = revoteRepository.findVotesByRevoteId(revote.id)
        val myVote = votes.find { it.userId == userId }
        val totalCount = meetingRepository.findParticipantsByMeetingId(meetingId).size
        return RevoteStatusData(
            revote = revote,
            candidates = candidates,
            votes = votes,
            myVotedDate = myVote?.votedDate,
            totalCount = totalCount,
        )
    }

    @Transactional
    fun confirmRevote(command: ConfirmRevoteCommand) {
        val meeting = meetingRepository.findById(command.meetingId)
            ?: throw MannaException(ErrorCode.MEETING_NOT_FOUND)
        if (!meeting.isHost(command.userId)) throw MannaException(ErrorCode.NOT_MEETING_HOST)

        val revote = revoteRepository.findOpenByMeetingId(command.meetingId)
            ?: throw MannaException(ErrorCode.REVOTE_NOT_FOUND)

        val totalParticipants = meetingRepository.findParticipantsByMeetingId(command.meetingId).size
        val totalVotes = revoteRepository.countVotesByRevoteId(revote.id)
        if (totalVotes < totalParticipants) throw MannaException(ErrorCode.REVOTE_NOT_COMPLETED)

        if (!revoteRepository.existsCandidateByRevoteIdAndDate(revote.id, command.confirmedDate)) {
            throw MannaException(ErrorCode.REVOTE_INVALID_CANDIDATE_DATE)
        }

        revote.status = RevoteStatus.CLOSED
        revoteRepository.save(revote)
        meeting.confirmedDate = command.confirmedDate
        meeting.status = MeetingStatus.CONFIRMED
        meetingRepository.save(meeting)
    }

    @Transactional
    fun cancelRevote(meetingId: Long, userId: Long) {
        val meeting = meetingRepository.findById(meetingId)
            ?: throw MannaException(ErrorCode.MEETING_NOT_FOUND)
        if (!meeting.isHost(userId)) throw MannaException(ErrorCode.NOT_MEETING_HOST)

        val revote = revoteRepository.findOpenByMeetingId(meetingId)
            ?: throw MannaException(ErrorCode.REVOTE_NOT_FOUND)

        revoteRepository.deleteVotesByRevoteId(revote.id)
        revoteRepository.deleteCandidatesByRevoteId(revote.id)
        revoteRepository.deleteRevotesByMeetingId(meetingId)
    }

    fun hasOpenRevote(meetingId: Long): Boolean =
        revoteRepository.findOpenByMeetingId(meetingId) != null

    @Transactional
    fun deleteAllByMeetingId(meetingId: Long) {
        val revotes = revoteRepository.findAllByMeetingId(meetingId)
        revotes.forEach { revote ->
            revoteRepository.deleteVotesByRevoteId(revote.id)
            revoteRepository.deleteCandidatesByRevoteId(revote.id)
        }
        revoteRepository.deleteRevotesByMeetingId(meetingId)
    }
}
