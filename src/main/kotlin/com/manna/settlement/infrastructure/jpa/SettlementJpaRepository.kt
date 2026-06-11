package com.manna.settlement.infrastructure.jpa

import com.manna.settlement.domain.entity.Settlement
import com.manna.settlement.domain.entity.SettlementItem
import com.manna.settlement.domain.entity.SettlementItemParticipant
import com.manna.settlement.domain.entity.SettlementParticipant
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementJpaRepository : JpaRepository<Settlement, Long> {
    fun findByMeetingId(meetingId: Long): List<Settlement>
}

interface SettlementParticipantJpaRepository : JpaRepository<SettlementParticipant, Long> {
    fun findBySettlementIdOrderByUserIdAsc(settlementId: Long): List<SettlementParticipant>
    fun findBySettlementIdInOrderByUserIdAsc(settlementIds: List<Long>): List<SettlementParticipant>
    fun findBySettlementIdAndUserId(settlementId: Long, userId: Long): SettlementParticipant?
}

interface SettlementItemJpaRepository : JpaRepository<SettlementItem, Long> {
    fun findBySettlementId(settlementId: Long): List<SettlementItem>
    fun findBySettlementIdIn(settlementIds: List<Long>): List<SettlementItem>
}

interface SettlementItemParticipantJpaRepository : JpaRepository<SettlementItemParticipant, Long> {
    fun findBySettlementItemIdIn(settlementItemIds: List<Long>): List<SettlementItemParticipant>
}
