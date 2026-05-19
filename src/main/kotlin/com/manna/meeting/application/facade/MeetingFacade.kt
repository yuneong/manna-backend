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
        return MeetingInfo.from(meeting)
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
        return MeetingInfo.from(meeting)
    }

    fun getMeeting(meetingId: Long): MeetingInfo {
        val meeting = meetingDomainService.getById(meetingId)
        return MeetingInfo.from(meeting)
    }

    fun getMyMeetings(userId: Long): List<MeetingInfo> =
        meetingDomainService.getMyMeetings(userId).map { MeetingInfo.from(it) }
}
