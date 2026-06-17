package com.manna.meeting.domain.service

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import com.manna.meeting.application.command.ConfirmRevoteCommand
import com.manna.meeting.application.command.CreateRevoteCommand
import com.manna.meeting.application.command.VoteRevoteCommand
import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
import com.manna.meeting.domain.entity.MeetingSchedule
import com.manna.meeting.domain.entity.MeetingStatus
import com.manna.meeting.domain.entity.Revote
import com.manna.meeting.domain.entity.RevoteCandidate
import com.manna.meeting.domain.entity.RevoteVote
import com.manna.meeting.domain.repository.MeetingRepository
import com.manna.meeting.domain.repository.RevoteRepository
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

class RevoteDomainServiceTest {

    private val revoteRepository: RevoteRepository = mock()
    private val meetingRepository: MeetingRepository = mock()
    private lateinit var revoteDomainService: RevoteDomainService

    private val d1 = LocalDate.of(2025, 6, 10)
    private val d2 = LocalDate.of(2025, 6, 15)

    @BeforeEach
    fun setUp() {
        revoteDomainService = RevoteDomainService(revoteRepository, meetingRepository)
    }

    private fun meeting(id: Long = 1L, hostId: Long = 1L, status: MeetingStatus = MeetingStatus.OPEN) = Meeting(
        id = id, hostId = hostId, title = "테스트 약속",
        dateRangeStart = LocalDate.of(2025, 6, 1),
        dateRangeEnd = LocalDate.of(2025, 6, 30),
        status = status,
    )

    private fun participant(meeting: Meeting, userId: Long) =
        MeetingParticipant(meeting = meeting, userId = userId)

    private fun schedule(meeting: Meeting, userId: Long, date: LocalDate) =
        MeetingSchedule(meeting = meeting, userId = userId, scheduledDate = date)

    private fun revote(id: Long = 1L, meetingId: Long = 1L) = Revote(id = id, meetingId = meetingId)

    private fun candidate(revoteId: Long = 1L, date: LocalDate) =
        RevoteCandidate(id = 0L, revoteId = revoteId, candidateDate = date)

    @Nested
    inner class CreateRevote {

        private fun command(candidateDates: List<LocalDate> = listOf(d1, d2)) =
            CreateRevoteCommand(meetingId = 1L, userId = 1L, candidateDates = candidateDates)

        @Test
        fun `재투표 생성 성공 — 후보 날짜별 candidate 저장`() {
            val m = meeting(hostId = 1L)
            val schedules = listOf(schedule(m, 1L, d1), schedule(m, 2L, d2))

            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(null)
            whenever(meetingRepository.findSchedulesByMeetingId(1L)).thenReturn(schedules)
            whenever(revoteRepository.save(any())).thenReturn(revote())
            whenever(revoteRepository.saveCandidate(any())).thenAnswer { it.arguments[0] as RevoteCandidate }

            revoteDomainService.createRevote(command())

            verify(revoteRepository).save(any())
            verify(revoteRepository, org.mockito.kotlin.times(2)).saveCandidate(any())
        }

        @Test
        fun `방장이 아닌 사용자 요청 시 NOT_MEETING_HOST 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting(hostId = 1L))

            val ex = assertThrows<MannaException> {
                revoteDomainService.createRevote(command().copy(userId = 2L))
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_HOST)
        }

        @Test
        fun `CANCELLED 상태 약속방 시 MEETING_NOT_OPEN 예외`() {
            whenever(meetingRepository.findById(1L))
                .thenReturn(meeting(hostId = 1L, status = MeetingStatus.CANCELLED))

            val ex = assertThrows<MannaException> { revoteDomainService.createRevote(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_OPEN)
        }

        @Test
        fun `이미 진행 중인 재투표 있으면 REVOTE_ALREADY_EXISTS 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting(hostId = 1L))
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(revote())

            val ex = assertThrows<MannaException> { revoteDomainService.createRevote(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.REVOTE_ALREADY_EXISTS)
        }

        @Test
        fun `후보 날짜 2개 미만이면 REVOTE_INVALID_CANDIDATE_DATE 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting(hostId = 1L))
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(null)

            val ex = assertThrows<MannaException> {
                revoteDomainService.createRevote(command(candidateDates = listOf(d1)))
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.REVOTE_INVALID_CANDIDATE_DATE)
        }

        @Test
        fun `히트맵에 없는 날짜 포함 시 REVOTE_INVALID_CANDIDATE_DATE 예외`() {
            val m = meeting(hostId = 1L)
            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(null)
            whenever(meetingRepository.findSchedulesByMeetingId(1L))
                .thenReturn(listOf(schedule(m, 1L, d1)))

            val ex = assertThrows<MannaException> {
                revoteDomainService.createRevote(command(candidateDates = listOf(d1, d2)))
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.REVOTE_INVALID_CANDIDATE_DATE)
        }

        @Test
        fun `존재하지 않는 약속방 — MEETING_NOT_FOUND 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(null)

