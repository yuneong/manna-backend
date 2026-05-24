package com.manna.place.domain.service

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
import com.manna.meeting.domain.entity.MeetingStatus
import com.manna.meeting.domain.repository.MeetingRepository
import com.manna.place.application.command.CreatePlaceCommand
import com.manna.place.domain.entity.Place
import com.manna.place.domain.entity.PlaceVote
import com.manna.place.domain.repository.PlaceRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class PlaceServiceTest {

    private val placeRepository: PlaceRepository = mock()
    private val meetingRepository: MeetingRepository = mock()
    private lateinit var placeService: PlaceService

    private val start = LocalDate.of(2026, 6, 1)
    private val end = LocalDate.of(2026, 6, 30)

    @BeforeEach
    fun setUp() {
        placeService = PlaceService(placeRepository, meetingRepository)
    }

    private fun meeting(id: Long = 1L, status: MeetingStatus = MeetingStatus.CONFIRMED) = Meeting(
        id = id,
        hostId = 1L,
        title = "테스트",
        dateRangeStart = start,
        dateRangeEnd = end,
        status = status,
    )

    private fun place(id: Long = 1L, meetingId: Long = 1L) = Place(
        id = id,
        meetingId = meetingId,
        suggestedBy = 1L,
        name = "테스트 장소",
    )

    private fun participant(meetingId: Long = 1L, userId: Long = 2L) =
        MeetingParticipant(meeting = meeting(meetingId), userId = userId)

    @Nested
    inner class Propose {

        private fun command(userId: Long = 2L) = CreatePlaceCommand(
            meetingId = 1L, userId = userId, name = "와인바", url = null, memo = null,
        )

        @Test
        fun `CONFIRMED 상태에서 첫 제안 성공 — status PLACE_VOTING으로 전환`() {
            val m = meeting(status = MeetingStatus.CONFIRMED)
            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L)).thenReturn(participant())
            whenever(placeRepository.save(any())).thenAnswer { it.arguments[0] as Place }
            whenever(meetingRepository.save(m)).thenReturn(m)

            placeService.propose(command())

            assertThat(m.status).isEqualTo(MeetingStatus.PLACE_VOTING)
            verify(meetingRepository).save(m)
        }

        @Test
        fun `PLACE_VOTING 상태에서 추가 제안 성공 — status 변경 없음`() {
            val m = meeting(status = MeetingStatus.PLACE_VOTING)
            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L)).thenReturn(participant())
            whenever(placeRepository.save(any())).thenAnswer { it.arguments[0] as Place }

            placeService.propose(command())

            assertThat(m.status).isEqualTo(MeetingStatus.PLACE_VOTING)
            verify(meetingRepository, never()).save(any())
        }

        @Test
        fun `OPEN 상태 약속방에서 제안 시 MEETING_NOT_CONFIRMED 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting(status = MeetingStatus.OPEN))

            val ex = assertThrows<MannaException> { placeService.propose(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_CONFIRMED)
            verify(placeRepository, never()).save(any())
        }

        @Test
        fun `참여자가 아닌 사용자 제안 시 NOT_MEETING_PARTICIPANT 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting())
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L)).thenReturn(null)

            val ex = assertThrows<MannaException> { placeService.propose(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_PARTICIPANT)
            verify(placeRepository, never()).save(any())
        }

        @Test
        fun `존재하지 않는 약속방에서 제안 시 MEETING_NOT_FOUND 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(null)

            val ex = assertThrows<MannaException> { placeService.propose(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_FOUND)
        }
    }

    @Nested
    inner class ToggleVote {

        @Test
        fun `투표 없으면 추가`() {
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L)).thenReturn(participant())
            whenever(placeRepository.findById(1L)).thenReturn(place())
            whenever(placeRepository.findVoteByPlaceIdAndUserId(1L, 2L)).thenReturn(null)
            whenever(placeRepository.saveVote(any())).thenAnswer { it.arguments[0] as PlaceVote }

            placeService.toggleVote(meetingId = 1L, placeId = 1L, userId = 2L)

            verify(placeRepository).saveVote(any())
            verify(placeRepository, never()).deleteVote(any())
        }

        @Test
        fun `투표 있으면 취소`() {
            val vote = PlaceVote(id = 1L, placeId = 1L, userId = 2L)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L)).thenReturn(participant())
            whenever(placeRepository.findById(1L)).thenReturn(place())
            whenever(placeRepository.findVoteByPlaceIdAndUserId(1L, 2L)).thenReturn(vote)

            placeService.toggleVote(meetingId = 1L, placeId = 1L, userId = 2L)

            verify(placeRepository).deleteVote(vote)
            verify(placeRepository, never()).saveVote(any())
        }

        @Test
        fun `참여자가 아닌 사용자 투표 시 NOT_MEETING_PARTICIPANT 예외`() {
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 99L)).thenReturn(null)

            val ex = assertThrows<MannaException> {
                placeService.toggleVote(meetingId = 1L, placeId = 1L, userId = 99L)
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_PARTICIPANT)
        }

        @Test
        fun `존재하지 않는 장소 투표 시 PLACE_NOT_FOUND 예외`() {
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L)).thenReturn(participant())
            whenever(placeRepository.findById(99L)).thenReturn(null)

            val ex = assertThrows<MannaException> {
                placeService.toggleVote(meetingId = 1L, placeId = 99L, userId = 2L)
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.PLACE_NOT_FOUND)
        }

        @Test
        fun `다른 약속방 장소 투표 시 PLACE_NOT_FOUND 예외`() {
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L)).thenReturn(participant())
            whenever(placeRepository.findById(1L)).thenReturn(place(meetingId = 999L))

            val ex = assertThrows<MannaException> {
                placeService.toggleVote(meetingId = 1L, placeId = 1L, userId = 2L)
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.PLACE_NOT_FOUND)
        }
    }

    @Nested
    inner class DeleteAllByMeetingId {

        @Test
        fun `장소마다 투표 삭제 후 장소 전체 삭제`() {
            val p1 = place(id = 1L)
            val p2 = place(id = 2L)
            whenever(placeRepository.findByMeetingId(1L)).thenReturn(listOf(p1, p2))

            placeService.deleteAllByMeetingId(1L)

            verify(placeRepository).deleteVotesByPlaceId(1L)
            verify(placeRepository).deleteVotesByPlaceId(2L)
            verify(placeRepository).deleteByMeetingId(1L)
        }

        @Test
        fun `장소 없으면 deleteByMeetingId만 호출`() {
            whenever(placeRepository.findByMeetingId(1L)).thenReturn(emptyList())

            placeService.deleteAllByMeetingId(1L)

            verify(placeRepository, never()).deleteVotesByPlaceId(any())
            verify(placeRepository).deleteByMeetingId(1L)
        }
    }
}
