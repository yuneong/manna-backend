package com.manna.meeting.domain.entity

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "meetings")
class Meeting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    val hostId: Long,

    @Column(nullable = false)
    val title: String,

    val description: String? = null,

    @Column(nullable = false)
    val dateRangeStart: LocalDate,

    @Column(nullable = false)
    val dateRangeEnd: LocalDate,

    var confirmedDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MeetingStatus = MeetingStatus.OPEN,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {

    fun confirmDate(userId: Long, date: LocalDate) {
        if (hostId != userId) throw MannaException(ErrorCode.NOT_MEETING_HOST)
        if (status != MeetingStatus.OPEN) throw MannaException(ErrorCode.MEETING_NOT_OPEN)
        if (date < dateRangeStart || date > dateRangeEnd) throw MannaException(ErrorCode.DATE_OUT_OF_RANGE)
        confirmedDate = date
        status = MeetingStatus.CONFIRMED
    }

    fun isHost(userId: Long) = hostId == userId

    fun requireOpen() {
        if (status != MeetingStatus.OPEN) throw MannaException(ErrorCode.MEETING_NOT_OPEN)
    }
}
