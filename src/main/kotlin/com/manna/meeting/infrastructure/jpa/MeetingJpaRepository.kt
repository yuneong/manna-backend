package com.manna.meeting.infrastructure.jpa

import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
import com.manna.meeting.domain.entity.MeetingSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MeetingJpaRepository : JpaRepository<Meeting, Long> {
    @Query("SELECT m FROM Meeting m JOIN MeetingParticipant mp ON m.id = mp.meeting.id WHERE mp.userId = :userId")
    fun findAllByUserId(userId: Long): List<Meeting>
}

interface MeetingParticipantJpaRepository : JpaRepository<MeetingParticipant, Long> {
    fun findByMeetingIdAndUserId(meetingId: Long, userId: Long): MeetingParticipant?
    fun findByMeetingId(meetingId: Long): List<MeetingParticipant>
    fun findByMeetingIdIn(meetingIds: List<Long>): List<MeetingParticipant>
    fun deleteByMeetingId(meetingId: Long)
}

interface MeetingScheduleJpaRepository : JpaRepository<MeetingSchedule, Long> {
    fun deleteByMeetingIdAndUserId(meetingId: Long, userId: Long)
    fun deleteByMeetingId(meetingId: Long)
    fun findByMeetingId(meetingId: Long): List<MeetingSchedule>
    fun findByMeetingIdIn(meetingIds: List<Long>): List<MeetingSchedule>
    fun findByMeetingIdAndUserId(meetingId: Long, userId: Long): List<MeetingSchedule>
}
