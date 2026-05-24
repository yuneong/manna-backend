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
    var title: String,

    var description: String? = null,

    @Column(nullable = false)
    var dateRangeStart: LocalDate,

    @Column(nullable = false)
    var dateRangeEnd: LocalDate,

    var confirmedDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MeetingStatus = MeetingStatus.OPEN,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {

    fun confirmDate(userId: Long, date: LocalDate) {
        if (hostId != userId) throw MannaException(ErrorCode.NOT_MEETING_HOST)
        if (status == MeetingStatus.CANCELLED) throw MannaException(ErrorCode.MEETING_NOT_OPEN)
        if (date < dateRangeStart || date > dateRangeEnd) throw MannaException(ErrorCode.DATE_OUT_OF_RANGE)
        confirmedDate = date
        status = MeetingStatus.CONFIRMED
    }

    fun cancelConfirm(userId: Long) {
        if (hostId != userId) throw MannaException(ErrorCode.NOT_MEETING_HOST)
        if (status !in CANCELLABLE_STATUSES) throw MannaException(ErrorCode.MEETING_NOT_CONFIRMED)
        confirmedDate = null
        status = MeetingStatus.OPEN
    }

    fun update(userId: Long, title: String, description: String?, dateRangeStart: LocalDate, dateRangeEnd: LocalDate): Boolean {
        if (hostId != userId) throw MannaException(ErrorCode.NOT_MEETING_HOST)
        val dateRangeChanged = this.dateRangeStart != dateRangeStart || this.dateRangeEnd != dateRangeEnd
        this.title = title
        this.description = description
        if (dateRangeChanged) {
            this.dateRangeStart = dateRangeStart
            this.dateRangeEnd = dateRangeEnd
            confirmedDate = null
            status = MeetingStatus.OPEN
        }
        return dateRangeChanged
    }

    fun isHost(userId: Long) = hostId == userId

    fun requireOpen() {
        if (status != MeetingStatus.OPEN) throw MannaException(ErrorCode.MEETING_NOT_OPEN)
    }

    companion object {
        val CANCELLABLE_STATUSES = setOf(
            MeetingStatus.CONFIRMED,
            MeetingStatus.PLACE_VOTING,
            MeetingStatus.SETTLING,
        )
    }
}
