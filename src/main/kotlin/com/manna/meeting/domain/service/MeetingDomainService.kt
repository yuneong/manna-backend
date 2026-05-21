package com.manna.meeting.domain.service

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import com.manna.meeting.application.command.ConfirmDateCommand
import com.manna.meeting.application.command.CreateMeetingCommand
import com.manna.meeting.application.command.JoinMeetingCommand
import com.manna.meeting.application.command.UpdateAvailabilityCommand
import com.manna.meeting.domain.entity.Availability
import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
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
    fun updateAvailability(command: UpdateAvailabilityCommand) {
        val meeting = getById(command.meetingId)
        meeting.requireOpen()
        meetingRepository.deleteAvailabilitiesByMeetingIdAndUserId(command.meetingId, command.userId)
        command.availableDates.forEach { date ->
            if (date < meeting.dateRangeStart || date > meeting.dateRangeEnd) {
                throw MannaException(ErrorCode.DATE_OUT_OF_RANGE)
            }
            meetingRepository.saveAvailability(Availability(meeting = meeting, userId = command.userId, availableDate = date))
        }
    }

    @Transactional
    fun confirmDate(command: ConfirmDateCommand): Meeting {
        val meeting = getById(command.meetingId)
        meeting.confirmDate(command.userId, command.confirmedDate)
        return meetingRepository.save(meeting)
    }

    fun getById(id: Long): Meeting =
        meetingRepository.findById(id) ?: throw MannaException(ErrorCode.MEETING_NOT_FOUND)

    fun getAvailabilityHeatmap(meetingId: Long): Map<String, Int> {
        val availabilities = meetingRepository.findAvailabilitiesByMeetingId(meetingId)
        return availabilities
            .groupBy { it.availableDate.toString() }
            .mapValues { (_, list) -> list.size }
    }

    fun getMyMeetings(userId: Long): List<Meeting> =
        meetingRepository.findAllByUserId(userId)

    fun getParticipantCount(meetingId: Long): Int =
        meetingRepository.findParticipantsByMeetingId(meetingId).size

    fun getParticipantsByMeetingIds(meetingIds: List<Long>): List<MeetingParticipant> =
        meetingRepository.findParticipantsByMeetingIds(meetingIds)

    fun isParticipant(meetingId: Long, userId: Long): Boolean =
        meetingRepository.findParticipantByMeetingIdAndUserId(meetingId, userId) != null

    fun getAvailabilitiesByMeetingIds(meetingIds: List<Long>): List<Availability> =
        meetingRepository.findAvailabilitiesByMeetingIds(meetingIds)

    fun getMyAvailability(meetingId: Long, userId: Long): List<LocalDate> {
        getById(meetingId)
        return meetingRepository.findAvailabilitiesByMeetingIdAndUserId(meetingId, userId)
            .map { it.availableDate }
    }
}
