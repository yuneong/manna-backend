package com.manna.meeting.domain.entity

enum class MeetingStatus {
    OPEN, CONFIRMED, PLACE_VOTING, SETTLING, DONE, CANCELLED;

    fun isSettlementAddable(): Boolean = this == PLACE_VOTING || this == SETTLING
}
