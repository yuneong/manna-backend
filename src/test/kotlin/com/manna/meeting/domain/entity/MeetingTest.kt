package com.manna.meeting.domain.entity

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class MeetingTest {

    private val start = LocalDate.of(2025, 6, 1)
    private val end = LocalDate.of(2025, 6, 30)

    private fun meeting(
        hostId: Long = 1L,
        status: MeetingStatus = MeetingStatus.OPEN,
    ) = Meeting(
        hostId = hostId,
        title = "테스트 약속",
        dateRangeStart = start,
        dateRangeEnd = end,
        status = status,
    )

    @Nested
    inner class ConfirmDate {

        @Test
        fun `날짜 확정 성공 시 status CONFIRMED, confirmedDate 설정`() {
            val meeting = meeting(hostId = 1L)
            val date = LocalDate.of(2025, 6, 15)

            meeting.confirmDate(userId = 1L, date = date)

            assertThat(meeting.status).isEqualTo(MeetingStatus.CONFIRMED)
            assertThat(meeting.confirmedDate).isEqualTo(date)
        }

        @Test
        fun `방장이 아닌 사용자 요청 시 NOT_MEETING_HOST 예외`() {
            val meeting = meeting(hostId = 1L)

            val ex = assertThrows<MannaException> {
                meeting.confirmDate(userId = 2L, date = LocalDate.of(2025, 6, 15))
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_HOST)
        }

        @Test
        fun `CONFIRMED 상태에서 재확정 시 날짜 업데이트`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.CONFIRMED)
            val newDate = LocalDate.of(2025, 6, 20)

            meeting.confirmDate(userId = 1L, date = newDate)

            assertThat(meeting.status).isEqualTo(MeetingStatus.CONFIRMED)
            assertThat(meeting.confirmedDate).isEqualTo(newDate)
        }

        @Test
        fun `CANCELLED 상태 약속방 확정 시 MEETING_NOT_OPEN 예외`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.CANCELLED)

            val ex = assertThrows<MannaException> {
                meeting.confirmDate(userId = 1L, date = LocalDate.of(2025, 6, 15))
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_OPEN)
        }

        @Test
        fun `날짜 범위 시작일보다 이전 날짜 확정 시 DATE_OUT_OF_RANGE 예외`() {
            val meeting = meeting(hostId = 1L)

            val ex = assertThrows<MannaException> {
                meeting.confirmDate(userId = 1L, date = LocalDate.of(2025, 5, 31))
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.DATE_OUT_OF_RANGE)
        }

        @Test
        fun `날짜 범위 종료일보다 이후 날짜 확정 시 DATE_OUT_OF_RANGE 예외`() {
            val meeting = meeting(hostId = 1L)

            val ex = assertThrows<MannaException> {
                meeting.confirmDate(userId = 1L, date = LocalDate.of(2025, 7, 1))
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.DATE_OUT_OF_RANGE)
        }
    }

    @Nested
    inner class CancelConfirm {

        @Test
        fun `확정 취소 성공 시 status OPEN, confirmedDate null`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.CONFIRMED)

            meeting.cancelConfirm(userId = 1L)

            assertThat(meeting.status).isEqualTo(MeetingStatus.OPEN)
            assertThat(meeting.confirmedDate).isNull()
        }

        @Test
        fun `방장이 아닌 사용자 요청 시 NOT_MEETING_HOST 예외`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.CONFIRMED)

            val ex = assertThrows<MannaException> { meeting.cancelConfirm(userId = 2L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_HOST)
        }

        @Test
        fun `PLACE_VOTING 상태에서 확정 취소 성공 — status OPEN, confirmedDate null`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.PLACE_VOTING).also {
                it.confirmedDate = LocalDate.of(2025, 6, 15)
            }

            meeting.cancelConfirm(userId = 1L)

            assertThat(meeting.status).isEqualTo(MeetingStatus.OPEN)
            assertThat(meeting.confirmedDate).isNull()
        }

        @Test
        fun `SETTLING 상태에서 확정 취소 성공 — status OPEN, confirmedDate null`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.SETTLING).also {
                it.confirmedDate = LocalDate.of(2025, 6, 15)
            }

            meeting.cancelConfirm(userId = 1L)

            assertThat(meeting.status).isEqualTo(MeetingStatus.OPEN)
            assertThat(meeting.confirmedDate).isNull()
        }

        @Test
        fun `OPEN 상태에서 취소 시 MEETING_NOT_CONFIRMED 예외`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.OPEN)

            val ex = assertThrows<MannaException> { meeting.cancelConfirm(userId = 1L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_CONFIRMED)
        }
    }

    @Nested
    inner class RequireOpen {

        @Test
        fun `OPEN 상태에서 requireOpen 정상 통과`() {
            val meeting = meeting(status = MeetingStatus.OPEN)

            meeting.requireOpen() // 예외 없이 통과
        }

        @Test
        fun `CONFIRMED 상태에서 requireOpen 시 MEETING_NOT_OPEN 예외`() {
            val meeting = meeting(status = MeetingStatus.CONFIRMED)

            val ex = assertThrows<MannaException> { meeting.requireOpen() }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_OPEN)
        }

        @Test
        fun `CANCELLED 상태에서 requireOpen 시 MEETING_NOT_OPEN 예외`() {
            val meeting = meeting(status = MeetingStatus.CANCELLED)

            val ex = assertThrows<MannaException> { meeting.requireOpen() }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_OPEN)
        }
    }

    @Nested
    inner class MarkDone {

        @Test
        fun `SETTLING 상태에서 약속 종료 성공 — status DONE`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.SETTLING)

            meeting.markDone(userId = 1L)

            assertThat(meeting.status).isEqualTo(MeetingStatus.DONE)
        }

        @Test
        fun `방장이 아닌 사용자 요청 시 NOT_MEETING_HOST 예외`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.SETTLING)

            val ex = assertThrows<MannaException> { meeting.markDone(userId = 2L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_HOST)
        }

        @Test
        fun `SETTLING 아닌 상태에서 종료 시 MEETING_NOT_SETTLING 예외`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.PLACE_VOTING)

            val ex = assertThrows<MannaException> { meeting.markDone(userId = 1L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_SETTLING)
        }
    }

    @Nested
    inner class IsHost {

        @Test
        fun `hostId와 일치하면 true`() {
            val meeting = meeting(hostId = 1L)

            assertThat(meeting.isHost(1L)).isTrue()
        }

        @Test
        fun `hostId와 불일치하면 false`() {
            val meeting = meeting(hostId = 1L)

            assertThat(meeting.isHost(2L)).isFalse()
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `날짜 범위 변경 없으면 제목·설명만 업데이트, false 반환`() {
            val meeting = meeting(hostId = 1L)

            val changed = meeting.update(1L, "새 제목", "새 설명", start, end)

            assertThat(changed).isFalse()
            assertThat(meeting.title).isEqualTo("새 제목")
            assertThat(meeting.description).isEqualTo("새 설명")
            assertThat(meeting.status).isEqualTo(MeetingStatus.OPEN)
            assertThat(meeting.confirmedDate).isNull()
        }

        @Test
        fun `날짜 범위 변경 시 스케줄 초기화 플래그 true 반환, status OPEN, confirmedDate null`() {
            val meeting = meeting(hostId = 1L, status = MeetingStatus.CONFIRMED).also {
                it.confirmedDate = LocalDate.of(2025, 6, 15)
            }
            val newStart = LocalDate.of(2025, 7, 1)
            val newEnd = LocalDate.of(2025, 7, 31)

            val changed = meeting.update(1L, "새 제목", null, newStart, newEnd)

            assertThat(changed).isTrue()
            assertThat(meeting.dateRangeStart).isEqualTo(newStart)
            assertThat(meeting.dateRangeEnd).isEqualTo(newEnd)
            assertThat(meeting.status).isEqualTo(MeetingStatus.OPEN)
            assertThat(meeting.confirmedDate).isNull()
        }

        @Test
        fun `방장이 아닌 사용자 수정 시 NOT_MEETING_HOST 예외`() {
            val meeting = meeting(hostId = 1L)

            val ex = assertThrows<MannaException> {
                meeting.update(99L, "새 제목", null, start, end)
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_HOST)
        }
    }
}
