package com.manna.settlement.application.facade

import com.manna.settlement.application.command.CreateSettlementCommand
import com.manna.settlement.application.info.SettlementInfo
import com.manna.settlement.domain.service.SettlementService
import com.manna.user.domain.service.UserDomainService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SettlementFacade(
    private val settlementService: SettlementService,
    private val userDomainService: UserDomainService,
) {

    @Transactional
    fun createSettlement(command: CreateSettlementCommand): SettlementInfo {
        val settlement = settlementService.create(command)
        val participants = settlementService.getParticipantsBySettlementId(settlement.id)
        val items = settlementService.getItemsBySettlementId(settlement.id)
        val itemParticipants = if (items.isEmpty()) emptyList()
        else settlementService.getItemParticipantsByItemIds(items.map { it.id })

        val allUserIds = (participants.map { it.userId } + listOf(settlement.creatorId)).distinct()
        val userMap = userDomainService.getUsersByIds(allUserIds).associateBy { it.id }
        val creator = userMap[settlement.creatorId]!!

        return SettlementInfo.from(settlement, creator, participants, items, itemParticipants, userMap)
    }

    fun getSettlements(meetingId: Long, userId: Long): List<SettlementInfo> {
        val settlements = settlementService.getByMeetingId(meetingId, userId)
        if (settlements.isEmpty()) return emptyList()

        val settlementIds = settlements.map { it.id }
        val participantsBySettlement = settlementService.getParticipantsBySettlementIds(settlementIds)
            .groupBy { it.settlement.id }
        val items = settlementService.getItemsBySettlementIds(settlementIds)
        val itemsBySettlement = items.groupBy { it.settlement.id }
        val itemParticipants = if (items.isEmpty()) emptyList()
        else settlementService.getItemParticipantsByItemIds(items.map { it.id })
        val itemParticipantsByItem = itemParticipants.groupBy { it.settlementItem.id }

        val allUserIds = (participantsBySettlement.values.flatten().map { it.userId } +
            settlements.map { it.creatorId }).distinct()
        val userMap = userDomainService.getUsersByIds(allUserIds).associateBy { it.id }

        return settlements.map { settlement ->
            val creator = userMap[settlement.creatorId]!!
            val settlementParticipants = participantsBySettlement[settlement.id] ?: emptyList()
            val settlementItems = itemsBySettlement[settlement.id] ?: emptyList()
            val settlementItemParticipants = settlementItems.flatMap { item ->
                itemParticipantsByItem[item.id] ?: emptyList()
            }
            SettlementInfo.from(settlement, creator, settlementParticipants, settlementItems, settlementItemParticipants, userMap)
        }
    }

    fun getSettlement(meetingId: Long, settlementId: Long, userId: Long): SettlementInfo {
        val settlement = settlementService.getById(meetingId, settlementId, userId)
        val participants = settlementService.getParticipantsBySettlementId(settlement.id)
        val items = settlementService.getItemsBySettlementId(settlement.id)
        val itemParticipants = if (items.isEmpty()) emptyList()
        else settlementService.getItemParticipantsByItemIds(items.map { it.id })

        val allUserIds = (participants.map { it.userId } + listOf(settlement.creatorId)).distinct()
        val userMap = userDomainService.getUsersByIds(allUserIds).associateBy { it.id }
        val creator = userMap[settlement.creatorId]!!

        return SettlementInfo.from(settlement, creator, participants, items, itemParticipants, userMap)
    }

    @Transactional
    fun markPaid(settlementId: Long, userId: Long) {
        settlementService.markPaid(settlementId, userId)
    }

    @Transactional
    fun complete(settlementId: Long, userId: Long) {
        settlementService.complete(settlementId, userId)
    }
}
