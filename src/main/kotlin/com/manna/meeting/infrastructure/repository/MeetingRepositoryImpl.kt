package com.manna.meeting.infrastructure.repository

import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
import com.manna.meeting.domain.entity.MeetingSchedule
import com.manna.meeting.domain.repository.MeetingRepository
import com.manna.meeting.infrastructure.jpa.MeetingJpaRepository
import com.manna.meeting.infrastructure.jpa.MeetingParticipantJpaRepository
import com.manna.meeting.infrastructure.jpa.MeetingScheduleJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class MeetingRepositoryImpl(
    private val meetingJpaRepository: MeetingJpaRepository,
    private val participantJpaRepository: MeetingParticipantJpaRepository,
    private val scheduleJpaRepository: MeetingScheduleJpaRepository,
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

    override fun saveSchedule(schedule: MeetingSchedule): MeetingSchedule =
        scheduleJpaRepository.save(schedule)

    @Transactional
    override fun deleteSchedulesByMeetingIdAndUserId(meetingId: Long, userId: Long) =
        scheduleJpaRepository.deleteByMeetingIdAndUserId(meetingId, userId)

    override fun findSchedulesByMeetingId(meetingId: Long): List<MeetingSchedule> =
        scheduleJpaRepository.findByMeetingId(meetingId)

    override fun findSchedulesByMeetingIds(meetingIds: List<Long>): List<MeetingSchedule> =
        scheduleJpaRepository.findByMeetingIdIn(meetingIds)

    override fun findSchedulesByMeetingIdAndUserId(meetingId: Long, userId: Long): List<MeetingSchedule> =
        scheduleJpaRepository.findByMeetingIdAndUserId(meetingId, userId)
}
