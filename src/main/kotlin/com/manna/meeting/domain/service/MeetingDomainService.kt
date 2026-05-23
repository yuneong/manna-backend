package com.manna.meeting.domain.service

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import com.manna.meeting.application.command.CancelConfirmCommand
import com.manna.meeting.application.command.ConfirmDateCommand
import com.manna.meeting.application.command.CreateMeetingCommand
import com.manna.meeting.application.command.JoinMeetingCommand
import com.manna.meeting.application.command.UpdateMeetingCommand
import com.manna.meeting.application.command.UpdateScheduleCommand
import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
import com.manna.meeting.domain.entity.MeetingSchedule
import com.manna.meeting.domain.entity.MeetingStatus
import com.manna.meeting.domain.repository.MeetingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class MeetingDomainService(private val meetingRepository: MeetingRepository) {

    @Transactional
    fun create(command: CreateMeetingCommand): Meeting {
        val meeting = meetingRepository.save(
            Meeting(
                hostId = command.hostId,
                title = command.title,
                description = command.description,
                dateRangeStart = command.dateRangeStart,
                dateRangeEnd = command.dateRangeEnd,
            ),
        )
        meetingRepository.saveParticipant(MeetingParticipant(meeting = meeting, userId = command.hostId))
        return meeting
    }

    @Transactional
    fun join(command: JoinMeetingCommand): MeetingParticipant {
        val meeting = getById(command.meetingId)
        meeting.requireOpen()
        if (meetingRepository.findParticipantByMeetingIdAndUserId(command.meetingId, command.userId) != null) {
            throw MannaException(ErrorCode.ALREADY_JOINED)
        }
        return meetingRepository.saveParticipant(MeetingParticipant(meeting = meeting, userId = command.userId))
    }

    @Transactional
    fun cancelConfirm(command: CancelConfirmCommand): Meeting {
        val meeting = getById(command.meetingId)
        meeting.cancelConfirm(command.userId)
        return meetingRepository.save(meeting)
    }

    @Transactional
    fun updateSchedule(command: UpdateScheduleCommand) {
        val meeting = getById(command.meetingId)
        if (meeting.status == MeetingStatus.CONFIRMED) throw MannaException(ErrorCode.MEETING_ALREADY_CONFIRMED)
        meeting.requireOpen()
        meetingRepository.deleteSchedulesByMeetingIdAndUserId(command.meetingId, command.userId)
        command.scheduledDates.forEach { date ->
            if (date < meeting.dateRangeStart || date > meeting.dateRangeEnd) {
                throw MannaException(ErrorCode.DATE_OUT_OF_RANGE)
            }
            meetingRepository.saveSchedule(MeetingSchedule(meeting = meeting, userId = command.userId, scheduledDate = date))
        }
    }

    @Transactional
    fun update(command: UpdateMeetingCommand): Meeting {
        val meeting = getById(command.meetingId)
        val dateRangeChanged = meeting.update(command.userId, command.title, command.description, command.dateRangeStart, command.dateRangeEnd)
        if (dateRangeChanged) {
            meetingRepository.deleteSchedulesByMeetingId(command.meetingId)
        }
        return meetingRepository.save(meeting)
    }

    @Transactional
    fun delete(meetingId: Long, userId: Long) {
        val meeting = getById(meetingId)
        if (!meeting.isHost(userId)) throw MannaException(ErrorCode.NOT_MEETING_HOST)
        meetingRepository.deleteSchedulesByMeetingId(meetingId)
        meetingRepository.deleteParticipantsByMeetingId(meetingId)
        meetingRepository.delete(meeting)
    }

    @Transactional
    fun confirmDate(command: ConfirmDateCommand): Meeting {
        val meeting = getById(command.meetingId)
        meeting.confirmDate(command.userId, command.confirmedDate)
        return meetingRepository.save(meeting)
    }

    fun getById(id: Long): Meeting =
        meetingRepository.findById(id) ?: throw MannaException(ErrorCode.MEETING_NOT_FOUND)

    fun getScheduleHeatmap(meetingId: Long): Map<String, List<Long>> =
        meetingRepository.findSchedulesByMeetingId(meetingId)
            .groupBy { it.scheduledDate.toString() }
            .mapValues { (_, list) -> list.map { it.userId } }

    fun getMyMeetings(userId: Long): List<Meeting> =
        meetingRepository.findAllByUserId(userId)

    fun getParticipantCount(meetingId: Long): Int =
        meetingRepository.findParticipantsByMeetingId(meetingId).size

    fun getParticipantsByMeetingIds(meetingIds: List<Long>): List<MeetingParticipant> =
        meetingRepository.findParticipantsByMeetingIds(meetingIds)

    fun isParticipant(meetingId: Long, userId: Long): Boolean =
        meetingRepository.findParticipantByMeetingIdAndUserId(meetingId, userId) != null

    fun getSchedulesByMeetingIds(meetingIds: List<Long>): List<MeetingSchedule> =
        meetingRepository.findSchedulesByMeetingIds(meetingIds)

    fun getMySchedules(meetingId: Long, userId: Long): List<LocalDate> {
        getById(meetingId)
        return meetingRepository.findSchedulesByMeetingIdAndUserId(meetingId, userId)
            .map { it.scheduledDate }
    }
}
