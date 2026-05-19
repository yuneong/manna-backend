package com.manna.meeting.infrastructure.jpa

import com.manna.meeting.domain.entity.Availability
import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MeetingJpaRepository : JpaRepository<Meeting, Long> {
    @Query("SELECT m FROM Meeting m JOIN MeetingParticipant mp ON m.id = mp.meeting.id WHERE mp.userId = :userId")
    fun findAllByUserId(userId: Long): List<Meeting>
}

interface MeetingParticipantJpaRepository : JpaRepository<MeetingParticipant, Long> {
    fun findByMeetingIdAndUserId(meetingId: Long, userId: Long): MeetingParticipant?
    fun findByMeetingId(meetingId: Long): List<MeetingParticipant>
}

interface AvailabilityJpaRepository : JpaRepository<Availability, Long> {
    fun deleteByMeetingIdAndUserId(meetingId: Long, userId: Long)
    fun findByMeetingId(meetingId: Long): List<Availability>
}
