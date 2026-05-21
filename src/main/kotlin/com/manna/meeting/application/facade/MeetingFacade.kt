package com.manna.meeting.application.facade

import com.manna.meeting.application.command.ConfirmDateCommand
import com.manna.meeting.application.command.CreateMeetingCommand
import com.manna.meeting.application.command.JoinMeetingCommand
import com.manna.meeting.application.command.UpdateAvailabilityCommand
import com.manna.meeting.application.info.AvailabilityHeatmapInfo
import com.manna.meeting.application.info.MeetingInfo
import com.manna.meeting.application.info.ParticipantInfo
import com.manna.meeting.domain.service.MeetingDomainService
import com.manna.user.domain.service.UserDomainService
import org.springframework.stereotype.Component

@Component
class MeetingFacade(
    private val meetingDomainService: MeetingDomainService,
    private val userDomainService: UserDomainService,
) {

    fun createMeeting(command: CreateMeetingCommand): MeetingInfo {
        val meeting = meetingDomainService.create(command)
        val meetingIds = listOf(meeting.id)
        val participants = resolveParticipants(meetingIds)
        val responseCount = resolveResponseCounts(meetingIds)[meeting.id] ?: 0
        return MeetingInfo.from(meeting, participants[meeting.id] ?: emptyList(), responseCount)
    }

    fun joinMeeting(command: JoinMeetingCommand) {
        meetingDomainService.join(command)
    }

    fun updateAvailability(command: UpdateAvailabilityCommand) {
        meetingDomainService.updateAvailability(command)
    }

    fun getHeatmap(meetingId: Long): AvailabilityHeatmapInfo {
        val heatmap = meetingDomainService.getAvailabilityHeatmap(meetingId)
        return AvailabilityHeatmapInfo(meetingId = meetingId, heatmap = heatmap)
    }

    fun confirmDate(command: ConfirmDateCommand): MeetingInfo {
        val meeting = meetingDomainService.confirmDate(command)
        val meetingIds = listOf(meeting.id)
        val participants = resolveParticipants(meetingIds)
        val responseCount = resolveResponseCounts(meetingIds)[meeting.id] ?: 0
        return MeetingInfo.from(meeting, participants[meeting.id] ?: emptyList(), responseCount)
    }

    fun getMeeting(meetingId: Long, userId: Long): MeetingInfo {
        val meeting = meetingDomainService.getById(meetingId)
        val participantsByMeeting = resolveParticipants(listOf(meetingId))
        val participants = participantsByMeeting[meetingId] ?: emptyList()
        val isParticipant = participants.any { it.id == userId }
        val responseCount = resolveResponseCounts(listOf(meetingId))[meetingId] ?: 0
        return MeetingInfo.from(meeting, participants, responseCount, isParticipant)
    }

    fun getMyMeetings(userId: Long): List<MeetingInfo> {
        val meetings = meetingDomainService.getMyMeetings(userId)
        if (meetings.isEmpty()) return emptyList()
        val meetingIds = meetings.map { it.id }
        val participantsByMeeting = resolveParticipants(meetingIds)
        val responseCountByMeeting = resolveResponseCounts(meetingIds)
        return meetings.map { meeting ->
            MeetingInfo.from(
                meeting,
                participantsByMeeting[meeting.id] ?: emptyList(),
                responseCountByMeeting[meeting.id] ?: 0,
            )
        }
    }

    fun getMyAvailability(meetingId: Long, userId: Long): List<String> =
        meetingDomainService.getMyAvailability(meetingId, userId).map { it.toString() }

    private fun resolveResponseCounts(meetingIds: List<Long>): Map<Long, Int> =
        meetingDomainService.getAvailabilitiesByMeetingIds(meetingIds)
            .groupBy { it.meeting.id }
            .mapValues { (_, list) -> list.map { it.userId }.distinct().size }

    private fun resolveParticipants(meetingIds: List<Long>): Map<Long, List<ParticipantInfo>> {
        val allParticipants = meetingDomainService.getParticipantsByMeetingIds(meetingIds)
        if (allParticipants.isEmpty()) return emptyMap()

        val userMap = userDomainService.getUsersByIds(allParticipants.map { it.userId }.distinct())
            .associateBy { it.id }

        return allParticipants.groupBy { it.meeting.id }
            .mapValues { (_, participants) ->
                participants.mapNotNull { p ->
                    userMap[p.userId]?.let { user -> ParticipantInfo(user.id, user.nickname) }
                }
            }
    }
}