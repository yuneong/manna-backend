package com.manna.meeting.application.facade

import com.manna.meeting.application.command.ConfirmDateCommand
import com.manna.meeting.application.command.CreateMeetingCommand
import com.manna.meeting.application.command.JoinMeetingCommand
import com.manna.meeting.application.command.UpdateAvailabilityCommand
import com.manna.meeting.application.info.AvailabilityHeatmapInfo
import com.manna.meeting.application.info.MeetingInfo
import com.manna.meeting.domain.service.MeetingDomainService
import org.springframework.stereotype.Component

@Component
class MeetingFacade(private val meetingDomainService: MeetingDomainService) {

    fun createMeeting(command: CreateMeetingCommand): MeetingInfo {
        val meeting = meetingDomainService.create(command)
        val count = meetingDomainService.getParticipantCount(meeting.id)
        return MeetingInfo.from(meeting, count)
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
        val count = meetingDomainService.getParticipantCount(meeting.id)
        return MeetingInfo.from(meeting, count)
    }

    fun getMeeting(meetingId: Long, userId: Long): MeetingInfo {
        val meeting = meetingDomainService.getById(meetingId)
        val count = meetingDomainService.getParticipantCount(meetingId)
        val isParticipant = meetingDomainService.isParticipant(meetingId, userId)
        return MeetingInfo.from(meeting, count, isParticipant)
    }

    fun getMyMeetings(userId: Long): List<MeetingInfo> =
        meetingDomainService.getMyMeetings(userId).map { meeting ->
            MeetingInfo.from(meeting, meetingDomainService.getParticipantCount(meeting.id))
        }

    fun getMyAvailability(meetingId: Long, userId: Long): List<String> =
        meetingDomainService.getMyAvailability(meetingId, userId).map { it.toString() }
}
