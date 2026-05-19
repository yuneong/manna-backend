package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.command.UpdateAvailabilityCommand
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class UpdateAvailabilityRequest(
    @field:NotNull(message = "가용 날짜 목록은 필수입니다")
    val availableDates: List<LocalDate>,
) {
    fun toCommand(meetingId: Long, userId: Long) = UpdateAvailabilityCommand(
        meetingId = meetingId,
        userId = userId,
        availableDates = availableDates,
    )
}
