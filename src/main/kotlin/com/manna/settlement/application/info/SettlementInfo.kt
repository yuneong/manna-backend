package com.manna.settlement.application.info

import com.manna.settlement.domain.entity.Settlement
import com.manna.settlement.domain.entity.SettlementItem
import com.manna.settlement.domain.entity.SettlementItemParticipant
import com.manna.settlement.domain.entity.SettlementParticipant
import com.manna.settlement.domain.entity.SettlementStatus
import com.manna.settlement.domain.entity.SettlementType
import com.manna.user.domain.entity.User

data class SettlementCreatorInfo(
    val userId: Long,
    val nickname: String,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
)

data class SettlementParticipantInfo(
    val userId: Long,
    val nickname: String,
    val profileImage: String?,
    val amount: Int,
    val isPaid: Boolean,
)

data class SettlementItemInfo(
    val name: String,
    val amount: Int,
    val participantUserIds: List<Long>,
)

data class SettlementInfo(
    val settlementId: Long,
    val title: String,
    val type: SettlementType,
    val status: SettlementStatus,
    val totalAmount: Int?,
    val creator: SettlementCreatorInfo,
    val participants: List<SettlementParticipantInfo>,
    val items: List<SettlementItemInfo>,
) {
    companion object {
        fun from(
            settlement: Settlement,
            creator: User,
            participants: List<SettlementParticipant>,
            items: List<SettlementItem>,
            itemParticipants: List<SettlementItemParticipant>,
            userMap: Map<Long, User>,
        ): SettlementInfo {
            val itemParticipantsByItemId = itemParticipants.groupBy { it.settlementItem.id }
            return SettlementInfo(
                settlementId = settlement.id,
                title = settlement.title,
                type = settlement.type,
                status = settlement.status,
                totalAmount = settlement.totalAmount,
                creator = SettlementCreatorInfo(
                    userId = creator.id,
                    nickname = creator.nickname,
                    bankName = settlement.bankName,
                    accountNumber = settlement.accountNumber,
                    accountHolder = settlement.accountHolder,
                ),
                participants = participants.mapNotNull { p ->
                    userMap[p.userId]?.let { user ->
                        SettlementParticipantInfo(
                            userId = user.id,
                            nickname = user.nickname,
                            profileImage = user.profileImageUrl,
                            amount = p.amount,
                            isPaid = p.isPaid,
                        )
                    }
                },
                items = items.map { item ->
                    SettlementItemInfo(
                        name = item.name,
                        amount = item.amount,
                        participantUserIds = itemParticipantsByItemId[item.id]?.map { it.userId } ?: emptyList(),
                    )
                },
            )
        }
    }
}
