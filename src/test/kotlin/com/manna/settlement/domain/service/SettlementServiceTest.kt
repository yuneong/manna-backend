package com.manna.settlement.domain.service

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingParticipant
import com.manna.meeting.domain.entity.MeetingStatus
import com.manna.meeting.domain.repository.MeetingRepository
import com.manna.settlement.application.command.CreateSettlementCommand
import com.manna.settlement.application.command.CreateSettlementItemCommand
import com.manna.settlement.domain.entity.Settlement
import com.manna.settlement.domain.entity.SettlementItem
import com.manna.settlement.domain.entity.SettlementItemParticipant
import com.manna.settlement.domain.entity.SettlementParticipant
import com.manna.settlement.domain.entity.SettlementStatus
import com.manna.settlement.domain.entity.SettlementType
import com.manna.settlement.domain.repository.SettlementRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class SettlementServiceTest {

    private val settlementRepository: SettlementRepository = mock()
    private val meetingRepository: MeetingRepository = mock()
    private lateinit var settlementService: SettlementService

    @BeforeEach
    fun setUp() {
        settlementService = SettlementService(settlementRepository, meetingRepository)
    }

    private fun meeting(id: Long = 1L, status: MeetingStatus = MeetingStatus.PLACE_VOTING) = Meeting(
        id = id,
        hostId = 1L,
        title = "테스트",
        dateRangeStart = LocalDate.of(2026, 6, 1),
        dateRangeEnd = LocalDate.of(2026, 6, 30),
        status = status,
    )

    private fun meetingParticipant(meeting: Meeting, userId: Long) =
        MeetingParticipant(meeting = meeting, userId = userId)

    private fun settlement(
        id: Long = 1L,
        meeting: Meeting,
        creatorId: Long = 1L,
        type: SettlementType = SettlementType.TOTAL,
        status: SettlementStatus = SettlementStatus.IN_PROGRESS,
    ) = Settlement(
        id = id,
        meeting = meeting,
        creatorId = creatorId,
        title = "테스트 정산",
        type = type,
        totalAmount = if (type == SettlementType.TOTAL) 10000 else null,
        bankName = "카카오뱅크",
        accountNumber = "1234-5678",
        accountHolder = "홍길동",
        status = status,
    )

    private fun totalCommand(
        meetingId: Long = 1L,
        creatorId: Long = 1L,
        totalAmount: Int = 10000,
        participantUserIds: List<Long> = listOf(2L, 3L),
    ) = CreateSettlementCommand(
        meetingId = meetingId,
        creatorId = creatorId,
        title = "테스트 정산",
        type = SettlementType.TOTAL,
        totalAmount = totalAmount,
        participantUserIds = participantUserIds,
        bankName = "카카오뱅크",
        accountNumber = "1234-5678",
        accountHolder = "홍길동",
        items = emptyList(),
    )

    private fun itemizedCommand(
        meetingId: Long = 1L,
        creatorId: Long = 1L,
        items: List<CreateSettlementItemCommand>,
    ) = CreateSettlementCommand(
        meetingId = meetingId,
        creatorId = creatorId,
        title = "테스트 정산",
        type = SettlementType.ITEMIZED,
        totalAmount = null,
        participantUserIds = emptyList(),
        bankName = "카카오뱅크",
        accountNumber = "1234-5678",
        accountHolder = "홍길동",
        items = items,
    )

    @Nested
    inner class Create {

        @Test
        fun `TOTAL 방식 생성 성공 — 1인당 금액 계산 및 PLACE_VOTING에서 SETTLING으로 전이`() {
            val m = meeting(status = MeetingStatus.PLACE_VOTING)
            val s = settlement(meeting = m)
            val command = totalCommand(totalAmount = 10000, participantUserIds = listOf(2L, 3L))

            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 1L))
                .thenReturn(meetingParticipant(m, 1L))
            whenever(meetingRepository.findParticipantsByMeetingId(1L))
                .thenReturn(listOf(meetingParticipant(m, 1L), meetingParticipant(m, 2L), meetingParticipant(m, 3L)))
            whenever(settlementRepository.save(any())).thenReturn(s)
            whenever(settlementRepository.saveParticipant(any())).thenAnswer { it.arguments[0] as SettlementParticipant }
            whenever(meetingRepository.save(m)).thenReturn(m)

            settlementService.create(command)

            val captor = argumentCaptor<SettlementParticipant>()
            verify(settlementRepository, times(2)).saveParticipant(captor.capture())
            assertThat(captor.allValues.map { it.amount }).containsOnly(5000)
            assertThat(m.status).isEqualTo(MeetingStatus.SETTLING)
            verify(meetingRepository).save(m)
        }

        @Test
        fun `TOTAL 방식 — 이미 SETTLING 상태이면 meeting status 변경 없음`() {
            val m = meeting(status = MeetingStatus.SETTLING)
            val s = settlement(meeting = m)
            val command = totalCommand()

            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 1L))
                .thenReturn(meetingParticipant(m, 1L))
            whenever(meetingRepository.findParticipantsByMeetingId(1L))
                .thenReturn(listOf(meetingParticipant(m, 1L), meetingParticipant(m, 2L), meetingParticipant(m, 3L)))
            whenever(settlementRepository.save(any())).thenReturn(s)
            whenever(settlementRepository.saveParticipant(any())).thenAnswer { it.arguments[0] as SettlementParticipant }

            settlementService.create(command)

            assertThat(m.status).isEqualTo(MeetingStatus.SETTLING)
            verify(meetingRepository, never()).save(any())
        }

        @Test
        fun `ITEMIZED 방식 생성 성공 — 항목별 참여자 금액 합산`() {
            val m = meeting(status = MeetingStatus.SETTLING)
            val s = settlement(meeting = m, type = SettlementType.ITEMIZED)
            // item1: 9000원 / 3명 = 3000, item2: 8000원 / 2명 = 4000
            // user2: 3000+4000=7000, user3: 3000+4000=7000, user4: 3000
            val items = listOf(
                CreateSettlementItemCommand("음식", 9000, listOf(2L, 3L, 4L)),
                CreateSettlementItemCommand("술", 8000, listOf(2L, 3L)),
            )
            val command = itemizedCommand(items = items)

            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 1L))
                .thenReturn(meetingParticipant(m, 1L))
            whenever(meetingRepository.findParticipantsByMeetingId(1L))
                .thenReturn((1L..4L).map { meetingParticipant(m, it) })
            whenever(settlementRepository.save(any())).thenReturn(s)
            whenever(settlementRepository.saveItem(any())).thenAnswer { it.arguments[0] as SettlementItem }
            whenever(settlementRepository.saveItemParticipant(any())).thenAnswer { it.arguments[0] as SettlementItemParticipant }
            whenever(settlementRepository.saveParticipant(any())).thenAnswer { it.arguments[0] as SettlementParticipant }

            settlementService.create(command)

            val captor = argumentCaptor<SettlementParticipant>()
            verify(settlementRepository, times(3)).saveParticipant(captor.capture())
            val amountByUser = captor.allValues.associate { it.userId to it.amount }
            assertThat(amountByUser[2L]).isEqualTo(7000)
            assertThat(amountByUser[3L]).isEqualTo(7000)
            assertThat(amountByUser[4L]).isEqualTo(3000)
        }

        @Test
        fun `존재하지 않는 약속방 — MEETING_NOT_FOUND 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(null)

            val ex = assertThrows<MannaException> { settlementService.create(totalCommand()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MEETING_NOT_FOUND)
            verify(settlementRepository, never()).save(any())
        }

        @Test
        fun `생성자가 약속방 참여자 아님 — NOT_MEETING_PARTICIPANT 예외`() {
            whenever(meetingRepository.findById(1L)).thenReturn(meeting())
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 1L)).thenReturn(null)

            val ex = assertThrows<MannaException> { settlementService.create(totalCommand()) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_PARTICIPANT)
            verify(settlementRepository, never()).save(any())
        }

        @Test
        fun `participantUserIds에 비참여자 포함 — NOT_MEETING_PARTICIPANT 예외`() {
            val m = meeting()
            whenever(meetingRepository.findById(1L)).thenReturn(m)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 1L))
                .thenReturn(meetingParticipant(m, 1L))
            // meeting 참여자는 1, 2만 있고 command에는 2, 99 포함 → 99는 비참여자
            whenever(meetingRepository.findParticipantsByMeetingId(1L))
                .thenReturn(listOf(meetingParticipant(m, 1L), meetingParticipant(m, 2L)))

            val ex = assertThrows<MannaException> {
                settlementService.create(totalCommand(participantUserIds = listOf(2L, 99L)))
            }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_PARTICIPANT)
            verify(settlementRepository, never()).save(any())
        }
    }

    @Nested
    inner class GetByMeetingId {

        @Test
        fun `정산 목록 조회 성공`() {
            val m = meeting()
            val s = settlement(meeting = m)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L))
                .thenReturn(meetingParticipant(m, 2L))
            whenever(settlementRepository.findByMeetingId(1L)).thenReturn(listOf(s))

            val result = settlementService.getByMeetingId(meetingId = 1L, userId = 2L)

            assertThat(result).hasSize(1)
        }

        @Test
        fun `비참여자 조회 시 NOT_MEETING_PARTICIPANT 예외`() {
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 99L)).thenReturn(null)

            val ex = assertThrows<MannaException> { settlementService.getByMeetingId(1L, 99L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_PARTICIPANT)
        }
    }

    @Nested
    inner class GetById {

        @Test
        fun `단건 조회 성공`() {
            val m = meeting()
            val s = settlement(id = 1L, meeting = m)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L))
                .thenReturn(meetingParticipant(m, 2L))
            whenever(settlementRepository.findById(1L)).thenReturn(s)

            val result = settlementService.getById(meetingId = 1L, settlementId = 1L, userId = 2L)

            assertThat(result.id).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않는 정산 — SETTLEMENT_NOT_FOUND 예외`() {
            val m = meeting()
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L))
                .thenReturn(meetingParticipant(m, 2L))
            whenever(settlementRepository.findById(999L)).thenReturn(null)

            val ex = assertThrows<MannaException> { settlementService.getById(1L, 999L, 2L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND)
        }

        @Test
        fun `다른 약속방 정산 조회 시 SETTLEMENT_NOT_FOUND 예외`() {
            val m1 = meeting(id = 1L)
            val m2 = meeting(id = 2L)
            val s = settlement(id = 1L, meeting = m2)
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 2L))
                .thenReturn(meetingParticipant(m1, 2L))
            whenever(settlementRepository.findById(1L)).thenReturn(s)

            val ex = assertThrows<MannaException> { settlementService.getById(meetingId = 1L, settlementId = 1L, userId = 2L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND)
        }

        @Test
        fun `비참여자 조회 시 NOT_MEETING_PARTICIPANT 예외`() {
            whenever(meetingRepository.findParticipantByMeetingIdAndUserId(1L, 99L)).thenReturn(null)

            val ex = assertThrows<MannaException> { settlementService.getById(1L, 1L, 99L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.NOT_MEETING_PARTICIPANT)
        }
    }

    @Nested
    inner class MarkPaid {

        @Test
        fun `납부 완료 처리 — isPaid true로 변경`() {
            val m = meeting()
            val s = settlement(meeting = m)
            val participant = SettlementParticipant(settlement = s, userId = 2L, amount = 5000, isPaid = false)

            whenever(settlementRepository.findParticipantBySettlementIdAndUserId(1L, 2L))
                .thenReturn(participant)
            whenever(settlementRepository.saveParticipant(any())).thenAnswer { it.arguments[0] as SettlementParticipant }

            val result = settlementService.markPaid(settlementId = 1L, userId = 2L)

            assertThat(result.isPaid).isTrue()
        }

        @Test
        fun `정산 대상자 아님 — SETTLEMENT_NOT_PARTICIPANT 예외`() {
            whenever(settlementRepository.findParticipantBySettlementIdAndUserId(1L, 99L)).thenReturn(null)

            val ex = assertThrows<MannaException> { settlementService.markPaid(1L, 99L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.SETTLEMENT_NOT_PARTICIPANT)
        }
    }

    @Nested
    inner class Complete {

        @Test
        fun `전원 납부 완료 시 정산 COMPLETED 처리`() {
            val m = meeting()
            val s = settlement(meeting = m)
            val participants = listOf(
                SettlementParticipant(settlement = s, userId = 2L, amount = 5000, isPaid = true),
                SettlementParticipant(settlement = s, userId = 3L, amount = 5000, isPaid = true),
            )

            whenever(settlementRepository.findById(1L)).thenReturn(s)
            whenever(settlementRepository.findParticipantsBySettlementId(1L)).thenReturn(participants)
            whenever(settlementRepository.save(any())).thenAnswer { it.arguments[0] as Settlement }

            val result = settlementService.complete(settlementId = 1L, userId = 1L)

            assertThat(result.status).isEqualTo(SettlementStatus.COMPLETED)
        }

        @Test
        fun `존재하지 않는 정산 완료 시 SETTLEMENT_NOT_FOUND 예외`() {
            whenever(settlementRepository.findById(999L)).thenReturn(null)

            val ex = assertThrows<MannaException> { settlementService.complete(999L, 1L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND)
        }

        @Test
        fun `수금자 아닌 사용자 완료 처리 시 SETTLEMENT_NOT_CREATOR 예외`() {
            val m = meeting()
            val s = settlement(meeting = m, creatorId = 1L)
            whenever(settlementRepository.findById(1L)).thenReturn(s)

            val ex = assertThrows<MannaException> { settlementService.complete(settlementId = 1L, userId = 99L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.SETTLEMENT_NOT_CREATOR)
            verify(settlementRepository, never()).save(any())
        }

        @Test
        fun `미납부자 있으면 SETTLEMENT_NOT_ALL_PAID 예외`() {
            val m = meeting()
            val s = settlement(meeting = m)
            val participants = listOf(
                SettlementParticipant(settlement = s, userId = 2L, amount = 5000, isPaid = true),
                SettlementParticipant(settlement = s, userId = 3L, amount = 5000, isPaid = false),
            )

            whenever(settlementRepository.findById(1L)).thenReturn(s)
            whenever(settlementRepository.findParticipantsBySettlementId(1L)).thenReturn(participants)

            val ex = assertThrows<MannaException> { settlementService.complete(settlementId = 1L, userId = 1L) }
            assertThat(ex.errorCode).isEqualTo(ErrorCode.SETTLEMENT_NOT_ALL_PAID)
            verify(settlementRepository, never()).save(any())
        }
    }
}
