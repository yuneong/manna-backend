package com.manna.settlement.infrastructure.repository

import com.manna.settlement.domain.entity.Settlement
import com.manna.settlement.domain.entity.SettlementItem
import com.manna.settlement.domain.entity.SettlementItemParticipant
import com.manna.settlement.domain.entity.SettlementParticipant
import com.manna.settlement.domain.repository.SettlementRepository
import com.manna.settlement.infrastructure.jpa.SettlementItemJpaRepository
import com.manna.settlement.infrastructure.jpa.SettlementItemParticipantJpaRepository
import com.manna.settlement.infrastructure.jpa.SettlementJpaRepository
import com.manna.settlement.infrastructure.jpa.SettlementParticipantJpaRepository
import org.springframework.stereotype.Repository

@Repository
class SettlementRepositoryImpl(
    private val settlementJpaRepository: SettlementJpaRepository,
    private val participantJpaRepository: SettlementParticipantJpaRepository,
    private val itemJpaRepository: SettlementItemJpaRepository,
    private val itemParticipantJpaRepository: SettlementItemParticipantJpaRepository,
) : SettlementRepository {

    override fun save(settlement: Settlement): Settlement =
        settlementJpaRepository.save(settlement)

    override fun findById(id: Long): Settlement? =
        settlementJpaRepository.findById(id).orElse(null)

    override fun findByMeetingId(meetingId: Long): List<Settlement> =
        settlementJpaRepository.findByMeetingId(meetingId)

    override fun saveParticipant(participant: SettlementParticipant): SettlementParticipant =
        participantJpaRepository.save(participant)

    override fun findParticipantsBySettlementId(settlementId: Long): List<SettlementParticipant> =
        participantJpaRepository.findBySettlementIdOrderByUserIdAsc(settlementId)

    override fun findParticipantsBySettlementIds(settlementIds: List<Long>): List<SettlementParticipant> =
        participantJpaRepository.findBySettlementIdInOrderByUserIdAsc(settlementIds)

    override fun findParticipantBySettlementIdAndUserId(settlementId: Long, userId: Long): SettlementParticipant? =
        participantJpaRepository.findBySettlementIdAndUserId(settlementId, userId)

    override fun saveItem(item: SettlementItem): SettlementItem =
        itemJpaRepository.save(item)

    override fun findItemsBySettlementId(settlementId: Long): List<SettlementItem> =
        itemJpaRepository.findBySettlementId(settlementId)

    override fun findItemsBySettlementIds(settlementIds: List<Long>): List<SettlementItem> =
        itemJpaRepository.findBySettlementIdIn(settlementIds)

    override fun saveItemParticipant(participant: SettlementItemParticipant): SettlementItemParticipant =
        itemParticipantJpaRepository.save(participant)

    override fun findItemParticipantsBySettlementItemIds(settlementItemIds: List<Long>): List<SettlementItemParticipant> =
        itemParticipantJpaRepository.findBySettlementItemIdIn(settlementItemIds)
}
