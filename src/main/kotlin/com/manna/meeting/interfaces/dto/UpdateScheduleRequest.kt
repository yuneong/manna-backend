package com.manna.meeting.interfaces.dto

import com.manna.meeting.application.command.UpdateScheduleCommand
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class UpdateScheduleRequest(
    @field:NotNull(message = "일정 날짜 목록은 필수입니다")
    val scheduledDates: List<LocalDate>,
) {
    fun toCommand(meetingId: Long, userId: Long) = UpdateScheduleCommand(
        meetingId = meetingId,
        userId = userId,
        scheduledDates = scheduledDates,
    )
}
