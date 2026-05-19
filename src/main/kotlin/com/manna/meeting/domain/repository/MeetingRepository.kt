package com.manna.meeting.domain.repository

import com.manna.meeting.domain.entity.Availability
import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant

interface MeetingRepository {
    fun save(meeting: Meeting): Meeting
    fun findById(id: Long): Meeting?
    fun findAllByUserId(userId: Long): List<Meeting>

    fun saveParticipant(participant: MeetingParticipant): MeetingParticipant
    fun findParticipantByMeetingIdAndUserId(meetingId: Long, userId: Long): MeetingParticipant?
    fun findParticipantsByMeetingId(meetingId: Long): List<MeetingParticipant>

    fun saveAvailability(availability: Availability): Availability
    fun deleteAvailabilitiesByMeetingIdAndUserId(meetingId: Long, userId: Long)
    fun findAvailabilitiesByMeetingId(meetingId: Long): List<Availability>
    fun findAvailabilitiesByMeetingIdAndUserId(meetingId: Long, userId: Long): List<Availability>
}
