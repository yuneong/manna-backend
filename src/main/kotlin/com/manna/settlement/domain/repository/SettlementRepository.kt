package com.manna.settlement.domain.repository

import com.manna.settlement.domain.entity.Settlement
import com.manna.settlement.domain.entity.SettlementItem
import com.manna.settlement.domain.entity.SettlementItemParticipant
import com.manna.settlement.domain.entity.SettlementParticipant

interface SettlementRepository {
    fun save(settlement: Settlement): Settlement
    fun findById(id: Long): Settlement?
    fun findByMeetingId(meetingId: Long): List<Settlement>

    fun saveParticipant(participant: SettlementParticipant): SettlementParticipant
    fun findParticipantsBySettlementId(settlementId: Long): List<SettlementParticipant>
    fun findParticipantsBySettlementIds(settlementIds: List<Long>): List<SettlementParticipant>
    fun findParticipantBySettlementIdAndUserId(settlementId: Long, userId: Long): SettlementParticipant?

    fun saveItem(item: SettlementItem): SettlementItem
    fun findItemsBySettlementId(settlementId: Long): List<SettlementItem>
    fun findItemsBySettlementIds(settlementIds: List<Long>): List<SettlementItem>

    fun saveItemParticipant(participant: SettlementItemParticipant): SettlementItemParticipant
    fun findItemParticipantsBySettlementItemIds(settlementItemIds: List<Long>): List<SettlementItemParticipant>
}
