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
        val participants = resolveParticipants(listOf(meeting.id))
        return MeetingInfo.from(meeting, participants[meeting.id] ?: emptyList())
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
        val participants = resolveParticipants(listOf(meeting.id))
        return MeetingInfo.from(meeting, participants[meeting.id] ?: emptyList())
    }

    fun getMeeting(meetingId: Long, userId: Long): MeetingInfo {
        val meeting = meetingDomainService.getById(meetingId)
        val participantsByMeeting = resolveParticipants(listOf(meetingId))
        val participants = participantsByMeeting[meetingId] ?: emptyList()
        val isParticipant = participants.any { it.id == userId }
        return MeetingInfo.from(meeting, participants, isParticipant)
    }

    fun getMyMeetings(userId: Long): List<MeetingInfo> {
        val meetings = meetingDomainService.getMyMeetings(userId)
        if (meetings.isEmpty()) return emptyList()
        val participantsByMeeting = resolveParticipants(meetings.map { it.id })
        return meetings.map { meeting ->
            MeetingInfo.from(meeting, participantsByMeeting[meeting.id] ?: emptyList())
        }
    }

    fun getMyAvailability(meetingId: Long, userId: Long): List<String> =
        meetingDomainService.getMyAvailability(meetingId, userId).map { it.toString() }

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