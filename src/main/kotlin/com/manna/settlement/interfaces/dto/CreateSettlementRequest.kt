package com.manna.settlement.interfaces.dto

import com.manna.settlement.application.command.CreateSettlementCommand
import com.manna.settlement.application.command.CreateSettlementItemCommand
import com.manna.settlement.domain.entity.SettlementType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateSettlementRequest(
    @field:NotBlank(message = "정산 제목은 필수입니다")
    val title: String,

    @field:NotNull(message = "정산 방식은 필수입니다")
    val type: SettlementType,

    val totalAmount: Int?,

    val participantUserIds: List<Long> = emptyList(),

    @field:NotBlank(message = "은행명은 필수입니다")
    val bankName: String,

    @field:NotBlank(message = "계좌번호는 필수입니다")
    val accountNumber: String,

    @field:NotBlank(message = "예금주는 필수입니다")
    val accountHolder: String,

    @field:Valid
    val items: List<CreateSettlementItemRequest> = emptyList(),
) {
    fun toCommand(meetingId: Long, creatorId: Long) = CreateSettlementCommand(
        meetingId = meetingId,
        creatorId = creatorId,
        title = title,
        type = type,
        totalAmount = totalAmount,
        participantUserIds = participantUserIds,
        bankName = bankName,
        accountNumber = accountNumber,
        accountHolder = accountHolder,
        items = items.map { it.toCommand() },
    )
}

data class CreateSettlementItemRequest(
    @field:NotBlank(message = "항목 이름은 필수입니다")
    val name: String,

    @field:NotNull(message = "항목 금액은 필수입니다")
    val amount: Int,

    val participantUserIds: List<Long> = emptyList(),
) {
    fun toCommand() = CreateSettlementItemCommand(
        name = name,
        amount = amount,
        participantUserIds = participantUserIds,
    )
}
