package com.manna.meeting.domain.repository

import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
import com.manna.meeting.domain.entity.MeetingSchedule

interface MeetingRepository {
    fun save(meeting: Meeting): Meeting
    fun delete(meeting: Meeting)
    fun findById(id: Long): Meeting?
    fun findAllByUserId(userId: Long): List<Meeting>

    fun saveParticipant(participant: MeetingParticipant): MeetingParticipant
    fun findParticipantByMeetingIdAndUserId(meetingId: Long, userId: Long): MeetingParticipant?
    fun findParticipantsByMeetingId(meetingId: Long): List<MeetingParticipant>
    fun findParticipantsByMeetingIds(meetingIds: List<Long>): List<MeetingParticipant>
    fun deleteParticipantsByMeetingId(meetingId: Long)

    fun saveSchedule(schedule: MeetingSchedule): MeetingSchedule
    fun deleteSchedulesByMeetingIdAndUserId(meetingId: Long, userId: Long)
    fun deleteSchedulesByMeetingId(meetingId: Long)
    fun findSchedulesByMeetingId(meetingId: Long): List<MeetingSchedule>
    fun findSchedulesByMeetingIds(meetingIds: List<Long>): List<MeetingSchedule>
    fun findSchedulesByMeetingIdAndUserId(meetingId: Long, userId: Long): List<MeetingSchedule>
}
