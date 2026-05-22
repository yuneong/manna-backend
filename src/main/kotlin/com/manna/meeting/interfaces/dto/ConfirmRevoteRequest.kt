package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.command.ConfirmRevoteCommand
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class ConfirmRevoteRequest(
    @field:NotNull(message = "확정 날짜는 필수입니다")
    val confirmedDate: LocalDate,
) {
    fun toCommand(meetingId: Long, userId: Long) = ConfirmRevoteCommand(
        meetingId = meetingId,
        userId = userId,
        confirmedDate = confirmedDate,
    )
}
