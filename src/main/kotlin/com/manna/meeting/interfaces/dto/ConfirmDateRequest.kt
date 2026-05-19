package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.command.ConfirmDateCommand
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class ConfirmDateRequest(
    @field:NotNull(message = "확정 날짜는 필수입니다")
    val confirmedDate: LocalDate,
) {
    fun toCommand(meetingId: Long, userId: Long) = ConfirmDateCommand(
        meetingId = meetingId,
        userId = userId,
        confirmedDate = confirmedDate,
    )
}
