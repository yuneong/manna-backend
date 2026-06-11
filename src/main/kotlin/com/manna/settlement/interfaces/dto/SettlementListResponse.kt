package com.manna.settlement.interfaces.dto

import com.manna.settlement.application.info.SettlementInfo

data class SettlementListResponse(
    val settlements: List<SettlementResponse>,
) {
    companion object {
        fun from(infos: List<SettlementInfo>) = SettlementListResponse(
            settlements = infos.map { SettlementResponse.from(it) },
        )
    }
}
