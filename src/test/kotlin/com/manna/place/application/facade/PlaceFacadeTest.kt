package com.manna.place.application.facade

import com.manna.meeting.domain.service.MeetingDomainService
import com.manna.place.application.command.CreatePlaceCommand
import com.manna.place.domain.entity.Place
import com.manna.place.domain.entity.PlaceVote
import com.manna.place.domain.service.PlaceService
import com.manna.user.domain.entity.User
import com.manna.user.domain.service.UserDomainService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

class PlaceFacadeTest {

    private val placeService: PlaceService = mock()
    private val meetingDomainService: MeetingDomainService = mock()
    private val userDomainService: UserDomainService = mock()
    private lateinit var placeFacade: PlaceFacade

    @BeforeEach
    fun setUp() {
        placeFacade = PlaceFacade(placeService, meetingDomainService, userDomainService)
    }

    private fun user(id: Long, nickname: String = "user$id") = User(
        id = id, email = "user$id@test.com", password = "pw", nickname = nickname,
    )

    private fun place(id: Long = 1L, suggestedBy: Long = 1L) = Place(
        id = id, meetingId = 1L, suggestedBy = suggestedBy, name = "장소$id",
    )

    @Nested
    inner class GetPlaces {

        @Test
        fun `득표 수 내림차순으로 정렬된다`() {
            val p1 = place(id = 1L, suggestedBy = 1L)
            val p2 = place(id = 2L, suggestedBy = 1L)
            val votes = listOf(
                PlaceVote(placeId = 2L, userId = 1L),
                PlaceVote(placeId = 2L, userId = 2L),
                PlaceVote(placeId = 1L, userId = 3L),
            )

            whenever(placeService.getPlaces(1L)).thenReturn(listOf(p1, p2))
            whenever(placeService.getVotesByPlaceIds(listOf(1L, 2L))).thenReturn(votes)
            whenever(userDomainService.getUsersByIds(listOf(1L, 2L, 3L)))
                .thenReturn(listOf(user(1L, "민지"), user(2L, "지원"), user(3L, "현우")))
            whenever(meetingDomainService.getParticipantCount(1L)).thenReturn(3)

            val result = placeFacade.getPlaces(meetingId = 1L, userId = 3L)

            assertThat(result.places[0].id).isEqualTo(2L)
            assertThat(result.places[0].voteCount).isEqualTo(2)
            assertThat(result.places[1].id).isEqualTo(1L)
            assertThat(result.places[1].voteCount).isEqualTo(1)
            assertThat(result.totalParticipants).isEqualTo(3)
        }

        @Test
        fun `myVoted는 요청 userId 기준으로 판별된다`() {
            val p = place(id = 1L, suggestedBy = 1L)
            val votes = listOf(PlaceVote(placeId = 1L, userId = 2L))

            whenever(placeService.getPlaces(1L)).thenReturn(listOf(p))
            whenever(placeService.getVotesByPlaceIds(listOf(1L))).thenReturn(votes)
            whenever(userDomainService.getUsersByIds(listOf(2L, 1L)))
                .thenReturn(listOf(user(1L), user(2L)))
            whenever(meetingDomainService.getParticipantCount(1L)).thenReturn(2)

            val asVoter = placeFacade.getPlaces(1L, userId = 2L)
            assertThat(asVoter.places[0].myVoted).isTrue()
        }

        @Test
        fun `장소 없으면 빈 목록 반환`() {
            whenever(placeService.getPlaces(1L)).thenReturn(emptyList())
            whenever(placeService.getVotesByPlaceIds(emptyList())).thenReturn(emptyList())
            whenever(userDomainService.getUsersByIds(emptyList())).thenReturn(emptyList())
            whenever(meetingDomainService.getParticipantCount(1L)).thenReturn(5)

            val result = placeFacade.getPlaces(1L, userId = 1L)

            assertThat(result.places).isEmpty()
            assertThat(result.totalParticipants).isEqualTo(5)
        }
    }

    @Nested
    inner class Propose {

        @Test
        fun `장소 제안 성공 시 myVoted false, voteCount 0`() {
            val command = CreatePlaceCommand(meetingId = 1L, userId = 2L, name = "와인바", url = null, memo = null)
            val saved = place(id = 1L, suggestedBy = 2L)

            whenever(placeService.propose(command)).thenReturn(saved)
            whenever(userDomainService.getUsersByIds(listOf(2L))).thenReturn(listOf(user(2L, "지원")))

            val result = placeFacade.propose(command)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.voteCount).isEqualTo(0)
            assertThat(result.myVoted).isFalse()
            assertThat(result.proposer.nickname).isEqualTo("지원")
        }
    }
}
