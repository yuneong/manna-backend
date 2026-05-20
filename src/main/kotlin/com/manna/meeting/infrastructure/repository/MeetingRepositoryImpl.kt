package com.manna.meeting.infrastructure.repository

import com.manna.meeting.domain.entity.Availability
import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
import com.manna.meeting.domain.repository.MeetingRepository
import com.manna.meeting.infrastructure.jpa.AvailabilityJpaRepository
import com.manna.meeting.infrastructure.jpa.MeetingJpaRepository
import com.manna.meeting.infrastructure.jpa.MeetingParticipantJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class MeetingRepositoryImpl(
    private val meetingJpaRepository: MeetingJpaRepository,
    private val participantJpaRepository: MeetingParticipantJpaRepository,
    private val availabilityJpaRepository: AvailabilityJpaRepository,
) : MeetingRepository {

    override fun save(meeting: Meeting): Meeting =
        meetingJpaRepository.save(meeting)

    override fun findById(id: Long): Meeting? =
        meetingJpaRepository.findById(id).orElse(null)

    override fun findAllByUserId(userId: Long): List<Meeting> =
        meetingJpaRepository.findAllByUserId(userId)

    override fun saveParticipant(participant: MeetingParticipant): MeetingParticipant =
        participantJpaRepository.save(participant)

    override fun findParticipantByMeetingIdAndUserId(meetingId: Long, userId: Long): MeetingParticipant? =
        participantJpaRepository.findByMeetingIdAndUserId(meetingId, userId)

    override fun findParticipantsByMeetingId(meetingId: Long): List<MeetingParticipant> =
        participantJpaRepository.findByMeetingId(meetingId)

    override fun findParticipantsByMeetingIds(meetingIds: List<Long>): List<MeetingParticipant> =
        participantJpaRepository.findByMeetingIdIn(meetingIds)

    override fun saveAvailability(availability: Availability): Availability =
        availabilityJpaRepository.save(availability)

    @Transactional
    override fun deleteAvailabilitiesByMeetingIdAndUserId(meetingId: Long, userId: Long) =
        availabilityJpaRepository.deleteByMeetingIdAndUserId(meetingId, userId)

    override fun findAvailabilitiesByMeetingId(meetingId: Long): List<Availability> =
        availabilityJpaRepository.findByMeetingId(meetingId)

    override fun findAvailabilitiesByMeetingIdAndUserId(meetingId: Long, userId: Long): List<Availability> =
        availabilityJpaRepository.findByMeetingIdAndUserId(meetingId, userId)
}
