package com.manna.settlement.application.command

import com.manna.settlement.domain.entity.SettlementType

data class CreateSettlementCommand(
    val meetingId: Long,
    val creatorId: Long,
    val title: String,
    val type: SettlementType,
    val totalAmount: Int?,
    val participantUserIds: List<Long>,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
    val items: List<CreateSettlementItemCommand>,
)

data class CreateSettlementItemCommand(
    val name: String,
    val amount: Int,
    val participantUserIds: List<Long>,
)