            val ex = assertThrows<MannaException> { revoteDomainService.createRevote(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_FOUND)
        }
    }

    @Nested
    inner class Vote {

        private fun command(votedDate: LocalDate = d1) =
            VoteRevoteCommand(meetingId = 1L, userId = 2L, votedDate = votedDate)

        @Test
        fun `투표 성공 — 기존 투표 없으면 신규 저장`() {
            val m = meeting()
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L))
                .thenReturn(participant(m, 2L))
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(revote())
            whenever(revoteRepository.existsCandidateByRevoteIdAndDate(1L, d1)).thenReturn(true)
            whenever(revoteRepository.findVoteByRevoteIdAndUserId(1L, 2L)).thenReturn(null)
            whenever(revoteRepository.saveVote(any())).thenAnswer { it.arguments[0] as RevoteVote }

            revoteDomainService.vote(command())

            verify(revoteRepository).saveVote(any())
        }

        @Test
        fun `이미 투표한 경우 — 기존 투표 날짜 업데이트`() {
            val m = meeting()
            val existingVote = RevoteVote(id = 1L, revoteId = 1L, userId = 2L, votedDate = d1)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L))
                .thenReturn(participant(m, 2L))
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(revote())
            whenever(revoteRepository.existsCandidateByRevoteIdAndDate(1L, d2)).thenReturn(true)
            whenever(revoteRepository.findVoteByRevoteIdAndUserId(1L, 2L)).thenReturn(existingVote)
            whenever(revoteRepository.saveVote(any())).thenAnswer { it.arguments[0] as RevoteVote }

            revoteDomainService.vote(command(votedDate = d2))

            assertThat(existingVote.votedDate).isEqualTo(d2)
            verify(revoteRepository).saveVote(existingVote)
        }

        @Test
        fun `참여자 아님 — NOT_MEETING_PARTICIPANT 예외`() {
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L)).thenReturn(null)

            val ex = assertThrows<MannaException> { revoteDomainService.vote(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_PARTICIPANT)
        }

        @Test
        fun `진행 중인 재투표 없음 — REVOTE_NOT_FOUND 예외`() {
            val m = meeting()
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L))
                .thenReturn(participant(m, 2L))
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(null)

            val ex = assertThrows<MannaException> { revoteDomainService.vote(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.REVOTE_NOT_FOUND)
        }

        @Test
        fun `후보 날짜 아님 — REVOTE_INVALID_CANDIDATE_DATE 예외`() {
            val m = meeting()
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L))
                .thenReturn(participant(m, 2L))
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(revote())
            whenever(revoteRepository.existsCandidateByRevoteIdAndDate(1L, d1)).thenReturn(false)

            val ex = assertThrows<MannaException> { revoteDomainService.vote(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.REVOTE_INVALID_CANDIDATE_DATE)
        }
    }

    @Nested
    inner class ConfirmRevote {

        private fun command(confirmedDate: LocalDate = d1) =
            ConfirmRevoteCommand(meetingId = 1L, userId = 1L, confirmedDate = confirmedDate)

        @Test
        fun `방장 확정 성공 — revote CLOSED, meeting CONFIRMED`() {
            val m = meeting(hostId = 1L)
            val rv = revote()
            val participants = listOf(participant(m, 1L), participant(m, 2L))

            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(rv)
            whenever(meetingRepository.findParticipantsByMeetingId(1L)).thenReturn(participants)
            whenever(revoteRepository.countVotesByRevoteId(1L)).thenReturn(2)
            whenever(revoteRepository.existsCandidateByRevoteIdAndDate(1L, d1)).thenReturn(true)
            whenever(revoteRepository.save(any())).thenReturn(rv)
            whenever(meetingRepository.save(any())).thenReturn(m)

            revoteDomainService.confirmRevote(command())

            assertThat(m.status).isEqualTo(MeetingStatus.CONFIRMED)
            assertThat(m.confirmedDate).isEqualTo(d1)
        }

        @Test
        fun `방장이 아닌 사용자 요청 — NOT_MEETING_HOST 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting(hostId = 1L))

            val ex = assertThrows<MannaException> {
                revoteDomainService.confirmRevote(command().copy(userId = 2L))
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_HOST)
        }

        @Test
        fun `진행 중인 재투표 없음 — REVOTE_NOT_FOUND 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting(hostId = 1L))
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(null)

            val ex = assertThrows<MannaException> { revoteDomainService.confirmRevote(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.REVOTE_NOT_FOUND)
        }

        @Test
        fun `전원 투표 미완료 — REVOTE_NOT_COMPLETED 예외`() {
            val m = meeting(hostId = 1L)
            val participants = listOf(participant(m, 1L), participant(m, 2L), participant(m, 3L))

            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(revote())
            whenever(meetingRepository.findParticipantsByMeetingId(1L)).thenReturn(participants)
            whenever(revoteRepository.countVotesByRevoteId(1L)).thenReturn(2)

            val ex = assertThrows<MannaException> { revoteDomainService.confirmRevote(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.REVOTE_NOT_COMPLETED)
        }

        @Test
        fun `후보 날짜 아닌 날짜 확정 — REVOTE_INVALID_CANDIDATE_DATE 예외`() {
            val m = meeting(hostId = 1L)
            val participants = listOf(participant(m, 1L), participant(m, 2L))

            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(revote())
            whenever(meetingRepository.findParticipantsByMeetingId(1L)).thenReturn(participants)
            whenever(revoteRepository.countVotesByRevoteId(1L)).thenReturn(2)
            whenever(revoteRepository.existsCandidateByRevoteIdAndDate(1L, d1)).thenReturn(false)

            val ex = assertThrows<MannaException> { revoteDomainService.confirmRevote(command()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.REVOTE_INVALID_CANDIDATE_DATE)
        }
    }

    @Nested
    inner class CancelRevote {

        @Test
        fun `재투표 취소 성공 — votes, candidates, revotes 순서로 삭제`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting(hostId = 1L))
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(revote(id = 1L))

            revoteDomainService.cancelRevote(1L, 1L)

            val inOrder = org.mockito.Mockito.inOrder(revoteRepository)
            inOrder.verify(revoteRepository).deleteVotesByRevoteId(1L)
            inOrder.verify(revoteRepository).deleteCandidatesByRevoteId(1L)
            inOrder.verify(revoteRepository).deleteRevotesByMeetingId(1L)
        }

        @Test
        fun `방장이 아닌 사용자 취소 — NOT_MEETING_HOST 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting(hostId = 1L))

            val ex = assertThrows<MannaException> { revoteDomainService.cancelRevote(1L, 2L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_HOST)
        }

        @Test
        fun `진행 중인 재투표 없음 — REVOTE_NOT_FOUND 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting(hostId = 1L))
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(null)

            val ex = assertThrows<MannaException> { revoteDomainService.cancelRevote(1L, 1L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.REVOTE_NOT_FOUND)
        }
    }

    @Nested
    inner class HasOpenRevote {

        @Test
        fun `진행 중인 재투표 있으면 true`() {
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(revote())

            assertThat(revoteDomainService.hasOpenRevote(1L)).isTrue()
        }

        @Test
        fun `진행 중인 재투표 없으면 false`() {
            whenever(revoteRepository.findOpenByMeetingId(1L)).thenReturn(null)

            assertThat(revoteDomainService.hasOpenRevote(1L)).isFalse()
        }
    }

    @Nested
    inner class DeleteAllByMeetingId {

        @Test
        fun `여러 재투표에 대해 votes, candidates, revotes 순서로 삭제`() {
            val rv1 = revote(id = 1L, meetingId = 1L)
            val rv2 = revote(id = 2L, meetingId = 1L)
            whenever(revoteRepository.findAllByMeetingId(1L)).thenReturn(listOf(rv1, rv2))

            revoteDomainService.deleteAllByMeetingId(1L)

            verify(revoteRepository).deleteVotesByRevoteId(1L)
            verify(revoteRepository).deleteCandidatesByRevoteId(1L)
            verify(revoteRepository).deleteVotesByRevoteId(2L)
            verify(revoteRepository).deleteCandidatesByRevoteId(2L)
            verify(revoteRepository).deleteRevotesByMeetingId(1L)
        }

        @Test
        fun `재투표 없으면 삭제 스킵 후 deleteRevotesByMeetingId만 호출`() {
            whenever(revoteRepository.findAllByMeetingId(1L)).thenReturn(emptyList())

            revoteDomainService.deleteAllByMeetingId(1L)

            verify(revoteRepository, never()).deleteVotesByRevoteId(any())
            verify(revoteRepository, never()).deleteCandidatesByRevoteId(any())
            verify(revoteRepository).deleteRevotesByMeetingId(1L)
        }
    }

    @Nested
    inner class GetRevoteStatusData {

        @Test
        fun `최신 재투표 현황 반환 — 후보, 투표, 참여자 수 포함`() {
            val m = meeting()
            val rv = revote(id = 1L)
            val candidates = listOf(candidate(1L, d1), candidate(1L, d2))
            val votes = listOf(RevoteVote(revoteId = 1L, userId = 2L, votedDate = d1))
            val participants = listOf(participant(m, 1L), participant(m, 2L))

            whenever(revoteRepository.findLatestByMeetingId(1L)).thenReturn(rv)
            whenever(revoteRepository.findCandidatesByRevoteId(1L)).thenReturn(candidates)
            whenever(revoteRepository.findVotesByRevoteId(1L)).thenReturn(votes)
            whenever(meetingRepository.findParticipantsByMeetingId(1L)).thenReturn(participants)

            val result = revoteDomainService.getRevoteStatusData(1L, userId = 2L)

            assertThat(result.candidates).hasSize(2)
            assertThat(result.votes).hasSize(1)
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.myVotedDate).isEqualTo(d1)
        }

        @Test
        fun `재투표 이력 없으면 REVOTE_NOT_FOUND 예외`() {
            whenever(revoteRepository.findLatestByMeetingId(1L)).thenReturn(null)

            val ex = assertThrows<MannaException> {
                revoteDomainService.getRevoteStatusData(1L, userId = 1L)
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.REVOTE_NOT_FOUND)
        }
    }
}
