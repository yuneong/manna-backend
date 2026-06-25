package com.manna.settlement.domain.service

import com.manna.common.exception.ErrorCode
import com.manna.common.exception.MannaException
import com.manna.meeting.domain.entity.MeetingStatus
import com.manna.meeting.domain.repository.MeetingRepository
import com.manna.settlement.application.command.CreateSettlementCommand
import com.manna.settlement.domain.entity.Settlement
import com.manna.settlement.domain.entity.SettlementItem
import com.manna.settlement.domain.entity.SettlementItemParticipant
import com.manna.settlement.domain.entity.SettlementParticipant
import com.manna.settlement.domain.entity.SettlementStatus
import com.manna.settlement.domain.entity.SettlementType
import com.manna.settlement.domain.repository.SettlementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val meetingRepository: MeetingRepository,
) {

    @Transactional
    fun create(command: CreateSettlementCommand): Settlement {
        val meeting = meetingRepository.findById(command.meetingId)
            ?: throw MannaException(ErrorCode.MEETING_NOT_FOUND)

        if (!meeting.status.isSettlementAddable()) throw MannaException(ErrorCode.MEETING_SETTLEMENT_NOT_ADDABLE)

        if (meetingRepository.findParticipantByMeetingIdAndUserId(command.meetingId, command.creatorId) == null) {
            throw MannaException(ErrorCode.NOT_MEETING_PARTICIPANT)
        }

        val allParticipantIds = when (command.type) {
            SettlementType.TOTAL -> command.participantUserIds
            SettlementType.ITEMIZED -> command.items.flatMap { it.participantUserIds }.distinct()
        }
        val meetingParticipantIds = meetingRepository.findParticipantsByMeetingId(command.meetingId)
            .map { it.userId }.toSet()
        val invalidIds = allParticipantIds.filter { it !in meetingParticipantIds }
        if (invalidIds.isNotEmpty()) throw MannaException(ErrorCode.NOT_MEETING_PARTICIPANT)

        val settlement = settlementRepository.save(
            Settlement(
                meeting = meeting,
                creatorId = command.creatorId,
                title = command.title,
                type = command.type,
                totalAmount = command.totalAmount,
                bankName = command.bankName,
                accountNumber = command.accountNumber,
                accountHolder = command.accountHolder,
            ),
        )

        when (command.type) {
            SettlementType.TOTAL -> {
                val ids = command.participantUserIds
                val total = command.totalAmount!!
                val base = total / ids.size
                val remainder = total % ids.size
                ids.forEach { userId ->
                    val isCreator = userId == command.creatorId
                    settlementRepository.saveParticipant(
                        SettlementParticipant(
                            settlement = settlement,
                            userId = userId,
                            amount = if (isCreator) base + remainder else base,
                            isPaid = isCreator,
                        ),
                    )
                }
            }
            SettlementType.ITEMIZED -> {
                val amountByUser = mutableMapOf<Long, Int>()
                command.items.forEach { itemCmd ->
                    val savedItem = settlementRepository.saveItem(
                        SettlementItem(settlement = settlement, name = itemCmd.name, amount = itemCmd.amount),
                    )
                    val ids = itemCmd.participantUserIds
                    val base = itemCmd.amount / ids.size
                    val remainder = itemCmd.amount % ids.size
                    // 나머지는 creator가 이 항목에 있으면 creator가 흡수, 없으면 userId 오름차순 앞 r명이 +1원씩
                    val extraByUser: Map<Long, Int> =
                        if (command.creatorId in ids) mapOf(command.creatorId to remainder)
                        else ids.sorted().take(remainder).associateWith { 1 }
                    ids.forEach { userId ->
                        settlementRepository.saveItemParticipant(
                            SettlementItemParticipant(settlementItem = savedItem, userId = userId),
                        )
                        amountByUser[userId] = (amountByUser[userId] ?: 0) + base + (extraByUser[userId] ?: 0)
                    }
                }
                amountByUser.forEach { (userId, amount) ->
                    settlementRepository.saveParticipant(
                        SettlementParticipant(
                            settlement = settlement,
                            userId = userId,
                            amount = amount,
                            isPaid = userId == command.creatorId,
                        ),
                    )
                }
            }
        }

        if (meeting.status == MeetingStatus.CONFIRMED || meeting.status == MeetingStatus.PLACE_VOTING) {
            meeting.status = MeetingStatus.SETTLING
            meetingRepository.save(meeting)
        }

        return settlement
    }

    fun getByMeetingId(meetingId: Long, userId: Long): List<Settlement> {
        if (meetingRepository.findParticipantByMeetingIdAndUserId(meetingId, userId) == null) {
            throw MannaException(ErrorCode.NOT_MEETING_PARTICIPANT)
        }
        return settlementRepository.findByMeetingId(meetingId)
    }

    fun getById(meetingId: Long, settlementId: Long, userId: Long): Settlement {
        if (meetingRepository.findParticipantByMeetingIdAndUserId(meetingId, userId) == null) {
            throw MannaException(ErrorCode.NOT_MEETING_PARTICIPANT)
        }
        val settlement = settlementRepository.findById(settlementId)
            ?: throw MannaException(ErrorCode.SETTLEMENT_NOT_FOUND)
        if (settlement.meeting.id != meetingId) throw MannaException(ErrorCode.SETTLEMENT_NOT_FOUND)
        return settlement
    }

    @Transactional
    fun markPaid(settlementId: Long, userId: Long): SettlementParticipant {
        val settlement = settlementRepository.findById(settlementId)
            ?: throw MannaException(ErrorCode.SETTLEMENT_NOT_FOUND)
        if (settlement.creatorId == userId) throw MannaException(ErrorCode.SETTLEMENT_NOT_PARTICIPANT)
        val participant = settlementRepository.findParticipantBySettlementIdAndUserId(settlementId, userId)
            ?: throw MannaException(ErrorCode.SETTLEMENT_NOT_PARTICIPANT)
        participant.isPaid = true
        return settlementRepository.saveParticipant(participant)
    }

    @Transactional
    fun complete(settlementId: Long, userId: Long): Settlement {
        val settlement = settlementRepository.findById(settlementId)
            ?: throw MannaException(ErrorCode.SETTLEMENT_NOT_FOUND)
        if (settlement.creatorId != userId) throw MannaException(ErrorCode.SETTLEMENT_NOT_CREATOR)

        val participants = settlementRepository.findParticipantsBySettlementId(settlementId)
        if (participants.any { !it.isPaid }) throw MannaException(ErrorCode.SETTLEMENT_NOT_ALL_PAID)

        settlement.status = SettlementStatus.COMPLETED
        return settlementRepository.save(settlement)
    }

    fun getParticipantsBySettlementId(settlementId: Long): List<SettlementParticipant> =
        settlementRepository.findParticipantsBySettlementId(settlementId)

    fun getItemsBySettlementId(settlementId: Long): List<SettlementItem> =
        settlementRepository.findItemsBySettlementId(settlementId)

    fun getItemsBySettlementIds(settlementIds: List<Long>): List<SettlementItem> =
        settlementRepository.findItemsBySettlementIds(settlementIds)

    fun getItemParticipantsByItemIds(itemIds: List<Long>): List<SettlementItemParticipant> =
        settlementRepository.findItemParticipantsBySettlementItemIds(itemIds)

    fun getParticipantsBySettlementIds(settlementIds: List<Long>): List<SettlementParticipant> =
        settlementRepository.findParticipantsBySettlementIds(settlementIds)

    fun isAllSettlementsCompleted(meetingId: Long): Boolean {
        val settlements = settlementRepository.findByMeetingId(meetingId)
        return settlements.isNotEmpty() && settlements.all { it.status == SettlementStatus.COMPLETED }
    }
}
