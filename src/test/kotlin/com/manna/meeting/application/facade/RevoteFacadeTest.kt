package com.manna.meeting.application.facade

import com.manna.meeting.application.command.ConfirmRevoteCommand
import com.manna.meeting.application.command.CreateRevoteCommand
import com.manna.meeting.application.command.VoteRevoteCommand
import com.manna.meeting.domain.entity.Revote
import com.manna.meeting.domain.entity.RevoteCandidate
import com.manna.meeting.domain.entity.RevoteStatus
import com.manna.meeting.domain.entity.RevoteVote
import com.manna.meeting.domain.service.RevoteDomainService
import com.manna.meeting.domain.service.RevoteStatusData
import com.manna.user.domain.entity.User
import com.manna.user.domain.service.UserDomainService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class RevoteFacadeTest {

    private val revoteDomainService: RevoteDomainService = mock()
    private val userDomainService: UserDomainService = mock()
    private lateinit var revoteFacade: RevoteFacade

    private val d1 = LocalDate.of(2025, 6, 10)
    private val d2 = LocalDate.of(2025, 6, 15)

    @BeforeEach
    fun setUp() {
        revoteFacade = RevoteFacade(revoteDomainService, userDomainService)
    }

    private fun openRevote(id: Long = 1L, meetingId: Long = 1L) =
        Revote(id = id, meetingId = meetingId, status = RevoteStatus.OPEN)

    private fun statusData(
        revote: Revote = openRevote(),
        candidates: List<RevoteCandidate> = listOf(
            RevoteCandidate(revoteId = 1L, candidateDate = d1),
            RevoteCandidate(revoteId = 1L, candidateDate = d2),
        ),
        votes: List<RevoteVote> = emptyList(),
        myVotedDate: LocalDate? = null,
        totalCount: Int = 2,
    ) = RevoteStatusData(
        revote = revote,
        candidates = candidates,
        votes = votes,
        myVotedDate = myVotedDate,
        totalCount = totalCount,
    )

    private fun user(id: Long, nickname: String = "유저$id") =
        User(id = id, email = "$id@test.com", password = "pw", nickname = nickname)

    @Nested
    inner class CreateRevote {

        @Test
        fun `재투표 생성 후 현황 정보 반환`() {
            val command = CreateRevoteCommand(meetingId = 1L, userId = 1L, candidateDates = listOf(d1, d2))
            val data = statusData()

            whenever(revoteDomainService.getRevoteStatusData(1L, 1L)).thenReturn(data)

            val result = revoteFacade.createRevote(command)

            verify(revoteDomainService).createRevote(command)
            assertThat(result.status).isEqualTo(RevoteStatus.OPEN)
            assertThat(result.candidates).hasSize(2)
            assertThat(result.totalCount).isEqualTo(2)
        }

        @Test
        fun `투표자 있는 경우 유저 정보 enrichment 포함`() {
            val command = CreateRevoteCommand(meetingId = 1L, userId = 1L, candidateDates = listOf(d1))
            val votes = listOf(RevoteVote(revoteId = 1L, userId = 2L, votedDate = d1))
            val data = statusData(
                candidates = listOf(RevoteCandidate(revoteId = 1L, candidateDate = d1)),
                votes = votes,
                myVotedDate = null,
            )

            whenever(revoteDomainService.getRevoteStatusData(1L, 1L)).thenReturn(data)
            whenever(userDomainService.getUsersByIds(listOf(2L))).thenReturn(listOf(user(2L, "지원")))

            val result = revoteFacade.createRevote(command)

            val candidate = result.candidates.first { it.date == d1 }
            assertThat(candidate.count).isEqualTo(1)
            assertThat(candidate.voters.first().nickname).isEqualTo("지원")
        }
    }

    @Nested
    inner class Vote {

        @Test
        fun `투표 위임 — revoteDomainService vote 호출`() {
            val command = VoteRevoteCommand(meetingId = 1L, userId = 2L, votedDate = d1)

            revoteFacade.vote(command)

            verify(revoteDomainService).vote(command)
        }
    }

    @Nested
    inner class GetRevoteStatus {

        @Test
        fun `재투표 현황 조회 — 후보별 투표자 enrichment`() {
            val votes = listOf(
                RevoteVote(revoteId = 1L, userId = 1L, votedDate = d1),
                RevoteVote(revoteId = 1L, userId = 2L, votedDate = d2),
            )
            val data = statusData(
                votes = votes,
                myVotedDate = d1,
                totalCount = 3,
            )

            whenever(revoteDomainService.getRevoteStatusData(1L, 1L)).thenReturn(data)
            whenever(userDomainService.getUsersByIds(listOf(1L, 2L)))
                .thenReturn(listOf(user(1L, "민지"), user(2L, "지원")))

            val result = revoteFacade.getRevoteStatus(meetingId = 1L, userId = 1L)

            assertThat(result.votedCount).isEqualTo(2)
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.myVotedDate).isEqualTo(d1)

            val d1Candidate = result.candidates.first { it.date == d1 }
            assertThat(d1Candidate.voters.map { it.nickname }).containsExactly("민지")

            val d2Candidate = result.candidates.first { it.date == d2 }
            assertThat(d2Candidate.voters.map { it.nickname }).containsExactly("지원")
        }

        @Test
        fun `투표자 없을 때 유저 조회 스킵 — 빈 voters 반환`() {
            val data = statusData(votes = emptyList())

            whenever(revoteDomainService.getRevoteStatusData(1L, 1L)).thenReturn(data)

            val result = revoteFacade.getRevoteStatus(meetingId = 1L, userId = 1L)

            assertThat(result.candidates.flatMap { it.voters }).isEmpty()
        }
    }

    @Nested
    inner class ConfirmRevote {

        @Test
        fun `재투표 확정 위임 — revoteDomainService confirmRevote 호출`() {
            val command = ConfirmRevoteCommand(meetingId = 1L, userId = 1L, confirmedDate = d1)

            revoteFacade.confirmRevote(command)

            verify(revoteDomainService).confirmRevote(command)
        }
    }

    @Nested
    inner class CancelRevote {

        @Test
        fun `재투표 취소 위임 — revoteDomainService cancelRevote 호출`() {
            revoteFacade.cancelRevote(meetingId = 1L, userId = 1L)

            verify(revoteDomainService).cancelRevote(1L, 1L)
        }
    }
}
