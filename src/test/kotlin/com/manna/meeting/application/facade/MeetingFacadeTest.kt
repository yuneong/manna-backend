package com.manna.meeting.application.facade

import com.manna.meeting.application.command.CreateMeetingCommand
import com.manna.meeting.domain.entity.Availability
import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
import com.manna.meeting.domain.entity.MeetingStatus
import com.manna.meeting.domain.service.MeetingDomainService
import com.manna.user.domain.entity.User
import com.manna.user.domain.service.UserDomainService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

class MeetingFacadeTest {

    private val meetingDomainService: MeetingDomainService = mock()
    private val userDomainService: UserDomainService = mock()
    private lateinit var meetingFacade: MeetingFacade

    private val start = LocalDate.of(2025, 6, 1)
    private val end = LocalDate.of(2025, 6, 30)

    @BeforeEach
    fun setUp() {
        meetingFacade = MeetingFacade(meetingDomainService, userDomainService)
    }

    private fun meeting(id: Long = 1L, hostId: Long = 1L) = Meeting(
        id = id,
        hostId = hostId,
        title = "테스트 약속",
        dateRangeStart = start,
        dateRangeEnd = end,
        status = MeetingStatus.OPEN,
    )

    private fun user(id: Long, nickname: String = "user$id") = User(
        id = id,
        email = "user$id@test.com",
        password = "pw",
        nickname = nickname,
    )

    @Nested
    inner class GetMeeting {

        @Test
        fun `participantCount와 responseCount가 올바르게 계산된다`() {
            val meeting = meeting(id = 1L, hostId = 1L)
            val participants = listOf(
                MeetingParticipant(meeting = meeting, userId = 1L),
                MeetingParticipant(meeting = meeting, userId = 2L),
                MeetingParticipant(meeting = meeting, userId = 3L),
            )
            // 3명 참여 중 2명만 availability 등록
            val availabilities = listOf(
                Availability(meeting = meeting, userId = 1L, availableDate = LocalDate.of(2025, 6, 10)),
                Availability(meeting = meeting, userId = 1L, availableDate = LocalDate.of(2025, 6, 11)),
                Availability(meeting = meeting, userId = 2L, availableDate = LocalDate.of(2025, 6, 10)),
            )

            whenever(meetingDomainService.getById(1L)).thenReturn(meeting)
            whenever(meetingDomainService.getParticipantsByMeetingIds(listOf(1L))).thenReturn(participants)
            whenever(meetingDomainService.getAvailabilitiesByMeetingIds(listOf(1L))).thenReturn(availabilities)
            whenever(userDomainService.getUsersByIds(listOf(1L, 2L, 3L)))
                .thenReturn(listOf(user(1L), user(2L), user(3L)))

            val result = meetingFacade.getMeeting(meetingId = 1L, userId = 1L)

            assertThat(result.participantCount).isEqualTo(3)
            assertThat(result.responseCount).isEqualTo(2) // userId 1, 2 — 중복 날짜 있어도 distinct
            assertThat(result.isParticipant).isTrue()
        }

        @Test
        fun `availability 없으면 responseCount는 0`() {
            val meeting = meeting(id = 1L)
            val participants = listOf(MeetingParticipant(meeting = meeting, userId = 1L))

            whenever(meetingDomainService.getById(1L)).thenReturn(meeting)
            whenever(meetingDomainService.getParticipantsByMeetingIds(listOf(1L))).thenReturn(participants)
            whenever(meetingDomainService.getAvailabilitiesByMeetingIds(listOf(1L))).thenReturn(emptyList())
            whenever(userDomainService.getUsersByIds(listOf(1L))).thenReturn(listOf(user(1L)))

            val result = meetingFacade.getMeeting(meetingId = 1L, userId = 1L)

            assertThat(result.responseCount).isEqualTo(0)
        }
    }

    @Nested
    inner class GetMyMeetings {

        @Test
        fun `여러 약속방의 participantCount와 responseCount가 각각 올바르게 계산된다`() {
            val m1 = meeting(id = 1L)
            val m2 = meeting(id = 2L)
            val participants = listOf(
                MeetingParticipant(meeting = m1, userId = 1L),
                MeetingParticipant(meeting = m1, userId = 2L),
                MeetingParticipant(meeting = m2, userId = 1L),
            )
            val availabilities = listOf(
                Availability(meeting = m1, userId = 1L, availableDate = LocalDate.of(2025, 6, 10)),
                Availability(meeting = m2, userId = 1L, availableDate = LocalDate.of(2025, 6, 11)),
                Availability(meeting = m2, userId = 1L, availableDate = LocalDate.of(2025, 6, 12)),
            )

            whenever(meetingDomainService.getMyMeetings(1L)).thenReturn(listOf(m1, m2))
            whenever(meetingDomainService.getParticipantsByMeetingIds(listOf(1L, 2L))).thenReturn(participants)
            whenever(meetingDomainService.getAvailabilitiesByMeetingIds(listOf(1L, 2L))).thenReturn(availabilities)
            whenever(userDomainService.getUsersByIds(listOf(1L, 2L)))
                .thenReturn(listOf(user(1L), user(2L)))

            val result = meetingFacade.getMyMeetings(userId = 1L)

            val r1 = result.first { it.id == 1L }
            val r2 = result.first { it.id == 2L }

            assertThat(r1.participantCount).isEqualTo(2)
            assertThat(r1.responseCount).isEqualTo(1) // userId 1만 등록

            assertThat(r2.participantCount).isEqualTo(1)
            assertThat(r2.responseCount).isEqualTo(1) // userId 1이 2일 등록했지만 distinct
        }
    }

    @Nested
    inner class CreateMeeting {

        @Test
        fun `약속방 생성 시 responseCount는 0`() {
            val command = CreateMeetingCommand(
                hostId = 1L, title = "테스트", description = null,
                dateRangeStart = start, dateRangeEnd = end,
            )
            val meeting = meeting(id = 1L, hostId = 1L)
            val participant = MeetingParticipant(meeting = meeting, userId = 1L)

            whenever(meetingDomainService.create(command)).thenReturn(meeting)
            whenever(meetingDomainService.getParticipantsByMeetingIds(listOf(1L))).thenReturn(listOf(participant))
            whenever(meetingDomainService.getAvailabilitiesByMeetingIds(listOf(1L))).thenReturn(emptyList())
            whenever(userDomainService.getUsersByIds(listOf(1L))).thenReturn(listOf(user(1L)))

            val result = meetingFacade.createMeeting(command)

            assertThat(result.responseCount).isEqualTo(0)
        }
    }

    @Nested
    inner class GetHeatmap {

        @Test
        fun `날짜별 availableParticipantIds와 count가 올바르게 반환된다`() {
            val heatmap = mapOf(
                "2025-06-10" to listOf(1L, 2L, 3L),
                "2025-06-15" to listOf(2L),
            )

            whenever(meetingDomainService.getAvailabilityHeatmap(1L)).thenReturn(heatmap)

            val result = meetingFacade.getHeatmap(1L)

            assertThat(result.heatmap["2025-06-10"]).containsExactlyInAnyOrder(1L, 2L, 3L)
            assertThat(result.heatmap["2025-06-15"]).containsExactly(2L)
        }

        @Test
        fun `availability 없으면 빈 heatmap 반환`() {
            whenever(meetingDomainService.getAvailabilityHeatmap(1L)).thenReturn(emptyMap())

            val result = meetingFacade.getHeatmap(1L)

            assertThat(result.heatmap).isEmpty()
        }
    }
}
