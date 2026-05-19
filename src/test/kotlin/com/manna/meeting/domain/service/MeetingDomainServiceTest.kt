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
import com.manna.meeting.domain.entity.MeetingStatus
import com.manna.meeting.domain.repository.MeetingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class MeetingDomainServiceTest {

    private val meetingRepository: MeetingRepository = mock()
    private lateinit var meetingDomainService: MeetingDomainService

    private val start = LocalDate.of(2025, 6, 1)
    private val end = LocalDate.of(2025, 6, 30)

    @BeforeEach
    fun setUp() {
        meetingDomainService = MeetingDomainService(meetingRepository)
    }

    private fun meeting(
        id: Long = 1L,
        hostId: Long = 1L,
        status: MeetingStatus = MeetingStatus.OPEN,
    ) = Meeting(
        id = id,
        hostId = hostId,
        title = "테스트 약속",
        dateRangeStart = start,
        dateRangeEnd = end,
        status = status,
    )

    @Nested
    inner class Create {

        @Test
        fun `약속방 생성 성공 시 방장이 참여자로 자동 등록`() {
            val command = CreateMeetingCommand(
                hostId = 1L, title = "6월 회식", description = null,
                dateRangeStart = start, dateRangeEnd = end,
            )
            val saved = meeting(hostId = command.hostId)

            whenever(meetingRepository.save(any())).thenReturn(saved)
            whenever(meetingRepository.saveParticipant(any()))
                .thenReturn(MeetingParticipant(meeting = saved, userId = command.hostId))

            meetingDomainService.create(command)

            verify(meetingRepository, times(1)).save(any())
            verify(meetingRepository, times(1)).saveParticipant(any())
        }
    }

    @Nested
    inner class Join {

        @Test
        fun `약속방 참여 성공`() {
            val command = JoinMeetingCommand(meetingId = 1L, userId = 2L)
            val found = meeting()

            whenever(meetingRepository.findById(command.meetingId)).thenReturn(found)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(command.meetingId, command.userId))
                .thenReturn(null)
            whenever(meetingRepository.saveParticipant(any()))
                .thenReturn(MeetingParticipant(meeting = found, userId = command.userId))

            val result = meetingDomainService.join(command)

            assertThat(result.userId).isEqualTo(command.userId)
        }

        @Test
        fun `이미 참여한 사용자 재참여 시 ALREADY_JOINED 예외`() {
            val command = JoinMeetingCommand(meetingId = 1L, userId = 2L)
            val found = meeting()
            val existing = MeetingParticipant(meeting = found, userId = command.userId)

            whenever(meetingRepository.findById(command.meetingId)).thenReturn(found)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(command.meetingId, command.userId))
                .thenReturn(existing)

            val ex = assertThrows<MannaException> { meetingDomainService.join(command) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.ALREADY_JOINED)
            verify(meetingRepository, never()).saveParticipant(any())
        }

        @Test
        fun `OPEN 아닌 약속방 참여 시 MEETING_NOT_OPEN 예외`() {
            val command = JoinMeetingCommand(meetingId = 1L, userId = 2L)

            whenever(meetingRepository.findById(command.meetingId))
                .thenReturn(meeting(status = MeetingStatus.CONFIRMED))

            val ex = assertThrows<MannaException> { meetingDomainService.join(command) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_OPEN)
        }

        @Test
        fun `존재하지 않는 약속방 참여 시 MEETING_NOT_FOUND 예외`() {
            val command = JoinMeetingCommand(meetingId = 999L, userId = 2L)

            whenever(meetingRepository.findById(command.meetingId)).thenReturn(null)

            val ex = assertThrows<MannaException> { meetingDomainService.join(command) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_FOUND)
        }
    }

    @Nested
    inner class UpdateAvailability {

        @Test
        fun `가용 날짜 등록 성공 시 기존 삭제 후 신규 저장`() {
            val dates = listOf(LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 15))
            val command = UpdateAvailabilityCommand(meetingId = 1L, userId = 2L, availableDates = dates)
            val found = meeting()

            whenever(meetingRepository.findById(command.meetingId)).thenReturn(found)
            whenever(meetingRepository.saveAvailability(any())).thenAnswer { it.arguments[0] as Availability }

            meetingDomainService.updateAvailability(command)

            verify(meetingRepository).deleteAvailabilitiesByMeetingIdAndUserId(command.meetingId, command.userId)
            verify(meetingRepository, times(dates.size)).saveAvailability(any())
        }

        @Test
        fun `날짜 범위를 벗어난 날짜 포함 시 DATE_OUT_OF_RANGE 예외`() {
            val command = UpdateAvailabilityCommand(
                meetingId = 1L, userId = 2L,
                availableDates = listOf(LocalDate.of(2025, 7, 5)),
            )

            whenever(meetingRepository.findById(command.meetingId)).thenReturn(meeting())
            whenever(meetingRepository.deleteAvailabilitiesByMeetingIdAndUserId(any(), any())).then {}

            val ex = assertThrows<MannaException> { meetingDomainService.updateAvailability(command) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.DATE_OUT_OF_RANGE)
            verify(meetingRepository, never()).saveAvailability(any())
        }
    }

    @Nested
    inner class ConfirmDate {

        @Test
        fun `날짜 확정 성공`() {
            val command = ConfirmDateCommand(
                meetingId = 1L, userId = 1L, confirmedDate = LocalDate.of(2025, 6, 15),
            )
            val found = meeting(hostId = 1L)

            whenever(meetingRepository.findById(command.meetingId)).thenReturn(found)
            whenever(meetingRepository.save(any())).thenReturn(found)

            val result = meetingDomainService.confirmDate(command)

            assertThat(result.status).isEqualTo(MeetingStatus.CONFIRMED)
            assertThat(result.confirmedDate).isEqualTo(command.confirmedDate)
        }

        @Test
        fun `방장이 아닌 사용자 확정 시 NOT_MEETING_HOST 예외`() {
            val command = ConfirmDateCommand(
                meetingId = 1L, userId = 2L, confirmedDate = LocalDate.of(2025, 6, 15),
            )

            whenever(meetingRepository.findById(command.meetingId)).thenReturn(meeting(hostId = 1L))

            val ex = assertThrows<MannaException> { meetingDomainService.confirmDate(command) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_HOST)
            verify(meetingRepository, never()).save(any())
        }
    }

    @Nested
    inner class GetAvailabilityHeatmap {

        @Test
        fun `날짜별 참여 가능 인원 수 집계`() {
            val found = meeting()
            val availabilities = listOf(
                Availability(meeting = found, userId = 1L, availableDate = LocalDate.of(2025, 6, 10)),
                Availability(meeting = found, userId = 2L, availableDate = LocalDate.of(2025, 6, 10)),
                Availability(meeting = found, userId = 3L, availableDate = LocalDate.of(2025, 6, 15)),
            )

            whenever(meetingRepository.findAvailabilitiesByMeetingId(1L)).thenReturn(availabilities)

            val result = meetingDomainService.getAvailabilityHeatmap(1L)

            assertThat(result["2025-06-10"]).isEqualTo(2)
            assertThat(result["2025-06-15"]).isEqualTo(1)
        }

        @Test
        fun `가용 날짜가 없으면 빈 맵 반환`() {
            whenever(meetingRepository.findAvailabilitiesByMeetingId(1L)).thenReturn(emptyList())

            val result = meetingDomainService.getAvailabilityHeatmap(1L)

            assertThat(result).isEmpty()
        }
    }
}
