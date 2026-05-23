package com.manna.meeting.domain.repository

import com.manna.meeting.domain.entity.Revote
import com.manna.meeting.domain.entity.RevoteCandidate
import com.manna.meeting.domain.entity.RevoteVote
import java.time.LocalDate

interface RevoteRepository {
    fun save(revote: Revote): Revote
    fun findOpenByMeetingId(meetingId: Long): Revote?
    fun findLatestByMeetingId(meetingId: Long): Revote?
    fun findAllByMeetingId(meetingId: Long): List<Revote>
    fun deleteRevotesByMeetingId(meetingId: Long)

    fun saveCandidate(candidate: RevoteCandidate): RevoteCandidate
    fun findCandidatesByRevoteId(revoteId: Long): List<RevoteCandidate>
    fun existsCandidateByRevoteIdAndDate(revoteId: Long, date: LocalDate): Boolean
    fun deleteCandidatesByRevoteId(revoteId: Long)

    fun saveVote(vote: RevoteVote): RevoteVote
    fun findVotesByRevoteId(revoteId: Long): List<RevoteVote>
    fun findVoteByRevoteIdAndUserId(revoteId: Long, userId: Long): RevoteVote?
    fun countVotesByRevoteId(revoteId: Long): Int
    fun deleteVotesByRevoteId(revoteId: Long)
}
