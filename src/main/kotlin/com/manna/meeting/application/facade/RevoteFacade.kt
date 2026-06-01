package com.manna.meeting.application.facade

import com.manna.meeting.application.command.ConfirmRevoteCommand
import com.manna.meeting.application.command.CreateRevoteCommand
import com.manna.meeting.application.command.VoteRevoteCommand
import com.manna.meeting.application.info.RevoteCandidateInfo
import com.manna.meeting.application.info.RevoteInfo
import com.manna.meeting.application.info.VoterInfo
import com.manna.meeting.domain.service.RevoteDomainService
import com.manna.meeting.domain.service.RevoteStatusData
import com.manna.user.domain.service.UserDomainService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RevoteFacade(
    private val revoteDomainService: RevoteDomainService,
    private val userDomainService: UserDomainService,
) {

    fun createRevote(command: CreateRevoteCommand): RevoteInfo {
        revoteDomainService.createRevote(command)
        val data = revoteDomainService.getRevoteStatusData(command.meetingId, command.userId)
        return buildRevoteInfo(data)
    }

    fun vote(command: VoteRevoteCommand) {
        revoteDomainService.vote(command)
    }

    fun getRevoteStatus(meetingId: Long, userId: Long): RevoteInfo {
        val data = revoteDomainService.getRevoteStatusData(meetingId, userId)
        return buildRevoteInfo(data)
    }

    fun confirmRevote(command: ConfirmRevoteCommand) {
        revoteDomainService.confirmRevote(command)
    }

    private fun buildRevoteInfo(data: RevoteStatusData): RevoteInfo {
        val voterIds = data.votes.map { it.userId }.distinct()
        val userMap = if (voterIds.isEmpty()) emptyMap()
        else userDomainService.getUsersByIds(voterIds).associateBy { it.id }

        val votesByDate = data.votes.groupBy { it.votedDate }
        val candidateInfos = data.candidates.map { candidate ->
            val dateVotes = votesByDate[candidate.candidateDate] ?: emptyList()
            RevoteCandidateInfo(
                date = candidate.candidateDate,
                count = dateVotes.size,
                voters = dateVotes.mapNotNull { vote -> userMap[vote.userId]?.let { VoterInfo(it.id, it.nickname) } },
            )
        }
        return RevoteInfo(
            status = data.revote.status,
            candidates = candidateInfos,
            votedCount = data.votes.size,
            totalCount = data.totalCount,
            myVotedDate = data.myVotedDate,
        )
    }
}
