package com.manna.settlement.application.facade

import com.manna.settlement.application.command.CreateSettlementCommand
import com.manna.settlement.domain.entity.Settlement
import com.manna.settlement.domain.entity.SettlementItem
import com.manna.settlement.domain.entity.SettlementItemParticipant
import com.manna.settlement.domain.entity.SettlementParticipant
import com.manna.settlement.domain.entity.SettlementStatus
import com.manna.settlement.domain.entity.SettlementType
import com.manna.settlement.domain.service.SettlementService
import com.manna.meeting.domain.entity.Meeting
import com.manna.meeting.domain.entity.MeetingStatus
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

class SettlementFacadeTest {

    private val settlementService: SettlementService = mock()
    private val userDomainService: UserDomainService = mock()
    private lateinit var settlementFacade: SettlementFacade

    @BeforeEach
    fun setUp() {
        settlementFacade = SettlementFacade(settlementService, userDomainService)
    }

    private fun meeting(id: Long = 1L) = Meeting(
        id = id,
        hostId = 1L,
        title = "테스트",
        dateRangeStart = LocalDate.of(2026, 6, 1),
        dateRangeEnd = LocalDate.of(2026, 6, 30),
        status = MeetingStatus.SETTLING,
    )

    private fun settlement(id: Long = 1L, meeting: Meeting = meeting(), creatorId: Long = 1L) = Settlement(
        id = id,
        meeting = meeting,
        creatorId = creatorId,
        title = "테스트 정산",
        type = SettlementType.TOTAL,
        totalAmount = 10000,
        bankName = "카카오뱅크",
        accountNumber = "1234-5678",
        accountHolder = "홍길동",
    )

    private fun user(id: Long) = User(
        id = id,
        email = "user$id@test.com",
        password = "pw",
        nickname = "유저$id",
    )

    @Nested
    inner class CreateSettlement {

        @Test
        fun `정산 생성 후 creator와 participants 정보가 포함된 SettlementInfo 반환`() {
            val m = meeting()
            val s = settlement(meeting = m, creatorId = 1L)
            val participants = listOf(
                SettlementParticipant(settlement = s, userId = 2L, amount = 5000),
                SettlementParticipant(settlement = s, userId = 3L, amount = 5000),
            )
            val command = CreateSettlementCommand(
                meetingId = 1L, creatorId = 1L, title = "테스트 정산",
                type = SettlementType.TOTAL, totalAmount = 10000,
                participantUserIds = listOf(2L, 3L),
                bankName = "카카오뱅크", accountNumber = "1234-5678", accountHolder = "홍길동",
                items = emptyList(),
            )

            whenever(settlementService.create(command)).thenReturn(s)
            whenever(settlementService.getParticipantsBySettlementId(1L)).thenReturn(participants)
            whenever(settlementService.getItemsBySettlementId(1L)).thenReturn(emptyList())
            whenever(userDomainService.getUsersByIds(listOf(2L, 3L, 1L)))
                .thenReturn(listOf(user(1L), user(2L), user(3L)))

            val result = settlementFacade.createSettlement(command)

            assertThat(result.settlementId).isEqualTo(1L)
            assertThat(result.creator.userId).isEqualTo(1L)
            assertThat(result.participants).hasSize(2)
            assertThat(result.participants.map { it.userId }).containsExactlyInAnyOrder(2L, 3L)
        }
    }

    @Nested
    inner class GetSettlements {

        @Test
        fun `정산이 없으면 빈 리스트 반환`() {
            whenever(settlementService.getByMeetingId(1L, 2L)).thenReturn(emptyList())

            val result = settlementFacade.getSettlements(meetingId = 1L, userId = 2L)

            assertThat(result).isEmpty()
        }

        @Test
        fun `정산 목록 반환 — 각 정산의 참여자 정보 포함`() {
            val m = meeting()
            val s1 = settlement(id = 1L, meeting = m)
            val s2 = settlement(id = 2L, meeting = m)
            val participants = listOf(
                SettlementParticipant(settlement = s1, userId = 2L, amount = 5000),
                SettlementParticipant(settlement = s2, userId = 3L, amount = 3000),
            )

            whenever(settlementService.getByMeetingId(1L, 2L)).thenReturn(listOf(s1, s2))
            whenever(settlementService.getParticipantsBySettlementIds(listOf(1L, 2L))).thenReturn(participants)
            whenever(settlementService.getItemsBySettlementIds(listOf(1L, 2L))).thenReturn(emptyList())
            whenever(userDomainService.getUsersByIds(listOf(2L, 3L, 1L)))
                .thenReturn(listOf(user(1L), user(2L), user(3L)))

            val result = settlementFacade.getSettlements(meetingId = 1L, userId = 2L)

            assertThat(result).hasSize(2)
            assertThat(result.first { it.settlementId == 1L }.participants.map { it.userId }).containsExactly(2L)
            assertThat(result.first { it.settlementId == 2L }.participants.map { it.userId }).containsExactly(3L)
        }
    }

    @Nested
    inner class GetSettlement {

        @Test
        fun `단건 조회 — ITEMIZED 방식은 items 포함`() {
            val m = meeting()
            val itemizedSettlement = Settlement(
                id = 1L,
                meeting = m,
                creatorId = 1L,
                title = "테스트",
                type = SettlementType.ITEMIZED,
                totalAmount = null,
                bankName = "카카오뱅크",
                accountNumber = "1234-5678",
                accountHolder = "홍길동",
            )
            val item = SettlementItem(id = 1L, settlement = itemizedSettlement, name = "음식", amount = 9000)
            val itemParticipant = SettlementItemParticipant(id = 1L, settlementItem = item, userId = 2L)
            val participant = SettlementParticipant(settlement = itemizedSettlement, userId = 2L, amount = 9000)

            whenever(settlementService.getById(1L, 1L, 2L)).thenReturn(itemizedSettlement)
            whenever(settlementService.getParticipantsBySettlementId(1L)).thenReturn(listOf(participant))
            whenever(settlementService.getItemsBySettlementId(1L)).thenReturn(listOf(item))
            whenever(settlementService.getItemParticipantsByItemIds(listOf(1L))).thenReturn(listOf(itemParticipant))
            whenever(userDomainService.getUsersByIds(listOf(2L, 1L)))
                .thenReturn(listOf(user(1L), user(2L)))

            val result = settlementFacade.getSettlement(meetingId = 1L, settlementId = 1L, userId = 2L)

            assertThat(result.items).hasSize(1)
            assertThat(result.items.first().participantUserIds).containsExactly(2L)
        }
    }

    @Nested
    inner class MarkPaid {

        @Test
        fun `납부 완료 처리 — service에 위임`() {
            settlementFacade.markPaid(settlementId = 1L, userId = 2L)

            verify(settlementService).markPaid(settlementId = 1L, userId = 2L)
        }
    }

    @Nested
    inner class Complete {

        @Test
        fun `정산 완료 처리 — service에 위임`() {
            val m = meeting()
            val s = settlement(meeting = m)
            whenever(settlementService.complete(1L, 1L)).thenReturn(s)

            settlementFacade.complete(settlementId = 1L, userId = 1L)

            verify(settlementService).complete(settlementId = 1L, userId = 1L)
        }
    }
}
