package com.manna.settlement.interfaces.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.manna.settlement.application.info.SettlementInfo
import com.manna.settlement.domain.entity.SettlementStatus
import com.manna.settlement.domain.entity.SettlementType

data class SettlementCreatorDto(
    val userId: Long,
    val nickname: String,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
)

data class SettlementParticipantDto(
    val userId: Long,
    val nickname: String,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val profileImage: String?,
    val amount: Int,
    val isPaid: Boolean,
)

data class SettlementItemDto(
    val name: String,
    val amount: Int,
    val participantUserIds: List<Long>,
)

data class SettlementResponse(
    val settlementId: Long,
    val title: String,
    val type: SettlementType,
    val status: SettlementStatus,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val totalAmount: Int?,
    val creator: SettlementCreatorDto,
    val participants: List<SettlementParticipantDto>,
    val items: List<SettlementItemDto>,
) {
    companion object {
        fun from(info: SettlementInfo) = SettlementResponse(
            settlementId = info.settlementId,
            title = info.title,
            type = info.type,
            status = info.status,
            totalAmount = info.totalAmount,
            creator = SettlementCreatorDto(
                userId = info.creator.userId,
                nickname = info.creator.nickname,
                bankName = info.creator.bankName,
                accountNumber = info.creator.accountNumber,
                accountHolder = info.creator.accountHolder,
            ),
            participants = info.participants.map {
                SettlementParticipantDto(
                    userId = it.userId,
                    nickname = it.nickname,
                    profileImage = it.profileImage,
                    amount = it.amount,
                    isPaid = it.isPaid,
                )
            },
            items = info.items.map {
                SettlementItemDto(
                    name = it.name,
                    amount = it.amount,
                    participantUserIds = it.participantUserIds,
                )
            },
        )
    }
}
